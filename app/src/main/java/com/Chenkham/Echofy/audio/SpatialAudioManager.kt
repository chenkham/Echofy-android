package com.Chenkham.Echofy.audio

import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object SpatialAudioManager {
    private const val TAG = "SpatialAudioManager"
    private var virtualizer: Virtualizer? = null
    private var orbitJob: Job? = null
    private var currentSessionId: Int = 0

    fun setup(
        sessionId: Int,
        enabled: Boolean,
        strength: Int, // 0..1000
        orbit8D: Boolean,
        scope: CoroutineScope,
        onPanUpdate: ((Float) -> Unit)? = null
    ) {
        if (sessionId <= 0) return

        try {
            if (virtualizer == null || currentSessionId != sessionId) {
                runCatching { virtualizer?.release() }
                virtualizer = Virtualizer(0, sessionId).apply {
                    currentSessionId = sessionId
                }
            }

            virtualizer?.let { virt ->
                virt.enabled = enabled
                if (enabled) {
                    virt.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }

            // Manage 8D Orbit Pan Loop
            orbitJob?.cancel()
            if (enabled && orbit8D) {
                orbitJob = scope.launch(Dispatchers.Default) {
                    var angle = 0.0
                    val speed = 0.05 // complete 360 rotation in ~12 seconds
                    while (isActive) {
                        angle += speed
                        val pan = sin(angle).toFloat() // -1.0 (Left) to +1.0 (Right)
                        withContext(Dispatchers.Main) {
                            onPanUpdate?.invoke(pan)
                        }
                        delay(50) // 20 FPS smooth orbital modulation
                    }
                }
            } else {
                onPanUpdate?.invoke(0f) // Center
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure SpatialAudio: ")
        }
    }

    fun release() {
        orbitJob?.cancel()
        orbitJob = null
        runCatching {
            virtualizer?.enabled = false
            virtualizer?.release()
        }
        virtualizer = null
        currentSessionId = 0
    }
}
