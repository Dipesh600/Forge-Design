package com.forge.vdesign.domain.repository

import com.forge.vdesign.domain.model.ChatMessage
import com.forge.vdesign.domain.model.Conversation
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.ForgeResult
import kotlinx.coroutines.flow.Flow

/**
 * Chat repository interface — the boundary between the domain and data layers.
 */
interface ChatRepository {

    /** Stream FORGE response token by token for the live chat UI. */
    fun sendMessageStream(conversationId: String, userMessage: String): Flow<String>

    /** Blocking send — for non-streaming agent-internal calls. */
    suspend fun sendMessage(conversationId: String, userMessage: String): ForgeResult<ChatMessage>

    /** Observe all messages for a conversation, newest last. */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /** One-shot load of all messages — used for restoring agent history on startup. */
    suspend fun getMessagesOnce(conversationId: String): List<ChatMessage>

    suspend fun getOrCreateConversation(conversationId: String): ForgeResult<Conversation>

    fun observeAllConversations(): Flow<List<Conversation>>

    suspend fun deleteConversation(conversationId: String): ForgeResult<Unit>

    /**
     * Persist ONLY the user message to Room — no MiniMax call.
     * Used in the ForgeAgent path where the LLM controls the response.
     */
    suspend fun persistUserMessage(conversationId: String, content: String): ForgeResult<ChatMessage>

    /**
     * Persist a plain assistant message — no MiniMax call.
     * Used for clarification questions and chat responses after streaming completes.
     * Stores optional thinkingContent into the metadata column so reasoning survives restarts.
     */
    suspend fun insertAssistantMessage(conversationId: String, content: String, thinkingContent: String? = null): ForgeResult<ChatMessage>

    /**
     * Persist a canvas card message to Room.
     *
     * The DesignBrief is serialized to JSON in the [MessageEntity.metadata] column.
     * When Room delivers this via [observeMessages], the mapper deserializes the brief
     * and populates [ChatMessage.isCanvasCard] = true and [ChatMessage.embeddedBrief].
     *
     * The UI renders an "Open Canvas →" card. Navigation only happens on tap.
     * No auto-launch, no in-memory channel hacks.
     */
    suspend fun insertCanvasCard(
        conversationId: String,
        brief: DesignBrief,
        displayContent: String
    ): ForgeResult<ChatMessage>

    /**
     * Persist a screen result card — shown inline in chat after the agent generates a screen.
     * Stores screenshotUrl, htmlUrl, and projectId in the metadata JSON column.
     */
    suspend fun insertScreenCard(
        conversationId: String,
        screenName: String,
        screenshotUrl: String?,
        htmlUrl: String?,
        projectId: String?,
        designReasoning: Map<String, String>? = null
    ): ForgeResult<ChatMessage>

    suspend fun updateDesignSystem(conversationId: String, designSystem: String): ForgeResult<Unit>

    /** Mark a screen card as rejected, hiding it from the workspace. */
    suspend fun rejectScreenCard(messageId: String): ForgeResult<Unit>

    /**
     * Persists an agent activity log (e.g. tool call, tool result) for the Chain of Thought timeline.
     */
    suspend fun insertAgentLog(
        conversationId: String,
        title: String,
        content: String
    ): ForgeResult<ChatMessage>

    suspend fun renameConversation(conversationId: String, newTitle: String): ForgeResult<Unit>

    suspend fun starConversation(conversationId: String, starred: Boolean): ForgeResult<Unit>
    
    /** 
     * Retroactively syncs project metadata (screen count, thumbnail) 
     * by scanning historical messages. 
     */
    suspend fun syncProjectMetadata(conversationId: String): ForgeResult<Unit>
}
