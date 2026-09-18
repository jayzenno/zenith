package com.zenplayer.app.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.ui.components.zenFocusEffect
import com.zenplayer.app.ui.theme.LocalFocusEffect
import com.zenplayer.app.ui.theme.LocalNavStyle
import com.zenplayer.app.ui.theme.LocalZenColors
import com.zenplayer.app.ui.theme.LocalZenPreset

private val NavItems = listOf(
    Triple(Icons.Default.Home, "home", "Start"),
    Triple(Icons.Default.DateRange, "epg", "TV"),
    Triple(Icons.Default.PlayArrow, "vod", "VOD"),
    Triple(Icons.Default.Star, "music", "Musik"),
    Triple(Icons.Default.Settings, "settings", "Einstellungen")
)

@Composable
fun ZenShell(
    settings: ZenSettings,
    container: AppContainer,
    content: @Composable (NavHostController) -> Unit
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val navStyle = LocalNavStyle.current
    val colors = LocalZenColors.current

    Box(Modifier.fillMaxSize().background(colors.background)) {
        ZenBackground(settings, Modifier.fillMaxSize())
        when (navStyle) {
            "top" -> {
                TopNav(currentRoute) { route ->
                    navigateTo(navController, route, currentRoute)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 76.dp)
                ) {
                    ScaledContent(settings, currentRoute) { content(navController) }
                }
            }
            "tiles" -> {
                TileNav(currentRoute) { route ->
                    navigateTo(navController, route, currentRoute)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 200.dp)
                ) {
                    ScaledContent(settings, currentRoute) { content(navController) }
                }
            }
            else -> {
                RailNav(currentRoute) { route ->
                    navigateTo(navController, route, currentRoute)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 92.dp)
                ) {
                    ScaledContent(settings, currentRoute) { content(navController) }
                }
            }
        }
    }
}

private fun navigateTo(navController: NavHostController, route: String, currentRoute: String) {
    if (route != currentRoute) {
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }
}

@Composable
private fun ScaledContent(
    settings: ZenSettings,
    route: String,
    content: @Composable () -> Unit
) {
    val scale = when (route) {
        "home" -> settings.homeScale
        "epg" -> settings.epgScale
        "vod" -> settings.browseScale
        "music" -> settings.browseScale
        "player" -> settings.playerScale
        "settings" -> settings.settingsScale
        "live" -> settings.browseScale
        else -> 1f
    }.takeIf { it.isFinite() }?.coerceIn(0.5f, 2f) ?: 1f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
    ) {
        content()
    }
}

