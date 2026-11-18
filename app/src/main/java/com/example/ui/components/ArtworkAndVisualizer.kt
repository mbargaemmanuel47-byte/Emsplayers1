package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Track
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoyalBlueAccent

fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val mins = s / 60
    val secs = s % 60
    return "%d:%02d".format(mins, secs)
}

@Composable
fun TrackArtworkCanvas(
    track: Track,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    val primary = Color(track.primaryColorHex)
    val secondary = Color(track.secondaryColorHex)
    val tertiary = Color(track.tertiaryColorHex)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Midnight)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height

                    val bgBrush = Brush.linearGradient(
                        colors = listOf(secondary, tertiary, primary.copy(alpha = 0.75f)),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    )
                    val orbCenter = Offset(w * 0.35f, h * 0.30f)
                    val orbRadius = (w * 0.42f).coerceAtLeast(1f)
                    val orbBrush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.78f), Color.Transparent),
                        center = orbCenter,
                        radius = orbRadius
                    )
                    val ringStrokeDashed = Stroke(
                        width = (w * 0.009f).coerceAtLeast(1.2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                    val ringStrokeThin = Stroke(width = (w * 0.006f).coerceAtLeast(1f))
                    val wave1 = Path().apply {
                        moveTo(w * 0.26f, h * 0.50f)
                        quadraticTo(w * 0.38f, h * 0.32f, w * 0.50f, h * 0.50f)
                        quadraticTo(w * 0.62f, h * 0.68f, w * 0.74f, h * 0.50f)
                    }
                    val wave2 = Path().apply {
                        moveTo(w * 0.32f, h * 0.56f)
                        quadraticTo(w * 0.42f, h * 0.42f, w * 0.50f, h * 0.56f)
                        quadraticTo(w * 0.58f, h * 0.70f, w * 0.68f, h * 0.56f)
                    }
                    val stroke1 = Stroke(width = (w * 0.020f).coerceAtLeast(2f), cap = StrokeCap.Round)
                    val stroke2 = Stroke(width = (w * 0.011f).coerceAtLeast(1.2f), cap = StrokeCap.Round)

                    onDrawBehind {
                        drawRect(brush = bgBrush)
                        drawCircle(brush = orbBrush, center = orbCenter, radius = orbRadius)
                        drawCircle(
                            color = primary.copy(alpha = 0.45f),
                            radius = w * 0.27f,
                            center = Offset(w * 0.5f, h * 0.5f),
                            style = ringStrokeDashed
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f),
                            radius = w * 0.17f,
                            center = Offset(w * 0.5f, h * 0.5f),
                            style = ringStrokeThin
                        )
                        drawPath(path = wave1, color = primary, style = stroke1)
                        drawPath(path = wave2, color = Color.White.copy(alpha = 0.72f), style = stroke2)
                    }
                }
        )
    }
}

@Composable
fun AudioVisualizerCanvas(
    bars: List<Float>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Obsidian.copy(alpha = 0.75f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = bars.size.coerceAtLeast(1)
            val horizontalPad = 12.dp.toPx()
            val availW = (size.width - horizontalPad * 2).coerceAtLeast(1f)
            val gap = 3.dp.toPx()
            val barWidth = ((availW - gap * (count - 1)) / count).coerceAtLeast(2f)

            for (i in 0 until count) {
                val amp = bars[i].coerceIn(0.06f, 1.0f)
                val barHeight = (amp * size.height * 0.82f).coerceAtLeast(6.dp.toPx())
                val x = horizontalPad + i * (barWidth + gap)
                val y = (size.height - barHeight) / 2f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(GoldBright, GoldMetallic, RoyalBlueAccent),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
