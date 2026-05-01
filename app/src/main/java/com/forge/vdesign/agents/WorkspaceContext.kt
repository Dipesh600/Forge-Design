package com.forge.vdesign.agents

/**
 * Context injected into the agent pipeline on every turn.
 * Ensures the pipeline has real-time grounding in the current project state.
 */
data class WorkspaceContext(
    val projectId: String,
    val designSystem: String,
    val generatedScreensManifest: String
)
