package com.Chenkham.Echofy.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val googleId: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val signInTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isPremium: Boolean = false
)
