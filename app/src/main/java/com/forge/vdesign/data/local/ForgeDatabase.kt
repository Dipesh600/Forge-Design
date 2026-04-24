package com.forge.vdesign.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 4,
    exportSchema = true
)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        /** v1 → v2: Add `metadata` column to messages table. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN metadata TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v2 → v3: Add `is_starred` column to conversations table (default false). */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN is_starred INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v3 → v4: Add `user_id` column to conversations table for multi-tenant isolation. */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
