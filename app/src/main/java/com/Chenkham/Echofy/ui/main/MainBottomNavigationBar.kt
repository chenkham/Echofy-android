package com.Chenkham.Echofy.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.NavigationBarHeight
import com.Chenkham.Echofy.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated bottom navigation bar supporting double-tap exploration, scroll-to-top, and sliding offsets.
 */
@Composable
fun MainBottomNavigationBar(
    navController: NavHostController,
    navigationItems: List<Screens>,
    navBackStackEntry: NavBackStackEntry?,
    navigationBarHeight: Dp,
    bottomInset: Dp,
    bottomInsetDp: Dp,
    playerBottomSheetProgress: Float,
    pureBlack: Boolean,
    insetBg: Color,
    slimNav: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onResetScroll: () -> Unit,
    onNavigateToScreen: (Screens) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        NavigationBar(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .align(Alignment.BottomCenter)
                    .offset {
                        if (navigationBarHeight == 0.dp) {
                            IntOffset(
                                x = 0,
                                y = (bottomInset + NavigationBarHeight).coerceAtLeast(0.dp).roundToPx(),
                            )
                        } else {
                            val slideOffset = (bottomInset + NavigationBarHeight) * playerBottomSheetProgress.coerceIn(0f, 1f)
                            val hideOffset = (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight).coerceAtLeast(0f)
                            IntOffset(
                                x = 0,
                                y = (slideOffset + hideOffset).coerceAtLeast(0.dp).roundToPx(),
                            )
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f),
                                ),
                        ),
                    ),
            containerColor = Color.Transparent,
            contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            var lastTapTime by remember { mutableLongStateOf(0L) }
            var lastTappedIcon by remember { mutableStateOf<Int?>(null) }
            var navigateToExplore by remember { mutableStateOf(false) }

            navigationItems.fastForEach { screen ->
                val isSelected =
                    navBackStackEntry?.destination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                NavigationBarItem(
                    selected = isSelected,
                    icon = {
                        Icon(
                            painter =
                                painterResource(
                                    id = if (isSelected) screen.iconIdActive else screen.iconIdInactive,
                                ),
                            contentDescription = stringResource(screen.titleId),
                        )
                    },
                    label = {
                        if (!slimNav) {
                            Text(
                                text = stringResource(screen.titleId),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = {
                        val currentTapTime = System.currentTimeMillis()
                        val timeSinceLastTap = currentTapTime - lastTapTime
                        val isDoubleTap =
                            screen.titleId == R.string.explore &&
                                lastTappedIcon == R.string.explore &&
                                timeSinceLastTap < 300L

                        lastTapTime = currentTapTime
                        lastTappedIcon = screen.titleId

                        if (screen.titleId == R.string.explore) {
                            if (isDoubleTap) {
                                onActiveChange(true)
                                navigateToExplore = false
                            } else {
                                navigateToExplore = true
                                coroutineScope.launch {
                                    delay(300L)
                                    if (navigateToExplore) {
                                        onNavigateToScreen(screen)
                                    }
                                }
                            }
                        } else {
                            if (isSelected) {
                                navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                onResetScroll()
                            } else {
                                onNavigateToScreen(screen)
                            }
                        }
                    },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .background(insetBg)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(bottomInsetDp),
        )
    }
}
