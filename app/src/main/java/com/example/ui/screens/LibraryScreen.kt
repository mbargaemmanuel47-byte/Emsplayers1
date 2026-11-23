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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SavedPlaylist
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
    savedPlaylists: List<SavedPlaylist>,
    currentTrack: Track?,
    isScanning: Boolean,
    globalSearch: String,
    topOverlayPadding: Dp,
    bottomOverlayPadding: Dp,
    onPlayTrack: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    onScanDeviceAudio: () -> Unit,
    onImportSafUri: (Uri) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onPlaySavedPlaylist: (SavedPlaylist) -> Unit,
    onDeleteSavedPlaylist: (Long) -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf("all") }
    var searchText by rememberSaveable(globalSearch) { mutableStateOf(globalSearch) }
    var sortMode by rememberSaveable { mutableStateOf("recent") }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        if (granted) onScanDeviceAudio()
    }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportSafUri(uri)
    }

    val filteredTracks = remember(tracks, selectedTab, searchText, sortMode) {
        val base = tracks.filter { track ->
            val tabOk = when (selectedTab) {
                "liked" -> track.isLiked
                else -> true
            }
            val q = searchText.trim().lowercase()
            val queryOk = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q) ||
                track.mimeFormat.lowercase().contains(q)
            tabOk && queryOk
        }
        when (sortMode) {
            "plays" -> base.sortedByDescending { it.playCount }
            "alpha" -> base.sortedBy { it.title.lowercase() }
            "duration" -> base.sortedByDescending { it.durationSec }
            else -> base
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = topOverlayPadding + 10.dp,
            bottom = bottomOverlayPadding + 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bibliothèque",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = "${tracks.size} titres",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    color = GoldLight
                )
            }
        }

        // Concise Importer + Scanner buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { safPickerLauncher.launch(arrayOf("audio/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("saf_import_audio_btn")
                ) {
                    Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.perm_saf_cta),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (PermissionManager.hasAudioLibraryPermission(context)) {
                            onScanDeviceAudio()
                        } else {
                            audioPermissionLauncher.launch(PermissionManager.audioLibraryPermission())
                        }
                    },
                    enabled = !isScanning,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("scan_device_audio_btn")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldLight, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.perm_scan_cta),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Compact tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "all" to "Toutes",
                    "liked" to "Favoris",
                    "playlists" to "Listes auto",
                    "artists" to "Artistes"
                ).forEach { (key, label) ->
                    val active = selectedTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) GoldBright else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { selectedTab = key }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("lib_tab_$key")
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
        }

        if (selectedTab == "all" || selectedTab == "liked") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        placeholder = { Text("Filtrer titre, artiste, format...", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "recent" to "Ordre d'ajout",
                            "plays" to "Plus écoutés",
                            "alpha" to "A–Z",
                            "duration" to "Durée"
                        ).forEach { (sKey, sLabel) ->
                            FilterTagChip(sLabel, sortMode == sKey) { sortMode = sKey }
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            "playlists" -> {
                if (tracks.isNotEmpty()) {
                    item {
                        Button(
                            onClick = { showCreatePlaylistDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("create_playlist_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Créer une playlist", fontSize = 12.sp)
                        }
                    }
                }

                if (savedPlaylists.isNotEmpty()) {
                    items(savedPlaylists, key = { "saved_${it.id}" }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(DeepBlue.copy(alpha = 0.8f), CardDarkSurface)))
                                .border(1.dp, BorderGold, RoundedCornerShape(14.dp))
                                .clickable { onPlaySavedPlaylist(playlist) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GoldBright.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.trackIds.size} titres • ${if (playlist.isAiGenerated) "Emma IA" else "Perso"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSavedPlaylist(playlist.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Supprimer",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                val smartLists = buildList {
                    val topPlayed = tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
                    if (topPlayed.isNotEmpty()) {
                        add(Triple("Les plus écoutés", "${topPlayed.size} titres", topPlayed.first()))
                    }
                    val liked = tracks.filter { it.isLiked }
                    if (liked.isNotEmpty()) {
                        add(Triple("Mes favoris", "${liked.size} titres", liked.first()))
                    }
                    val recent = tracks.sortedByDescending { it.lastPlayedAt }.filter { it.lastPlayedAt > 0L }
                    if (recent.isNotEmpty()) {
                        add(Triple("Écoutés récemment", "${recent.size} titres", recent.first()))
                    }
                }

                if (smartLists.isEmpty() && savedPlaylists.isEmpty()) {
                    item {
                        EmptyLibraryNotice("Écoutez, aimez ou créez une playlist pour la retrouver ici.")
                    }
                } else {
                    items(smartLists) { (title, subtitle, firstTrack) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(DeepBlue.copy(alpha = 0.65f), CardDarkSurface)))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { onPlayTrack(firstTrack) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            "artists" -> {
                val artists = tracks.groupBy { it.artist }.entries.toList()
                if (artists.isEmpty()) {
                    item { EmptyLibraryNotice("Aucun artiste détecté. Lancez Scanner ou Importer.") }
                } else {
                    items(artists) { (artist, artistTracks) ->
                        val plays = artistTracks.sumOf { it.playCount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardDarkSurface.copy(alpha = 0.85f))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { artistTracks.firstOrNull()?.let(onPlayTrack) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(GoldBright, RoyalBlueAccent))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.firstOrNull()?.uppercase() ?: "A",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Obsidian
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${artistTracks.size} titre(s) • $plays écoute(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                if (filteredTracks.isEmpty()) {
                    item {
                        EmptyLibraryNotice(
                            if (tracks.isEmpty()) "Bibliothèque vide. Touchez Scanner ou Importer."
                            else "Aucun titre ne correspond à votre recherche."
                        )
                    }
                } else {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackListRow(
                            track = track,
                            isCurrent = track.id == currentTrack?.id,
                            onPlay = { onPlayTrack(track) },
                            onToggleLike = { onToggleLike(track) },
                            onDelete = { onDeleteTrack(track) }
                        )
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var playlistTitle by remember { mutableStateOf("") }
        val selectedIds = remember { mutableStateListOf<Long>() }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = CardDarkSurface,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = { Text("Nouvelle playlist", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = playlistTitle,
                        onValueChange = { playlistTitle = it },
                        singleLine = true,
                        placeholder = { Text("Nom de la playlist...", fontSize = 12.sp, color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldBright,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(tracks.take(60), key = { "pick_${it.id}" }) { track ->
                            val checked = selectedIds.contains(track.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (checked) selectedIds.remove(track.id) else selectedIds.add(track.id)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (!selectedIds.contains(track.id)) selectedIds.add(track.id)
                                        } else {
                                            selectedIds.remove(track.id)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = GoldBright,
                                        checkmarkColor = Obsidian
                                    )
                                )
                                Text(
                                    text = "${track.title} — ${track.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalIds = if (selectedIds.isNotEmpty()) {
                            selectedIds.toList()
                        } else {
                            tracks.take(5).map { it.id }
                        }
                        onCreatePlaylist(playlistTitle.ifBlank { "Ma playlist" }, finalIds)
                        showCreatePlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Annuler", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun EmptyLibraryNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDarkSurface.copy(alpha = 0.85f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FilterTagChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) GoldBright.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (active) GoldBright else BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            color = if (active) GoldBright else TextSecondary
        )
    }
}
