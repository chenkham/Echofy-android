package com.Chenkham.Echofy.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.Chenkham.Echofy.App
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ui.component.SettingsPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoCodeSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val subscriptionManager = remember { App.instance.subscriptionManager }
    val isSubscribed by subscriptionManager.isSubscribed.collectAsState()
    var promoCode by remember { mutableStateOf("") }
    var redeemStarted by remember { mutableStateOf(false) }
    var showCongrats by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, redeemStarted) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && redeemStarted) {
                scope.launch {
                    delay(500)
                    subscriptionManager.restorePurchases()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isSubscribed, redeemStarted) {
        if (redeemStarted && isSubscribed) {
            showCongrats = true
            redeemStarted = false
        }
    }

    if (showCongrats) {
        AlertDialog(
            onDismissRequest = { showCongrats = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.verified_user),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("Congratulations!") },
            text = { Text("Premium is active on your account.") },
            confirmButton = {
                TextButton(onClick = { showCongrats = false }) {
                    Text("Enjoy")
                }
            },
        )
    }

    SettingsPage(
        title = "Promo Codes",
        navController = navController,
        scrollBehavior = scrollBehavior,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.attach_money),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Enter your promo code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = promoCode,
                    onValueChange = {
                        promoCode = it.uppercase()
                        codeError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Promo code") },
                    singleLine = true,
                    isError = codeError,
                    supportingText = if (codeError) {
                        { Text("Enter your promo code") }
                    } else {
                        null
                    },
                )

                Button(
                    onClick = {
                        val code = promoCode.trim()
                        if (code.isBlank()) {
                            codeError = true
                            return@Button
                        }
                        redeemStarted = true
                        subscriptionManager.restorePurchases()
                        val redeemUri = Uri.parse("https://play.google.com/redeem?code=${Uri.encode(code)}")
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, redeemUri).apply {
                            setPackage("com.android.vending")
                        }
                        try {
                            context.startActivity(playStoreIntent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, redeemUri))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Redeem")
                }
            }
        }
    }
}
