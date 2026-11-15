package com.Chenkham.Echofy.appwrite

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Realtime

/**
 * Lazily initialised Appwrite client.
 * Swap [AppwriteConfig.ENDPOINT] to move from cloud → self-hosted VPS.
 */
object AppwriteClientProvider {
    @Volatile private var client: Client? = null

    fun get(context: Context): Client =
        client ?: synchronized(this) {
            client ?: Client(context.applicationContext)
                .setEndpoint(AppwriteConfig.ENDPOINT)
                .setProject(AppwriteConfig.PROJECT_ID)
                .apply {
                    // Google Play Security Policy: Never enable self-signed in production release builds
                    if (com.Chenkham.Echofy.BuildConfig.DEBUG && AppwriteConfig.SELF_SIGNED) {
                        setSelfSigned(true)
                    }
                }
                .also { client = it }
        }

    fun account(context: Context) = Account(get(context))

    fun databases(context: Context) = Databases(get(context))

    fun realtime(context: Context) = Realtime(get(context))
}
