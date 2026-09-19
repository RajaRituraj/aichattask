package com.example.aichattask.di

import com.example.aichattask.data.repository.AiRepositoryImpl
import com.example.aichattask.data.repository.ChatRepositoryImpl
import com.example.aichattask.domain.repository.AiRepository
import com.example.aichattask.domain.repository.ChatRepository
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
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
}
