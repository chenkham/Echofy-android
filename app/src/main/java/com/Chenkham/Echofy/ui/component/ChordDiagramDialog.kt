package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.audio.ChordFingering
import com.Chenkham.Echofy.audio.ChordsManager

@Composable
fun ChordDiagramDialog(
    initialChord: String,
    progression: List<String> = emptyList(),
    onDismiss: () -> Unit
) {
    var selectedChord by remember { mutableStateOf(initialChord) }
    var instrument by remember { mutableStateOf("Guitar") } // "Guitar" or "Ukulele"
    val fingering = remember(selectedChord) { ChordsManager.getChordFingering(selectedChord) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "🎸 Chord Diagram",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live Guitar & Ukulele Tabs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Instrument Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("Guitar", "Ukulele").forEach { inst ->
                            val selected = instrument == inst
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { instrument = inst }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = inst,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chord Progression Chips
                if (progression.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(progression.distinct()) { chord ->
                            val isCurrent = chord == selectedChord
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.clickable { selectedChord = chord }
                            ) {
                                Text(
                                    text = chord,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Big Chord Title
                Text(
                    text = selectedChord,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Canvas Fretboard Diagram
                Box(
                    modifier = Modifier
                        .size(width = 180.dp, height = 210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FretboardCanvas(
                        fingering = fingering,
                        isUkulele = instrument == "Ukulele",
                        dotColor = MaterialTheme.colorScheme.primary,
                        fretColor = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun FretboardCanvas(
    fingering: ChordFingering,
    isUkulele: Boolean,
    dotColor: Color,
    fretColor: Color,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val frets = if (isUkulele) fingering.ukeFrets else fingering.guitarFrets
    val stringCount = if (isUkulele) 4 else 6
    val fretCount = 5

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val topPadding = 24.dp.toPx()
        val sidePadding = 16.dp.toPx()
        val bottomPadding = 12.dp.toPx()

        val fretAreaHeight = height - topPadding - bottomPadding
        val fretAreaWidth = width - (sidePadding * 2)

        val stringSpacing = fretAreaWidth / (stringCount - 1)
        val fretSpacing = fretAreaHeight / fretCount

        // Nut (Top thick bar)
        drawLine(
            color = fretColor,
            start = Offset(sidePadding, topPadding),
            end = Offset(width - sidePadding, topPadding),
            strokeWidth = 6.dp.toPx()
        )

        // Fret lines
        for (i in 1..fretCount) {
            val y = topPadding + (i * fretSpacing)
            drawLine(
                color = fretColor.copy(alpha = 0.6f),
                start = Offset(sidePadding, y),
                end = Offset(width - sidePadding, y),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Strings
        for (i in 0 until stringCount) {
            val x = sidePadding + (i * stringSpacing)
            drawLine(
                color = fretColor.copy(alpha = 0.8f),
                start = Offset(x, topPadding),
                end = Offset(x, topPadding + fretAreaHeight),
                strokeWidth = if (i == 0 && !isUkulele) 3.dp.toPx() else 1.5.dp.toPx()
            )

            // Draw mute (X) or open (O) or finger dot
            val fretNum = frets.getOrElse(i) { 0 }
            if (fretNum == -1) {
                // X (Muted)
                val textY = topPadding - 10.dp.toPx()
                drawLine(
                    color = Color.Red.copy(alpha = 0.8f),
                    start = Offset(x - 4.dp.toPx(), textY - 4.dp.toPx()),
                    end = Offset(x + 4.dp.toPx(), textY + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.Red.copy(alpha = 0.8f),
                    start = Offset(x + 4.dp.toPx(), textY - 4.dp.toPx()),
                    end = Offset(x - 4.dp.toPx(), textY + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            } else if (fretNum == 0) {
                // O (Open string)
                val circleY = topPadding - 10.dp.toPx()
                drawCircle(
                    color = dotColor.copy(alpha = 0.8f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, circleY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else if (fretNum in 1..fretCount) {
                // Finger dot on fret
                val dotY = topPadding + ((fretNum - 0.5f) * fretSpacing)
                drawCircle(
                    color = dotColor,
                    radius = 8.dp.toPx(),
                    center = Offset(x, dotY)
                )
            }
        }
    }
}
