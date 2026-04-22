package com.forge.vdesign.agents

import android.os.Parcelable
import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.forge.vdesign.skills.models.Skill
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

// ─── CriticReport domain model ──────────────────────────────────────────────

@Parcelize
data class CriticReport(
    val overallScore: Float = 0f,
    val passed: Boolean = false,
    val ruleScores: Map<String, Float> = emptyMap(),
    val violations: List<Violation> = emptyList(),
    val suggestedFixes: List<String> = emptyList()
) : Parcelable

@Parcelize
data class Violation(
    val ruleId: String = "",
    val description: String = "",
    val severity: String = "WARNING"
) : Parcelable

/**
 * Input to the CriticAgent — the natural-language output from Stitch MCP.
 * FORGE is a design agent: we evaluate DESIGN QUALITY, not code syntax.
 */
data class StitchOutput(
    val description: String,          // Stitch's text description of what was generated
    val suggestions: List<String> = emptyList(), // Stitch's own hints
    val screenshotUrl: String? = null
)

/**
 * CriticAgent
 *
 * Evaluates Stitch's design output (description + suggestions) against activated
 * SkillRules using MiniMax. Returns [CriticReport] with score, violations, fixes.
 *
 * If score < 7.0, BrainOrchestrator calls edit_screens with the fix instructions.
 */
@Singleton
class CriticAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val gson: Gson
) {
    companion object {
        private const val PASS_THRESHOLD = 7.0f
    }

    /**
     * Evaluate Stitch's output against design skill rules.
     *
     * @param stitchOutput  What Stitch generated (description + suggestions)
     * @param skills        Relevant skills from SkillRouter
     */
    suspend fun evaluate(stitchOutput: StitchOutput, skills: List<Skill>): CriticReport {
        if (skills.isEmpty()) {
            // No skills ingested yet — pass through so generation isn't blocked
            return CriticReport(overallScore = 8.0f, passed = true)
        }

        if (stitchOutput.description.isBlank()) {
            // Nothing to evaluate
            return CriticReport(overallScore = 5.0f, passed = false,
                violations = listOf(Violation("empty-output", "Stitch returned no description", "ERROR")))
        }

        val rulesContext = skills.joinToString("\n\n") { skill ->
            "Skill: ${skill.name} (${skill.sourceBooks.joinToString(", ")})\n" +
            skill.rules.take(5).joinToString("\n") { "  [${it.id}] ${it.rule} (${it.severity})" }
        }

        val systemPrompt = """
You are the CriticAgent for FORGE AI Design Studio.
You evaluate UI design quality based on design principles — not code.

Output ONLY valid JSON (no markdown, no explanation):
{
  "overallScore": float,
  "passed": boolean,
  "ruleScores": { "rule-id": float },
  "violations": [
    { "ruleId": "string", "description": "what was violated", "severity": "ERROR"|"WARNING" }
  ],
  "suggestedFixes": ["Fix instruction for Stitch edit_screens call"]
}

Score 0-10. Pass threshold is 7.0.
suggestedFixes should be actionable edit instructions for Stitch (e.g. "Increase contrast of CTA button").
        """.trimIndent()

        val userMessage = """
Evaluate this Stitch-generated screen design against the FORGE skill rules.

--- DESIGN RULES ---
$rulesContext

--- STITCH OUTPUT DESCRIPTION ---
${stitchOutput.description.take(4000)}

--- STITCH SUGGESTIONS ---
${stitchOutput.suggestions.joinToString("\n").take(1000)}
        """.trimIndent()

        return try {
            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(
                    MiniMaxMessage("system", systemPrompt),
                    MiniMaxMessage("user", userMessage)
                ))
            )
            val raw = response.choices?.firstOrNull()?.message?.content ?: ""
            val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = gson.fromJson(json, CriticResponse::class.java)

            CriticReport(
                overallScore    = parsed.overallScore ?: 0f,
                passed          = (parsed.overallScore ?: 0f) >= PASS_THRESHOLD,
                ruleScores      = parsed.ruleScores ?: emptyMap(),
                violations      = parsed.violations?.map {
                    Violation(it.ruleId ?: "", it.description ?: "", it.severity ?: "WARNING")
                } ?: emptyList(),
                suggestedFixes  = parsed.suggestedFixes ?: emptyList()
            )
        } catch (e: Exception) {
            android.util.Log.e("CriticAgent", "Evaluation failed — passing through", e)
            CriticReport(overallScore = 7.5f, passed = true)
        }
    }

    private data class CriticResponse(
        val overallScore: Float?,
        val passed: Boolean?,
        val ruleScores: Map<String, Float>?,
        val violations: List<ViolationResponse>?,
        val suggestedFixes: List<String>?
    )

    private data class ViolationResponse(
        val ruleId: String?,
        val description: String?,
        val severity: String?
    )
}
