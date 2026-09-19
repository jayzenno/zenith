package com.zenplayer.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zenplayer.app.data.model.EpgProgram
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.player.PlaybackProgress
import com.zenplayer.app.player.PlaybackStatus
import com.zenplayer.app.player.ZenPlayerSession
import com.zenplayer.app.ui.components.ZenShapes
import com.zenplayer.app.ui.theme.LocalZenColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CONTROL_PREV = 0
private const val CONTROL_PLAY = 1
private const val CONTROL_NEXT = 2
private const val SEEK_STEP_MS = 10_000L
private const val AUTO_HIDE_PLAYING_MS = 5_000L

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
    val settings by container.settings.settingsFlow.collectAsStateWithLifecycle(initialValue = ZenSettings())

    val engineChoice = settings.playerEngine
    // The session owns both engines and the fallback decision. Recreated only when the user
    // changes the preferred engine or the user agent.
    val session = remember(engineChoice, settings.userAgent) {
        ZenPlayerSession(context, engineChoice, settings.userAgent)
    }
    DisposableEffect(session) {
        onDispose { session.release() }
    }

    // Pause playback when the Activity stops (HOME on TV, input switch, screen off).
    // Without this the engine would keep playing audio/video in the background
    // indefinitely. Deliberately no auto-resume: the user resumes manually (OK).
    // ON_STOP (not ON_PAUSE): on TV there is no multi-window/PIP, and ON_STOP is the
    // exact boundary where the app is no longer visible. Both ExoPlayer and VLC are
    // safe to pause while idle/loading.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) session.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val status by session.status.collectAsStateWithLifecycle()
    val activeEngine by session.engine.collectAsStateWithLifecycle()
    val progress by session.progress.collectAsStateWithLifecycle()
    val colors = LocalZenColors.current

    val active = state as? PlayerUi.Active
    val channel = active?.channel

    LaunchedEffect(channel?.id, session) {
        val current = channel ?: return@LaunchedEffect
        session.play(current.url, current.name, null)
    }

    val upcomingFlow = remember(channel?.id) { vm.upcoming(channel?.id.orEmpty()) }
    val upcoming by upcomingFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val nowTick by produceState(initialValue = System.currentTimeMillis(), key1 = channel?.id) {
        while (true) {
            value = System.currentTimeMillis()
            delay(20_000L)
        }
    }

    val isLive = mediaType == MediaType.LIVE
    val isLoading = status == PlaybackStatus.Loading
    val isBuffering = status == PlaybackStatus.Buffering
    val isPlaying = status == PlaybackStatus.Playing
    val isEnded = status == PlaybackStatus.Ended
    val error = status as? PlaybackStatus.Error
    val timedOut = status == PlaybackStatus.Timeout
    val canSeek = !isLive && progress.durationMs > 0L

    var controlsVisible by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(CONTROL_PLAY) }
    var interaction by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Elegant auto-hide while playing; stays open when paused/finished so the state is readable.
    LaunchedEffect(controlsVisible, interaction, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(AUTO_HIDE_PLAYING_MS)
            controlsVisible = false
        }
    }

    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(channel?.id) {
        bannerVisible = channel != null
        if (channel != null) {
            delay(3_600L)
            bannerVisible = false
        }
    }

    val clock by produceState(initialValue = formatClock(), key1 = controlsVisible) {
        if (controlsVisible) {
            while (true) {
                value = formatClock()
                delay(15_000L)
            }
        }
    }

    val currentProgram = upcoming.firstOrNull { it.startTs <= nowTick && it.endTs > nowTick }
    val nextProgram = upcoming.firstOrNull { it.startTs > nowTick }
    val epgFraction = currentProgram?.let {
        val span = (it.endTs - it.startTs).toFloat()
        if (span <= 0f) 0f else ((nowTick - it.startTs).toFloat() / span).coerceIn(0f, 1f)
    } ?: 0f

    fun reveal() {
        controlsVisible = true
        interaction++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val needsRestart = error != null || timedOut || isEnded
                when (event.key) {
                    Key.DirectionLeft -> {
                        reveal()
                        if (canSeek) session.seekBy(-SEEK_STEP_MS) else selected = (selected + 2) % 3
                        true
                    }

                    Key.DirectionRight -> {
                        reveal()
                        if (canSeek) session.seekBy(SEEK_STEP_MS) else selected = (selected + 1) % 3
                        true
                    }

                    Key.DirectionUp -> {
                        reveal()
                        if (isLive) vm.zap(-1) else selected = (selected + 2) % 3
                        true
                    }

                    Key.DirectionDown -> {
                        reveal()
                        if (isLive) vm.zap(1) else selected = (selected + 1) % 3
                        true
                    }

                    Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                        reveal()
                        when {
                            needsRestart -> session.retry()
                            selected == CONTROL_PREV -> if (isLive) vm.zap(-1) else session.seekBy(-SEEK_STEP_MS)
                            selected == CONTROL_NEXT -> if (isLive) vm.zap(1) else session.seekBy(SEEK_STEP_MS)
                            else -> session.toggle()
                        }
                        true
                    }

                    Key.MediaPlayPause -> {
                        reveal()
                        session.toggle()
                        true
                    }

                    Key.MediaPlay -> {
                        reveal()
                        session.resume()
                        true
                    }

                    Key.MediaPause -> {
                        reveal()
                        session.pause()
                        true
                    }

                    Key.MediaNext -> {
                        reveal()
                        if (isLive) vm.zap(1)
                        true
                    }

                    Key.MediaPrevious -> {
                        reveal()
                        if (isLive) vm.zap(-1)
                        true
                    }

                    Key.MediaRewind -> {
                        reveal()
                        if (canSeek) session.seekBy(-SEEK_STEP_MS)
                        true
                    }

                    Key.MediaFastForward -> {
                        reveal()
                        if (canSeek) session.seekBy(SEEK_STEP_MS)
                        true
                    }

                    Key.Back -> {
                        if (controlsVisible) {
                            controlsVisible = false
                            true
                        } else {
                            onBack()
                            true
                        }
                    }

                    else -> false
                }
            }
    ) {
        // Video surface for the currently active engine. Keyed so an automatic fallback
        // swaps the host view deterministically.
        key(activeEngine) {
            AndroidView(
                factory = { session.videoView(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Minimal, non-blocking status indicator. Never shown while video plays.
        when {
            error != null -> StatusCard(
                modifier = Modifier.align(Alignment.Center),
                title = "Wiedergabe fehlgeschlagen",
                message = error.message?.let { "Code: $it" } ?: "Der Stream konnte nicht geöffnet werden.",
                hint = "OK zum erneuten Versuch",
                accent = colors.accentStart
            )

            timedOut -> StatusCard(
                modifier = Modifier.align(Alignment.Center),
                title = "Kein Signal",
                message = "Der Stream antwortet nicht.",
                hint = "OK zum erneuten Versuch",
                accent = colors.accentStart
            )

            isEnded -> StatusCard(
                modifier = Modifier.align(Alignment.Center),
                title = "Wiedergabe beendet",
                message = channel?.name ?: "",
                hint = "OK zum erneuten Abspielen",
                accent = colors.accentStart
            )

            isLoading || isBuffering -> LoadingPill(
                modifier = Modifier.align(Alignment.Center),
                label = if (isBuffering) "Puffert…" else "Lädt…",
                accent = colors.accentStart
            )
        }

        // Transient channel banner shown on tune-in and zapping.
        AnimatedVisibility(
            visible = bannerVisible && !controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(450)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            ChannelBanner(
                name = channel?.name ?: mediaType.name,
                subtitle = channel?.category ?: mediaType.name,
                logoUrl = channel?.logoUrl,
                isLive = isLive
            )
        }

        // One-time control hint that fades with the tune-in banner.
        AnimatedVisibility(
            visible = bannerVisible && !controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(450)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                Modifier
                    .padding(bottom = 44.dp)
                    .clip(ZenShapes.pill)
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), ZenShapes.pill)
                    .padding(horizontal = 22.dp, vertical = 11.dp)
            ) {
                Text(
                    text = if (isLive) "OK Steuerung  ·  ▲ ▼ Sender  ·  Back zurück"
                    else "OK Steuerung  ·  ◀ ▶ Spulen  ·  Back zurück",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }

        // TV overlay: top + bottom bars, hidden by default.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { -it / 3 },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                title = channel?.name ?: mediaType.name,
                subtitle = channel?.category ?: mediaType.name,
                logoUrl = channel?.logoUrl,
                isLive = isLive,
                engineLabel = activeEngine.label,
                positionLabel = active?.let { "${it.index + 1}/${it.total}" } ?: "-",
                clock = clock,
                accent = colors.accentStart,
                onBack = { controlsVisible = false }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 3 },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerBottomBar(
                isLive = isLive,
                isPlaying = isPlaying,
                selected = selected,
                accent = colors.accentStart,
                onAccent = colors.onPrimary,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                epgFraction = epgFraction,
                progress = progress,
                onPrev = {
                    selected = CONTROL_PREV
                    interaction++
                    if (isLive) vm.zap(-1) else session.seekBy(-SEEK_STEP_MS)
                },
                onNext = {
                    selected = CONTROL_NEXT
                    interaction++
                    if (isLive) vm.zap(1) else session.seekBy(SEEK_STEP_MS)
                },
                onToggle = {
                    selected = CONTROL_PLAY
                    interaction++
                    session.toggle()
                }
            )
        }
    }
}

