package com.zenplayer.app.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenplayer.app.ZenPlayerApplication
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.ui.components.ZenCard
import com.zenplayer.app.ui.components.ZenShapes
import com.zenplayer.app.ui.epg.EPG_CHANNELS
import com.zenplayer.app.ui.epg.channelArt
import com.zenplayer.app.ui.epg.nowMin
import com.zenplayer.app.ui.epg.nowProg
import com.zenplayer.app.ui.theme.LocalHomeStyle
import com.zenplayer.app.ui.theme.LocalZenColors
import com.zenplayer.app.ui.theme.LocalZenPreset

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val colors = LocalZenColors.current
    val preset = LocalZenPreset.current
    val homeStyle = LocalHomeStyle.current
    val app = LocalContext.current.applicationContext as ZenPlayerApplication
    val providers by app.container.iptvRepo.providersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by app.container.settings.settingsFlow.collectAsStateWithLifecycle(initialValue = ZenSettings())
    val favs = settings.epgFavs

    when (homeStyle) {
        "kanal" -> KanalHome(settings, onNavigate)
        else -> HubHome(settings, favs, onNavigate, colors, preset)
    }
}

@Composable
private fun HubHome(
    settings: ZenSettings,
    favs: Set<Int>,
    onNavigate: (String) -> Unit,
    colors: com.zenplayer.app.ui.theme.ZenColors,
    preset: com.zenplayer.app.ui.theme.ZenPreset
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 28.dp, vertical = 26.dp)
    ) {
        HeroBanner(onNavigate, colors, preset)
        Spacer(Modifier.height(26.dp))
        if (favs.isNotEmpty()) {
            SectionTitle("Deine Favoriten", "Zum Live-TV", colors)
            Spacer(Modifier.height(12.dp))
            FavRow(favs, colors)
            Spacer(Modifier.height(26.dp))
        }
        SectionTitle("Jetzt LIVE", "Sender", colors)
        Spacer(Modifier.height(12.dp))
        LiveTileRow(colors)
        Spacer(Modifier.height(26.dp))
        SectionTitle("Empfohlen für dich", "Mehr ansehen", colors)
        Spacer(Modifier.height(12.dp))
        PostRow(colors)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun KanalHome(settings: ZenSettings, onNavigate: (String) -> Unit) {
    val colors = LocalZenColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text("Kanalliste", style = MaterialTheme.typography.displaySmall, color = colors.onSurface)
            Spacer(Modifier.height(18.dp))
        }
        items(EPG_CHANNELS) { ch ->
            val vi = EPG_CHANNELS.indexOf(ch)
            val prog = nowProg(vi, 0, nowMin())
            ZenCard(
                onClick = { onNavigate("live") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                cornerRadius = 16.dp
            ) { focused ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(channelArt(vi))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ch.code, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(ch.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(prog?.t ?: "–", fontSize = 11.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(colors.accentStart).padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("HD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = colors.background)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(onNavigate: (String) -> Unit, colors: com.zenplayer.app.ui.theme.ZenColors, preset: com.zenplayer.app.ui.theme.ZenPreset) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(preset.blobB, preset.blobA, colors.background),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(1.dp, line, RoundedCornerShape(16.dp))
            .clickable { onNavigate("live") }
    ) {
        HeroArt(colors, preset, Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.25f), Color.Transparent),
                        startX = 0f,
                        endX = Float.POSITIVE_INFINITY
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(36.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.accentStart)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = colors.background)
                }
                Text("Sender 17 · Sky", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface.copy(alpha = 0.55f))
                Text("Spielfilm", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface.copy(alpha = 0.55f))
            }
            Spacer(Modifier.height(10.dp))
            Text("Action Now", fontSize = 38.sp, fontWeight = FontWeight.Black, color = colors.onSurface, lineHeight = 1.sp)
            Text(
                "Ein Undercover-Ermittler flieht vor seiner Vergangenheit – und gerät mitten in einen internationalen Schlagabtausch.",
                fontSize = 13.sp,
                color = colors.onSurface.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(460.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.accentBrush)
                        .clickable { onNavigate("live") }
                        .padding(horizontal = 24.dp, vertical = 11.dp)
                ) {
                    Text("Jetzt ansehen", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.background)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 24.dp, vertical = 11.dp)
                ) {
                    Text("Details", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                }
            }
        }
    }
}

