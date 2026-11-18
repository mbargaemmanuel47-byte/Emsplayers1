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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiGenerationUiState
import com.example.model.ChatMessage
import com.example.model.GeneratedMixResult
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
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun StudioScreen(
    tracks: List<Track>,
    aiPlaylistState: AiGenerationUiState<GeneratedMixResult>,
    chatMessages: List<ChatMessage>,
    isCopilotTyping: Boolean,
    isProUnlocked: Boolean,
    onOpenPaywall: () -> Unit,
    onGeneratePlaylist: (String, Int, Int) -> Unit,
    onPlayGeneratedMix: () -> Unit,
    onPlayTrackById: (Long) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptText by rememberSaveable {
        mutableStateOf("R&B moderne avec rythme zouk doux, basse chaude et nappes nocturnes")
    }
    var targetBpm by rememberSaveable { mutableFloatStateOf(102f) }
    var emotionalIntensity by rememberSaveable { mutableFloatStateOf(75f) }
    var chatInput by rememberSaveable { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 820.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "STUDIO IA EMS PLAYER • GEMINI 3.8 FLASH",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright
                    )
                    Text(
                        text = "Studio & Co-Pilote Musical IA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (isProUnlocked) {
                            "Abonnement Pro actif : composez des mixes et remixes Gemini sur mesure."
                        } else {
                            "La génération musicale par IA est une fonctionnalité payante (Ems Player Pro)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (!isProUnlocked) {
                item {
                    ProLockedBannerCard(onUnlockClick = onOpenPaywall)
                }
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GeneratorPromptCard(
                                promptText = promptText,
                                onPromptChange = { promptText = it },
                                targetBpm = targetBpm,
                                onBpmChange = { targetBpm = it },
                                emotionalIntensity = emotionalIntensity,
                                onIntensityChange = { emotionalIntensity = it },
                                isProUnlocked = isProUnlocked,
                                isLoading = aiPlaylistState is AiGenerationUiState.Loading,
                                onGenerate = {
                                    if (isProUnlocked) {
                                        onGeneratePlaylist(promptText, targetBpm.toInt(), emotionalIntensity.toInt())
                                    } else {
                                        onOpenPaywall()
                                    }
                                }
                            )
                            GeneratedResultCard(
                                state = aiPlaylistState,
                                tracks = tracks,
                                onPlayAll = onPlayGeneratedMix,
                                onPlayTrackById = onPlayTrackById
                            )
                        }
                        CopilotChatCard(
                            messages = chatMessages,
                            isTyping = isCopilotTyping,
                            isProUnlocked = isProUnlocked,
                            chatInput = chatInput,
                            onChatInputChange = { chatInput = it },
                            onSend = {
                                if (isProUnlocked) {
                                    onSendChatMessage(chatInput)
                                    chatInput = ""
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            onQuickPrompt = { prompt ->
                                if (isProUnlocked) onSendChatMessage(prompt) else onOpenPaywall()
                            },
                            onPlayRecommendedTrack = onPlayTrackById,
                            onClear = onClearChat,
                            onOpenPaywall = onOpenPaywall,
                            modifier = Modifier.weight(0.85f)
                        )
                    }
                }
            } else {
                item {
                    GeneratorPromptCard(
                        promptText = promptText,
                        onPromptChange = { promptText = it },
                        targetBpm = targetBpm,
                        onBpmChange = { targetBpm = it },
                        emotionalIntensity = emotionalIntensity,
                        onIntensityChange = { emotionalIntensity = it },
                        isProUnlocked = isProUnlocked,
                        isLoading = aiPlaylistState is AiGenerationUiState.Loading,
                        onGenerate = {
                            if (isProUnlocked) {
                                onGeneratePlaylist(promptText, targetBpm.toInt(), emotionalIntensity.toInt())
                            } else {
                                onOpenPaywall()
                            }
                        }
                    )
                }
                item {
                    GeneratedResultCard(
                        state = aiPlaylistState,
                        tracks = tracks,
                        onPlayAll = onPlayGeneratedMix,
                        onPlayTrackById = onPlayTrackById
                    )
                }
                item {
                    CopilotChatCard(
                        messages = chatMessages,
                        isTyping = isCopilotTyping,
                        isProUnlocked = isProUnlocked,
                        chatInput = chatInput,
                        onChatInputChange = { chatInput = it },
                        onSend = {
                            if (isProUnlocked) {
                                onSendChatMessage(chatInput)
                                chatInput = ""
                            } else {
                                onOpenPaywall()
                            }
                        },
                        onQuickPrompt = { prompt ->
                            if (isProUnlocked) onSendChatMessage(prompt) else onOpenPaywall()
                        },
                        onPlayRecommendedTrack = onPlayTrackById,
                        onClear = onClearChat,
                        onOpenPaywall = onOpenPaywall
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProLockedBannerCard(onUnlockClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(DeepBlue, CardDarkSurface)))
            .border(1.dp, BorderGold, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GoldBright.copy(alpha = 0.16f))
                .border(1.dp, BorderGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Fonctionnalité Pro payante",
                tint = GoldBright,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Ems Player Pro Requis",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "La génération de musique, de remixes et le Co-Pilote IA sont réservés aux abonnés Google Play Pro.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Button(
            onClick = onUnlockClick,
            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("studio_unlock_pro_btn")
        ) {
            Text("Débloquer", style = MaterialTheme.typography.labelLarge, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GeneratorPromptCard(
    promptText: String,
    onPromptChange: (String) -> Unit,
    targetBpm: Float,
    onBpmChange: (Float) -> Unit,
    emotionalIntensity: Float,
    onIntensityChange: (Float) -> Unit,
    isProUnlocked: Boolean,
    isLoading: Boolean,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardDarkSurface.copy(alpha = 0.92f))
            .border(1.dp, BorderGold, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎹 Générateur de Playlist IA",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Gemini 3.8 Flash",
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight
            )
        }

        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChange,
            minLines = 2,
            maxLines = 4,
            placeholder = { Text("Décrivez une ambiance, un style ou un moment...", color = TextMuted) },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldBright,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = Obsidian.copy(alpha = 0.8f),
                unfocusedContainerColor = Obsidian.copy(alpha = 0.8f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_prompt_input")
        )

        val quickTags = listOf(
            "Zouk & Kompa doux",
            "Afrobeats & Amapiano",
            "Piano Rhodes & Saxo",
            "Synthwave Nocturne",
            "Lo-Fi Focus 90 BPM"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickTags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable {
                            if (!promptText.contains(tag, ignoreCase = true)) {
                                onPromptChange(
                                    if (promptText.isBlank()) tag else "${promptText.trim()}, $tag"
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ $tag",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tempo cible", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("${targetBpm.toInt()} BPM", style = MaterialTheme.typography.labelMedium, color = GoldBright)
                }
                Slider(
                    value = targetBpm,
                    onValueChange = onBpmChange,
                    valueRange = 75f..140f,
                    colors = SliderDefaults.colors(thumbColor = GoldBright, activeTrackColor = GoldBright)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Émotion", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("${emotionalIntensity.toInt()}%", style = MaterialTheme.typography.labelMedium, color = GoldBright)
                }
                Slider(
                    value = emotionalIntensity,
                    onValueChange = onIntensityChange,
                    valueRange = 20f..100f,
                    colors = SliderDefaults.colors(thumbColor = GoldBright, activeTrackColor = GoldBright)
                )
            }
        }

        Button(
            onClick = onGenerate,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("studio_generate_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Obsidian,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Composition IA en cours...", style = MaterialTheme.typography.labelLarge)
            } else if (!isProUnlocked) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Débloquer la Génération IA (Pro)", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Générer la Playlist Sur Mesure", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun GeneratedResultCard(
    state: AiGenerationUiState<GeneratedMixResult>,
    tracks: List<Track>,
    onPlayAll: () -> Unit,
    onPlayTrackById: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardDarkSurface.copy(alpha = 0.90f))
            .border(1.dp, BorderGold, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            is AiGenerationUiState.Idle -> {
                Text("Prêt à composer votre prochaine session IA.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            is AiGenerationUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GoldBright, strokeWidth = 2.dp)
                    Text("Analyse harmonique et sélection Gemini...", style = MaterialTheme.typography.bodyMedium, color = GoldLight)
                }
            }
            is AiGenerationUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoseError.copy(alpha = 0.14f))
                        .border(1.dp, RoseError.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text("Erreur Gemini AI Logic", style = MaterialTheme.typography.labelLarge, color = RoseError)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
            is AiGenerationUiState.Success -> {
                val mix = state.data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SESSION GÉNÉRÉE PAR EMS PLAYER IA", style = MaterialTheme.typography.labelSmall, color = GoldBright)
                        Text(mix.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("studio_play_mix_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tout jouer", style = MaterialTheme.typography.labelLarge, fontSize = 12.sp)
                    }
                }

                Text(
                    text = "💡 ${mix.rationale}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepBlue.copy(alpha = 0.5f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                )

                mix.trackIds.forEachIndexed { idx, id ->
                    val track = tracks.find { it.id == id }
                    if (track != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { onPlayTrackById(track.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("0${idx + 1}", style = MaterialTheme.typography.labelMedium, color = GoldBright)
                            TrackArtworkCanvas(track = track, cornerRadius = 8.dp, modifier = Modifier.size(38.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                Text("${track.artist} • ${track.genre}", style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, color = TextMuted)
                            }
                            Text(
                                text = if (track.bpm > 0) "${track.bpm} BPM" else "Local",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldLight
                            )
                            Text(formatDuration(track.durationSec), style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotChatCard(
    messages: List<ChatMessage>,
    isTyping: Boolean,
    isProUnlocked: Boolean,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onPlayRecommendedTrack: (Long) -> Unit,
    onClear: () -> Unit,
    onOpenPaywall: () -> Unit,
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldBright.copy(alpha = 0.16f))
                        .border(1.dp, BorderGold, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", style = MaterialTheme.typography.labelLarge, color = GoldBright)
                }
                Column {
                    Text("Co-Pilote DJ Aura", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(EmeraldAccent))
                        Text(
                            text = when {
                                !isProUnlocked -> "Verrouillé • Ems Player Pro requis"
                                isTyping -> "Génération en direct..."
                                else -> "En ligne • Gemini Streaming"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProUnlocked) EmeraldAccent else GoldBright
                        )
                    }
                }
            }
            Text(
                text = if (isProUnlocked) "Effacer" else "Pro",
                style = MaterialTheme.typography.labelMedium,
                color = if (isProUnlocked) TextMuted else GoldBright,
                modifier = Modifier
                    .clickable(onClick = if (isProUnlocked) onClear else onOpenPaywall)
                    .padding(8.dp)
            )
        }

        // Messages list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.takeLast(8).forEach { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (msg.isUser) GoldBright else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (msg.isUser) GoldBright else BorderSubtle,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = msg.text.ifBlank { if (msg.isStreaming) "Aura réfléchit..." else "" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isUser) Obsidian else TextSecondary,
                            fontWeight = if (msg.isUser) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (msg.recommendedTrackId != null && !msg.isStreaming) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GoldBright)
                                    .clickable { onPlayRecommendedTrack(msg.recommendedTrackId) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "▶ Lancer la piste recommandée",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 11.sp,
                                    color = Obsidian
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick chat suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Analyse le morceau actuel",
                "Crée une file d'attente Chill & Focus",
                "Recommande un titre Zouk R&B"
            ).forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { onQuickPrompt(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(suggestion, style = MaterialTheme.typography.labelMedium, color = GoldLight)
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onChatInputChange,
                singleLine = true,
                placeholder = { Text("Demandez à Ems Player...", color = TextMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldBright,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = Obsidian.copy(alpha = 0.8f),
                    unfocusedContainerColor = Obsidian.copy(alpha = 0.8f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_chat_input")
            )
            IconButton(
                onClick = onSend,
                enabled = (!isProUnlocked || chatInput.isNotBlank()) && !isTyping,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(GoldBright, GoldMetallic)))
                    .testTag("studio_chat_send_btn")
            ) {
                Icon(
                    imageVector = if (isProUnlocked) Icons.AutoMirrored.Filled.Send else Icons.Filled.Lock,
                    contentDescription = if (isProUnlocked) "Envoyer" else "Débloquer Pro",
                    tint = Obsidian
                )
            }
        }
    }
}
