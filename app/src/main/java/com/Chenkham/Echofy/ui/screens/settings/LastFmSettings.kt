package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.EnableLastFMScrobblingKey
import com.Chenkham.Echofy.constants.LastFMUseNowPlaying
import com.Chenkham.Echofy.constants.LastFmSessionKeyKey
import com.Chenkham.Echofy.constants.LastFmUsernameKey
import com.Chenkham.Echofy.constants.ScrobbleDelayPercentKey
import com.Chenkham.Echofy.constants.ScrobbleDelaySecondsKey
import com.Chenkham.Echofy.constants.ScrobbleMinSongDurationKey
import com.Chenkham.Echofy.lastfm.LastFmException
import com.Chenkham.Echofy.lastfm.LastFmGateway
import com.Chenkham.Echofy.ui.component.PreferenceEntry
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.component.TextFieldDialog
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var sessionKey by rememberPreference(LastFmSessionKeyKey, "")
    var username by rememberPreference(LastFmUsernameKey, "")
    var scrobbleEnabled by rememberPreference(EnableLastFMScrobblingKey, false)
    val (nowPlayingEnabled, onNowPlayingEnabledChange) = rememberPreference(LastFMUseNowPlaying, true)
    var delayPercent by rememberPreference(ScrobbleDelayPercentKey, 50f)
    var minSongDuration by rememberPreference(ScrobbleMinSongDurationKey, 30)
    var delaySeconds by rememberPreference(ScrobbleDelaySecondsKey, 240)

    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showDelaySecondsDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val isLoggedIn = sessionKey.isNotBlank()

    if (showLoginDialog) {
        var tempUsername by rememberSaveable { mutableStateOf("") }
        var tempPassword by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                if (!isLoggingIn) {
                    showLoginDialog = false
                    loginError = null
                }
            },
            title = { Text("Login to Last.fm") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = {
                            tempUsername = it
                            loginError = null
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        enabled = !isLoggingIn,
                    )
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = {
                            tempPassword = it
                            loginError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isLoggingIn,
                    )

                    loginError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (isLoggingIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Logging in...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempUsername.isBlank() || tempPassword.isBlank()) {
                            loginError = "Please enter both username and password"
                            return@TextButton
                        }

                        isLoggingIn = true
                        loginError = null

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val session = LastFmGateway.getMobileSession(tempUsername, tempPassword)
                                username = session.username
                                sessionKey = session.sessionKey
                                scrobbleEnabled = true

                                withContext(Dispatchers.Main) {
                                    isLoggingIn = false
                                    showLoginDialog = false
                                    loginError = null
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoggingIn = false
                                    loginError = when (e) {
                                        is LastFmException -> {
                                            when (e.code) {
                                                4 -> "Invalid username or password"
                                                else -> "Login failed: ${e.message}"
                                            }
                                        }
                                        else -> "Network error. Please check your connection."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isLoggingIn) {
                            showLoginDialog = false
                            loginError = null
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMinDurationDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.lastfm_min_song_duration)) },
            initialTextFieldValue = TextFieldValue(minSongDuration.toString()),
            onDone = { minSongDuration = it.toIntOrNull()?.coerceAtLeast(0) ?: minSongDuration },
            onDismiss = { showMinDurationDialog = false },
            isInputValid = { it.toIntOrNull() != null },
        )
    }

    if (showDelaySecondsDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.lastfm_scrobble_delay_seconds)) },
            initialTextFieldValue = TextFieldValue(delaySeconds.toString()),
            onDone = { delaySeconds = it.toIntOrNull()?.coerceAtLeast(0) ?: delaySeconds },
            onDismiss = { showDelaySecondsDialog = false },
            isInputValid = { it.toIntOrNull() != null },
        )
    }

    SettingsPage(
        title = stringResource(R.string.lastfm_integration),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.account),
            items = listOf(
                {
                    PreferenceEntry(
                        title = {
                            Text(
                                if (isLoggedIn) {
                                    username.ifBlank { stringResource(R.string.connected) }
                                } else {
                                    stringResource(R.string.not_logged_in)
                                }
                            )
                        },
                        description = if (isLoggedIn) {
                            stringResource(R.string.lastfm_ready_description)
                        } else {
                            "Connect your Last.fm account to scrobble tracks and publish now-playing updates."
                        },
                        icon = { Icon(painterResource(R.drawable.favorite), null) },
                        trailingContent = {
                            if (isLoggedIn) {
                                OutlinedButton(onClick = {
                                    sessionKey = ""
                                    username = ""
                                    scrobbleEnabled = false
                                }) {
                                    Text(stringResource(R.string.logout))
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    showLoginDialog = true
                                }) {
                                    Text("Login")
                                }
                            }
                        }
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.options),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.lastfm_enable_scrobbling)) },
                        description = stringResource(R.string.lastfm_enable_scrobbling_description),
                        icon = { Icon(painterResource(R.drawable.cached), null) },
                        checked = scrobbleEnabled,
                        onCheckedChange = { scrobbleEnabled = it },
                        isEnabled = isLoggedIn
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.lastfm_now_playing)) },
                        description = stringResource(R.string.lastfm_now_playing_description),
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = nowPlayingEnabled,
                        onCheckedChange = onNowPlayingEnabledChange,
                        isEnabled = isLoggedIn && scrobbleEnabled
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.lastfm_scrobble_delay_percent)) },
                        description = "${delayPercent.roundToInt()}%",
                        icon = { Icon(painterResource(R.drawable.schedule), null) },
                        content = {
                            Column {
                                Spacer(Modifier.height(6.dp))
                                Slider(
                                    value = delayPercent.coerceIn(10f, 90f),
                                    onValueChange = { delayPercent = it },
                                    valueRange = 10f..90f,
                                    steps = 15,
                                    enabled = isLoggedIn
                                )
                            }
                        },
                        isEnabled = isLoggedIn
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.lastfm_scrobble_delay_seconds)) },
                        description = "$delaySeconds s",
                        icon = { Icon(painterResource(R.drawable.schedule), null) },
                        onClick = { showDelaySecondsDialog = true },
                        isEnabled = isLoggedIn
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.lastfm_min_song_duration)) },
                        description = "$minSongDuration s",
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        onClick = { showMinDurationDialog = true },
                        isEnabled = isLoggedIn
                    )
                },
            )
        )
    }
}
