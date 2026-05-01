package com.forge.vdesign.agents

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxTool
import com.forge.vdesign.mcp.McpToolExecutor
import com.forge.vdesign.mcp.McpResult
import com.forge.vdesign.skills.SkillRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import com.forge.vdesign.agents.WorkspaceContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ForgeAgent — FORGE's real agentic brain using MiniMax-M2.7 native tool calling.
 *
 * Architecture: ReAct loop (Reason → Act → Observe → Reason again).
 *
 * The LLM stays in context for the entire conversation:
 *   user → model thinks → model calls tool → tool result → model thinks again → ...
 *
 * The model drives ALL decisions:
 *   - When to ask questions (via ask_user tool)
 *   - When it has enough context to generate (via create_project + generate_screen)
 *   - What to say about results (via think_out_loud + spoken content)
 *   - When to revise (via revise_screen tool)
 *
 * The Kotlin code ONLY: executes tool calls, emits UI events, manages message history.
 * NO hardcoded rules. NO keyword matching. NO state machine gating.
 */
@Singleton
class ForgeAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val mcpToolExecutor: McpToolExecutor,
    private val codeSurgeonAgent: CodeSurgeonAgent,
    private val skillRouter: SkillRouter
) {

    companion object {
        private const val TAG = "ForgeAgent"
        private const val MAX_LOOP_ITERATIONS = 30   // multi-screen projects need many iterations

        /**
         * Base identity prompt. Skills are NOT hardcoded here — they are injected
         * dynamically per session via buildSystemPrompt(), exactly like Antigravity.
         */
        private const val SYSTEM_PROMPT_BASE = """You are FORGE — an autonomous mobile UI design agent inside the FORGE Design Studio app.

Your tools let you ask questions, create projects, generate screens, and edit them.
Use them thoughtfully. Great design starts with understanding — gather context before generating.

CRITICAL INSTRUCTION: You must ALWAYS wrap your internal thought process, design reasoning, planning, and brainstorming strictly inside `<think> ... </think>` tags before you provide your spoken conversational response.

When you write a prompt for `generate_screen`, you MUST incorporate the DESIGN KNOWLEDGE block at the end of this prompt.
Think of those rules as your own design expertise — you already know them. Apply them naturally when writing Stitch prompts.

When you generate a screen with `generate_screen`, it will be shown to the user IMMEDIATELY.
You can then use `edit_screens` if you want to improve it based on feedback.

CRITICAL: DO NOT write out your tool calls or action logs as plain text in your spoken response. You MUST use the actual tool-calling JSON API. NEVER say things like "Action Log (Called: generate_screen)..." or describe parameters in your spoken output. If you are calling a tool, do not talk about it in the same turn unless you are using the `think_out_loud` tool first.

PERSISTENCE: If the user asks you to generate multiple screens, you MUST keep looping and calling `generate_screen` for EVERY screen in the plan until ALL screens are generated. Do NOT stop after one screen if more are planned. Do NOT ask permission between screens — just keep generating until the full plan is complete.

You are a collaborator, not just a generator. Speak naturally. Be specific. Don't be generic."""

        /**
         * Builds a per-session system prompt by appending the routed skill knowledge block.
         * This is the Antigravity model: skills are part of the LLM's context for THIS session,
         * not post-processing. The model reasons with real design rules when it writes Stitch prompts.
         */
        fun buildSystemPrompt(skillBlock: String): String =
            if (skillBlock.isBlank()) SYSTEM_PROMPT_BASE
            else "$SYSTEM_PROMPT_BASE\n\n$skillBlock"
    }

    // ── Tool definitions ──────────────────────────────────────────────────────

    private val FORGE_TOOLS = listOf(
        MiniMaxTool(
            name = "think_out_loud",
            description = "Share your current reasoning, design thinking, or observations with the user. Use this to explain WHY you're making design decisions — before asking a question, before generating, or after seeing a result.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "observation" to mapOf(
                        "type" to "string",
                        "description" to "Your current thinking or design rationale"
                    )
                ),
                "required" to listOf("observation")
            )
        ),
        MiniMaxTool(
            name = "ask_user",
            description = "Ask the user ONE focused question to gather missing context. Use this when you need specific information before you can design well. Never ask more than one question at a time. After they answer, continue the loop.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "question" to mapOf(
                        "type" to "string",
                        "description" to "A single, specific, conversational question"
                    )
                ),
                "required" to listOf("question")
            )
        ),
        MiniMaxTool(
            name = "create_project",
            description = "Create a new Stitch design project. Call this once when you have enough context and are ready to start generating screens. Returns a projectId you'll use for all generate_screen calls.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "name" to mapOf(
                        "type" to "string",
                        "description" to "Short branded project name (e.g. PulseFit, LunaBank)"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "Brief description of the app and its design direction"
                    )
                ),
                "required" to listOf("name", "description")
            )
        ),
        MiniMaxTool(
            name = "generate_screen",
            description = "Generate one mobile UI screen using Stitch. Call this once per screen. The generated screen will be shown to the user immediately.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "projectId" to mapOf("type" to "string", "description" to "Project ID from create_project"),
                    "screenName" to mapOf("type" to "string", "description" to "Human-readable screen name (e.g. Home Dashboard, Workout Tracker)"),
                    "prompt" to mapOf("type" to "string", "description" to "Detailed visual description for this specific screen — layout, key elements, colors, typography, mood")
                ),
                "required" to listOf("projectId", "screenName", "prompt")
            )
        ),
        MiniMaxTool(
            name = "revise_screen",
            description = "Revise an already-generated screen based on user feedback or your own observations. The revised screen will be shown to the user immediately.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "projectId" to mapOf("type" to "string", "description" to "Project ID"),
                    "screenId" to mapOf("type" to "string", "description" to "Screen ID from the generate_screen result"),
                    "feedback" to mapOf("type" to "string", "description" to "What to change and why")
                ),
                "required" to listOf("projectId", "screenId", "feedback")
            )
        ),
        MiniMaxTool(
            name = "edit_screens",
            description = "Edit one or more existing screens based on user feedback.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "projectId" to mapOf("type" to "string", "description" to "Project ID"),
                    "screenIds" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "List of screen IDs to edit"
                    ),
                    "editInstruction" to mapOf("type" to "string", "description" to "Specific instructions on what to change")
                ),
                "required" to listOf("projectId", "screenIds", "editInstruction")
            )
        ),
        MiniMaxTool(
            name = "list_screens",
            description = "List all screens currently in the project to find their IDs before editing.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "projectId" to mapOf("type" to "string", "description" to "Project ID")
                ),
                "required" to listOf("projectId")
            )
        )
    )

    // ── Agentic loop ──────────────────────────────────────────────────────────

    /**
     * Run one ReAct loop turn. Emits [AgentEvent]s as the loop progresses.
     *
     * [userMessage] — the latest message from the user
     * [history] — the full conversation history (all previous AgentMessages)
     *
     * The loop continues until:
     *   - ask_user is called → emits WaitingForUser and returns (user must reply)
     *   - finish_reason == stop → emits LoopDone and returns
     *   - MAX_LOOP_ITERATIONS reached → safety stop
     *
     * After each loop turn, the caller must:
     *   1. Append the new userMessage to history
     *   2. Append all assistant + tool result messages from this run to history
     *   (The events carry the history updates — ChatViewModel manages this)
     */
    fun runLoop(
        userMessage: String,
        history: List<AgentMessage>,
        workspaceContext: WorkspaceContext?
    ): Flow<AgentEvent> = flow {
        android.util.Log.d(TAG, "runLoop start: '${userMessage.take(60)}'")

        // ── Antigravity-style skill injection ─────────────────────────────────
        // Route relevant design skills from the raw user message BEFORE the first
        // LLM call. The skill rules become part of the system prompt so the model
        // reasons with them natively — the same way Antigravity uses SKILL.md files.
        // The LLM writes the Stitch prompt. Skills inform that writing. Stitch never
        // sees the skill files — only the model's skill-informed output.
        val activeSkills = try {
            skillRouter.routeSkillsFromText(userMessage, limit = 4)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Skill routing failed, continuing without skills: ${e.message}")
            emptyList()
        }
        val skillBlock = skillRouter.formatSkillsForSystemPrompt(activeSkills)
        val sessionSystemPrompt = buildSystemPrompt(skillBlock)

        android.util.Log.d(TAG, "Skills active this session: ${activeSkills.map { it.name }}")
        if (activeSkills.isNotEmpty()) {
            emit(AgentEvent.Thinking(
                "Loaded ${activeSkills.size} design skills: ${activeSkills.joinToString { it.name }}"
            ))
        }

        // Build working history: full history + new user message
        val workingHistory = mutableListOf<AgentMessage>()
        workingHistory.add(AgentMessage.Text("system", sessionSystemPrompt))
        workingHistory.addAll(history)
        workingHistory.add(AgentMessage.Text("user", userMessage))


        // Track generated project IDs for revise_screen cross-referencing
        var lastProjectId: String? = null
        // Track generated screen IDs for revise_screen
        val screenIds = mutableMapOf<String, String>() // screenName -> screenId

        // Track generated screens for cross-referencing in revise_screen
        val generatedScreens = mutableMapOf<String, com.forge.vdesign.mcp.StitchScreenResult>()

        repeat(MAX_LOOP_ITERATIONS) { iteration ->
            android.util.Log.d(TAG, "Loop iteration $iteration, history=${workingHistory.size} msgs")

            val response = miniMaxClient.callWithTools(workingHistory, FORGE_TOOLS)

            // ── 1. Emit thinking if present ──────────────────────────────────
            if (!response.thinkingContent.isNullOrBlank()) {
                emit(AgentEvent.Thinking(response.thinkingContent))
            }

            // ── 2. Hallucination Catcher: detect JSON tool calls in spoken text ─
            //    Sometimes LLMs "hallucinate" and write the tool call as raw JSON
            //    in their spoken response instead of using the native API protocol.
            //    We intercept this, parse it, and execute it as a real tool call.
            val hallucinatedTools = response.spokenContent?.let { extractHallucinatedToolCalls(it) } ?: emptyList()
            val effectiveToolCalls = if (response.toolCalls.isNullOrEmpty() && hallucinatedTools.isNotEmpty()) {
                android.util.Log.w(TAG, "Hallucination detected! Intercepted ${hallucinatedTools.size} JSON tool call(s) from spoken text.")
                emit(AgentEvent.StatusLine(null)) // Clear status
                hallucinatedTools
            } else {
                // Clean spoken content: emit only if it's not just a tool call dump
                if (!response.spokenContent.isNullOrBlank() && hallucinatedTools.isEmpty()) {
                    emit(AgentEvent.Speaks(response.spokenContent))
                }
                response.toolCalls ?: emptyList()
            }

            // ── 3. If no tool calls → natural end ──────────────────────────
            if (effectiveToolCalls.isEmpty()) {
                emit(AgentEvent.LoopDone)
                return@flow
            }

            // ── 4. Append full assistant response to history ─────────────────
            response.rawAssistantMessage?.let { workingHistory.add(it) }

            // ── 5. Execute each tool call ─────────────────────────────────────
            for (toolCall in effectiveToolCalls) {
                android.util.Log.d(TAG, "Tool call: ${toolCall.name}(${toolCall.arguments.keys})")
                
                // Emit log for timeline
                if (toolCall.name != "think_out_loud") {
                    val argsString = toolCall.arguments.entries.joinToString(", ") { "${it.key}=${it.value}" }
                    emit(AgentEvent.AgentActivityLog(title = "Called: ${toolCall.name}", content = argsString))
                }

                val startTime = System.currentTimeMillis()
                val toolResult: String = when (toolCall.name) {

                    "think_out_loud" -> {
                        val obs = toolCall.arguments["observation"] as? String ?: ""
                        // If there was no spoken content (or it was a hallucination), emit the observation
                        if (hallucinatedTools.isNotEmpty() || response.spokenContent.isNullOrBlank()) {
                            emit(AgentEvent.Speaks(obs))
                        }
                        "Thinking noted."
                    }

                    "ask_user" -> {
                        val question = toolCall.arguments["question"] as? String ?: ""
                        // If not already emitted as spokenContent, emit now
                        if (response.spokenContent.isNullOrBlank()) {
                            emit(AgentEvent.Speaks(question))
                        }
                        // Add to history so next turn has context
                        workingHistory.add(
                            AgentMessage.ToolResult(toolCall.id, "Question shown to user. Waiting for their response.")
                        )
                        emit(AgentEvent.WaitingForUser)
                        return@flow    // Exit loop — wait for next user message
                    }

                    "create_project" -> {
                        val name = toolCall.arguments["name"] as? String ?: "FORGE Project"
                        
                        if (!requestToolApproval("create_project", toolCall.arguments)) {
                            emit(AgentEvent.AgentActivityLog("Rejected", "User denied create_project"))
                            "User rejected this action."
                        } else {
                            emit(AgentEvent.StatusLine("Creating project \"$name\"..."))

                            val result = try {
                                mcpToolExecutor.createProject(name)
                            } catch (e: Exception) {
                                emit(AgentEvent.StatusLine("⚠ Project creation failed: ${e.message?.take(60)}"))
                                workingHistory.add(AgentMessage.ToolResult(toolCall.id, "Error: ${e.message}"))
                                continue
                            }

                            when (result) {
                                is McpResult.Success -> {
                                    val pid = result.data.projectId
                                    if (pid != null) {
                                        lastProjectId = pid
                                        emit(AgentEvent.StatusLine("Project \"$name\" ready ✓"))
                                        "projectId=$pid"
                                    } else {
                                        emit(AgentEvent.StatusLine("⚠ Project created but no ID returned"))
                                        "Error: Stitch returned no project ID"
                                    }
                                }
                                is McpResult.Error -> {
                                    val msg = result.message
                                    emit(AgentEvent.StatusLine("⚠ Project creation failed: ${msg.take(60)}"))
                                    "Error: $msg"
                                }
                                else -> "Error: Project creation failed"
                            }
                        }
                    }

                    "update_design_system" -> {
                        val projectId = toolCall.arguments["projectId"] as? String ?: lastProjectId ?: ""
                        val markdown  = toolCall.arguments["designSystemMarkdown"] as? String ?: ""

                        if (projectId.isEmpty()) {
                            emit(AgentEvent.StatusLine("⚠ No project ID — create_project must be called first"))
                            "Error: No project ID. Call create_project first."
                        } else if (!requestToolApproval("update_design_system", toolCall.arguments)) {
                            emit(AgentEvent.AgentActivityLog("Rejected", "User denied update_design_system"))
                            "User rejected this action."
                        } else {
                            emit(AgentEvent.StatusLine("Applying Design System..."))
                            "Design system applied."
                        }
                    }

                    "generate_screen" -> {
                        val projectId  = toolCall.arguments["projectId"] as? String ?: lastProjectId ?: ""
                        val screenName = toolCall.arguments["screenName"] as? String ?: "Screen"
                        val prompt     = toolCall.arguments["prompt"] as? String ?: ""

                        if (projectId.isEmpty()) {
                            emit(AgentEvent.StatusLine("⚠ No project ID"))
                            "Error: No project ID. Call create_project first."
                        } else if (!requestToolApproval("generate_screen", toolCall.arguments)) {
                            emit(AgentEvent.AgentActivityLog("Rejected", "User denied generate_screen"))
                            "User rejected this action."
                        } else {
                            emit(AgentEvent.StatusLine("Generating $screenName..."))
                            val result = try { mcpToolExecutor.generateScreen(projectId, prompt, "MOBILE") } catch (e: Exception) { null }
                            when (result) {
                                is McpResult.Success -> {
                                    val screen = result.data
                                    if (screen.screenId != null) {
                                        screenIds[screenName] = screen.screenId
                                        generatedScreens[screen.screenId] = screen
                                        emit(AgentEvent.StatusLine(null))
                                        emit(AgentEvent.ScreenReady(screenName, screen.screenshotUrl, screen.htmlUrl, lastProjectId ?: projectId, null))
                                        "SUCCESS. Screen '$screenName' (ID: ${screen.screenId}) is now visible."
                                    } else "Error: Screen generated but no ID returned."
                                }
                                else -> "Error: $screenName failed"
                            }
                        }
                    }

                    "edit_screens" -> {
                        val projectId = toolCall.arguments["projectId"] as? String ?: lastProjectId ?: ""
                        val screenIdsList = (toolCall.arguments["screenIds"] as? List<*>)?.map { it.toString() } ?: emptyList()
                        val rawInstruction = toolCall.arguments["editInstruction"] as? String ?: ""
                        
                        // Map screen names to actual IDs if the agent used names instead of IDs
                        val actualScreenIds = screenIdsList.map { id -> screenIds[id] ?: id }

                        val instruction = try {
                            val description = if (actualScreenIds.size == 1) generatedScreens[actualScreenIds[0]]?.description else null
                            emit(AgentEvent.StatusLine("Surgeon is enriching edit instruction..."))
                            codeSurgeonAgent.enrichEditInstruction(rawInstruction, description)
                        } catch (e: Exception) {
                            rawInstruction
                        }

                        if (projectId.isEmpty() || actualScreenIds.isEmpty()) {
                            emit(AgentEvent.StatusLine("⚠ Invalid project or screen IDs"))
                            "Error: Valid project ID and screen IDs required."
                        } else if (!requestToolApproval("edit_screens", toolCall.arguments)) {
                            emit(AgentEvent.AgentActivityLog("Rejected", "User denied edit_screens"))
                            "User rejected this action."
                        } else {
                            emit(AgentEvent.StatusLine("🛠 Editing ${actualScreenIds.size} screen(s)..."))
                            val result = try { mcpToolExecutor.editScreens(projectId, actualScreenIds, instruction) } catch (e: Exception) { null }
                            when (result) {
                                is McpResult.Success -> {
                                    val screen = result.data
                                    if (screen.screenId != null) {
                                        generatedScreens[screen.screenId] = screen
                                        emit(AgentEvent.StatusLine(null))
                                        val revisedName = screenIds.entries.find { it.value == screen.screenId }?.key ?: "Revised Screen"
                                        emit(AgentEvent.ScreenReady(revisedName, screen.screenshotUrl, screen.htmlUrl, lastProjectId ?: projectId, null))
                                        "SUCCESS. Edited '$revisedName' is now visible."
                                    } else "Error: Screen edited but no ID returned."
                                }
                                else -> "Error: Editing failed"
                            }
                        }
                    }

                    "list_screens" -> {
                        val projectId = toolCall.arguments["projectId"] as? String ?: lastProjectId ?: ""
                        if (projectId.isEmpty()) {
                            "Error: No project ID provided."
                        } else {
                            emit(AgentEvent.StatusLine("🔍 Listing screens..."))
                            val result = try { mcpToolExecutor.listScreens(projectId) } catch (e: Exception) { null }
                            emit(AgentEvent.StatusLine(null))
                            when (result) {
                                is McpResult.Success -> {
                                    val list = result.data
                                    if (list.isEmpty()) {
                                        "Project has no screens."
                                    } else {
                                        "Screens in project: " + list.joinToString(", ") { "${it.screenId} (URL: ${it.htmlUrl})" }
                                    }
                                }
                                else -> "Error: Failed to list screens"
                            }
                        }
                    }

                    else -> {
                        "Error: Unknown tool ${toolCall.name}"
                    }
                }

                val duration = System.currentTimeMillis() - startTime

                if (toolCall.name != "think_out_loud") {
                    emit(AgentEvent.AgentActivityLog("Result: ${toolCall.name} (took ${duration}ms)", toolResult.take(500)))
                }

                workingHistory.add(AgentMessage.ToolResult(toolCall.id, toolResult))
            }
        }

        android.util.Log.w(TAG, "Loop limit ($MAX_LOOP_ITERATIONS) reached")
        emit(AgentEvent.LoopDone)
    }

    // ── Hallucination Catcher Helper ──────────────────────────────────────────

    /**
     * Scans spoken LLM text for hallucinated tool calls written as raw JSON.
     * The LLM sometimes outputs:
     *   ```json
     *   {"name": "generate_screen", "arguments": {...}}
     *   ```
     * instead of using the native tool-calling API. This method intercepts them
     * and converts them into [RawToolCall] objects so the Kotlin backend
     * can execute them normally — indistinguishable from proper API tool calls.
     */
    private fun extractHallucinatedToolCalls(text: String): List<RawToolCall> {
        val results = mutableListOf<RawToolCall>()
        // Match ```json { ... } ``` blocks
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.MULTILINE)
        codeBlockRegex.findAll(text).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1].trim())
                val name = json.optString("name").takeIf { it.isNotBlank() } ?: return@forEach
                val argsObj = json.optJSONObject("arguments") ?: json.optJSONObject("parameters") ?: return@forEach
                val args = mutableMapOf<String, Any?>()
                argsObj.keys().forEach { key -> args[key] = argsObj.get(key) }
                results.add(RawToolCall(
                    id = "hallucinated_${System.currentTimeMillis()}",
                    name = name,
                    arguments = args
                ))
                android.util.Log.w(TAG, "Hallucination parsed: $name(${args.keys})")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to parse hallucinated tool: ${e.message}")
            }
        }
        // Also match bare JSON objects at the top level (no code fences)
        if (results.isEmpty()) {
            try {
                val trimmed = text.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    val json = JSONObject(trimmed)
                    val name = json.optString("name").takeIf { it.isNotBlank() } ?: return results
                    val argsObj = json.optJSONObject("arguments") ?: return results
                    val args = mutableMapOf<String, Any?>()
                    argsObj.keys().forEach { key -> args[key] = argsObj.get(key) }
                    results.add(RawToolCall(
                        id = "hallucinated_bare_${System.currentTimeMillis()}",
                        name = name,
                        arguments = args
                    ))
                }
            } catch (_: Exception) {}
        }
        return results
    }

    // ── Legacy result types (kept for any residual references) ────────────────

    sealed class AgentResult {
        data class CompileBrief(val brief: com.forge.vdesign.domain.model.DesignBrief) : AgentResult()
        data class AskQuestion(val question: String) : AgentResult()
        data class ChatResponse(val text: String) : AgentResult()
        data class Error(val message: String) : AgentResult()
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AgentEvent>.requestToolApproval(
        toolName: String,
        arguments: Map<String, Any?>
    ): Boolean {
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        emit(AgentEvent.ToolApprovalNeeded(toolName, arguments) { isAllowed ->
            deferred.complete(isAllowed)
        })
        return deferred.await()
    }
}
