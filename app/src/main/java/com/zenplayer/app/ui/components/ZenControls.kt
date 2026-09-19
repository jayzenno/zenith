package com.zenplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenplayer.app.ui.theme.LocalFocusEffect
import com.zenplayer.app.ui.theme.LocalZenColors

@Composable
fun ZenChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 0,
    height: Int = 0
) {
    val colors = LocalZenColors.current
    val focusEffect = LocalFocusEffect.current
    var focused by remember { mutableStateOf(false) }
    val bg: Brush = if (selected) colors.accentBrush else SolidColor(colors.surfaceHigh.copy(alpha = 0.55f))
    var m = modifier
        .clip(ZenShapes.pill)
        .background(bg)
        .zenFocusEffect(focused, focusEffect, ZenShapes.pill)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
        .focusable()
        .onFocusChanged { focused = it.isFocused && it.hasFocus }
    if (width > 0) m = m.width(width.dp)
    if (height > 0) m = m.height(height.dp)
    Box(
        modifier = m
            .then(if (selected) Modifier.border(2.dp, colors.accentStart.copy(alpha = 0.9f), ZenShapes.pill) else Modifier)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BackZenButton(onClick: () -> Unit) {
    val colors = LocalZenColors.current
    ZenCard(
        onClick = onClick,
        modifier = Modifier.size(84.dp),
        shape = ZenShapes.pill,
        cornerRadius = 42.dp
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Zurück",
            tint = colors.onSurface,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
fun ZenScreenHeading(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    val colors = LocalZenColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            BackZenButton(onBack)
            Spacer(Modifier.width(30.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = colors.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalZenColors.current
    LiquidGlassPanel(modifier = modifier, shape = ZenShapes.page, cornerRadius = 36.dp) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 720.dp)
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(30.dp))
                ZenChip(label = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun StepRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val colors = LocalZenColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = Modifier.width(230.dp)
        )
        ZenChip(label = "−", onClick = onDecrease, modifier = Modifier.width(76.dp).height(56.dp))
        Box(
            modifier = Modifier.width(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = colors.accentStart
            )
        }
        ZenChip(label = "+", onClick = onIncrease, modifier = Modifier.width(76.dp).height(56.dp))
    }
}