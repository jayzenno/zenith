package com.zenplayer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenplayer.app.ui.theme.FocusEffect
import com.zenplayer.app.ui.theme.LocalCardStyle
import com.zenplayer.app.ui.theme.LocalFocusEffect
import com.zenplayer.app.ui.theme.LocalZenColors

@Composable
fun ZenCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ZenShapes.card,
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    val focusEffect = LocalFocusEffect.current
    val cardStyle = LocalCardStyle.current
    val colors = LocalZenColors.current
    var focused by remember { mutableStateOf(false) }
    val zoomEnabled = focusEffect == FocusEffect.ZOOM
    val scale by animateFloatAsState(
        targetValue = if (focused && zoomEnabled) 1.06f else 1f,
        animationSpec = tween(durationMillis = 220)
    )
    Box(
        modifier
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
            .zenFocusEffect(focused, focusEffect, shape)
            // onFocusChanged must precede the focus target (clickable) in the modifier chain:
            // focus events propagate outward from the active FocusTargetNode and stop at the
            // next outer FocusTarget. One focus target per element (clickable provides it).
            .onFocusChanged { state -> focused = state.isFocused && state.hasFocus }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        when (cardStyle) {
            "grad" -> Box(Modifier.matchParentSize().clip(shape).background(colors.accentBrush))
            "flat" -> Box(Modifier.matchParentSize().clip(shape).background(colors.surface))
            else -> LiquidGlassPanel(shape = shape, cornerRadius = cornerRadius) {}
        }
        Box(Modifier.matchParentSize()) { content(focused) }
    }
}