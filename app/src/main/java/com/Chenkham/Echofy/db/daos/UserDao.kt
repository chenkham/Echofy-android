package com.Chenkham.Echofy.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.Chenkham.Echofy.db.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE isActive = 1 LIMIT 1")
    fun getActiveUser(): Flow<UserEntity?>
    
    @Query("SELECT * FROM user WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveUserOnce(): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    

    
    @Query("UPDATE user SET isActive = 0")
    suspend fun deactivateAllUsers(): Int
    
    @Query("UPDATE user SET isActive = 1 WHERE googleId = :googleId")
    suspend fun activateUser(googleId: String): Int
    
    @Query("DELETE FROM user WHERE googleId = :googleId")
    suspend fun deleteUser(googleId: String): Int
    
    @Query("DELETE FROM user")
    suspend fun deleteAllUsers(): Int

    @Query("UPDATE user SET isPremium = :isPremium WHERE googleId = :googleId")
    suspend fun updatePremiumStatus(googleId: String, isPremium: Boolean): Int
}
