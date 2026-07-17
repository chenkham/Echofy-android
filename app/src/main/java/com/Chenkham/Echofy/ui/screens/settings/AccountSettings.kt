package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ui.component.PreferenceEntry
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.Chenkham.Echofy.App.Companion.forgetAccount
import com.Chenkham.Echofy.constants.AccountChannelHandleKey
import com.Chenkham.Echofy.constants.AccountEmailKey
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.DataSyncIdKey
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.constants.UseLoginForBrowse
import com.Chenkham.Echofy.constants.VisitorDataKey
import com.Chenkham.Echofy.constants.YtmSyncKey
import com.Chenkham.Echofy.constants.LastFmSessionKeyKey
import com.Chenkham.Echofy.constants.LastFmUsernameKey
import com.Chenkham.Echofy.ui.component.InfoLabel
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.component.TextFieldDialog
import com.Chenkham.Echofy.utils.rememberPreference
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.utils.parseCookieString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val (accountName, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, defaultValue = true)
    val lastFmSessionKey by rememberPreference(LastFmSessionKeyKey, "")
    val lastFmUsername by rememberPreference(LastFmUsernameKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val getAccountDisplayName =
        remember(accountName, accountEmail, accountChannelHandle, isLoggedIn) {
            when {
                !isLoggedIn -> ""
                accountName.isNotBlank() -> accountName
                accountEmail.isNotBlank() -> accountEmail.substringBefore("@")
                accountChannelHandle.isNotBlank() -> accountChannelHandle
                else -> "No username"
            }
        }
    val getAccountDescription = remember(accountEmail, accountChannelHandle, isLoggedIn) {
        when {
            !isLoggedIn -> null
            accountEmail.isNotBlank() -> accountEmail
            accountChannelHandle.isNotBlank() -> accountChannelHandle
            else -> null
        }
    }

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(R.string.account),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = "Integration",
            items = listOf(
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.discord_integration)) },
                        description = stringResource(R.string.discord_gateway_description),
                        icon = { Icon(painterResource(R.drawable.discord), null) },
                        onClick = { navController.navigate("settings/discord") }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.lastfm_integration)) },
                        description = if (lastFmSessionKey.isBlank()) {
                            stringResource(R.string.not_logged_in)
                        } else {
                            lastFmUsername.ifBlank { stringResource(R.string.connected) }
                        },
                        icon = { Icon(painterResource(R.drawable.ic_lastfm), null) },
                        onClick = { navController.navigate("settings/lastfm") }
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = "YouTube Music (Cookie)",
            items = listOf(
                {
                    PreferenceEntry(
                        title = {
                            Text(
                                if (isLoggedIn) {
                                    getAccountDisplayName.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.login)
                                } else {
                                    stringResource(R.string.login)
                                }
                            )
                        },
                        description = if (isLoggedIn) getAccountDescription else null,
                        icon = { Icon(painterResource(R.drawable.login), null) },
                        trailingContent = {
                            if (isLoggedIn) {
                                OutlinedButton(onClick = {
                                    onInnerTubeCookieChange("")
                                    onAccountNameChange("")
                                    onAccountEmailChange("")
                                    onAccountChannelHandleChange("")
                                    onVisitorDataChange("")
                                    onDataSyncIdChange("")
                                    forgetAccount(context)
                                }) {
                                    Text(stringResource(R.string.logout))
                                }
                            }
                        },
                        onClick = { if (!isLoggedIn) navController.navigate("login") }
                    )
                },
                {
                    if (showTokenEditor) {
                        val text =
                            "***INNERTUBE COOKIE*** =${innerTubeCookie}\n\n***VISITOR DATA*** =${visitorData}\n\n***DATASYNC ID*** =${dataSyncId}\n\n***ACCOUNT NAME*** =${accountName}\n\n***ACCOUNT EMAIL*** =${accountEmail}\n\n***ACCOUNT CHANNEL HANDLE*** =${accountChannelHandle}"
                        TextFieldDialog(
                            initialTextFieldValue = TextFieldValue(text),
                            onDone = { data ->
                                data.split("\n").forEach {
                                    when {
                                        it.startsWith("***INNERTUBE COOKIE*** =") -> {
                                            onInnerTubeCookieChange(
                                                it.substringAfter("***INNERTUBE COOKIE*** =").trim()
                                            )
                                        }

                                        it.startsWith("***VISITOR DATA*** =") -> {
                                            onVisitorDataChange(
                                                it.substringAfter("***VISITOR DATA*** =").trim()
                                            )
                                        }

                                        it.startsWith("***DATASYNC ID*** =") -> {
                                            onDataSyncIdChange(
                                                it.substringAfter("***DATASYNC ID*** =").trim()
                                            )
                                        }

                                        it.startsWith("***ACCOUNT NAME*** =") -> {
                                            onAccountNameChange(
                                                it.substringAfter("***ACCOUNT NAME*** =").trim()
                                            )
                                        }

                                        it.startsWith("***ACCOUNT EMAIL*** =") -> {
                                            onAccountEmailChange(
                                                it.substringAfter("***ACCOUNT EMAIL*** =").trim()
                                            )
                                        }

                                        it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> {
                                            onAccountChannelHandleChange(
                                                it.substringAfter("***ACCOUNT CHANNEL HANDLE*** =").trim()
                                            )
                                        }
                                    }
                                }
                            },
                            onDismiss = { showTokenEditor = false },
                            singleLine = false,
                            maxLines = 20,
                            isInputValid = { input ->
                                input.isNotEmpty() &&
                                    try {
                                        val cookieLine = input.lines()
                                            .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                                        if (cookieLine != null) {
                                            val cookie =
                                                cookieLine.substringAfter("***INNERTUBE COOKIE*** =")
                                                    .trim()
                                            cookie.isEmpty() || "SAPISID" in parseCookieString(cookie)
                                        } else {
                                            false
                                        }
                                    } catch (e: Exception) {
                                        false
                                    }
                            },
                            extraContent = {
                                InfoLabel(text = stringResource(R.string.token_adv_login_description))
                            }
                        )
                    }
                },
                {
                    PreferenceEntry(
                        title = {
                            if (!isLoggedIn) {
                                Text(stringResource(R.string.advanced_login))
                            } else if (showToken) {
                                Text(stringResource(R.string.token_shown))
                            } else {
                                Text(stringResource(R.string.token_hidden))
                            }
                        },
                        icon = { Icon(painterResource(R.drawable.token), null) },
                        onClick = {
                            if (!isLoggedIn) {
                                showTokenEditor = true
                            } else if (!showToken) {
                                showToken = true
                            } else {
                                showTokenEditor = true
                            }
                        },
                    )
                },
                {
                    if (isLoggedIn) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.use_login_for_browse)) },
                            description = stringResource(R.string.use_login_for_browse_desc),
                            icon = { Icon(painterResource(R.drawable.person), null) },
                            checked = useLoginForBrowse,
                            onCheckedChange = {
                                YouTube.useLoginForBrowse = it
                                onUseLoginForBrowseChange(it)
                            }
                        )
                    }
                },
                {
                    if (isLoggedIn) {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.ytm_sync)) },
                            icon = { Icon(painterResource(R.drawable.cached), null) },
                            checked = ytmSync,
                            onCheckedChange = onYtmSyncChange,
                            isEnabled = isLoggedIn
                        )
                    }
                },
            )
        )
    }
}
