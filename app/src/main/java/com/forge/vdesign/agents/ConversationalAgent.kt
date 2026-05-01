package com.forge.vdesign.agents

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A lightweight agent for handling casual conversation when the user
 * doesn't want to design anything (e.g. "Hello", "How are you?").
 * 
 * It runs completely deterministically (no ReAct tool loop) and 
 * emits AgentEvents so it plugs perfectly into ChatViewModel.
 */
@Singleton
class ConversationalAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val gson: Gson
) {
    private val systemPrompt = """
        You are FORGE, an elite AI Design Studio.
        The user is currently just chatting with you. You are helpful, polite, and brief.
        If they want to design an Android app, tell them you are ready to help them build screens.
    """.trimIndent()

    fun chat(userMessage: String, history: List<AgentMessage>): Flow<AgentEvent> = flow {
        emit(AgentEvent.Thinking("Thinking..."))
        
        try {
            val apiHistory = mutableListOf<MiniMaxMessage>()
            apiHistory.add(MiniMaxMessage("system", systemPrompt))
            
            history.forEach { msg ->
                when (msg) {
                    is AgentMessage.Text -> apiHistory.add(MiniMaxMessage(msg.role, msg.content))
                    is AgentMessage.AssistantWithTools -> apiHistory.add(MiniMaxMessage("assistant", msg.content ?: ""))
                    is AgentMessage.ToolResult -> apiHistory.add(MiniMaxMessage("user", msg.content))
                }
            }
            apiHistory.add(MiniMaxMessage("user", userMessage))

            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = apiHistory)
            )

            val text = response.choices?.firstOrNull()?.message?.content ?: "I'm sorry, I encountered an error parsing that."
            
            // Just emit the text
            emit(AgentEvent.Speaks(text))
        } catch (e: Exception) {
            emit(AgentEvent.Speaks("Sorry, I had trouble processing that: ${e.message}"))
        }
    }
}
