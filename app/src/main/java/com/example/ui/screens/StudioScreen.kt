package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiGenerationUiState
import com.example.model.ChatMessage
import com.example.model.GeneratedMixResult
import com.example.model.ListeningBehaviorProfile
import com.example.model.Track
import com.example.ui.components.CompactGoldSlider
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
    behaviorProfile: ListeningBehaviorProfile,
    proactiveNotificationsEnabled: Boolean,
    aiPlaylistState: AiGenerationUiState<GeneratedMixResult>,
    chatMessages: List<ChatMessage>,
    isCopilotTyping: Boolean,
    isProUnlocked: Boolean,
    topOverlayPadding: Dp,
    bottomOverlayPadding: Dp,
    onOpenPaywall: () -> Unit,
    onGeneratePlaylist: (String, Int, Int) -> Unit,
    onPlayGeneratedMix: () -> Unit,
    onPlayTrackById: (Long) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onToggleProactiveNotifications: (Boolean) -> Unit,
    onTriggerProactiveNotificationTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptText by rememberSaveable {
        mutableStateOf("Ambiance douce et rythmée pour écouter maintenant")
    }
    var targetBpm by rememberSaveable { mutableFloatStateOf(102f) }
    var emotionalIntensity by rememberSaveable { mutableFloatStateOf(75f) }
    var chatInput by rememberSaveable { mutableStateOf("") }
    var activeTab by rememberSaveable { mutableStateOf("chat") }
    var showInsights by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 820.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Emma • Compagne IA",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${behaviorProfile.currentSlot.label} • ${behaviorProfile.compactBadgeSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = GoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!isProUnlocked) {
                item {
                    ProLockedBannerCard(onUnlockClick = onOpenPaywall)
                }
            }

            item {
                EmmaBehaviorSummaryCard(
                    profile = behaviorProfile,
                    proactiveEnabled = proactiveNotificationsEnabled,
                    expanded = showInsights,
                    onToggleExpand = { showInsights = !showInsights },
                    onToggleProactive = onToggleProactiveNotifications,
                    onTestAlert = onTriggerProactiveNotificationTest
                )
            }

            if (!isWide) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("chat" to "Chat avec Emma", "mix" to "Mix IA").forEach { (tabKey, label) ->
                            val selected = activeTab == tabKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (selected) GoldBright else Color.Transparent)
                                    .clickable { activeTab = tabKey }
                                    .padding(vertical = 8.dp)
                                    .testTag("studio_tab_$tabKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Obsidian else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        CopilotChatCard(
                            messages = chatMessages,
                            profile = behaviorProfile,
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
                            modifier = Modifier.weight(1.05f)
                        )
                        Column(
                            modifier = Modifier.weight(0.95f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    }
                }
            } else if (activeTab == "chat") {
                item {
                    CopilotChatCard(
                        messages = chatMessages,
                        profile = behaviorProfile,
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
            }
        }
    }
}

