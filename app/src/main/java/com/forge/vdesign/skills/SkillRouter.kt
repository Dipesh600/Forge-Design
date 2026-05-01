package com.forge.vdesign.skills

import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.skills.models.Skill
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SkillRouter @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val skillRepository: SkillRepository
) {

    /**
     * Returns the top N most relevant skills for a given DesignBrief.
     *
     * Routing strategy (in order):
     * 1. For skills WITH embeddings: cosine similarity against a brief embedding (MiniMax)
     * 2. For skills WITHOUT embeddings (asset-bundled): keyword/category matching against
     *    the brief's appCategory, screenType, and mood fields
     * 3. All skills are scored, sorted by relevance, top-N returned
     *
     * This dual-strategy means the pipeline works from day one with bundled skills,
     * and gets smarter when Firestore skills with semantic embeddings are ingested.
     */
    suspend fun routeSkills(brief: DesignBrief, limit: Int = 3): List<Skill> {
        val allSkills = skillRepository.getAllSkills()
        if (allSkills.isEmpty()) return emptyList()

        // Split into embedding-capable vs keyword-only skills
        val withEmbeddings    = allSkills.filter { it.embedding.isNotEmpty() }
        val withoutEmbeddings = allSkills.filter { it.embedding.isEmpty() }

        val scoredSkills = mutableListOf<Pair<Skill, Float>>()

        // ── Semantic routing (embedding skills) ──────────────────────────────
        if (withEmbeddings.isNotEmpty()) {
            try {
                val briefContext = buildBriefContext(brief)
                val embeddings = miniMaxClient.getEmbeddings(listOf(briefContext))
                val briefVector = embeddings.firstOrNull()

                if (briefVector != null) {
                    withEmbeddings.forEach { skill ->
                        val sim = cosineSimilarity(briefVector, skill.embedding)
                        scoredSkills.add(skill to sim)
                    }
                } else {
                    // Fallback: keyword-match the embedding skills too
                    withEmbeddings.forEach { skill ->
                        scoredSkills.add(skill to keywordScore(brief, skill))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SkillRouter", "Embedding failed, using keyword fallback: ${e.message}")
                withEmbeddings.forEach { skill ->
                    scoredSkills.add(skill to keywordScore(brief, skill))
                }
            }
        }

        // ── Keyword routing (asset-bundled skills without embeddings) ─────────
        withoutEmbeddings.forEach { skill ->
            scoredSkills.add(skill to keywordScore(brief, skill))
        }

        val result = scoredSkills
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        android.util.Log.d(
            "SkillRouter",
            "Routed ${result.size} skills for ${brief.projectName}: ${result.map { it.name }}"
        )
        return result
    }

    /**
     * Keyword-based relevance score for asset skills.
     *
     * Scores based on:
     * - Category match (e.g., skill.category == "fintech" when brief mentions fintech)
     * - Screen type match (e.g., navigation_patterns for any screen)
     * - Mood match (e.g., minimal mood → productivity/health skills score higher)
     *
     * Returns 0.0–1.0; 1.0 = perfect category match.
     */
    private fun keywordScore(brief: DesignBrief, skill: Skill): Float {
        var score = 0f

        // Category matching (highest weight)
        val briefCategory = brief.rawPrompt.lowercase() + " " +
            (brief.screenType + " " + brief.userGoal + " " + brief.mood).lowercase()

        // Direct category match
        if (skill.category.isNotBlank() && briefCategory.contains(skill.category.lowercase())) {
            score += 0.8f
        }

        // Universal skills always get a base score
        if (skill.category == "navigation" || skill.category == "accessibility") {
            score += 0.5f
        }

        // Onboarding skill for onboarding/signup/login screens
        if (skill.category == "onboarding") {
            val screenTypes = listOf("onboarding", "login", "signup")
            if (screenTypes.any { brief.screenType.lowercase().contains(it) ||
                    brief.rawPrompt.lowercase().contains(it) }) {
                score += 0.6f
            }
        }

        // Mood hints
        when (brief.mood.lowercase()) {
            "vibrant", "expressive" -> if (skill.category in listOf("social", "fitness")) score += 0.2f
            "structured" -> if (skill.category in listOf("fintech", "ecommerce", "productivity")) score += 0.2f
            "minimal" -> if (skill.category in listOf("productivity", "health")) score += 0.2f
        }

        return score.coerceAtMost(1.0f)
    }

    /**
     * Lightweight entry point for ForgeAgent — routes skills from a raw user message string,
     * without needing a full DesignBrief. Used at the start of the ReAct loop so skills can
     * be injected into the system prompt before any LLM call.
     *
     * Uses each skill's own keyword list (from SKILL.md metadata) for matching.
     * Universal design skills (android, color, spatial) always get a baseline score.
     */
    suspend fun routeSkillsFromText(text: String, limit: Int = 4): List<Skill> {
        val allSkills = skillRepository.getAllSkills()
        if (allSkills.isEmpty()) return emptyList()

        val lower = text.lowercase()

        val scored = allSkills.map { skill ->
            var score = 0f

            // Score based on skill's own keyword list — each SKILL.md defines its own relevance criteria
            val keywordMatches = skill.keywords.count { lower.contains(it) }
            score += keywordMatches * 0.25f

            // Universal foundational skills always get a baseline — every screen needs these
            if (skill.category in listOf("android", "color", "spatial", "hierarchy")) {
                score += 0.5f
            }

            // Boost if category name itself appears in the request
            if (lower.contains(skill.category)) score += 0.3f

            skill to score.coerceAtMost(1.5f)
        }

        val result = scored
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        android.util.Log.d("SkillRouter", "routeSkillsFromText → ${result.map { "${it.name}(${it.rules.size}r)" }}")
        return result
    }

    /**
     * Formats a list of routed skills into a structured system prompt block —
     * exactly how Antigravity reads SKILL.md files: full rule anatomy with
     * concrete bad/good examples for high-priority rules, compact bullets for the rest.
     *
     * The Bad/Good example pair is the most important element — it's what forces
     * the LLM to apply a rule concretely rather than just "knowing" it abstractly.
     */
    fun formatSkillsForSystemPrompt(skills: List<Skill>): String {
        if (skills.isEmpty()) return ""

        return buildString {
            appendLine()
            appendLine("════════════════════════════════════════════════")
            appendLine("DESIGN KNOWLEDGE — FORGE SKILL LIBRARY")
            appendLine("These are your design expertise. Apply them when writing Stitch prompts.")
            appendLine("════════════════════════════════════════════════")

            for (skill in skills) {
                if (skill.rules.isEmpty()) continue
                appendLine()
                appendLine("▸ ${skill.name.uppercase()} (${skill.sourceBooks.firstOrNull() ?: ""})")

                val sortedRules = skill.rules.sortedByDescending { it.weight }

                // High-weight rules (≥ 0.85): show full anatomy with examples
                // This is exactly what Antigravity reads — the examples are what make rules stick
                val highPriority = sortedRules.filter { it.weight >= 0.85f }.take(4)
                if (highPriority.isNotEmpty()) {
                    appendLine("  NON-NEGOTIABLE RULES:")
                    for (rule in highPriority) {
                        appendLine("  • ${rule.rule}")
                        if (rule.check.isNotBlank()) appendLine("    CHECK: ${rule.check}")
                        if (rule.bad.isNotBlank())   appendLine("    ✗ BAD:  ${rule.bad}")
                        if (rule.good.isNotBlank())  appendLine("    ✓ GOOD: ${rule.good}")
                    }
                }

                // Mid-weight rules (0.5–0.84): rule + good example only
                val midPriority = sortedRules.filter { it.weight in 0.5f..0.84f }.take(3)
                if (midPriority.isNotEmpty()) {
                    appendLine("  GUIDELINES:")
                    for (rule in midPriority) {
                        appendLine("  • ${rule.rule}")
                        if (rule.good.isNotBlank()) appendLine("    ✓ ${rule.good}")
                    }
                }

                // Low-weight rules: compact bullets only
                val lowPriority = sortedRules.filter { it.weight < 0.5f }.take(2)
                if (lowPriority.isNotEmpty()) {
                    lowPriority.forEach { appendLine("  · ${it.rule}") }
                }
            }

            appendLine()
            appendLine("════════════════════════════════════════════════")
            appendLine("CITATION REQUIREMENT: In your <think> block before every generate_screen call,")
            appendLine("write a DESIGN DECISION LOG listing which specific rules above you are applying")
            appendLine("and how each manifests in your Stitch prompt. Reference the ✓ GOOD examples")
            appendLine("as concrete targets. This is mandatory — not optional.")
            appendLine("════════════════════════════════════════════════")
        }.trimEnd()
    }


    private fun buildBriefContext(brief: DesignBrief): String = """
        Designing an Android mobile screen.
        App: ${brief.projectName}
        Type: ${brief.screenType}
        Goal: ${brief.userGoal}
        Mood: ${brief.mood}
        Constraints: ${brief.constraints.joinToString(", ")}
        Prompt: ${brief.rawPrompt}
    """.trimIndent()

    private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        if (norm1 == 0f || norm2 == 0f) return 0f
        return (dotProduct / (sqrt(norm1) * sqrt(norm2)))
    }
}
