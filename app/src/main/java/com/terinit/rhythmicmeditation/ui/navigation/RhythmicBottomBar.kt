package com.terinit.rhythmicmeditation.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreen
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import com.terinit.rhythmicmeditation.ui.theme.WarmCreamSurface

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(AppRoute.Today.route, "Today", Icons.Outlined.Home),
    BottomTab(AppRoute.Sessions.route, "Sessions", Icons.Outlined.PlayCircleOutline),
    BottomTab(AppRoute.Insights.route, "Insights", Icons.Outlined.Insights),
    BottomTab(AppRoute.Settings.route, "Settings", Icons.Outlined.Settings)
)

/**
 * Bottom navigation shell: Today / Sessions / Insights / Settings.
 */
@Composable
fun RhythmicBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = WarmCreamSurface,
        tonalElevation = 0.dp
    ) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = tab.label)
                },
                label = {
                    Text(text = tab.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MeditationGreen,
                    selectedTextColor = MeditationGreen,
                    unselectedIconColor = SlateTextMuted,
                    unselectedTextColor = SlateTextMuted,
                    indicatorColor = MeditationGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}
