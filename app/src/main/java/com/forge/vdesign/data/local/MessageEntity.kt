package com.forge.vdesign.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    /**
     * JSON blob for structured metadata attached to this message.
     * Canvas cards store the serialized DesignBrief here so the UI
     * can render a proper launch card without any extra network call.
     * Format: {"type":"canvas_card","brief":{...DesignBrief fields...}}
     */
    @ColumnInfo(name = "metadata", defaultValue = "")
    val metadata: String = ""
)
