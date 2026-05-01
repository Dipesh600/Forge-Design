package com.forge.vdesign.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE user_id = :userId ORDER BY updatedAt DESC")
    fun loadAll(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun loadById(id: String): ConversationEntity?

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE conversations SET is_starred = :starred, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStarred(id: String, starred: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET project_manifest = :manifest, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectManifest(id: String, manifest: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET design_system = :designSystem, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDesignSystem(id: String, designSystem: String, updatedAt: Long = System.currentTimeMillis())
}

