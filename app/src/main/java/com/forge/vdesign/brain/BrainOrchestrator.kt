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
        
        val briefWithProject = brief.copy(stitchProjectId = projectId)
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
        onLog(AgentLogEntry("Stitch MCP", "Creating project...", LogStatus.RUNNING))

        val projectResult = try {
            mcpToolExecutor.createProject(brief.projectName)
        } catch (e: Exception) {
            emit(OrchestrationResult.Failure("createProject error: ${e.message}"))
            return@flow
        }

        val projectId = when (projectResult) {
            is McpResult.Success -> projectResult.data.projectId
            is McpResult.Error  -> {
                emit(OrchestrationResult.Failure("Stitch project creation failed: ${projectResult.message}"))
                return@flow
            }
        }

        if (projectId == null) {
            emit(OrchestrationResult.Failure("Stitch returned no project ID — check server logs"))
            return@flow
        }

        onLog(AgentLogEntry("Stitch MCP", "Project ready ✓ $projectId", LogStatus.DONE))
        val briefWithProject = brief.copy(stitchProjectId = projectId)

        for (screenName in brief.plannedScreens) {
            onLog(AgentLogEntry("BrainOrchestrator", "Generating: $screenName", LogStatus.RUNNING, screenName))
            val result = try {
                generateSingleScreen(briefWithProject, screenName, onLog)
            } catch (e: Exception) {
                android.util.Log.e("BrainOrchestrator", "generateSingleScreen crash: ${e.message}", e)
                OrchestrationResult.Failure("$screenName failed: ${e.message}")
            }
            emit(result)
        }
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
        val systemPrompt = "You are a professional Stitch prompt engineer. Convert the user's design goal and these skills into a detailed UI description for the Stitch rendering engine. Output ONLY the description."
        val userPrompt = "App: ${brief.projectName}\nScreen: $screenName\nGoal: ${brief.userGoal}\nSkills: ${skills.joinToString { it.name }}"

        return try {
            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(
                    MiniMaxMessage("system", systemPrompt),
                    MiniMaxMessage("user", userPrompt)
                ))
            )
            response.choices?.firstOrNull()?.message?.content ?: brief.userGoal
        } catch (e: Exception) {
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
