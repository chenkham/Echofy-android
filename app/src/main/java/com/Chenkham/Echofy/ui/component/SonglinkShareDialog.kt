package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.content.Intent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.SonglinkApiKeyKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.songlink.Songlink
import com.Chenkham.songlink.models.PlatformLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lets the user share a song as a link that works on whichever service the
 * recipient uses, instead of a YouTube Music URL they may not be able to open.
 *
 * Resolution happens through Songlink/Odesli when the dialog opens. If it fails,
 * the plain YouTube Music link is still offered so sharing never dead-ends.
 */
@Composable
fun SonglinkShareDialog(
    videoId: String,
    songTitle: String,
    artistName: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val fallbackUrl = remember(videoId) { "https://music.youtube.com/watch?v=$videoId" }

    var platforms by remember { mutableStateOf<List<PlatformLink>>(emptyList()) }
    var universalUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        val apiKey = withContext(Dispatchers.IO) {
            context.dataStore[SonglinkApiKeyKey]?.takeIf { it.isNotBlank() }
        }

        Songlink.resolveYouTubeVideo(videoId, apiKey = apiKey)
            .onSuccess { links ->
                platforms = links.platforms
                universalUrl = links.pageUrl.takeIf { it.isNotBlank() }
                failed = links.platforms.isEmpty()
            }.onFailure {
                failed = true
            }

        isLoading = false
    }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.share),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.share)) },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = songTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (artistName.isNotBlank()) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val echofyShareText = remember(videoId, songTitle, artistName) {
                com.Chenkham.Echofy.utils.ShareUtils.buildTrackShareText(context, videoId, songTitle, artistName)
            }
            ShareOptionRow(
                label = "Share Echofy Universal Link",
                icon = R.drawable.share,
            ) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, echofyShareText)
                }
                context.startActivity(Intent.createChooser(intent, null))
                onDismiss()
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Finding links…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                failed -> {
                    Text(
                        text = "Could not find links on other services.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ShareOptionRow(
                        label = "Share YouTube Music link",
                        icon = R.drawable.link,
                    ) {
                        shareText(context, fallbackUrl, songTitle, artistName)
                        onDismiss()
                    }
                }

                else -> {
                    universalUrl?.let { url ->
                        ShareOptionRow(
                            label = "Share link for all services",
                            icon = R.drawable.link,
                        ) {
                            shareText(context, url, songTitle, artistName)
                            onDismiss()
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    if (platforms.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.songlink_long_press_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    platforms.forEach { platform ->
                        ShareOptionRow(
                            label = platform.displayName,
                            icon = R.drawable.music_note,
                            onLongClick = {
                                openUrl(context, platform.url)
                                onDismiss()
                            },
                        ) {
                            shareText(context, platform.url, songTitle, artistName)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ShareOptionRow(
    label: String,
    icon: Int,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url),
            ),
        )
    }
}

private fun shareText(
    context: android.content.Context,
    url: String,
    songTitle: String,
    artistName: String,
) {
    val intent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        type = "text/plain"
        putExtra(
            android.content.Intent.EXTRA_TEXT,
            buildString {
                append("Check out this song on Echofy!\n")
                append(songTitle)
                if (artistName.isNotBlank()) append(" - $artistName")
                append("\n")
                append(url)
            },
        )
    }
    context.startActivity(
        android.content.Intent.createChooser(intent, context.getString(R.string.share)),
    )
}
