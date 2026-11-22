package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.SeedCatalog
import com.example.model.Track
import com.example.permissions.PermissionManager
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    tracks: List<Track>,
    currentTrack: Track,
    globalSearch: String,
    onPlayTrack: (Track) -> Unit,
    onPlayTrackById: (Long) -> Unit,
    onToggleLike: (Track) -> Unit,
    onScanDeviceAudio: () -> Unit,
    onImportSafUri: (Uri) -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf("all") }
    var searchText by rememberSaveable(globalSearch) { mutableStateOf(globalSearch) }
    var selectedGenre by rememberSaveable { mutableStateOf("all") }
    var selectedBpmRange by rememberSaveable { mutableStateOf("all") }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        if (granted) {
            onScanDeviceAudio()
        }
    }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportSafUri(uri)
        }
    }

    val filteredTracks = remember(tracks, selectedTab, searchText, selectedGenre, selectedBpmRange) {
        tracks.filter { track ->
            val tabOk = when (selectedTab) {
                "liked" -> track.isLiked
                else -> true
            }
            val genreOk = selectedGenre == "all" || track.genre == selectedGenre
            val bpmOk = when (selectedBpmRange) {
                "slow" -> track.bpm in 1..99
                "mid" -> track.bpm in 100..115
                "fast" -> track.bpm > 115
                else -> true
            }
            val q = searchText.trim().lowercase()
            val queryOk = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.musicalKey.lowercase().contains(q) ||
                track.genre.lowercase().contains(q)
            tabOk && genreOk && bpmOk && queryOk
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bibliothèque & Playlists", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    "Catalogue synthétisé Ems Player et fichiers audio locaux (Android 6 à 15)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Local device scan + zero-permission SAF import
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { safPickerLauncher.launch(arrayOf("audio/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("saf_import_audio_btn")
                ) {
                    Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.perm_saf_cta), maxLines = 1, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (PermissionManager.hasAudioLibraryPermission(context)) {
                            onScanDeviceAudio()
                        } else {
                            audioPermissionLauncher.launch(PermissionManager.audioLibraryPermission())
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_device_audio_btn")
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.perm_scan_cta), maxLines = 1, fontSize = 12.sp)
                }
            }
        }

        // Library tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "Toutes les pistes",
                    "liked" to "Titres Likés ❤️",
                    "playlists" to "Playlists",
                    "artists" to "Artistes"
                ).forEach { (key, label) ->
                    val active = selectedTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) GoldBright else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { selectedTab = key }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("lib_tab_$key")
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
        }

        // Search + Genre & BPM filters
        if (selectedTab == "all" || selectedTab == "liked") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardDarkSurface.copy(alpha = 0.85f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        placeholder = { Text("Filtrer par titre, artiste, tonalité (Fm)...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldBright,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Obsidian.copy(alpha = 0.7f),
                            unfocusedContainerColor = Obsidian.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("all" to "Tous genres", "Zouk & R&B" to "Zouk & R&B", "Focus & Ambient" to "Focus", "Énergie Studio" to "Studio", "Chill & Synthwave" to "Synthwave", "Audio Local" to "Local")
                            .forEach { (gKey, gLabel) ->
                                val active = selectedGenre == gKey
                                FilterTagChip(gLabel, active) { selectedGenre = gKey }
                            }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("all" to "Tous BPM", "slow" to "< 100 BPM", "mid" to "100–115 BPM", "fast" to "> 115 BPM")
                            .forEach { (bKey, bLabel) ->
                                val active = selectedBpmRange == bKey
                                FilterTagChip(bLabel, active) { selectedBpmRange = bKey }
                            }
                    }
                }
            }
        }

        // Content based on selectedTab
        when (selectedTab) {
            "playlists" -> {
                items(SeedCatalog.moodPresets) { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(DeepBlue.copy(alpha = 0.8f), CardDarkSurface)))
                            .border(1.dp, BorderGold, RoundedCornerShape(18.dp))
                            .clickable { onPlayTrackById(preset.trackId) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldBright.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = GoldBright)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(preset.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            Text(preset.bpmRange, style = MaterialTheme.typography.labelMedium, color = GoldLight)
                        }
                    }
                }
            }

            "artists" -> {
                val artists = tracks.groupBy { it.artist }.entries.toList()
                items(artists) { (artist, artistTracks) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardDarkSurface.copy(alpha = 0.9f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                            .clickable { artistTracks.firstOrNull()?.let(onPlayTrack) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GoldBright, RoyalBlueAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = artist.firstOrNull()?.uppercase() ?: "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = Obsidian
                            )
                        }
                        Column {
                            Text(artist, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                text = "${artistTracks.size} piste(s) • Catalogue Ems Player",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            else -> {
                if (filteredTracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardDarkSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aucune piste ne correspond à vos filtres actuels.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackListRow(
                            track = track,
                            isCurrent = track.id == currentTrack.id,
                            onPlay = { onPlayTrack(track) },
                            onToggleLike = { onToggleLike(track) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun FilterTagChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) GoldBright.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) GoldBright else TextSecondary
        )
    }
}
