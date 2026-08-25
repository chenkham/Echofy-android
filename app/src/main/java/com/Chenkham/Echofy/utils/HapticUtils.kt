package com.Chenkham.Echofy.utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class HapticType {
    Click,
    ToggleOn,
    ToggleOff,
    Confirm,
    Reject,
    LongPress,
    Keyboard,
    SegmentTick,
    SegmentFrequentTick,
    DragStart,
    GestureEnd
}

class HapticManager(
    private val view: View,
    private val enabled: Boolean = true,
) {

    fun perform(type: HapticType = HapticType.Click) {
        if (!enabled) return

        val feedbackConstant = when (type) {
            HapticType.Click ->
                HapticFeedbackConstants.CONTEXT_CLICK

            HapticType.ToggleOn ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_ON
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }

            HapticType.ToggleOff ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_OFF
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }

            HapticType.Confirm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }

            HapticType.Reject ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }

            HapticType.LongPress ->
                HapticFeedbackConstants.LONG_PRESS

            HapticType.Keyboard ->
                HapticFeedbackConstants.KEYBOARD_TAP

            HapticType.SegmentTick ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.SEGMENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }

            HapticType.SegmentFrequentTick ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }

            HapticType.DragStart ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.DRAG_START
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }

            HapticType.GestureEnd ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.GESTURE_END
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
        }

        view.performHapticFeedback(
            feedbackConstant,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
        )
    }

    fun click() = perform(HapticType.Click)

    fun toggleOn() = perform(HapticType.ToggleOn)

    fun toggleOff() = perform(HapticType.ToggleOff)

    fun toggle(enabled: Boolean) {
        perform(
            if (enabled) {
                HapticType.ToggleOn
            } else {
                HapticType.ToggleOff
            }
        )
    }

    fun confirm() = perform(HapticType.Confirm)

    fun reject() = perform(HapticType.Reject)

    fun longPress() = perform(HapticType.LongPress)

    fun keyboard() = perform(HapticType.Keyboard)

    fun segmentTick() = perform(HapticType.SegmentTick)

    fun segmentFrequentTick() = perform(HapticType.SegmentFrequentTick)

    fun dragStart() = perform(HapticType.DragStart)

    fun gestureEnd() = perform(HapticType.GestureEnd)
}

@Composable
fun rememberHaptic(
    enabled: Boolean = true,
): HapticManager {
    val view = LocalView.current

    return remember(view, enabled) {
        HapticManager(
            view = view,
            enabled = enabled,
        )
    }
}
