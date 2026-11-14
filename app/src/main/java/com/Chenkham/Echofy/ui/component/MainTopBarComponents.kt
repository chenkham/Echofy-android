package com.Chenkham.Echofy.ui.component

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.Echofy.R
import timber.log.Timber

/**
 * Switch preference widget for toggling runtime notification permissions.
 */
@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val checkNotificationPermission =
        remember {
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            permissionGranted = isGranted
            if (!isGranted) {
                Timber.d("Notification permission denied")
            }
        }

    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    permissionGranted = checkNotificationPermission()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        icon = {
            Icon(
                painter =
                    painterResource(
                        id =
                            if (permissionGranted) {
                                R.drawable.notification_on
                            } else {
                                R.drawable.notification_off
                            },
                    ),
                contentDescription =
                    stringResource(
                        if (permissionGranted) {
                            R.string.notifications_enabled
                        } else {
                            R.string.notifications_disabled
                        },
                    ),
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            when {
                checked && !permissionGranted -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                }
                !checked && permissionGranted -> {
                    openNotificationSettings(context)
                }
            }
        },
    )
}

/**
 * Open system app notification settings screen.
 */
fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Timber.e(e, "Failed to open notification settings")
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

/**
 * Top bar profile icon with update indicator badge.
 */
@Composable
fun ProfileIconWithUpdateBadge(
    currentVersion: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
    var showUpdateBadge by remember { mutableStateOf(false) }
    val updatedOnClick = rememberUpdatedState(onProfileClick)

    val infiniteTransition = rememberInfiniteTransition(label = "badge_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    try {
                        updatedOnClick.value()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (currentSelection) {
                is AvatarSelection.Custom -> {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                                .crossfade(true)
                                .error(R.drawable.person)
                                .placeholder(R.drawable.person)
                                .build(),
                        contentDescription = "Avatar",
                        modifier =
                            modifier
                                .size(28.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }

                is AvatarSelection.DiceBear -> {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data((currentSelection as AvatarSelection.DiceBear).url)
                                .crossfade(true)
                                .error(R.drawable.person)
                                .placeholder(R.drawable.person)
                                .build(),
                        contentDescription = "Avatar DiceBear",
                        modifier =
                            modifier
                                .size(28.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Avatar",
                        modifier = modifier,
                    )
                }
            }
        }

        if (showUpdateBadge) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .scale(scale)
                            .background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha),
                                                Color.Transparent,
                                            ),
                                    ),
                                shape = CircleShape,
                            ),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = CircleShape,
                            ),
                )

                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = "Update available",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .align(Alignment.Center)
                            .scale(scale)
                            .alpha(alpha),
                )
            }
        }
    }
}
