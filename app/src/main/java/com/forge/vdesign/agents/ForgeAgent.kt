package com.forge.vdesign.agents

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxTool
import com.forge.vdesign.mcp.McpToolExecutor
import com.forge.vdesign.mcp.McpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
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
    private val mcpToolExecutor: McpToolExecutor
) {

    companion object {
        private const val TAG = "ForgeAgent"
        private const val MAX_LOOP_ITERATIONS = 12   // safety cap

        /**
         * Short identity prompt — describes WHO the agent is and WHAT it can do.
         * Behavior emerges from the model's reasoning + tool definitions.
         * NOT a rulebook.
         */
        private const val SYSTEM_PROMPT = """You are FORGE — an autonomous mobile UI design agent inside the FORGE Design Studio app.

Your tools let you ask questions, create projects, generate screens, and revise them.
Use them thoughtfully. Great design starts with understanding — gather context before generating.

When you generate a screen, you will receive its screenshot URL as the tool result.
React to what you see: comment on what worked, what could be stronger, whether it matches the brief.
You are a collaborator, not just a generator. Have opinions. Push back when something feels off.

Speak naturally. Be specific. Don't be generic."""
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
            description = "Generate one mobile UI screen using Stitch. Call this once per screen. Returns a screenshotUrl when done — look at what was generated and react to it before moving to the next screen.",
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
            description = "Revise an already-generated screen based on feedback. Use this when the user wants changes, or when you notice something off after seeing the result.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "projectId" to mapOf("type" to "string", "description" to "Project ID"),
                    "screenId" to mapOf("type" to "string", "description" to "Screen ID from the generate_screen result"),
                    "feedback" to mapOf("type" to "string", "description" to "What to change and why")
                ),
                "required" to listOf("projectId", "screenId", "feedback")
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
        history: List<AgentMessage>
    ): Flow<AgentEvent> = flow {
        android.util.Log.d(TAG, "runLoop start: '${userMessage.take(60)}'")

        // Build working history: full history + new user message
        val workingHistory = mutableListOf<AgentMessage>()
        workingHistory.add(AgentMessage.Text("system", SYSTEM_PROMPT))
        workingHistory.addAll(history)
        workingHistory.add(AgentMessage.Text("user", userMessage))

        // Track generated project IDs for revise_screen cross-referencing
        var lastProjectId: String? = null
        // Track generated screen IDs for revise_screen
        val screenIds = mutableMapOf<String, String>() // screenName -> screenId

        repeat(MAX_LOOP_ITERATIONS) { iteration ->
            android.util.Log.d(TAG, "Loop iteration $iteration, history=${workingHistory.size} msgs")

            val response = miniMaxClient.callWithTools(workingHistory, FORGE_TOOLS)

            // ── 1. Emit thinking if present ──────────────────────────────────
            if (!response.thinkingContent.isNullOrBlank()) {
                emit(AgentEvent.Thinking(response.thinkingContent))
            }

            // ── 2. Emit spoken content if present ────────────────────────────
            if (!response.spokenContent.isNullOrBlank()) {
                emit(AgentEvent.Speaks(response.spokenContent))
            }

            // ── 3. If no tool calls → natural end ────────────────────────────
            if (response.toolCalls.isNullOrEmpty()) {
                emit(AgentEvent.LoopDone)
                return@flow
            }

            // ── 4. Append full assistant response to history ─────────────────
            response.rawAssistantMessage?.let { workingHistory.add(it) }

            // ── 5. Execute each tool call ─────────────────────────────────────
            for (toolCall in response.toolCalls) {
                android.util.Log.d(TAG, "Tool call: ${toolCall.name}(${toolCall.arguments.keys})")

                val toolResult: String = when (toolCall.name) {

                    "think_out_loud" -> {
                        // Already shown as Speaks — just ack it
                        val obs = toolCall.arguments["observation"] as? String ?: ""
                        if (response.spokenContent.isNullOrBlank()) {
                            // Sometimes think_out_loud IS the spoken content
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
                        }
                    }

                    "generate_screen" -> {
                        val projectId  = toolCall.arguments["projectId"] as? String ?: lastProjectId ?: ""
                        val screenName = toolCall.arguments["screenName"] as? String ?: "Screen"
                        val prompt     = toolCall.arguments["prompt"] as? String ?: ""

                        if (projectId.isEmpty()) {
                            emit(AgentEvent.StatusLine("⚠ No project ID — create_project must be called first"))
                            "Error: No project ID. Call create_project first."
                        } else {
                            emit(AgentEvent.StatusLine("Generating $screenName..."))

                            val result = try {
                                mcpToolExecutor.generateScreen(projectId, prompt, "MOBILE")
                            } catch (e: Exception) {
                                emit(AgentEvent.StatusLine("⚠ $screenName failed: ${e.message?.take(60)}"))
                                workingHistory.add(AgentMessage.ToolResult(toolCall.id, "Error: ${e.message}"))
                                continue
                            }

                            when (result) {
                                is McpResult.Success -> {
                                    val screen = result.data
                                    if (screen.screenId != null) screenIds[screenName] = screen.screenId
                                    emit(AgentEvent.ScreenReady(
                                        screenName   = screenName,
                                        screenshotUrl = screen.screenshotUrl,
                                        htmlUrl      = screen.htmlUrl,
                                        projectId    = projectId
                                    ))
                                    "Screen generated. screenshotUrl=${screen.screenshotUrl ?: "pending"}. screenId=${screen.screenId ?: "unknown"}"
                                }
                                is McpResult.Error -> {
                                    emit(AgentEvent.StatusLine("⚠ $screenName failed: ${result.message.take(60)}"))
                                    "Error: ${result.message}"
                                }
                            }
                        }
                    }

                    "revise_screen" -> {
                        val projectId = toolCall.arguments["projectId"] as? String ?: lastProjectId ?: ""
                        val screenId  = toolCall.arguments["screenId"] as? String ?: ""
                        val feedback  = toolCall.arguments["feedback"] as? String ?: ""

                        if (projectId.isEmpty() || screenId.isEmpty()) {
                            "Error: projectId and screenId are required for revision"
                        } else {
                            emit(AgentEvent.StatusLine("Revising screen..."))

                            val result: McpResult<com.forge.vdesign.mcp.StitchScreenResult> = try {
                                mcpToolExecutor.editScreens(projectId, listOf(screenId), feedback)
                            } catch (e: Exception) {
                                McpResult.Error(toolName = "edit_screens", message = e.message ?: "Unknown error", cause = e)
                            }

                            when (result) {
                                is McpResult.Success -> {
                                    val screen = result.data
                                    emit(AgentEvent.ScreenReady(
                                        screenName    = "Revised Screen",
                                        screenshotUrl = screen.screenshotUrl,
                                        htmlUrl       = screen.htmlUrl,
                                        projectId     = projectId
                                    ))
                                    "Screen revised. screenshotUrl=${screen.screenshotUrl ?: "pending"}"
                                }
                                is McpResult.Error -> {
                                    emit(AgentEvent.StatusLine("⚠ Revision failed: ${result.message.take(60)}"))
                                    "Error: ${result.message}"
                                }
                            }
                        }
                    }

                    else -> {
                        android.util.Log.w(TAG, "Unknown tool: ${toolCall.name}")
                        "Unknown tool: ${toolCall.name}"
                    }
                }

                // Append tool result to history so LLM sees what happened
                workingHistory.add(AgentMessage.ToolResult(toolCall.id, toolResult))
            }

            // Continue loop — LLM will see tool results and reason about next step
        }

        // Safety: loop limit reached
        android.util.Log.w(TAG, "Loop limit ($MAX_LOOP_ITERATIONS) reached")
        emit(AgentEvent.LoopDone)
    }

    // ── Legacy result types (kept for any residual references) ────────────────

    sealed class AgentResult {
        data class CompileBrief(val brief: com.forge.vdesign.domain.model.DesignBrief) : AgentResult()
        data class AskQuestion(val question: String) : AgentResult()
        data class ChatResponse(val text: String) : AgentResult()
        data class Error(val message: String) : AgentResult()
    }
}
