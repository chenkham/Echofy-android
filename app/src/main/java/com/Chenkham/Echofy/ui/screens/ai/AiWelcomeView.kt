package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R

/**
 * Data class representing a quick mix card in the AI welcome view.
 */
data class AiQuickMixItem(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val prompt: String,
    val accentColor: Color
)

/**
 * Welcome view shown when the AI chat has no messages yet.
 * Contains greeting, suggestion chips, quick mix carousel, and capability cards.
 * Extracted from the monolithic AiAssistantScreen for reusability and maintainability.
 */
@Composable
fun AiWelcomeView(onSuggestionClick: (String) -> Unit) {
    val scrollState = rememberScrollState()

    val quickMixes = remember {
        listOf(
            AiQuickMixItem("🎧", "Lofi Beats", "Chill instrumental vibe", "Play relaxing lofi beats", Color(0xFF7DD3FC)),
            AiQuickMixItem("🚀", "Bollywood Hits", "Trending Hindi tracks", "Play top Hindi trending songs", Color(0xFFF472B6)),
            AiQuickMixItem("⚡", "Workout Hype", "High energy motivation", "Play high energy workout music", Color(0xFFFBBF24)),
            AiQuickMixItem("🌙", "Night Drive", "Synthwave & electronic", "Play synthwave & chill electronic", Color(0xFFC4B5FD)),
            AiQuickMixItem("🎸", "Rock Classics", "Legendary guitar anthems", "Play legendary rock classics", Color(0xFF34D399)),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        // Minimalist Echofy Assistant Orb
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Primary.copy(alpha = 0.25f), Color.Transparent),
                            radius = 120f
                        )
                    )
            )
            Image(
                painter = painterResource(R.drawable.echofy),
                contentDescription = "Echofy AI",
                modifier = Modifier.size(54.dp).clip(CircleShape)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "How can I help you listen?",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.2).sp
            )
        )

        Spacer(Modifier.height(24.dp))

        // Suggestion chips — 2×2 grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip("🎵 Play chill music", Modifier.weight(1f)) {
                    onSuggestionClick("Play some chill music")
                }
                SuggestionChip("📊 My top artists", Modifier.weight(1f)) {
                    onSuggestionClick("Who are my most played artists?")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip("💿 Create playlist", Modifier.weight(1f)) {
                    onSuggestionClick("Create a playlist based on my listening history")
                }
                SuggestionChip("🔥 Trending hits", Modifier.weight(1f)) {
                    onSuggestionClick("Play trending songs")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Minimal Quick Mix Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(quickMixes) { item ->
                QuickMixCard(item) {
                    onSuggestionClick(item.prompt)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickMixCard(item: AiQuickMixItem, onClick: () -> Unit) {
    Surface(
        color = SurfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            Column {
                Text(
                    text = item.title,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = TextStyle(fontSize = 10.sp, color = OnSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CapabilityChip(title: String, desc: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary))
            Spacer(Modifier.height(2.dp))
            Text(desc, style = TextStyle(fontSize = 10.sp, color = OnSurfaceVariant), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SuggestionChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = SurfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = 13.sp, color = OnSurfaceVariant, lineHeight = 18.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        )
    }
}
