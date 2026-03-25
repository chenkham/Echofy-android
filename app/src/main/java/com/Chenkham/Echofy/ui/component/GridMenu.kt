package com.Chenkham.Echofy.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.utils.makeTimeString

val GridMenuItemHeight = 96.dp

/**
 * Simple 2-column grid menu without lazy loading for better performance
 * Similar to YouTube Music's player menu design
 */
@Composable
fun GridMenu(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Simple grid menu item composable for non-lazy usage
 */
@Composable
fun SimpleGridMenuItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    title: String,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .height(GridMenuItemHeight)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SimpleGridMenuItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    @StringRes title: Int,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SimpleGridMenuItem(
        modifier = modifier,
        icon = icon,
        title = stringResource(title),
        tint = tint,
        enabled = enabled,
        onClick = onClick
    )
}

fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    tint: @Composable () -> Color = { LocalContentColor.current },
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = GridMenuItem(
    modifier = modifier,
    icon = {
        Icon(
            painter = painterResource(icon),
            tint = tint(),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    },
    title = title,
    enabled = enabled,
    onClick = onClick
)

fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    item {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .height(GridMenuItemHeight)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
                content = icon
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


fun LazyGridScope.DownloadGridMenu(
    @Download.State state: Int?,
    onRemoveDownload: () -> Unit,
    onDownload: () -> Unit,
) {
    when (state) {
        Download.STATE_COMPLETED -> {
            GridMenuItem(
                icon = R.drawable.offline,
                title = R.string.remove_download,
                onClick = onRemoveDownload
            )
        }

        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
            GridMenuItem(
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                },
                title = R.string.downloading,
                onClick = onRemoveDownload
            )
        }

        else -> {
            GridMenuItem(
                icon = R.drawable.download,
                title = R.string.download,
                onClick = onDownload
            )
        }
    }
}

fun LazyGridScope.SleepTimerGridMenu(
    modifier: Modifier = Modifier,
    sleepTimerTimeLeft: Long,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    item {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .height(GridMenuItemHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painterResource(R.drawable.bedtime),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (enabled) 1f else 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (enabled) makeTimeString(sleepTimerTimeLeft) else stringResource(
                    id = R.string.sleep_timer
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
