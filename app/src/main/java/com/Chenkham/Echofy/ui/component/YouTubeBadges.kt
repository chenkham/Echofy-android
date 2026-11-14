package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.R
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.YTItem

/**
 * Standard badge overlays (liked, library, explicit, download progress) for YouTube items.
 */
@Composable
fun RowScope.YouTubeBadges(
    item: YTItem,
    isLiked: Boolean? = null,
    inLibrary: Boolean? = null,
    isBookmarked: Boolean? = null,
) {
    val database = LocalDatabase.current
    val songState =
        if (isLiked == null || inLibrary == null) {
            database.song(item.id).collectAsState(initial = null).value
        } else {
            null
        }

    val albumState =
        if (isBookmarked == null) {
            database.album(item.id).collectAsState(initial = null).value
        } else {
            null
        }

    val finalLiked = isLiked ?: (songState?.song?.liked == true)
    val finalInLibrary = inLibrary ?: (songState?.song?.inLibrary != null)
    val finalBookmarked = isBookmarked ?: (albumState?.album?.bookmarkedAt != null)

    if (item is SongItem && finalLiked || item is AlbumItem && finalBookmarked) {
        Icon(
            painter = painterResource(R.drawable.favorite),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp).padding(end = 2.dp),
        )
    }
    if (item is SongItem && finalInLibrary) {
        Icon(
            painter = painterResource(R.drawable.library_add_check),
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(end = 2.dp),
        )
    }
    if (item.explicit) {
        Icon(
            painter = painterResource(R.drawable.explicit),
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(end = 2.dp),
        )
    }
    if (item is SongItem) {
        val downloads by LocalDownloadUtil.current.getDownload(item.id).collectAsState(initial = null)
        when (downloads?.state) {
            STATE_COMPLETED -> {
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 2.dp),
                )
            }
            STATE_QUEUED, STATE_DOWNLOADING -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp).padding(end = 2.dp),
                )
            }
            else -> {}
        }
    }
}
