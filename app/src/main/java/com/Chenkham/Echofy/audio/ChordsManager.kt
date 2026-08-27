package com.Chenkham.Echofy.audio

import kotlin.math.abs

data class ChordFingering(
    val name: String,
    val guitarFrets: List<Int>, // -1 for mute (X), 0 for open (O), 1..5 for fret number (6 strings: E A D G B e)
    val guitarFingers: List<Int> = listOf(), // 1=Index, 2=Middle, 3=Ring, 4=Pinky
    val ukeFrets: List<Int> = listOf(0, 0, 0, 0), // 4 strings: G C E A
    val baseFret: Int = 1
)

data class SongChordTimeline(
    val key: String,
    val chords: List<TimedChord>
)

data class TimedChord(
    val startMs: Long,
    val endMs: Long,
    val chord: String,
    val section: String // 'Intro', 'Verse', 'Chorus', 'Bridge', 'Outro'
)

object ChordsManager {
    // Chord Dictionary with accurate fingering positions
    val chordDictionary: Map<String, ChordFingering> = mapOf(
        "C" to ChordFingering("C", listOf(-1, 3, 2, 0, 1, 0), listOf(0, 3, 2, 0, 1, 0), listOf(0, 0, 0, 3)),
        "G" to ChordFingering("G", listOf(3, 2, 0, 0, 0, 3), listOf(2, 1, 0, 0, 0, 3), listOf(0, 2, 3, 2)),
        "Am" to ChordFingering("Am", listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), listOf(2, 0, 0, 0)),
        "F" to ChordFingering("F", listOf(1, 3, 3, 2, 1, 1), listOf(1, 3, 4, 2, 1, 1), listOf(2, 0, 1, 0)),
        "D" to ChordFingering("D", listOf(-1, -1, 0, 2, 3, 2), listOf(0, 0, 0, 1, 3, 2), listOf(2, 2, 2, 0)),
        "Em" to ChordFingering("Em", listOf(0, 2, 2, 0, 0, 0), listOf(0, 1, 2, 0, 0, 0), listOf(0, 4, 3, 2)),
        "A" to ChordFingering("A", listOf(-1, 0, 2, 2, 2, 0), listOf(0, 0, 1, 2, 3, 0), listOf(2, 1, 0, 0)),
        "E" to ChordFingering("E", listOf(0, 2, 2, 1, 0, 0), listOf(0, 2, 3, 1, 0, 0), listOf(1, 4, 0, 2)),
        "Dm" to ChordFingering("Dm", listOf(-1, -1, 0, 2, 3, 1), listOf(0, 0, 0, 2, 3, 1), listOf(2, 2, 1, 0)),
        "Bm" to ChordFingering("Bm", listOf(-1, 2, 4, 4, 3, 2), listOf(0, 1, 3, 4, 2, 1), listOf(4, 2, 2, 2)),
        "Cadd9" to ChordFingering("Cadd9", listOf(-1, 3, 2, 0, 3, 3), listOf(0, 2, 1, 0, 3, 4), listOf(0, 2, 0, 3)),
        "Dsus4" to ChordFingering("Dsus4", listOf(-1, -1, 0, 2, 3, 3), listOf(0, 0, 0, 1, 2, 4), listOf(0, 2, 3, 0)),
        "C7" to ChordFingering("C7", listOf(-1, 3, 2, 3, 1, 0), listOf(0, 3, 2, 4, 1, 0), listOf(0, 0, 0, 1)),
        "G7" to ChordFingering("G7", listOf(3, 2, 0, 0, 0, 1), listOf(3, 2, 0, 0, 0, 1), listOf(0, 2, 1, 2)),
        "D7" to ChordFingering("D7", listOf(-1, -1, 0, 2, 1, 2), listOf(0, 0, 0, 2, 1, 3), listOf(2, 0, 2, 0)),
        "A7" to ChordFingering("A7", listOf(-1, 0, 2, 0, 2, 0), listOf(0, 0, 2, 0, 3, 0), listOf(0, 1, 0, 0)),
        "E7" to ChordFingering("E7", listOf(0, 2, 0, 1, 0, 0), listOf(0, 2, 0, 1, 0, 0), listOf(1, 2, 0, 2)),
        "B7" to ChordFingering("B7", listOf(-1, 2, 1, 2, 0, 2), listOf(0, 2, 1, 3, 0, 4), listOf(2, 3, 2, 2)),
        "F#m" to ChordFingering("F#m", listOf(2, 4, 4, 2, 2, 2), listOf(1, 3, 4, 1, 1, 1), listOf(2, 1, 2, 0)),
        "Gm" to ChordFingering("Gm", listOf(3, 5, 5, 3, 3, 3), listOf(1, 3, 4, 1, 1, 1), listOf(0, 2, 3, 1)),
        "Bb" to ChordFingering("Bb", listOf(-1, 1, 3, 3, 3, 1), listOf(0, 1, 2, 3, 4, 1), listOf(3, 2, 1, 1))
    )

    private val progressionTemplates = listOf(
        // Key C / Am
        listOf("C", "G", "Am", "F"),
        listOf("Am", "F", "C", "G"),
        listOf("C", "Am", "F", "G"),
        // Key G / Em
        listOf("G", "D", "Em", "C"),
        listOf("Em", "C", "G", "D"),
        listOf("G", "Em", "C", "D"),
        // Key D / Bm
        listOf("D", "A", "Bm", "G"),
        listOf("Bm", "G", "D", "A"),
        // Key A / F#m
        listOf("A", "E", "F#m", "D"),
        // Key E / C#m
        listOf("E", "B7", "C#m", "A")
    )

    /**
     * Generates a coherent harmonic chord progression timeline for any song in real-time.
     * Uses deterministic hashing of song metadata to maintain consistent chords per track.
     */
    fun generateTimeline(songId: String, title: String, artist: String, durationMs: Long): SongChordTimeline {
        val totalMs = if (durationMs > 10000) durationMs else 210000L
        val seed = abs((songId + title + artist).hashCode())
        val progression = progressionTemplates[seed % progressionTemplates.size]
        val key = progression[0]

        val chordDurationMs = 3500L // ~2-4 measures per chord change
        val timedChords = mutableListOf<TimedChord>()
        var current = 0L
        var chordIdx = 0

        while (current < totalMs) {
            val end = minOf(current + chordDurationMs, totalMs)
            val progressPct = current.toFloat() / totalMs

            val section = when {
                progressPct < 0.08f -> "Intro"
                progressPct in 0.08f..0.35f -> "Verse"
                progressPct in 0.35f..0.55f -> "Chorus"
                progressPct in 0.55f..0.75f -> "Verse 2"
                progressPct in 0.75f..0.88f -> "Bridge"
                else -> "Outro"
            }

            val chordName = progression[chordIdx % progression.size]
            timedChords.add(TimedChord(current, end, chordName, section))
            current = end
            chordIdx++
        }

        return SongChordTimeline(key, timedChords)
    }

    fun getChordFingering(chordName: String): ChordFingering {
        return chordDictionary[chordName] ?: ChordFingering(chordName, listOf(0, 0, 0, 0, 0, 0))
    }
}
