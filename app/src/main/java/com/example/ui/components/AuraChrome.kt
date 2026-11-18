package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.TextMuted

object AuraRoutes {
    const val HOME = "home"
    const val PLAYER = "player"
    const val STUDIO = "studio"
    const val LIBRARY = "library"
    const val EQUALIZER = "equalizer"
    const val PERMISSIONS = "permissions"
}

data class AuraNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val auraNavDestinations = listOf(
    AuraNavDestination(AuraRoutes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    AuraNavDestination(AuraRoutes.PLAYER, R.string.nav_player, Icons.Filled.PlayArrow, Icons.Outlined.PlayCircle, "nav_player"),
    AuraNavDestination(AuraRoutes.STUDIO, R.string.nav_studio, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_studio"),
    AuraNavDestination(AuraRoutes.LIBRARY, R.string.nav_library, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
    AuraNavDestination(AuraRoutes.EQUALIZER, R.string.nav_equalizer, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_equalizer")
)

@Composable
fun AuraTopHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onInstantAiMix: () -> Unit,
    onOpenPermissions: () -> Unit,
    allPermissionsGranted: Boolean,
    isProUnlocked: Boolean,
    showSearchBar: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Midnight.copy(alpha = 0.92f),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Brand emblem
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(GoldBright, GoldMetallic)))
                            .padding(1.5.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Obsidian),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Ems Player",
                            tint = GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Ems ",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Player",
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldBright
                    )
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Rechercher titre, BPM, tonalité...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted)
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldBright.copy(alpha = 0.6f),
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("global_search_input")
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Instant AI Mix CTA
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(DeepBlue)
                        .border(1.dp, BorderGold, RoundedCornerShape(24.dp))
                        .clickable(onClick = onInstantAiMix)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("instant_ai_mix_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isProUnlocked) Icons.Filled.Bolt else Icons.Filled.Lock,
                        contentDescription = if (isProUnlocked) "IA Mix Pro" else "IA Mix Pro verrouillé",
                        tint = GoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isProUnlocked) "IA Mix" else "IA Mix • PRO",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldLight,
                        fontSize = 12.sp
                    )
                }

                // Android 7-15 Permissions Shield button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable(onClick = onOpenPermissions)
                        .testTag("permissions_center_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = stringResource(R.string.cd_permissions_shield),
                        tint = if (allPermissionsGranted) EmeraldAccent else GoldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderSubtle)
            )
        }
    }
}

@Composable
fun AuraMiniPlayer(
    track: Track,
    isPlaying: Boolean,
    positionSec: Float,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (track.durationSec > 0) (positionSec / track.durationSec).coerceIn(0f, 1f) else 0f

    Surface(
        color = CardDarkSurface.copy(alpha = 0.96f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Thin golden progress line at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(GoldBright, GoldMetallic)))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayer)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("mini_player_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrackArtworkCanvas(
                    track = track,
                    cornerRadius = 10.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, BorderGold, RoundedCornerShape(10.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = if (track.bpm > 0) "${track.bpm} BPM" else track.genre.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.09f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${track.artist} • ${formatDuration(positionSec.toInt())} / ${formatDuration(track.durationSec)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("mini_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous_track),
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GoldBright, GoldMetallic)))
                        .clickable(onClick = onTogglePlay)
                        .testTag("mini_play_button"),
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
                    modifier = Modifier.testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_track),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onOpenPlayer,
                    modifier = Modifier.testTag("mini_expand_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = stringResource(R.string.cd_expand_player),
                        tint = GoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AuraBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = Midnight.copy(alpha = 0.98f),
        contentColor = GoldBright,
        modifier = modifier.navigationBarsPadding()
    ) {
        auraNavDestinations.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(dest.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Obsidian,
                    selectedTextColor = GoldBright,
                    indicatorColor = GoldBright,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag(dest.testTag)
            )
        }
    }
}

@Composable
fun AuraNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = Midnight,
        contentColor = GoldBright,
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        auraNavDestinations.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(dest.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(dest.labelRes),
                        fontSize = 11.sp
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Obsidian,
                    selectedTextColor = GoldBright,
                    indicatorColor = GoldBright,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("rail_${dest.testTag}")
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = currentRoute == AuraRoutes.PERMISSIONS,
            onClick = onOpenPermissions,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = stringResource(R.string.nav_permissions)
                )
            },
            label = { Text(stringResource(R.string.nav_permissions), fontSize = 11.sp) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Obsidian,
                selectedTextColor = GoldBright,
                indicatorColor = GoldBright,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AuraToastOverlay(
    toast: Pair<String, String>?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 64.dp, start = 16.dp, end = 16.dp)
    ) {
        if (toast != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDarkSurface.copy(alpha = 0.96f))
                    .border(1.dp, GoldBright.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(GoldBright)
                )
                Column {
                    Text(
                        text = toast.first,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (toast.second.isNotBlank()) {
                        Text(
                            text = toast.second,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = GoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
