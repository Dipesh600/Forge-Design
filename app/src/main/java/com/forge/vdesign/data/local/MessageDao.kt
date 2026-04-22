package com.forge.vdesign.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    /** Observe all messages for a conversation in chronological order — primary UI source */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun loadByConversation(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Synchronous load of recent messages for MiniMax context window.
     * Returns in ASC order (oldest first) so they read correctly as a conversation.
     * Subquery trick: take last N rows from a DESC sort, then re-sort ASC.
     */
    @Query("SELECT * FROM (SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    suspend fun loadRecentSync(conversationId: String, limit: Int): List<MessageEntity>

    /** Delete all messages in a conversation (used when user swipes-to-delete a chat) */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

