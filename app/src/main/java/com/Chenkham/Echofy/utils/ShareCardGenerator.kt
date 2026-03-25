package com.Chenkham.Echofy.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.applyCanvas
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates beautiful branded share cards for Echofy
 * Features: Gradients, glassmorphism, song info, artwork
 */
object ShareCardGenerator {
    
    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1920
    private const val PADDING = 80
    
    /**
     * Generate a branded share card
     * @param context Android context
     * @param songTitle Title of the song
     * @param artistName Artist name
     * @param albumArtUrl URL to album artwork
     * @return Bitmap of the generated card
     */
    suspend fun generateShareCard(
        context: Context,
        songTitle: String,
        artistName: String,
        albumArtUrl: String?,
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 1. Draw gradient background
        drawGradientBackground(canvas)
        
        // 2. Draw album artwork (if available)
        val artwork = albumArtUrl?.let { loadImage(context, it) }
        if (artwork != null) {
            drawBlurredArtwork(canvas, artwork)
            drawArtwork(canvas, artwork)
        }
        
        // 3. Draw song info with glassmorphism
        drawSongInfo(canvas, songTitle, artistName)
        
        // 4. Draw Echofy branding
        drawBranding(canvas)
        
        bitmap
    }
    
    private fun drawGradientBackground(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f, 0f, CARD_HEIGHT.toFloat(),
            intArrayOf(
                0xFF1a1a2e.toInt(), // Dark blue-purple
                0xFF16213e.toInt(), // Navy
                0xFF0f3460.toInt()  // Deep blue
            ),
            null,
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
            isAntiAlias = true
        }
        
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), paint)
    }
    
    private suspend fun loadImage(context: Context, url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            
            val result = (ImageLoader(context).execute(request) as? SuccessResult)
                ?.drawable as? android.graphics.drawable.BitmapDrawable
            
            result?.bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    private fun drawBlurredArtwork(canvas: Canvas, artwork: Bitmap) {
        // Draw large blurred background artwork
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = 60 // 25% opacity
        }
        
        val scaledArtwork = Bitmap.createScaledBitmap(artwork, CARD_WIDTH, CARD_WIDTH, true)
        canvas.drawBitmap(scaledArtwork, 0f, 200f, paint)
    }
    
    private fun drawArtwork(canvas: Canvas, artwork: Bitmap) {
        val artworkSize = 800
        val x = (CARD_WIDTH - artworkSize) / 2f
        val y = 400f
        
        // Draw shadow
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = 0x80000000.toInt()
            maskFilter = android.graphics.BlurMaskFilter(40f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRect(x + 20f, y + 20f, x + artworkSize + 20f, y + artworkSize + 20f, shadowPaint)
        
        // Draw artwork with rounded corners
        val scaledArtwork = Bitmap.createScaledBitmap(artwork, artworkSize, artworkSize, true)
        val roundedArtwork = createRoundedBitmap(scaledArtwork, 60f)
        canvas.drawBitmap(roundedArtwork, x, y, Paint().apply { isAntiAlias = true })
    }
    
    private fun createRoundedBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        
        canvas.drawRoundRect(
            0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(),
            radius, radius, paint
        )
        
        return output
    }
    
    private fun drawSongInfo(canvas: Canvas, title: String, artist: String) {
        val containerY = 1300f
        val containerHeight = 300f
        
        // Glassmorphism background
        val glassPaint = Paint().apply {
            color = 0x40FFFFFF.toInt() // 25% white
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            PADDING.toFloat(),
            containerY,
            CARD_WIDTH - PADDING.toFloat(),
            containerY + containerHeight,
            40f, 40f,
            glassPaint
        )
        
        // Song title
        val titlePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val truncatedTitle = truncateText(title, titlePaint, CARD_WIDTH - PADDING * 2f - 80f)
        canvas.drawText(truncatedTitle, PADDING + 40f, containerY + 100f, titlePaint)
        
        // Artist name
        val artistPaint = Paint().apply {
            color = 0xCCFFFFFF.toInt() // 80% white
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val truncatedArtist = truncateText(artist, artistPaint, CARD_WIDTH - PADDING * 2f - 80f)
        canvas.drawText(truncatedArtist, PADDING + 40f, containerY + 170f, artistPaint)
        
        // "Tap to listening" text
        val ctaPaint = Paint().apply {
            color = 0xFFE94560.toInt() // Vibrant red/pink
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("♪ Listen on Echofy", PADDING + 40f, containerY + 240f, ctaPaint)
    }
    
    private fun drawBranding(canvas: Canvas) {
        // Echofy logo text at bottom
        val logoPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("ECHOFY", CARD_WIDTH / 2f, CARD_HEIGHT - 100f, logoPaint)
        
        // Tagline
        val taglinePaint = Paint().apply {
            color = 0x99FFFFFF.toInt() // 60% white
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Your Music, Amplified", CARD_WIDTH / 2f, CARD_HEIGHT - 40f, taglinePaint)
    }
    
    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        var truncated = text
        while (paint.measureText(truncated) > maxWidth && truncated.isNotEmpty()) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.length < text.length) "$truncated..." else text
    }
}
