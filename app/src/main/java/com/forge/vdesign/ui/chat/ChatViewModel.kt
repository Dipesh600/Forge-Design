package com.forge.vdesign.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.vdesign.agents.AgentEvent
import com.forge.vdesign.agents.AgentMessage

import com.forge.vdesign.agents.ConversationalAgent
import com.forge.vdesign.agents.ForgeAgent
import com.forge.vdesign.agents.IntentArchitectAgent
import com.forge.vdesign.domain.model.ChatMessage
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.MessageRole
import com.forge.vdesign.domain.repository.AuthRepository
import com.forge.vdesign.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingToolApproval(
    val toolName: String,
    val arguments: Map<String, Any?>,
    val onDecision: (Boolean) -> Unit
)

/**
 * ChatUiState
 *
 * Single source of truth for the chat screen.
 * [persistedMessages] drives the RecyclerView — ONLY Room updates this.
 * [streamingContent] drives the animated streaming bubble.
 * [thinkingContent] drives the collapsible thinking bubble.
 * [statusLine] drives the small 1-line status under the streaming bubble.
 */
data class LiveAgentState(
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val thinkingContent: String? = null,
    val statusLine: String? = null,
    val pendingToolApproval: PendingToolApproval? = null
)

object AgentSessionTracker {
    val liveStates = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<LiveAgentState>>()
}

data class ChatUiState(
    val conversationId: String = "",
    val conversationTitle: String = "New Chat",
    val userDisplayName: String = "",
    val persistedMessages: List<ChatMessage> = emptyList(),
    val streamingContent: String = "",
    val isStreaming: Boolean = false,
    val isSending: Boolean = false,
    val thinkingContent: String? = null,
    val isThinkingExpanded: Boolean = false,
    val statusLine: String? = null,
    val error: String? = null,
    val isSignedOut: Boolean = false,
    val pendingToolApproval: PendingToolApproval? = null
)

