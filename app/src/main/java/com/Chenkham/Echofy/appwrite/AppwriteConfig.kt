package com.Chenkham.Echofy.appwrite

/**
 * Single source of truth for all Appwrite configuration.
 *
 * TO SWITCH FROM CLOUD → SELF-HOSTED VPS:
 *   Change ENDPOINT to your VPS URL, e.g. "https://appwrite.yourdomain.com/v1"
 *   Everything else stays the same.
 */
object AppwriteConfig {

    // Cloud:       "https://sgp.cloud.appwrite.io/v1"
    // Self-hosted: "https://appwrite.yourdomain.com/v1"
    const val ENDPOINT   = "https://sgp.cloud.appwrite.io/v1"
    const val PROJECT_ID = "69e369ed001887816869"

    // ─── Database ─────────────────────────────────────────────────────────
    const val DATABASE_ID    = "echofy"

    // 2 collections only — no queue, no separate playback doc
    const val COL_ROOMS    = "together_rooms"     // room meta + current playback state (1 doc per room)
    const val COL_PRESENCE = "together_presence"  // 1 doc per participant
}
