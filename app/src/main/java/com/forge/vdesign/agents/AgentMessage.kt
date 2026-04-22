package com.forge.vdesign.agents

import org.json.JSONArray
import org.json.JSONObject

/**
 * AgentMessage — represents every message type in a proper multi-turn tool-calling loop.
 *
 * Standard text messages use [Text].
 * When the model returns tool_calls, the FULL assistant response must be re-added
 * as [AssistantWithTools] — the IDs must be preserved to link tool results.
 * Tool results are sent back as [ToolResult] referencing the original call ID.
 */
sealed class AgentMessage {

    /** Simple user or assistant text message */
    data class Text(val role: String, val content: String) : AgentMessage()

    /**
     * Full assistant response when it called tools.
     * Must be appended verbatim to history — the tool_call IDs link results back.
     */
    data class AssistantWithTools(
        val content: String?,
        val thinkingContent: String?,
        val toolCalls: List<RawToolCall>
    ) : AgentMessage()

    /** Tool result — sent after executing a tool call */
    data class ToolResult(
        val toolCallId: String,
        val content: String
    ) : AgentMessage()

    /** Serialize to a Map for the Firebase proxy (which passes them to MiniMax as-is) */
    fun toMap(): Map<String, Any?> = when (this) {
        is Text -> mapOf("role" to role, "content" to content)

        is AssistantWithTools -> {
            val map = mutableMapOf<String, Any?>(
                "role" to "assistant",
                "content" to (content ?: "")
            )
            if (toolCalls.isNotEmpty()) {
                map["tool_calls"] = toolCalls.map { tc ->
                    mapOf(
                        "id"       to tc.id,
                        "type"     to "function",
                        "function" to mapOf(
                            "name"      to tc.name,
                            "arguments" to org.json.JSONObject(tc.arguments.filterValues { it != null } as Map<String, Any>).toString()
                        )
                    )
                }
            }
            map
        }

        is ToolResult -> mapOf(
            "role"         to "tool",
            "tool_call_id" to toolCallId,
            "content"      to content
        )
    }
}

/** A single tool call returned by the model */
data class RawToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>
)
