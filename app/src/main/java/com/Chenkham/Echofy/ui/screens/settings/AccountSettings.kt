package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.utils.parseCookieString
import com.Chenkham.Echofy.App.Companion.forgetAccount
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AccountChannelHandleKey
import com.Chenkham.Echofy.constants.AccountEmailKey
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.DataSyncIdKey
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.constants.UseLoginForBrowse
import com.Chenkham.Echofy.constants.VisitorDataKey
import com.Chenkham.Echofy.constants.YtmSyncKey
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.InfoLabel
import com.Chenkham.Echofy.ui.component.PreferenceEntry
import com.Chenkham.Echofy.ui.component.PreferenceGroupTitle
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.component.TextFieldDialog
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AccountViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val googleUser by viewModel.user.collectAsState()

    val (accountName, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(
        AccountChannelHandleKey,
        ""
    )
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

    // Function to securely obtain the account name
    val getAccountDisplayName =
        remember(accountName, accountEmail, accountChannelHandle, isLoggedIn) {
            when {
                !isLoggedIn -> ""
                accountName.isNotBlank() -> accountName
                accountEmail.isNotBlank() -> accountEmail.substringBefore("@")
                accountChannelHandle.isNotBlank() -> accountChannelHandle
                else -> "No username" // Fallback to prevent crashes
            }
        }

    // Function to securely obtain the account description
    val getAccountDescription = remember(accountEmail, accountChannelHandle, isLoggedIn) {
        when {
            !isLoggedIn -> null
            accountEmail.isNotBlank() -> accountEmail
            accountChannelHandle.isNotBlank() -> accountChannelHandle
            else -> null
        }
    }

    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, defaultValue = true)

    var showToken: Boolean by remember {
        mutableStateOf(false)
    }
    var showTokenEditor by remember {
        mutableStateOf(false)
    }
    var showSignOutDialog by remember { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(R.string.account),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        // NEW: App Account Section
        SettingsGeneralCategory(
            title = "App Account",
            items = listOf(
                {
                    if (googleUser != null) {
                        PreferenceEntry(
                            title = { Text(googleUser!!.displayName ?: "User") },
                            description = googleUser!!.email,
                            icon = {
                                coil.compose.AsyncImage(
                                    model = googleUser!!.photoUrl,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    error = painterResource(R.drawable.person),
                                    placeholder = painterResource(R.drawable.person)
                                )
                            },
                            trailingContent = {
                                OutlinedButton(onClick = { showSignOutDialog = true }) {
                                    Text("Sign Out")
                                }
                            }
                        )
                    } else {
                        PreferenceEntry(
                            title = { Text("Sign in with Google") },
                            description = "Sync your preferences and data",
                            icon = { Icon(painterResource(R.drawable.login), null) },
                            onClick = { navController.navigate("sign_in?chained=true") }
                        )
                    }
                }
            )
        )

        SettingsGeneralCategory(
            title = "YouTube Music (Cookie)", // Clarified title
            items = listOf(
                {PreferenceEntry(
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
                                // Clear all account data
                                onInnerTubeCookieChange("")
                                onAccountNameChange("")
                                onAccountEmailChange("")
                                onAccountChannelHandleChange("")
                                onVisitorDataChange("")
                                onDataSyncIdChange("")
                                forgetAccount(context)
                            }
                            ) {
                                Text(stringResource(R.string.logout))
                            }
                        }
                    },
                    onClick = { if (!isLoggedIn) navController.navigate("login") }
                )},

                {if (showTokenEditor) {
                    val text =
                        "***INNERTUBE COOKIE*** =${innerTubeCookie}\n\n***VISITOR DATA*** =${visitorData}\n\n***DATASYNC ID*** =${dataSyncId}\n\n***ACCOUNT NAME*** =${accountName}\n\n***ACCOUNT EMAIL*** =${accountEmail}\n\n***ACCOUNT CHANNEL HANDLE*** =${accountChannelHandle}"
                    TextFieldDialog(
                        modifier = Modifier,
                        initialTextFieldValue = TextFieldValue(text),
                        onDone = { data ->
                            data.split("\n").forEach {
                                when {
                                    it.startsWith("***INNERTUBE COOKIE*** =") -> {
                                        val cookie =
                                            it.substringAfter("***INNERTUBE COOKIE*** =").trim()
                                        onInnerTubeCookieChange(cookie)
                                    }

                                    it.startsWith("***VISITOR DATA*** =") -> {
                                        val visitorDataValue =
                                            it.substringAfter("***VISITOR DATA*** =").trim()
                                        onVisitorDataChange(visitorDataValue)
                                    }

                                    it.startsWith("***DATASYNC ID*** =") -> {
                                        val dataSyncIdValue =
                                            it.substringAfter("***DATASYNC ID*** =").trim()
                                        onDataSyncIdChange(dataSyncIdValue)
                                    }

                                    it.startsWith("***ACCOUNT NAME*** =") -> {
                                        val name = it.substringAfter("***ACCOUNT NAME*** =").trim()
                                        onAccountNameChange(name)
                                    }

                                    it.startsWith("***ACCOUNT EMAIL*** =") -> {
                                        val email = it.substringAfter("***ACCOUNT EMAIL*** =").trim()
                                        onAccountEmailChange(email)
                                    }

                                    it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> {
                                        val handle =
                                            it.substringAfter("***ACCOUNT CHANNEL HANDLE*** =").trim()
                                        onAccountChannelHandleChange(handle)
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
                }},

                {PreferenceEntry(
                    title = {
                        if (!isLoggedIn) {
                            Text(stringResource(R.string.advanced_login))
                        } else {
                            if (showToken) {
                                Text(stringResource(R.string.token_shown))
                            } else {
                                Text(stringResource(R.string.token_hidden))
                            }
                        }
                    },
                    icon = { Icon(painterResource(R.drawable.token), null) },
                    onClick = {
                        if (!isLoggedIn) {
                            showTokenEditor = true
                        } else {
                            if (!showToken) {
                                showToken = true
                            } else {
                                showTokenEditor = true
                            }
                        }
                    },
                )},

                {if (isLoggedIn) {
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
                }},

                {if (isLoggedIn) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ytm_sync)) },
                        icon = { Icon(painterResource(R.drawable.cached), null) },
                        checked = ytmSync,
                        onCheckedChange = onYtmSyncChange,
                        isEnabled = isLoggedIn
                    )
                }},

                )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.discord),
            items = listOf(
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.discord_integration)) },
                    icon = { Icon(painterResource(R.drawable.discord), null) },
                    onClick = { navController.navigate("settings/discord") }
                )}
            )
        )

        if (showSignOutDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out?") },
                text = { Text("Are you sure you want to sign out from the app?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showSignOutDialog = false
                            viewModel.signOut()
                            navController.navigate("sign_in") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    ) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}