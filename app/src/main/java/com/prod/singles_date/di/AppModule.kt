package com.prod.singles_date.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.prod.singles_date.repository.AuthRepository
import com.prod.singles_date.repository.ChatRepository
import com.prod.singles_date.repository.MediaRepository
import com.prod.singles_date.repository.ProfileRepository
import com.prod.singles_date.repository.S3MediaClient
import com.prod.singles_date.repository.ThoughtRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideS3MediaClient(): S3MediaClient = S3MediaClient()

    @Provides
    @Singleton
    fun provideMediaRepository(s3MediaClient: S3MediaClient): MediaRepository =
        MediaRepository(s3MediaClient)

    @Provides
    @Singleton
    fun provideAuthRepository(mediaRepository: MediaRepository): AuthRepository =
        AuthRepository(mediaRepository = mediaRepository)

    @Provides
    @Singleton
    fun provideThoughtRepository(mediaRepository: MediaRepository): ThoughtRepository =
        ThoughtRepository(mediaRepository = mediaRepository)

    @Provides
    @Singleton
    fun provideChatRepository(): ChatRepository = ChatRepository()

    @Provides
    @Singleton
    fun provideProfileRepository(): ProfileRepository = ProfileRepository()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}
