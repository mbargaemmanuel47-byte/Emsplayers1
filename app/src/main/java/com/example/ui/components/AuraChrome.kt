package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.example.ui.theme.RoyalBlueAccent
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
    onOpenSearch: () -> Unit,
    onInstantAiMix: () -> Unit,
    onOpenPermissions: () -> Unit,
    allPermissionsGranted: Boolean,
    isProUnlocked: Boolean,
    isPlaying: Boolean,
    showSearchBar: Boolean,
    modifier: Modifier = Modifier
) {
    // Translucent frosted glass header allowing scrolled items underneath to diffuse through
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(18.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Frosted glass backdrop layer with soft haze diffusion
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Midnight.copy(alpha = 0.72f),
                            Obsidian.copy(alpha = 0.56f),
                            Obsidian.copy(alpha = 0.34f)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                GoldBright.copy(alpha = 0.08f),
                                DeepBlue.copy(alpha = 0.22f),
                                RoyalBlueAccent.copy(alpha = 0.10f)
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact Em Player logo + wordmark
                Row(
                    modifier = if (!showSearchBar) Modifier.weight(1f) else Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmPlayerLogo(
                        isPlaying = isPlaying,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                                append("Em ")
                            }
                            withStyle(SpanStyle(color = GoldBright, fontWeight = FontWeight.Bold)) {
                                append("Player")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Rechercher...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldBright.copy(alpha = 0.55f),
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("global_search_input")
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clickable(onClick = onOpenSearch)
                            .testTag("header_search_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Rechercher",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Compact Instant AI Mix pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(DeepBlue.copy(alpha = 0.78f))
                        .border(1.dp, BorderGold, RoundedCornerShape(18.dp))
                        .clickable(onClick = onInstantAiMix)
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .testTag("instant_ai_mix_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isProUnlocked) Icons.Filled.Bolt else Icons.Filled.Lock,
                        contentDescription = "Mix IA",
                        tint = GoldBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isProUnlocked) "Mix IA" else "Pro",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Compact Permissions button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable(onClick = onOpenPermissions)
                        .testTag("permissions_center_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = stringResource(R.string.cd_permissions_shield),
                        tint = if (allPermissionsGranted) EmeraldAccent else GoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Soft luminous glass edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.16f),
                                GoldBright.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun AuraMiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    positionSec: Float,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val transition = rememberInfiniteTransition(label = "mini_float")
    val floatOffsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) -2.5f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mini_float_y"
    )
    val progress = if (track.durationSec > 0) (positionSec / track.durationSec).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .offset(y = floatOffsetY.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, GoldBright.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
    ) {
        // Slim golden progress line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.5.dp)
                    .background(Brush.horizontalGradient(listOf(GoldBright, GoldMetallic)))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("mini_player_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrackArtworkCanvas(
                track = track,
                cornerRadius = 8.dp,
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${formatDuration(positionSec.toInt())} / ${formatDuration(track.durationSec)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("mini_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_track),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("mini_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_track),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
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
    // Floating translucent pill navigation bar respecting system navigation bar insets
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Midnight.copy(alpha = 0.86f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            auraNavDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) GoldBright else Color.Transparent)
                        .clickable { onNavigate(dest.route) }
                        .padding(horizontal = 2.dp, vertical = 6.dp)
                        .testTag(dest.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes),
                        tint = if (selected) Obsidian else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(dest.labelRes),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Obsidian else TextMuted
                    )
                }
            }
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
        containerColor = Midnight.copy(alpha = 0.90f),
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
                        contentDescription = stringResource(dest.labelRes),
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(dest.labelRes),
                        fontSize = 10.sp
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
                    contentDescription = stringResource(R.string.nav_permissions),
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text(stringResource(R.string.nav_permissions), fontSize = 10.sp) },
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
            .padding(top = 56.dp, start = 14.dp, end = 14.dp)
    ) {
        if (toast != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDarkSurface.copy(alpha = 0.94f))
                    .border(1.dp, GoldBright.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GoldBright)
                )
                Column {
                    Text(
                        text = toast.first,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (toast.second.isNotBlank()) {
                        Text(
                            text = toast.second,
                            style = MaterialTheme.typography.bodySmall,
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
