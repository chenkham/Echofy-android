package com.Chenkham.Echofy.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Chenkham.Echofy.viewmodels.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val canProceed by remember {
        derivedStateOf { viewModel.canProceed() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )

            Text(
                text = "Step ${currentStep + 1} of 4",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animated step content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300)
                        ) + fadeIn() togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(300)
                                ) + fadeOut()
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(300)
                        ) + fadeIn() togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300)
                                ) + fadeOut()
                    }
                },
                label = "step_transition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> CountrySelectionStep(viewModel)
                    1 -> ArtistSelectionStep(viewModel)
                    2 -> LanguageSelectionStep(viewModel)
                    3 -> LoginPromptStep(viewModel, onComplete)
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Skip button
                TextButton(
                    onClick = {
                        viewModel.skipAll()
                        onSkip()
                    }
                ) {
                    Text("Skip")
                }

                // Next/Finish button
                Button(
                    onClick = {
                        if (currentStep < 3) {
                            viewModel.nextStep()
                        } else {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    },
                    enabled = canProceed
                ) {
                    Text(if (currentStep < 3) "Next" else "Get Started")
                }
            }
        }
    }
}
