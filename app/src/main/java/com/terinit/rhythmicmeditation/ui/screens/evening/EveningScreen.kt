package com.terinit.rhythmicmeditation.ui.screens.evening

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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
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
import com.terinit.rhythmicmeditation.ui.theme.EssentialAccessAmber
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evening Wind-Down shell.
 *
 * PLACEHOLDER CONTENT (Pass 1): the 30-minute session card, calm summary, and
 * reflection prompts are illustrative. Snooze/defer are local placeholders —
 * the evening flow is completed in a later pass.
 */
@Composable
fun EveningScreen(
    onStartNow: () -> Unit,
    onSnooze: () -> Unit,
    onDefer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Evening\nWind-Down",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
                .format(Date()).uppercase(Locale.US),
            style = MaterialTheme.typography.labelMedium,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "End the day with stillness.\nA calmer tomorrow begins tonight.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 14.dp)
        )

        Spacer(Modifier.height(24.dp))

        CalmCard(containerColor = Color.White.copy(alpha = 0.6f)) {
            IconBadge(
                icon = Icons.Outlined.Spa,
                contentDescription = null,
                containerColor = MeditationGreenSoft
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "30-Minute\nEvening Meditation",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "A gentle practice to help you reflect, release the day, " +
                    "and settle your mind for restful sleep.",
                style = MaterialTheme.typography.bodyLarge,
                color = SlateTextMuted
            )
            Spacer(Modifier.height(20.dp))
            PrimaryPillButton(
                text = "Start now",
                onClick = onStartNow,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryPillButton(
                    text = "Snooze 15 min",
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = MeditationGreenDarkSafe()
                        )
                    }
                )
                SecondaryPillButton(
                    text = "Defer tonight",
                    onClick = onDefer,
                    modifier = Modifier.weight(1f),
                    containerColor = MistBlueSurface,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        InfoBanner(
            title = "Ongoing calls, messages, and banking are not interrupted.",
            body = "You can always defer this practice when needed.",
            icon = Icons.Outlined.Info,
            containerColor = EssentialAccessAmber
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = MeditationGreenSoft.copy(alpha = 0.5f),
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        diameter = 42.dp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Today's Calm Summary",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "See how you showed up for yourself today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(12.dp))
                listOf("1 meditation session", "More presence", "A calmer, clearer mind").forEach {
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
                containerColor = MistBlueSurface,
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.Bedtime,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                        diameter = 42.dp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Bedtime Reflection",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "A few gentle prompts to close your day with gratitude and clarity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(12.dp))
                listOf(
                    Icons.Outlined.Book to "What felt good today?",
                    Icons.Outlined.Favorite to "What can I release?",
                    Icons.Outlined.AutoAwesome to "What am I looking forward to tomorrow?"
                ).forEach { (icon, prompt) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextMuted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MeditationGreenDarkSafe(): Color =
    com.terinit.rhythmicmeditation.ui.theme.MeditationGreenDark
