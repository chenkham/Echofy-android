package com.Chenkham.Echofy.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.graphics.HardwareRenderer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.Chenkham.Echofy.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class PlayerWidgetState(
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String? = null,
    val artworkBitmap: Bitmap? = null,
    val backgroundBlurBitmap: Bitmap? = null,
    val dominantColor: Int? = null,
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        suspend fun fromService(
            context: Context,
            player: Player,
            metadata: MediaMetadata?,
            currentArtworkBitmap: Bitmap? = null,
        ): PlayerWidgetState {
            val artworkBitmap = currentArtworkBitmap
                ?: metadata?.thumbnailUrl?.let { loadArtworkFromUrl(context, it) }

            val blurBitmap = artworkBitmap?.let { computeBackgroundBlur(it) }
            val dominantColor = artworkBitmap?.let { computeDominantColor(it) }

            return PlayerWidgetState(
                title = metadata?.title.orEmpty(),
                artist = metadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                thumbnailUrl = metadata?.thumbnailUrl,
                artworkBitmap = artworkBitmap,
                backgroundBlurBitmap = blurBitmap,
                dominantColor = dominantColor,
                isPlaying = player.isPlaying,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem(),
                durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                positionMs = player.currentPosition.coerceAtLeast(0L),
            )
        }

        private suspend fun computeBackgroundBlur(source: Bitmap): Bitmap? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

            return withContext(Dispatchers.Default) {
                try {
                    blurBitmapApi31(source, radius = 28f, downscaleTo = 64)
                } catch (e: Exception) {
                    Timber.tag("PlayerWidgetState")
                        .w(e, "Background blur failed, falling back to dominant color")
                    null
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun blurBitmapApi31(source: Bitmap, radius: Float, downscaleTo: Int): Bitmap? {
            val ratio = source.width.toFloat() / source.height.toFloat()
            val width =
                if (ratio >= 1f) downscaleTo else (downscaleTo * ratio).toInt().coerceAtLeast(1)
            val height =
                if (ratio >= 1f) (downscaleTo / ratio).toInt().coerceAtLeast(1) else downscaleTo
            val small = Bitmap.createScaledBitmap(source, width, height, true)

            var reader: ImageReader? = null
            return try {
                reader = ImageReader.newInstance(
                    small.width,
                    small.height,
                    android.graphics.PixelFormat.RGBA_8888,
                    1,
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
                )

                val renderNode = RenderNode("widgetBlur")
                renderNode.setPosition(0, 0, small.width, small.height)
                renderNode.setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
                )

                val canvas = renderNode.beginRecording()
                canvas.drawBitmap(small, 0f, 0f, null)
                renderNode.endRecording()

                val hardwareRenderer = HardwareRenderer()
                hardwareRenderer.setSurface(reader.surface)
                hardwareRenderer.setContentRoot(renderNode)
                hardwareRenderer.createRenderRequest()
                    .setWaitForPresent(true)
                    .syncAndDraw()

                val image = reader.acquireNextImage()
                    ?: return null
                val hardwareBuffer = image.hardwareBuffer
                    ?: return null.also { image.close() }

                val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                hardwareBuffer.close()
                image.close()
                hardwareRenderer.destroy()

                val softwareCopy = hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                hardwareBitmap?.recycle()
                softwareCopy
            } finally {
                reader?.close()
                if (small != source) small.recycle()
            }
        }

        private suspend fun computeDominantColor(bitmap: Bitmap): Int? =
            withContext(Dispatchers.Default) {
                try {
                    val palette = Palette.from(bitmap).generate()
                    palette.vibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                } catch (e: Exception) {
                    Timber.tag("PlayerWidgetState").w(e, "Dominant color extraction failed")
                    null
                }
            }

        private suspend fun loadArtworkFromUrl(context: Context, url: String): Bitmap? {
            if (url.isBlank()) return null

            return withContext(Dispatchers.IO) {
                try {
                    val imageLoader = context.imageLoader
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(200, 200)
                        .allowHardware(false)
                        .build()

                    val result = imageLoader.execute(request)
                    when (result) {
                        is SuccessResult -> (result as? SuccessResult)?.drawable?.toBitmap()
                        else -> null
                    }
                } catch (e: Exception) {
                    Timber.tag("PlayerWidgetState").e(e, "Failed to load artwork from URL: $url")
                    null
                }
            }
        }

        fun fromPreferences(preferences: Preferences): PlayerWidgetState {
            val bytes = preferences[PlayerWidgetStateKeys.ArtworkBytes]
            val bitmap = bytes?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            val blurBytes = preferences[PlayerWidgetStateKeys.BackgroundBlurBytes]
            val blurBitmap = blurBytes?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            return PlayerWidgetState(
                title = preferences[PlayerWidgetStateKeys.Title].orEmpty(),
                artist = preferences[PlayerWidgetStateKeys.Artist].orEmpty(),
                thumbnailUrl = preferences[PlayerWidgetStateKeys.ThumbnailUrl],
                artworkBitmap = bitmap,
                backgroundBlurBitmap = blurBitmap,
                dominantColor = preferences[PlayerWidgetStateKeys.DominantColor],
                isPlaying = preferences[PlayerWidgetStateKeys.IsPlaying] ?: false,
                hasPrevious = preferences[PlayerWidgetStateKeys.HasPrevious] ?: false,
                hasNext = preferences[PlayerWidgetStateKeys.HasNext] ?: false,
                durationMs = preferences[PlayerWidgetStateKeys.DurationMs] ?: 0L,
                positionMs = preferences[PlayerWidgetStateKeys.PositionMs] ?: 0L,
            )
        }
    }
}

object PlayerWidgetStateKeys {
    val Title = stringPreferencesKey("title")
    val Artist = stringPreferencesKey("artist")
    val ThumbnailUrl = stringPreferencesKey("thumbnail_url")
    val IsPlaying = booleanPreferencesKey("is_playing")
    val HasPrevious = booleanPreferencesKey("has_previous")
    val HasNext = booleanPreferencesKey("has_next")
    val DurationMs = longPreferencesKey("duration_ms")
    val PositionMs = longPreferencesKey("position_ms")
    val ArtworkBytes = byteArrayPreferencesKey("artwork_bytes")
    val BackgroundBlurBytes = byteArrayPreferencesKey("background_blur_bytes")
    val DominantColor = intPreferencesKey("dominant_color")
}
