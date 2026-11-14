package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.lyrics.LyricsResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsProviderDialog(
    currentProvider: String?,
    providerNames: List<String>,
    streamingResults: List<LyricsResult>,
    isLoadingLyrics: Boolean,
    onSelectProvider: (index: Int, providerName: String) -> Unit,
    onRefetchAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Lyrics Provider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                currentProvider?.let {
                    Text(
                        text = "Currently using: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                providerNames.forEachIndexed { index, providerName ->
                    val hasResult = streamingResults.any { it.providerName == providerName }
                    val isCurrentProvider = currentProvider == providerName

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = hasResult || !isLoadingLyrics) {
                                    onSelectProvider(index, providerName)
                                },
                        shape = RoundedCornerShape(8.dp),
                        color =
                            when {
                                isCurrentProvider -> MaterialTheme.colorScheme.primaryContainer
                                hasResult -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = providerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrentProvider) FontWeight.Bold else FontWeight.Normal,
                            )

                            when {
                                isLoadingLyrics && !hasResult -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                hasResult -> {
                                    val isSynced =
                                        streamingResults.find { it.providerName == providerName }
                                            ?.lyrics?.startsWith("[") == true
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isSynced) {
                                            Icon(
                                                painter = painterResource(R.drawable.sync),
                                                contentDescription = "Synced",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.check_circle),
                                            contentDescription = "Available",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Tap to try",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onRefetchAll,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refetch All")
                }
            }
        }
    }
}
