package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.constants.GridThumbnailHeight

@Composable
fun SmartPlaylistListItem(
    title: String,
    icon: Painter,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Gradient Icon Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(gradientColors))
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SmartPlaylistGridItem(
    title: String,
    icon: Painter,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = false
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(GridThumbnailHeight))
    ) {
        // Gradient Icon Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(gradientColors))
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp) // Larger icon for grid
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}
