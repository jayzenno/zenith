package com.zenplayer.app.ui.epg

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.di.AppViewModelFactory
import com.zenplayer.app.ui.components.zenFocusEffect
import com.zenplayer.app.ui.theme.LocalFocusEffect
import com.zenplayer.app.ui.theme.LocalZenColors
import kotlinx.coroutines.delay

private const val EPG_START = 300
private const val EPG_END = 1740

private val BaseChColW = 200.dp
private val BaseTimeColW = 120.dp
private val BaseChRowH = 66.dp
private val BaseGridHeaderH = 48.dp
private val BaseTopBarH = 56.dp

private fun ZenSettings.scaledChColW(): androidx.compose.ui.unit.Dp = BaseChColW * epgSize.coerceAtLeast(0.5f)
private fun ZenSettings.scaledTimeColW(): androidx.compose.ui.unit.Dp = BaseTimeColW * epgColW.coerceAtLeast(0.5f)
private fun ZenSettings.scaledChRowH(): androidx.compose.ui.unit.Dp = BaseChRowH * epgSize.coerceAtLeast(0.5f)
private fun ZenSettings.scaledGridHeaderH(): androidx.compose.ui.unit.Dp = BaseGridHeaderH * epgSize.coerceAtLeast(0.5f)
private fun ZenSettings.scaledTopBarH(): androidx.compose.ui.unit.Dp = BaseTopBarH * epgSize.coerceAtLeast(0.5f)

@Composable
fun EpgScreen(container: AppContainer, onExit: () -> Unit, onOpenPlayer: (Channel) -> Unit) {
    val vm: EpgViewModel = viewModel(factory = AppViewModelFactory(container))
    val settings by container.settings.settingsFlow.collectAsStateWithLifecycle(initialValue = ZenSettings())
    val colors = LocalZenColors.current
    val clock = remember { mutableStateOf(nowMin()) }

    DisposableEffect(onOpenPlayer) {
        vm.onOpenPlayer = { vi -> buildEpgChannel(vi)?.let(onOpenPlayer) }
        onDispose { vm.onOpenPlayer = null }
    }

    LaunchedEffect(Unit) {
        while (true) {
            vm.tick()
            clock.value = nowMin()
            delay(60_000)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { ev -> handleEpgKey(ev, vm, onExit) }
    ) {
        Column(Modifier.fillMaxSize()) {
            EpgTopBar(vm, settings, clock.value, colors)
            EpgBody(vm, settings, clock.value, colors)
        }
        if (vm.previewOn && settings.epgPipp) {
            EpgPip(vm, colors, Modifier.align(Alignment.BottomEnd).padding(18.dp))
        }
        if (vm.toastText != null) {
            EpgToast(vm.toastText!!, colors, Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 26.dp), vm::clearToast)
        }
        if (vm.ctxOpen) {
            EpgContextOverlay(vm, colors, Modifier.align(Alignment.Center))
        }
    }
}

private fun handleEpgKey(ev: KeyEvent, vm: EpgViewModel, onExit: () -> Unit): Boolean {
    if (ev.type != KeyEventType.KeyDown && ev.type != KeyEventType.KeyUp) return false
    if (vm.ctxOpen && ev.type == KeyEventType.KeyDown) {
        return when (ev.key) {
            Key.DirectionUp, Key.DirectionLeft -> { vm.ctxMove(EpgMoveDir.UP); true }
            Key.DirectionDown, Key.DirectionRight -> { vm.ctxMove(EpgMoveDir.DOWN); true }
            Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                val vi = vm.currentVis().getOrNull(vm.row) ?: vm.currentChannel()
                vm.ctxItems(vi).getOrNull(vm.ctxIndex)?.let { vm.ctxAct(it.action) }
                true
            }
            Key.Escape, Key.Back, Key.Backspace -> { vm.hideCtx(); true }
            else -> false
        }
    }
    if (ev.type == KeyEventType.KeyDown) {
        return when (ev.key) {
            Key.DirectionUp -> { vm.move(EpgMoveDir.UP); true }
            Key.DirectionDown -> { vm.move(EpgMoveDir.DOWN); true }
            Key.DirectionLeft -> { vm.move(EpgMoveDir.LEFT); true }
            Key.DirectionRight -> { vm.move(EpgMoveDir.RIGHT); true }
            Key.Enter, Key.NumPadEnter -> { vm.enterPress(); true }
            Key.Spacebar -> { vm.enterPress(); true }
            Key.Escape, Key.Back, Key.Backspace -> { vm.backKey(onExit); true }
            Key.PageUp -> { vm.setDayStep(-1); true }
            Key.PageDown -> { vm.setDayStep(1); true }
            Key.C -> { vm.openCtx(); true }
            else -> false
        }
    } else if (ev.type == KeyEventType.KeyUp) {
        return when (ev.key) {
            Key.Enter, Key.NumPadEnter, Key.Spacebar -> { vm.enterRelease(); true }
            else -> false
        }
    }
    return false
}

