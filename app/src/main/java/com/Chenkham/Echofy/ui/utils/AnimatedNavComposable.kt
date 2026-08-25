package com.Chenkham.Echofy.ui.utils

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Wraps screen content and blocks all touch events when the screen is
 * in its exit animation to prevent "ghost taps" passing through to
 * the screen underneath.
 */
@Composable
fun AnimatedVisibilityScope.TouchBlockingWrapper(
    content: @Composable () -> Unit
) {
    // Only block touches while this screen is actually animating out. The previous version
    // read transition.currentState outside the derivedStateOf and compared targetState, so
    // the blocker could stay mounted after the animation settled and swallow the first tap
    // on the new screen - the "have to tap twice" symptom.
    val isExiting by remember(transition) {
        derivedStateOf {
            transition.currentState != transition.targetState &&
                transition.targetState == EnterExitState.PostExit
        }
    }

    Box {
        content()
        if (isExiting) {
            // Invisible overlay that consumes all pointer events to prevent
            // taps from passing through to the screen behind this one.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                // Consume all changes so nothing propagates
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}