@Composable
private fun HeroArt(colors: com.zenplayer.app.ui.theme.ZenColors, preset: com.zenplayer.app.ui.theme.ZenPreset, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hero")
    val y1 by transition.animateFloat(0f, 24f, infiniteRepeatable(tween(14000), RepeatMode.Reverse))
    val y2 by transition.animateFloat(0f, -18f, infiniteRepeatable(tween(18000), RepeatMode.Reverse))
    Box(modifier = modifier.drawBehind {
        val r1 = size.minDimension * 0.55f
        drawCircle(
            Brush.radialGradient(listOf(preset.blobA, Color.Transparent), center = Offset(size.width * 0.15f, size.height * 0.2f + y1), radius = r1),
            radius = r1,
            center = Offset(size.width * 0.15f, size.height * 0.2f + y1)
        )
        val r2 = size.minDimension * 0.48f
        drawCircle(
            Brush.radialGradient(listOf(preset.blobB, Color.Transparent), center = Offset(size.width * 0.75f, size.height * 0.15f + y2), radius = r2),
            radius = r2,
            center = Offset(size.width * 0.75f, size.height * 0.15f + y2)
        )
    })
}

@Composable
private fun SectionTitle(title: String, action: String, colors: com.zenplayer.app.ui.theme.ZenColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.onSurface, letterSpacing = (-0.3).sp)
        Spacer(Modifier.weight(1f))
        Text(action.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.55f), letterSpacing = 1.sp)
    }
}

@Composable
private fun FavRow(favs: Set<Int>, colors: com.zenplayer.app.ui.theme.ZenColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        favs.take(4).forEach { vi ->
            if (vi in EPG_CHANNELS.indices) {
                FavCard(vi, Modifier.weight(1f), colors)
            }
        }
    }
}

@Composable
private fun FavCard(vi: Int, modifier: Modifier, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val ch = EPG_CHANNELS[vi]
    val prog = nowProg(vi, 0, nowMin())
    ZenCard(
        onClick = {},
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(16.dp),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(channelArt(vi))),
                contentAlignment = Alignment.Center
            ) {
                Text(ch.code, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ch.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(prog?.t ?: "–", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(colors.accentStart).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("HD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = colors.background)
            }
        }
    }
}

@Composable
private fun LiveTileRow(colors: com.zenplayer.app.ui.theme.ZenColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(16, 17, 18, 19).filter { it in EPG_CHANNELS.indices }.forEach { vi ->
            LiveTile(vi, Modifier.weight(1f), colors)
        }
    }
}

@Composable
private fun LiveTile(vi: Int, modifier: Modifier, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val ch = EPG_CHANNELS[vi]
    val prog = nowProg(vi, 0, nowMin())
    ZenCard(
        onClick = {},
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(16.dp),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(ch.num.toString(), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.14f), modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(colors.accentStart)
            )
            Spacer(Modifier.height(8.dp))
            Text(ch.name, fontSize = 14.5.sp, fontWeight = FontWeight.Black, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(prog?.t ?: "–", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PostRow(colors: com.zenplayer.app.ui.theme.ZenColors) {
    val posts = listOf(
        "Tatort" to "Krimi",
        "Die Anstalt" to "Satire",
        "Top Gun: Maverick" to "Film",
        "Bundesliga: Topspiel" to "Sport",
        "Bares für Rares" to "Show"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        posts.forEach { (title, tag) ->
            PostCard(title, tag, Modifier.weight(1f), colors)
        }
    }
}

@Composable
private fun PostCard(title: String, tag: String, modifier: Modifier, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val preset = LocalZenPreset.current
    val idx = title.length % 5
    val bg = when (idx) {
        0 -> Brush.verticalGradient(listOf(preset.blobA, preset.blobB))
        1 -> Brush.verticalGradient(listOf(preset.blobB, preset.blobC))
        2 -> Brush.verticalGradient(listOf(preset.blobC, preset.accentStart))
        3 -> Brush.verticalGradient(listOf(preset.accentStart, preset.accentEnd))
        else -> Brush.verticalGradient(listOf(colors.surfaceHigh, colors.surface))
    }
    val line = colors.onSurface.copy(alpha = 0.12f)
    ZenCard(
        onClick = {},
        modifier = modifier.height(220.dp),
        shape = RoundedCornerShape(12.dp),
        cornerRadius = 12.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(bg).border(1.dp, line, RoundedCornerShape(12.dp)).padding(10.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(tag.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
