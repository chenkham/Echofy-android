package com.Chenkham.Echofy.jam

import android.content.Context
import com.Chenkham.Echofy.appwrite.AppwriteConfig

/**
 * Builds the default [JamRegistry] backed by Appwrite.
 * No Firebase Realtime Database config needed here anymore.
 */
object JamBootstrapConfig {

    fun defaultRegistry(context: Context): JamRegistry {
        return JamRegistry(
            version = 1,
            defaultTtlSeconds = 86_400,
            inviteBaseUrl = context.optionalString("jam_invite_base_url").orEmpty(),
            shards = listOf(
                JamShardConfig(
                    id = "01",
                    status = JamShardStatus.ACTIVE,
                    region = "bootstrap",
                    weight = 100,
                    capacity = JamShardCapacity(softRooms = 10_000, hardRooms = 15_000),
                    features = JamShardFeatures(canCreateRooms = true, canJoinRooms = true),
                ),
            ),
        )
    }

    private fun Context.optionalString(name: String): String? {
        val id = resources.getIdentifier(name, "string", packageName)
        if (id == 0) return null
        return getString(id).takeIf { it.isNotBlank() }
    }
}