/** Canvas launch event — kept for backward compat with ScreenCanvasActivity */
data class CanvasLaunchEvent(val brief: DesignBrief, val originPrompt: String)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val intentArchitectAgent: IntentArchitectAgent,
    private val conversationalAgent: ConversationalAgent,
    private val forgeAgent: ForgeAgent,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var autoApproveTools = false

    private val _canvasLaunchChannel = Channel<CanvasLaunchEvent>(Channel.BUFFERED)
    val canvasLaunchEvents = _canvasLaunchChannel.receiveAsFlow()

    /**
     * Full agent message history — the LLM sees all of this on every turn.
     * Includes: user messages, assistant responses WITH tool_calls, tool results.
     * This is what makes the agent "stay in context" across the whole conversation.
     */
    private val agentHistory = mutableListOf<AgentMessage>()

    private var messageObserverJob: Job? = null
    private var generationJob: Job? = null

    companion object {
        const val KEY_CONVERSATION_ID = "extra_conversation_id"
    }

    init {
        val user = authRepository.currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(isSignedOut = true)
        } else {
            val displayName = user.displayName ?: user.email ?: "Designer"
            _uiState.value = _uiState.value.copy(userDisplayName = displayName)
            
            // Try to load an existing intent ID if launched that way, else start fresh
            val initialId = savedStateHandle.get<String>(KEY_CONVERSATION_ID)
                ?: savedStateHandle.get<String>("conversationId")
            
            switchConversation(initialId)
        }
    }

    /**
     * Swaps the active conversation context. If id is null, it spins up a fresh chat.
     */
    fun switchConversation(id: String?) {
        val newId = id ?: java.util.UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(
            conversationId = newId,
            conversationTitle = "New Chat",
            persistedMessages = emptyList(),
            streamingContent = "",
            isStreaming = false,
            isSending = false,
            thinkingContent = null,
            statusLine = null,
            error = null
        )
        agentHistory.clear()
        messageObserverJob?.cancel()
        
        initConversation(newId)
    }

    private fun initConversation(conversationId: String) {
        viewModelScope.launch {
            // Restore agent history from persisted messages so LLM has full context
            restoreAgentHistory(conversationId)
            observeMessages(conversationId)
        }
        
        viewModelScope.launch {
            AgentSessionTracker.liveStates.getOrPut(conversationId) { MutableStateFlow(LiveAgentState()) }
                .collect { live ->
                    _uiState.value = _uiState.value.copy(
                        isSending = live.isSending,
                        isStreaming = live.isStreaming,
                        streamingContent = live.streamingContent,
                        thinkingContent = live.thinkingContent,
                        statusLine = live.statusLine,
                        pendingToolApproval = live.pendingToolApproval
                    )
                }
        }
    }

    /**
     * Rebuild agentHistory from Room on startup.
     *
     * We convert persisted ChatMessages back into AgentMessage.Text entries.
     * Screen cards and canvas cards are translated into assistant observations
     * so the LLM knows what was already generated.
     *
     * We do NOT re-add tool_calls (they can't be reconstructed from Room) — but
     * the assistant text already summarises what happened, so context is preserved.
     */
    private suspend fun restoreAgentHistory(conversationId: String) {
        try {
            val messages = chatRepository.getMessagesOnce(conversationId)
            agentHistory.clear()
            for (msg in messages) {
                when {
                    msg.isScreenCard -> {
                        // Tell the LLM what was already generated, adding a warning if rejected
                        val contentStr = if (msg.isRejected) {
                            "I generated the ${msg.content} screen, but the user REJECTED it. I should avoid using or expanding on this design."
                        } else {
                            "I generated the ${msg.content} screen. screenshotUrl=${msg.screenshotUrl ?: "unknown"}"
                        }
                        agentHistory.add(
                            AgentMessage.Text(
                                role    = "assistant",
                                content = contentStr
                            )
                        )
                    }
                    msg.isCanvasCard -> {
                        agentHistory.add(
                            AgentMessage.Text(
                                role    = "assistant",
                                content = "I compiled a design brief: ${msg.embeddedBrief?.projectName ?: msg.content}"
                            )
                        )
                    }
                    msg.role.apiValue == "user" -> {
                        agentHistory.add(AgentMessage.Text("user", msg.content))
                    }
                    msg.role.apiValue == "assistant" && msg.content.isNotBlank() -> {
                        agentHistory.add(AgentMessage.Text("assistant", msg.content))
                    }
                }
            }
            android.util.Log.d("ChatViewModel", "Restored ${agentHistory.size} history messages")
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "History restore failed: ${e.message}", e)
        }
    }

    private fun observeMessages(conversationId: String) {
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId)
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { messages ->
                    val processed = groupMessagesForTimeline(messages)
                    _uiState.value = _uiState.value.copy(persistedMessages = processed)
                    if (messages.isNotEmpty() && _uiState.value.conversationTitle == "New Chat") {
                        val firstMsg = messages.first().content.take(42)
                        _uiState.value = _uiState.value.copy(
                            conversationTitle = if (messages.first().content.length > 42) "$firstMsg…" else firstMsg
                        )
                    }
                }
        }
    }

    // ── Main send path ────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        val liveState = AgentSessionTracker.liveStates.getOrPut(_uiState.value.conversationId) { MutableStateFlow(LiveAgentState()) }
        if (text.isBlank() || liveState.value.isSending || liveState.value.isStreaming) return
        val conversationId = _uiState.value.conversationId
        if (conversationId.isEmpty()) return

        liveState.value = liveState.value.copy(isSending = true)
        _uiState.value = _uiState.value.copy(error = null)
        autoApproveTools = false

        val backgroundScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        generationJob?.cancel()
        generationJob = backgroundScope.launch {
            // 0. Lazily create the conversation in Room if this is the very first message
            chatRepository.getOrCreateConversation(conversationId)
            
            // 1. Persist user message immediately
            chatRepository.persistUserMessage(conversationId, text)

            // Fetch the active state from Room before generation
            val activeConv = chatRepository.observeAllConversations().first()
                .find { it.id == conversationId } 

            val manifestObj = try { org.json.JSONObject(activeConv?.projectManifest ?: "{}") } catch (e: Exception) { org.json.JSONObject() }
            val activeProjectId = manifestObj.optString("active_projectId", "")

            val workspaceContext = com.forge.vdesign.agents.WorkspaceContext(
                projectId = activeProjectId,
                designSystem = activeConv?.designSystem ?: "",
                generatedScreensManifest = activeConv?.projectManifest ?: "{}"
            )

            // 2. Collect agent events
            liveState.value = liveState.value.copy(isSending = false, isStreaming = true)
            var pendingSpeaks = StringBuilder()
            var pendingThoughts: String? = null

            val historyStrings = agentHistory.mapNotNull {
                if (it is AgentMessage.Text) it.role to it.content else null
            }
            
            // Count assistant turns to give IntentArchitect context on how deep we are
            val designTurns = historyStrings.count { it.first == "assistant" }
            
            // Parse intent deterministically
            liveState.value = liveState.value.copy(thinkingContent = "Analyzing request...")
            val intent = intentArchitectAgent.run(historyStrings, designTurns)
            liveState.value = liveState.value.copy(thinkingContent = null)

            // Route: if design intent is ready → ForgeAgent's full ReAct loop
            //        if needs clarification    → emit the question
            //        otherwise                 → casual conversation
            val eventFlow = if (intent.isReadyToGenerate) {
                // ForgeAgent takes full control — it will plan, generate, critique, revise, and publish
                forgeAgent.runLoop(
                    userMessage = text,
                    history     = agentHistory.toList(),
                    workspaceContext = workspaceContext
                )
            } else if (intent.clarificationQuestion != null) {
                // We need more info, ask the user
                kotlinx.coroutines.flow.flow { 
                    emit(AgentEvent.Speaks(intent.clarificationQuestion)) 
                }
            } else {
                // Not a design request, just chat
                conversationalAgent.chat(text, agentHistory.toList())
            }

            eventFlow
                .catch { e ->
                    android.util.Log.e("ChatViewModel", "Pipeline error", e)
                    liveState.value = LiveAgentState() // reset
                    val errMsg = "Something went wrong: ${e.message?.take(80)}"
                    chatRepository.insertAssistantMessage(conversationId, errMsg, pendingThoughts)
                    agentHistory.add(AgentMessage.Text("user", text))
                    agentHistory.add(AgentMessage.Text("assistant", errMsg))
                }
                .collect { event ->
                    when (event) {
                        is AgentEvent.Thinking -> {
                            liveState.value = liveState.value.copy(thinkingContent = event.content)
                            chatRepository.insertAgentLog(conversationId, "💭 Agent Thought", event.content)
                            agentHistory.add(AgentMessage.Text("assistant", "Thought: ${event.content}"))
                        }
                        is AgentEvent.AgentActivityLog -> {
                            chatRepository.insertAgentLog(conversationId, event.title, event.content)
                            agentHistory.add(AgentMessage.Text("assistant", "Action Log (${event.title}): ${event.content}"))
                        }
                        is AgentEvent.Speaks -> {
                            val cleaned = sanitizeSpokenContent(event.text)
                            if (cleaned.isBlank()) return@collect
                            // Do NOT null out thinkingContent. We want it visible while typing natively!
                            streamToUI(cleaned, liveState)
                            if (pendingSpeaks.isNotEmpty()) pendingSpeaks.append("\n\n")
                            pendingSpeaks.append(cleaned)
                        }
                        is AgentEvent.StatusLine -> {
                            liveState.value = liveState.value.copy(statusLine = event.text)
                        }
                        is AgentEvent.DesignSystemDefined -> {
                            liveState.value = liveState.value.copy(statusLine = null)
                            chatRepository.updateDesignSystem(conversationId, event.markdownStr)
                        }
                        is AgentEvent.ScreenReady -> {
                            liveState.value = liveState.value.copy(statusLine = null)
                            // Flush any pending spoken content first
                            if (pendingSpeaks.isNotEmpty()) {
                                val spokenSoFar = pendingSpeaks.toString()
                                chatRepository.insertAssistantMessage(conversationId, spokenSoFar, null)
                                agentHistory.add(AgentMessage.Text("assistant", spokenSoFar))
                                pendingSpeaks.clear()
                            }
                            chatRepository.insertScreenCard(
                                conversationId = conversationId,
                                screenName     = event.screenName,
                                screenshotUrl  = event.screenshotUrl,
                                htmlUrl        = event.htmlUrl,
                                projectId      = event.projectId,
                                designReasoning = event.designReasoning
                            )
                            // Record in agent history so future turns know what was generated
                            agentHistory.add(AgentMessage.Text("assistant", "✅ Screen '${event.screenName}' generated successfully. URL: ${event.htmlUrl}"))
                        }
                        is AgentEvent.WaitingForUser -> {
                            liveState.value = liveState.value.copy(statusLine = null, thinkingContent = null)
                            if (pendingSpeaks.isNotEmpty()) {
                                val finalText = pendingSpeaks.toString()
                                chatRepository.insertAssistantMessage(conversationId, finalText, null)
                                pendingSpeaks.clear()
                            }
                            agentHistory.add(AgentMessage.Text("user", text))
                            liveState.value = LiveAgentState()
                        }
                        is AgentEvent.LoopDone -> {
                            // Always record the user's message in agent history
                            agentHistory.add(AgentMessage.Text("user", text))
                            val finalText = when {
                                pendingSpeaks.isNotEmpty() -> pendingSpeaks.toString()
                                pendingThoughts != null    -> "I've finished processing your request."
                                else                       -> null
                            }
                            if (finalText != null) {
                                chatRepository.insertAssistantMessage(conversationId, finalText, pendingThoughts)
                                agentHistory.add(AgentMessage.Text("assistant", finalText))
                            }
                            pendingSpeaks.clear()
                            pendingThoughts = null
                            liveState.value = LiveAgentState()
                        }
                        is AgentEvent.Error -> {
                            liveState.value = LiveAgentState()
                            chatRepository.insertAssistantMessage(conversationId, event.message)
                            agentHistory.add(AgentMessage.Text("user", text))
                            agentHistory.add(AgentMessage.Text("assistant", event.message))
                        }
                        is AgentEvent.ToolApprovalNeeded -> {
                            if (autoApproveTools) {
                                event.onDecision(true)
                            } else {
                                liveState.value = liveState.value.copy(
                                    isStreaming = false, // Pause animation
                                    pendingToolApproval = PendingToolApproval(
                                        toolName = event.toolName,
                                        arguments = event.arguments,
                                        onDecision = { allowed ->
                                            // Resume streaming UI
                                            liveState.value = liveState.value.copy(
                                                isStreaming = true,
                                                pendingToolApproval = null
                                            )
                                            event.onDecision(allowed)
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            
            // Safety: ensure we're not stuck in streaming state
            liveState.value = LiveAgentState()
        }
    }
    
    fun submitToolApproval(isAllowed: Boolean) {
        val conversationId = _uiState.value.conversationId
        val liveState = AgentSessionTracker.liveStates[conversationId] ?: return
        val current = liveState.value
        current.pendingToolApproval?.onDecision?.invoke(isAllowed)
    }

    fun submitToolApprovalAll() {
        autoApproveTools = true
        submitToolApproval(true)
    }

    // ── Thinking bubble toggle ────────────────────────────────────────────────

    fun toggleThinkingExpanded() {
        _uiState.value = _uiState.value.copy(
            isThinkingExpanded = !_uiState.value.isThinkingExpanded
        )
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun stopGeneration() {
        generationJob?.cancel()
        val liveState = AgentSessionTracker.liveStates[_uiState.value.conversationId]
        liveState?.value = LiveAgentState() // reset sending/streaming state
    }

    fun rejectScreenCard(messageId: String) {
        viewModelScope.launch {
            chatRepository.rejectScreenCard(messageId)
            restoreAgentHistory(_uiState.value.conversationId)
        }
    }

    // ── Canvas launch (legacy) ────────────────────────────────────────────────

    fun openCanvas(brief: DesignBrief, prompt: String) {
        viewModelScope.launch {
            _canvasLaunchChannel.send(CanvasLaunchEvent(brief, prompt))
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = _uiState.value.copy(isSignedOut = true)
    }

    // ── Streaming helper ──────────────────────────────────────────────────────

    private suspend fun streamToUI(text: String, liveState: MutableStateFlow<LiveAgentState>) {
        liveState.value = liveState.value.copy(streamingContent = "", isStreaming = true)
        val words = text.split(" ")
        var built = ""
        words.forEachIndexed { i, word ->
            built += if (i == 0) word else " $word"
            liveState.value = liveState.value.copy(streamingContent = built)
            kotlinx.coroutines.delay(16L)
        }
        liveState.value = liveState.value.copy(streamingContent = "")
    }

    private fun clearLiveState() {
        _uiState.value = _uiState.value.copy(
            streamingContent   = "",
            isStreaming        = false,
            isSending          = false,
            thinkingContent    = null,
            statusLine         = null
        )
    }

    /**
     * Strip any raw XML-style tags that the LLM leaks into spoken content.
     *
     * Models sometimes emit `<observation>...</observation>`, `<think>...</think>`,
     * or `<workspace_state>` blocks as literal text instead of tool calls or
     * internal reasoning. We must intercept these before they reach the chat UI.
     *
     * Strategy:
     *  1. Remove full tag blocks (with content inside)
     *  2. Remove lone open/close tags
     *  3. Trim and collapse extra whitespace
     */
    private fun groupMessagesForTimeline(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return emptyList()

        val result = mutableListOf<ChatMessage>()
        var currentTimeline: MutableList<com.forge.vdesign.domain.model.AgentTask>? = null
        var lastConvoId = ""

        for (msg in messages) {
            if (msg.isAgentLog) {
                if (currentTimeline == null) {
                    currentTimeline = mutableListOf()
                }
                currentTimeline.add(
                    com.forge.vdesign.domain.model.AgentTask(
                        label = msg.agentLogTitle,
                        detail = msg.agentLogContent,
                        status = com.forge.vdesign.domain.model.TaskStatus.DONE
                    )
                )
                lastConvoId = msg.conversationId
            } else {
                if (currentTimeline != null) {
                    // Flush existing timeline
                    result.add(
                        ChatMessage(
                            id = "timeline_${messages.indexOf(msg)}",
                            conversationId = lastConvoId,
                            role = MessageRole.ASSISTANT,
                            content = "Activity Timeline",
                            agentTasks = currentTimeline.toList()
                        )
                    )
                    currentTimeline = null
                }
                result.add(msg)
            }
        }

        // Final flush
        if (currentTimeline != null) {
            result.add(
                ChatMessage(
                    id = "timeline_final",
                    conversationId = lastConvoId,
                    role = MessageRole.ASSISTANT,
                    content = "Activity Timeline",
                    agentTasks = currentTimeline.toList()
                )
            )
        }

        return result
    }

    private fun sanitizeSpokenContent(raw: String): String {
        val xmlTagsToStrip = listOf(
            "observation", "think", "workspace_state", "thinking",
            "reasoning", "plan", "internal"
        )
        var result = raw
        for (tag in xmlTagsToStrip) {
            // Strip full blocks: <tag>...</tag> (greedy, handles multiline)
            result = result.replace(Regex("<$tag>[\\s\\S]*?</$tag>", RegexOption.IGNORE_CASE), "")
            // Strip lone open/close tags
            result = result.replace(Regex("</?$tag\\s*/?>", RegexOption.IGNORE_CASE), "")
        }
        // Strip hallucinated "Action Log (Called: ...)" lines the LLM sometimes emits in spoken text
        result = result.replace(Regex("Action Log \\(Called:.*?\\)[^\\n]*\\n?", RegexOption.IGNORE_CASE), "")
        // Strip JSON code blocks that contain tool call patterns (hallucinated tool execution)
        result = result.replace(Regex("```(?:json)?\\s*\\{[\\s\\S]*?\"name\"\\s*:[\\s\\S]*?\\}\\s*```", RegexOption.IGNORE_CASE), "")
        // Collapse multiple blank lines to one
        result = result.replace(Regex("\\n{3,}"), "\n\n")
        return result.trim()
    }
}
