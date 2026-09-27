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
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.domain.model.MeditationMode
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.runtime.RecoveryNotice
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
 * Active Session screen.
 *
 * The timer is a render-only projection of the monotonic ledger (see
 * MeditationRuntimeController). Pause / Resume / End are the only writes.
 *
 * UX follows the mockup direction: breathing orb, "12:48 / 30:00" timer,
 * calm quote, Pause / End Session, and the essential access reminder.
 * Optional MeditationMode chips only reframe the practice — qualification is
 * identical for every mode.
 */
@Composable
fun ActiveSessionScreen(
    onClose: () -> Unit,
    onSessionCompleted: () -> Unit,
    onSessionCancelled: () -> Unit,
    viewModel: ActiveSessionViewModel = viewModel(factory = ActiveSessionViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var showEndConfirmation by remember { mutableStateOf(false) }

    // Terminal states navigate exactly once each. Status-driven (not event
    // driven) so a completion that happened while the screen was off — or
    // before this screen was created — still routes correctly.
    LaunchedEffect(state.status) {
        when (state.status) {
            MeditationSessionStatus.COMPLETED -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSessionCompleted()
            }
            MeditationSessionStatus.CANCELLED -> onSessionCancelled()
            else -> Unit
        }
    }

    if (showEndConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndConfirmation = false },
            title = { Text("End this session?") },
            text = {
                Text("Your current meditation requirement will remain incomplete.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirmation = false
                    viewModel.onEndSessionConfirmed()
                }) { Text("End session") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmation = false }) { Text("Keep meditating") }
            }
        )
    }

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

        Spacer(Modifier.height(20.dp))

        // Recovery notice (process restore / reboot) — progress is safe, resume needed.
        val notice = state.recoveryNotice
        if (notice is RecoveryNotice.RestoredToPaused) {
            InfoBanner(
                title = "Session restored",
                body = if (notice.rebootDetected) {
                    "The device restarted. Your confirmed progress is safe — " +
                        "unverified time was not counted. Tap Resume to continue."
                } else {
                    "The app was closed mid-session. Only your confirmed progress " +
                        "was kept. Tap Resume to continue."
                },
                icon = Icons.Outlined.Restore,
                containerColor = EssentialAccessAmber
            )
            Spacer(Modifier.height(16.dp))
        }

        // Breathing orb
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BreathingOrb()
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = state.sessionLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MeditationGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = TimeFormat.mmSs(state.elapsedSeconds.coerceAtMost(state.requiredSeconds)),
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
        BreathPhaseRow(mode = state.mode, elapsedSeconds = state.elapsedSeconds)
        Spacer(Modifier.height(18.dp))

        Text(
            text = "Be here now.\nLet your breath guide you back to stillness.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        ModeSelector(selected = state.mode, onSelect = viewModel::onSelectMode)

        Spacer(Modifier.height(18.dp))

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
            if (state.isPaused) {
                PrimaryPillButton(
                    text = "Resume",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onPauseResume()
                    },
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        IconCircleBadge(
                            icon = Icons.Outlined.PlayArrow,
                            background = Color.White.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            } else {
                PrimaryPillButton(
                    text = "Pause",
                    onClick = viewModel::onPauseResume,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        IconCircleBadge(
                            icon = Icons.Outlined.Pause,
                            background = Color.White.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            }
            SecondaryPillButton(
                text = "End Session",
                onClick = {
                    if (state.requirementMet) {
                        viewModel.onEndSessionConfirmed()
                    } else {
                        showEndConfirmation = true
                    }
                },
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
private fun ModeSelector(
    selected: MeditationMode,
    onSelect: (MeditationMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MeditationMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MeditationGreenSoft,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
        }
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
 * Purely decorative framing; it never drives qualification.
 */
@Composable
private fun BreathingOrb() {
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
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
                .size(195.dp)
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
                .size(140.dp)
                .background(
                    Brush.linearGradient(
                        listOf(MistBlue.copy(alpha = 0.55f), MeditationGreen.copy(alpha = 0.35f))
                    ),
                    CircleShape
                )
        )
    }
}

/**
 * Phase indicator. In BREATH mode it cycles INHALE · HOLD · EXHALE with the
 * elapsed timer (no extra clocks); other modes show a calm framing line.
 */
@Composable
private fun BreathPhaseRow(mode: MeditationMode, elapsedSeconds: Int) {
    if (mode != MeditationMode.BREATH) {
        Text(
            text = when (mode) {
                MeditationMode.STILLNESS -> "STILLNESS"
                MeditationMode.BODY -> "SOFTEN · NOTICE · RELEASE"
                MeditationMode.IMAGINATION -> "IMAGINE · REST · RETURN"
                else -> ""
            },
            style = MaterialTheme.typography.labelMedium,
            color = SlateTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    val phase = (elapsedSeconds % 12) / 4 // 4s inhale, 4s hold, 4s exhale
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("INHALE", 0, phase),
            Triple("HOLD", 1, phase),
            Triple("EXHALE", 2, phase)
        ).forEach { (label, index, current) ->
            PhaseDot(active = index == current)
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (index == current) MeditationGreen else SlateTextMuted
            )
            Spacer(Modifier.width(8.dp))
        }
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
