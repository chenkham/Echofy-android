package com.Chenkham.Echofy.ui.screens.premium

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Chenkham.Echofy.R

@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val user by viewModel.activeUser.collectAsState()
    val subscriptionPrice by viewModel.subscriptionPrice.collectAsState()
    val isPremium = user?.isPremium == true
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
                .padding(bottom = 150.dp), // Increased padding to avoid mini player overlap
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
                        text = if (isPremium) "Echofy Premium Active" else "Get Echofy Premium",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thank you for supporting us!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Benefits Grid
            Text(
                text = "Premium Benefits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            BenefitItem(
                iconId = R.drawable.check,
                title = "Ad-Free Experience", 
                description = "Enjoy music without interruptions",
                isActive = isPremium
            )
            BenefitItem(
                iconId = R.drawable.download, 
                title = "Offline Downloads", 
                description = "Listen to your favorite songs offline",
                isActive = isPremium
            )
            BenefitItem(
                iconId = R.drawable.diamond_outlined, 
                title = "Support Development", 
                description = "Help us build more amazing features",
                isActive = isPremium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pricing Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monthly Plan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (subscriptionPrice.isNotEmpty()) "$subscriptionPrice / month" else "Loading...",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (!isSignedIn) {
                        // Not signed in
                        Text(
                            text = "Sign in with Google to subscribe",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (isPremium) {
                        // Already premium
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Subscription Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Manage your subscription in Google Play Store",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    try {
                                        context.startActivity(android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
                                        ))
                                    } catch (e: Exception) {
                                        // Fallback
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Manage Subscription")
                            }
                        }
                    } else {
                        // Not premium, signed in
                        Button(
                            onClick = { 
                                activity?.let { viewModel.launchBillingFlow(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = subscriptionPrice.isNotEmpty()
                        ) {
                            Text(
                                "Subscribe Now",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
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
fun BenefitItem(
    iconId: Int,
    title: String,
    description: String,
    isActive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
