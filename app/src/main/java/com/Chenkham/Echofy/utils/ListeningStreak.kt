package com.Chenkham.Echofy.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.Chenkham.Echofy.constants.ListeningStreakBestKey
import com.Chenkham.Echofy.constants.ListeningStreakCountKey
import com.Chenkham.Echofy.constants.ListeningStreakEnabledKey
import com.Chenkham.Echofy.constants.ListeningStreakLastDayKey
import com.Chenkham.Echofy.db.DatabaseDao
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Works out how many days in a row the user has listened to something, based on the
 * listening history already recorded in the `event` table.
 *
 * Nothing is tracked unless the user turns streaks on in settings.
 */
object ListeningStreak {

    /**
     * Recalculates the streak from history and stores the result. Returns the current
     * streak length, or 0 when the feature is disabled.
     */
    suspend fun refresh(
        context: Context,
        dao: DatabaseDao,
    ): Int {
        val enabled = context.dataStore.data.first()[ListeningStreakEnabledKey] ?: false
        if (!enabled) return 0

        val days = dao.listeningDays(currentUtcOffsetSeconds())
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (days.isEmpty()) return 0

        val today = LocalDate.now()
        val mostRecent = days.first()

        // A streak survives if the user listened today or yesterday; any older gap breaks it.
        if (mostRecent.isBefore(today.minusDays(1))) {
            context.dataStore.edit { prefs ->
                prefs[ListeningStreakCountKey] = 0
                prefs[ListeningStreakLastDayKey] = mostRecent.toEpochDay()
            }
            return 0
        }

        var streak = 1
        var expected = mostRecent.minusDays(1)
        for (day in days.drop(1)) {
            if (day == expected) {
                streak++
                expected = day.minusDays(1)
            } else if (day.isBefore(expected)) {
                break
            }
        }

        context.dataStore.edit { prefs ->
            prefs[ListeningStreakCountKey] = streak
            prefs[ListeningStreakLastDayKey] = mostRecent.toEpochDay()
            val best = prefs[ListeningStreakBestKey] ?: 0
            if (streak > best) prefs[ListeningStreakBestKey] = streak
        }

        return streak
    }
}
