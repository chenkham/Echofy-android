package com.Chenkham.Echofy.db.daos

suspend fun UserDao.setActiveUser(googleId: String) {
    // Deactivate all users
    deactivateAllUsers()
    // Activate the selected user
    activateUser(googleId)
}