@Composable
private fun ZenBackground(settings: ZenSettings, modifier: Modifier = Modifier) {
    val preset = LocalZenPreset.current
    val colors = LocalZenColors.current
    val bgBlur = settings.backgroundBlurDp.takeIf { it.isFinite() }?.coerceIn(0f, 64f) ?: 0f
    val blurMod = if (bgBlur > 0) Modifier.blur(bgBlur.dp) else Modifier

    Box(
        modifier = modifier.then(blurMod)
    ) {
        when (settings.backgroundType) {
            "plain" -> Box(Modifier.fillMaxSize().background(colors.background))
            "grad" -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(preset.blobA.copy(alpha = 0.35f), colors.background, preset.blobB.copy(alpha = 0.25f), preset.blobC.copy(alpha = 0.2f)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
            )
            "holo" -> {
                Box(Modifier.fillMaxSize().background(colors.background))
                HoloGrid(Modifier.fillMaxSize(), colors.onSurface.copy(alpha = 0.08f))
                Vignette(Modifier.fillMaxSize())
            }
            "aurora" -> AuroraLayer(Modifier.fillMaxSize(), preset, settings.backgroundAnimated)
            "wave" -> WaveLayer(Modifier.fillMaxSize(), preset, settings.backgroundAnimated)
            "blur" -> {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(colors.background, preset.bgBase))
                    )
                )
                BlobLayer(Modifier.fillMaxSize().blur(80.dp), preset, false)
                HoloGrid(Modifier.fillMaxSize(), colors.onSurface)
                Vignette(Modifier.fillMaxSize())
            }
            "none" -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(colors.background.mix(Color.White, 0.04f), colors.background),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY * 0.55f)
                    )
                )
            )
            "static" -> {
                AuroraLayer(Modifier.fillMaxSize(), preset, false)
                BlobLayer(Modifier.fillMaxSize(), preset, false)
                HoloGrid(Modifier.fillMaxSize(), colors.onSurface)
                Vignette(Modifier.fillMaxSize())
            }
            else -> {
                AuroraLayer(Modifier.fillMaxSize(), preset, settings.backgroundAnimated)
                BlobLayer(Modifier.fillMaxSize(), preset, settings.backgroundAnimated)
                HoloGrid(Modifier.fillMaxSize(), colors.onSurface)
                Vignette(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun AuroraLayer(modifier: Modifier = Modifier, preset: com.zenplayer.app.ui.theme.ZenPreset, animated: Boolean) {
    val rotation by if (animated) {
        val transition = rememberInfiniteTransition(label = "aurora")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(70000, easing = LinearEasing), RepeatMode.Restart),
            label = "aurspin"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = modifier
            .rotate(rotation)
            .blur(60.dp)
            .background(
                Brush.sweepGradient(
                    listOf(preset.blobA, preset.blobB, preset.blobC, preset.accentEnd, preset.blobA),
                    center = Offset(0.5f, 0.5f)
                )
            )
            .offset((-12).dp, (-12).dp)
    )
}

@Composable
private fun BlobLayer(modifier: Modifier = Modifier, preset: com.zenplayer.app.ui.theme.ZenPreset, animated: Boolean) {
    val b1x by animatedFloat(0.08f, 0.30f, 26000, animated)
    val b1y by animatedFloat(0.58f, 0.80f, 28000, animated)
    val b2x by animatedFloat(0.72f, 0.96f, 32000, animated)
    val b2y by animatedFloat(0.04f, 0.26f, 33000, animated)
    val b3x by animatedFloat(0.40f, 0.64f, 38000, animated)
    val b3y by animatedFloat(0.30f, 0.52f, 39000, animated)

    Box(modifier) {
        Blob(b1x, b1y, 0.70f, preset.blobA, 80.dp, 0.85f)
        Blob(b2x, b2y, 0.64f, preset.blobB, 90.dp, 0.80f)
        Blob(b3x, b3y, 0.58f, preset.blobC, 100.dp, 0.75f)
    }
}

@Composable
private fun WaveLayer(modifier: Modifier = Modifier, preset: com.zenplayer.app.ui.theme.ZenPreset, animated: Boolean) {
    val phase by if (animated) {
        val transition = rememberInfiniteTransition(label = "wave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
            label = "wavePhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = modifier
            .background(LocalZenColors.current.background)
            .drawBehind { drawWaves(preset, phase) }
    )
}

private fun DrawScope.drawWaves(preset: com.zenplayer.app.ui.theme.ZenPreset, phase: Float) {
    val waves = listOf(preset.blobA, preset.blobB, preset.blobC, preset.accentStart)
    val height = size.height
    val width = size.width
    waves.forEachIndexed { index, color ->
        val amp = (20 + index * 14).dp.toPx()
        val freq = 0.003f + index * 0.001f
        val offset = phase + index * 90f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height / 2)
            for (x in 0..width.toInt() step 8) {
                val y = height / 2 + kotlin.math.sin((x * freq + offset * 0.05f).toDouble()).toFloat() * amp
                lineTo(x.toFloat(), y)
            }
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.18f - index * 0.03f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun animatedFloat(initial: Float, target: Float, duration: Int, animated: Boolean): androidx.compose.runtime.State<Float> {
    return if (animated) {
        val transition = rememberInfiniteTransition(label = "blob")
        transition.animateFloat(
            initialValue = initial,
            targetValue = target,
            animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
            label = "blobVal"
        )
    } else {
        remember { mutableFloatStateOf((initial + target) / 2f) }
    }
}

@Composable
private fun Blob(
    fx: Float,
    fy: Float,
    size: Float,
    color: Color,
    blurRadius: androidx.compose.ui.unit.Dp,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = (fx * 100 - size * 50).dp, y = (fy * 100 - size * 50).dp)
    ) {
        Box(
            modifier = Modifier
                .size((size * 100).dp)
                .clip(RoundedCornerShape(50))
                .blur(blurRadius)
                .background(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                        center = Offset(0.4f, 0.4f),
                        radius = 0.62f
                    )
                )
        )
    }
}

