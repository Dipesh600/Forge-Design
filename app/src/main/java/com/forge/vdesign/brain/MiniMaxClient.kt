package com.forge.vdesign.brain

import com.forge.vdesign.agents.AgentMessage
import com.forge.vdesign.agents.RawToolCall
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// ── Simple message types (backward compat for BrainOrchestrator prompt synthesis) ──

data class MiniMaxMessage(
    val role: String,
    val content: String
)

data class MiniMaxRequest(
    val messages: List<MiniMaxMessage>,
    val model: String = "MiniMax-Text-01",
    val stream: Boolean = false
)

data class MiniMaxChoice(val message: MiniMaxMessage)
data class MiniMaxResponse(val id: String?, val choices: List<MiniMaxChoice>?)

// ── Tool calling types ────────────────────────────────────────────────────────

data class MiniMaxTool(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

/**
 * Full response from a tool-calling turn.
 *
 * [rawAssistantMessage] MUST be appended back to history exactly as-is — it contains
 * the tool_call IDs that link tool results correctly in subsequent turns.
 *
 * [thinkingContent] is extracted from <think>...</think> tags in the content field (M2.7 feature).
 * [spokenContent] is the content with think tags stripped.
 * [toolCalls] contains the parsed tool calls the model wants to make.
 * [isFinished] is true when stop_reason == "stop" (no more tool calls).
 */
data class ToolCallResponse(
    val thinkingContent: String?,
    val spokenContent: String?,
    val toolCalls: List<RawToolCall>?,
    val rawAssistantMessage: AgentMessage.AssistantWithTools?,
    val isFinished: Boolean
)

/**
 * MiniMaxClient
 *
 * Handles both simple completion (for BrainOrchestrator's prompt synthesis)
 * and the full agentic tool-calling loop (for ForgeAgent's ReAct loop).
 *
 * Security: MiniMax API key lives ONLY in the Firebase Cloud Function — never on device.
 */
@Singleton
class MiniMaxClient @Inject constructor(
    private val functions: FirebaseFunctions,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "MiniMaxClient"
        private const val AGENT_MODEL = "MiniMax-M2.7"
        private const val SIMPLE_MODEL = "MiniMax-Text-01"
        private val THINK_REGEX = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
    }

    // ── Simple completion (used by BrainOrchestrator prompt synthesis) ────────

    suspend fun chatCompletion(request: MiniMaxRequest): MiniMaxResponse {
        val data = mapOf(
            "messages" to request.messages.map { mapOf("role" to it.role, "content" to it.content) },
            "model"    to request.model
        )
        val result = functions.getHttpsCallable("minimax_proxy").call(data).await()
        val json = gson.toJson(result.data)
        android.util.Log.d(TAG, "chatCompletion: ${json.take(200)}")
        checkMiniMaxError(json)
        return gson.fromJson(json, MiniMaxResponse::class.java)
    }

    // ── Agentic tool-calling (used by ForgeAgent's ReAct loop) ───────────────

    /**
     * Single turn in the ReAct loop. Sends full message history + tool definitions.
     * Returns the parsed response including think content, tool calls, and the
     * raw assistant message that must be re-appended to history.
     */
    suspend fun callWithTools(
        messages: List<AgentMessage>,
        tools: List<MiniMaxTool>
    ): ToolCallResponse {
        val toolSchemas = tools.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name"        to tool.name,
                    "description" to tool.description,
                    "parameters"  to tool.parameters
                )
            )
        }

        val data = mapOf(
            "messages"    to messages.map { it.toMap() },
            "model"       to AGENT_MODEL,
            "tools"       to toolSchemas,
            "tool_choice" to "auto"
        )

        return try {
            val result = functions.getHttpsCallable("minimax_proxy").call(data).await()
            val json = gson.toJson(result.data)
            android.util.Log.d(TAG, "callWithTools response: ${json.take(500)}")
            parseToolCallResponse(json)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "callWithTools failed: ${e.message}", e)
            ToolCallResponse(
                thinkingContent = null,
                spokenContent = "I hit a technical issue: ${e.message?.take(80)}. Let me try again.",
                toolCalls = null,
                rawAssistantMessage = null,
                isFinished = true
            )
        }
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private fun parseToolCallResponse(json: String): ToolCallResponse {
        return try {
            val root       = JSONObject(json)
            val choices    = root.optJSONArray("choices") ?: run {
                // Check for API-level error
                val baseResp = root.optJSONObject("base_resp")
                val msg = baseResp?.optString("status_msg") ?: "MiniMax returned no choices"
                return ToolCallResponse(null, msg, null, null, true)
            }
            val choice     = choices.optJSONObject(0) ?: return ToolCallResponse(null, "Empty response", null, null, true)
            val message    = choice.optJSONObject("message") ?: JSONObject()
            val finishReason = choice.optString("finish_reason", "stop")
            val rawContent = message.optString("content", "")

            // Extract <think> content and <tool_code> calls
            val thinkMatch = THINK_REGEX.find(rawContent)
            val thinkingText = thinkMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
            
            val toolRegex = "<tool_code>(.*?)</tool_code>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val toolMatch = toolRegex.findAll(rawContent)
            val toolCodeText = toolMatch.joinToString("\n\n") { it.value.trim() }.takeIf { it.isNotBlank() }
            
            val thinking = buildString {
                if (thinkingText != null) append(thinkingText)
                if (toolCodeText != null) {
                    if (isNotEmpty()) append("\n\n")
                    append("🛠 Tool Execution:\n")
                    append(toolCodeText)
                }
            }.takeIf { it.isNotBlank() }

            val spoken   = rawContent.replace(THINK_REGEX, "").replace(toolRegex, "").trim().takeIf { it.isNotBlank() }

            // Parse tool_calls if present
            val toolCallsJson = message.optJSONArray("tool_calls")
            val toolCalls = if (toolCallsJson != null && toolCallsJson.length() > 0) {
                (0 until toolCallsJson.length()).mapNotNull { i ->
                    val tc  = toolCallsJson.optJSONObject(i) ?: return@mapNotNull null
                    val fn  = tc.optJSONObject("function") ?: return@mapNotNull null
                    val id  = tc.optString("id", "call_${System.currentTimeMillis()}_$i")
                    val name = fn.optString("name", "")
                    val argsStr = fn.optString("arguments", "{}")
                    val args = try {
                        val argsJson = JSONObject(argsStr)
                        argsJson.keys().asSequence().associateWith { k -> argsJson.opt(k) }
                    } catch (_: Exception) { emptyMap() }
                    RawToolCall(id = id, name = name, arguments = args)
                }
            } else null

            val rawAssistant = if (!toolCalls.isNullOrEmpty()) {
                AgentMessage.AssistantWithTools(
                    content = rawContent,
                    thinkingContent = thinking,
                    toolCalls = toolCalls
                )
            } else null

            ToolCallResponse(
                thinkingContent = thinking,
                spokenContent   = spoken,
                toolCalls       = toolCalls?.takeIf { it.isNotEmpty() },
                rawAssistantMessage = rawAssistant,
                isFinished      = finishReason == "stop" || toolCalls.isNullOrEmpty()
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "parseToolCallResponse failed: ${e.message}\nJSON: ${json.take(300)}", e)
            ToolCallResponse(null, "Parse error: ${e.message?.take(60)}", null, null, true)
        }
    }

    // ── Streaming (simulated word-by-word for chat bubbles) ──────────────────

    fun chatCompletionStream(request: MiniMaxRequest): Flow<String> = flow {
        val data = mapOf(
            "messages" to request.messages.map { mapOf("role" to it.role, "content" to it.content) },
            "model"    to request.model
        )
        try {
            val result = functions.getHttpsCallable("minimax_proxy").call(data).await()
            val json = gson.toJson(result.data)
            val response = gson.fromJson(json, MiniMaxResponse::class.java)
            val text = response.choices?.firstOrNull()?.message?.content ?: ""
            val words = text.split(" ")
            words.forEachIndexed { i, word ->
                emit(if (i == 0) word else " $word")
                kotlinx.coroutines.delay(18L)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Stream failed", e)
            emit("I'm having trouble connecting. Please try again.")
        }
    }.flowOn(Dispatchers.IO)

    // ── Embeddings ────────────────────────────────────────────────────────────

    suspend fun getEmbeddings(texts: List<String>): List<List<Float>> {
        val data = mapOf("texts" to texts)
        val result = functions.getHttpsCallable("minimax_embedding").call(data).await()
        val jsonMap = result.data as? Map<*, *>
        val vectorsList = jsonMap?.get("vectors") as? List<*>
        return vectorsList?.mapNotNull { vector ->
            (vector as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() }
        } ?: emptyList()
    }

    // ── Error check ───────────────────────────────────────────────────────────

    private fun checkMiniMaxError(json: String) {
        try {
            val root = JSONObject(json)
            val baseResp = root.optJSONObject("base_resp") ?: return
            val statusCode = baseResp.optInt("status_code", 1000)
            val statusMsg  = baseResp.optString("status_msg", "")
            if (statusCode != 1000 && statusCode != 0) {
                val friendly = when (statusCode) {
                    2049 -> "MiniMax API key invalid (${statusCode}). Please update the secret."
                    1039 -> "MiniMax rate limit. Wait a moment and try again."
                    1002 -> "MiniMax quota exceeded. Check account balance."
                    else -> "MiniMax error ${statusCode}: $statusMsg"
                }
                android.util.Log.e(TAG, "API error: $friendly")
                throw RuntimeException(friendly)
            }
        } catch (_: org.json.JSONException) { }
    }
}
