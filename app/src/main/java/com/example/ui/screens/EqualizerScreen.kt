package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSettingsState
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

private val eqFreqLabels = listOf("60", "170", "350", "1k", "3k", "6k", "12k")

@Composable
fun EqualizerScreen(
    settings: AudioSettingsState,
    dspStatus: String,
    topOverlayPadding: Dp,
    bottomOverlayPadding: Dp,
    onBandChange: (Int, Float) -> Unit,
    onSelectPreset: (String) -> Unit,
    onToggleCrossfade: (Boolean) -> Unit,
    onToggleNormalize: (Boolean) -> Unit,
    onToggleSpatial3d: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pulseHaptic = {
        if (settings.hapticFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 820.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = topOverlayPadding + 10.dp,
                bottom = bottomOverlayPadding + 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Égaliseur",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = dspStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        EqualizerBandsCard(
                            settings = settings,
                            onBandChange = onBandChange,
                            onSelectPreset = {
                                pulseHaptic()
                                onSelectPreset(it)
                            },
                            modifier = Modifier.weight(1.2f)
                        )
                        DspSettingsCard(
                            settings = settings,
                            onToggleCrossfade = onToggleCrossfade,
                            onToggleNormalize = onToggleNormalize,
                            onToggleSpatial3d = onToggleSpatial3d,
                            onToggleHapticFeedback = { enabled ->
                                if (enabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleHapticFeedback(enabled)
                            },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            } else {
                item {
                    EqualizerBandsCard(
                        settings = settings,
                        onBandChange = onBandChange,
                        onSelectPreset = {
                            pulseHaptic()
                            onSelectPreset(it)
                        }
                    )
                }
                item {
                    DspSettingsCard(
                        settings = settings,
                        onToggleCrossfade = onToggleCrossfade,
                        onToggleNormalize = onToggleNormalize,
                        onToggleSpatial3d = onToggleSpatial3d,
                        onToggleHapticFeedback = { enabled ->
                            if (enabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleHapticFeedback(enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerBandsCard(
    settings: AudioSettingsState,
    onBandChange: (Int, Float) -> Unit,
    onSelectPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "flat" to "Plat",
                "bass" to "Basses",
                "zouk" to "Doux",
                "studio" to "Clair",
                "vocal" to "Voix"
            ).forEach { (key, label) ->
                val active = settings.activePreset == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) GoldBright else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { onSelectPreset(key) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("eq_preset_$key")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = if (active) Obsidian else TextSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Obsidian.copy(alpha = 0.68f))
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            eqFreqLabels.forEachIndexed { idx, freqLabel ->
                val gain = settings.eqGainsDb.getOrElse(idx) { 0f }
                val gainText = if (gain > 0f) "+${gain.roundToInt()}" else "${gain.roundToInt()}"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = gainText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    VerticalEqSlider(
                        value = gain,
                        onValueChange = { onBandChange(idx, it) },
                        modifier = Modifier
                            .height(124.dp)
                            .width(28.dp)
                            .testTag("eq_band_$idx")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = TextMuted
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-12 à +12 dB • Fréquences (Hz)",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                text = "Réinitialiser",
                style = MaterialTheme.typography.labelMedium,
                color = GoldBright,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clickable { onSelectPreset("flat") }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun VerticalEqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = { onValueChange(it.roundToInt().toFloat()) },
        valueRange = -12f..12f,
        colors = SliderDefaults.colors(
            thumbColor = GoldBright,
            activeTrackColor = GoldBright,
            inactiveTrackColor = Color.White.copy(alpha = 0.16f)
        ),
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}

@Composable
private fun DspSettingsCard(
    settings: AudioSettingsState,
    onToggleCrossfade: (Boolean) -> Unit,
    onToggleNormalize: (Boolean) -> Unit,
    onToggleSpatial3d: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Effets & Confort", style = MaterialTheme.typography.titleSmall, color = Color.White)

        DspToggleRow(
            title = "Fondu enchaîné",
            subtitle = "Enchaînement doux entre les titres",
            checked = settings.crossfadeEnabled,
            onCheckedChange = onToggleCrossfade,
            testTag = "toggle_crossfade"
        )

        DspToggleRow(
            title = "Normalisation",
            subtitle = "Équilibre le volume entre les morceaux",
            checked = settings.normalizeEnabled,
            onCheckedChange = onToggleNormalize,
            testTag = "toggle_normalize"
        )

        DspToggleRow(
            title = "Son spatial 3D",
            subtitle = "Élargit la scène stéréo au casque",
            checked = settings.spatial3dEnabled,
            onCheckedChange = onToggleSpatial3d,
            testTag = "toggle_spatial3d"
        )

        DspToggleRow(
            title = "Vibrations",
            subtitle = "Retour tactile léger sur les boutons",
            checked = settings.hapticFeedbackEnabled,
            onCheckedChange = onToggleHapticFeedback,
            testTag = "toggle_haptic_feedback"
        )
    }
}

@Composable
private fun DspToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, BorderGold.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextMuted
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Obsidian,
                checkedTrackColor = GoldBright,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
