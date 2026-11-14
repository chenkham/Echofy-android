package com.Chenkham.Echofy.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

/**
 * Monitors user idle time and navigates to Always-On-Display screen when enabled.
 */
@Composable
fun MainAodAutoController(
    aodTimeoutSecs: Int,
    isPlaying: Boolean,
    navBackStackEntry: NavBackStackEntry?,
    navController: NavHostController,
    getLastInteractionTime: () -> Long,
    onResetInteractionTime: () -> Unit,
) {
    LaunchedEffect(aodTimeoutSecs, isPlaying, navBackStackEntry) {
        if (aodTimeoutSecs <= 0) return@LaunchedEffect
        while (true) {
            delay(1000L)
            val currentRoute = navBackStackEntry?.destination?.route
            val isAodScreen = currentRoute == "always_on_display"
            if (isAodScreen) {
                onResetInteractionTime()
            } else if (isPlaying) {
                val idleMs = System.currentTimeMillis() - getLastInteractionTime()
                if (idleMs >= aodTimeoutSecs * 1000L) {
                    onResetInteractionTime()
                    navController.navigate("always_on_display")
                }
            }
        }
    }
}
