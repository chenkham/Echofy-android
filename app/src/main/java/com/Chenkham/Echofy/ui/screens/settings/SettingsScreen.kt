package com.Chenkham.Echofy.ui.screens.settings

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.innertube.utils.parseCookieString
import com.Chenkham.Echofy.BuildConfig
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.ui.component.AvatarPreferenceManager
import com.Chenkham.Echofy.ui.component.AvatarSelection
import com.Chenkham.Echofy.ui.component.ChangelogScreen
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.SettingsCategory
import com.Chenkham.Echofy.ui.component.SettingsCategoryItem
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import androidx.compose.material3.Switch
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ads.SubscriptionManager
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import com.Chenkham.Echofy.MainViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import android.app.Activity
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.VoiceControlEnabledKey
import com.Chenkham.Echofy.ui.component.BackpaperBackground

/**
 * Premium Subscription Card for ad-free experience
 */
@Composable
fun PremiumSubscriptionCard(
    subscriptionManager: SubscriptionManager,
    activity: Activity
) {
    val isSubscribed by subscriptionManager.isSubscribed.collectAsState()
    val isLoading by subscriptionManager.isLoading.collectAsState()
    val price by subscriptionManager.subscriptionPrice.collectAsState()
    
    Spacer(Modifier.height(16.dp))
    
    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubscribed) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            if (isSubscribed) R.drawable.verified_user else R.drawable.workspace_premium
                        ),
                        contentDescription = null,
                        tint = if (isSubscribed) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (isSubscribed) "Premium Active" else "Go Premium",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSubscribed) 
                                "Enjoying ad-free experience" 
                            else 
                                "Remove all ads for just $price",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (!isSubscribed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { subscriptionManager.launchPurchaseFlow(activity) },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Subscribe")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { subscriptionManager.restorePurchases() },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restore")
                    }
                }
            }
        }
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        }
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VersionCard(uriHandler: UriHandler) {
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }

    Spacer(Modifier.height(16.dp))

    SettingsCategory(
        title = stringResource(R.string.app_info), // App Information
        items = listOf(
            SettingsCategoryItem(
                icon = painterResource(R.drawable.info),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.Version),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = appVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
            )
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BugReportCard() {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    var bugDescription by remember { mutableStateOf("") }
    var bugCategory by remember { mutableStateOf("General Bug") }
    val appVersion = remember { getAppVersion(context) }
    
    val categories = listOf(
        "General Bug",
        "Playback Issue",
        "UI/Display Problem",
        "Crash/Freeze",
        "Listen Together Issue",
        "Feature Request",
        "Other"
    )
    
    Spacer(Modifier.height(16.dp))

    SettingsCategory(
        title = stringResource(R.string.feedback),
        items = listOf(
            SettingsCategoryItem(
                icon = painterResource(R.drawable.bug_report),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.report_bug),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.report_bug_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showReportDialog = true }
            )
        )
    )
    
    if (showReportDialog) {
        var expanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.report_bug),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category selector
                    Text(
                        text = stringResource(R.string.bug_category),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { expanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bugCategory, style = MaterialTheme.typography.bodyMedium)
                                Icon(
                                    painter = painterResource(R.drawable.expand_more),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        bugCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Bug description
                    Text(
                        text = stringResource(R.string.bug_description),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text(stringResource(R.string.bug_description_placeholder)) },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )
                    
                    // Device info preview
                    Text(
                        text = stringResource(R.string.device_info_included),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Compose email with bug report
                        val deviceInfo = """
                            |
                            |--- Device Information ---
                            |App Version: $appVersion
                            |Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                            |Device: ${Build.MANUFACTURER} ${Build.MODEL}
                            |Product: ${Build.PRODUCT}
                        """.trimMargin()
                        
                        val emailBody = """
                            |Bug Category: $bugCategory
                            |
                            |Description:
                            |$bugDescription
                            |$deviceInfo
                        """.trimMargin()
                        
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("chenkhamechofy@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "[Echofy Bug Report] $bugCategory")
                            putExtra(Intent.EXTRA_TEXT, emailBody)
                        }
                        
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Bug Report"))
                            showReportDialog = false
                            bugDescription = ""
                            bugCategory = "General Bug"
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "No email app found",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = bugDescription.isNotBlank()
                ) {
                    Text(stringResource(R.string.send_report))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showReportDialog = false
                    bugDescription = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun VoiceControlCard(
    voiceControlEnabled: Boolean,
    onVoiceControlEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Voice Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control playback with voice commands",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = voiceControlEnabled,
                onCheckedChange = onVoiceControlEnabledChange
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val mainViewModel: MainViewModel = hiltViewModel()
    val activeUser by mainViewModel.activeUser.collectAsState()

    BackpaperBackground(screen = BackpaperScreen.SETTINGS) {
    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )
        val context = LocalContext.current
        val avatarManager = remember { AvatarPreferenceManager(context) }
        val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
        val accountName by rememberPreference(AccountNameKey, "")
        val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
        val (voiceControlEnabled, onVoiceControlEnabledChange) = rememberPreference(VoiceControlEnabledKey, defaultValue = false)
        val isCookieLoggedIn = remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
        val isLoggedIn = isCookieLoggedIn || activeUser != null

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn) {
                var imageLoadError by remember { mutableStateOf(false) }
                var isImageLoading by remember { mutableStateOf(false) }

                // Avatar with wave effect and soft shadow
                Box(
                    contentAlignment = Alignment.Center
                ) {

                    // Avatar principal
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeUser != null) {
                             AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(activeUser?.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar of ${activeUser?.displayName}",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            when {
                                currentSelection is AvatarSelection.Custom && !imageLoadError -> {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                                            .crossfade(true)
                                            .listener(
                                                onStart = { isImageLoading = true },
                                                onSuccess = { _, _ ->
                                                    isImageLoading = false
                                                    imageLoadError = false
                                                },
                                                onError = { _, _ ->
                                                    isImageLoading = false
                                                    imageLoadError = true
                                                }
                                            )
                                            .build(),
                                    contentDescription = "Avatar of $accountName",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (isImageLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                currentSelection is AvatarSelection.DiceBear && !imageLoadError -> {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data((currentSelection as AvatarSelection.DiceBear).url)
                                            .crossfade(true)
                                            .listener(
                                                onStart = { isImageLoading = true },
                                                onSuccess = { _, _ ->
                                                    isImageLoading = false
                                                    imageLoadError = false
                                                },
                                                onError = { _, _ ->
                                                    isImageLoading = false
                                                    imageLoadError = true
                                                }
                                            )
                                            .build(),
                                        contentDescription = "DiceBear Avatar of $accountName",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (isImageLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    val initials = remember(accountName) {
                                        val cleanName = accountName.replace("@", "").trim()
                                        when {
                                            cleanName.isEmpty() -> "?"
                                            cleanName.contains(" ") -> {
                                                val parts = cleanName.split(" ")
                                                "${parts.first().firstOrNull()?.uppercase() ?: ""}${
                                                    parts.last().firstOrNull()?.uppercase() ?: ""
                                                }"
                                            }
                                            else -> cleanName.take(2).uppercase()
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(100f, 100f)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Online status indicator (optional)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 32.dp, y = 32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name with subtle animation
                AnimatedContent(
                    targetState = if (activeUser != null) activeUser!!.displayName ?: "User" else accountName.replace("@", "").takeIf { it.isNotBlank() } ?: "",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "username"
                ) { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }


            } else {
                // Not logged in state - tap to connect YouTube Music
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.clickable { navController.navigate("login") }
                ) {
                    // Logo with glassmorphism effect
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.echofy_monochrome),
                            contentDescription = "Echofy Logo",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Guest",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Tap to connect YouTube Music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Main settings category
        SettingsCategory(
            title = stringResource(R.string.general_settings),
            items = listOf(
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.person),
                    title = { Text(stringResource(R.string.account)) },
                    onClick = { navController.navigate("account_settings") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.appearance)) },
                    onClick = { navController.navigate("settings/appearance") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.content)) },
                    onClick = { navController.navigate("settings/content") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    onClick = { navController.navigate("settings/player") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.storage)) },
                    onClick = { navController.navigate("settings/storage") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.data_saver_on),
                    title = { Text("Saver") },
                    onClick = { navController.navigate("settings/saver") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.security),
                    title = { Text(stringResource(R.string.privacy)) },
                    onClick = { navController.navigate("settings/privacy") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text(stringResource(R.string.backup_restore)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                ),
            )
        )

        Spacer(Modifier.height(16.dp))

        // Community Section
        SettingsCategory(
            title = stringResource(R.string.community),
            items = listOf(
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.group),
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.Telegramchanel),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.join_telegram),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { uriHandler.openUri("https://t.me/echofyapp") }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.group),
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.whatsapp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.join_whatsapp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { uriHandler.openUri("https://chat.whatsapp.com/ItflzF589v46iXvUWBk6r3") }
                ),
            )
        )

        Spacer(Modifier.height(16.dp))


        // Version card
        VersionCard(uriHandler)
        
        // Bug Report Card
        BugReportCard()


        Spacer(Modifier.height(16.dp))
    }




    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
    }