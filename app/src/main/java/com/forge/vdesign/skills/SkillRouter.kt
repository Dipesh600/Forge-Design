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
