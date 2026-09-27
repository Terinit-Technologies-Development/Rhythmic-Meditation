package com.terinit.rhythmicmeditation.ui.screens.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.components.PrimaryPillButton
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted

/**
 * Sessions tab shell.
 *
 * PLACEHOLDER CONTENT (Pass 1): shows a current-session summary when a session
 * is live. A full session history list is added in a later pass.
 */
@Composable
fun SessionsScreen(
    onOpenActiveSession: () -> Unit,
    viewModel: ActiveSessionViewModel = viewModel(factory = ActiveSessionViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                text = if (state.session != null) "Current session" else "No session in progress",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.session != null) {
                    "${state.sessionLabel.lowercase().replaceFirstChar { it.uppercase() }} · " +
                        "${state.progressPercent}% complete"
                } else {
                    "Start a meditation from Today, or begin a restorative session."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
            Spacer(Modifier.height(18.dp))
            PrimaryPillButton(
                text = if (state.session != null) "Open session" else "Start a session",
                onClick = onOpenActiveSession
            )
        }

        Spacer(Modifier.height(16.dp))

        CalmCard(containerColor = MistBlueSurface) {
            IconBadge(
                icon = Icons.Outlined.Spa,
                contentDescription = null,
                containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Session history",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A local history of completed morning, restorative, and " +
                    "evening sessions will appear here (later pass).",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