private fun buildEpgChannel(vi: Int): Channel? {
    val ch = EPG_CHANNELS.getOrNull(vi) ?: return null
    return Channel(
        id = "epg_$vi",
        providerId = -1L,
        mediaType = MediaType.LIVE,
        name = ch.name,
        url = "",
        logoUrl = null,
        category = null,
        number = ch.num
    )
}

@Composable
private fun EpgTopBar(vm: EpgViewModel, settings: ZenSettings, now: Int, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val card = colors.surface.copy(alpha = 0.42f)
    val line = colors.onSurface.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(settings.scaledTopBarH())
            .background(card)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            Text("TV-PROGRAMM", fontSize = 14.sp, fontWeight = FontWeight.Black, color = colors.onSurface, letterSpacing = 0.4.sp)
            Text(dayLabel(vm.day()).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface.copy(alpha = 0.45f), letterSpacing = 1.5.sp)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                ClockIcon(Modifier.size(11.dp), colors.accentStart)
                Text(hh(now), fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = colors.accentStart, letterSpacing = 0.5.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            EpgZbtn("◀", colors) { vm.setDayStep(-1) }
            Text(dayLabel(vm.day()), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface.copy(alpha = 0.55f), letterSpacing = 0.5.sp, modifier = Modifier.width(76.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            EpgZbtn("▶", colors) { vm.setDayStep(1) }
        }
        Spacer(Modifier.width(20.dp))
        Text("Pause = jetzt · ◄ ► = Tag", fontSize = 9.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.45f), letterSpacing = 1.6.sp)
    }
}

@Composable
private fun ClockIcon(modifier: Modifier = Modifier, tint: Color) {
    Box(modifier = modifier.drawBehind {
        drawCircle(tint, radius = size.minDimension / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        drawLine(tint, start = Offset(size.width / 2, size.height / 4), end = Offset(size.width / 2, size.height / 2), strokeWidth = 2f)
        drawLine(tint, start = Offset(size.width / 2, size.height / 2), end = Offset(size.width * 0.7f, size.height * 0.6f), strokeWidth = 2f)
    })
}

@Composable
private fun EpgZbtn(text: String, colors: com.zenplayer.app.ui.theme.ZenColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
    }
}

