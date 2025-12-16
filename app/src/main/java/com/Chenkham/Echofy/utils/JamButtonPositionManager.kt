package com.Chenkham.Echofy.utils

import android.content.Context

/**
 * Manages saving and restoring the floating Jam button position.
 * Position is persisted across app restarts using SharedPreferences.
 */
object JamButtonPositionManager {
    private const val PREFS_NAME = "jam_button_prefs"
    private const val KEY_X = "button_x"
    private const val KEY_Y = "button_y"
    
    // Default position: bottom-right corner (relative to screen)
    private const val DEFAULT_X = -1f // -1 means use default
    private const val DEFAULT_Y = -1f
    
    /**
     * Save button position to SharedPreferences
     */
    fun savePosition(context: Context, x: Float, y: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_X, x)
            .putFloat(KEY_Y, y)
            .apply()
    }
    
    /**
     * Get saved button position or defaults
     * @return Pair of (x, y) coordinates. Returns (-1, -1) if no position saved.
     */
    fun getPosition(context: Context): Pair<Float, Float> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(
            prefs.getFloat(KEY_X, DEFAULT_X),
            prefs.getFloat(KEY_Y, DEFAULT_Y)
        )
    }
    
    /**
     * Check if position has been saved before
     */
    fun hasPosition(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_X) && prefs.contains(KEY_Y)
    }
    
    /**
     * Clear saved position
     */
    fun clearPosition(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
