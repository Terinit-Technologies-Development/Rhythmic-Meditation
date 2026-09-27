package com.terinit.rhythmicmeditation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.terinit.rhythmicmeditation.ui.theme.SkyHazeBottom
import com.terinit.rhythmicmeditation.ui.theme.SkyHazeTop

/**
 * Soft sky-haze backdrop used behind every screen (mockup art direction).
 * Photographic hero art is intentionally absent in Pass 1; gradients keep the
 * app light, offline, and calm.
 */
@Composable
fun CalmScreenBackground(
    modifier: Modifier = Modifier,
    topColor: Color = SkyHazeTop,
    bottomColor: Color = SkyHazeBottom,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
    ) {
        content(PaddingValues(0.dp))
    }
}

/** Convenience padding for screen content below the status bar. */
fun Modifier.screenPadding(horizontal: androidx.compose.ui.unit.Dp = 20.dp): Modifier =
    padding(start = horizontal, end = horizontal, top = 12.dp, bottom = 24.dp)
