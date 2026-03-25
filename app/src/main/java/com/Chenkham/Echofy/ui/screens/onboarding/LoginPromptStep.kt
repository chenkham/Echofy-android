package com.Chenkham.Echofy.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.Chenkham.Echofy.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.viewmodels.OnboardingViewModel

@Composable
fun LoginPromptStep(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.account),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign In to YouTube Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Unlock the full Echofy experience",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Benefits list
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BenefitItem(
                iconRes = R.drawable.sync,
                title = "Sync Your Library",
                description = "Access your playlists and liked songs"
            )

            BenefitItem(
                iconRes = R.drawable.heart,
                title = "Personalized Recommendations",
                description = "Get music tailored to your taste"
            )

            BenefitItem(
                iconRes = R.drawable.library_music,
                title = "Unlimited Access",
                description = "Stream millions of songs"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login button
        Button(
            onClick = {
                // Mark as complete FIRST so it doesn't show on return
                viewModel.completeOnboarding()
                onComplete()
                
                // Open YouTube Music login
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com"))
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Sign In with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                viewModel.completeOnboarding()
                onComplete()
            }
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun BenefitItem(
    iconRes: Int,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
