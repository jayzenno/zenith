package com.zenplayer.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
fun rememberLiquidState(durationMillis: Int = 16000): LiquidState {
    val transition = rememberInfiniteTransition(label = "liquid")

    @Composable
    fun animated(
        initial: Float,
        target: Float,
        duration: Int,
        reverse: Boolean
    ): State<Float> = transition.animateFloat(
        initialValue = initial,
        targetValue = target,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "blob"
    )

    val b0 = ZenBlobState(
        cx = animated(0.08f, 0.30f, durationMillis, true),
        cy = animated(0.62f, 0.90f, durationMillis + 2000, true),
        radius = animated(0.44f, 0.62f, (durationMillis * 0.9).toInt(), true),
        tint = animated(0f, 1f, durationMillis / 2, false)
    )
    val b1 = ZenBlobState(
        cx = animated(0.72f, 0.94f, durationMillis + 3000, true),
        cy = animated(0.08f, 0.30f, durationMillis + 1000, true),
        radius = animated(0.40f, 0.58f, (durationMillis * 0.8).toInt(), true),
        tint = animated(1f, 0f, durationMillis / 3, false)
    )
    val b2 = ZenBlobState(
        cx = animated(0.46f, 0.64f, durationMillis + 5000, true),
        cy = animated(0.38f, 0.56f, durationMillis + 4000, true),
        radius = animated(0.52f, 0.72f, durationMillis, true),
        tint = animated(0f, 1f, durationMillis / 4, false)
    )
    return remember { LiquidState(listOf(b0, b1, b2)) }
}

@Composable
fun LiquidBackdrop(modifier: Modifier = Modifier) {
    val liquid = LocalLiquidState.current
    val preset = LocalZenPreset.current
    Canvas(modifier = modifier) {
        drawLiquid(liquid, preset)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.22f))
            )
        )
    }
}