package com.forge.vdesign.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.vdesign.agents.AgentEvent
import com.forge.vdesign.agents.AgentMessage
import com.forge.vdesign.agents.ForgeAgent
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val statusLine: String? = null
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
    val isSignedOut: Boolean = false
)

/** Canvas launch event — kept for backward compat with ScreenCanvasActivity */
data class CanvasLaunchEvent(val brief: DesignBrief, val originPrompt: String)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val forgeAgent: ForgeAgent,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _canvasLaunchChannel = Channel<CanvasLaunchEvent>(Channel.BUFFERED)
    val canvasLaunchEvents = _canvasLaunchChannel.receiveAsFlow()

    /**
     * Full agent message history — the LLM sees all of this on every turn.
     * Includes: user messages, assistant responses WITH tool_calls, tool results.
     * This is what makes the agent "stay in context" across the whole conversation.
     */
    private val agentHistory = mutableListOf<AgentMessage>()

    private var messageObserverJob: Job? = null

    companion object {
        const val KEY_CONVERSATION_ID = "extra_conversation_id"
    }

    init {
        val user = authRepository.currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(isSignedOut = true)
        } else {
            val conversationId = savedStateHandle.get<String>(KEY_CONVERSATION_ID)
                ?: savedStateHandle.get<String>("conversationId")

            if (conversationId.isNullOrBlank()) {
                android.util.Log.e("ChatViewModel", "No conversationId. Keys: ${savedStateHandle.keys()}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to open chat session. Please go back and try again."
                )
            } else {
                val displayName = user.displayName ?: user.email ?: "Designer"
                _uiState.value = _uiState.value.copy(
                    conversationId  = conversationId,
                    userDisplayName = displayName
                )
                initConversation(conversationId)
            }
        }
    }

    private fun initConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getOrCreateConversation(conversationId)
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
                        statusLine = live.statusLine
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
                        // Tell the LLM what was already generated
                        agentHistory.add(
                            AgentMessage.Text(
                                role    = "assistant",
                                content = "I generated the ${msg.content} screen. screenshotUrl=${msg.screenshotUrl ?: "unknown"}"
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
                    _uiState.value = _uiState.value.copy(persistedMessages = messages)
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

        val backgroundScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        backgroundScope.launch {
            // 1. Persist user message immediately
            chatRepository.persistUserMessage(conversationId, text)

            // 2. Collect agent events
            liveState.value = liveState.value.copy(isSending = false, isStreaming = true)
            var pendingSpeaks = StringBuilder()

            forgeAgent.runLoop(
                userMessage = text,
                history     = agentHistory.toList()
            )
                .catch { e ->
                    android.util.Log.e("ChatViewModel", "Agent loop error", e)
                    liveState.value = LiveAgentState() // reset
                    val errMsg = "Something went wrong: ${e.message?.take(80)}"
                    chatRepository.insertAssistantMessage(conversationId, errMsg)
                    agentHistory.add(AgentMessage.Text("user", text))
                    agentHistory.add(AgentMessage.Text("assistant", errMsg))
                }
                .collect { event ->
                    when (event) {
                        is AgentEvent.Thinking -> {
                            liveState.value = liveState.value.copy(thinkingContent = event.content)
                        }
                        is AgentEvent.Speaks -> {
                            liveState.value = liveState.value.copy(thinkingContent = null)
                            streamToUI(event.text, liveState)
                            if (pendingSpeaks.isNotEmpty()) pendingSpeaks.append("\n\n")
                            pendingSpeaks.append(event.text)
                        }
                        is AgentEvent.StatusLine -> {
                            liveState.value = liveState.value.copy(statusLine = event.text)
                        }
                        is AgentEvent.ScreenReady -> {
                            liveState.value = liveState.value.copy(statusLine = null)
                            if (pendingSpeaks.isNotEmpty()) {
                                chatRepository.insertAssistantMessage(conversationId, pendingSpeaks.toString())
                                pendingSpeaks.clear()
                            }
                            chatRepository.insertScreenCard(
                                conversationId = conversationId,
                                screenName     = event.screenName,
                                screenshotUrl  = event.screenshotUrl,
                                htmlUrl        = event.htmlUrl,
                                projectId      = event.projectId
                            )
                        }
                        is AgentEvent.WaitingForUser -> {
                            liveState.value = liveState.value.copy(statusLine = null, thinkingContent = null)
                            if (pendingSpeaks.isNotEmpty()) {
                                chatRepository.insertAssistantMessage(conversationId, pendingSpeaks.toString())
                                pendingSpeaks.clear()
                            }
                            agentHistory.add(AgentMessage.Text("user", text))
                            liveState.value = LiveAgentState()
                        }
                        is AgentEvent.LoopDone -> {
                            if (pendingSpeaks.isNotEmpty()) {
                                chatRepository.insertAssistantMessage(conversationId, pendingSpeaks.toString())
                                agentHistory.add(AgentMessage.Text("user", text))
                                agentHistory.add(AgentMessage.Text("assistant", pendingSpeaks.toString()))
                                pendingSpeaks.clear()
                            } else {
                                agentHistory.add(AgentMessage.Text("user", text))
                            }
                            liveState.value = LiveAgentState()
                        }
                        is AgentEvent.Error -> {
                            liveState.value = LiveAgentState()
                            chatRepository.insertAssistantMessage(conversationId, event.message)
                            agentHistory.add(AgentMessage.Text("user", text))
                            agentHistory.add(AgentMessage.Text("assistant", event.message))
                        }
                    }
                }
            
            // Safety: ensure we're not stuck in streaming state
            liveState.value = LiveAgentState()
        }
    }

    // ── Thinking bubble toggle ────────────────────────────────────────────────

    fun toggleThinkingExpanded() {
        _uiState.value = _uiState.value.copy(
            isThinkingExpanded = !_uiState.value.isThinkingExpanded
        )
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
}
