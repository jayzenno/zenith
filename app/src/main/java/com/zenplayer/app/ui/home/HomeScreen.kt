package com.zenplayer.app.ui.home

import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.ui.components.EmptyState
import com.zenplayer.app.ui.components.ZenCard
import com.zenplayer.app.ui.epg.EpgChannel
import com.zenplayer.app.ui.epg.mapDbChannel
import com.zenplayer.app.ui.epg.nowMin
import com.zenplayer.app.ui.epg.nowProg
import com.zenplayer.app.ui.player.channelQualityHint
import com.zenplayer.app.ui.theme.LocalHomeStyle
import com.zenplayer.app.ui.theme.LocalZenColors
import com.zenplayer.app.ui.theme.LocalZenPreset
import com.zenplayer.app.ui.theme.ZenColors
import com.zenplayer.app.ui.theme.ZenPreset

/**
 * Real player route for a real provider channel ("player/{provider}/LIVE/{channelId}").
 * Every Home card that looks playable starts the actual stream — never a placeholder.
 */
private fun playRoute(channel: Channel): String =
    "player/${channel.providerId}/${channel.mediaType.name}/${Uri.encode(channel.id)}"

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val colors = LocalZenColors.current
    val preset = LocalZenPreset.current
    val homeStyle = LocalHomeStyle.current
    val app = LocalContext.current.applicationContext as ZenPlayerApplication
    // Repository-bound: the same deterministic `allLiveChannels()` flow the Guide uses
    // (ORDER BY number, name), so Home channel indices stay in the Guide's index space.
    val channels by app.container.channels.allLiveChannels()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by app.container.settings.settingsFlow
        .collectAsStateWithLifecycle(initialValue = ZenSettings())

    // Display mapping identical to the Guide: provider number (else position+1),
    // name-based art/code. Pure derivation from REAL channel data.
    val epg: List<EpgChannel> = remember(channels) { channels.mapIndexed { i, ch -> mapDbChannel(ch, i) } }

    when (homeStyle) {
        "kanal" -> KanalHome(channels, epg, onNavigate)
        else -> HubHome(channels, epg, settings, onNavigate, colors, preset)
    }
}

