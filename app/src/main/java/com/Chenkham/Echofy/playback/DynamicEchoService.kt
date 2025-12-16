package com.Chenkham.Echofy.playback

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.DynamicEchoStyle
import com.Chenkham.Echofy.constants.DynamicEchoStyleKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.ui.screens.settings.DarkMode
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.res.Configuration

/**
 * Service that manages the Dynamic Echo edge visualization overlay.
 * Shows wave or equalizer visualizations on top, left, and right edges.
 */
class DynamicEchoService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var visualizer: Visualizer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var visualizerJob: Job? = null

    // Playback state
    var isPlaying by mutableStateOf(false)
        private set
    
    // Visualization data (FFT amplitudes for 8 bands)
    var amplitudes by mutableStateOf(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
        private set
    
    // Echo style
    var echoStyle by mutableStateOf(DynamicEchoStyle.WAVE)
        private set
    
    // Theme state
    var isDarkTheme by mutableStateOf(true)
        private set
    var accentColor by mutableStateOf(Color(0xFF4285F4))
        private set

    companion object {
        private var instance: DynamicEchoService? = null

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, DynamicEchoService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DynamicEchoService::class.java)
            context.stopService(intent)
        }

        fun getInstance(): DynamicEchoService? = instance
        
        fun updateStyle(style: DynamicEchoStyle) {
            instance?.echoStyle = style
        }
        
        fun updateTheme(isDark: Boolean, accent: Color) {
            instance?.apply {
                isDarkTheme = isDark
                accentColor = accent
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        connectToMediaSession()
        updateThemeFromPreferences()
        updateStyleFromPreferences()
    }
    
    private fun updateThemeFromPreferences() {
        try {
            val darkModeString = dataStore[DarkModeKey]
            val isSystemDark = isSystemInDarkTheme()
            
            val darkMode = try {
                darkModeString?.let { DarkMode.valueOf(it) } ?: DarkMode.AUTO
            } catch (e: Exception) {
                DarkMode.AUTO
            }
            
            isDarkTheme = when (darkMode) {
                DarkMode.AUTO -> isSystemDark
                DarkMode.ON -> true
                DarkMode.OFF -> false
            }
        } catch (e: Exception) {
            isDarkTheme = isSystemInDarkTheme()
        }
    }
    
    private fun updateStyleFromPreferences() {
        try {
            val styleString = dataStore[DynamicEchoStyleKey]
            echoStyle = try {
                styleString?.let { DynamicEchoStyle.valueOf(it) } ?: DynamicEchoStyle.WAVE
            } catch (e: Exception) {
                DynamicEchoStyle.WAVE
            }
        } catch (e: Exception) {
            echoStyle = DynamicEchoStyle.WAVE
        }
    }
    
    private fun isSystemInDarkTheme(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == 
               Configuration.UI_MODE_NIGHT_YES
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        updateThemeFromPreferences()
        updateStyleFromPreferences()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        visualizerJob?.cancel()
        releaseVisualizer()
        removeOverlay()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        instance = null
        super.onDestroy()
    }

    private fun connectToMediaSession() {
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupPlayerListener()
            updateState()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    showOverlay()
                    setupVisualizer()
                } else {
                    releaseVisualizer()
                    removeOverlay()
                }
            }
        })
    }

    private fun updateState() {
        mediaController?.let { controller ->
            isPlaying = controller.isPlaying
            if (isPlaying) {
                showOverlay()
                setupVisualizer()
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun setupVisualizer() {
        try {
            releaseVisualizer()
            
            val audioSessionId = mediaController?.let {
                // Try to get audio session from player
                0 // Use output mix (0) for visualization
            } ?: 0
            
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Not used for this implementation
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { processFftData(it) }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            // Fallback to simulated visualization if Visualizer fails
            startSimulatedVisualization()
        }
    }
    
    private fun processFftData(fft: ByteArray) {
        // Process FFT data into 8 frequency bands
        val bands = FloatArray(8)
        val bandSize = (fft.size / 2) / 8
        
        for (i in 0 until 8) {
            var sum = 0f
            for (j in 0 until bandSize) {
                val index = (i * bandSize + j) * 2
                if (index + 1 < fft.size) {
                    val real = fft[index].toFloat()
                    val imaginary = fft[index + 1].toFloat()
                    val magnitude = kotlin.math.sqrt(real * real + imaginary * imaginary)
                    sum += magnitude
                }
            }
            bands[i] = (sum / bandSize / 128f).coerceIn(0f, 1f)
        }
        
        amplitudes = bands
    }
    
    private fun startSimulatedVisualization() {
        visualizerJob?.cancel()
        visualizerJob = serviceScope.launch {
            while (isActive && isPlaying) {
                // Generate simulated wave patterns
                val simulated = FloatArray(8) { 
                    (0.3f + Math.random().toFloat() * 0.7f)
                }
                amplitudes = simulated
                delay(50) // 20fps
            }
        }
    }
    
    private fun releaseVisualizer() {
        visualizerJob?.cancel()
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return

        updateThemeFromPreferences()
        updateStyleFromPreferences()
        
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicEchoService)
            setViewTreeSavedStateRegistryOwner(this@DynamicEchoService)
            
            setContent {
                DynamicEchoOverlay(
                    amplitudes = amplitudes,
                    isPlaying = isPlaying,
                    style = echoStyle,
                    isDarkTheme = isDarkTheme,
                    accentColor = accentColor
                )
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) { }
            overlayView = null
        }
    }
}
