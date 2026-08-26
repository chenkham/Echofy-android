package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AmbientSoundsEnabledKey
import com.Chenkham.Echofy.constants.ArtistBioEnabledKey
import com.Chenkham.Echofy.constants.ArtistInfoEnabledKey
import com.Chenkham.Echofy.constants.ArtistInfoShowGenresKey
import com.Chenkham.Echofy.constants.ArtistInfoShowLinksKey
import com.Chenkham.Echofy.constants.BandsintownAppIdKey
import com.Chenkham.Echofy.constants.ConcertsEnabledKey
import com.Chenkham.Echofy.constants.DiscogsEnabledKey
import com.Chenkham.Echofy.constants.DiscogsTokenKey
import com.Chenkham.Echofy.constants.CustomShareDomainKey
import com.Chenkham.Echofy.constants.EnableGeniusKey
import com.Chenkham.Echofy.constants.FreesoundApiKeyKey
import com.Chenkham.Echofy.constants.GeniusAccessTokenKey
import com.Chenkham.Echofy.constants.MixcloudEnabledKey
import com.Chenkham.Echofy.constants.SimilarArtistsEnabledKey
import com.Chenkham.Echofy.constants.SonglinkEnabledKey
import com.Chenkham.Echofy.constants.TasteDiveApiKeyKey
import com.Chenkham.Echofy.constants.TheAudioDbApiKeyKey
import com.Chenkham.Echofy.ui.component.EditTextPreference
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberPreference

