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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.ui.components.AudioVisualizerCanvas
import com.example.ui.components.CompactGoldSlider
import com.example.ui.components.TrackArtworkCanvas
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatPlayCount
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.TextMuted

@Composable
fun PlayerScreen(
    track: Track?,
    isPlaying: Boolean,
    positionSec: Float,
    durationSec: Int,
    volume: Float,
    isMuted: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isProUnlocked: Boolean,
    sleepTimerMinutes: Int,
    spectrumBars: List<Float>,
    topOverlayPadding: Dp = 0.dp,
    bottomOverlayPadding: Dp = 0.dp,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onIsolateVocals: () -> Unit,
    onHarmonicTransition: () -> Unit,
    onTriggerInstantAiMix: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = topOverlayPadding + 16.dp, bottom = bottomOverlayPadding + 16.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkSurface.copy(alpha = 0.90f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Aucun morceau sélectionné",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Ouvrez l'onglet Biblio pour scanner ou importer vos fichiers audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onOpenLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("player_open_library_btn")
                ) {
                    Text("Ouvrir Biblio", fontSize = 12.sp)
                }
            }
        }
        return
    }

    val effectiveDurationSec = durationSec.coerceAtLeast(track.durationSec).coerceAtLeast(1)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 780.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = topOverlayPadding + 8.dp,
                bottom = bottomOverlayPadding + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PlayerDeckCard(
                            track = track,
                            isPlaying = isPlaying,
                            positionSec = positionSec,
                            durationSec = effectiveDurationSec,
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
                        PlayerMetadataAndActionsCard(
                            track = track,
                            isProUnlocked = isProUnlocked,
                            sleepTimerMinutes = sleepTimerMinutes,
                            onCycleSleepTimer = onCycleSleepTimer,
                            onIsolateVocals = onIsolateVocals,
                            onHarmonicTransition = onHarmonicTransition,
                            onTriggerInstantAiMix = onTriggerInstantAiMix,
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
                        durationSec = effectiveDurationSec,
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
                    PlayerMetadataAndActionsCard(
                        track = track,
                        isProUnlocked = isProUnlocked,
                        sleepTimerMinutes = sleepTimerMinutes,
                        onCycleSleepTimer = onCycleSleepTimer,
                        onIsolateVocals = onIsolateVocals,
                        onHarmonicTransition = onHarmonicTransition,
                        onTriggerInstantAiMix = onTriggerInstantAiMix
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerDeckCard(
    track: Track,
    isPlaying: Boolean,
    positionSec: Float,
    durationSec: Int,
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
    val liveProgress = (positionSec / durationSec.coerceAtLeast(1)).coerceIn(0f, 1f)
    var isSeeking by remember(track.id) { mutableStateOf(false) }
    var dragFraction by remember(track.id) { mutableFloatStateOf(0f) }

    val displayedProgress = if (isSeeking) dragFraction else liveProgress
    val displayedSec = if (isSeeking) {
        (dragFraction * durationSec).toInt()
    } else {
        positionSec.toInt()
    }
    val remainingSec = (durationSec - displayedSec).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(track.primaryColorHex).copy(alpha = 0.20f), CardDarkSurface.copy(alpha = 0.94f))
                )
            )
            .border(1.dp, BorderGold, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact horizontal header: Artwork + Metadata + Like button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(GoldBright, RoyalBlueAccent, GoldMetallic)))
                    .padding(1.5.dp)
            ) {
                TrackArtworkCanvas(
                    track = track,
                    cornerRadius = 12.5.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (track.album.isNotBlank() && track.album != "Stockage local") {
                        "${track.artist} • ${track.album}"
                    } else {
                        track.artist
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = GoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatPlayCount(track.playCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = GoldLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldBright.copy(alpha = 0.14f))
                            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                    if (isPlaying) {
                        Text(
                            text = "En lecture",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = EmeraldAccent
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("player_like_button")
            ) {
                Icon(
                    imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (track.isLiked) RoseError else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Sleek spectrum visualizer
        AudioVisualizerCanvas(
            bars = spectrumBars,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )

        // Sleek seek bar + live elapsed / total & remaining counters
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CompactGoldSlider(
                value = displayedProgress,
                onValueChange = { frac ->
                    isSeeking = true
                    dragFraction = frac
                },
                onValueChangeFinished = {
                    val commitFraction = dragFraction
                    isSeeking = false
                    onSeekFraction(commitFraction)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(displayedSec),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = GoldLight,
                    modifier = Modifier.testTag("player_elapsed_time")
                )
                Text(
                    text = "-${formatDuration(remainingSec)} • ${formatDuration(durationSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = TextMuted
                )
            }
        }

        // Compact transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("player_shuffle_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    tint = if (isShuffle) GoldBright else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("player_prev_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_track),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("player_next_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_track),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("player_repeat_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = stringResource(R.string.cd_repeat),
                    tint = if (isRepeat) GoldBright else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Sleek compact volume row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onToggleMute, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.cd_mute),
                    tint = GoldLight,
                    modifier = Modifier.size(17.dp)
                )
            }
            CompactGoldSlider(
                value = if (isMuted) 0f else volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${((if (isMuted) 0f else volume) * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 32.dp)
            )
        }
    }
}

@Composable
private fun PlayerMetadataAndActionsCard(
    track: Track,
    isProUnlocked: Boolean,
    sleepTimerMinutes: Int,
    onCycleSleepTimer: () -> Unit,
    onIsolateVocals: () -> Unit,
    onHarmonicTransition: () -> Unit,
    onTriggerInstantAiMix: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeDisplay = formatFileSize(track.fileSizeBytes).ifBlank { "—" }
    val bitrateDisplay = if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "—"
    val formatDisplay = track.mimeFormat.ifBlank { "AUDIO" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Informations du fichier",
            style = MaterialTheme.typography.labelLarge,
            color = GoldLight
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactMetaTile("Format", formatDisplay, GoldBright, Modifier.weight(1f))
            CompactMetaTile("Débit", bitrateDisplay, Color.White, Modifier.weight(1f))
            CompactMetaTile("Taille", sizeDisplay, EmeraldAccent, Modifier.weight(1f))
            CompactMetaTile(
                label = if (track.year.isNotBlank()) "Année" else "Écoutes",
                value = if (track.year.isNotBlank()) track.year else "${track.playCount}",
                color = GoldLight,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactPlayerAction(
                icon = Icons.Filled.Timer,
                title = if (sleepTimerMinutes > 0) "$sleepTimerMinutes min" else "Minuteur",
                subtitle = if (sleepTimerMinutes > 0) "Arrêt auto" else "15 / 30 / 45m",
                onClick = onCycleSleepTimer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_sleep_timer_btn")
            )
            CompactPlayerAction(
                icon = Icons.Filled.Mic,
                title = "Mode Voix",
                subtitle = "Clarté chant",
                onClick = onIsolateVocals,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactPlayerAction(
                icon = Icons.Filled.Tune,
                title = "Fondu",
                subtitle = "Piste suivante",
                onClick = onHarmonicTransition,
                modifier = Modifier.weight(1f)
            )
            CompactPlayerAction(
                icon = if (isProUnlocked) Icons.Filled.AutoAwesome else Icons.Filled.Lock,
                title = if (isProUnlocked) "Mix IA" else "Mix Pro",
                subtitle = "File intelligente",
                onClick = onTriggerInstantAiMix,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactMetaTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Obsidian.copy(alpha = 0.72f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 10.5.sp,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactPlayerAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = GoldBright, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