@Composable
private fun HoloGrid(modifier: Modifier = Modifier, onSurface: Color) {
    Box(
        modifier = modifier.drawBehind {
            drawHoloGrid(onSurface)
        }
    )
}

private fun DrawScope.drawHoloGrid(onSurface: Color) {
    val step = 56.dp.toPx()
    val lineColor = onSurface.copy(alpha = 0.035f)
    var x = 0f
    while (x < size.width) {
        drawLine(lineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

@Composable
private fun Vignette(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                0.55f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.35f),
                center = Offset(0.5f, 0f),
                radius = 1.3f
            )
        )
    )
}

@Composable
private fun RailNav(currentRoute: String, onNavigate: (String) -> Unit) {
    val colors = LocalZenColors.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 18.dp)
            .width(70.dp)
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(70.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface.copy(alpha = 0.42f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NavItems.forEach { (icon, route, _) ->
                    RailItem(icon, route, currentRoute, onNavigate)
                }
            }
        }
    }
}

@Composable
private fun TopNav(currentRoute: String, onNavigate: (String) -> Unit) {
    val colors = LocalZenColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItems.forEach { (icon, route, label) ->
            TopNavItem(icon, route, label, currentRoute, onNavigate)
        }
    }
}

@Composable
private fun TileNav(currentRoute: String, onNavigate: (String) -> Unit) {
    val colors = LocalZenColors.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(200.dp)
            .padding(18.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface.copy(alpha = 0.42f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        NavItems.forEach { (icon, route, label) ->
            TileNavItem(icon, route, label, currentRoute, onNavigate)
        }
    }
}

@Composable
private fun RailItem(icon: ImageVector, route: String, currentRoute: String, onNavigate: (String) -> Unit) {
    NavItemBox(icon, route, route, currentRoute, onNavigate, showLabel = false, modifier = Modifier.size(50.dp))
}

@Composable
private fun TopNavItem(icon: ImageVector, route: String, label: String, currentRoute: String, onNavigate: (String) -> Unit) {
    NavItemBox(icon, route, label, currentRoute, onNavigate, showLabel = true, modifier = Modifier.height(50.dp))
}

@Composable
private fun TileNavItem(icon: ImageVector, route: String, label: String, currentRoute: String, onNavigate: (String) -> Unit) {
    NavItemBox(icon, route, label, currentRoute, onNavigate, showLabel = true, modifier = Modifier.fillMaxWidth().height(64.dp))
}

@Composable
private fun NavItemBox(
    icon: ImageVector,
    route: String,
    label: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalZenColors.current
    val focusEffect = LocalFocusEffect.current
    var focused by remember { mutableStateOf(false) }
    val selected = currentRoute == route
    val bg = if (selected) colors.accentBrush else null
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (bg != null) Modifier.background(bg) else Modifier.background(Color.Transparent))
            .zenFocusEffect(focused, focusEffect, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onNavigate(route) }
            )
            .focusable()
            .onFocusChanged { focused = it.isFocused && it.hasFocus },
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = route, tint = if (selected) colors.onPrimary else colors.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) colors.onPrimary else colors.onSurface.copy(alpha = 0.7f))
            }
        } else {
            Icon(icon, contentDescription = route, tint = if (selected) colors.onPrimary else colors.onSurface.copy(alpha = 0.55f), modifier = Modifier.size(24.dp))
        }
    }
}

private fun Color.mix(with: Color, amount: Float): Color =
    androidx.compose.ui.graphics.lerp(this, with, amount.coerceIn(0f, 1f))
