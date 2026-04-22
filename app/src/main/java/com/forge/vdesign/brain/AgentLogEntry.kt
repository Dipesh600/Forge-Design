package com.forge.vdesign.brain

/**
 * A single step in the agentic pipeline, surfaced live in the Canvas Agent Log panel.
 *
 * The Canvas shows these entries as they arrive so the user sees the
 * agent thinking — not just a spinner.
 *
 * Example sequence:
 *   ✦ IntentArchitect   "Parsing design intent…"           RUNNING
 *   ✦ IntentArchitect   "DesignBrief ready: Login screen"  DONE
 *   ✦ SkillRouter       "Loaded 3 skills from Firestore"   DONE
 *   ✦ PromptSynthesis   "Building Stitch prompt…"          RUNNING
 *   ✦ Stitch MCP        "generate_screen_from_text…"       RUNNING
 *   ✦ Stitch MCP        "Screen generated ✓"               DONE
 *   ✦ CriticAgent       "Score: 8.4 / 10 — passed"         DONE
 */
enum class LogStatus { RUNNING, DONE, FAILED }

data class AgentLogEntry(
    val agent: String,        // "IntentArchitect" | "SkillRouter" | "Stitch MCP" | "CriticAgent"
    val action: String,       // human-readable description of what's happening
    val status: LogStatus,
    val screenName: String? = null,  // which screen this log entry belongs to (null = setup phase)
    val timestamp: Long = System.currentTimeMillis()
)
