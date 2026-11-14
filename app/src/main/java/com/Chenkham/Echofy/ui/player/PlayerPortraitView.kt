package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.menu.PlayerMenu

@Composable
fun PlayerPortraitView(
    mediaMetadata: MediaMetadata?,
    playbackMode: PlaybackMode,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    isVideoAvailable: Boolean,
    enableListenTogether: Boolean,
    onBackgroundColor: Color,
    state: BottomSheetState,
    navController: NavController,
    queueSheetState: BottomSheetState,
    onOpenFullscreenLyrics: () -> Unit,
    onOpenJamSheet: () -> Unit,
    onShowDetailsDialog: () -> Unit,
    controlsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 750
    val topBarTopPadding = if (isSmallScreen) 24.dp else 36.dp
    val thumbnailPadding = if (isSmallScreen) 16.dp else 8.dp
    val menuState = LocalMenuState.current

    val consumeOverflowNestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity = available

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset = available
            }
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = queueSheetState.collapsedBound),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = topBarTopPadding, bottom = 0.dp),
        ) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                SongVideoSwitch(
                    selectedMode = playbackMode,
                    onModeChange = onPlaybackModeChange,
                    isVideoAvailable = isVideoAvailable,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { state.collapseSoft() },
                ) {
                    Image(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = stringResource(R.string.acc_minimize),
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(28.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (enableListenTogether) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onOpenJamSheet),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.group),
                                contentDescription = "Start Together",
                                colorFilter = ColorFilter.tint(onBackgroundColor),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    menuState.show {
                                        PlayerMenu(
                                            mediaMetadata = mediaMetadata ?: return@show,
                                            navController = navController,
                                            onShowDetailsDialog = onShowDetailsDialog,
                                            onDismiss = menuState::dismiss,
                                            onNavigateAway = { state.collapseSoft() },
                                        )
                                    }
                                },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = stringResource(R.string.acc_more_options),
                            colorFilter = ColorFilter.tint(onBackgroundColor),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .nestedScroll(consumeOverflowNestedScrollConnection)
                        .nestedScroll(state.preUpPostDownNestedScrollConnection),
            ) {
                Thumbnail(
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (playbackMode == PlaybackMode.VIDEO) {
                                    Modifier
                                } else {
                                    Modifier.padding(horizontal = thumbnailPadding)
                                },
                            ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        controlsContent()

        Spacer(Modifier.height(24.dp))
    }
}
