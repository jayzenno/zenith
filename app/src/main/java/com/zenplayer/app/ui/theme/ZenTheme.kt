package com.zenplayer.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.zenplayer.app.data.settings.ZenSettings

enum class FocusEffect {
    RING, GLOW, BAR, ZOOM, CORNERS, HALO, SWEEP
}

fun String.toFocusEffect(): FocusEffect = runCatching { FocusEffect.valueOf(this.uppercase()) }.getOrDefault(FocusEffect.RING)

data class ZenColors(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val primary: Color,
    val onPrimary: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val onBackground: Color,
    val onSurface: Color
) {
    val accentBrush: androidx.compose.ui.graphics.Brush
        get() = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(accentStart, accentEnd)
        )
}

data class LiquidStyle(
    val blur: Dp,
    val frost: Float,
    val sheen: Float,
    val glassTint: Color = Color.White
)

val LocalZenColors = androidx.compose.runtime.staticCompositionLocalOf {
    ZenColors(
        background = Color(0xFF06121F),
        surface = Color(0xFF0E1B2A),
        surfaceHigh = Color(0xFF15283B),
        primary = Color(0xFF00E5C6),
        onPrimary = Color(0xFF001712),
        accentStart = Color(0xFF00E5C6),
        accentEnd = Color(0xFF0057FF),
        onBackground = Color(0xFFF0FFFC),
        onSurface = Color(0xFFF0FFFC)
    )
}

val LocalZenPreset = androidx.compose.runtime.staticCompositionLocalOf { ThemePresets.all.first() }

val LocalLiquidStyle = androidx.compose.runtime.staticCompositionLocalOf {
    LiquidStyle(blur = 30.dp, frost = 0.42f, sheen = 0.9f)
}

val LocalLiquidState = androidx.compose.runtime.staticCompositionLocalOf { LiquidState(emptyList()) }

val LocalFocusEffect = androidx.compose.runtime.staticCompositionLocalOf { FocusEffect.RING }

val LocalCardStyle = androidx.compose.runtime.staticCompositionLocalOf { "glass" }

val LocalNavStyle = androidx.compose.runtime.staticCompositionLocalOf { "rail" }

val LocalHomeStyle = androidx.compose.runtime.staticCompositionLocalOf { "hub" }

fun Typography.scaled(factor: Float): Typography {
    val f = factor.coerceIn(0.5f, 2f)
    fun TextStyle.scale(): TextStyle = copy(
        fontSize = if (fontSize.isUnspecified) fontSize else fontSize * f,
        lineHeight = if (lineHeight.isUnspecified) lineHeight else lineHeight * f
    )
    return copy(
        displayLarge = displayLarge.scale(),
        displayMedium = displayMedium.scale(),
        displaySmall = displaySmall.scale(),
        headlineLarge = headlineLarge.scale(),
        headlineMedium = headlineMedium.scale(),
        headlineSmall = headlineSmall.scale(),
        titleLarge = titleLarge.scale(),
        titleMedium = titleMedium.scale(),
        titleSmall = titleSmall.scale(),
        bodyLarge = bodyLarge.scale(),
        bodyMedium = bodyMedium.scale(),
        bodySmall = bodySmall.scale(),
        labelLarge = labelLarge.scale(),
        labelMedium = labelMedium.scale(),
        labelSmall = labelSmall.scale()
    )
}

fun buildZenColors(preset: ZenPreset, customAccentArgb: Long?): ZenColors {
    val accent = if (customAccentArgb == null) preset.accentStart else Color(customAccentArgb.toInt())
    val accentEnd = if (customAccentArgb == null) preset.accentEnd else accent.rotateHue(24f)
    return ZenColors(
        background = preset.bgBase,
        surface = preset.bgBase.mix(Color.White, 0.07f),
        surfaceHigh = preset.bgBase.mix(Color.White, 0.13f),
        primary = accent,
        onPrimary = accent.lightened(0.55f).mix(Color.Black, 0.88f),
        accentStart = accent,
        accentEnd = accentEnd,
        onBackground = preset.onSurface,
        onSurface = preset.onSurface
    )
}

@Composable
fun ZenTheme(
    settings: ZenSettings,
    showBackdrop: Boolean = true,
    content: @Composable () -> Unit
) {
    val preset = ThemePresets.byId(settings.themePresetId)
    val colors = buildZenColors(preset, settings.customAccentArgb)
    val liquid = rememberLiquidState()
    val glassBlur = settings.glassBlurDp.takeIf { it.isFinite() }?.coerceIn(0f, 64f) ?: 0f
    val style = LiquidStyle(
        blur = glassBlur.dp,
        frost = settings.glassFrost.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.42f,
        sheen = settings.glassSheen.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.9f
    )
    val focusEffect = settings.focusEffect.toFocusEffect()
    val density = LocalDensity.current
    val densityScale = settings.densityScale.takeIf { it.isFinite() }?.coerceIn(0.5f, 2f) ?: 1f
    val scaledDensity = remember(density, densityScale) {
        Density(density.density * densityScale, density.fontScale)
    }
    val fontSizeScale = when (settings.fontSize) {
        "small" -> 0.85f
        "large" -> 1.15f
        else -> 1f
    }
    val typography = remember(fontSizeScale) { ZenTypography.scaled(fontSizeScale) }
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalZenColors provides colors,
        LocalZenPreset provides preset,
        LocalLiquidStyle provides style,
        LocalLiquidState provides liquid,
        LocalFocusEffect provides focusEffect,
        LocalCardStyle provides settings.cardStyle,
        LocalNavStyle provides settings.navStyle,
        LocalHomeStyle provides settings.homeStyle
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.accentStart,
                onPrimary = colors.onPrimary,
                secondary = colors.accentEnd,
                background = colors.background,
                surface = colors.surface,
                surfaceVariant = colors.surfaceHigh,
                onSurface = colors.onSurface,
                onBackground = colors.onBackground
            ),
            typography = typography
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                if (showBackdrop) {
                    LiquidBackdrop(Modifier.fillMaxSize())
                }
                content()
            }
        }
    }
}