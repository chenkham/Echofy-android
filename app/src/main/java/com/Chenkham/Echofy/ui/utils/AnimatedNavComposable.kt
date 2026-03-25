package com.Chenkham.Echofy.ui.utils

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val isExiting by transition.currentState.let {
        // We consider the screen "exiting" when the transition target is PostExit
        // OR when the transition is running and heading toward PostExit.
        androidx.compose.runtime.derivedStateOf {
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
