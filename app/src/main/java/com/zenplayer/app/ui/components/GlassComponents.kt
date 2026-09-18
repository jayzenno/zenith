package com.zenplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenplayer.app.ui.theme.LocalZenColors

@Composable
fun <T : Any> GridRow(
    title: String?,
    items: List<T>,
    modifier: Modifier = Modifier,
    key: (T) -> Any = { it },
    itemContent: @Composable (T) -> Unit
) {
    Column(modifier) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = LocalZenColors.current.onSurface.copy(alpha = 0.72f),
                    letterSpacing = 2.sp
                ),
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 140.dp)
        ) {
            items(items, key = key) { itemContent(it) }
        }
    }
}

@Composable
fun GlassTile(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalZenColors.current
    ZenCard(
        onClick = onClick,
        modifier = modifier.width(320.dp).height(226.dp),
        shape = ZenShapes.tile,
        cornerRadius = 22.dp
    ) { focused ->
        Column(
            modifier = Modifier.fillMaxSize().padding(26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(if (focused) colors.accentBrush else SolidColor(colors.surfaceHigh.copy(alpha = 0.55f))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (focused) colors.onPrimary else colors.accentStart,
                    modifier = Modifier.size(42.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (focused) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}