@Composable
private fun EmmaBehaviorSummaryCard(
    profile: ListeningBehaviorProfile,
    proactiveEnabled: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleProactive: (Boolean) -> Unit,
    onTestAlert: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .testTag("emma_insights_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Insights,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mémoire musicale d'Emma",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profile.dominantHabitForWindow?.topTrackTitle?.let {
                        "Habitude ${profile.dominantHabitForWindow.slot.label.lowercase()} : $it"
                    } ?: "Analyse continue de vos écoutes, favoris et horaires",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Réduire" else "Développer",
                tint = GoldLight,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile.topTracks.isNotEmpty()) {
                    Text(
                        text = "Top écoutes : ${profile.topTracks.take(3).joinToString(", ") { "${it.title} (${it.playCount})" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                if (profile.topTransitions.isNotEmpty()) {
                    val topSeq = profile.topTransitions.first()
                    Text(
                        text = "Enchaînement fréquent : ${topSeq.fromTitle} → ${topSeq.toTitle} (${topSeq.count}x)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = EmeraldAccent
                    )
                }
                if (profile.playlists.isNotEmpty()) {
                    Text(
                        text = "Playlists mémorisées : ${profile.playlists.take(3).joinToString(", ") { it.title }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = GoldLight
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Alertes proactives d'Emma",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Suggestions personnalisées selon vos créneaux",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = proactiveEnabled,
                        onCheckedChange = onToggleProactive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Obsidian,
                            checkedTrackColor = GoldBright,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("studio_proactive_switch")
                    )
                }

                OutlinedButton(
                    onClick = onTestAlert,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("emma_test_notification_btn")
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recevoir une suggestion maintenant", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProLockedBannerCard(onUnlockClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(DeepBlue, CardDarkSurface)))
            .border(1.dp, BorderGold, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Em Player Pro",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Débloquez la création de mixes et l'assistant.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = onUnlockClick,
            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("studio_unlock_pro_btn")
        ) {
            Text("Débloquer", fontSize = 11.sp, maxLines = 1, softWrap = false)
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
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderGold, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Créer une playlist", style = MaterialTheme.typography.titleSmall, color = Color.White)

        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChange,
            minLines = 2,
            maxLines = 3,
            placeholder = { Text("Décrivez l'ambiance souhaitée...", fontSize = 12.sp, color = TextMuted) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldBright,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = Obsidian.copy(alpha = 0.78f),
                unfocusedContainerColor = Obsidian.copy(alpha = 0.78f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_prompt_input")
        )

        val quickTags = listOf("Détente", "Énergie", "Soirée", "Concentration")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            if (!promptText.contains(tag, ignoreCase = true)) {
                                onPromptChange(
                                    if (promptText.isBlank()) tag else "${promptText.trim()}, $tag"
                                )
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("+ $tag", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, color = TextSecondary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rythme",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    Text(
                        text = "${targetBpm.toInt()} BPM",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = GoldBright
                    )
                }
                CompactGoldSlider(
                    value = targetBpm,
                    onValueChange = onBpmChange,
                    valueRange = 75f..140f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intensité",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    Text(
                        text = "${emotionalIntensity.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = GoldBright
                    )
                }
                CompactGoldSlider(
                    value = emotionalIntensity,
                    onValueChange = onIntensityChange,
                    valueRange = 20f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = onGenerate,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("studio_generate_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Obsidian, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Création en cours...", fontSize = 12.sp)
            } else {
                Icon(
                    imageVector = if (isProUnlocked) Icons.Filled.AutoAwesome else Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Créer la playlist", fontSize = 12.sp)
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
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (state) {
            is AiGenerationUiState.Idle -> {
                Text("Votre sélection apparaîtra ici.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            is AiGenerationUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GoldBright, strokeWidth = 2.dp)
                    Text("Préparation de la sélection...", style = MaterialTheme.typography.bodySmall, color = GoldLight)
                }
            }
            is AiGenerationUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseError.copy(alpha = 0.14f))
                        .border(1.dp, RoseError.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("Impossible de créer le mix", style = MaterialTheme.typography.labelMedium, color = RoseError)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
            is AiGenerationUiState.Success -> {
                val mix = state.data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mix.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mix.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("studio_play_mix_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lire", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }

                mix.trackIds.forEachIndexed { idx, id ->
                    val track = tracks.find { it.id == id }
                    if (track != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { onPlayTrackById(track.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, color = GoldBright, maxLines = 1, softWrap = false)
                            TrackArtworkCanvas(track = track, cornerRadius = 7.dp, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
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
                                maxLines = 1,
                                softWrap = false,
                                color = TextMuted
                            )
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
    profile: ListeningBehaviorProfile,
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
    val chatScrollState = rememberScrollState()
    val lastText = messages.lastOrNull()?.text.orEmpty()
    LaunchedEffect(messages.size, lastText.length) {
        chatScrollState.animateScrollTo(chatScrollState.maxValue)
    }

    val quickSuggestions = buildList {
        add("Quel titre pour ce ${profile.currentSlot.label.lowercase()} ?")
        add("Analyse mes habitudes d'écoute")
        if (profile.topTransitions.isNotEmpty()) add("Suite après mon dernier son")
        add("Mes favoris")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardDarkSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                )
                Text(
                    text = "Emma • IA Musicale",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (isProUnlocked) "Effacer" else "Pro",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clickable(onClick = if (isProUnlocked) onClear else onOpenPaywall)
                    .padding(6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 280.dp)
                .verticalScroll(chatScrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.takeLast(14).forEach { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (msg.isUser) GoldBright else Color.White.copy(alpha = 0.07f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = msg.text.ifBlank { if (msg.isStreaming) "Emma réfléchit..." else "" },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (msg.isUser) Obsidian else Color.White.copy(alpha = 0.92f),
                            fontWeight = if (msg.isUser) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (msg.recommendedTrackId != null && !msg.isStreaming) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldBright)
                                    .clickable { onPlayRecommendedTrack(msg.recommendedTrackId) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("▶ Écouter", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, color = Obsidian)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickSuggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onQuickPrompt(suggestion) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(suggestion, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, color = GoldLight)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onChatInputChange,
                singleLine = true,
                placeholder = { Text("Parlez musique avec Emma...", fontSize = 12.sp, color = TextMuted) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldBright,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = Obsidian.copy(alpha = 0.78f),
                    unfocusedContainerColor = Obsidian.copy(alpha = 0.78f),
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
                    contentDescription = "Envoyer",
                    tint = Obsidian,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
