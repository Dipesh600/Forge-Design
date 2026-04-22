package com.forge.vdesign.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Holds a single Stitch-generated design screen.
 *
 * FORGE is a design agent — Stitch HTML/screenshots are the final output.
 * There is no Android XML translation. The canvas renders screenshotUrl in an
 * ImageView and htmlUrl in a WebView for the full interactive preview.
 */
@Parcelize
data class GeneratedScreen(
    val screenId: String,
    val screenName: String,                         // e.g. "Login", "Home Dashboard"
    val htmlUrl: String? = null,                    // Stitch-hosted HTML URL
    val screenshotUrl: String? = null,              // Stitch PNG preview URL
    val localHtmlPath: String? = null,              // local path after download to .stitch/designs/
    val description: String? = null,                // Stitch's natural-language output description
    val stitchSuggestions: List<String> = emptyList(), // Stitch's own improvement hints
    val brief: DesignBrief,
    val designReasoning: List<DesignReasoning> = emptyList(),
    val criticScore: Float = 0f,                    // 0-10 from CriticAgent
    val revisionCount: Int = 0,
    val a11yIssueCount: Int = 0,                    // From AccessibilityAuditorAgent (error count)
    val wcagLevel: String = "AA",                   // "AA", "AAA", or "FAIL"
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * A single design decision surfaced in the Design Reasoning panel.
 */
@Parcelize
data class DesignReasoning(
    val decision: String,    // e.g. "Used sticky nav with glassmorphism"
    val principle: String,   // e.g. "Navigation should always be accessible"
    val sourceBook: String,  // e.g. "Refactoring UI"
    val skillId: String      // e.g. "visual-hierarchy-v1"
) : Parcelable
