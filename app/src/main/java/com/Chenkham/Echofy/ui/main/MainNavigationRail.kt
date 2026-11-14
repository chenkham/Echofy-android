package com.Chenkham.Echofy.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ui.screens.Screens

/**
 * Tablet, foldable, and landscape side navigation rail for Echofy.
 */
@Composable
fun MainNavigationRail(
    navController: NavHostController,
    navigationItems: List<Screens>,
    navBackStackEntry: NavBackStackEntry?,
    pureBlack: Boolean,
    slimNav: Boolean,
    onNavigateToScreen: (Screens) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier =
            modifier
                .fillMaxHeight()
                .width(80.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                )
                .background(
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                if (pureBlack) {
                                    Color.Black
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.95f,
                                    )
                                },
                                if (pureBlack) {
                                    Color.Black
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.75f,
                                    )
                                },
                            ),
                    ),
                ),
        containerColor = Color.Transparent,
        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        header = {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.echofy),
                contentDescription = "Echofy",
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape),
            )
            Spacer(Modifier.height(12.dp))
        },
    ) {
        navigationItems.fastForEach { screen ->
            val isSelected =
                navBackStackEntry?.destination?.hierarchy?.any {
                    it.route == screen.route
                } == true

            NavigationRailItem(
                selected = isSelected,
                icon = {
                    Icon(
                        painter =
                            painterResource(
                                id =
                                    if (isSelected) {
                                        screen.iconIdActive
                                    } else {
                                        screen.iconIdInactive
                                    },
                            ),
                        contentDescription = stringResource(screen.titleId),
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    if (!slimNav) {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                colors =
                    NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                onClick = {
                    if (isSelected) {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "scrollToTop",
                            true,
                        )
                    } else {
                        onNavigateToScreen(screen)
                    }
                },
            )
        }
    }
}
