package com.forge.vdesign.data.repository

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.forge.vdesign.data.local.ConversationDao
import com.forge.vdesign.data.local.ConversationEntity
import com.forge.vdesign.data.local.MessageDao
import com.forge.vdesign.data.local.MessageEntity
import com.forge.vdesign.domain.model.ChatMessage
import com.forge.vdesign.domain.model.Conversation
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.ForgeException
import com.forge.vdesign.domain.model.ForgeResult
import com.forge.vdesign.domain.model.MessageRole
import com.forge.vdesign.domain.repository.AuthRepository
import com.forge.vdesign.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject

/**
 * FORGE system persona — injected as system message on every MiniMax call.
 * MiniMax becomes FORGE: an elite AI design consultant, not a generic chatbot.
 */
private const val FORGE_SYSTEM_PROMPT = """You are FORGE, an elite AI design consultant built into the FORGE AI Design Studio — an Android app that generates real mobile UI screens using AI.

Your personality:
- You speak like a senior product designer: thoughtful, precise, opinionated, and inspiring
- You never give generic answers — every response is grounded in real design principles
- You're direct. You ask exactly the right follow-up question, not a list of five
- You think in screens, flows, and design systems — not isolated components

CRITICAL IDENTITY RULE:
- You are NOT a general-purpose AI. You are FORGE — a design studio that GENERATES real screens.
- NEVER say "I can't generate visual designs" or "I'm just an AI" — FORGE literally generates screens.
- However, since you are currently in the chat interface, DO NOT announce that you are opening the Canvas or generating screens yourself. You lack the system capability to open the Canvas from standard chat.
- If the user wants to see the screens/designs/UI right now, tell them to explicitly type: "Generate the screens" to engage the FORGE Native Engine.

When a user wants to design something:
1. Ask ONE focused question to clarify: app category, key screens, or visual style
2. Let the user answer. If they are ready to see designs, tell them to reply with "Generate the screens".

For design questions and feedback, answer as a world-class design consultant.
Use markdown lightly. Keep responses concise and punchy — no walls of text.
Never break character. You are FORGE, not ChatGPT or MiniMax."""

    private fun injectSpatialMemory(basePrompt: String, manifest: String): String {
        if (manifest == "{}" || manifest.isEmpty()) return basePrompt
        return "$basePrompt\n\n### SPATIAL MEMORY (PROJECT MANIFEST) ###\nYou have live access to the current project's generated screens. DO NOT suggest to generate a screen that already exists in this manifest unless the user explicitly wants to redesign/replace it. Use these IDs when referencing specific screens.\nMANIFEST:\n$manifest"
    }

