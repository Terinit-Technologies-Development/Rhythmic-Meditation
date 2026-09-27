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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.components.InfoBanner
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.components.SecondaryPillButton
import com.terinit.rhythmicmeditation.ui.theme.EssentialAccessAmber
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenDark
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import com.terinit.rhythmicmeditation.util.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evening Wind-Down.
 *
 * Renders the REAL evening state (due / snoozed / in progress / complete /
 * deferred / not due yet) from [EveningViewModel]. The practice is optional:
 * deferring is a decision, never a failure, and the wording stays non-punitive
 * everywhere. The Bedtime Reflection is display-only — prompts, no text
 * fields, no persistence, no sharing.
 */
@Composable
fun EveningScreen(
    onStartSession: () -> Unit,
    viewModel: EveningViewModel = viewModel(factory = EveningViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            text = "End the day with stillness.",
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

            when (state.state) {
                EveningMeditationState.NOT_DUE -> NotDueContent()
                EveningMeditationState.DUE -> DueContent(
                    onStartNow = { viewModel.onStartNow(onStartSession) },
                    onSnooze15 = viewModel::onSnooze15,
                    onDeferTonight = viewModel::onDeferTonight
                )
                EveningMeditationState.SNOOZED -> SnoozedContent(
                    snoozedUntilEpochMs = state.snoozedUntilEpochMs,
                    onResume = viewModel::onResumeFromSnooze
                )
                EveningMeditationState.IN_PROGRESS -> InProgressContent(
                    qualifiedSeconds = state.eveningQualifiedSeconds,
                    requiredSeconds = state.eveningRequiredSeconds,
                    onContinue = onStartSession
                )
                EveningMeditationState.COMPLETED -> CompletedContent(
                    recordedSeconds = state.eveningQualifiedSeconds
                )
                EveningMeditationState.DEFERRED -> DeferredContent()
            }
        }

        if (state.state == EveningMeditationState.NOT_DUE ||
            state.state == EveningMeditationState.DUE ||
            state.state == EveningMeditationState.SNOOZED
        ) {
            Spacer(Modifier.height(16.dp))

            InfoBanner(
                title = "Calls and essential activity won't be interrupted before you begin.",
                body = "You can defer this practice when needed.",
                icon = Icons.Outlined.Info,
                containerColor = EssentialAccessAmber
            )
        }

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
                listOf(
                    if (state.todaySessionCount == 1) "1 meditation session"
                    else "${state.todaySessionCount} meditation sessions",
                    "${state.todayMeditationMinutes} minutes recorded",
                    "A calmer, clearer mind"
                ).forEach {
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
                    Icons.Outlined.Book to "What felt meaningful today?",
                    Icons.Outlined.Favorite to "What can I release?",
                    Icons.Outlined.AutoAwesome to "What would you like to carry into tomorrow?"
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Just for your thoughts — nothing is saved or shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextMuted
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Not due yet — the trigger is Routine-owned; nothing is fabricated here. */
@Composable
private fun NotDueContent() {
    Text(
        text = "Not due yet",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Your evening wind-down is not due yet. It will appear here " +
            "when the day begins to wind down.",
        style = MaterialTheme.typography.bodyMedium,
        color = SlateTextMuted
    )
}

/** The offer: exactly three actions — start, snooze, or defer. */
@Composable
private fun DueContent(
    onStartNow: () -> Unit,
    onSnooze15: () -> Unit,
    onDeferTonight: () -> Unit
) {
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
            onClick = onSnooze15,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = MeditationGreenDark
                )
            }
        )
        SecondaryPillButton(
            text = "Defer tonight",
            onClick = onDeferTonight,
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

/** Snoozed — quiet status with one calm way back in. */
@Composable
private fun SnoozedContent(
    snoozedUntilEpochMs: Long?,
    onResume: () -> Unit
) {
    Text(
        text = snoozedUntilEpochMs?.let { "Snoozed · back at ${TimeFormat.timeOfDay(it)}" }
            ?: "Snoozed",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "The practice will be offered again at that time. " +
            "You can pick it back up whenever you like.",
        style = MaterialTheme.typography.bodyMedium,
        color = SlateTextMuted
    )
    Spacer(Modifier.height(16.dp))
    PrimaryPillButton(text = "Resume", onClick = onResume)
}

/** Session running (or awaiting Resume) — continue where you left off. */
@Composable
private fun InProgressContent(
    qualifiedSeconds: Int,
    requiredSeconds: Int,
    onContinue: () -> Unit
) {
    Text(
        text = "Your evening meditation is in progress.",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "${TimeFormat.wholeMinutes(qualifiedSeconds)} of " +
            "${TimeFormat.wholeMinutes(requiredSeconds)} minutes recorded so far.",
        style = MaterialTheme.typography.bodyMedium,
        color = SlateTextMuted
    )
    Spacer(Modifier.height(16.dp))
    PrimaryPillButton(
        text = "Continue session",
        onClick = onContinue,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    )
}

/** Complete — a calm summary of what was recorded. No reward language. */
@Composable
private fun CompletedContent(recordedSeconds: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            icon = Icons.Outlined.CheckCircle,
            contentDescription = null,
            containerColor = MeditationGreenSoft,
            diameter = 42.dp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Meditation complete",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = "${TimeFormat.wholeMinutes(recordedSeconds)} minutes recorded tonight. " +
            "Rest well.",
        style = MaterialTheme.typography.bodyMedium,
        color = SlateTextMuted
    )
}

/** Deferred — a decision, never a failure. Tomorrow is unaffected. */
@Composable
private fun DeferredContent() {
    Text(
        text = "Evening practice deferred",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "This practice is here whenever you'd like it — nothing is lost, " +
            "and tomorrow begins fresh.",
        style = MaterialTheme.typography.bodyMedium,
        color = SlateTextMuted
    )
}
