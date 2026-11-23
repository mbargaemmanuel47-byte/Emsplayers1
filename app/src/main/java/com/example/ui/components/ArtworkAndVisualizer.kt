package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Track
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoyalBlueAccent
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val mins = s / 60
    val secs = s % 60
    return String.format(Locale.FRANCE, "%d:%02d", mins, secs)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.FRANCE, "%.0f Ko", kb)
    val mb = kb / 1024.0
    return String.format(Locale.FRANCE, "%.1f Mo", mb)
}

fun formatPlayCount(count: Int): String {
    return when {
        count <= 0 -> "Nouveau"
        count == 1 -> "1 écoute"
        else -> "$count écoutes"
    }
}

@Composable
fun EmPlayerLogo(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "em_logo_pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Image(
        painter = painterResource(id = R.drawable.em_player_custom_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            val s = if (isPlaying) pulse else 1f
            scaleX = s
            scaleY = s
        }
    )
}

@Composable
fun FloatingAmbientBackground(
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ambient_motion")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 14000 else 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Floating ambient orb 1 (gold)
        val c1 = Offset(
            x = w * (0.22f + 0.14f * cos(orbit)),
            y = h * (0.18f + 0.08f * sin(orbit))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldBright.copy(alpha = 0.14f), Color.Transparent),
                center = c1,
                radius = w * 0.45f
            ),
            center = c1,
            radius = w * 0.45f
        )

        // Floating ambient orb 2 (track accent / sapphire)
        val c2 = Offset(
            x = w * (0.78f - 0.16f * sin(orbit * 0.8f)),
            y = h * (0.52f + 0.12f * cos(orbit * 0.8f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
                center = c2,
                radius = w * 0.52f
            ),
            center = c2,
            radius = w * 0.52f
        )

        // Floating ambient orb 3 (deep emerald/blue bottom)
        val c3 = Offset(
            x = w * (0.38f + 0.18f * (drift - 0.5f)),
            y = h * (0.86f - 0.06f * sin(orbit))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RoyalBlueAccent.copy(alpha = 0.15f), Color.Transparent),
                center = c3,
                radius = w * 0.42f
            ),
            center = c3,
            radius = w * 0.42f
        )

        // Subtle small floating luminous particles
        val particleCount = 10
        for (i in 0 until particleCount) {
            val seedX = ((i * 37) % 100) / 100f
            val seedY = ((i * 53 + 17) % 100) / 100f
            val speed = 1f + (i % 3) * 0.35f
            val px = ((seedX + 0.06f * sin(orbit * speed + i)) * w).coerceIn(0f, w)
            val py = ((seedY - (orbit / (2f * PI.toFloat())) * 0.18f * speed + 1f) % 1f) * h
            val r = (2.2f + (i % 3) * 1.4f).dp.toPx()
            val alpha = 0.16f + 0.14f * ((sin(orbit * 2f + i) + 1f) * 0.5f)
            val particleColor = if (i % 2 == 0) GoldBright else RoyalBlueAccent

            drawCircle(
                color = particleColor.copy(alpha = alpha),
                radius = r,
                center = Offset(px, py)
            )
        }
    }
}

@Composable
fun TrackArtworkCanvas(
    track: Track,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
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
                        colors = listOf(secondary, tertiary, primary.copy(alpha = 0.72f)),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    )
                    val orbCenter = Offset(w * 0.35f, h * 0.30f)
                    val orbRadius = (w * 0.42f).coerceAtLeast(1f)
                    val orbBrush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.75f), Color.Transparent),
                        center = orbCenter,
                        radius = orbRadius
                    )
                    val ringStrokeDashed = Stroke(
                        width = (w * 0.010f).coerceAtLeast(1.1f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                    )
                    val ringStrokeThin = Stroke(width = (w * 0.006f).coerceAtLeast(1f))
                    val wave1 = Path().apply {
                        moveTo(w * 0.25f, h * 0.50f)
                        quadraticTo(w * 0.38f, h * 0.32f, w * 0.50f, h * 0.50f)
                        quadraticTo(w * 0.62f, h * 0.68f, w * 0.75f, h * 0.50f)
                    }
                    val wave2 = Path().apply {
                        moveTo(w * 0.32f, h * 0.57f)
                        quadraticTo(w * 0.42f, h * 0.43f, w * 0.50f, h * 0.57f)
                        quadraticTo(w * 0.58f, h * 0.71f, w * 0.68f, h * 0.57f)
                    }
                    val stroke1 = Stroke(width = (w * 0.022f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
                    val stroke2 = Stroke(width = (w * 0.012f).coerceAtLeast(1.1f), cap = StrokeCap.Round)

                    onDrawBehind {
                        drawRect(brush = bgBrush)
                        drawCircle(brush = orbBrush, center = orbCenter, radius = orbRadius)
                        drawCircle(
                            color = primary.copy(alpha = 0.42f),
                            radius = w * 0.27f,
                            center = Offset(w * 0.5f, h * 0.5f),
                            style = ringStrokeDashed
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.24f),
                            radius = w * 0.16f,
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
            .clip(RoundedCornerShape(14.dp))
            .background(Obsidian.copy(alpha = 0.68f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = bars.size.coerceAtLeast(1)
            val horizontalPad = 10.dp.toPx()
            val availW = (size.width - horizontalPad * 2).coerceAtLeast(1f)
            val gap = 2.5.dp.toPx()
            val barWidth = ((availW - gap * (count - 1)) / count).coerceAtLeast(2f)

            for (i in 0 until count) {
                val amp = bars[i].coerceIn(0.06f, 1.0f)
                val barHeight = (amp * size.height * 0.80f).coerceAtLeast(4.dp.toPx())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactGoldSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = ((clamped - valueRange.start) / span).coerceIn(0f, 1f)

    Slider(
        value = clamped,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier.height(26.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(GoldBright)
                    .border(2.dp, Obsidian, CircleShape)
            )
        },
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(GoldBright, GoldMetallic)))
                )
            }
        }
    )
}
