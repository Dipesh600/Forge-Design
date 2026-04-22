package com.forge.vdesign.domain.model

/**
 * The design DNA of a project — extracted and updated by MemoryKeeperAgent
 * after every screen generation. Injected into future generation prompts
 * to enforce cross-screen visual consistency.
 *
 * Persisted to Firestore under collection "design_dna/{projectId}".
 * Retrieved before each screen generation to provide cross-screen coherence context.
 */
data class DesignDna(
    val projectId: String,
    val primaryColor: String = "#6750A4",
    val secondaryColor: String = "#625B71",
    val neutralPalette: List<String> = emptyList(),
    val typescaleHeadline: Int = 24,           // sp
    val typescaleBody: Int = 16,               // sp
    val typescaleCaption: Int = 12,            // sp
    val cornerRadius: Int = 12,                // dp
    val spacingUnit: Int = 8,                  // dp base grid
    val componentPatterns: List<String> = emptyList(), // e.g. ["bottom-nav", "card-list", "fab"]
    val brandVoice: String = "friendly, clear, confident",
    val screenCount: Int = 0
)
