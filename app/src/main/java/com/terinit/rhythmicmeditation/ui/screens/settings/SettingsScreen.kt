package com.terinit.rhythmicmeditation.ui.screens.settings

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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreen
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import com.terinit.rhythmicmeditation.ui.theme.SoftDivider

/**
 * Settings shell.
 *
 * Real local toggles (sound / haptics / integration switch) are persisted via
 * DataStore. The protocol status block is a placeholder until Routine pairing
 * is completed in a later pass. Local data controls are placeholders — the
 * destructive actions land with the data-management pass.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Quiet controls for a quiet practice.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(22.dp))

        // Preferences
        CalmCard(containerColor = Color.White.copy(alpha = 0.55f)) {
            Text(
                text = "Practice",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            ToggleRow(
                icon = Icons.Outlined.MusicNote,
                title = "Session sound",
                subtitle = "A soft tone marks the start and end of a session.",
                checked = state.sessionSoundEnabled,
                onCheckedChange = viewModel::setSessionSoundEnabled
            )
            HorizontalDivider(color = SoftDivider)
            ToggleRow(
                icon = Icons.Outlined.NotificationsActive,
                title = "Subtle haptics",
                subtitle = "Gentle vibration cues during a session.",
                checked = state.subtleHapticsEnabled,
                onCheckedChange = viewModel::setSubtleHapticsEnabled
            )
        }

        Spacer(Modifier.height(16.dp))

        // Integration / protocol status placeholder
        CalmCard(containerColor = Color.White.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.Handshake,
                    contentDescription = null,
                    containerColor = Color.White.copy(alpha = 0.6f),
                    diameter = 42.dp
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Routine integration",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Protocol status (placeholder)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Protocol version: ${state.supportedProtocolVersion} · " +
                    "Provider: ${MeditationProtocol.STATUS_PROVIDER_AUTHORITY}",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextMuted
            )
            Text(
                text = "Paired package: ${state.knownPairedPackageName ?: "not paired yet"}",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextMuted
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                icon = Icons.Outlined.Handshake,
                title = "Paired integration",
                subtitle = "Allow Rhythmic Routine to exchange session status.",
                checked = state.pairedIntegrationEnabled,
                onCheckedChange = viewModel::setPairedIntegrationEnabled
            )
        }

        Spacer(Modifier.height(16.dp))

        // Local data controls
        CalmCard(containerColor = Color.White.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    containerColor = Color.White.copy(alpha = 0.6f),
                    diameter = 42.dp
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Local data",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Meditation evidence stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Clearing local session history and export controls arrive " +
                    "with the data-management pass (later).",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(16.dp))

        // About
        CalmCard(containerColor = Color.White.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.Info,
                    contentDescription = null,
                    containerColor = Color.White.copy(alpha = 0.6f),
                    diameter = 42.dp
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "About Rhythmic Meditation",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 0.1.0 · Pass 1 foundation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextMuted
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Rhythmic Meditation is a companion to Rhythmic Routine. " +
                    "Meditation owns meditation evidence only; Rhythmic Routine remains " +
                    "the policy authority for cooldowns and requirements. Everything here " +
                    "works offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextMuted
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MeditationGreen,
                checkedThumbColor = Color.White
            )
        )
    }
}
