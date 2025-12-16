package com.Chenkham.Echofy.data.remote

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Databases
import io.appwrite.services.Realtime

/**
 * Appwrite client singleton for Listen Together feature.
 */
object AppwriteClient {
    private const val ENDPOINT = "https://syd.cloud.appwrite.io/v1"
    private const val PROJECT_ID = "693c1265002efbd4af1f"
    
    // Database and collection IDs
    const val DATABASE_ID = "echofy_db"
    const val COLLECTION_ROOMS = "693d4364003b290d6966"
    const val COLLECTION_QUEUE = "session_queue"
    const val COLLECTION_MESSAGES = "session_messages" // For real-time chat
    
    private var client: Client? = null
    private var databases: Databases? = null
    private var realtime: Realtime? = null
    
    fun init(context: Context) {
        if (client == null) {
            client = Client(context)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
                .setSelfSigned(false)
        }
    }
    
    fun getClient(context: Context): Client {
        if (client == null) init(context)
        return client!!
    }
    
    fun getDatabases(context: Context): Databases {
        if (databases == null) {
            databases = Databases(getClient(context))
        }
        return databases!!
    }
    
    fun getRealtime(context: Context): Realtime {
        if (realtime == null) {
            realtime = Realtime(getClient(context))
        }
        return realtime!!
    }
}
