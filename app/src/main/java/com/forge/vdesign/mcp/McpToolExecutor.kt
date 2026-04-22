package com.forge.vdesign.mcp

import javax.inject.Inject
import javax.inject.Singleton

/**
 * McpToolExecutor
 *
 * Typed Kotlin wrappers around all Stitch MCP tools.
 * Adds retry logic, timeout handling, and error normalization.
 *
 * Correct Stitch tool mapping:
 *   generate_screen_from_text  → generateScreen()
 *   edit_screens               → editScreens()
 *   list_screens               → listScreens()
 *   create_project             → createProject()
 */
@Singleton
class McpToolExecutor @Inject constructor(
    private val stitchMcpClient: StitchMcpClient
) {
    companion object {
        private const val MAX_RETRIES = 3
        private const val TAG = "McpToolExecutor"
    }

    /** Generate a new screen — calls generate_screen_from_text */
    suspend fun generateScreen(
        projectId: String,
        prompt: String,
        deviceType: String = "MOBILE"
    ): McpResult<StitchScreenResult> = executeWithRetry("generate_screen_from_text") {
        stitchMcpClient.generateScreen(projectId, prompt, deviceType)
    }

    /** Edit existing screen(s) — calls edit_screens */
    suspend fun editScreens(
        projectId: String,
        screenIds: List<String>,
        editInstruction: String
    ): McpResult<StitchScreenResult> = executeWithRetry("edit_screens") {
        stitchMcpClient.editScreens(projectId, screenIds, editInstruction)
    }

    /** List all screens in a project */
    suspend fun listScreens(projectId: String): McpResult<List<StitchScreenResult>> =
        executeWithRetry("list_screens") {
            stitchMcpClient.listScreens(projectId)
        }

    /** Create a new Stitch project — always first step of generateScreenSet */
    suspend fun createProject(title: String): McpResult<StitchProjectResult> =
        executeWithRetry("create_project") {
            stitchMcpClient.createProject(title)
        }

    // ── Retry wrapper ─────────────────────────────────────────────────────────

    private suspend fun <T> executeWithRetry(
        toolName: String,
        action: suspend () -> T
    ): McpResult<T> {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                android.util.Log.d(TAG, "$toolName attempt ${attempt + 1}/$MAX_RETRIES")
                val result = action()
                android.util.Log.d(TAG, "$toolName succeeded")
                return McpResult.Success(result)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "$toolName attempt ${attempt + 1} failed: ${e.message}")
                lastException = e
                kotlinx.coroutines.delay(1000L * (attempt + 1))
            }
        }
        return McpResult.Error(
            toolName = toolName,
            message  = lastException?.message ?: "Unknown error after $MAX_RETRIES attempts",
            cause    = lastException
        )
    }
}

/** Typed result wrapper for all MCP tool calls */
sealed class McpResult<out T> {
    data class Success<T>(val data: T) : McpResult<T>()
    data class Error(
        val toolName: String,
        val message: String,
        val cause: Exception? = null
    ) : McpResult<Nothing>()

    val isSuccess get() = this is Success
    fun getOrNull() = (this as? Success)?.data
}
