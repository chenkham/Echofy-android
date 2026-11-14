package com.Chenkham.Echofy.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.constants.GridThumbnailHeight
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.YTItem
import kotlinx.coroutines.CoroutineScope

/**
 * Composable item for displaying YouTube items in a grid layout.
 */
@SuppressLint("SuspiciousIndentation")
@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    isLiked: Boolean? = null,
    inLibrary: Boolean? = null,
    isBookmarked: Boolean? = null,
    badges: @Composable RowScope.() -> Unit = {
        YouTubeBadges(item, isLiked, inLibrary, isBookmarked)
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val thumbnailShape =
        if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
    val thumbnailRatio = thumbnailRatio

    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)
            },
    ) {
        YouTubeGridThumbnail(
            item = item,
            thumbnailShape = thumbnailShape,
            thumbnailRatio = thumbnailRatio,
            isActive = isActive,
            isPlaying = isPlaying,
            coroutineScope = coroutineScope,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
            modifier =
                Modifier
                    .basicMarquee(iterations = 3)
                    .fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            val subtitle =
                when (item) {
                    is SongItem ->
                        joinByBullet(
                            item.artists.joinToString { it.name },
                            makeTimeString(item.duration?.times(1000L)),
                        )

                    is AlbumItem ->
                        joinByBullet(
                            item.artists?.joinToString { it.name },
                            item.year?.toString(),
                        )

                    is ArtistItem -> null
                    is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
