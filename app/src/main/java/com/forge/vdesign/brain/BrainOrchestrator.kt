package com.forge.vdesign.brain

import com.forge.vdesign.agents.AccessibilityAuditorAgent
import com.forge.vdesign.agents.CriticAgent
import com.forge.vdesign.agents.CriticReport
import com.forge.vdesign.agents.IntentArchitectAgent
import com.forge.vdesign.agents.MemoryKeeperAgent
import com.forge.vdesign.agents.StitchOutput
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.DesignDna
import com.forge.vdesign.domain.model.DesignReasoning
import com.forge.vdesign.domain.model.GeneratedScreen
import com.forge.vdesign.mcp.McpResult
import com.forge.vdesign.mcp.McpToolExecutor
import com.forge.vdesign.mcp.StitchScreenResult
import com.forge.vdesign.skills.SkillRepository
import com.forge.vdesign.skills.SkillRouter
import com.forge.vdesign.skills.models.Skill
import com.forge.vdesign.agents.AgentEvent
import com.forge.vdesign.agents.WorkspaceContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BrainOrchestrator
 *
 * Production-grade controller for the FORGE agentic design loop.
 */
@Singleton
class BrainOrchestrator @Inject constructor(
    private val intentArchitectAgent: IntentArchitectAgent,
    private val skillRouter: SkillRouter,
    private val skillRepository: SkillRepository,
    private val mcpToolExecutor: McpToolExecutor,
    private val criticAgent: CriticAgent,
    private val miniMaxClient: MiniMaxClient,
    private val memoryKeeperAgent: MemoryKeeperAgent,
    private val accessibilityAuditorAgent: AccessibilityAuditorAgent
) {

    companion object {
        private const val MAX_REVISIONS = 3
        private const val TAG = "BrainOrchestrator"
    }

    private var projectDna: DesignDna? = null

    /**
     * Entry point for a single screen generation.
     */
    suspend fun generateScreen(
        prompt: String,
        onLog: (AgentLogEntry) -> Unit = {}
    ): OrchestrationResult {
        onLog(AgentLogEntry("IntentArchitect", "Analyzing design requirements...", LogStatus.RUNNING))

        val intentResult = intentArchitectAgent.run(listOf("user" to prompt))
        val brief = intentResult.brief ?: return OrchestrationResult.Failure("Could not structure design intent.")
        onLog(AgentLogEntry("IntentArchitect", "Design brief finalized.", LogStatus.DONE))

        onLog(AgentLogEntry("Stitch MCP", "Initializing production project...", LogStatus.RUNNING))
        val projectResult = mcpToolExecutor.createProject(brief.projectName)
        
        val projectId = when (projectResult) {
            is McpResult.Success -> projectResult.data.projectId
            is McpResult.Error -> {
                onLog(AgentLogEntry("Stitch MCP", "Failed: ${projectResult.message}", LogStatus.FAILED))
                return OrchestrationResult.Failure("Stitch Project Creation Failed: ${projectResult.message}")
            }
        } ?: return OrchestrationResult.Failure("Stitch API returned no Project ID.")

        onLog(AgentLogEntry("Stitch MCP", "Project created: $projectId", LogStatus.DONE))
        
        var assetId: String? = null
        if (brief.primaryColorHex != null || brief.fontFamily != null) {
            onLog(AgentLogEntry("Stitch MCP", "Creating Design System...", LogStatus.RUNNING))
            val dsMap = mutableMapOf<String, Any>()
            val colors = mutableMapOf<String, Any>()
            brief.primaryColorHex?.let { colors["primary"] = it }
            if (colors.isNotEmpty()) dsMap["colors"] = colors
            
            val typography = mutableMapOf<String, Any>()
            brief.fontFamily?.let { typography["fontFamily"] = it }
            if (typography.isNotEmpty()) dsMap["typography"] = typography
            
            dsMap["appearance"] = mapOf("mode" to if (brief.isDarkMode) "DARK" else "LIGHT")
            
            val dsResult = mcpToolExecutor.createDesignSystem(projectId, dsMap)
            if (dsResult is McpResult.Success && dsResult.data.assetId != null) {
                assetId = dsResult.data.assetId
                onLog(AgentLogEntry("Stitch MCP", "Design System created: $assetId", LogStatus.DONE))
            } else {
                onLog(AgentLogEntry("Stitch MCP", "Design System creation failed", LogStatus.FAILED))
            }
        }

        val briefWithProject = brief.copy(stitchProjectId = projectId, designSystemAssetId = assetId)
        val screenName = brief.plannedScreens.firstOrNull() ?: "Home Screen"

        return generateSingleScreen(briefWithProject, screenName, onLog)
    }

    /**
     * Multi-screen generation flow.
     */
    fun generateScreenSet(
        brief: DesignBrief,
        onLog: (AgentLogEntry) -> Unit = {}
    ): Flow<OrchestrationResult> = flow {
        // Kept for backward compatibility
    }

    /**
     * The main deterministic generation pipeline for the Chat UI.
     * Replaces the ReAct loop in ForgeAgent.
     */
    fun runPipeline(
        brief: DesignBrief,
        context: WorkspaceContext
    ): Flow<AgentEvent> = flow {
        emit(AgentEvent.Thinking("Structuring design requirements..."))
        emit(AgentEvent.StatusLine("Setting up project: ${brief.projectName}"))

        // 1. Create or use existing project
        val projectId = if (context.projectId.isNotEmpty()) {
            context.projectId
        } else {
            emit(AgentEvent.Thinking("Creating new Stitch project: ${brief.projectName}"))
            val projectResult = try {
                mcpToolExecutor.createProject(brief.projectName)
            } catch (e: Exception) {
                emit(AgentEvent.Speaks("Failed to create project: ${e.message}"))
                return@flow
            }
            when (projectResult) {
                is McpResult.Success -> projectResult.data.projectId
                is McpResult.Error -> {
                    emit(AgentEvent.Speaks("Stitch project creation failed: ${projectResult.message}"))
                    return@flow
                }
            }
        }

        if (projectId == null) {
            emit(AgentEvent.Speaks("Could not get a valid Project ID from Stitch."))
            return@flow
        }
        
        var assetId: String? = null
        if (brief.primaryColorHex != null || brief.fontFamily != null) {
            emit(AgentEvent.Thinking("Creating Design System..."))
            val dsMap = mutableMapOf<String, Any>()
            val colors = mutableMapOf<String, Any>()
            brief.primaryColorHex?.let { colors["primary"] = it }
            if (colors.isNotEmpty()) dsMap["colors"] = colors
            
            val typography = mutableMapOf<String, Any>()
            brief.fontFamily?.let { typography["fontFamily"] = it }
            if (typography.isNotEmpty()) dsMap["typography"] = typography
            
            dsMap["appearance"] = mapOf("mode" to if (brief.isDarkMode) "DARK" else "LIGHT")
            
            val dsResult = mcpToolExecutor.createDesignSystem(projectId, dsMap)
            if (dsResult is McpResult.Success && dsResult.data.assetId != null) {
                assetId = dsResult.data.assetId
                emit(AgentEvent.StatusLine("Applied Design System"))
            }
        }

        val briefWithProject = brief.copy(stitchProjectId = projectId, designSystemAssetId = assetId)

        // 2. Generate each planned screen
        for (screenName in brief.plannedScreens) {
            emit(AgentEvent.Thinking("Designing $screenName..."))
            emit(AgentEvent.StatusLine("Designing $screenName"))

            // 2a. Skill Injection
            emit(AgentEvent.Thinking("Retrieving design skills for $screenName..."))
            val skills = try { skillRouter.routeSkills(briefWithProject, limit = 3) } catch (e: Exception) { emptyList() }
            
            // 2b. Synthesize Stitch Prompt
            val stitchPrompt = synthesizeStitchPrompt(briefWithProject, screenName, skills)

            // 2c. Generation
            emit(AgentEvent.StatusLine("Rendering pixels in Stitch..."))
            val mcpResult = mcpToolExecutor.generateScreen(projectId, stitchPrompt, "MOBILE")

            when (mcpResult) {
                is McpResult.Success -> {
                    val screen = mcpResult.data
                    if (screen.error != null) {
                        emit(AgentEvent.Speaks("Failed to render $screenName: ${screen.error}"))
                    } else if (screen.isSuccess) {
                        // 2d. Critique (Phase 5 addition)
                        emit(AgentEvent.Thinking("Analyzing design quality of $screenName..."))
                        val critique = criticAgent.evaluate(StitchOutput(screen.description ?: "", screen.suggestions ?: emptyList(), screen.screenshotUrl), skills)
                        
                        var finalScreen = screen
                        var finalCritique = critique

                        // 2e. Auto-Revise if score is low
                        if (critique.overallScore < 7.0 && critique.suggestedFixes.isNotEmpty() && screen.screenId != null) {
                            emit(AgentEvent.StatusLine("Score too low (${critique.overallScore}/10). Auto-revising..."))
                            val editInstruction = critique.suggestedFixes.joinToString("; ")
                            val reviseResult = mcpToolExecutor.editScreens(projectId, listOf(screen.screenId), editInstruction)
                            
                            if (reviseResult is McpResult.Success && reviseResult.data.isSuccess) {
                                finalScreen = reviseResult.data
                                // Re-evaluate (optional, but good for reporting)
                                finalCritique = criticAgent.evaluate(StitchOutput(finalScreen.description ?: "", finalScreen.suggestions ?: emptyList(), finalScreen.screenshotUrl), skills)
                            }
                        }

                        // Apply the design system if available
                        if (briefWithProject.designSystemAssetId != null && finalScreen.screenId != null) {
                            emit(AgentEvent.StatusLine("Applying Design System..."))
                            mcpToolExecutor.applyDesignSystem(projectId, listOf(finalScreen.screenId), briefWithProject.designSystemAssetId)
                        }

                        // Emit final result
                        emit(AgentEvent.Speaks("I've finished designing the **$screenName**. (Quality Score: ${finalCritique.overallScore}/10)"))
                        emit(AgentEvent.ScreenReady(
                            screenName = screenName,
                            screenshotUrl = finalScreen.screenshotUrl,
                            htmlUrl = finalScreen.htmlUrl,
                            projectId = projectId,
                            designReasoning = mapOf("Critique" to (finalCritique.suggestedFixes.take(3).joinToString("; ").takeIf { it.isNotBlank() } ?: "Layout generated successfully."))
                        ))
                    } else {
                         emit(AgentEvent.Speaks("Stitch generated $screenName, but returned no preview URL."))
                    }
                }
                is McpResult.Error -> {
                    emit(AgentEvent.Speaks("Failed to generate $screenName: ${mcpResult.message}"))
                }
            }
        }
        
        emit(AgentEvent.StatusLine(null)) // clear status
    }

    /**
     * Regenerate an existing screen.
     */
    suspend fun regenerate(
        screenId: String,
        brief: DesignBrief,
        screenName: String,
        onLog: (AgentLogEntry) -> Unit = {}
    ): OrchestrationResult {
        val projectId = brief.stitchProjectId ?: return OrchestrationResult.Failure("No Project ID")
        
        onLog(AgentLogEntry("Stitch MCP", "Regenerating $screenName...", LogStatus.RUNNING, screenName))
        val mcpResult = mcpToolExecutor.editScreens(
            projectId = projectId,
            screenIds = listOf(screenId),
            editInstruction = "Apply a different layout style while keeping the same content."
        )

        return when (mcpResult) {
            is McpResult.Success -> {
                onLog(AgentLogEntry("Stitch MCP", "Regeneration complete.", LogStatus.DONE, screenName))
                buildOrchestrationResult(mcpResult.data, screenName, brief, emptyList())
            }
            is McpResult.Error -> OrchestrationResult.Failure(mcpResult.message)
        }
    }

    private suspend fun generateSingleScreen(
        brief: DesignBrief,
        screenName: String,
        onLog: (AgentLogEntry) -> Unit
    ): OrchestrationResult {
        // ✔ Safe null check — !! was causing NPE crash
        val projectId = brief.stitchProjectId
            ?: return OrchestrationResult.Failure("No Stitch project ID for $screenName")

        onLog(AgentLogEntry("SkillRouter", "Retrieving design skills...", LogStatus.RUNNING, screenName))
        val skills = try { skillRouter.routeSkills(brief, limit = 3) } catch (e: Exception) { emptyList() }
        onLog(AgentLogEntry("SkillRouter", "${skills.size} skills loaded.", LogStatus.DONE, screenName))

        onLog(AgentLogEntry("MiniMax", "Synthesizing prompt...", LogStatus.RUNNING, screenName))
        val stitchPrompt = synthesizeStitchPrompt(brief, screenName, skills)
        onLog(AgentLogEntry("MiniMax", "Prompt ready.", LogStatus.DONE, screenName))

        onLog(AgentLogEntry("Stitch MCP", "Rendering screen...", LogStatus.RUNNING, screenName))
        val mcpResult = mcpToolExecutor.generateScreen(
            projectId = projectId,
            prompt = stitchPrompt,
            deviceType = "MOBILE"
        )

        return when (mcpResult) {
            is McpResult.Success -> {
                val screen = mcpResult.data
                if (screen.error != null) {
                    onLog(AgentLogEntry("Stitch MCP", "Error: ${screen.error}", LogStatus.FAILED, screenName))
                    OrchestrationResult.Failure(screen.error)
                } else {
                    onLog(AgentLogEntry("Stitch MCP", "$screenName done ✓", LogStatus.DONE, screenName))
                    
                    if (brief.designSystemAssetId != null && screen.screenId != null) {
                        onLog(AgentLogEntry("Stitch MCP", "Applying Design System...", LogStatus.RUNNING, screenName))
                        mcpToolExecutor.applyDesignSystem(projectId, listOf(screen.screenId), brief.designSystemAssetId)
                        onLog(AgentLogEntry("Stitch MCP", "Design System applied.", LogStatus.DONE, screenName))
                    }
                    
                    buildOrchestrationResult(screen, screenName, brief, skills)
                }
            }
            is McpResult.Error -> {
                onLog(AgentLogEntry("Stitch MCP", "Failed: ${mcpResult.message}", LogStatus.FAILED, screenName))
                OrchestrationResult.Failure(mcpResult.message)
            }
        }
    }

    private suspend fun synthesizeStitchPrompt(
        brief: DesignBrief,
        screenName: String,
        skills: List<Skill>
    ): String {
        // Format skill rules into the three-tier block — same format as ForgeAgent's injection.
        // The LLM synthesizing the Stitch prompt now has the actual design rules in context,
        // not just skill names. It reasons with them the same way Antigravity does.
        val skillBlock = skillRouter.formatSkillsForSystemPrompt(skills)

        val systemPrompt = buildString {
            append("You are a Stitch prompt engineer for FORGE AI Design Studio. ")
            append("Convert the design brief below into a detailed, pixel-precise mobile UI description ")
            append("for the Stitch rendering engine. Output ONLY the Stitch description — no explanations.\n\n")
            if (skillBlock.isNotBlank()) {
                append("The following design rules are your expertise. Apply every applicable rule ")
                append("when writing the Stitch description:\n")
                append(skillBlock)
            }
        }

        val userPrompt = buildString {
            appendLine("App: ${brief.projectName}")
            appendLine("Screen: $screenName")
            appendLine("Platform: Android Mobile, ${if (brief.isDarkMode) "Dark Mode" else "Light Mode"}")
            appendLine("Goal: ${brief.userGoal}")
            if (brief.mood.isNotBlank()) appendLine("Mood: ${brief.mood}")
            if (brief.primaryColorHex != null) appendLine("Primary Color: ${brief.primaryColorHex}")
            if (brief.fontFamily != null) appendLine("Font: ${brief.fontFamily}")
        }

        return try {
            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(
                    MiniMaxMessage("system", systemPrompt),
                    MiniMaxMessage("user", userPrompt)
                ))
            )
            response.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                ?: brief.userGoal
        } catch (e: Exception) {
            android.util.Log.w("BrainOrchestrator", "Prompt synthesis failed, using raw goal: ${e.message}")
            brief.userGoal
        }
    }


    private fun buildOrchestrationResult(
        screen: StitchScreenResult,
        screenName: String,
        brief: DesignBrief,
        skills: List<Skill>
    ): OrchestrationResult {
        return OrchestrationResult.Success(
            GeneratedScreen(
                screenId = screen.screenId ?: UUID.randomUUID().toString(),
                screenName = screenName,
                htmlUrl = screen.htmlUrl,
                screenshotUrl = screen.screenshotUrl,
                description = screen.description,
                brief = brief,
                designReasoning = skills.map { DesignReasoning(it.name, it.rules.firstOrNull()?.rule ?: "", "", it.id) }
            ),
            skills,
            CriticReport(10f, true)
        )
    }
}

sealed class OrchestrationResult {
    data class Success(val screen: GeneratedScreen, val activeSkills: List<Skill>, val report: CriticReport) : OrchestrationResult()
    data class Failure(val error: String) : OrchestrationResult()
}
