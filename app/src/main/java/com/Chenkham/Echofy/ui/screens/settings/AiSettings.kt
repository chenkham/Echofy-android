package com.Chenkham.Echofy.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ai.AiPresets
import com.Chenkham.Echofy.ai.AiSettingsDataStore
import com.Chenkham.Echofy.constants.VoiceControlEnabledKey
import com.Chenkham.Echofy.ui.component.PreferenceEntry
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.screens.ai.MultiProviderSettingsDialog
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.launch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Dedicated AI & Voice Settings Screen.
 * Integrated into the Echofy Settings navigation hierarchy.
 * Manages voice commands, provider profiles, TTS, and context privacy settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiSettingsDataStore = remember { AiSettingsDataStore(context) }

    val wakeWordEnabled by aiSettingsDataStore.isWakeWordEnabled.collectAsState(initial = false)
    val drivingModeEnabled by aiSettingsDataStore.isDrivingModeEnabled.collectAsState(initial = false)
    val ttsEnabled by aiSettingsDataStore.isTtsEnabled.collectAsState(initial = true)
    val includeHistoryContext by aiSettingsDataStore.isIncludeHistoryContextEnabled.collectAsState(initial = true)
    val includeLikesContext by aiSettingsDataStore.isIncludeLikesContextEnabled.collectAsState(initial = true)

    val profiles by aiSettingsDataStore.profiles.collectAsState(initial = AiPresets.BUILTIN_PROFILES)
    val activeProfileId by aiSettingsDataStore.activeProfileId.collectAsState(initial = "preset_groq")

    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    val activeProfileText = activeProfile?.let { "${it.name} (${it.modelName})" } ?: "No provider selected"

    var showProviderDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    /** Quiet-environment advisory shown before driving mode takes the microphone. */
    var showDrivingModeInfoDialog by remember { mutableStateOf(false) }

    /** Which toggle triggered the permission request, so the grant enables that one. */
    var pendingEnableDrivingMode by remember { mutableStateOf(false) }

    val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
        val forDrivingMode = pendingEnableDrivingMode
        pendingEnableDrivingMode = false
        coroutineScope.launch {
            if (forDrivingMode) {
                aiSettingsDataStore.setDrivingModeEnabled(audioGranted)
            } else {
                aiSettingsDataStore.setWakeWordEnabled(audioGranted)
            }
        }
    }

    val (voiceControlEnabled, setVoiceControlEnabled) = rememberPreference(VoiceControlEnabledKey, defaultValue = false)

    SettingsPage(
        title = "AI & Voice",
        navController = navController,
        scrollBehavior = scrollBehavior,
    ) {
        // Section 1: Voice Activation
        SettingsGeneralCategory(
            title = "Voice Activation",
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Hey Jarvis Wake Word") },
                        description = if (drivingModeEnabled) {
                            "Required by Driving Mode \u2014 turn Driving Mode off to disable"
                        } else {
                            "Say \"Hey Jarvis\", then your command. The mic stays idle until the wake word is heard."
                        },
                        icon = { Icon(painterResource(R.drawable.auto_awesome), contentDescription = null) },
                        checked = wakeWordEnabled,
                        // Driving mode depends on the assistant, so it is locked on.
                        isEnabled = !drivingModeEnabled,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                coroutineScope.launch {
                                    aiSettingsDataStore.setWakeWordEnabled(false)
                                }
                            } else {
                                val audioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (audioPermission) {
                                    coroutineScope.launch {
                                        aiSettingsDataStore.setWakeWordEnabled(true)
                                    }
                                } else {
                                    pendingEnableDrivingMode = false
                                    showPermissionRationaleDialog = true
                                }
                            }
                        }
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Driving Mode") },
                        description = "Hands-free: no wake word needed, just speak your command. Best used alone in a quiet car \u2014 background voices may trigger it.",
                        icon = { Icon(painterResource(R.drawable.assistant), contentDescription = null) },
                        checked = drivingModeEnabled,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                coroutineScope.launch {
                                    aiSettingsDataStore.setDrivingModeEnabled(false)
                                }
                            } else {
                                val audioPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (audioPermission) {
                                    // Explain the trade-offs before enabling always-on
                                    // recognition, rather than silently taking the mic.
                                    showDrivingModeInfoDialog = true
                                } else {
                                    // Remember what the user actually asked for so the
                                    // permission callback enables the right toggle.
                                    pendingEnableDrivingMode = true
                                    showPermissionRationaleDialog = true
                                }
                            }
                        }
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Google Assistant Integration") },
                        description = "Let Google Assistant play music here, e.g. \"Play Bohemian Rhapsody on Echofy\"",
                        icon = { Icon(painterResource(R.drawable.settings), contentDescription = null) },
                        checked = voiceControlEnabled,
                        onCheckedChange = setVoiceControlEnabled
                    )
                }
            )
        )

        Spacer(Modifier.padding(8.dp))

        // Section 2: AI Provider
        SettingsGeneralCategory(
            title = "AI Provider",
            items = listOf(
                {
                    PreferenceEntry(
                        title = { Text("Manage AI Providers") },
                        description = activeProfileText,
                        icon = { Icon(painterResource(R.drawable.tune), contentDescription = null) },
                        onClick = { showProviderDialog = true }
                    )
                }
            )
        )

        Spacer(Modifier.padding(8.dp))

        // Section 3: AI Behavior
        SettingsGeneralCategory(
            title = "AI Behavior",
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Voice Replies (Text-to-Speech)") },
                        description = "Read AI responses aloud using system voice synthesizer",
                        icon = { Icon(painterResource(R.drawable.volume_up), contentDescription = null) },
                        checked = ttsEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                aiSettingsDataStore.setTtsEnabled(checked)
                            }
                        }
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Include Listening History") },
                        description = "Provide recent history context to AI for personalized chat",
                        icon = { Icon(painterResource(R.drawable.info), contentDescription = null) },
                        checked = includeHistoryContext,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                aiSettingsDataStore.setIncludeHistoryContextEnabled(checked)
                            }
                        }
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Include Liked Songs") },
                        description = "Provide liked songs context to AI for better recommendations",
                        icon = { Icon(painterResource(R.drawable.info), contentDescription = null) },
                        checked = includeLikesContext,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                aiSettingsDataStore.setIncludeLikesContextEnabled(checked)
                            }
                        }
                    )
                }
            )
        )

        Spacer(Modifier.padding(8.dp))

        // Section 4: Data & Privacy
        SettingsGeneralCategory(
            title = "Data & Privacy",
            items = listOf(
                {
                    PreferenceEntry(
                        title = { Text("Clear All Chat History") },
                        description = "Delete all saved AI chat sessions and messages",
                        icon = { Icon(painterResource(R.drawable.delete), contentDescription = null) },
                        onClick = { showClearHistoryDialog = true }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text("Privacy Info") },
                        description = "API keys are stored locally on-device and never shared.",
                        icon = { Icon(painterResource(R.drawable.security), contentDescription = null) },
                        onClick = null
                    )
                }
            )
        )
    }

    if (showProviderDialog) {
        MultiProviderSettingsDialog(
            profiles = profiles,
            activeProfileId = activeProfileId,
            onDismiss = { showProviderDialog = false },
            onSaveProfiles = { updatedProfiles, newActiveId ->
                coroutineScope.launch {
                    aiSettingsDataStore.saveProfiles(updatedProfiles, newActiveId)
                }
                showProviderDialog = false
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Chat History") },
            text = { Text("Are you sure you want to clear all chat history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val aiChatPrefs = context.getSharedPreferences("echofy_ai_chat_prefs", Context.MODE_PRIVATE)
                        val chatSessionsPrefs = context.getSharedPreferences("echofy_chat_sessions_prefs", Context.MODE_PRIVATE)
                        aiChatPrefs.edit().clear().apply()
                        chatSessionsPrefs.edit().clear().apply()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("Echofy needs microphone permission to listen for the 'Hey Jarvis' wake word on-device while playing music. Audio is processed strictly locally on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationaleDialog = false
                        permissionLauncher.launch(permissionsToRequest)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDrivingModeInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDrivingModeInfoDialog = false },
            title = { Text("Before you start driving") },
            text = {
                Text(
                    "Driving Mode listens continuously and runs commands without the " +
                        "\"Hey Jarvis\" wake word, so you never touch your phone.\n\n" +
                        "For best results, use it alone in a quiet car. Passengers " +
                        "talking, radio audio or road noise can be picked up as " +
                        "commands.\n\nThe wake word stays enabled while Driving Mode is on."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDrivingModeInfoDialog = false
                        coroutineScope.launch {
                            aiSettingsDataStore.setDrivingModeEnabled(true)
                        }
                    }
                ) {
                    Text("Turn On")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrivingModeInfoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
