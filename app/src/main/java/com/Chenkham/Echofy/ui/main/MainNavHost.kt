package com.Chenkham.Echofy.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.util.fastAny
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ui.screens.Screens
import com.Chenkham.Echofy.ui.screens.navigationBuilder

/**
 * Root navigation host managing screen routes and Spotify-grade spring transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost(
    navController: NavHostController,
    startDestinationRoute: String,
    topLevelScreens: List<String>,
    navigationItems: List<Screens>,
    navBackStackEntry: NavBackStackEntry?,
    searchBarScrollBehavior: TopAppBarScrollBehavior,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    adManager: AdManager,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestinationRoute,
        enterTransition = {
            if (initialState.destination.route in topLevelScreens &&
                targetState.destination.route in topLevelScreens
            ) {
                fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            } else {
                fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                    )
            }
        },
        exitTransition = {
            if (initialState.destination.route in topLevelScreens &&
                targetState.destination.route in topLevelScreens
            ) {
                fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            } else {
                fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                    slideOutHorizontally(
                        targetOffsetX = { -it / 5 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
            }
        },
        popEnterTransition = {
            if ((
                    initialState.destination.route in topLevelScreens ||
                        initialState.destination.route?.startsWith("search/") == true
                ) &&
                targetState.destination.route in topLevelScreens
            ) {
                fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            } else {
                fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                    )
            }
        },
        popExitTransition = {
            if ((
                    initialState.destination.route in topLevelScreens ||
                        initialState.destination.route?.startsWith("search/") == true
                ) &&
                targetState.destination.route in topLevelScreens
            ) {
                fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            } else {
                fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                    slideOutHorizontally(
                        targetOffsetX = { it / 5 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
            }
        },
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(
                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                        navBackStackEntry?.destination?.route?.startsWith("search/") == true
                    ) {
                        searchBarScrollBehavior.nestedScrollConnection
                    } else {
                        topAppBarScrollBehavior.nestedScrollConnection
                    },
                ),
    ) {
        navigationBuilder(
            navController,
            topAppBarScrollBehavior,
            latestVersionName,
            adManager = adManager,
        )
    }
}
