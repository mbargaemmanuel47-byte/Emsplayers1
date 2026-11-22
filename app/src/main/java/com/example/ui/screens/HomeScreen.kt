package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SeedCatalog
import com.example.model.AiMoodPreset
import com.example.model.Track
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
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalBlueAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    tracks: List<Track>,
    currentTrack: Track,
    isPlaying: Boolean,
    isProUnlocked: Boolean,
    hasDismissedOnboarding: Boolean,
    hapticEnabled: Boolean,
    selectedMood: String,
    onMoodSelected: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayTrackById: (Long) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDismissOnboarding: () -> Unit,
    onReopenOnboarding: () -> Unit,
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
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val (greetingPrefix, greetingHighlight, contextBadge) = when (hour) {
        in 5..11 -> Triple("Bonjour, démarrez votre matinée avec une clarté sonore ", "sur mesure.", "Mode Matinal • Clarté & Énergie Douce")
        in 12..17 -> Triple("Bon après-midi, boostez votre créativité avec ", "Ems Player.", "Mode Productivité • Audio Haute Fidélité")
        else -> Triple("Bonsoir, prêt pour une immersion nocturne ", "sur mesure ?", "Mode Nocturne • Zouk Velours & Synthwave")
    }

    val filteredTracks = remember(tracks, selectedMood) {
        if (selectedMood == "all") tracks else tracks.filter { it.mood == selectedMood }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        val presetColumns = if (maxWidth >= 700.dp) 4 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 0. 5-Second Visual Quick-Start Guide (dismissible & persisted in Room)
            item {
                AnimatedVisibility(
                    visible = !hasDismissedOnboarding,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    QuickStartOnboardingCard(
                        onDismiss = {
                            triggerHaptic()
                            onDismissOnboarding()
                        },
                        onStartFirstTrack = {
                            triggerHaptic()
                            tracks.firstOrNull()?.let(onPlayTrack)
                            onDismissOnboarding()
                        }
                    )
                }
            }

            // 1. Hero Banner + Active Acoustic Signature
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(DeepBlue.copy(alpha = 0.90f), Midnight, Obsidian)
                            )
                        )
                        .border(1.dp, BorderGold, RoundedCornerShape(26.dp))
                        .padding(20.dp)
                ) {
                    if (isWide) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroLeftContent(
                                contextBadge = contextBadge,
                                greetingPrefix = greetingPrefix,
                                greetingHighlight = greetingHighlight,
                                isProUnlocked = isProUnlocked,
                                showGuideChip = hasDismissedOnboarding,
                                onReopenGuide = {
                                    triggerHaptic()
                                    onReopenOnboarding()
                                },
                                onStartSession = {
                                    triggerHaptic()
                                    tracks.firstOrNull()?.let(onPlayTrack)
                                },
                                onOpenStudio = {
                                    triggerHaptic()
                                    onOpenStudio()
                                },
                                modifier = Modifier.weight(1.4f)
                            )
                            AcousticSignatureCard(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            HeroLeftContent(
                                contextBadge = contextBadge,
                                greetingPrefix = greetingPrefix,
                                greetingHighlight = greetingHighlight,
                                isProUnlocked = isProUnlocked,
                                showGuideChip = hasDismissedOnboarding,
                                onReopenGuide = {
                                    triggerHaptic()
                                    onReopenOnboarding()
                                },
                                onStartSession = {
                                    triggerHaptic()
                                    tracks.firstOrNull()?.let(onPlayTrack)
                                },
                                onOpenStudio = {
                                    triggerHaptic()
                                    onOpenStudio()
                                }
                            )
                            AcousticSignatureCard(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying
                            )
                        }
                    }
                }
            }

        // 2. Mood & Energy Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FILTRER PAR HUMEUR & ÉNERGIE",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    val label = when (selectedMood) {
                        "focus" -> "Focus & Flow"
                        "zouk" -> "Zouk & R&B Nocturne"
                        "studio" -> "Énergie Studio"
                        "chill" -> "Chill & Synthwave"
                        else -> "Toutes les ambiances"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright
                    )
                }

                val pills = listOf(
                    "all" to "✨ Tout explorer",
                    "focus" to "🧠 Focus & Flow",
                    "zouk" to "🌙 Zouk & R&B",
                    "studio" to "⚡ Studio",
                    "chill" to "🌌 Synthwave"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pills.forEach { (key, title) ->
                        val active = selectedMood == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (active) GoldBright else Color.White.copy(alpha = 0.06f))
                                .border(
                                    1.dp,
                                    if (active) GoldBright else BorderSubtle,
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable {
                                    triggerHaptic()
                                    onMoodSelected(key)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("mood_pill_$key")
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Obsidian else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. 4 Preset Mood Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ambiances Audio Intégrées",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                val presets = SeedCatalog.moodPresets
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    presets.chunked(presetColumns).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                MoodPresetCard(
                                    preset = preset,
                                    onClick = {
                                        triggerHaptic()
                                        onPlayTrackById(preset.trackId)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowPresets.size < presetColumns) {
                                repeat(presetColumns - rowPresets.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Track Catalog Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkSurface.copy(alpha = 0.85f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Catalogue & Pistes Locales",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "48KHZ DSP",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldBright.copy(alpha = 0.15f))
                                .border(1.dp, BorderGold, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Touchez une piste pour démarrer le moteur audio natif",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Text(
                    text = "Voir tout",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldBright,
                    modifier = Modifier
                        .clickable(onClick = onOpenLibrary)
                        .padding(8.dp)
                )
            }
        }

        // Lazy track rows for fast first frame draw
        items(filteredTracks, key = { it.id }) { track ->
            val isCurrent = track.id == currentTrack.id
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

        item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun QuickStartOnboardingCard(
    onDismiss: () -> Unit,
    onStartFirstTrack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(DeepBlue.copy(alpha = 0.92f), CardDarkSurface)))
            .border(1.dp, BorderGold, RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("quick_start_onboarding_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRISE EN MAIN EN 3 ÉTAPES",
                style = MaterialTheme.typography.labelMedium,
                color = GoldBright
            )
            Text(
                text = "Masquer",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("dismiss_onboarding_btn")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OnboardingStepPill("1. Écouter", "Touchez un titre ou importez vos MP3", Modifier.weight(1f))
            OnboardingStepPill("2. Sculpter", "Égaliseur 7 bandes & Audio 3D", Modifier.weight(1f))
            OnboardingStepPill("3. Studio IA", "Pro débloqué : mixes Gemini", Modifier.weight(1f))
        }
        Button(
            onClick = onStartFirstTrack,
            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Écouter le premier titre maintenant", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OnboardingStepPill(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Obsidian.copy(alpha = 0.65f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = GoldLight, fontWeight = FontWeight.Bold)
        Text(text = detail, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun HeroLeftContent(
    contextBadge: String,
    greetingPrefix: String,
    greetingHighlight: String,
    isProUnlocked: Boolean,
    showGuideChip: Boolean,
    onReopenGuide: () -> Unit,
    onStartSession: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GoldBright.copy(alpha = 0.12f))
                    .border(1.dp, BorderGold, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GoldBright)
                )
                Text(
                    text = contextBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showGuideChip) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guide ?",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldBright,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onReopenGuide)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("reopen_onboarding_btn")
                )
            }
        }

        Text(
            text = buildAnnotatedString {
                append(greetingPrefix)
                withStyle(SpanStyle(color = GoldBright, fontWeight = FontWeight.ExtraBold)) {
                    append(greetingHighlight)
                }
            },
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Text(
            text = "Ems Player lit vos fichiers locaux hors-ligne et synthétise des univers 48kHz avec égaliseur 7 bandes (Android 6 à 15). Studio IA Pro débloqué.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartSession,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldBright,
                    contentColor = Obsidian
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("hero_start_session_btn")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lancer la session", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onOpenStudio,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.testTag("hero_studio_btn")
            ) {
                Text(if (isProUnlocked) "Studio IA • PRO" else "Studio IA", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AcousticSignatureCard(
    currentTrack: Track,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val tempoValue = if (currentTrack.bpm > 0) "${currentTrack.bpm} BPM" else "Local"
    val energyValue = if (currentTrack.energy > 0) "${currentTrack.energy}%" else "Natif"
    val sourceLabel = when {
        currentTrack.localUri != null -> "FICHIER LOCAL"
        currentTrack.isAiGenerated -> "GÉNÉRÉ IA PRO"
        else -> "SYNTHÈSE 48KHZ"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Obsidian.copy(alpha = 0.65f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = if (isPlaying) "EN LECTURE" else "EN VEILLE",
                style = MaterialTheme.typography.labelMedium,
                color = if (isPlaying) EmeraldAccent else GoldBright,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = currentTrack.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SignatureMetricPill("Tempo", tempoValue, GoldLight, Modifier.weight(1f))
            SignatureMetricPill("Tonalité", currentTrack.musicalKey, Color.White, Modifier.weight(1f))
            SignatureMetricPill("Énergie", energyValue, EmeraldAccent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SignatureMetricPill(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MoodPresetCard(
    preset: AiMoodPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColors = when (preset.id) {
        "focus" -> listOf(RoyalBlueAccent.copy(alpha = 0.32f), GoldBright.copy(alpha = 0.14f))
        "zouk" -> listOf(GoldMetallic.copy(alpha = 0.34f), DeepBlue.copy(alpha = 0.55f))
        "studio" -> listOf(GoldBright.copy(alpha = 0.30f), IndigoAccent.copy(alpha = 0.45f))
        else -> listOf(IndigoAccent.copy(alpha = 0.36f), GoldMetallic.copy(alpha = 0.18f))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(accentColors))
            .border(1.dp, BorderGold, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("mood_card_${preset.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.badge,
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Obsidian.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GoldBright, GoldMetallic))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = preset.title,
                    tint = Obsidian,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = preset.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preset.bpmRange,
                style = MaterialTheme.typography.labelMedium,
                color = GoldBright
            )
        }
    }
}

@Composable
fun TrackListRow(
    track: Track,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) GoldBright.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (isCurrent) GoldBright.copy(alpha = 0.45f) else BorderSubtle,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onPlay)
            .padding(10.dp)
            .testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrackArtworkCanvas(
            track = track,
            cornerRadius = 10.dp,
            modifier = Modifier.size(48.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isCurrent) GoldLight else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val trackSubtitle = if (track.bpm > 0) {
                "${track.artist} • ${track.bpm} BPM • ${track.musicalKey}"
            } else {
                "${track.artist} • ${track.genre}"
            }
            Text(
                text = trackSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(track.durationSec),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )

        IconButton(
            onClick = onToggleLike,
            modifier = Modifier.testTag("like_track_${track.id}")
        ) {
            Icon(
                imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favori",
                tint = if (track.isLiked) RoseError else TextMuted
            )
        }
    }
}
