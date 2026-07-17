package com.Chenkham.Echofy.appwrite

import com.Chenkham.Echofy.BuildConfig

/**
 * Single source of truth for Appwrite configuration.
 * Defaults point to the Echofy Appwrite Cloud project in Frankfurt.
 */
object AppwriteConfig {
    val ENDPOINT: String = BuildConfig.APPWRITE_ENDPOINT
    val PROJECT_ID: String = BuildConfig.APPWRITE_PROJECT_ID
    val DATABASE_ID: String = BuildConfig.APPWRITE_DATABASE_ID
    val SELF_SIGNED: Boolean = BuildConfig.APPWRITE_SELF_SIGNED

    const val COL_ROOMS = "together_rooms"
    const val COL_PRESENCE = "together_presence"
    const val COL_DONATIONS = "donations"
}
