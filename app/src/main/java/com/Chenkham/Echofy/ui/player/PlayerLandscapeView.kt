package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.menu.PlayerMenu

@Composable
fun PlayerLandscapeView(
    mediaMetadata: MediaMetadata?,
    playbackMode: PlaybackMode,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    isVideoAvailable: Boolean,
    enableListenTogether: Boolean,
    onBackgroundColor: Color,
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    onOpenJamSheet: () -> Unit,
    onShowDetailsDialog: () -> Unit,
    controlsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        // Top header: Minimize chevron + Song/Video Switch + Options
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { state.collapseSoft() },
            ) {
                Image(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = stringResource(R.string.acc_minimize),
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier = Modifier.size(26.dp),
                )
            }

            SongVideoSwitch(
                selectedMode = playbackMode,
                onModeChange = onPlaybackModeChange,
                isVideoAvailable = isVideoAvailable,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (enableListenTogether) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenJamSheet),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.group),
                            contentDescription = "Start Together",
                            colorFilter = ColorFilter.tint(onBackgroundColor),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(38.dp)
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
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // 2-Column Split: Left = Video/Artwork, Right = Full Playback Controls
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Column: Artwork / Video Container
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
            ) {
                Thumbnail(
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    modifier =
                        Modifier
                            .fillMaxHeight(0.9f)
                            .align(Alignment.Center),
                )
            }

            // Right Column: Controls with vertical scrolling support to avoid overlap
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
            ) {
                controlsContent()
            }
        }
    }
}