@Composable
private fun HubHome(
    channels: List<Channel>,
    epg: List<EpgChannel>,
    settings: ZenSettings,
    onNavigate: (String) -> Unit,
    colors: ZenColors,
    preset: ZenPreset
) {
    val scroll = rememberScrollState()
    val favs = settings.epgFavs
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 28.dp, vertical = 26.dp)
    ) {
        HeroBanner(channels, epg, settings, onNavigate, colors, preset)
        if (channels.isEmpty()) return@Column
        Spacer(Modifier.height(26.dp))
        if (favs.isNotEmpty()) {
            SectionTitle("Deine Favoriten", "Zum Live-TV", colors)
            Spacer(Modifier.height(12.dp))
            FavRow(channels, epg, favs, onNavigate, colors)
            Spacer(Modifier.height(26.dp))
        }
        SectionTitle("Jetzt LIVE", "Sender", colors)
        Spacer(Modifier.height(12.dp))
        LiveTileRow(channels, epg, onNavigate, colors)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun KanalHome(channels: List<Channel>, epg: List<EpgChannel>, onNavigate: (String) -> Unit) {
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
        if (channels.isEmpty()) {
            item {
                EmptyState(
                    title = "Noch keine Kanäle",
                    message = "Füge in den Einstellungen einen IPTV-Anbieter hinzu und synchronisiere ihn, damit hier deine echten Sender erscheinen.",
                    actionLabel = "Einstellungen öffnen",
                    onAction = { onNavigate("settings") },
                    modifier = Modifier.fillMaxWidth().height(480.dp)
                )
            }
        } else {
            itemsIndexed(channels) { vi, ch ->
                // The display mapping is derived from the same real channel; getOrNull only
                // covers a stale index while the flow refreshes (e.g. provider re-sync).
                val ec = epg.getOrNull(vi) ?: mapDbChannel(ch, vi)
                val prog = nowProg(vi, 0, nowMin())
                ZenCard(
                    onClick = { onNavigate(playRoute(ch)) },
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
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(ec.colorStart),
                                        Color(ec.colorEnd)
                                    )
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ec.code, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(ch.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(prog?.t ?: "Keine Programmdaten", fontSize = 11.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        channelQualityHint(ch.name, ch.url)?.let { q ->
                            QualityChip(q, colors)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    channels: List<Channel>,
    epg: List<EpgChannel>,
    settings: ZenSettings,
    onNavigate: (String) -> Unit,
    colors: ZenColors,
    preset: ZenPreset
) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    val heroIdx = homeHeroIndex(channels, settings.epgFavs, settings.epgRecent)
    val heroCh = heroIdx?.let { channels.getOrNull(it) }
    val heroEpg = heroIdx?.let { epg.getOrNull(it) }
    val prog = heroIdx?.let { nowProg(it, 0, nowMin()) }

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
        if (heroCh != null && heroEpg != null) {
            // REAL live channel + REAL current programme — never an invented banner.
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
                    channelQualityHint(heroCh.name, heroCh.url)?.let { q ->
                        QualityChip(q, colors)
                    }
                    Text("Kanal ${heroEpg.num}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface.copy(alpha = 0.55f))
                }
                Spacer(Modifier.height(10.dp))
                Text(heroCh.name, fontSize = 38.sp, fontWeight = FontWeight.Black, color = colors.onSurface, lineHeight = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    prog?.t ?: "Keine Programmdaten für diesen Sender",
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
                            .clickable { onNavigate(playRoute(heroCh)) }
                            .padding(horizontal = 24.dp, vertical = 11.dp)
                    ) {
                        Text("Jetzt ansehen", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.background)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                            .clickable { onNavigate("epg") }
                            .padding(horizontal = 24.dp, vertical = 11.dp)
                    ) {
                        Text("Zum Guide", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                    }
                }
            }
        } else {
            // Honest onboarding state instead of an invented "now playing" banner.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(36.dp)
            ) {
                Text("Willkommen bei Zenith", fontSize = 34.sp, fontWeight = FontWeight.Black, color = colors.onSurface, lineHeight = 1.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Füge deinen ersten IPTV-Anbieter hinzu, damit hier deine echten Sender und Programmdaten erscheinen.",
                    fontSize = 13.sp,
                    color = colors.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(460.dp)
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.accentBrush)
                        .clickable { onNavigate("settings") }
                        .padding(horizontal = 24.dp, vertical = 11.dp)
                ) {
                    Text("Einstellungen öffnen", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.background)
                }
            }
        }
    }
}

@Composable
private fun HeroArt(colors: ZenColors, preset: ZenPreset, modifier: Modifier = Modifier) {
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
private fun SectionTitle(title: String, action: String, colors: ZenColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.onSurface, letterSpacing = (-0.3).sp)
        Spacer(Modifier.weight(1f))
        Text(action.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.55f), letterSpacing = 1.sp)
    }
}

/** Honest quality badge: only rendered when the provider data actually implies a quality. */
@Composable
private fun QualityChip(quality: String, colors: ZenColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(colors.surfaceHigh)
            .border(1.dp, colors.onSurface.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(quality.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.85f))
    }
}

@Composable
private fun FavRow(
    channels: List<Channel>,
    epg: List<EpgChannel>,
    favs: Set<Int>,
    onNavigate: (String) -> Unit,
    colors: ZenColors
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        favs.filter { it in channels.indices }.take(4).forEach { vi ->
            FavCard(channels[vi], epg.getOrNull(vi), vi, onNavigate, Modifier.weight(1f), colors)
        }
    }
}

@Composable
private fun FavCard(
    ch: Channel,
    ec: EpgChannel?,
    vi: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier,
    colors: ZenColors
) {
    val prog = nowProg(vi, 0, nowMin())
    ZenCard(
        onClick = { onNavigate(playRoute(ch)) },
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(16.dp),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(ec?.colorStart ?: 0xFF222222),
                            Color(ec?.colorEnd ?: 0xFF333333)
                        )
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(ec?.code ?: "?", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ch.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(prog?.t ?: "–", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            channelQualityHint(ch.name, ch.url)?.let { q ->
                QualityChip(q, colors)
            }
        }
    }
}

@Composable
private fun LiveTileRow(
    channels: List<Channel>,
    epg: List<EpgChannel>,
    onNavigate: (String) -> Unit,
    colors: ZenColors
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        channels.take(4).forEachIndexed { i, ch ->
            LiveTile(ch, epg.getOrNull(i), i, onNavigate, Modifier.weight(1f), colors)
        }
    }
}

@Composable
private fun LiveTile(
    ch: Channel,
    ec: EpgChannel?,
    vi: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier,
    colors: ZenColors
) {
    val prog = nowProg(vi, 0, nowMin())
    ZenCard(
        onClick = { onNavigate(playRoute(ch)) },
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(16.dp),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                (ec?.num ?: 0).toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.align(Alignment.End)
            )
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
