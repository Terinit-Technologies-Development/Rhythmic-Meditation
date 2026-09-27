package com.terinit.rhythmicmeditation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Rhythmic Meditation theme.
 *
 * Light-only for Pass 1: the product is a calm, daytime-to-evening companion
 * and the mockups define a single light art direction. A dark scheme can be
 * added later without touching screen code.
 */
private val RhythmicLightColorScheme = lightColorScheme(
    primary = MeditationGreen,
    onPrimary = WarmCreamSurface,
    primaryContainer = MeditationGreenSoft,
    onPrimaryContainer = MeditationGreenDark,
    secondary = MistBlue,
    onSecondary = WarmCreamSurface,
    secondaryContainer = MistBlueSurface,
    onSecondaryContainer = MistBlueDeep,
    tertiary = WarmCreamDeep,
    onTertiary = SlateText,
    background = SkyHazeBottom,
    onBackground = SlateText,
    surface = WarmCreamSurface,
    onSurface = SlateText,
    surfaceVariant = WarmCream,
    onSurfaceVariant = SlateTextMuted,
    surfaceContainer = WarmCreamSurface,
    surfaceContainerHigh = WarmCream,
    surfaceContainerHighest = WarmCream,
    outline = SoftDivider,
    outlineVariant = SoftDivider,
    error = GentleAlert
)

@Composable
fun RhythmicMeditationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RhythmicLightColorScheme,
        typography = RhythmicTypography,
        shapes = RhythmicShapes,
        content = content
    )
}