/**
 * Settings for the optional third-party data sources that enrich artists,
 * albums, lyrics and browsing. Key-required sources are gated behind user-supplied
 * API keys with expandable setup instructions and clear visual borders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (artistInfoEnabled, onArtistInfoEnabledChange) = rememberPreference(
        key = ArtistInfoEnabledKey,
        defaultValue = true
    )
    val (artistInfoShowGenres, onArtistInfoShowGenresChange) = rememberPreference(
        key = ArtistInfoShowGenresKey,
        defaultValue = true
    )
    val (artistInfoShowLinks, onArtistInfoShowLinksChange) = rememberPreference(
        key = ArtistInfoShowLinksKey,
        defaultValue = true
    )
    val (artistBioEnabled, onArtistBioEnabledChange) = rememberPreference(
        key = ArtistBioEnabledKey,
        defaultValue = true
    )
    val (theAudioDbApiKey, onTheAudioDbApiKeyChange) = rememberPreference(
        key = TheAudioDbApiKeyKey,
        defaultValue = ""
    )
    val (concertsEnabled, onConcertsEnabledChange) = rememberPreference(
        key = ConcertsEnabledKey,
        defaultValue = false
    )
    val (bandsintownAppId, onBandsintownAppIdChange) = rememberPreference(
        key = BandsintownAppIdKey,
        defaultValue = ""
    )
    val (similarArtistsEnabled, onSimilarArtistsEnabledChange) = rememberPreference(
        key = SimilarArtistsEnabledKey,
        defaultValue = false
    )
    val (tasteDiveApiKey, onTasteDiveApiKeyChange) = rememberPreference(
        key = TasteDiveApiKeyKey,
        defaultValue = ""
    )
    val (geniusEnabled, onGeniusEnabledChange) = rememberPreference(
        key = EnableGeniusKey,
        defaultValue = false
    )
    val (geniusAccessToken, onGeniusAccessTokenChange) = rememberPreference(
        key = GeniusAccessTokenKey,
        defaultValue = ""
    )
    val (discogsEnabled, onDiscogsEnabledChange) = rememberPreference(
        key = DiscogsEnabledKey,
        defaultValue = false
    )
    val (discogsToken, onDiscogsTokenChange) = rememberPreference(
        key = DiscogsTokenKey,
        defaultValue = ""
    )
    val (songlinkEnabled, onSonglinkEnabledChange) = rememberPreference(
        key = SonglinkEnabledKey,
        defaultValue = false
    )
    val (mixcloudEnabled, onMixcloudEnabledChange) = rememberPreference(
        key = MixcloudEnabledKey,
        defaultValue = false
    )
    val (ambientEnabled, onAmbientEnabledChange) = rememberPreference(
        key = AmbientSoundsEnabledKey,
        defaultValue = false
    )
    val (freesoundApiKey, onFreesoundApiKeyChange) = rememberPreference(
        key = FreesoundApiKeyKey,
        defaultValue = ""
    )
    val (customShareDomain, onCustomShareDomainChange) = rememberPreference(
        key = CustomShareDomainKey,
        defaultValue = ""
    )

    SettingsPage(
        title = stringResource(R.string.integrations),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.integrations_artist),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.integration_artist_details)) },
                        description = stringResource(R.string.integration_artist_details_desc),
                        icon = { Icon(painterResource(R.drawable.info), null) },
                        checked = artistInfoEnabled,
                        onCheckedChange = onArtistInfoEnabledChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.integration_artist_genres)) },
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        checked = artistInfoShowGenres,
                        onCheckedChange = onArtistInfoShowGenresChange,
                        isEnabled = artistInfoEnabled,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.integration_artist_links)) },
                        description = stringResource(R.string.integration_artist_links_desc),
                        icon = { Icon(painterResource(R.drawable.link), null) },
                        checked = artistInfoShowLinks,
                        onCheckedChange = onArtistInfoShowLinksChange,
                        isEnabled = artistInfoEnabled,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.integration_artist_bio)) },
                        description = stringResource(R.string.integration_artist_bio_desc),
                        icon = { Icon(painterResource(R.drawable.person), null) },
                        checked = artistBioEnabled,
                        onCheckedChange = onArtistBioEnabledChange,
                    )
                },
                {
                    EditTextPreference(
                        title = { Text(stringResource(R.string.integration_theaudiodb_key)) },
                        icon = { Icon(painterResource(R.drawable.token), null) },
                        value = theAudioDbApiKey,
                        onValueChange = onTheAudioDbApiKeyChange,
                        isInputValid = { true },
                        isEnabled = artistBioEnabled,
                    )
                },
                {
                    KeyRequiredIntegrationCard(
                        title = stringResource(R.string.integration_concerts),
                        description = stringResource(R.string.integration_concerts_desc),
                        icon = R.drawable.event,
                        enabled = concertsEnabled,
                        onEnabledChange = onConcertsEnabledChange,
                        keyValue = bandsintownAppId,
                        onKeyValueChange = onBandsintownAppIdChange,
                        keyTitle = stringResource(R.string.integration_bandsintown_id),
                        websiteUrl = "https://manager.bandsintown.com",
                        steps = listOf(
                            "Visit manager.bandsintown.com or API developer docs.",
                            "Sign up or log in to your developer account.",
                            "Copy your assigned App ID and paste it above."
                        )
                    )
                },
                {
                    KeyRequiredIntegrationCard(
                        title = stringResource(R.string.integration_similar_artists),
                        description = stringResource(R.string.integration_similar_artists_desc),
                        icon = R.drawable.discover_tune,
                        enabled = similarArtistsEnabled,
                        onEnabledChange = onSimilarArtistsEnabledChange,
                        keyValue = tasteDiveApiKey,
                        onKeyValueChange = onTasteDiveApiKeyChange,
                        keyTitle = stringResource(R.string.integration_tastedive_key),
                        websiteUrl = "https://tastedive.com/account/api_access",
                        steps = listOf(
                            "Create or log in to your TasteDive account.",
                            "Navigate to Account -> API Access.",
                            "Copy your free API Key and paste it above."
                        )
                    )
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.integrations_music),
            items = listOf(
                {
                    KeyRequiredIntegrationCard(
                        title = stringResource(R.string.integration_genius),
                        description = stringResource(R.string.integration_genius_desc),
                        icon = R.drawable.lyrics,
                        enabled = geniusEnabled,
                        onEnabledChange = onGeniusEnabledChange,
                        keyValue = geniusAccessToken,
                        onKeyValueChange = onGeniusAccessTokenChange,
                        keyTitle = stringResource(R.string.integration_genius_token),
                        websiteUrl = "https://genius.com/api-clients",
                        steps = listOf(
                            "Sign in to genius.com.",
                            "Go to API Clients (genius.com/api-clients) and create a client.",
                            "Click 'Generate Access Token' and paste it above."
                        )
                    )
                },
                {
                    KeyRequiredIntegrationCard(
                        title = stringResource(R.string.integration_discogs),
                        description = stringResource(R.string.integration_discogs_desc),
                        icon = R.drawable.album,
                        enabled = discogsEnabled,
                        onEnabledChange = onDiscogsEnabledChange,
                        keyValue = discogsToken,
                        onKeyValueChange = onDiscogsTokenChange,
                        keyTitle = stringResource(R.string.integration_discogs_token),
                        websiteUrl = "https://www.discogs.com/settings/developers",
                        steps = listOf(
                            "Log in to your Discogs account.",
                            "Go to Settings -> Developers.",
                            "Click 'Generate new token' and paste it above."
                        )
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.integration_songlink)) },
                        description = stringResource(R.string.integration_songlink_desc),
                        icon = { Icon(painterResource(R.drawable.share), null) },
                        checked = songlinkEnabled,
                        onCheckedChange = onSonglinkEnabledChange,
                    )
                },
                {
                    EditTextPreference(
                        title = {
                            Column {
                                Text("Echofy Share Domain")
                                Text(
                                    text = if (customShareDomain.isBlank()) "Default: https://chenkham.github.io" else customShareDomain,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        icon = { Icon(painterResource(R.drawable.link), null) },
                        value = customShareDomain,
                        onValueChange = onCustomShareDomainChange,
                        isInputValid = { true },
                    )
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.integrations_explore),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.integration_mixcloud)) },
                        description = stringResource(R.string.integration_mixcloud_desc),
                        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                        checked = mixcloudEnabled,
                        onCheckedChange = onMixcloudEnabledChange,
                    )
                },
                {
                    KeyRequiredIntegrationCard(
                        title = stringResource(R.string.integration_ambient),
                        description = stringResource(R.string.integration_ambient_desc),
                        icon = R.drawable.music_note,
                        enabled = ambientEnabled,
                        onEnabledChange = onAmbientEnabledChange,
                        keyValue = freesoundApiKey,
                        onKeyValueChange = onFreesoundApiKeyChange,
                        keyTitle = stringResource(R.string.integration_freesound_key),
                        websiteUrl = "https://freesound.org/apiv2/apply",
                        steps = listOf(
                            "Log in or register at freesound.org.",
                            "Go to API -> Apply for API key.",
                            "Copy your API Key and paste it above."
                        )
                    )
                },
            )
        )
    }
}

/**
 * Custom bordered card for key-gated integrations with input field,
 * expandable step-by-step setup guide, direct web link, and GATED toggle.
 */
