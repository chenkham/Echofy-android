package com.Chenkham.Echofy.ui.screens.onboarding

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.Chenkham.Echofy.constants.OnboardingCompletedKey
import com.Chenkham.Echofy.utils.rememberPreference

/**
 * Simple wrapper to show onboarding on first launch
 * 
 * Usage in MainActivity:
 * Replace your main content composable with:
 * 
 * OnboardingWrapper {
 *     // Your existing main app content here
 *     Scaffold(...) { ... }
 * }
 */
@Composable
fun OnboardingWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (isOnboardingCompleted, onOnboardingCompletedChange) = 
        rememberPreference(OnboardingCompletedKey, defaultValue = false)
    
    // Permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
           // Proceed regardless of result
           onOnboardingCompletedChange(true) 
           if (isGranted) {
               Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
           }
        }
    )

    fun completeOnboarding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onOnboardingCompletedChange(true)
        }
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onComplete = {
                completeOnboarding()
            },
            onSkip = {
                completeOnboarding()
            }
        )
    } else {
         content()
    }
}
