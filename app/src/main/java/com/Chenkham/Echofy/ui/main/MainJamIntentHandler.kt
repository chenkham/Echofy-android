package com.Chenkham.Echofy.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.Chenkham.Echofy.jam.JamRoomCode
import com.Chenkham.Echofy.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles incoming Together / Jam intents, deep links, and control-plane configuration overrides.
 */
object MainJamIntentHandler {
    val JamRegistryUrlKey = stringPreferencesKey("jam_registry_url_override")
    val JamInviteBaseUrlKey = stringPreferencesKey("jam_invite_base_url_override")
    val JamAllocationUrlKey = stringPreferencesKey("jam_room_allocate_url_override")

    fun extractJamRoomCode(uri: Uri): String? {
        val normalizedScheme = uri.scheme?.lowercase()
        val normalizedHost = uri.host?.lowercase()
        val cleanSegments = uri.pathSegments.filterNot { it.equals("echofy-website", ignoreCase = true) }

        val candidate =
            when {
                normalizedScheme == "echofy" && normalizedHost in setOf("jam", "r", "together") ->
                    uri.pathSegments.firstOrNull() ?: uri.getQueryParameter("code") ?: uri.getQueryParameter("room")

                normalizedScheme == "echofy" && normalizedHost in setOf("join", "open") ->
                    uri.getQueryParameter("room")
                        ?: uri.getQueryParameter("roomCode")
                        ?: uri.getQueryParameter("jam")
                        ?: uri.getQueryParameter("code")

                cleanSegments.firstOrNull()?.lowercase() in setOf("jam", "r", "together") ->
                    cleanSegments.getOrNull(1)

                else ->
                    uri.getQueryParameter("jam")
                        ?: uri.getQueryParameter("room")
                        ?: uri.getQueryParameter("together")
                        ?: uri.getQueryParameter("code")
            }

        return candidate
            ?.trim()
            ?.uppercase()
            ?.takeIf { JamRoomCode.parse(it) != null }
    }

    fun handleJamJoinIntent(
        context: Context,
        intent: Intent,
        onPendingJamRoomCode: (String) -> Unit,
    ): Boolean {
        val rawText = intent.extras?.getString(Intent.EXTRA_TEXT)
        val uriCandidates =
            buildList {
                intent.data?.let(::add)
                rawText?.let { text ->
                    runCatching { Uri.parse(text.trim()) }.getOrNull()?.let(::add)
                }
            }

        val roomCode =
            uriCandidates
                .firstNotNullOfOrNull(::extractJamRoomCode)
                ?: rawText
                    ?.trim()
                    ?.uppercase()
                    ?.takeIf { JamRoomCode.parse(it) != null }
                ?: return false

        onPendingJamRoomCode(roomCode)
        Toast.makeText(context, "Together invite ready", Toast.LENGTH_SHORT).show()
        return true
    }

    fun handleJamConfigIntent(
        context: Context,
        intent: Intent,
        scope: CoroutineScope,
        dataStore: DataStore<Preferences>,
        playerConnectionProvider: () -> PlayerConnection?,
    ): Boolean {
        val uri = intent.data ?: return false
        if (uri.scheme?.lowercase() != "echofy" || uri.host?.lowercase() != "config") {
            return false
        }

        val registryUrl = uri.getQueryParameter("jamRegistryUrl") ?: uri.getQueryParameter("registryUrl")
        val inviteBaseUrl = uri.getQueryParameter("jamInviteBaseUrl") ?: uri.getQueryParameter("inviteBaseUrl")
        val allocationUrl = uri.getQueryParameter("jamRoomAllocateUrl") ?: uri.getQueryParameter("roomAllocateUrl")

        if (registryUrl.isNullOrBlank() && inviteBaseUrl.isNullOrBlank() && allocationUrl.isNullOrBlank()) {
            Toast.makeText(context, "No Jam config found in this link", Toast.LENGTH_SHORT).show()
            return true
        }

        scope.launch {
            dataStore.edit { preferences ->
                registryUrl?.trim()?.takeIf { it.isNotBlank() }?.let { preferences[JamRegistryUrlKey] = it }
                inviteBaseUrl?.trim()?.takeIf { it.isNotBlank() }?.let { preferences[JamInviteBaseUrlKey] = it.removeSuffix("/") }
                allocationUrl?.trim()?.takeIf { it.isNotBlank() }?.let { preferences[JamAllocationUrlKey] = it }
            }
            playerConnectionProvider()?.service?.jamSessionManager?.refreshRegistry(forceRefresh = true)
            Toast.makeText(context, "Echofy Together control plane connected", Toast.LENGTH_SHORT).show()
        }
        return true
    }
}
