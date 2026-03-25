package com.Chenkham.Echofy.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_search_song",
    indices = [
        Index(
            value = ["songId"],
            unique = false,
        ),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecentSearchSong(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val timestamp: Long = System.currentTimeMillis(),
)
