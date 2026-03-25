package com.Chenkham.Echofy.di

import com.Chenkham.ytmusicapi.YTMusicApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YTMusicModule {

    @Provides
    @Singleton
    fun provideYTMusicApi(): YTMusicApi {
        return YTMusicApi()
    }
}
