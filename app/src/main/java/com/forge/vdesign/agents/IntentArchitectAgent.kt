package com.forge.vdesign.agents

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.forge.vdesign.domain.model.DesignBrief
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IntentArchitectAgent
 *
 * The first agent in the FORGE pipeline. Acts as the consultative layer:
 *
 * - Turn 0: User expresses a vague design idea
 * - If context is thin, returns a focused clarifying question (max 2 questions)
 * - Once enough context is gathered, produces a full DesignBrief with:
 *     - plannedScreens: which screens to generate (3-5 typically)
 *     - projectName: a branded name for the Stitch project
 *     - mood, constraints, screenType, userGoal
 *
 * This agent makes FORGE feel like a design consultant, not a command executor.
 */
@Singleton
class IntentArchitectAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val gson: Gson
) {

    companion object {
        /**
         * Action verbs that signal the user WANTS to design something (not just discuss it).
         * "design", "create", "build" → generation intent
         * "What makes a great X?" → conversational, NO action verb → falls to regular chat
         */
        private val ACTION_VERBS = listOf(
            "design", "create", "make", "build", "generate", "develop",
            "i want", "i need", "i'd like", "id like",
            "can you make", "can you design", "can you build", "can you create",
            "let's design", "lets design", "let's build", "lets build",
            "help me design", "help me build", "help me create",
            "show me a", "draw me", "prototype", "wire", "mockup"
        )

        /**
         * Design nouns — only checked when an ACTION_VERB is also present.
         * Prevents pure questions ("What makes a great dashboard?") from triggering generation.
         */
        private val DESIGN_NOUNS = listOf(
            "screen", "screens", "page", "pages", "ui", "app", "login", "home",
            "dashboard", "profile", "settings", "onboarding", "signup", "register",
            "interface", "flow", "wireframe", "prototype", "layout",
            "navigation", "checkout", "feed", "detail screen"
        )

        /**
         * Explicit canvas/generation triggers — no noun check needed.
         * These phrases unambiguously mean “do it now”.
         */
        private val EXPLICIT_TRIGGERS = listOf(
            "the screens", "those screens", "the designs", "those designs",
            "open canvas", "open the canvas", "show me the screens",
            "just build", "just create", "just design", "just make",
            "go ahead", "proceed", "start designing", "start building",
            "yes", "yeah go", "do it", "let's go", "lets go"
        )

        /** Max clarification turns before we force generate from available context */
        private const val MAX_CLARIFICATION_TURNS = 2
    }

    data class IntentResult(
        val brief: DesignBrief? = null,
        val clarificationQuestion: String? = null,
        val isReadyToGenerate: Boolean = false
    )

    /**
     * Returns true when the user clearly wants to design/generate something.
     *
     * Two detection paths:
     *  1. In-progress design: If designTurns > 0, the user is answering our follow-up question.
     *  2. Explicit trigger phrases — e.g. "the screens", "open canvas", "just build it"
     *     These work even with no prior context (user is confirming a design request).
     *  3. Action verb + design noun — e.g. "design a dashboard", "build me a login"
     *     Prevents false positives like "What makes a great dashboard?" from triggering.
     */
    fun isDesignRequest(message: String, designTurns: Int = 0): Boolean {
        if (designTurns > 0) return true // Keep them in the pipeline if we're actively clarifying!

        val lower = message.lowercase().trim()
        // Path 1: explicit trigger (highest priority)
        if (EXPLICIT_TRIGGERS.any { lower.contains(it) }) return true
        // Path 2: action verb present AND design noun present
        val hasActionVerb = ACTION_VERBS.any { lower.contains(it) }
        val hasDesignNoun = DESIGN_NOUNS.any { lower.contains(it) }
        return hasActionVerb && hasDesignNoun
    }

    /**
     * Run the intent analysis.
     *
     * @param conversationHistory  All messages so far (user + assistant) for context
     * @param designTurns          How many design-focused turns have happened (0-indexed)
     */
    suspend fun run(
        conversationHistory: List<Pair<String, String>>, // role → content
        designTurns: Int = 0
    ): IntentResult {

        // On the FIRST design turn, always clarify — no matter what MiniMax says.
        // This is what makes FORGE feel like a consultant, not a command executor.
        val alwaysAskOnFirstTurn = designTurns == 0
        val forceBrief = designTurns >= MAX_CLARIFICATION_TURNS

        val systemPrompt = """
You are the Intent Architect for FORGE AI Design Studio — an elite AI design consultant.

Your job: analyse the user's design request and decide what to design.

${if (forceBrief) """
IMPORTANT: The user has already provided enough context. DO NOT ask any more questions.
Force a DesignBrief now from whatever context is available.
""" else ""}

Respond with ONLY a single valid JSON object:
{
  "clarificationNeeded": boolean,
  "clarificationQuestion": "string | null",
  "brief": {
    "screenType": "login|signup|onboarding|home|dashboard|profile|settings|list|detail|search|checkout|custom",
    "userGoal": "1-sentence distilled goal",
    "constraints": ["array of requirements"],
    "mood": "minimal|expressive|structured|vibrant",
    "projectName": "Short branded project name (2-3 words)",
    "plannedScreens": ["Screen Name 1", "Screen Name 2", ...]
  }
}

Rules:
- clarificationNeeded=true ONLY if you genuinely cannot determine even the app category
- If you can infer the category, set clarificationNeeded=false and produce the full brief
- plannedScreens: list 3-5 key screens for the app (e.g. ["Login", "Home", "Profile", "Settings"])
- projectName: make it branded and memorable (e.g. "PulseFit", "LunaBank", "ShopFlow")
- clarificationQuestion: ask ONE focused question about the most important missing piece
        """.trimIndent()

        val history = conversationHistory.takeLast(6) // last 3 turns
        val messages = mutableListOf(MiniMaxMessage("system", systemPrompt))
        history.forEach { (role, content) ->
            messages.add(MiniMaxMessage(role, content))
        }

        return try {
            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = messages)
            )
            val rawJson = response.choices?.firstOrNull()?.message?.content ?: ""
            val json = rawJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = gson.fromJson(json, IntentResponse::class.java)

            // First turn: ALWAYS return a clarification question so FORGE feels consultative.
            // Use MiniMax's suggested question if available, otherwise craft a standard one.
            if (alwaysAskOnFirstTurn && !forceBrief) {
                val question = parsed.clarificationQuestion
                    ?: generateFirstTurnQuestion(conversationHistory.lastOrNull()?.second ?: "")
                return IntentResult(
                    brief = null,
                    clarificationQuestion = question,
                    isReadyToGenerate = false
                )
            }

            if (!forceBrief && parsed.clarificationNeeded == true && parsed.clarificationQuestion != null) {
                IntentResult(
                    brief = null,
                    clarificationQuestion = parsed.clarificationQuestion,
                    isReadyToGenerate = false
                )
            } else {
                val b = parsed.brief
                IntentResult(
                    brief = DesignBrief(
                        screenType     = b?.screenType ?: "custom",
                        userGoal       = b?.userGoal ?: "Design a mobile app",
                        constraints    = b?.constraints ?: emptyList(),
                        mood           = b?.mood ?: "minimal",
                        projectName    = b?.projectName ?: "FORGE Project",
                        plannedScreens = b?.plannedScreens?.takeIf { it.isNotEmpty() }
                            ?: listOf("Home", "Profile", "Settings"),
                        rawPrompt      = conversationHistory.lastOrNull()?.second ?: ""
                    ),
                    isReadyToGenerate = true
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("IntentArchitect", "Failed to parse intent", e)
            // Graceful fallback — never block the pipeline
            IntentResult(
                brief = DesignBrief(
                    screenType     = "custom",
                    userGoal       = conversationHistory.lastOrNull()?.second ?: "Design an app",
                    constraints    = emptyList(),
                    mood           = "minimal",
                    projectName    = "FORGE Project",
                    plannedScreens = listOf("Home", "Dashboard", "Profile"),
                    rawPrompt      = conversationHistory.lastOrNull()?.second ?: ""
                ),
                isReadyToGenerate = true
            )
        }
    }

    /**
     * Fallback question for turn 0 when MiniMax doesn’t suggest one.
     * Asks about the single most important missing piece.
     */
    private fun generateFirstTurnQuestion(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("fintech") || lower.contains("bank") || lower.contains("payment") ->
                "Who’s your primary user — consumers managing personal finances, or businesses handling payments?"
            lower.contains("health") || lower.contains("fitness") || lower.contains("wellness") ->
                "Is this focused on tracking (workouts, nutrition, sleep) or connecting with a professional like a doctor or trainer?"
            lower.contains("social") || lower.contains("connect") ->
                "What’s the core social action — sharing content, messaging, or building a professional network?"
            lower.contains("ecommerce") || lower.contains("shop") || lower.contains("store") ->
                "What’s being sold — physical products, digital goods, or services? And who are the buyers?"
            lower.contains("dashboard") || lower.contains("analytics") ->
                "What data will this dashboard show, and who’s the user — a business owner, analyst, or end consumer?"
            else ->
                "What’s the core problem this app solves, and who’s the primary user?"
        }
    }

    // Internal deserialization models
    private data class IntentResponse(
        val clarificationNeeded: Boolean?,
        val clarificationQuestion: String?,
        val brief: BriefResponse?
    )

    private data class BriefResponse(
        val screenType: String?,
        val userGoal: String?,
        val constraints: List<String>?,
        val mood: String?,
        val projectName: String?,
        val plannedScreens: List<String>?
    )
}
