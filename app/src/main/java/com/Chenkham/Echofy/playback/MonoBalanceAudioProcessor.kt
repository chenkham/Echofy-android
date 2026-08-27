package com.Chenkham.Echofy.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.C
import java.nio.ByteBuffer

/**
 * Applies mono downmixing, left/right balance and karaoke vocal suppression to 16-bit PCM
 * stereo audio.
 *
 * Mono downmixing helps users listening with a single earbud or with hearing loss in one
 * ear, who would otherwise lose anything panned to the missing side. Balance shifts the
 * mix towards one ear for the same reason.
 *
 * The processor is a no-op unless the user turns one of the options on.
 */
class MonoBalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var monoEnabled: Boolean = false

    /** -1 = full left, 0 = centred, 1 = full right. */
    @Volatile
    var balance: Float = 0f

    /**
     * Karaoke vocal suppression, 0 = off through 1 = full cancellation.
     *
     * Works by subtracting the right channel from the left, which removes whatever sits
     * dead centre in the stereo image. Lead vocals are usually mixed there, but so are
     * bass and kick, and the result is mono by definition. This is centre cancellation,
     * not source separation, so how well it works depends entirely on the mix; the level
     * is adjustable so a track that ends up thin can be dialled back.
     */
    @Volatile
    var vocalSuppression: Float = 0f

    private val isNoOp: Boolean
        get() = !monoEnabled && balance == 0f && vocalSuppression == 0f

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        // Only 16-bit stereo PCM is handled; anything else passes through untouched.
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount != 2
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean = super.isActive() && !isNoOp

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (isNoOp) {
            val count = inputBuffer.remaining()
            val outputBuffer = replaceOutputBuffer(count)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val frameCount = (limit - position) / 4
        val outputBuffer = replaceOutputBuffer(frameCount * 4)

        // Constant-gain panning: one side is attenuated rather than the other boosted, so
        // the output can never clip.
        val leftGain = if (balance > 0f) (1f - balance).coerceIn(0f, 1f) else 1f
        val rightGain = if (balance < 0f) (1f + balance).coerceIn(0f, 1f) else 1f

        var index = position
        while (index < limit) {
            val left = inputBuffer.getShort(index)
            val right = inputBuffer.getShort(index + 2)

            var outLeft: Int
            var outRight: Int

            if (vocalSuppression > 0f) {
                // Centre cancellation has to happen before any downmix, because mixing the
                // channels together first would destroy the very difference it relies on.
                // Blended against the dry signal so partial suppression stays musical, and
                // halved to offset the level jump subtraction causes on wide material.
                val difference = (left - right) / 2
                val dry = (left + right) / 2
                val wet = difference
                val mixed = (dry * (1f - vocalSuppression) + wet * vocalSuppression).toInt()
                outLeft = mixed
                outRight = mixed
            } else if (monoEnabled) {
                val mixed = (left + right) / 2
                outLeft = mixed
                outRight = mixed
            } else {
                outLeft = left.toInt()
                outRight = right.toInt()
            }

            outLeft = (outLeft * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            outRight = (outRight * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            outputBuffer.putShort(outLeft.toShort())
            outputBuffer.putShort(outRight.toShort())

            index += 4
        }

        inputBuffer.position(limit)
        outputBuffer.flip()
    }
}
