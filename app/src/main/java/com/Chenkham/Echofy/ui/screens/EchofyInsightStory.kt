package com.Chenkham.Echofy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.db.entities.SongWithStats
import com.Chenkham.Echofy.utils.makeTimeString
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun EchofyInsightDialog(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTimeMs: Long,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val totalPages = 4
        val pagerState = rememberPagerState(pageCount = { totalPages })
        val coroutineScope = rememberCoroutineScope()
        val currentYear = LocalDateTime.now().year

        val progress = remember { Animatable(0f) }

        LaunchedEffect(pagerState.currentPage) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 6500, easing = LinearEasing)
            )
            if (pagerState.currentPage < totalPages - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF16062B),
                            Color(0xFF38084B),
                            Color(0xFF6B0E50),
                            Color(0xFF28051E)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            coroutineScope.launch {
                                if (offset.x < size.width * 0.35f) {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                } else {
                                    if (pagerState.currentPage < totalPages - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Top Progress Indicators (6 segments)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until totalPages) {
                        val currentProgress = when {
                            i < pagerState.currentPage -> 1f
                            i == pagerState.currentPage -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.5.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }

                // Top Bar: Back | Echofy Insight | Year Pill (Matching media_1787641439230.png)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "OpenTune",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "Insight",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Year Dropdown Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "$currentYear ⌵",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Story Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> {
                            // Cover Page (Exact match for media_1787641439230.png)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "your",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Text(
                                    text = "Insight",
                                    fontSize = 62.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = (-1).sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Pink/Magenta 2026 Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFFF2A85),
                                                    Color(0xFFFF4081)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 22.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "$currentYear",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(36.dp))

                                Text(
                                    text = "Everything you listened to\nthis year, in one place.",
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = "• • •  Swipe to explore",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        1 -> {
                            // Top Artist Page
                            val topArtist = topArtists.firstOrNull()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "YOUR TOP ARTIST",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF80AB),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                AsyncImage(
                                    model = topArtist?.artist?.thumbnailUrl,
                                    contentDescription = topArtist?.artist?.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = topArtist?.artist?.name ?: "No Artist",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${topArtist?.songCount ?: 0} songs played • ${makeTimeString(topArtist?.timeListened?.toLong()?.times(1000L))}",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        2 -> {
                            // Top Songs Page
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "YOUR TOP TRACKS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80D8FF),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                topSongs.take(5).forEachIndexed { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.width(36.dp)
                                        )

                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${song.songCountListened} plays • ${makeTimeString(song.timeListened)}",
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Total Listening Time Page
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL LISTENING TIME",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB388FF),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = makeTimeString(totalListenTimeMs),
                                    fontSize = 58.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF90CAF9)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Thank you for listening to music with Echofy!",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(36.dp))

                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85)),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Text(
                                        text = "Done",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
