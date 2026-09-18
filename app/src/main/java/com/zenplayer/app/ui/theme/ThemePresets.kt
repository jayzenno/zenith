package com.zenplayer.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

data class ZenPreset(
    val id: String,
    val name: String,
    val bgBase: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val onSurface: Color
)

object ThemePresets {

    val all = listOf(
        ZenPreset(
            id = "aqua", name = "Aqua Glass",
            bgBase = Color(0xFF07070C), blobA = Color(0xFF12424E), blobB = Color(0xFF16345C), blobC = Color(0xFF171040),
            accentStart = Color(0xFF35CFB2), accentEnd = Color(0xFF3D7BFF), onSurface = Color(0xFFF2F4F8)
        ),
        ZenPreset(
            id = "midnight", name = "Midnight",
            bgBase = Color(0xFF050814), blobA = Color(0xFF1B2A6B), blobB = Color(0xFF3A0CA3), blobC = Color(0xFF0A3AA9),
            accentStart = Color(0xFF6272FF), accentEnd = Color(0xFF0A3AA9), onSurface = Color(0xFFECF0FF)
        ),
        ZenPreset(
            id = "glacier", name = "Neon Glacier",
            bgBase = Color(0xFF0B0217), blobA = Color(0xFF7B2FF7), blobB = Color(0xFF00F0FF), blobC = Color(0xFFF72585),
            accentStart = Color(0xFF00F0FF), accentEnd = Color(0xFFB05CFF), onSurface = Color(0xFFF4F0FF)
        ),
        ZenPreset(
            id = "solar", name = "Solar Ember",
            bgBase = Color(0xFF160F02), blobA = Color(0xFFFFB703), blobB = Color(0xFFFB8500), blobC = Color(0xFF3A1C00),
            accentStart = Color(0xFFFFB703), accentEnd = Color(0xFFFB8500), onSurface = Color(0xFFFFF9EC)
        ),
        ZenPreset(
            id = "emerald", name = "Emerald Moss",
            bgBase = Color(0xFF04100C), blobA = Color(0xFF10B981), blobB = Color(0xFF047857), blobC = Color(0xFF0A3D2E),
            accentStart = Color(0xFF34E0A0), accentEnd = Color(0xFF047857), onSurface = Color(0xFFEFFFF8)
        ),
        ZenPreset(
            id = "crimson", name = "Royal Crimson",
            bgBase = Color(0xFF12040A), blobA = Color(0xFFE63946), blobB = Color(0xFF800000), blobC = Color(0xFF2B000A),
            accentStart = Color(0xFFFF5D73), accentEnd = Color(0xFF9D0208), onSurface = Color(0xFFFFF2F4)
        ),
        ZenPreset(
            id = "rose", name = "Rose Quartz",
            bgBase = Color(0xFF160710), blobA = Color(0xFFFF8FAB), blobB = Color(0xFFC9184A), blobC = Color(0xFF5C002E),
            accentStart = Color(0xFFFFB3CC), accentEnd = Color(0xFFC9184A), onSurface = Color(0xFFFFF3F7)
        ),
        ZenPreset(
            id = "carbon", name = "Carbon Mono",
            bgBase = Color(0xFF070707), blobA = Color(0xFF3A3A3A), blobB = Color(0xFF6E6E6E), blobC = Color(0xFF1C1C1C),
            accentStart = Color(0xFFF5F5F5), accentEnd = Color(0xFF9E9E9E), onSurface = Color(0xFFF7F7F7)
        ),
        ZenPreset(
            id = "ocean", name = "Deep Ocean",
            bgBase = Color(0xFF020A12), blobA = Color(0xFF48CAE4), blobB = Color(0xFF0077B6), blobC = Color(0xFF023E8A),
            accentStart = Color(0xFF90E0EF), accentEnd = Color(0xFF0077B6), onSurface = Color(0xFFF0FAFF)
        ),
        ZenPreset(
            id = "nebula", name = "Nebula",
            bgBase = Color(0xFF0A0616), blobA = Color(0xFF64DFDF), blobB = Color(0xFF7400B8), blobC = Color(0xFF2D00F7),
            accentStart = Color(0xFF64DFDF), accentEnd = Color(0xFF7400B8), onSurface = Color(0xFFF4F6FF)
        )
    )

    fun byId(id: String): ZenPreset = all.firstOrNull { it.id == id } ?: all.first()
}

fun Color.rotateHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees / 360f).mod(1f)
    val argb = android.graphics.Color.HSVToColor(255, hsv)
    return Color(argb)
}

fun Color.mix(with: Color, amount: Float): Color =
    androidx.compose.ui.graphics.lerp(this, with, amount.coerceIn(0f, 1f))

fun Color.lightened(factor: Float): Color =
    mix(Color.White, factor)

fun Color.withLightness(target: Float): Color {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsl)
    // hsl[2] is value (brightness); emulate lightness target by scaling value
    val r = ((target - 1f) / 1f).coerceIn(-1f, 1f)
    val adjusted = if (r >= 0) (hsl[1] * (1f - r)).coerceIn(0f, 1f) else hsl[1].coerceIn(0f, 1f)
    hsl[1] = adjusted
    hsl[2] = (1f - (1f - target)).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(255, hsl))
}

fun Long.toColor(): Color = Color(toInt())