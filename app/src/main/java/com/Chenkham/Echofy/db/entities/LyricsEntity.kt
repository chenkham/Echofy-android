package com.Chenkham.Echofy.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val id: String,
    val lyrics: String,
    val providerName: String? = null,
) {
    companion object {
        const val LYRICS_NOT_FOUND = " "
    }
}
