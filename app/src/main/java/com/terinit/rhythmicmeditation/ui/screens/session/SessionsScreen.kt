package com.terinit.rhythmicmeditation.ui.screens.session

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Current-session summary and local, read-only meditation history.
 */
@Composable
fun SessionsScreen(
    onOpenActiveSession: () -> Unit,
    viewModel: ActiveSessionViewModel = viewModel(factory = ActiveSessionViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionsViewModel: SessionsViewModel = viewModel(factory = SessionsViewModel.Factory)
    val sessionsState by sessionsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasLiveSession = state.status == MeditationSessionStatus.PENDING ||
        state.status == MeditationSessionStatus.ACTIVE ||
        state.status == MeditationSessionStatus.PAUSED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Sessions",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Your practice, kept quietly on this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        CalmCard(containerColor = MaterialTheme.colorScheme.surface) {
            IconBadge(icon = Icons.Outlined.PlayCircleOutline, contentDescription = null)
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (hasLiveSession) "Current session" else "No session in progress",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hasLiveSession) {
                    "${state.sessionLabel.lowercase().replaceFirstChar { it.uppercase() }} · " +
                        "${state.progressPercent}% complete"
                } else {
                    "Begin a free 30-minute practice here, or open Routine for " +
                        "a policy-bound restorative session."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
            Spacer(Modifier.height(18.dp))
            PrimaryPillButton(
                text = if (hasLiveSession) "Open session" else "Start a session",
                onClick = {
                    if (hasLiveSession) {
                        onOpenActiveSession()
                    } else {
                        sessionsViewModel.startStandaloneSession(
                            onStarted = onOpenActiveSession,
                            onFailure = {
                                Toast.makeText(
                                    context,
                                    "The session could not be started.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconBadge(
                icon = Icons.Outlined.History,
                contentDescription = null,
                containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Session history",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        if (sessionsState.sessions.isEmpty()) {
            CalmCard(containerColor = MistBlueSurface) {
                Text(
                    text = "No finished sessions yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Completed and ended practices are recorded here on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
            }
        } else {
            sessionsState.sessions.forEach { session ->
                SessionHistoryCard(session)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SessionHistoryCard(session: MeditationSession) {
    val timestamp = session.completedAtEpochMs
        ?: session.startedAtEpochMs
        ?: session.createdAtEpochMs
    val locale = LocalLocale.current.platformLocale
    val timeLabel = remember(timestamp, locale) {
        SimpleDateFormat("MMM d, yyyy · h:mm a", locale).format(Date(timestamp))
    }
    val practicedMinutes = session.completedQualifiedSeconds.coerceAtLeast(0) / 60

    CalmCard(containerColor = MistBlueSurface) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Outlined.Spa,
                contentDescription = null,
                containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = session.kind.historyLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextMuted
                )
            }
            Text(
                text = session.status.historyLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = SlateTextMuted
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (session.status == MeditationSessionStatus.INVALID) {
                "Session timing evidence is unavailable."
            } else {
                "$practicedMinutes min of qualified practice recorded"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SlateTextMuted
        )
        if (session.pauseCount > 0 || session.interruptionCount > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildList {
                    if (session.pauseCount > 0) add("${session.pauseCount} pauses")
                    if (session.interruptionCount > 0) {
                        add("${session.interruptionCount} interruptions")
                    }
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextMuted
            )
        }
    }
}

private fun MeditationSessionKind.historyLabel(): String = when (this) {
    MeditationSessionKind.MORNING_REQUIRED -> "Morning Meditation"
    MeditationSessionKind.COOLDOWN_RESTORATIVE -> "Restorative Meditation"
    MeditationSessionKind.EVENING_WIND_DOWN -> "Evening Wind-Down"
    MeditationSessionKind.STANDALONE -> "Open Practice"
}

private fun MeditationSessionStatus.historyLabel(): String = when (this) {
    MeditationSessionStatus.COMPLETED -> "Completed"
    MeditationSessionStatus.CANCELLED -> "Ended early"
    MeditationSessionStatus.EXPIRED -> "Expired"
    MeditationSessionStatus.INVALID -> "Unavailable"
    MeditationSessionStatus.PENDING,
    MeditationSessionStatus.ACTIVE,
    MeditationSessionStatus.PAUSED -> "In progress"
}
