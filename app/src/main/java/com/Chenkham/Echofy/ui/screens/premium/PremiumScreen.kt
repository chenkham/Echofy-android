package com.Chenkham.Echofy.ui.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ads.SubscriptionManager

@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val user by viewModel.activeUser.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val monthlyPrice by viewModel.monthlyPrice.collectAsState()
    val fiveYearPrice by viewModel.fiveYearPrice.collectAsState()
    val lifetimePrice by viewModel.lifetimePrice.collectAsState()
    
    val isSignedIn = user != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.diamond_filled),
                        contentDescription = "Premium",
                        modifier = Modifier.size(64.dp),
                        tint = if (isPremium) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isPremium) "Echofy Pro Active" else "Get Echofy Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Welcome to the ultimate music experience.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pricing Section
            if (!isSignedIn) {
                Text(
                    text = "Sign in with Google to subscribe",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )
            } else if (isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Subscription Active",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have unlocked all Pro features.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                try {
                                    context.startActivity(android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
                                    ))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Manage Subscription")
                        }
                    }
                }
                
            } else {
                Text(
                    text = "Choose Your Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Monthly Plan
                PricingCard(
                    title = "Monthly Plan",
                    price = "$monthlyPrice / month",
                    subtitle = "Cancel anytime",
                    onClick = { activity?.let { viewModel.launchBillingFlow(it, SubscriptionManager.PREMIUM_MONTHLY_ID) } }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // 5-Year Plan
                PricingCard(
                    title = "5-Year Plan",
                    price = fiveYearPrice,
                    subtitle = "Massive savings",
                    onClick = { activity?.let { viewModel.launchBillingFlow(it, SubscriptionManager.PREMIUM_5_YEAR_ID) } },
                    isHighlighted = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Lifetime Plan
                PricingCard(
                    title = "Lifetime Access",
                    price = lifetimePrice,
                    subtitle = "One-time payment, Pro forever",
                    onClick = { activity?.let { viewModel.launchBillingFlow(it, SubscriptionManager.PREMIUM_LIFETIME_ID) } },
                    isHighlighted = true,
                    highlightColor = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Massive Features List
            Text(
                text = "Everything You Get",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val features = remember {
                listOf(
                    "Ad-Free Experience",
                    "Unlimited Offline Downloads",
                    "Pitch Black AMOLED Mode",
                    "Custom Player UI Themes",
                    "Audio Speed & Pitch Controls",
                    "Live Fluid Player Backgrounds",
                    "3D Spatial / Concert Hall Audio",
                    "Seamless DJ Crossfade",
                    "Volume Normalization",
                    "Built-in Ringtone Maker",
                    "Real-Time Audio Visualizers",
                    "Haptic Bass Beats",
                    "Shake to Skip Gestures",
                    "Custom Playlist Thumbnails",
                    "Save Local EQ Presets",
                    "Hardware Volume Button Skips",
                    "Wake-to-Music Alarm Clock",
                    "Endless Offline Auto-Radio",
                    "Batch Download Offline Lyrics",
                    "Incognito Mode / Ignore History",
                    "Apple Music Style UI Option",
                    "Local Smart Playlists",
                    "Home Screen Custom Widgets",
                    "Custom Typography / Fonts",
                    "Live Lyric Peek on Player",
                    "Fade-Out Sleep Timer",
                    "Shareable .echofy Offline Playlists"
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.check), // Ensure check icon exists
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun PricingCard(
    title: String,
    price: String,
    subtitle: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(onClick = onClick)
            .let {
                if (isHighlighted) {
                    it.border(2.dp, highlightColor, RoundedCornerShape(16.dp))
                } else it
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProSettingSwitch(
    title: String,
    subtitle: String,
    icon: Int,
    preferenceKey: androidx.datastore.preferences.core.Preferences.Key<Boolean>
) {
    val (isChecked, onCheckedChange) = com.Chenkham.Echofy.utils.rememberPreference(preferenceKey, defaultValue = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
