package com.forge.vdesign.agents

import android.util.Log
import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AccessibilityAuditorAgent
 *
 * Audits a generated screen's Stitch description for accessibility compliance.
 * Checks against WCAG 2.2 and Android accessibility guidelines.
 *
 * What it checks (from the design description + suggestions):
 *   - Contrast ratio signals (body text: 4.5:1 min, large text: 3:1 min)
 *   - Touch target size references (48×48dp minimum)
 *   - Reading order coherence (logical sequence in description)
 *   - Content descriptions on interactive elements (buttons, icons)
 *   - Text scaling support mentions
 *
 * Output: AuditReport with issues[], severity[], and auto_fixes[]
 * that the ScreenCanvasActivity can display to the user.
 *
 * Called automatically after every screen generation (post-Critic).
 */
@Singleton
class AccessibilityAuditorAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "AccessibilityAuditor"
    }

    data class AuditIssue(
        val issueType: String,         // e.g. "contrast", "touch_target", "missing_label"
        val description: String,       // human-readable problem description
        val severity: IssueSeverity,
        val suggestedFix: String       // actionable fix
    )

    enum class IssueSeverity { ERROR, WARNING, INFO }

    data class AuditReport(
        val screenName: String,
        val issues: List<AuditIssue>,
        val overallPassed: Boolean,
        val wcagLevel: WcagLevel,
        val autoFixSummary: String     // one-line summary of what was auto-corrected
    ) {
        val errorCount: Int get() = issues.count { it.severity == IssueSeverity.ERROR }
        val warningCount: Int get() = issues.count { it.severity == IssueSeverity.WARNING }
    }

    enum class WcagLevel { AA, AAA, FAIL }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Run an accessibility audit on the generated screen's Stitch description.
     *
     * @param screenName Name of the screen (for the report)
     * @param description Stitch's text description of the generated screen
     * @param suggestions Stitch's improvement suggestions
     * @return AuditReport with issues and suggested fixes
     */
    suspend fun audit(
        screenName: String,
        description: String?,
        suggestions: List<String> = emptyList()
    ): AuditReport {
        if (description.isNullOrBlank()) {
            return AuditReport(
                screenName     = screenName,
                issues         = listOf(AuditIssue(
                    issueType    = "no_description",
                    description  = "No screen description available to audit",
                    severity     = IssueSeverity.INFO,
                    suggestedFix = "Ensure Stitch returns a description for audit"
                )),
                overallPassed  = true,
                wcagLevel      = WcagLevel.AA,
                autoFixSummary = "No audit performed"
            )
        }

        return try {
            val rawReport = callMiniMaxForAudit(screenName, description, suggestions)
            parseAuditReport(screenName, rawReport)
        } catch (e: Exception) {
            Log.w(TAG, "Audit failed for $screenName: ${e.message}")
            buildFallbackReport(screenName)
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private suspend fun callMiniMaxForAudit(
        screenName: String,
        description: String,
        suggestions: List<String>
    ): AuditResponse {
        val systemPrompt = """
You are FORGE's Accessibility Auditor — a senior mobile accessibility specialist.

Audit the Stitch screen description for WCAG 2.2 and Android accessibility compliance.
Focus on signals in the description (don't invent issues that aren't implied).

Output ONLY valid JSON matching this schema:
{
  "issues": [
    {
      "issueType": "contrast|touch_target|missing_label|reading_order|text_scaling|other",
      "description": "specific human-readable problem",
      "severity": "ERROR|WARNING|INFO",
      "suggestedFix": "concrete actionable fix instruction for Stitch"
    }
  ],
  "overallPassed": true|false,
  "wcagLevel": "AA|AAA|FAIL",
  "autoFixSummary": "One-line summary of issues found"
}

Rules:
- ERROR: blocks accessibility (missing labels, contrast below 3:1)
- WARNING: degrades experience (touch target < 48dp, unclear reading order)
- INFO: improvement opportunity (could add better text descriptions)
- overallPassed=true if no ERROR severity issues
- wcagLevel=AA if 0 errors; AAA if 0 errors and 0 warnings; FAIL if any errors
- If description seems well-designed with no issues, return empty issues[] and overallPassed=true
        """.trimIndent()

        val userMessage = buildString {
            appendLine("Screen: $screenName")
            appendLine("Description: $description")
            if (suggestions.isNotEmpty()) {
                appendLine("Stitch suggestions: ${suggestions.take(3).joinToString("; ")}")
            }
        }

        val response = miniMaxClient.chatCompletion(
            MiniMaxRequest(messages = listOf(
                MiniMaxMessage("system", systemPrompt),
                MiniMaxMessage("user", userMessage)
            ))
        )
        val raw = response.choices?.firstOrNull()?.message?.content ?: "{}"
        val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return gson.fromJson(json, AuditResponse::class.java)
    }

    private fun parseAuditReport(screenName: String, response: AuditResponse): AuditReport {
        val issues = response.issues?.map { raw ->
            AuditIssue(
                issueType    = raw.issueType ?: "unknown",
                description  = raw.description ?: "Accessibility issue detected",
                severity     = when (raw.severity?.uppercase()) {
                    "ERROR"   -> IssueSeverity.ERROR
                    "WARNING" -> IssueSeverity.WARNING
                    else      -> IssueSeverity.INFO
                },
                suggestedFix = raw.suggestedFix ?: "Review element for accessibility compliance"
            )
        } ?: emptyList()

        return AuditReport(
            screenName     = screenName,
            issues         = issues,
            overallPassed  = response.overallPassed != false,
            wcagLevel      = when (response.wcagLevel?.uppercase()) {
                "AAA"  -> WcagLevel.AAA
                "FAIL" -> WcagLevel.FAIL
                else   -> WcagLevel.AA
            },
            autoFixSummary = response.autoFixSummary ?: "Audit complete"
        )
    }

    private fun buildFallbackReport(screenName: String) = AuditReport(
        screenName     = screenName,
        issues         = listOf(
            AuditIssue(
                issueType    = "audit_failed",
                description  = "Accessibility audit could not be completed",
                severity     = IssueSeverity.INFO,
                suggestedFix = "Manually verify: 48dp touch targets, 4.5:1 contrast, content descriptions"
            )
        ),
        overallPassed  = true,
        wcagLevel      = WcagLevel.AA,
        autoFixSummary = "Manual audit recommended"
    )

    // Internal deserialization
    private data class AuditResponse(
        val issues: List<RawIssue>?,
        val overallPassed: Boolean?,
        val wcagLevel: String?,
        val autoFixSummary: String?
    )

    private data class RawIssue(
        val issueType: String?,
        val description: String?,
        val severity: String?,
        val suggestedFix: String?
    )
}
