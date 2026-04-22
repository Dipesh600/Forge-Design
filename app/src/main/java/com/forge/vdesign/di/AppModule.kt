package com.forge.vdesign.di

import android.content.Context
import androidx.room.Room
import com.forge.vdesign.data.local.ConversationDao
import com.forge.vdesign.data.local.ForgeDatabase
import com.forge.vdesign.data.local.MessageDao
import com.forge.vdesign.mcp.McpToolExecutor
import com.forge.vdesign.mcp.StitchMcpClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ForgeDatabase {
        return Room.databaseBuilder(
            context,
            ForgeDatabase::class.java,
            "forge_database"
        )
            .addMigrations(ForgeDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration() // dev safety net — remove before prod release
            .build()
    }


    @Provides
    fun provideMessageDao(database: ForgeDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideConversationDao(database: ForgeDatabase): ConversationDao = database.conversationDao()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("us-central1")

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideStitchMcpClient(gson: Gson): StitchMcpClient = StitchMcpClient(gson)

    @Provides
    @Singleton
    fun provideMcpToolExecutor(
        stitchMcpClient: StitchMcpClient
    ): McpToolExecutor = McpToolExecutor(stitchMcpClient)
}
