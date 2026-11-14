package com.Chenkham.Echofy.ui.main

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.SearchSource
import com.Chenkham.Echofy.ui.screens.Screens
import com.Chenkham.Echofy.ui.screens.musicrecognition.openMusicRecognition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldTopBar(
    activity: Activity,
    navController: NavHostController,
    navBackStackEntry: NavBackStackEntry?,
    shouldShowTopBar: Boolean,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    searchBarScrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    navigationItems: List<Screens>,
    searchSource: SearchSource,
    onSearchSourceChange: (SearchSource) -> Unit,
    searchBarFocusRequester: FocusRequester,
    pureBlack: Boolean,
    playerBottomSheetDismissed: Boolean,
) {
    val hasUnreadNotifications = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val prefs = activity.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
        val notifications = prefs.getStringSet("notifications", emptySet()) ?: emptySet()
        hasUnreadNotifications.value = notifications.any { it.contains("|false") }
    }

    if (shouldShowTopBar) {
        MainTopAppBar(
            scrollBehavior = searchBarScrollBehavior,
            hasUnreadNotifications = hasUnreadNotifications.value,
            onOpenMusicRecognition = {
                runCatching { navController.openMusicRecognition() }
                    .onFailure { Toast.makeText(activity, R.string.navigation_error, Toast.LENGTH_SHORT).show() }
            },
            onOpenNotifications = {
                runCatching { navController.navigate("notifications") }
                    .onFailure { Toast.makeText(activity, R.string.navigation_error, Toast.LENGTH_SHORT).show() }
            },
            onOpenSearch = { onActiveChange(true) },
            onOpenSettings = {
                runCatching { navController.navigate("settings") }
                    .onFailure { Toast.makeText(activity, R.string.navigation_error, Toast.LENGTH_SHORT).show() }
            },
            versionName = latestVersionName,
        )
    }
    if (active || navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
        MainSearchLayer(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = active,
            onActiveChange = onActiveChange,
            navController = navController,
            navBackStackEntry = navBackStackEntry,
            navigationItems = navigationItems,
            searchSource = searchSource,
            onSearchSourceChange = onSearchSourceChange,
            focusRequester = searchBarFocusRequester,
            pureBlack = pureBlack,
            playerBottomSheetDismissed = playerBottomSheetDismissed,
        )
    }
}
