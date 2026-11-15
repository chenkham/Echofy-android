package com.Chenkham.Echofy

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.constants.DisableScreenshotKey
import com.Chenkham.Echofy.constants.HardwareVolButtonSkipKey
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.MusicService
import com.Chenkham.Echofy.playback.MusicService.MusicBinder
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.LocaleManager
import com.Chenkham.Echofy.ui.main.MainAppContent
import com.Chenkham.Echofy.ui.main.MainIntentHandler
import com.Chenkham.Echofy.ui.main.NotificationDeepLinkManager
import com.Chenkham.Echofy.ui.main.PictureInPictureHelper
import com.Chenkham.Echofy.ui.screens.settings.NavigationTab
import com.Chenkham.Echofy.utils.SyncUtils
import com.Chenkham.Echofy.utils.dataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Spotify-grade single-activity entry point for Echofy.
 * Lifecycle, IPC service connection, and system hardware events are coordinated here.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: MusicDatabase

    @Inject lateinit var downloadUtil: DownloadUtil

    @Inject lateinit var syncUtils: SyncUtils

    @Inject lateinit var adManager: AdManager

    @Inject lateinit var authRepository: com.Chenkham.Echofy.auth.AuthRepository

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isInPipMode by mutableStateOf(false)
    private var pipEnabled = true
    private var autoClearCache = false
    private var stopMusicOnTaskClear = false
    private var hardwareVolumeSkipEnabled = false
    private var lastUserInteractionTime = System.currentTimeMillis()
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)
    private var pendingJamRoomCode by mutableStateOf<String?>(null)

    private val pipHelper by lazy { PictureInPictureHelper(this) { playerConnection } }
    private val notificationDeepLinkManager by lazy { NotificationDeepLinkManager(this) }
    private val mainIntentHandler by lazy {
        MainIntentHandler(
            context = this,
            scope = lifecycleScope,
            database = database,
            downloadUtil = downloadUtil,
            dataStore = dataStore,
            playerConnectionProvider = { playerConnection },
            onPendingJamRoomCode = { pendingJamRoomCode = it },
        )
    }

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lastUserInteractionTime = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        notificationDeepLinkManager.handleNotificationDeepLink(intent)
    }

    fun consumePendingNotificationNavigation(): String? = notificationDeepLinkManager.consumePendingNotificationNavigation()

    private var volumeDownLongPressStartTime = 0L
    private var volumeUpLongPressStartTime = 0L

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (hardwareVolumeSkipEnabled) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN && volumeDownLongPressStartTime == 0L) {
                        volumeDownLongPressStartTime = System.currentTimeMillis()
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        val held = System.currentTimeMillis() - volumeDownLongPressStartTime
                        volumeDownLongPressStartTime = 0L
                        if (held >= 600L) {
                            playerConnection?.player?.seekToPreviousMediaItem()
                            return true
                        }
                    }
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN && volumeUpLongPressStartTime == 0L) {
                        volumeUpLongPressStartTime = System.currentTimeMillis()
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        val held = System.currentTimeMillis() - volumeUpLongPressStartTime
                        volumeUpLongPressStartTime = 0L
                        if (held >= 600L) {
                            playerConnection?.player?.seekToNextMediaItem()
                            return true
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) adManager.releaseNativeAds()
        pipHelper.unregister(this)
        runCatching { unbindService(serviceConnection) }
        if (autoClearCache) {
            runCatching {
                cacheDir.resolve("coil").deleteRecursively()
                cacheDir.resolve("exoplayer").deleteRecursively()
            }
        }
        if (stopMusicOnTaskClear && playerConnection?.isPlaying?.value == true && isFinishing) {
            stopService(Intent(this, MusicService::class.java))
            playerConnection = null
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerConnection?.isPlaying?.value == true && pipEnabled) pipHelper.enterPipMode()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipHelper.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipMode = isInPictureInPictureMode
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.getInstance(newBase).applyLocaleToContext(newBase))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data.map { it[DisableScreenshotKey] ?: false }.distinctUntilChanged().collectLatest {
                if (it) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        lifecycleScope.launch {
            dataStore.data.map { it[HardwareVolButtonSkipKey] ?: false }.distinctUntilChanged().collectLatest {
                hardwareVolumeSkipEnabled = it
            }
        }

        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        intent?.let { mainIntentHandler.handleIntent(it) }
        lifecycleScope.launch { runCatching { com.Chenkham.Echofy.utils.DynamicShortcuts.refresh(this@MainActivity, database) } }
        pipHelper.register(this)

        setContent {
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { /* Logged internally */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MainAppContent(
                activity = this,
                playerConnection = playerConnection,
                isInPipMode = isInPipMode,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
                adManager = adManager,
                latestVersionName = latestVersionName,
                pendingJamRoomCode = pendingJamRoomCode,
                onClearJamRoomCode = { pendingJamRoomCode = null },
                getLastInteractionTime = { lastUserInteractionTime },
                onResetInteractionTime = { lastUserInteractionTime = System.currentTimeMillis() },
                searchActionTriggered = intent?.action == ACTION_SEARCH,
                shortcutDestinationTab =
                    if (intent?.action == ACTION_LIBRARY) {
                        NavigationTab.LIBRARY
                    } else if (intent?.action == ACTION_EXPLORE) {
                        NavigationTab.EXPLORE
                    } else {
                        null
                    },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainIntentHandler.handleIntent(intent)
    }

    companion object {
        const val ACTION_SEARCH = "com.Chenkham.Echofy.action.SEARCH"
        const val ACTION_EXPLORE = "com.Chenkham.Echofy.action.EXPLORE"
        const val ACTION_LIBRARY = "com.Chenkham.Echofy.action.LIBRARY"
    }
}
