package com.forge.vdesign.agents

import android.util.Log
import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.mcp.McpResult
import com.forge.vdesign.mcp.McpToolExecutor
import com.forge.vdesign.mcp.StitchScreenResult
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VariantExplorerAgent
 *
 * Generates 3 directional variations from the same brief, each exploring
 * a different design philosophy:
 *
 *   1. MINIMAL    — most restrained: generous whitespace, monochromatic palette,
 *                   single typeface, no decorative elements. "Less is more."
 *
 *   2. EXPRESSIVE — bold and emotive: rich colour, typographic personality,
 *                   playful motion, illustrations. "Design as delight."
 *
 *   3. STRUCTURED — precision and order: dense information layout, strong grid,
 *                   clear data hierarchy, trust-inducing. "Design as clarity."
 *
 * Each variant is generated in parallel via Stitch MCP using a tailored prompt.
 * The agent also writes a one-paragraph trade-off explanation for each.
 *
 * Triggered when user taps "Explore Variants" in the canvas.
 */
@Singleton
class VariantExplorerAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val mcpToolExecutor: McpToolExecutor,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "VariantExplorerAgent"
    }

    data class Variant(
        val philosophy: VariantPhilosophy,
        val displayName: String,
        val tradeoffExplanation: String,
        val stitchResult: StitchScreenResult?
    )

    enum class VariantPhilosophy(val label: String, val tagline: String) {
        MINIMAL("Minimal", "Less is more"),
        EXPRESSIVE("Expressive", "Design as delight"),
        STRUCTURED("Structured", "Design as clarity")
    }

    data class VariantSet(
        val screenName: String,
        val variants: List<Variant>
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Generate 3 variants for a single screen in parallel.
     *
     * @param brief The design brief (used for context)
     * @param screenName Which screen to explore (e.g. "Login", "Dashboard")
     * @param baseDescription The Stitch description of the existing screen (for context)
     * @return VariantSet with 3 variants (may have null stitchResult on MCP failure)
     */
    suspend fun exploreVariants(
        brief: DesignBrief,
        screenName: String,
        baseDescription: String?
    ): VariantSet = coroutineScope {

        val projectId = brief.stitchProjectId
            ?: return@coroutineScope buildFallbackVariantSet(screenName, brief)

        // Generate all 3 in parallel to minimise wait time
        val variantJobs = VariantPhilosophy.values().map { philosophy ->
            async {
                generateVariant(brief, screenName, baseDescription, philosophy, projectId)
            }
        }

        val variants = variantJobs.awaitAll()
        VariantSet(screenName = screenName, variants = variants)
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private suspend fun generateVariant(
        brief: DesignBrief,
        screenName: String,
        baseDescription: String?,
        philosophy: VariantPhilosophy,
        projectId: String
    ): Variant {
        // 1. Ask MiniMax to synthesise a philosophy-specific Stitch prompt
        val stitchPrompt = synthesiseVariantPrompt(brief, screenName, baseDescription, philosophy)

        // 2. Call Stitch MCP
        val stitchResult = try {
            when (val mcpResult = mcpToolExecutor.generateScreen(
                projectId  = projectId,
                prompt     = stitchPrompt,
                deviceType = "MOBILE"
            )) {
                is McpResult.Success -> mcpResult.data
                is McpResult.Error   -> {
                    Log.w(TAG, "MCP failed for ${philosophy.label}: ${mcpResult.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception generating ${philosophy.label} variant", e)
            null
        }

        // 3. Generate trade-off explanation
        val tradeoff = generateTradeoffExplanation(philosophy, brief, screenName)

        return Variant(
            philosophy          = philosophy,
            displayName         = "${philosophy.label} — ${philosophy.tagline}",
            tradeoffExplanation = tradeoff,
            stitchResult        = stitchResult
        )
    }

    private suspend fun synthesiseVariantPrompt(
        brief: DesignBrief,
        screenName: String,
        baseDescription: String?,
        philosophy: VariantPhilosophy
    ): String {
        val philosophyInstructions = when (philosophy) {
            VariantPhilosophy.MINIMAL ->
                """DESIGN PHILOSOPHY: MINIMAL
Principles to apply:
- Maximum 2 colours: near-white background, single brand accent
- Only 2 font sizes: heading and body. No decorative typography
- Touch targets only — no icons unless essential
- 32dp+ whitespace between sections, 8dp between related elements
- No shadows, no gradients, no illustrations
- The absence of elements IS the design"""

            VariantPhilosophy.EXPRESSIVE ->
                """DESIGN PHILOSOPHY: EXPRESSIVE
Principles to apply:
- Bold, saturated colour palette — use the brand colour generously
- Large hero typography — headline should fill the screen width
- Illustrative or icon-rich — celebrate the content
- Micro-detail: rounded corners (24dp+), playful component shapes
- Layered depth: 2-3 elevation levels with shadows
- Colour-coded states and moods — delight is the goal"""

            VariantPhilosophy.STRUCTURED ->
                """DESIGN PHILOSOPHY: STRUCTURED
Principles to apply:
- Dense information layout — trust-inspiring, no wasted space
- Strict 8-column grid — every element aligned to a column
- Clear data hierarchy: labels above values, values bold
- Muted colour palette — navy, grey, white — with a single utility accent
- Table or list patterns preferred over card grids
- Power-user density: show more, scroll less"""
        }

        val systemPrompt = """
You are a Stitch prompt engineer. Write a mobile screen description tuned to a specific design philosophy.
Output ONLY the description. No JSON, no markdown, no explanation. Be specific about layout, colour, spacing, components.
        """.trimIndent()

        val userMessage = buildString {
            appendLine("Write a Stitch screen description for the $philosophyInstructions variant of:")
            appendLine("App: ${brief.projectName} — ${brief.userGoal}")
            appendLine("Screen: $screenName")
            if (!baseDescription.isNullOrBlank()) appendLine("Base design context: $baseDescription")
            appendLine()
            appendLine(philosophyInstructions)
        }

        return try {
            val resp = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(
                    MiniMaxMessage("system", systemPrompt),
                    MiniMaxMessage("user", userMessage)
                ))
            )
            resp.choices?.firstOrNull()?.message?.content?.trim()
                ?: buildFallbackVariantPrompt(brief, screenName, philosophy)
        } catch (e: Exception) {
            Log.w(TAG, "Prompt synthesis failed for ${philosophy.label}", e)
            buildFallbackVariantPrompt(brief, screenName, philosophy)
        }
    }

    private suspend fun generateTradeoffExplanation(
        philosophy: VariantPhilosophy,
        brief: DesignBrief,
        screenName: String
    ): String {
        val prompt = """
In one paragraph (3-4 sentences), explain the trade-offs of the ${philosophy.label} design approach
for a ${brief.projectName} ${screenName} screen. 
What does it gain? What does it sacrifice? When is this approach the right choice?
Be direct and opinionated — you are a senior product designer explaining to a client.
        """.trimIndent()

        return try {
            val resp = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(MiniMaxMessage("user", prompt)))
            )
            resp.choices?.firstOrNull()?.message?.content?.trim()
                ?: defaultTradeoff(philosophy)
        } catch (e: Exception) {
            defaultTradeoff(philosophy)
        }
    }

    private fun buildFallbackVariantSet(screenName: String, brief: DesignBrief) = VariantSet(
        screenName = screenName,
        variants = VariantPhilosophy.values().map { philosophy ->
            Variant(
                philosophy          = philosophy,
                displayName         = "${philosophy.label} — ${philosophy.tagline}",
                tradeoffExplanation = defaultTradeoff(philosophy),
                stitchResult        = null
            )
        }
    )

    private fun buildFallbackVariantPrompt(
        brief: DesignBrief,
        screenName: String,
        philosophy: VariantPhilosophy
    ) = "${brief.projectName} $screenName — ${philosophy.label.lowercase()} design variant. " +
        "${brief.userGoal}. ${philosophy.tagline}. Mobile. Material Design 3."

    private fun defaultTradeoff(philosophy: VariantPhilosophy) = when (philosophy) {
        VariantPhilosophy.MINIMAL ->
            "The Minimal approach maximises focus and cognitive clarity by removing everything non-essential. " +
            "It sacrifices personality and emotional warmth for raw usability. " +
            "Choose this when your users are task-oriented and efficiency is the primary metric."
        VariantPhilosophy.EXPRESSIVE ->
            "The Expressive approach builds brand identity and emotional connection from the first interaction. " +
            "It sacrifices information density to prioritise delight and memorability. " +
            "Choose this for consumer apps where first impressions and virality matter most."
        VariantPhilosophy.STRUCTURED ->
            "The Structured approach communicates reliability and trustworthiness through order and density. " +
            "It sacrifices warmth and playfulness to serve power users who need data fast. " +
            "Choose this for B2B tools, dashboards, and professional productivity contexts."
    }
}