@Composable
private fun EpgBody(vm: EpgViewModel, settings: ZenSettings, now: Int, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val vis = vm.currentVis()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        ChannelColumn(vm, vis, now, vScroll, settings, colors, Modifier.width(settings.scaledChColW()).fillMaxHeight())
        Spacer(Modifier.width(14.dp))
        ProgramGrid(vm, vis, now, vScroll, hScroll, settings, colors, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun ChannelColumn(vm: EpgViewModel, vis: List<Int>, now: Int, vScroll: androidx.compose.foundation.ScrollState, settings: ZenSettings, colors: com.zenplayer.app.ui.theme.ZenColors, modifier: Modifier) {
    val card = colors.surface.copy(alpha = 0.42f)
    val line = colors.onSurface.copy(alpha = 0.12f)
    val focusEffect = LocalFocusEffect.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(settings.scaledGridHeaderH())
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("SENDER", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.55f), letterSpacing = 1.5.sp)
            Text(vis.size.toString(), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.55f))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
        ) {
            vis.forEachIndexed { idx, vi ->
                val selected = idx == vm.row
                val ch = EPG_CHANNELS.getOrNull(vi) ?: return@forEachIndexed
                val prog = nowProg(vi, vm.day(), now)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(settings.scaledChRowH())
                        .clip(RoundedCornerShape(13.dp))
                        .then(
                            if (selected) Modifier.background(colors.accentBrush)
                            else Modifier.background(Color.Transparent)
                        )
                        .border(1.dp, if (selected) Color.Transparent else if (vi == vm.currentChannel()) colors.accentEnd else Color.Transparent, RoundedCornerShape(13.dp))
                        .zenFocusEffect(selected, focusEffect, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    if (settings.epgLogos) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Brush.horizontalGradient(channelArt(vi))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ch.code, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (selected) colors.background else Color.White)
                        }
                        Spacer(Modifier.width(0.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(ch.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) colors.background else colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(prog?.t ?: "–", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = if (selected) colors.background.copy(alpha = 0.72f) else colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(ch.num.toString(), fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = if (selected) colors.background.copy(alpha = 0.72f) else colors.onSurface.copy(alpha = 0.45f))
                    if (vm.isFav(vi)) {
                        Text("♥", fontSize = 10.sp, color = if (selected) colors.background else Color(0xFFFFD23F))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramGrid(vm: EpgViewModel, vis: List<Int>, now: Int, vScroll: androidx.compose.foundation.ScrollState, hScroll: androidx.compose.foundation.ScrollState, settings: ZenSettings, colors: com.zenplayer.app.ui.theme.ZenColors, modifier: Modifier) {
    val timeColW = settings.scaledTimeColW()
    val totalW = timeColW * EPG_HOURS
    val nowLineX = remember(now, timeColW) { ((now - EPG_START).coerceAtLeast(0).toFloat() / 60f * timeColW.value).dp }
    val card = colors.surface.copy(alpha = 0.42f)
    val line = colors.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
        ) {
            TimeHeader(totalW, now, settings, colors)
            vis.forEachIndexed { rowIdx, vi ->
                ProgramRow(vm, vi, rowIdx, totalW, now, settings, colors)
            }
        }
        Box(
            modifier = Modifier
                .padding(top = settings.scaledGridHeaderH())
                .width(2.dp)
                .offset(x = nowLineX)
                .fillMaxHeight()
                .drawBehind {
                    drawCircle(colors.accentBrush, radius = 5f, center = Offset(0f, -7f))
                    drawLine(Brush.verticalGradient(listOf(colors.accentStart, colors.accentEnd)), start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2f)
                }
        )
    }
}

@Composable
private fun TimeHeader(totalW: Dp, now: Int, settings: ZenSettings, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val timeColW = settings.scaledTimeColW()
    val line = colors.onSurface.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .width(totalW)
            .height(settings.scaledGridHeaderH())
            .background(colors.background.copy(alpha = 0.82f))
    ) {
        for (h in 0 until EPG_HOURS) {
            val hour = (h + 5) % 24
            val cur = hour == now / 60
            Box(
                modifier = Modifier
                    .width(timeColW)
                    .fillMaxHeight()
                    .border(1.dp, line)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("${pad2(hour)}:00", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (cur) colors.accentStart else colors.onSurface.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun ProgramRow(vm: EpgViewModel, vi: Int, rowIdx: Int, totalW: Dp, now: Int, settings: ZenSettings, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val progs = epgProgramsFor(vi, vm.day())
    val selected = rowIdx == vm.row
    val timeColW = settings.scaledTimeColW()
    val chRowH = settings.scaledChRowH()
    val focusEffect = LocalFocusEffect.current
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .width(totalW)
            .height(chRowH + 5.dp)
            .drawBehind {
                if (rowIdx < vm.currentVis().size - 1) {
                    drawLine(line, start = Offset(0f, size.height - 1.dp.toPx()), end = Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                }
            }
    ) {
        progs.forEachIndexed { idx, prog ->
            val left = ((prog.s - EPG_START).toFloat() / 60f * timeColW.value).dp
            val w = ((prog.e - prog.s).toFloat() / 60f * timeColW.value).dp - 4.dp
            val isNow = prog.s <= now && prog.e > now
            val focused = selected && idx == vm.ecol
            val catName = programCategoryName(prog.c)
            val pgNow = colors.accentStart.copy(alpha = 0.16f)
            Box(
                modifier = Modifier
                    .offset(x = left, y = 5.dp)
                    .width(w.coerceAtLeast(4.dp))
                    .height(chRowH - 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isNow) Brush.linearGradient(listOf(pgNow, Color.White.copy(alpha = 0.05f)))
                        else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
                    )
                    .border(1.dp, if (isNow) colors.accentEnd else Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                    .then(
                        if (focused) Modifier.border(2.dp, colors.accentStart, RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                        else Modifier
                    )
                    .zenFocusEffect(focused, focusEffect, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    Text("${hh(prog.s)}${if (w > 124.dp) " – ${hh(prog.e)}" else ""}", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.45f), maxLines = 1)
                    Text(prog.t, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (w > 170.dp) {
                        Text(catName, fontSize = 9.5.sp, color = colors.onSurface.copy(alpha = 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (isNow) {
                    NowProgressBar(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp), colors)
                }
            }
        }
    }
}

@Composable
private fun NowProgressBar(modifier: Modifier, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val transition = rememberInfiniteTransition(label = "pg")
    val alpha by transition.animateFloat(0.75f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse))
    Box(
        modifier = modifier.background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.accentStart.copy(alpha = alpha), colors.accentEnd.copy(alpha = alpha)))))
    }
}

private fun programCategoryName(c: String): String = when (c) {
    "news" -> "Info"
    "doku" -> "Doku"
    "serie" -> "Serie"
    "show" -> "Show"
    "sport" -> "Sport"
    "film" -> "Film"
    "kids" -> "Kinder"
    else -> ""
}

@Composable
private fun EpgPip(vm: EpgViewModel, colors: com.zenplayer.app.ui.theme.ZenColors, modifier: Modifier) {
    val vi = vm.currentVis().getOrNull(vm.row) ?: vm.currentChannel()
    val ch = EPG_CHANNELS.getOrNull(vi) ?: return
    val prog = epgProgramsFor(vi, vm.day()).getOrNull(vm.ecol) ?: nowProg(vi, vm.day(), vm.nowMin)
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = modifier
            .width(320.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, line, RoundedCornerShape(14.dp))
            .background(colors.background)
    ) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(channelArt(vi).map { it.copy(alpha = 0.35f) })))
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(colors.accentStart)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("VORSCHAU", fontSize = 8.sp, fontWeight = FontWeight.Black, color = colors.background)
        }
        Text(
            ch.code,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.3f)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                .padding(12.dp)
        ) {
            Text(ch.name, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(prog?.t ?: "–", fontSize = 9.5.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EpgToast(text: String, colors: com.zenplayer.app.ui.theme.ZenColors, modifier: Modifier, onDismiss: () -> Unit) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    LaunchedEffect(text) {
        delay(2000)
        onDismiss()
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.94f))
            .border(1.dp, line, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
    }
}

@Composable
private fun EpgContextOverlay(vm: EpgViewModel, colors: com.zenplayer.app.ui.theme.ZenColors, modifier: Modifier) {
    val vi = vm.currentVis().getOrNull(vm.row) ?: vm.currentChannel()
    val ch = EPG_CHANNELS.getOrNull(vi) ?: return
    val items = vm.ctxItems(vi)
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { vm.hideCtx() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier
                .width(360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface.copy(alpha = 0.97f))
                .border(1.dp, line, RoundedCornerShape(18.dp))
                .padding(10.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp).border(1.dp, line, RoundedCornerShape(0.dp))) {
                Text(ch.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
                Text("Kanal ${ch.num}", fontSize = 10.5.sp, color = colors.onSurface.copy(alpha = 0.55f))
            }
            items.forEachIndexed { idx, item ->
                val selected = idx == vm.ctxIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (selected) Modifier.background(Color.White.copy(alpha = 0.08f)) else Modifier)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.action) {
                            "play" -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.accentStart, modifier = Modifier.size(18.dp))
                            "fav" -> Text("♥", fontSize = 14.sp, color = colors.accentStart)
                            else -> Icon(Icons.Filled.Settings, contentDescription = null, tint = colors.accentStart, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
                    Text(item.key, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (selected) colors.accentStart else colors.onSurface.copy(alpha = 0.45f), letterSpacing = 1.sp)
                }
            }
        }
    }
}