class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val miniMaxClient: MiniMaxClient,
    private val authRepository: AuthRepository,
    private val gson: Gson
) : ChatRepository {

    /**
     * Stream a response from FORGE for the given user message.
     *
     * Flow contract:
     *  - Emits String tokens one by one for live UI update
     *  - Caller is responsible for showing streaming state in UI
     *  - This function persists BOTH the user message AND the full AI response to Room
     *  - Room Flow then fires automatically → UI updates from Room (single source of truth)
     *
     * NOTE: We do NOT return the user message optimistically. We insert it to Room first,
     * then Room's Flow emits — ensuring no duplicates or disappearing messages.
     */
    override fun sendMessageStream(
        conversationId: String,
        userMessage: String
    ): Flow<String> = kotlinx.coroutines.flow.flow {
        // 1. Persist user message → Room Flow fires → UI shows it
        val userEntity = MessageEntity(
            conversationId = conversationId,
            role           = MessageRole.USER.apiValue,
            content        = userMessage
        )
        messageDao.insert(userEntity)

        // 2. Auto-title conversation from first user message
        autoTitleConversation(conversationId, userMessage)

        // 3. Build conversation context (last 12 messages, in correct order)
        val history = messageDao.loadRecentSync(conversationId, limit = 12)
        val conversationEntity = conversationDao.loadById(conversationId)
        val manifest = conversationEntity?.projectManifest ?: "{}"
        
        val messages = mutableListOf(MiniMaxMessage("system", injectSpatialMemory(FORGE_SYSTEM_PROMPT, manifest)))
        history.forEach { messages.add(MiniMaxMessage(it.role.lowercase(), it.content)) }

        // 4. Stream tokens
        var fullResponse = ""
        miniMaxClient.chatCompletionStream(
            MiniMaxRequest(messages = messages)
        ).collect { token ->
            fullResponse += token
            emit(token)
        }

        // 5. Persist full AI response → Room Flow fires → UI updates
        if (fullResponse.isNotBlank()) {
            messageDao.insert(MessageEntity(
                conversationId = conversationId,
                role           = MessageRole.ASSISTANT.apiValue,
                content        = fullResponse
            ))
        }

        // 6. Bump conversation updatedAt
        conversationDao.loadById(conversationId)?.let { existing ->
            conversationDao.update(existing.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /** Blocking send — used by agent pipeline calls (not chat UI) */
    override suspend fun sendMessage(
        conversationId: String,
        userMessage: String
    ): ForgeResult<ChatMessage> {
        return try {
            messageDao.insert(MessageEntity(
                conversationId = conversationId,
                role = MessageRole.USER.apiValue,
                content = userMessage
            ))
            autoTitleConversation(conversationId, userMessage)

            val history = messageDao.loadRecentSync(conversationId, limit = 12)
            val conversationEntity = conversationDao.loadById(conversationId)
            val manifest = conversationEntity?.projectManifest ?: "{}"

            val messages = mutableListOf(MiniMaxMessage("system", injectSpatialMemory(FORGE_SYSTEM_PROMPT, manifest)))
            history.forEach { messages.add(MiniMaxMessage(it.role.lowercase(), it.content)) }

            val response = miniMaxClient.chatCompletion(MiniMaxRequest(messages = messages))
            val replyText = response.choices?.firstOrNull()?.message?.content
                ?: "I couldn't generate a response. Please try again."

            val aiEntity = MessageEntity(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT.apiValue,
                content = replyText
            )
            messageDao.insert(aiEntity)
            conversationDao.loadById(conversationId)?.let {
                conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
            }
            ForgeResult.Success(aiEntity.toDomainModel())
        } catch (e: Exception) {
            android.util.Log.e("FORGE_CHAT", "sendMessage failed", e)
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Unknown error", e))
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.loadByConversation(conversationId).map { it.map { e -> e.toDomainModel() } }

    override suspend fun getMessagesOnce(conversationId: String): List<ChatMessage> =
        messageDao.loadRecentSync(conversationId, limit = 200).map { it.toDomainModel() }

    override suspend fun getOrCreateConversation(conversationId: String): ForgeResult<Conversation> {
        return try {
            val uid = authRepository.currentUser?.uid ?: ""
            var entity = conversationDao.loadById(conversationId)
            if (entity == null) {
                entity = ConversationEntity(id = conversationId, title = "New Chat", userId = uid)
                conversationDao.insert(entity)
            }
            ForgeResult.Success(entity.toDomainModel())
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    override fun observeAllConversations(): Flow<List<Conversation>> {
        val uid = authRepository.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return conversationDao.loadAll(uid).map { it.map { e -> e.toDomainModel() } }
    }

    override suspend fun deleteConversation(conversationId: String): ForgeResult<Unit> {
        return try {
            messageDao.deleteByConversation(conversationId)
            conversationDao.deleteById(conversationId)
            ForgeResult.Success(Unit)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    override suspend fun persistUserMessage(conversationId: String, content: String): ForgeResult<ChatMessage> {
        return try {
            val entity = MessageEntity(
                conversationId = conversationId,
                role = MessageRole.USER.apiValue,
                content = content
            )
            messageDao.insert(entity)
            autoTitleConversation(conversationId, content)
            ForgeResult.Success(entity.toDomainModel())
        } catch (e: Exception) {
            android.util.Log.e("FORGE_CHAT", "persistUserMessage failed", e)
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    override suspend fun insertAssistantMessage(conversationId: String, content: String, thinkingContent: String?): ForgeResult<ChatMessage> {
        return try {
            val metadataJson = if (thinkingContent != null) {
                JSONObject().apply {
                    put("type", "thinking")
                    put("thinking_content", thinkingContent)
                }.toString()
            } else ""

            val aiEntity = MessageEntity(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT.apiValue,
                content = content,
                metadata = metadataJson
            )
            messageDao.insert(aiEntity)
            conversationDao.loadById(conversationId)?.let {
                conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
            }
            ForgeResult.Success(aiEntity.toDomainModel())
        } catch (e: Exception) {
            android.util.Log.e("FORGE_CHAT", "insertAssistantMessage failed", e)
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    /**
     * Persist a canvas card message to Room.
     * The DesignBrief is serialized to JSON and stored in the metadata column.
     * When this message is later loaded by Room and mapped to a domain model,
     * the brief is deserialized and isCanvasCard is set to true automatically.
     */
    override suspend fun insertCanvasCard(
        conversationId: String,
        brief: DesignBrief,
        displayContent: String
    ): ForgeResult<ChatMessage> {
        return try {
            val metadata = JSONObject().apply {
                put("type", "canvas_card")
                put("brief", JSONObject(gson.toJson(brief)))
            }.toString()

            val entity = MessageEntity(
                conversationId = conversationId,
                role     = MessageRole.ASSISTANT.apiValue,
                content  = displayContent,
                metadata = metadata
            )
            messageDao.insert(entity)
            conversationDao.loadById(conversationId)?.let {
                conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
            }
            ForgeResult.Success(entity.toDomainModel())
        } catch (e: Exception) {
            android.util.Log.e("FORGE_CHAT", "insertCanvasCard failed", e)
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    /**
     * Persist a screen result card — stores screenshotUrl, htmlUrl, projectId in metadata JSON.
     * The mapper reads this and populates isScreenCard=true in the domain model.
     */
    override suspend fun insertScreenCard(
        conversationId: String,
        screenName: String,
        screenshotUrl: String?,
        htmlUrl: String?,
        projectId: String?,
        designReasoning: Map<String, String>?
    ): ForgeResult<ChatMessage> {
        return try {
            val metadata = JSONObject().apply {
                put("type", "screen_card")
                put("screenName", screenName)
                screenshotUrl?.let { put("screenshotUrl", it) }
                htmlUrl?.let { put("htmlUrl", it) }
                projectId?.let { put("projectId", it) }
                designReasoning?.let { put("designReasoning", JSONObject(it)) }
            }.toString()

            val entity = MessageEntity(
                conversationId = conversationId,
                role           = MessageRole.ASSISTANT.apiValue,
                content        = screenName,
                metadata       = metadata
            )
            messageDao.insert(entity)
            conversationDao.loadById(conversationId)?.let {
                val currentManifest = try { JSONObject(it.projectManifest) } catch (e: Exception) { JSONObject() }
                currentManifest.put("active_projectId", projectId ?: "")
                val screensArray = currentManifest.optJSONArray("generated_screens") ?: org.json.JSONArray()
                
                val newScreen = JSONObject().apply {
                    put("name", screenName)
                    put("id", "s_${screensArray.length() + 1}_${projectId ?: ""}")
                }
                screensArray.put(newScreen)
                currentManifest.put("generated_screens", screensArray)

                conversationDao.update(it.copy(
                    updatedAt = System.currentTimeMillis(),
                    projectManifest = currentManifest.toString(),
                    screenCount = it.screenCount + 1,
                    thumbnailUrl = it.thumbnailUrl ?: screenshotUrl
                ))
            }
            ForgeResult.Success(entity.toDomainModel())
        } catch (e: Exception) {
            android.util.Log.e("FORGE_CHAT", "insertScreenCard failed", e)
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Auto-sets the conversation title to the first ~40 chars of the first user message.
     * Only sets it if the title is still "New Chat" (default).
     */
    private suspend fun autoTitleConversation(conversationId: String, firstMessage: String) {
        try {
            val entity = conversationDao.loadById(conversationId)
            if (entity != null && (entity.title == "New Chat" || entity.title.isEmpty())) {
                val shortText = firstMessage.take(40).replace("\n", " ").trim()
                val safeTitle = if (shortText.length == 40) "$shortText..." else shortText
                val uid = authRepository.currentUser?.uid ?: ""
                conversationDao.update(entity.copy(title = safeTitle, userId = uid))
            }
        } catch (e: Exception) {
            android.util.Log.w("FORGE_CHAT", "Auto-title failed: ${e.message}")
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /**
     * Maps MessageEntity → ChatMessage domain model.
     *
     * For canvas card messages (metadata contains type=canvas_card), we deserialize
     * the embedded DesignBrief JSON so the UI can render a proper "Open Canvas →" card
     * with the real brief attached — no fallback brief, no extra data source.
     */
    private fun MessageEntity.toDomainModel(): ChatMessage {
        var isCanvasCard = false
        var canvasPrompt = ""
        var embeddedBrief: DesignBrief? = null
        var isScreenCard = false
        var screenshotUrl: String? = null
        var htmlUrl: String? = null
        var screenCardProjectId: String? = null
        var isRejected = false
        val designDecisions = mutableListOf<com.forge.vdesign.domain.model.DesignDecision>()

        var thinkingContent: String? = null
        var isAgentLog = false
        var agentLogTitle = ""

        if (metadata.isNotBlank()) {
            runCatching {
                val json = JSONObject(metadata)
                when (json.optString("type")) {
                    "thinking" -> {
                        thinkingContent = json.optString("thinking_content").takeIf { it.isNotBlank() }
                    }
                    "canvas_card" -> {
                        isCanvasCard = true
                        val briefJson = json.optJSONObject("brief")
                        if (briefJson != null) {
                            embeddedBrief = gson.fromJson(briefJson.toString(), DesignBrief::class.java)
                            canvasPrompt = embeddedBrief?.let { b ->
                                "${b.projectName} · ${b.plannedScreens.joinToString(" · ")} · ${b.mood}"
                            } ?: content
                        }
                    }
                    "screen_card" -> {
                        isScreenCard = true
                        screenshotUrl = json.optString("screenshotUrl").takeIf { it.isNotBlank() }
                        htmlUrl = json.optString("htmlUrl").takeIf { it.isNotBlank() }
                        screenCardProjectId = json.optString("projectId").takeIf { it.isNotBlank() }
                        isRejected = json.optBoolean("isRejected", false)
                        
                        val reasoningObj = json.optJSONObject("designReasoning")
                        reasoningObj?.keys()?.forEach { key ->
                            designDecisions.add(
                                com.forge.vdesign.domain.model.DesignDecision(
                                    decision = key,
                                    principle = reasoningObj.getString(key),
                                    sourceBook = "Forge Agent",
                                    skillId = "agent"
                                )
                            )
                        }
                    }
                    "agent_log" -> {
                        isAgentLog = true
                        agentLogTitle = json.optString("title", "Agent Activity")
                    }
                }
            }.onFailure {
                android.util.Log.w("FORGE_CHAT", "Failed to parse message metadata: ${it.message}")
            }
        }

        // --- Retroactive Mapping for Legacy Messages ---
        var finalIsAgentLog = isAgentLog
        var finalAgentLogTitle = agentLogTitle
        
        if (!finalIsAgentLog && role == MessageRole.ASSISTANT.apiValue) {
            if (content.startsWith("Called: ") || content.startsWith("tool_call:") || (content.startsWith("{") && content.contains("tool_call"))) {
                finalIsAgentLog = true
                finalAgentLogTitle = if (content.contains("Called: ")) {
                    content.substringBefore("(").replace("Called: ", "Tool Execute")
                } else {
                    "Agent Action"
                }
            }
        }

        return ChatMessage(
            id                  = id,
            conversationId      = conversationId,
            role                = MessageRole.fromApiValue(role),
            content             = content,
            thinkingContent     = thinkingContent,
            timestamp           = timestamp,
            tokenCount          = tokenCount,
            designReasoning     = designDecisions,
            isCanvasCard        = isCanvasCard,
            canvasPrompt        = canvasPrompt,
            embeddedBrief       = embeddedBrief,
            isScreenCard        = isScreenCard,
            screenshotUrl       = screenshotUrl,
            htmlUrl             = htmlUrl,
            screenCardProjectId = screenCardProjectId,
            isAgentLog          = finalIsAgentLog,
            agentLogTitle       = finalAgentLogTitle,
            agentLogContent     = content,
            isRejected          = isRejected
        )
    }

    private fun ConversationEntity.toDomainModel() = Conversation(
        id          = id,
        title       = title,
        createdAt   = createdAt,
        updatedAt   = updatedAt,
        totalTokens = totalTokens,
        messageCount = 0,
        isStarred   = isStarred,
        designSystem = designSystem,
        projectManifest = projectManifest,
        screenCount = screenCount,
        thumbnailUrl = thumbnailUrl
    )

    override suspend fun insertAgentLog(
        conversationId: String,
        title: String,
        content: String
    ): ForgeResult<ChatMessage> {
        val metaJson = JSONObject().apply {
            put("type", "agent_log")
            put("title", title)
        }
        val msg = MessageEntity(
            conversationId = conversationId,
            role = "assistant",
            content = content,
            metadata = metaJson.toString()
        )
        return try {
            messageDao.insert(msg)
            ForgeResult.Success(msg.toDomainModel())
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException("Failed to insert log", e))
        }
    }

    override suspend fun updateDesignSystem(conversationId: String, designSystem: String): ForgeResult<Unit> {
        return try {
            conversationDao.updateDesignSystem(conversationId, designSystem, System.currentTimeMillis())
            ForgeResult.Success(Unit)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException("Failed to update design system", e))
        }
    }

    override suspend fun rejectScreenCard(messageId: String): ForgeResult<Unit> {
        return try {
            val entity = messageDao.getById(messageId) ?: throw Exception("Message not found")
            val json = JSONObject(entity.metadata)
            json.put("isRejected", true)
            messageDao.insert(entity.copy(metadata = json.toString()))
            ForgeResult.Success(Unit)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException("Failed to reject screen card", e))
        }
    }

    override suspend fun renameConversation(conversationId: String, newTitle: String): ForgeResult<Unit> {
        return try {
            conversationDao.updateTitle(conversationId, newTitle.trim(), System.currentTimeMillis())
            ForgeResult.Success(Unit)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException(e.message ?: "Error", e))
        }
    }

    override suspend fun starConversation(conversationId: String, starred: Boolean): ForgeResult<Unit> {
        return try {
            conversationDao.updateStarred(conversationId, starred)
            ForgeResult.Success(Unit)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException("Failed to star", e))
        }
    }

    override suspend fun syncProjectMetadata(conversationId: String): ForgeResult<Unit> {
        return try {
            val messages = getMessagesOnce(conversationId)
            val screenCards = messages.filter { it.isScreenCard && !it.isRejected }
            val count = screenCards.size
            val thumb = screenCards.firstOrNull { !it.screenshotUrl.isNullOrBlank() }?.screenshotUrl

            conversationDao.loadById(conversationId)?.let { convo ->
                conversationDao.update(convo.copy(
                    screenCount = count,
                    thumbnailUrl = convo.thumbnailUrl ?: thumb
                ))
            }
            ForgeResult.Success(Unit)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.UnknownException("Sync failed", e))
        }
    }
}
