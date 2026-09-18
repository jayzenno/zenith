package com.zenplayer.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenplayer.app.ui.theme.FocusEffect
import com.zenplayer.app.ui.theme.LocalLiquidState
import com.zenplayer.app.ui.theme.LocalLiquidStyle
import com.zenplayer.app.ui.theme.LocalZenColors
import com.zenplayer.app.ui.theme.LocalZenPreset
import com.zenplayer.app.ui.theme.drawLiquid

object ZenShapes {
    val page: RoundedCornerShape = RoundedCornerShape(36.dp)
    val card: RoundedCornerShape = RoundedCornerShape(28.dp)
    val tile: RoundedCornerShape = RoundedCornerShape(24.dp)
    val pill: RoundedCornerShape = RoundedCornerShape(100.dp)
    val chip: RoundedCornerShape = RoundedCornerShape(14.dp)
}

@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = ZenShapes.card,
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val liquid = LocalLiquidState.current
    val preset = LocalZenPreset.current
    val style = LocalLiquidStyle.current

    Box(modifier.clip(shape)) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind { drawLiquid(liquid, preset) }
                .blur(style.blur)
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            style.glassTint.copy(alpha = (style.frost * 0.55f).coerceIn(0f, 0.8f)),
                            style.glassTint.copy(alpha = (style.frost * 0.30f).coerceIn(0f, 0.8f))
                        )
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .background(preset.accentEnd.copy(alpha = style.frost * 0.10f))
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind { drawGlassSheen(cornerRadius.toPx(), style.sheen) }
        )
        Box(Modifier.matchParentSize()) { content() }
    }
}

private fun DrawScope.drawGlassSheen(cornerRadiusPx: Float, sheen: Float) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (0.16f * sheen).coerceIn(0f, 0.35f)),
                Color.White.copy(alpha = 0.02f)
            ),
            start = Offset(-w * 0.2f, -h * 0.2f),
            end = Offset(w * 1.25f, h)
        ),
        cornerRadius = CornerRadius(cornerRadiusPx)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = (0.55f * sheen).coerceIn(0f, 0.9f)),
                Color.White.copy(alpha = 0.10f)
            ),
            start = Offset.Zero,
            end = Offset(w * 0.45f, 0f)
        ),
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = CornerRadius(cornerRadiusPx),
        style = Stroke(width = 2.2.dp.toPx())
    )
}

@Composable
fun Modifier.zenGlow(
    focused: Boolean,
    shape: Shape = ZenShapes.card
): Modifier = zenFocusEffect(focused, FocusEffect.GLOW, shape)

@Composable
fun Modifier.zenFocusEffect(
    focused: Boolean,
    effect: FocusEffect,
    shape: Shape = ZenShapes.card
): Modifier {
    val colors = LocalZenColors.current
    if (!focused || effect == FocusEffect.ZOOM) return this
    val accent = colors.accentBrush
    return when (effect) {
        FocusEffect.RING -> this
            .shadow(elevation = 18.dp, shape = shape, clip = false)
            .border(3.dp, accent, shape)
        FocusEffect.GLOW -> this
            .shadow(elevation = 30.dp, shape = shape, clip = false)
            .border(2.dp, colors.accentStart.copy(alpha = 0.55f), shape)
        FocusEffect.BAR -> this.drawBehind {
            val barW = 5.dp.toPx()
            drawRect(accent, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(barW, size.height))
        }
        FocusEffect.CORNERS -> this.drawBehind {
            val cornerSize = 18.dp.toPx()
            val stroke = 3.dp.toPx()
            listOf(
                Offset(0f, 0f),
                Offset(size.width - cornerSize, 0f),
                Offset(0f, size.height - cornerSize),
                Offset(size.width - cornerSize, size.height - cornerSize)
            ).forEach { origin ->
                drawRect(
                    brush = accent,
                    topLeft = origin,
                    size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize),
                    style = Stroke(width = stroke)
                )
            }
        }
        FocusEffect.HALO -> this.drawBehind {
            val inset = 6.dp.toPx()
            drawRoundRect(
                brush = accent,
                topLeft = Offset(-inset, -inset),
                size = androidx.compose.ui.geometry.Size(size.width + inset * 2, size.height + inset * 2),
                cornerRadius = CornerRadius(36.dp.toPx()),
                alpha = 0.22f
            )
        }
        FocusEffect.SWEEP -> {
            val transition = rememberInfiniteTransition(label = "sweep")
            val angle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
                label = "sweepAngle"
            )
            this.drawBehind {
                rotate(angle, pivot = center) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, colors.accentStart, colors.accentEnd, Color.Transparent), center = center),
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}