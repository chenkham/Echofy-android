package com.Chenkham.Echofy.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.Chenkham.Echofy.db.AuthDatabase
import com.Chenkham.Echofy.db.entities.UserEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val userDao = AuthDatabase.getInstance(context).userDao()
    
    fun getActiveUser(): Flow<UserEntity?> = userDao.getActiveUser()
    
    suspend fun getActiveUserOnce(): UserEntity? = userDao.getActiveUserOnce()
    
    suspend fun signInWithGoogle(webClientId: String, activityContext: Context): Result<UserEntity> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            
            val user = UserEntity(
                googleId = credential.id,
                email = credential.id, // Google ID is email
                displayName = credential.displayName,
                photoUrl = credential.profilePictureUri?.toString(),
                signInTimestamp = System.currentTimeMillis(),
                isActive = true,
                isPremium = false
            )
            
            // Deactivate all users and save new user
            userDao.deactivateAllUsers()
            userDao.insertUser(user)
            
            Result.success(user)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut() {
        userDao.deactivateAllUsers()
    }
    
    suspend fun deleteAccount(googleId: String) {
        userDao.deleteUser(googleId)
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        val user = userDao.getActiveUserOnce()
        if (user != null) {
            userDao.updatePremiumStatus(user.googleId, isPremium)
        }
    }
}
