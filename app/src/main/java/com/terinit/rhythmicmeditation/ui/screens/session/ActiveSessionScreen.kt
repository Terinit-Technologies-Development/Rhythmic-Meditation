package com.terinit.rhythmicmeditation.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.ui.components.InfoBanner
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SecondaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SoftProgressBar
import com.terinit.rhythmicmeditation.ui.theme.EssentialAccessAmber
import com.terinit.rhythmicmeditation.ui.theme.GentleAlert
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreen
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlue
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import com.terinit.rhythmicmeditation.util.TimeFormat

/**
 * Active Session screen shell.
 *
 * PLACEHOLDER CONTENT (Pass 1): the breathing orb, phase indicator, and timer
 * use demo values when no session is running. Real qualified-time tracking and
 * pause/resume semantics are completed in Pass 2.
 */
@Composable
fun ActiveSessionScreen(
    onClose: () -> Unit,
    onSessionEnded: () -> Unit,
    viewModel: ActiveSessionViewModel = viewModel(factory = ActiveSessionViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rhythmic Meditation",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close session",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Breathing orb (placeholder visual)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BreathingOrb()
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = state.sessionLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MeditationGreen,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = TimeFormat.mmSs(state.elapsedSeconds),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "  / ${TimeFormat.mmSs(state.requiredSeconds)}",
                style = MaterialTheme.typography.headlineMedium,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(14.dp))
        BreathPhaseRow()
        Spacer(Modifier.height(18.dp))

        Text(
            text = "Be here now.\nLet your breath guide you back to stillness.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftProgressBar(
                progress = state.progress,
                modifier = Modifier.weight(1f),
                height = 12.dp
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${state.progressPercent}% complete",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PrimaryPillButton(
                text = "Pause",
                onClick = viewModel::onPause,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    IconCircleBadge(
                        icon = Icons.Outlined.Pause,
                        background = Color.White.copy(alpha = 0.25f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
            SecondaryPillButton(
                text = "End Session",
                onClick = { viewModel.onEndSession(onSessionEnded) },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    IconCircleBadge(
                        icon = Icons.Outlined.Stop,
                        background = GentleAlert.copy(alpha = 0.12f),
                        contentColor = GentleAlert
                    )
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        InfoBanner(
            title = "Essential access remains available",
            body = "Calls, messages, and banking are available during your session. " +
                "The rest of your phone stays locked to help you stay present.",
            icon = Icons.Outlined.Lock,
            containerColor = EssentialAccessAmber
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IconCircleBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
    }
}

/**
 * Calm breathing orb — concentric soft rings with a sage/mist gradient core.
 * Placeholder animation-free visual for Pass 1.
 */
@Composable
private fun BreathingOrb() {
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MeditationGreenSoft.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(210.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(
                    Brush.linearGradient(
                        listOf(MistBlue.copy(alpha = 0.55f), MeditationGreen.copy(alpha = 0.35f))
                    ),
                    CircleShape
                )
        )
    }
}

/** "INHALE · HOLD · EXHALE" phase indicator (placeholder state). */
@Composable
private fun BreathPhaseRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhaseDot(active = false)
        Spacer(Modifier.width(8.dp))
        Text("INHALE", style = MaterialTheme.typography.labelMedium, color = SlateTextMuted)
        Spacer(Modifier.width(8.dp))
        PhaseDot(active = true)
        Spacer(Modifier.width(8.dp))
        Text("HOLD", style = MaterialTheme.typography.labelMedium, color = SlateTextMuted)
        Spacer(Modifier.width(8.dp))
        PhaseDot(active = false)
        Spacer(Modifier.width(8.dp))
        Text("EXHALE", style = MaterialTheme.typography.labelMedium, color = SlateTextMuted)
        Spacer(Modifier.width(8.dp))
        PhaseDot(active = false)
    }
}

@Composable
private fun PhaseDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(if (active) 10.dp else 8.dp)
            .background(
                if (active) MeditationGreen else MistBlue.copy(alpha = 0.5f),
                CircleShape
            )
    )
}
