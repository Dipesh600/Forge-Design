package com.forge.vdesign.agents

/**
 * AgentEvent — the real-time event stream that ForgeAgent emits to the UI.
 *
 * ChatViewModel collects these and translates them to visible UI state.
 * Every event except [WaitingForUser] and [LoopDone] should show something in chat.
 */
sealed class AgentEvent {

    /**
     * Model's internal reasoning (the <think> content from M2.7).
     * Shown as a collapsible thinking bubble with ✦ prefix.
     */
    data class Thinking(val content: String) : AgentEvent()

    /**
     * FORGE says something in chat — streamed word-by-word.
     * Used for: questions, commentary, design rationale, summaries.
     */
    data class Speaks(val text: String) : AgentEvent()

    /**
     * Small one-line status update — not a full bubble.
     * Used for: "Creating project...", "Generating Home screen...", "Project ready ✓"
     */
    data class StatusLine(val text: String?) : AgentEvent()

    /**
     * A screen was successfully generated.
     * Shows as a thumbnail card in chat — image loads via Glide, tap opens ScreenPreviewActivity.
     */
    data class ScreenReady(
        val screenName: String,
        val screenshotUrl: String?,
        val htmlUrl: String?,
        val projectId: String?,
        val designReasoning: Map<String, String>? = null
    ) : AgentEvent()

    /** Emitted when the agent defines the global project rules. */
    data class DesignSystemDefined(val markdownStr: String) : AgentEvent()

    /** Emits a persistent timeline entry (e.g. tool execution log) */
    data class AgentActivityLog(val title: String, val content: String) : AgentEvent()

    /** Requires the user to allow or reject a high-impact tool call. */
    data class ToolApprovalNeeded(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val onDecision: (Boolean) -> Unit
    ) : AgentEvent()

    /**
     * Agent asked the user a question — loop pauses here.
     * ChatViewModel waits for next sendMessage() call to resume.
     */
    object WaitingForUser : AgentEvent()

    /**
     * Agent finished naturally (no tool called, finish_reason == stop).
     * Loop ends, UI returns to idle.
     */
    object LoopDone : AgentEvent()

    /** Something failed — show in chat as error message */
    data class Error(val message: String) : AgentEvent()
}
