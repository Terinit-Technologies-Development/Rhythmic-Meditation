package com.terinit.rhythmicmeditation.ui.screens.restorative

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.components.InfoBanner
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SecondaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SoftProgressBar
import com.terinit.rhythmicmeditation.ui.theme.EssentialAccessAmber
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted

/**
 * Cooldown restorative choice shell: Meditation or Reading.
 *
 * PLACEHOLDER CONTENT (Pass 1): the requirement text and cooldown progress are
 * illustrative. Rhythmic Routine owns cooldown policy and supplies the real
 * requirement in a later pass. Both paths merely record meditation evidence.
 */
@Composable
fun RestorativeChoiceScreen(
    onBeginMeditation: () -> Unit,
    onOpenReader: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Cooldown context banner
        CalmCard(containerColor = Color.White.copy(alpha = 0.6f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.HourglassTop,
                    contentDescription = null,
                    containerColor = MeditationGreenSoft,
                    diameter = 44.dp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Cooldown 4",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily reading baseline already complete.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Choose a\nrestorative path",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You've reached your final cooldown threshold for today. " +
                "Complete either restorative activity below to satisfy this cooldown requirement.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Meditation path
            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = Color.White.copy(alpha = 0.65f),
                contentPadding = 16.dp
            ) {
                IconBadge(
                    icon = Icons.Outlined.Spa,
                    contentDescription = null,
                    containerColor = MeditationGreenSoft,
                    diameter = 46.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Meditation",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "30 min quiet session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Settle the mind, return to presence, and restore your balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(18.dp))
                PrimaryPillButton(text = "Begin", onClick = onBeginMeditation)
            }

            // Reading path (handoff to Rhythmic Reader)
            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = MistBlueSurface,
                contentPadding = 16.dp
            ) {
                IconBadge(
                    icon = Icons.Outlined.Book,
                    contentDescription = null,
                    containerColor = Color.White.copy(alpha = 0.65f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    diameter = 46.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Reading",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "11 pages · 30 min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Explore something meaningful and give your mind a break.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(18.dp))
                SecondaryPillButton(
                    text = "Open Reader",
                    onClick = onOpenReader,
                    containerColor = Color.White.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        InfoBanner(
            title = "Both options satisfy this cooldown",
            body = "Completing either meditation or reading will fulfill the restorative " +
                "requirement for this cooldown. The 90-minute cooldown period still needs " +
                "to elapse before you can resume focused sessions.",
            icon = Icons.Outlined.HourglassTop,
            containerColor = EssentialAccessAmber
        )

        Spacer(Modifier.height(16.dp))

        // Cooldown time remaining
        CalmCard(containerColor = Color.White.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Cooldown time remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                    Text(
                        text = "1 hr 32 min",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    SoftProgressBar(progress = 0.35f)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "of 90 min",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
