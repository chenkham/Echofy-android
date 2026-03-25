package com.Chenkham.Echofy.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Chenkham.Echofy.R

@Composable
fun SignInScreen(
    chained: Boolean = false,
    onSignInSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val signInState by viewModel.signInState.collectAsState()
    
    var showErrorDialog by remember { mutableStateOf(false) }
    
    // We need the preference to check if YT Music is already connected
    val innerTubeCookie by com.Chenkham.Echofy.utils.rememberPreference(
        com.Chenkham.Echofy.constants.InnerTubeCookieKey, ""
    )

    LaunchedEffect(signInState) {
        when (signInState) {
            is SignInState.Success -> onSignInSuccess()
            is SignInState.Error -> showErrorDialog = true
            else -> {}
        }
    }

    if (showErrorDialog && signInState is SignInState.Error) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Sign-In Error") },
            text = { Text((signInState as SignInState.Error).message) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App logo/branding
            Image(
                painter = painterResource(R.drawable.echofy),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = "Echofy",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Your Music, Amplified",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Google Sign-In Button
            Button(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp),
                enabled = signInState !is SignInState.Loading
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (signInState is SignInState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.google),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                    Text(
                        text = if (signInState is SignInState.Loading) 
                            "Signing in..." 
                        else 
                            "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Continue as Guest
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue as Guest",
                    fontSize = 16.sp
                )
            }
            
            // Error message
            if (signInState is SignInState.Error) {
                Text(
                    text = "Sign-in failed. Please try again.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Privacy notice
            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
