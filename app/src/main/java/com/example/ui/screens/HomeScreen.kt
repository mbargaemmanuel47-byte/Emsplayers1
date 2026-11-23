package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
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
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    isScanning: Boolean,
    hapticEnabled: Boolean,
    selectedFilter: String,
    topOverlayPadding: Dp = 0.dp,
    bottomOverlayPadding: Dp = 0.dp,
    onFilterSelected: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLike: (Track) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val triggerHaptic = {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val totalPlays = remember(tracks) { tracks.sumOf { it.playCount } }

    val displayedTracks = remember(tracks, selectedFilter) {
        when (selectedFilter) {
            "top" -> tracks.sortedByDescending { it.playCount }
            "liked" -> tracks.filter { it.isLiked }
            "recent" -> tracks.sortedByDescending { it.lastPlayedAt }
            else -> tracks
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = topOverlayPadding + 8.dp,
            bottom = bottomOverlayPadding + 10.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isScanning) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldBright.copy(alpha = 0.12f))
                        .border(1.dp, BorderGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldBright, strokeWidth = 2.dp)
                    Text(
                        text = "Analyse des fichiers audio en cours...",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                }
            }
        }

        // Slim active-playback summary strip
        if (currentTrack != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardDarkSurface.copy(alpha = 0.88f))
                        .border(1.dp, BorderGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .clickable {
                            triggerHaptic()
                            onOpenPlayer()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("home_now_playing_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    TrackArtworkCanvas(
                        track = currentTrack,
                        cornerRadius = 8.dp,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentTrack.artist} • ${if (isPlaying) "En lecture" else "En pause"} • ${formatPlayCount(currentTrack.playCount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = GoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(GoldBright)
                            .clickable {
                                triggerHaptic()
                                onTogglePlayPause()
                            }
                            .testTag("hero_start_session_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lire",
                            tint = Obsidian,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Compact filter chips + total counters + Studio quick shortcut
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "all" to "Tous (${tracks.size})",
                    "top" to "Plus écoutés",
                    "liked" to "Favoris",
                    "recent" to "Récents"
                ).forEach { (key, label) ->
                    val active = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) GoldBright else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable {
                                triggerHaptic()
                                onFilterSelected(key)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("home_filter_$key")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = if (active) Obsidian else TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            triggerHaptic()
                            onOpenStudio()
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("hero_studio_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Studio IA",
                            tint = GoldLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Parler à Emma",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = GoldLight
                        )
                    }
                }
                if (totalPlays > 0) {
                    Text(
                        text = "$totalPlays écoutes",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = EmeraldAccent,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = onOpenLibrary)
                    )
                }
            }
        }

        if (displayedTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDarkSurface.copy(alpha = 0.88f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                        .testTag("home_empty_card"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldBright.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (selectedFilter == "liked") "Aucun favori" else "Aucune musique chargée",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (selectedFilter == "liked") {
                            "Touchez le cœur sur un morceau pour l'ajouter ici."
                        } else {
                            "Ouvrez l'onglet Biblio pour scanner ou importer vos fichiers audio."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    if (selectedFilter != "liked") {
                        Button(
                            onClick = {
                                triggerHaptic()
                                onOpenLibrary()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("home_open_library_btn")
                        ) {
                            Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ouvrir Biblio", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        } else {
            items(displayedTracks, key = { it.id }) { track ->
                val isCurrent = track.id == currentTrack?.id
                TrackListRow(
                    track = track,
                    isCurrent = isCurrent,
                    onPlay = {
                        triggerHaptic()
                        onPlayTrack(track)
                    },
                    onToggleLike = {
                        triggerHaptic()
                        onToggleLike(track)
                    }
                )
            }
        }
    }
}

@Composable
fun TrackListRow(
    track: Track,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sizeText = formatFileSize(track.fileSizeBytes)
    val playText = formatPlayCount(track.playCount)
    val metaLine = buildList {
        add(track.artist)
        if (track.mimeFormat.isNotBlank()) add(track.mimeFormat)
        if (sizeText.isNotBlank()) add(sizeText)
        add(playText)
    }.joinToString(" • ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) GoldBright.copy(alpha = 0.14f) else Midnight.copy(alpha = 0.68f))
            .border(
                1.dp,
                if (isCurrent) GoldBright.copy(alpha = 0.45f) else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        TrackArtworkCanvas(
            track = track,
            cornerRadius = 8.dp,
            modifier = Modifier.size(36.dp)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.5.sp,
                color = if (isCurrent) GoldLight else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(track.durationSec),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            maxLines = 1,
            softWrap = false,
            color = if (isCurrent) GoldBright else TextMuted
        )

        IconButton(
            onClick = onToggleLike,
            modifier = Modifier
                .size(30.dp)
                .testTag("like_track_${track.id}")
        ) {
            Icon(
                imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favori",
                tint = if (track.isLiked) RoseError else TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("delete_track_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Retirer",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
