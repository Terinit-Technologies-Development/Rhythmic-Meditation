package com.terinit.rhythmicmeditation.ui.screens.today

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import com.terinit.rhythmicmeditation.ui.components.AppBrand
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.components.InfoBanner
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SoftProgressBar
import com.terinit.rhythmicmeditation.ui.theme.EssentialAccessAmber
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreen
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Today / Home screen.
 *
 * The morning meditation card is REAL local state (Pass 2): REQUIRED until
 * today's morning session runs, in-progress / paused while it does, COMPLETE
 * afterwards. There is deliberately no Skip action.
 *
 * Cooldown and restorative policy belongs to Rhythmic Routine. This screen
 * links there instead of showing a second, locally invented choice flow.
 */
@Composable
fun TodayScreen(
    onStartSession: () -> Unit,
    onOpenRoutine: () -> Unit,
    onOpenEvening: () -> Unit,
    onOpenInsights: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Brand row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBrand(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile (placeholder)",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Greeting
        Text(
            text = "Good morning",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = todayLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = "A calmer day\nbegins with a quieter you.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 14.dp)
        )
        Spacer(Modifier.height(24.dp))

        // Morning meditation — real state, no Skip
        MorningCard(
            status = state.morningStatus,
            onBegin = { viewModel.onBeginMorningSession(onReady = onStartSession) },
            onContinue = onStartSession,
            onOpenInsights = onOpenInsights
        )

        Spacer(Modifier.height(16.dp))

        // Today's attention + restorative balance
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = MeditationGreenSoft.copy(alpha = 0.55f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.TrackChanges,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        diameter = 44.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Today's Attention",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cultivate presence in simple moments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(12.dp))
                listOf("Be present", "Notice what's enough", "Move with intention").forEach {
                    Text(
                        text = "🍃  $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }

            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = MistBlueSurface
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.BarChart,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                        diameter = 44.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Today's Routine",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cooldowns and restorative choices are managed in " +
                        "Rhythmic Routine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(16.dp))
                PrimaryPillButton(
                    text = "Open Rhythmic Routine",
                    onClick = onOpenRoutine
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Essential access
        InfoBanner(
            title = "Essential access",
            body = "Calls, messages, and banking are available after your morning meditation.",
            icon = Icons.Outlined.Lock,
            containerColor = EssentialAccessAmber,
            onClick = onOpenInsights
        )

        Spacer(Modifier.height(16.dp))

        // Entry point to the evening wind-down
        CalmCard(containerColor = MistBlueSurface) {
            Text(
                text = "Evening Wind-Down",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "An optional 30-minute practice to close the day. " +
                    "You can snooze it or defer it — nothing is lost.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton(text = "Open evening wind-down", onClick = onOpenEvening)
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The morning meditation card. No Skip action exists anywhere in this flow.
 */
@Composable
private fun MorningCard(
    status: MorningStatus,
    onBegin: () -> Unit,
    onContinue: () -> Unit,
    onOpenInsights: () -> Unit
) {
    val icon: ImageVector = when (status) {
        MorningStatus.REQUIRED -> Icons.Outlined.Spa
        MorningStatus.IN_PROGRESS -> Icons.Outlined.PlayArrow
        MorningStatus.PAUSED -> Icons.Outlined.PauseCircle
        MorningStatus.COMPLETE -> Icons.Outlined.CheckCircle
    }
    CalmCard(containerColor = MaterialTheme.colorScheme.surface) {
        IconBadge(
            icon = icon,
            contentDescription = null,
            containerColor = MeditationGreenSoft.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (status) {
                MorningStatus.REQUIRED -> "Morning Meditation Required"
                MorningStatus.IN_PROGRESS -> "Morning Meditation in progress"
                MorningStatus.PAUSED -> "Morning Meditation paused"
                MorningStatus.COMPLETE -> "Morning Meditation Complete"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (status) {
                MorningStatus.REQUIRED ->
                    "Your morning buffer has ended.\nBegin today in stillness to unlock your day."
                MorningStatus.IN_PROGRESS ->
                    "Your session is still running.\nCome back to it whenever you are ready."
                MorningStatus.PAUSED ->
                    "Your confirmed progress is safe.\nResume when you are ready to continue."
                MorningStatus.COMPLETE ->
                    "You've finished today's meditation.\nYour presence creates positive momentum."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted
        )
        Spacer(Modifier.height(20.dp))
        when (status) {
            MorningStatus.REQUIRED -> PrimaryPillButton(
                text = "Begin Session",
                onClick = onBegin
            )
            MorningStatus.IN_PROGRESS, MorningStatus.PAUSED -> PrimaryPillButton(
                text = if (status == MorningStatus.PAUSED) "Resume Session" else "Continue Session",
                onClick = onContinue
            )
            MorningStatus.COMPLETE -> PrimaryPillButton(
                text = "View insights",
                onClick = onOpenInsights
            )
        }
    }
}

private fun todayLabel(): String {
    val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    return formatter.format(Date()).uppercase(Locale.US)
}
