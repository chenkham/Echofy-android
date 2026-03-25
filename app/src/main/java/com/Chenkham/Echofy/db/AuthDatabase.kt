package com.Chenkham.Echofy.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.Chenkham.Echofy.db.daos.UserDao
import com.Chenkham.Echofy.db.entities.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AuthDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AuthDatabase? = null

        fun getInstance(context: Context): AuthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuthDatabase::class.java,
                    "auth.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
