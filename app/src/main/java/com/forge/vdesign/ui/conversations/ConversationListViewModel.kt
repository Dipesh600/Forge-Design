package com.forge.vdesign.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.vdesign.domain.model.Conversation
import com.forge.vdesign.domain.model.ForgeResult
import com.forge.vdesign.domain.repository.AuthRepository
import com.forge.vdesign.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ConversationListState(
    val conversations: List<Conversation> = emptyList(),
    val userDisplayName: String = "",
    val isLoading: Boolean = true,
    val isSignedOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationListState())
    val state: StateFlow<ConversationListState> = _state.asStateFlow()

    init {
        val user = authRepository.currentUser
        if (user == null) {
            _state.value = _state.value.copy(isSignedOut = true)
        } else {
            _state.value = _state.value.copy(
                userDisplayName = user.displayName ?: user.email ?: "Designer"
            )
            observeConversations()
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.observeAllConversations()
                .catch { e -> _state.value = _state.value.copy(error = e.message) }
                .collect { list ->
                    _state.value = _state.value.copy(conversations = list, isLoading = false)
                }
        }
    }

    /** Creates a new conversation and returns its ID */
    fun createNewConversation(): String {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch {
            chatRepository.getOrCreateConversation(newId)
        }
        return newId
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameConversation(conversationId, newTitle)
        }
    }

    fun starConversation(conversationId: String, starred: Boolean) {
        viewModelScope.launch {
            chatRepository.starConversation(conversationId, starred)
        }
    }

    fun signOut() {
        authRepository.signOut()
        _state.value = _state.value.copy(isSignedOut = true)
    }
}
