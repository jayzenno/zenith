package com.zenplayer.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.ui.theme.LocalZenColors

private val On = Color(0xFFF2F4F8)
private val Mut = Color(0xFF9FA3AB)
private val Line = Color(0xFF1E1E2A)
private val AccentA = Color(0xFF35CFB2)

@Composable
fun LiveChannelCard(channel: Channel, onClick: () -> Unit) {
    val colors = LocalZenColors.current
    val progress = rememberProgress(channel.id.hashCode())
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A12).copy(alpha = 0.42f))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(colors.accentStart.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = colors.accentStart, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = On, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(channel.category ?: "Live", fontSize = 11.sp, color = Mut, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                ProgressBar(progress, colors.accentStart, colors.accentEnd)
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(AccentA).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("HD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF04100C))
            }
        }
    }
}

@Composable
fun PosterChannelCard(channel: Channel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(214.dp)
            .height(304.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A12).copy(alpha = 0.42f))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = channel.logoUrl ?: channel.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                .padding(16.dp)
        ) {
            Text(channel.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, start: Color, end: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(start, end)))
        )
    }
}

@Composable
private fun rememberProgress(seed: Int): Float {
    return androidx.compose.runtime.remember(seed) { ((seed % 100).coerceAtLeast(0) / 100f).coerceIn(0.05f, 0.95f) }
}
