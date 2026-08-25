package com.Chenkham.Echofy.di

import com.Chenkham.Echofy.db.MusicDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReleaseRadarEntryPoint {
    fun database(): MusicDatabase
}
