package com.Chenkham.Echofy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    isOnboardingCompleted: Boolean,
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Logo Animations
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logoAlpha"
    )
    
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 800),
        label = "logoScale"
    )
    
    val taglineAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 400),
        label = "taglineAlpha"
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Show splash for 3 seconds
        if (isOnboardingCompleted) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }
    
    // Clean black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Center content (Logo only)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "Echofy Logo",
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )
        }
        
        // Tagline at the bottom
        Text(
            text = "your music no limits",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(taglineAlpha)
        )
    }
}
