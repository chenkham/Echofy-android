package com.Chenkham.Echofy.ui.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.Chenkham.Echofy.ai.AiSettingsDataStore
import com.Chenkham.Echofy.ai.EchofyAiManager
import com.Chenkham.Echofy.ai.WakeWordService
import com.Chenkham.Echofy.playback.PlayerConnection

/**
 * Manages app-wide hands-free Hey Jarvis / Echofy AI wake word detection and driving mode.
 */
@Composable
fun rememberMainWakeWordManager(
    context: Context,
    playerConnection: PlayerConnection?,
): EchofyAiManager {
    val wakeWordAiManager = remember(context) { EchofyAiManager.getInstance(context) }
    val aiSettingsStore = remember(context) { AiSettingsDataStore(context) }
    val wakeWordEnabled by aiSettingsStore.isWakeWordEnabled.collectAsState(initial = false)
    val drivingModeEnabled by aiSettingsStore.isDrivingModeEnabled.collectAsState(initial = false)
    val listeningEnabled = wakeWordEnabled || drivingModeEnabled

    LaunchedEffect(listeningEnabled, drivingModeEnabled, playerConnection) {
        wakeWordAiManager.attachPlayerConnection(playerConnection)
        if (listeningEnabled) {
            wakeWordAiManager.setDrivingMode(drivingModeEnabled)
            WakeWordService.start(context, drivingModeEnabled)
            wakeWordAiManager.startHeyCommandListener(playerConnection)
        } else {
            wakeWordAiManager.setDrivingMode(false)
            WakeWordService.stop(context)
            wakeWordAiManager.stopHeyCommandListener()
        }
    }

    return wakeWordAiManager
}
