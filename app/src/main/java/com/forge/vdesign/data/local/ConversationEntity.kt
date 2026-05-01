package com.forge.vdesign.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalTokens: Int = 0,
    @ColumnInfo(name = "is_starred", defaultValue = "0")
    val isStarred: Boolean = false,
    @ColumnInfo(name = "user_id", defaultValue = "")
    val userId: String = "",
    @ColumnInfo(name = "project_manifest", defaultValue = "{}")
    val projectManifest: String = "{}",
    @ColumnInfo(name = "design_system", defaultValue = "")
    val designSystem: String = "",
    @ColumnInfo(name = "screen_count", defaultValue = "0")
    val screenCount: Int = 0,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null
)
