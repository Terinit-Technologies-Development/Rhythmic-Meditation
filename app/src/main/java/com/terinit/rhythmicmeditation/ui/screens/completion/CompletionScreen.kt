package com.terinit.rhythmicmeditation.ui.screens.completion

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.ui.components.AppBrand
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SecondaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SoftProgressBar
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreen
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted

/**
 * Completion screen shell.
 *
 * PLACEHOLDER CONTENT (Pass 1): cooldown remaining time is illustrative —
 * Rhythmic Routine owns cooldowns and Meditation never shortens them.
 */
@Composable
fun CompletionScreen(
    onReturnToToday: () -> Unit,
    onViewInsights: () -> Unit,
    viewModel: CompletionViewModel = viewModel(factory = CompletionViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AppBrand(modifier = Modifier.padding(top = 12.dp, bottom = 28.dp))

        // Completion emblem
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(MeditationGreenSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MeditationGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Session complete",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Your meditation requirement is complete.\n" +
                "Rhythmic Routine will continue to manage\nthe remaining cooldown for you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = SlateTextMuted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        // Requirement card
        CalmCard(containerColor = MeditationGreenSoft.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Outlined.Spa, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Meditation requirement",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Complete",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MeditationGreen
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MeditationGreen
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "You've finished today's meditation.\nYour presence creates positive momentum.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(16.dp))

        // Cooldown card
        CalmCard(containerColor = MistBlueSurface) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.BarChart,
                    contentDescription = null,
                    containerColor = Color.White.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Cooldown",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.cooldownMinutesLeft} min left",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Meditation does not shorten the cooldown.\n" +
                    "Rhythmic Routine will continue to manage your remaining time.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftProgressBar(
                    progress = 1f - (state.cooldownMinutesLeft.toFloat() /
                        state.cooldownTotalMinutes.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${state.cooldownMinutesLeft} min remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextMuted
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        PrimaryPillButton(
            text = "Return to Today",
            onClick = onReturnToToday,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        )
        Spacer(Modifier.height(12.dp))
        SecondaryPillButton(
            text = "View insights",
            onClick = onViewInsights,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}
