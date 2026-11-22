package com.example.model

data class LyricLine(
    val timeSec: Int,
    val text: String
)

data class SynthProfile(
    val scaleHz: List<Double> = emptyList(),
    val bassHz: List<Double> = emptyList(),
    val waveType: String = "pcm",
    val rhythmPattern: List<Float> = emptyList(),
    val groove: Int = 0,
    val warmth: Int = 0,
    val clarity: Int = 0
)

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val mood: String,
    val bpm: Int,
    val musicalKey: String,
    val energy: Int,
    val durationSec: Int,
    val isLiked: Boolean,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val tertiaryColorHex: Long,
    val synthProfile: SynthProfile = SynthProfile(),
    val aiInsight: String = "",
    val lyrics: List<LyricLine> = emptyList(),
    val localUri: String? = null,
    val isAiGenerated: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val bitrateKbps: Int = 0,
    val mimeFormat: String = "",
    val year: String = ""
)

data class AudioSettingsState(
    val eqGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val activePreset: String = "flat",
    val crossfadeEnabled: Boolean = true,
    val normalizeEnabled: Boolean = true,
    val spatial3dEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val recommendedTrackId: Long? = null,
    val isStreaming: Boolean = false
)

data class GeneratedMixResult(
    val title: String,
    val rationale: String,
    val trackIds: List<Long>
)

sealed interface AiGenerationUiState<out T> {
    data object Idle : AiGenerationUiState<Nothing>
    data object Loading : AiGenerationUiState<Nothing>
    data class Success<T>(val data: T) : AiGenerationUiState<T>
    data class Error(val message: String) : AiGenerationUiState<Nothing>
}
