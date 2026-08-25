package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Shared AI screen color palette.
 *
 * These were fixed dark-theme literals, so the AI tab stayed dark while the rest of
 * the app followed light / dark / pure-black. Reading from MaterialTheme.colorScheme
 * instead means the whole tab recomposes with the app theme automatically — pure black
 * already arrives here because ColorScheme.pureBlack() overrides `surface`.
 */

// Surface hierarchy
internal val SurfaceDark: Color
    @Composable get() = MaterialTheme.colorScheme.surface

internal val SurfaceContainer: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer

internal val SurfaceContainerHigh: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

internal val SurfaceContainerHighest: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

// On-surface text/icon colors
internal val OnSurface: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

internal val OnSurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// Outlines
internal val Outline: Color
    @Composable get() = MaterialTheme.colorScheme.outline

internal val OutlineVariant: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

// Accent colors
internal val Primary: Color
    @Composable get() = MaterialTheme.colorScheme.primary

internal val OnPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

internal val PrimaryContainer: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer

internal val Secondary: Color
    @Composable get() = MaterialTheme.colorScheme.secondary

internal val SecondaryContainer: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer

internal val Tertiary: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary

// Chat bubble specific. The user bubble keeps a fixed saturated gradient in every
// theme so its white text stays legible; only the AI bubble tracks the surface stack.
internal val UserBubbleGradientStart = Color(0xFF2563EB) // blue-600
internal val UserBubbleGradientEnd = Color(0xFF7C3AED)   // violet-600

internal val AiBubbleBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
