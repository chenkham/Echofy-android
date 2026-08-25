package com.Chenkham.Echofy.utils

import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The device's current offset from UTC, in seconds.
 *
 * Listening history is stored as epoch milliseconds in UTC, but every date the UI reasons
 * about comes from LocalDate.now(). Queries that group history by day or by hour therefore
 * have to shift the stored value into local time first, or a user east of UTC sees plays
 * land on the wrong day and mood buckets match the wrong hours.
 *
 * Read at call time rather than cached, so it stays correct across DST changes and travel.
 */
fun currentUtcOffsetSeconds(): Int =
    ZonedDateTime.now(ZoneId.systemDefault()).offset.totalSeconds
