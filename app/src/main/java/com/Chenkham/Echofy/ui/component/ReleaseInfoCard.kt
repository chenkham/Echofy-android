package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.discogs.models.ReleaseInfo

/**
 * Physical release details from Discogs: pressing formats, label, catalog
 * number and a link to the release page. Renders nothing when Discogs had no
 * usable data for the album.
 */
@Composable
fun ReleaseInfoCard(
    releaseInfo: ReleaseInfo,
    modifier: Modifier = Modifier,
) {
    val facts = buildList {
        releaseInfo.year?.let { add("Released $it") }
        releaseInfo.country?.let { add(it) }
        releaseInfo.labelName?.let { label ->
            val catalog = releaseInfo.catalogNumber
            add(if (catalog != null) "$label ($catalog)" else label)
        }
    }

    if (facts.isEmpty() && releaseInfo.formats.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.album),
                    contentDescription = null,
                    modifier = Modifier.width(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Release details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (facts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = facts.joinToString("  •  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (releaseInfo.formats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = releaseInfo.formats.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            releaseInfo.discogsUrl?.let { url ->
                Spacer(Modifier.height(12.dp))
                val uriHandler = LocalUriHandler.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { uriHandler.openUri(url) },
                        label = { Text("View on Discogs") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.link),
                                contentDescription = null,
                                modifier = Modifier.width(18.dp),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Data from Discogs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
