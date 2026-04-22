package com.forge.vdesign.di

import com.forge.vdesign.data.repository.AuthRepositoryImpl
import com.forge.vdesign.data.repository.ChatRepositoryImpl
import com.forge.vdesign.domain.repository.AuthRepository
import com.forge.vdesign.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
