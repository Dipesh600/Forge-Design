package com.forge.vdesign.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        /**
         * v1 → v2: Add `metadata` column to messages table.
         * Existing rows default to empty string (no canvas card).
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE messages ADD COLUMN metadata TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
