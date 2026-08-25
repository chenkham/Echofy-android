package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R

/**
 * Sleek, uncluttered top bar for the AI Assistant screen.
 * Fits flush at the top of the AI tab screen.
 */
@Composable
fun AiTopBar(
    onHistoryClick: () -> Unit,
) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Echofy avatar logo
            Image(
                painter = painterResource(R.drawable.echofy),
                contentDescription = "Echofy AI",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )

            Spacer(Modifier.width(10.dp))

            // AI Title
            Text(
                text = "AI",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    letterSpacing = (-0.3).sp
                )
            )

            Spacer(Modifier.weight(1f))

            // Chat History drawer icon button
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.history),
                    contentDescription = "Chat History",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
