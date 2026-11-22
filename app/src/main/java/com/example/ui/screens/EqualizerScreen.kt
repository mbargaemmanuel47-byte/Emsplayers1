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
import androidx.compose.ui.unit.Constraints
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

private val eqFreqLabels = listOf("60Hz", "170Hz", "350Hz", "1kHz", "3kHz", "6kHz", "12kHz")

@Composable
fun EqualizerScreen(
    settings: AudioSettingsState,
    dspStatus: String,
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DSP & ACOUSTIQUE TEMPS RÉEL", style = MaterialTheme.typography.labelMedium, color = GoldBright)
                    Text("Égaliseur 7 Bandes & Audio Spatial", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(
                        "Ajustez la courbe fréquentielle du moteur AudioTrack / Equalizer en direct et activez les traitements DSP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
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
                            dspStatus = dspStatus,
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
                        dspStatus = dspStatus,
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

            item { Spacer(modifier = Modifier.height(12.dp)) }
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
            .clip(RoundedCornerShape(24.dp))
            .background(CardDarkSurface.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Égaliseur Graphique 7 Bandes", style = MaterialTheme.typography.titleMedium, color = Color.White)

        // Presets row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "flat" to "Plat",
                "bass" to "Bass Boost",
                "zouk" to "R&B / Zouk",
                "studio" to "Studio DSP",
                "vocal" to "Vocal"
            ).forEach { (key, label) ->
                val active = settings.activePreset == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) GoldBright else Color.White.copy(alpha = 0.06f))
                        .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(key) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("eq_preset_$key")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 12.sp,
                        color = if (active) Obsidian else TextSecondary
                    )
                }
            }
        }

        // 7 vertical sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Obsidian.copy(alpha = 0.72f))
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            eqFreqLabels.forEachIndexed { idx, freqLabel ->
                val gain = settings.eqGainsDb.getOrElse(idx) { 0f }
                val gainText = if (gain > 0f) "+${gain.roundToInt()}dB" else "${gain.roundToInt()}dB"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = gainText,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    VerticalEqSlider(
                        value = gain,
                        onValueChange = { onBandChange(idx, it) },
                        modifier = Modifier
                            .height(155.dp)
                            .width(36.dp)
                            .testTag("eq_band_$idx")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.labelSmall,
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
            Text("Plage dynamique : -12 dB à +12 dB", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextMuted)
            Text(
                text = "Réinitialiser à 0 dB",
                style = MaterialTheme.typography.labelLarge,
                color = GoldBright,
                modifier = Modifier
                    .clickable { onSelectPreset("flat") }
                    .padding(6.dp)
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
    dspStatus: String,
    onToggleCrossfade: (Boolean) -> Unit,
    onToggleNormalize: (Boolean) -> Unit,
    onToggleSpatial3d: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardDarkSurface.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Traitements DSP & Confort", style = MaterialTheme.typography.titleMedium, color = Color.White)

        DspToggleRow(
            title = "Fondu Enchaîné Temps Réel",
            subtitle = "Enchaîne les pistes avec rampe d'entrée et de sortie (3,5 s).",
            checked = settings.crossfadeEnabled,
            onCheckedChange = onToggleCrossfade,
            testTag = "toggle_crossfade"
        )

        DspToggleRow(
            title = "Normalisation Loudness",
            subtitle = "Active LoudnessEnhancer matériel (-14 LUFS cible) entre chaque piste.",
            checked = settings.normalizeEnabled,
            onCheckedChange = onToggleNormalize,
            testTag = "toggle_normalize"
        )

        DspToggleRow(
            title = "Spatial Audio 3D Binaural",
            subtitle = "Élargit la scène stéréo avec spatialisation psychoacoustique.",
            checked = settings.spatial3dEnabled,
            onCheckedChange = onToggleSpatial3d,
            badge = "BINAURAL",
            testTag = "toggle_spatial3d"
        )

        DspToggleRow(
            title = "Retour Haptique (Vibrations)",
            subtitle = "Légère vibration tactile sur les contrôles de lecture (désactivable).",
            checked = settings.hapticFeedbackEnabled,
            onCheckedChange = onToggleHapticFeedback,
            testTag = "toggle_haptic_feedback"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Obsidian.copy(alpha = 0.8f))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Statut Moteur DSP", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text(dspStatus, style = MaterialTheme.typography.labelMedium, color = EmeraldAccent)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Échantillonnage", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text("48000 Hz • Stéréo 16-bit", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun DspToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, BorderGold.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
                if (badge != null) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldBright.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = TextMuted)
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
