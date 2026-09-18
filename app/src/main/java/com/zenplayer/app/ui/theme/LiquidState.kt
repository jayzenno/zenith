package com.zenplayer.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue

data class ZenBlobState(
    val cx: State<Float>,
    val cy: State<Float>,
    val radius: State<Float>,
    val tint: State<Float>
)

class LiquidState(
    val blobs: List<ZenBlobState>
)

fun DrawScope.drawLiquid(
    state: LiquidState,
    preset: ZenPreset,
    coverAlpha: Float = 1f
) {
    val blobColors = listOf(preset.blobA, preset.blobB, preset.blobC)
    state.blobs.forEachIndexed { index, blob ->
        if (index >= blobColors.size) return@forEachIndexed
        val base = blobColors[index]
        val accent = preset.accentEnd
        val mixed = androidx.compose.ui.graphics.lerp(base, accent, 0.35f)
        val current = androidx.compose.ui.graphics.lerp(base, mixed, blob.tint.value)
        val center = Offset(size.width * blob.cx.value, size.height * blob.cy.value)
        val radius = size.maxDimension * blob.radius.value
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(current.copy(alpha = coverAlpha), current.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}