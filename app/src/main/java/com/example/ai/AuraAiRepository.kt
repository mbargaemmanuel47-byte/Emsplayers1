package com.example.ai

import android.content.Context
import com.example.model.ChatMessage
import com.example.model.GeneratedMixResult
import com.example.model.ListeningBehaviorProfile
import com.example.model.ProactiveNotificationDraft
import com.example.model.Track
import kotlinx.coroutines.flow.Flow

internal const val EMMA_SYSTEM_INSTRUCTION = MusicAssistantManager.EMMA_SYSTEM_INSTRUCTION
internal const val EMMA_PLAYLIST_INSTRUCTION = MusicAssistantManager.EMMA_PLAYLIST_INSTRUCTION

class AuraAiRepository(appContext: Context) {

    private val assistantManager = MusicAssistantManager(appContext)

    suspend fun generateCustomPlaylist(
        prompt: String,
        targetBpm: Int,
        emotionalIntensity: Int,
        catalog: List<Track>,
        behaviorProfile: ListeningBehaviorProfile? = null
    ): GeneratedMixResult {
        return assistantManager.generatePlaylist(
            prompt = prompt,
            targetBpm = targetBpm,
            emotionalIntensity = emotionalIntensity,
            catalog = catalog,
            behaviorProfile = behaviorProfile
        )
    }

    fun streamCopilotReply(
        userMessage: String,
        currentTrack: Track?,
        catalog: List<Track>,
        recentHistory: List<ChatMessage> = emptyList(),
        behaviorProfile: ListeningBehaviorProfile? = null
    ): Flow<String> {
        return assistantManager.streamChatReply(
            userMessage = userMessage,
            currentTrack = currentTrack,
            catalog = catalog,
            recentHistory = recentHistory,
            behaviorProfile = behaviorProfile
        )
    }

    suspend fun generateProactiveNotificationCopy(
        profile: ListeningBehaviorProfile
    ): ProactiveNotificationDraft? {
        return assistantManager.generateProactiveNotificationCopy(profile)
    }
}
