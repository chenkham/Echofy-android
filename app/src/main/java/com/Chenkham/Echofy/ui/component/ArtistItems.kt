package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.Artist

/**
 * Composable items for displaying artists in list or grid layouts.
 */
@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .clip(CircleShape),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun ArtistSmallGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
) = SmallGridItem(
    title = artist.artist.name,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    },
    thumbnailShape = CircleShape,
    modifier = modifier,
    isArtist = true,
)
