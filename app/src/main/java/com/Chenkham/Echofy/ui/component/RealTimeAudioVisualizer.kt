package com.Chenkham.Echofy.ui.component

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

@Composable
fun RealTimeAudioVisualizer(
    audioSessionId: Int,
    color: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean,
    barCount: Int = 28,
) {
    val amplitudes = remember(barCount) {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0.08f) }
        }
    }
    var hasLiveCapture by remember(audioSessionId, isActive, barCount) { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val fallbackTransition = rememberInfiniteTransition(label = "visualizerFallback")
    val fallbackPhase = fallbackTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fallbackPhase",
    ).value

    DisposableEffect(audioSessionId, isActive, barCount) {
        amplitudes.indices.forEach { index -> amplitudes[index] = 0.08f }
        hasLiveCapture = false

        if (!isActive || audioSessionId <= 0) {
            onDispose { }
        } else {
            val visualizer = try {
                Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int,
                            ) {
                                val snapshot = waveform ?: return
                                val grouped = mapWaveformToAmplitudes(snapshot, barCount)
                                mainHandler.post {
                                    grouped.forEachIndexed { index, value ->
                                        val current = amplitudes[index]
                                        amplitudes[index] = (current * 0.42f) + (value * 0.58f)
                                    }
                                }
                            }

                            override fun onFftDataCapture(
                                visualizer: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int,
                            ) = Unit
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        true,
                        false,
                    )
                    enabled = true
                }
            } catch (_: Throwable) {
                null
            }
            hasLiveCapture = visualizer != null

            onDispose {
                hasLiveCapture = false
                visualizer?.runCatching {
                    enabled = false
                    release()
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        val barWidth = size.width / (barCount * 1.7f)
        val gap = barWidth * 0.7f
        val minBarHeight = size.height * 0.12f

        for (index in 0 until barCount) {
            val fallbackWave =
                0.18f + (((fallbackPhase + (index * 0.07f)) % 1f) * (1f - ((fallbackPhase + (index * 0.07f)) % 1f))) * 0.9f
            val amplitude =
                if (hasLiveCapture) amplitudes[index].coerceIn(0.08f, 1f)
                else fallbackWave
            val barHeight = max(minBarHeight, size.height * amplitude)
            val left = index * (barWidth + gap)
            val top = (size.height - barHeight) / 2f

            drawRoundRect(
                color = color.copy(alpha = 0.22f + (0.55f * amplitude.coerceIn(0f, 1f))),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

private fun mapWaveformToAmplitudes(
    waveform: ByteArray,
    barCount: Int,
): List<Float> {
    if (waveform.isEmpty() || barCount <= 0) return List(barCount) { 0.08f }

    val chunkSize = max(1, waveform.size / barCount)
    return List(barCount) { index ->
        val start = index * chunkSize
        val end = minOf(waveform.size, start + chunkSize)
        if (start >= end) {
            0.08f
        } else {
            var total = 0f
            for (cursor in start until end) {
                val centeredSample = ((waveform[cursor].toInt() and 0xFF) - 128) / 128f
                total += abs(centeredSample)
            }
            ((total / (end - start)) * 1.35f).coerceIn(0.08f, 1f)
        }
    }
}
