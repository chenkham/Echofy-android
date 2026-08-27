package com.Chenkham.Echofy.audio

enum class HarmonicBassMode(val label: String, val description: String, val subGain: Float, val harmonicGain: Float) {
    PUNCHY("Punchy 808", "Tight, snappy kicks with clear transient punch", 0.35f, 0.20f),
    RUMBLE("Cinema Deep Rumble", "Ultra-low sub-bass rumble without distortion", 0.45f, 0.12f),
    CLUB("Club Bassline", "High-energy dance floor sub-bass saturation", 0.40f, 0.28f),
    WARMTH("Acoustic Warmth", "Natural acoustic body for rock, jazz and vocals", 0.20f, 0.30f)
}

object HarmonicBassManager {
    fun getModes(): List<HarmonicBassMode> = HarmonicBassMode.values().toList()

    fun findMode(name: String): HarmonicBassMode {
        return HarmonicBassMode.values().find { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) }
            ?: HarmonicBassMode.PUNCHY
    }

    /**
     * Applies psychoacoustic harmonic overtone synthesis to EQ band levels.
     * Takes existing 5-band levels (0..1) and synthesizes the missing fundamental
     * by boosting band 0 (60Hz) and its harmonic overtone band 1 (230Hz).
     */
    fun applyHarmonics(
        baseLevels: List<Float>,
        intensityPct: Int, // 0..100
        mode: HarmonicBassMode
    ): List<Float> {
        if (baseLevels.size < 5 || intensityPct <= 0) return baseLevels
        val factor = (intensityPct / 100f).coerceIn(0f, 1f)

        val result = baseLevels.toMutableList()
        // Sub-bass band (60Hz)
        result[0] = (result[0] + (mode.subGain * factor)).coerceIn(0f, 1f)
        // First harmonic band (230Hz) - provides the psychoacoustic overtone
        result[1] = (result[1] + (mode.harmonicGain * factor)).coerceIn(0f, 1f)

        return result
    }
}
