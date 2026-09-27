package com.terinit.rhythmicmeditation.ui.screens.insights

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicmeditation.ui.components.CalmCard
import com.terinit.rhythmicmeditation.ui.components.IconBadge
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreen
import com.terinit.rhythmicmeditation.ui.theme.MeditationGreenSoft
import com.terinit.rhythmicmeditation.ui.theme.MistBlue
import com.terinit.rhythmicmeditation.ui.theme.SlateTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Insights screen shell.
 *
 * PLACEHOLDER CONTENT (Pass 1): all metrics are illustrative demo values.
 * Real insight computation is a later pass; this screen defines the layout
 * structure that computation will populate.
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Insights",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
                .format(Date()).uppercase(Locale.US),
            style = MaterialTheme.typography.labelMedium,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = "Your attention is deepening and\nyour balance is taking root.",
            style = MaterialTheme.typography.bodyLarge,
            color = SlateTextMuted,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = MeditationGreenSoft.copy(alpha = 0.5f),
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.Spa,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        diameter = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Today's\nMeditation",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SlateTextMuted
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.todayMeditationMinutes}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " min",
                        style = MaterialTheme.typography.titleLarge,
                        color = SlateTextMuted
                    )
                }
                Text(
                    text = "A calmer, clearer you today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
            }

            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface,
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.BarChart,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                        diameter = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Weekly\nSessions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SlateTextMuted
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.weeklySessionsCompleted}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " of ${state.weeklySessionsTarget}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SlateTextMuted
                    )
                }
                Text(
                    text = "You're building a steady rhythm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Weekly minutes bar chart placeholder
        CalmCard(containerColor = Color.White.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Weekly Meditation Minutes",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(18.dp))
            WeeklyBars(minutes = state.weeklyMinutes)
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
                        icon = Icons.Outlined.Spa,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        diameter = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Restorative Gates",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SlateTextMuted
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.restorativeGatesCompleted}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " of ${state.restorativeGatesTarget}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SlateTextMuted
                    )
                }
                Text(
                    text = "Elements completed this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextMuted
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(state.restorativeGatesTarget) { index ->
                        val done = index < state.restorativeGatesCompleted
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    if (done) MeditationGreen.copy(alpha = 0.35f)
                                    else Color.White.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = if (done) MeditationGreen else SlateTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            CalmCard(
                modifier = Modifier.weight(1f),
                containerColor = com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface,
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Outlined.BarChart,
                        contentDescription = null,
                        containerColor = Color.White.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                        diameter = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Reading vs. Meditation",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SlateTextMuted
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        meditationShare = state.meditationShare,
                        modifier = Modifier.size(110.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        LegendRow(
                            color = MeditationGreen,
                            label = "Meditation",
                            value = "${state.meditationMinutes} min",
                            share = "${(state.meditationShare * 100).toInt()}%"
                        )
                        Spacer(Modifier.height(10.dp))
                        LegendRow(
                            color = MistBlue,
                            label = "Reading",
                            value = "${state.readingMinutes} min",
                            share = "${(100 - (state.meditationShare * 100).toInt())}%"
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // A closer look
        CalmCard(containerColor = MeditationGreenSoft.copy(alpha = 0.4f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.WbSunny,
                    contentDescription = null,
                    containerColor = Color.White.copy(alpha = 0.6f),
                    diameter = 42.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "A Closer Look",
                    style = MaterialTheme.typography.titleMedium,
                    color = SlateTextMuted
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Your attention is steadier when the day begins in stillness.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You're more present, make clearer choices, and feel calmer " +
                    "throughout the day.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextMuted
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WeeklyBars(minutes: List<Int>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val max = (minutes.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        minutes.forEachIndexed { index, value ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                val isToday = index == 1 // placeholder: Tue highlighted like the mockup
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height((20 + 90f * value / max).dp)
                        .background(
                            if (value == 0) Color.Transparent
                            else if (isToday) MeditationGreen
                            else MeditationGreen.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = days.getOrElse(index) { "" },
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextMuted
                )
            }
        }
    }
}

@Composable
private fun DonutChart(meditationShare: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Butt)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            // Reading segment (mist blue)
            drawArc(
                color = MistBlue,
                startAngle = -90f,
                sweepAngle = 360f * (1f - meditationShare),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke
            )
            // Meditation segment (sage)
            drawArc(
                color = MeditationGreen,
                startAngle = -90f + 360f * (1f - meditationShare),
                sweepAngle = 360f * meditationShare,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "2h 40m",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "total",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextMuted
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String, share: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("$value · $share", style = MaterialTheme.typography.bodySmall, color = SlateTextMuted)
        }
    }
}
