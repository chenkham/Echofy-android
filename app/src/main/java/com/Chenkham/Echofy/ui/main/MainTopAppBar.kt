package com.Chenkham.Echofy.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ui.component.ProfileIconWithUpdateBadge

/**
 * Top app bar with app identity, music recognition, notifications, search, and profile actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    hasUnreadNotifications: Boolean,
    onOpenMusicRecognition: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.echofy),
                    contentDescription = null,
                    modifier = Modifier.size(27.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenMusicRecognition) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = stringResource(R.string.music_recognition),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(
                            painter = painterResource(R.drawable.notification_on),
                            contentDescription = stringResource(R.string.notification),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (hasUnreadNotifications) {
                        Box(
                            modifier =
                                Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-8).dp, y = 8.dp)
                                    .background(
                                        color = Color.Red,
                                        shape = CircleShape,
                                    ),
                        )
                    }
                }

                IconButton(onClick = onOpenSearch) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ProfileIconWithUpdateBadge(
                    currentVersion = versionName,
                    onProfileClick = onOpenSettings,
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
    )
}
