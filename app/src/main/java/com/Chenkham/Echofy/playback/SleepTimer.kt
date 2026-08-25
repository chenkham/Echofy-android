package com.Chenkham.Echofy.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    val player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    var triggerTime by mutableStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    /**
     * Set by [MusicService] from user preferences. When enabled the volume is ramped down
     * over [fadeDurationSeconds] before pausing, so the music does not cut out abruptly and
     * wake the listener.
     */
    var fadeOutEnabled: Boolean = false
    var fadeDurationSeconds: Int = 30

    fun start(minute: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minute == -1) {
            pauseWhenSongEnd = true
        } else {
            triggerTime = System.currentTimeMillis() + minute.minutes.inWholeMilliseconds
            sleepTimerJob =
                scope.launch {
                    val totalMillis = minute.minutes.inWholeMilliseconds
                    val fadeMillis =
                        if (fadeOutEnabled) {
                            // Never fade for longer than the timer itself.
                            (fadeDurationSeconds * 1000L).coerceAtMost(totalMillis)
                        } else {
                            0L
                        }

                    delay(totalMillis - fadeMillis)

                    if (fadeMillis > 0L) {
                        fadeOutVolume(fadeMillis)
                    }

                    player.pause()
                    triggerTime = -1L
                }
        }
    }

    /**
     * Ramps [Player.volume] to zero across [durationMillis], then restores the original
     * value so the next playback session starts at the user's normal level.
     */
    private suspend fun fadeOutVolume(durationMillis: Long) {
        val startVolume = player.volume
        val steps = 40
        val stepDelay = durationMillis / steps

        try {
            for (step in 1..steps) {
                player.volume = startVolume * (1f - step.toFloat() / steps)
                delay(stepDelay)
            }
        } finally {
            // Runs even if the timer is cancelled mid-fade, so the user is never left
            // with a silent player.
            player.volume = startVolume
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }
}
