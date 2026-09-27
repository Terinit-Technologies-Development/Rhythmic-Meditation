package com.terinit.rhythmicmeditation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import com.terinit.rhythmicmeditation.ui.theme.EssentialAccessAmber
import com.terinit.rhythmicmeditation.ui.theme.MistBlueSurface
import com.terinit.rhythmicmeditation.ui.theme.WarmCream

/**
 * Rounded icon-in-a-soft-circle badge used across cards.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = WarmCream,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    diameter: Dp = 52.dp
) {
    Box(
        modifier = modifier
            .size(diameter)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(diameter * 0.5f)
        )
    }
}

/**
 * Soft informational banner (essential access, cooldown reminders).
 */
@Composable
fun InfoBanner(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = WarmCream,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = icon,
            contentDescription = null,
            containerColor = Color.White.copy(alpha = 0.55f),
            diameter = 48.dp
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(0.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The mist-blue variant used for restorative/data cards. */
val MistCardColor = MistBlueSurface
/** The cream variant used for essential-access reminders. */
val EssentialCardColor = EssentialAccessAmber
