package com.forge.vdesign.domain.model

/**
 * Domain model for a conversation thread.
 * One conversation = one project context in FORGE.
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalTokens: Int = 0,
    val messageCount: Int = 0,
    val isStarred: Boolean = false,
    val designSystem: String = "",
    val projectManifest: String = "{}",
    val screenCount: Int = 0,
    val thumbnailUrl: String? = null
)
