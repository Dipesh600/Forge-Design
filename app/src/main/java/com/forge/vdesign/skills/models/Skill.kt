package com.forge.vdesign.skills.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a set of design rules extracted from a canonical source (e.g. a book).
 */
@Parcelize
data class Skill(
    val id: String = "",
    val name: String = "",
    val version: String = "1.0",
    val category: String = "",
    val sourceBooks: List<String> = emptyList(),
    val rules: List<SkillRule> = emptyList(),
    /**
     * Float array of embeddings used by SkillRouter to find relevant skills.
     * Stored as flat list in Firestore. Empty for asset-bundled skills;
     * SkillRouter uses keyword fallback when embedding is empty.
     */
    val embedding: List<Float> = emptyList()
) : Parcelable

/**
 * A concrete, machine-checkable design rule.
 */
@Parcelize
data class SkillRule(
    val id: String = "",
    val rule: String = "",
    /** Relevance weight 0.0-1.0 — higher = more important rule (used for prompt injection priority) */
    val weight: Float = 1.0f,
    val severity: SkillSeverity = SkillSeverity.WARNING,
    val checkType: SkillCheckType = SkillCheckType.STRUCTURAL
) : Parcelable

enum class SkillSeverity {
    ERROR,      // Must be fixed before the screen is approved
    WARNING     // Suggestion but can pass if critic score is high enough
}

enum class SkillCheckType {
    STRUCTURAL, // Can be checked statically against XML (e.g. layout density, font sizes)
    SEMANTIC    // Requires visual or content analysis (e.g. contrast, terminology)
}