@Composable
private fun KeyRequiredIntegrationCard(
    title: String,
    description: String,
    icon: Int,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    keyValue: String,
    onKeyValueChange: (String) -> Unit,
    keyTitle: String,
    websiteUrl: String,
    steps: List<String>,
) {
    val uriHandler = LocalUriHandler.current
    var guideExpanded by remember { mutableStateOf(false) }
    val hasKey = keyValue.isNotBlank()

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasKey) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Feature Switch (GATED: Disabled until API Key is provided)
            SwitchPreference(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                description = if (!hasKey) "$description (API key required to enable)" else description,
                icon = { Icon(painterResource(icon), null) },
                checked = enabled && hasKey,
                onCheckedChange = { isChecked ->
                    if (hasKey) {
                        onEnabledChange(isChecked)
                    }
                },
                isEnabled = hasKey,
            )

            // API Key Input
            EditTextPreference(
                title = { Text(keyTitle) },
                icon = { Icon(painterResource(R.drawable.token), null) },
                value = keyValue,
                onValueChange = { newKey ->
                    onKeyValueChange(newKey)
                    if (newKey.isBlank() && enabled) {
                        onEnabledChange(false)
                    }
                },
                isInputValid = { true },
                isEnabled = true,
            )

            // Expandable Guide Section Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { guideExpanded = !guideExpanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (guideExpanded) "Hide setup guide" else "How to get a free API key?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(
                        if (guideExpanded) R.drawable.expand_less else R.drawable.expand_more
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expandable Guide Content with Web Link & Steps
            AnimatedVisibility(
                visible = guideExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Follow these steps to obtain your key:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { uriHandler.openUri(websiteUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.link),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Open Key Website", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

