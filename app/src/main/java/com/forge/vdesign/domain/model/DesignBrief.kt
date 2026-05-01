package com.forge.vdesign.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A structured representation of the user's design intent.
 *
 * Created by IntentArchitectAgent after 1-3 consultative turns in chat.
 * [plannedScreens] is the agent's decision about which screens to generate.
 * [stitchProjectId] is populated by BrainOrchestrator after create_project succeeds —
 * reused across all screens in the set for design consistency.
 */
@Parcelize
data class DesignBrief(
    val screenType: String,                 // primary screen type, e.g. "dashboard"
    val userGoal: String,                   // distilled user intent
    val constraints: List<String>,          // e.g. ["dark mode", "fintech", "no bottom nav"]
    val mood: String,                       // "minimal" | "expressive" | "structured" | "vibrant"
    val platform: String = "mobile",        // always mobile for FORGE
    val rawPrompt: String = "",             // original user message(s) preserved for context
    val projectName: String = "FORGE Project", // Stitch project name
    val plannedScreens: List<String> = emptyList(), // e.g. ["Login", "Dashboard", "Profile"]
    val stitchProjectId: String? = null,    // set after Stitch create_project
    val primaryColorHex: String? = null,    // e.g. "#FF5722"
    val fontFamily: String? = null,         // e.g. "Inter", "Roboto"
    val isDarkMode: Boolean = false,        // dark or light mode
    val designSystemAssetId: String? = null // set after apply_design_system
) : Parcelable
