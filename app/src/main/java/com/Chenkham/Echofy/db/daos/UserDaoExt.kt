package com.Chenkham.Echofy.db.daos

import androidx.room.Transaction

suspend fun UserDao.setActiveUser(googleId: String) {
    // Deactivate all users
    deactivateAllUsers()
    // Activate the selected user
    activateUser(googleId)
}
