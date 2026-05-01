package com.forge.vdesign.agents

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CodeSurgeonAgent
 *
 * Acts as a "prompt pre-processor" to translate natural language user requests
 * into atomic, high-precision technical instructions for Stitch, minimizing edit failures.
 */
@Singleton
class CodeSurgeonAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient
) {
    suspend fun enrichEditInstruction(
        userPrompt: String,
        screenDescription: String?
    ): String {
        val systemPrompt = """
            You are CodeSurgeon, an expert prompt engineer for the Stitch UI generation engine.
            Your job is to translate vague or broad user edit requests into precise, atomic, and deterministic technical instructions.
            
            Stitch edit prompts fail when they are ambiguous. You must:
            1. Convert vague nouns into concrete UI components (e.g., "make it pop" -> "increase saturation of primary buttons").
            2. Specify exact layout changes (e.g., "move to top" -> "anchor element to top edge with 16dp padding").
            3. Maintain the original design intent.
            
            Output ONLY the enriched, precise technical instruction. Do not include conversational text, markdown formatting, or explanations.
        """.trimIndent()

        val userMessage = buildString {
            appendLine("Original Request: $userPrompt")
            if (!screenDescription.isNullOrBlank()) {
                appendLine("Current Screen Context: $screenDescription")
            }
        }

        return try {
            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(
                    MiniMaxMessage("system", systemPrompt),
                    MiniMaxMessage("user", userMessage)
                ))
            )
            val content = response.choices?.firstOrNull()?.message?.content?.trim()
            if (!content.isNullOrBlank()) content else userPrompt
        } catch (e: Exception) {
            userPrompt
        }
    }
}
