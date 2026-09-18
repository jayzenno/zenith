package com.zenplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenplayer.app.data.model.EpgProgram
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.settings.PlayerEngineChoice
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.player.ExoPlayerController
import com.zenplayer.app.player.PlaybackStatus
import com.zenplayer.app.player.VlcPlayerController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AccentA = Color(0xFF35CFB2)
private val AccentB = Color(0xFF3D7BFF)
private val Panel = Color(0xFF0A0A12).copy(alpha = 0.42f)
private val Line = Color(0xFF1E1E2A)

@Composable
fun PlayerScreen(
    container: AppContainer,
    providerId: Long,
    mediaType: MediaType,
    channelId: String,
    onBack: () -> Unit
) {
    val vm: PlayerViewModel = viewModel(
        key = "player-$providerId-$mediaType-$channelId",
        initializer = { PlayerViewModel(container, providerId, mediaType, channelId) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val engine by container.settings.settingsFlow.collectAsStateWithLifecycle(initialValue = ZenSettings())
    val controller = remember(engine) {
        if (engine.playerEngine == PlayerEngineChoice.EXO) ExoPlayerController(context) else VlcPlayerController(context)
    }
    val status by controller.status.collectAsStateWithLifecycle()
    val active = state as? PlayerUi.Active
    val channel = active?.channel

    val loadingTimeout = remember { mutableStateOf(false) }

    LaunchedEffect(channel?.id) {
        loadingTimeout.value = false
        channel?.let { controller.play(it.url, it.name, null) }
    }

    LaunchedEffect(status) {
        if (status == PlaybackStatus.Loading) {
            loadingTimeout.value = false
            delay(15000L)
            if (status == PlaybackStatus.Loading) {
                loadingTimeout.value = true
            }
        } else {
            loadingTimeout.value = false
        }
    }

    DisposableEffect(engine.playerEngine) {
        onDispose { controller.release() }
    }

    val upcoming by vm.upcoming(channel?.id.orEmpty()).collectAsStateWithLifecycle(initialValue = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { vm.zap(-1); true }
                    Key.DirectionRight -> { vm.zap(1); true }
                    Key.Enter -> { channel?.let { controller.toggle() }; true }
                    Key.Back -> { onBack(); true }
                    else -> false
                }
            }
    ) {
        when (controller) {
            is ExoPlayerController -> AndroidView(factory = { controller.playerView(it) }, modifier = Modifier.fillMaxSize())
            is VlcPlayerController -> AndroidView(factory = { controller.videoLayout(it) }, modifier = Modifier.fillMaxSize())
        }

        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(140.dp).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))
            )
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(channel?.name ?: "", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(channel?.category ?: mediaType.name, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Panel).border(1.dp, Line, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(engine.playerEngine.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentA)
                        Text(if (active != null) "${active.index + 1}/${active.total}" else "-", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        val loading = status == PlaybackStatus.Loading || status == PlaybackStatus.Idle
        if (loading || status is PlaybackStatus.Error || loadingTimeout.value) {
            Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Panel).border(1.dp, Line, RoundedCornerShape(50)).padding(horizontal = 28.dp, vertical = 18.dp)) {
                    when {
                        status is PlaybackStatus.Error -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Fehler beim Abspielen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (status.message != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(status.message, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                        loadingTimeout.value -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(34.dp), color = AccentA, strokeWidth = 3.5.dp)
                                Spacer(Modifier.width(18.dp))
                                Column {
                                    Text("Lädt zu lange...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Drücke OK zum Retry", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(34.dp), color = AccentA, strokeWidth = 3.5.dp)
                                Spacer(Modifier.width(18.dp))
                                Text("Lädt...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
            )
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 26.dp)) {
                NowNextBar(upcoming)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    PlayerBtn("Vorheriger") { vm.zap(-1) }
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brush.linearGradient(listOf(AccentA, AccentB))).clickable { channel?.let { controller.toggle() } }.padding(horizontal = 28.dp, vertical = 14.dp)
                    ) { Text("Pause / Play", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF04100C)) }
                    Spacer(Modifier.width(14.dp))
                    PlayerBtn("Naechster") { vm.zap(1) }
                }
            }
        }
    }
}

@Composable
private fun PlayerBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Panel).border(1.dp, Line, RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp)
    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
}

@Composable
private fun NowNextBar(upcoming: List<EpgProgram>) {
    val now = System.currentTimeMillis()
    val current = upcoming.firstOrNull { it.startTs <= now && it.endTs > now }
    val next = upcoming.firstOrNull { it.startTs > now }
    if (current == null && next == null) return
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (current != null) {
                Column(Modifier.weight(1f)) {
                    Text("Jetzt", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.sp)
                    Text(current.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatTime(current.startTs)} - ${formatTime(current.endTs)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
            if (next != null) {
                Column(Modifier.weight(1f)) {
                    Text("Als naechstes", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.sp)
                    Text(next.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTime(next.startTs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
