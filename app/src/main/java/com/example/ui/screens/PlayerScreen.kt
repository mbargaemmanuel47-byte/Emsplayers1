package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.ui.components.AudioVisualizerCanvas
import com.example.ui.components.TrackArtworkCanvas
import com.example.ui.components.formatDuration
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerScreen(
    track: Track,
    isPlaying: Boolean,
    positionSec: Float,
    volume: Float,
    isMuted: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isProUnlocked: Boolean,
    isRemixGenerating: Boolean,
    spectrumBars: List<Float>,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onSeekSeconds: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onExtendBeat60s: () -> Unit,
    onIsolateVocals: () -> Unit,
    onHarmonicTransition: () -> Unit,
    onGenerateRemix: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 780.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PlayerDeckCard(
                            track = track,
                            isPlaying = isPlaying,
                            positionSec = positionSec,
                            volume = volume,
                            isMuted = isMuted,
                            isShuffle = isShuffle,
                            isRepeat = isRepeat,
                            spectrumBars = spectrumBars,
                            onTogglePlay = onTogglePlay,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onSeekFraction = onSeekFraction,
                            onVolumeChange = onVolumeChange,
                            onToggleMute = onToggleMute,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onToggleLike = onToggleLike,
                            modifier = Modifier.weight(1f)
                        )
                        PlayerInsightsCard(
                            track = track,
                            positionSec = positionSec,
                            isProUnlocked = isProUnlocked,
                            isRemixGenerating = isRemixGenerating,
                            selectedSubTab = selectedSubTab,
                            onSelectSubTab = { selectedSubTab = it },
                            onSeekSeconds = onSeekSeconds,
                            onExtendBeat60s = onExtendBeat60s,
                            onIsolateVocals = onIsolateVocals,
                            onHarmonicTransition = onHarmonicTransition,
                            onGenerateRemix = onGenerateRemix,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                item {
                    PlayerDeckCard(
                        track = track,
                        isPlaying = isPlaying,
                        positionSec = positionSec,
                        volume = volume,
                        isMuted = isMuted,
                        isShuffle = isShuffle,
                        isRepeat = isRepeat,
                        spectrumBars = spectrumBars,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeekFraction = onSeekFraction,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onToggleLike = onToggleLike
                    )
                }
                item {
                    PlayerInsightsCard(
                        track = track,
                        positionSec = positionSec,
                        isProUnlocked = isProUnlocked,
                        isRemixGenerating = isRemixGenerating,
                        selectedSubTab = selectedSubTab,
                        onSelectSubTab = { selectedSubTab = it },
                        onSeekSeconds = onSeekSeconds,
                        onExtendBeat60s = onExtendBeat60s,
                        onIsolateVocals = onIsolateVocals,
                        onHarmonicTransition = onHarmonicTransition,
                        onGenerateRemix = onGenerateRemix
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PlayerDeckCard(
    track: Track,
    isPlaying: Boolean,
    positionSec: Float,
    volume: Float,
    isMuted: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    spectrumBars: List<Float>,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (track.durationSec > 0) (positionSec / track.durationSec).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(track.primaryColorHex).copy(alpha = 0.22f), CardDarkSurface)
                )
            )
            .border(1.dp, BorderGold, RoundedCornerShape(26.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header badge + Favorite button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val deckStatusBadge = when {
                track.localUri != null -> "FICHIER LOCAL • AUDIO DSP"
                track.isAiGenerated -> "CRÉATION IA PRO • EMS STUDIO"
                else -> "SYNTHÈSE 48KHZ • AUDIO DSP"
            }
            Text(
                text = deckStatusBadge,
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GoldBright.copy(alpha = 0.12f))
                    .border(1.dp, BorderGold, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("player_like_button")
            ) {
                Icon(
                    imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (track.isLiked) RoseError else Color.White
                )
            }
        }

        // Dynamic Artwork with metallic gold frame
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(GoldBright, RoyalBlueAccent, GoldMetallic)))
                .padding(3.dp)
        ) {
            TrackArtworkCanvas(
                track = track,
                cornerRadius = 23.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Title & Artist
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
        }

        // Real-time 42-bar spectrum visualizer
        AudioVisualizerCanvas(
            bars = spectrumBars,
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
        )

        // Seekbar + timestamps
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = onSeekFraction,
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = GoldBright,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("player_progress_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(positionSec.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Text(
                    text = "-${formatDuration((track.durationSec - positionSec.toInt()).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }

        // Transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.testTag("player_shuffle_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    tint = if (isShuffle) GoldBright else TextMuted
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("player_prev_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_track),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GoldBright, GoldMetallic)))
                    .clickable(onClick = onTogglePlay)
                    .testTag("player_play_pause_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = Obsidian,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("player_next_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_track),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier.testTag("player_repeat_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = stringResource(R.string.cd_repeat),
                    tint = if (isRepeat) GoldBright else TextMuted
                )
            }
        }

        // Volume row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.cd_mute),
                    tint = GoldLight
                )
            }
            Slider(
                value = if (isMuted) 0f else volume,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = GoldBright,
                    activeTrackColor = GoldBright,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${((if (isMuted) 0f else volume) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PlayerInsightsCard(
    track: Track,
    positionSec: Float,
    isProUnlocked: Boolean,
    isRemixGenerating: Boolean,
    selectedSubTab: Int,
    onSelectSubTab: (Int) -> Unit,
    onSeekSeconds: (Float) -> Unit,
    onExtendBeat60s: () -> Unit,
    onIsolateVocals: () -> Unit,
    onHarmonicTransition: () -> Unit,
    onGenerateRemix: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardDarkSurface.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Obsidian)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SubTabChip(
                text = "🎤 Paroles Sync",
                selected = selectedSubTab == 0,
                onClick = { onSelectSubTab(0) },
                modifier = Modifier.weight(1f)
            )
            SubTabChip(
                text = "🎛️ Analyse IA & Mix",
                selected = selectedSubTab == 1,
                onClick = { onSelectSubTab(1) },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedSubTab == 0) {
            if (track.lyrics.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Aucune parole synchronisée",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Ce fichier audio local ne contient pas de paroles horodatées.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                val activeLyricIndex = track.lyrics.indexOfLast { positionSec >= it.timeSec }.coerceAtLeast(0)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    track.lyrics.forEachIndexed { idx, line ->
                        val active = idx == activeLyricIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) GoldBright.copy(alpha = 0.16f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (active) BorderGold else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSeekSeconds(line.timeSec.toFloat()) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = formatDuration(line.timeSec),
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldBright.copy(alpha = 0.85f)
                            )
                            Text(
                                text = line.text,
                                style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = if (active) GoldLight else TextMuted
                            )
                        }
                    }
                }
            }

            // Acoustic / Artistic description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DeepBlue.copy(alpha = 0.6f))
                    .border(1.dp, BorderGold, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (track.isAiGenerated) "✨ Composition IA Pro" else "🎧 Note Acoustique & Mix",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldLight
                )
                Text(
                    text = track.aiInsight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            // Musical metrics (no fabricated values for local files)
            val tempoDisplay = if (track.bpm > 0) "${track.bpm} BPM" else "Local"
            val keyDisplay = track.musicalKey.ifBlank { "—" }
            val energyDisplay = if (track.energy > 0) "${track.energy}%" else "—"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalysisMetricCard("Tempo", tempoDisplay, GoldBright, Modifier.weight(1f))
                AnalysisMetricCard("Tonalité", keyDisplay, Color.White, Modifier.weight(1f))
                AnalysisMetricCard("Énergie", energyDisplay, EmeraldAccent, Modifier.weight(1f))
            }

            if (track.localUri == null) {
                // Real DSP synthesis voice balance profile
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PROFIL SYNTHÈSE DSP • ${track.genre.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                    SpectralBarRow("Groove & Percussions", track.synthProfile.groove, GoldBright)
                    SpectralBarRow("Chaleur Harmonique", track.synthProfile.warmth, RoyalBlueAccent)
                    SpectralBarRow("Clarté Vocale / Mélodie", track.synthProfile.clarity, EmeraldAccent)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "FICHIER AUDIO LOCAL • ANALYSE PCM",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                    Text(
                        text = "Le spectre 42 bandes ci-dessus est décodé en temps réel depuis les échantillons PCM du fichier (${track.genre}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Live Studio Actions (Remix IA gated behind Play Billing Pro)
            Text(
                text = "ACTIONS STUDIO EN DIRECT",
                style = MaterialTheme.typography.labelMedium,
                color = GoldLight
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveActionButton(
                        icon = Icons.Filled.Timer,
                        title = "Étendre (+60s)",
                        subtitle = if (track.localUri == null) "Boucle synthétiseur" else "Synthèse uniquement",
                        onClick = onExtendBeat60s,
                        modifier = Modifier.weight(1f)
                    )
                    LiveActionButton(
                        icon = Icons.Filled.Mic,
                        title = "Isoler Lead",
                        subtitle = "Filtre 1kHz–3kHz",
                        onClick = onIsolateVocals,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveActionButton(
                        icon = Icons.Filled.Tune,
                        title = "Transition DJ",
                        subtitle = "Enchaînement fluide",
                        onClick = onHarmonicTransition,
                        modifier = Modifier.weight(1f)
                    )
                    LiveActionButton(
                        icon = if (isProUnlocked) Icons.Filled.AutoAwesome else Icons.Filled.Lock,
                        title = if (isRemixGenerating) "Remix IA..." else if (isProUnlocked) "Remix IA" else "Remix IA • PRO",
                        subtitle = when {
                            isRemixGenerating -> "Gemini en cours..."
                            isProUnlocked -> "Arrangement Gemini"
                            else -> "Payant (Ems Pro)"
                        },
                        isLoading = isRemixGenerating,
                        onClick = onGenerateRemix,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GoldBright else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = if (selected) Obsidian else TextSecondary
        )
    }
}

@Composable
private fun AnalysisMetricCard(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DeepBlue.copy(alpha = 0.5f))
            .border(1.dp, BorderGold, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun SpectralBarRow(label: String, percent: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextSecondary)
            Text(text = "$percent%", style = MaterialTheme.typography.labelMedium, color = color)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun LiveActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = GoldBright,
                strokeWidth = 2.dp
            )
        } else {
            Icon(imageVector = icon, contentDescription = title, tint = GoldBright, modifier = Modifier.size(18.dp))
        }
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color.White, fontSize = 13.sp)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontSize = 11.sp)
    }
}