@Composable
private fun ChannelLogo(logoUrl: String?, accent: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(ZenShapes.chip)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), ZenShapes.chip),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text("Z", fontSize = 18.sp, fontWeight = FontWeight.Black, color = accent)
        }
    }
}

@Composable
private fun ChannelBanner(
    name: String,
    subtitle: String,
    logoUrl: String?,
    isLive: Boolean
) {
    val colors = LocalZenColors.current
    Row(
        modifier = Modifier
            .padding(start = 44.dp, top = 40.dp)
            .clip(ZenShapes.card)
            .background(Color(0xFF0A0A12).copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), ZenShapes.card)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelLogo(logoUrl = logoUrl, accent = colors.accentStart, size = 52.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) {
                    LiveChip()
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveChip() {
    Box(
        modifier = Modifier
            .clip(ZenShapes.pill)
            .background(Color(0xFFE23A4A))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text("LIVE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    subtitle: String,
    logoUrl: String?,
    isLive: Boolean,
    engineLabel: String,
    positionLabel: String,
    clock: String,
    accent: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.82f),
                        Color.Black.copy(alpha = 0.38f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Zurück",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.width(20.dp))
        ChannelLogo(logoUrl = logoUrl, accent = accent, size = 58.dp)
        Spacer(Modifier.width(20.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) {
                    LiveChip()
                    Spacer(Modifier.width(14.dp))
                }
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(24.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(clock, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.92f))
            Text(
                text = "$engineLabel · $positionLabel",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.52f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun PlayerBottomBar(
    isLive: Boolean,
    isPlaying: Boolean,
    selected: Int,
    accent: Color,
    onAccent: Color,
    currentProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    epgFraction: Float,
    progress: PlaybackProgress,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.48f),
                        Color.Black.copy(alpha = 0.90f)
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLive) {
            if (currentProgram != null || nextProgram != null) {
                LiveProgramInfo(currentProgram, nextProgram, epgFraction, accent)
                Spacer(Modifier.height(20.dp))
            }
        } else {
            VodProgress(progress, accent)
            Spacer(Modifier.height(20.dp))
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlPill(
                label = if (isLive) "Zap −" else "−10 s",
                selected = selected == CONTROL_PREV,
                accent = accent,
                onAccent = onAccent,
                onClick = onPrev
            )
            Spacer(Modifier.width(22.dp))
            PlayPauseButton(
                playing = isPlaying,
                selected = selected == CONTROL_PLAY,
                accent = accent,
                onAccent = onAccent,
                onClick = onToggle
            )
            Spacer(Modifier.width(22.dp))
            ControlPill(
                label = if (isLive) "Zap +" else "+10 s",
                selected = selected == CONTROL_NEXT,
                accent = accent,
                onAccent = onAccent,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun LiveProgramInfo(
    current: EpgProgram?,
    next: EpgProgram?,
    fraction: Float,
    accent: Color
) {
    Column(
        modifier = Modifier
            .clip(ZenShapes.chip)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), ZenShapes.chip)
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (current != null) {
                Column(Modifier.weight(1f)) {
                    Text("JETZT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accent, letterSpacing = 1.6.sp)
                    Text(
                        current.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatClock(current.startTs)} – ${formatClock(current.endTs)}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
            if (next != null) {
                if (current != null) {
                    Box(
                        Modifier
                            .padding(horizontal = 26.dp)
                            .width(1.dp)
                            .height(42.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("DANACH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.6.sp)
                    Text(
                        next.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatClock(next.startTs),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
        if (current != null) {
            Spacer(Modifier.height(12.dp))
            ProgressBar(fraction = fraction, accent = accent)
        }
    }
}

@Composable
private fun VodProgress(progress: PlaybackProgress, accent: Color) {
    val duration = progress.durationMs
    val fraction = if (duration > 0L) (progress.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(
        modifier = Modifier
            .clip(ZenShapes.chip)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), ZenShapes.chip)
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatDuration(progress.positionMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f)) { ProgressBar(fraction = fraction, accent = accent, track = true) }
            Spacer(Modifier.width(16.dp))
            Text(formatDuration(duration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, accent: Color, track: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (track) 5.dp else 4.dp)
            .clip(ZenShapes.pill)
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(if (track) 5.dp else 4.dp)
                .clip(ZenShapes.pill)
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.65f))))
        )
    }
}

@Composable
private fun ControlPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = tween(180),
        label = "pillScale"
    )
    val background: Brush = if (selected) Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f)))
    else SolidColor(Color.White.copy(alpha = 0.09f))

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(ZenShapes.pill)
            .background(background)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
                shape = ZenShapes.pill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 34.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) onAccent else Color.White
        )
    }
}

@Composable
private fun PlayPauseButton(
    playing: Boolean,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(180),
        label = "playScale"
    )
    val background: Brush = if (selected) Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f)))
    else SolidColor(Color.White.copy(alpha = 0.14f))
    val glyph = if (selected) onAccent else Color.White

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(92.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.22f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        PlayPauseGlyph(playing = playing, color = glyph, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun PlayPauseGlyph(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (playing) {
            val barWidth = w * 0.26f
            val gap = w * 0.20f
            val total = barWidth * 2f + gap
            val startX = (w - total) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(startX, 0f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth * 0.36f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(startX + barWidth + gap, 0f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth * 0.36f)
            )
        } else {
            val path = Path().apply {
                moveTo(w * 0.22f, 0f)
                lineTo(w, h / 2f)
                lineTo(w * 0.22f, h)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
private fun LoadingPill(
    modifier: Modifier,
    label: String,
    accent: Color
) {
    Row(
        modifier = modifier
            .clip(ZenShapes.pill)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), ZenShapes.pill)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = accent,
            strokeWidth = 2.5.dp
        )
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier,
    title: String,
    message: String,
    hint: String,
    accent: Color
) {
    Column(
        modifier = modifier
            .clip(ZenShapes.card)
            .background(Color(0xFF0A0A12).copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), ZenShapes.card)
            .padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = Color.White.copy(alpha = 0.62f))
        }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(ZenShapes.pill)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.5f), ZenShapes.pill)
                .padding(horizontal = 20.dp, vertical = 9.dp)
        ) {
            Text(hint, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

private fun formatClock(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun formatClock(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
