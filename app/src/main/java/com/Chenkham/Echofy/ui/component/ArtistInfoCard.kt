package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.enrichment.ArtistEnrichment
import com.Chenkham.bandsintown.models.Concert
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Artist metadata aggregated from MusicBrainz, TheAudioDB, Bandsintown and
 * TasteDive. Every section is optional and hides itself when its source has no
 * data, so the card degrades gracefully rather than showing empty scaffolding.
 */
@Composable
fun ArtistInfoCard(
    enrichment: ArtistEnrichment,
    modifier: Modifier = Modifier,
    showGenres: Boolean = true,
    showLinks: Boolean = true,
    onSimilarArtistClick: ((String) -> Unit)? = null,
) {
    if (enrichment.isEmpty) return

    val info = enrichment.info
    val bio = enrichment.bio

    val facts = remember(info, bio) {
        buildList {
            info?.type?.let { add(it) }
            (info?.country ?: bio?.country)?.let { add(it) }
            activeYears(
                begin = info?.beginDate ?: bio?.formedYear,
                end = info?.endDate,
                isEnded = info?.isEnded == true,
            )?.let { add(it) }
        }
    }

    val genres = remember(info, bio) {
        info?.genres?.takeIf { it.isNotEmpty() }
            ?: bio?.genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    }

    val hasAnything = facts.isNotEmpty() ||
        !bio?.biography.isNullOrBlank() ||
        (showGenres && genres.isNotEmpty()) ||
        enrichment.concerts.isNotEmpty() ||
        enrichment.similarArtists.isNotEmpty() ||
        (showLinks && (info?.officialHomepage != null || info?.wikipediaUrl != null))

    if (!hasAnything) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString("  •  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            bio?.biography?.takeIf { it.isNotBlank() }?.let { biography ->
                if (facts.isNotEmpty()) Spacer(Modifier.height(12.dp))
                ExpandableBiography(biography)
            }

            if (showGenres && genres.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    genres.take(6).forEach { genre ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(genre.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }

            if (enrichment.concerts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ConcertsSection(enrichment.concerts)
            }

            if (enrichment.similarArtists.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(
                    icon = R.drawable.artist,
                    title = "Fans also like",
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    enrichment.similarArtists.take(10).forEach { rec ->
                        SuggestionChip(
                            onClick = { onSimilarArtistClick?.invoke(rec.name) },
                            label = { Text(rec.name) },
                            enabled = onSimilarArtistClick != null,
                        )
                    }
                }
            }

            if (showLinks) {
                val homepage = info?.officialHomepage
                val wikipedia = info?.wikipediaUrl ?: bio?.website
                if (homepage != null || wikipedia != null) {
                    Spacer(Modifier.height(12.dp))
                    val uriHandler = LocalUriHandler.current
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        homepage?.let { url ->
                            AssistChip(
                                onClick = { uriHandler.openUri(url) },
                                label = { Text("Website") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.link),
                                        contentDescription = null,
                                        modifier = Modifier.width(18.dp),
                                    )
                                },
                            )
                        }
                        wikipedia?.let { url ->
                            AssistChip(
                                onClick = { uriHandler.openUri(url) },
                                label = { Text("Wikipedia") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.info),
                                        contentDescription = null,
                                        modifier = Modifier.width(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = attributionFor(enrichment),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Biographies from TheAudioDB routinely run several hundred words, so they start
 * collapsed.
 */
@Composable
private fun ExpandableBiography(biography: String) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = biography,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        // Only offer the toggle when there is meaningfully more to reveal.
        if (biography.length > 200) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 0.dp,
                    vertical = 4.dp,
                ),
            ) {
                Text(if (expanded) "Show less" else "Read more")
            }
        }
    }
}

@Composable
private fun ConcertsSection(concerts: List<Concert>) {
    val uriHandler = LocalUriHandler.current

    SectionHeader(
        icon = R.drawable.event,
        title = "Upcoming concerts",
    )
    Spacer(Modifier.height(8.dp))

    concerts.take(5).forEach { concert ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = concert.venueName.ifBlank { concert.location },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(formatConcertDate(concert.datetime), concert.location)
                        .filter { it.isNotBlank() }
                        .joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = { uriHandler.openUri(concert.ticketUrl) },
                label = { Text("Tickets") },
                colors = AssistChipDefaults.assistChipColors(),
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.width(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Bandsintown returns ISO-8601 local datetimes. Fall back to the raw date
 * portion if parsing fails rather than showing nothing.
 */
private fun formatConcertDate(datetime: String): String =
    runCatching {
        OffsetDateTime.parse(datetime).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }.recoverCatching {
        java.time.LocalDateTime.parse(datetime)
            .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }.getOrElse {
        datetime.substringBefore('T')
    }

private fun activeYears(begin: String?, end: String?, isEnded: Boolean): String? {
    val start = begin?.take(4)?.takeIf { it.isNotBlank() } ?: return null
    val finish = end?.take(4)
    return when {
        finish != null -> "$start – $finish"
        isEnded -> "$start – ?"
        else -> "Since $start"
    }
}

private fun attributionFor(enrichment: ArtistEnrichment): String {
    val sources = buildList {
        if (enrichment.info != null) add("MusicBrainz")
        if (enrichment.bio != null) add("TheAudioDB")
        if (enrichment.concerts.isNotEmpty()) add("Bandsintown")
        if (enrichment.similarArtists.isNotEmpty()) add("TasteDive")
    }
    return "Metadata from ${sources.joinToString(", ")}"
}
