package com.example.model

data class LyricLine(
    val timeSec: Int,
    val text: String
)

data class SynthProfile(
    val scaleHz: List<Double>,
    val bassHz: List<Double>,
    val waveType: String, // "sine", "triangle", "sawtooth", "square"
    val rhythmPattern: List<Float>,
    val groove: Int,
    val warmth: Int,
    val clarity: Int
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
    val synthProfile: SynthProfile,
    val aiInsight: String,
    val lyrics: List<LyricLine>,
    val localUri: String? = null,
    val isAiGenerated: Boolean = false
)

data class AiMoodPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val bpmRange: String,
    val badge: String,
    val moodFilter: String,
    val trackId: Long
)

data class EqBandState(
    val index: Int,
    val frequencyHz: Int,
    val label: String,
    val gainDb: Float
)

data class AudioSettingsState(
    val eqGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val activePreset: String = "flat",
    val crossfadeEnabled: Boolean = true,
    val normalizeEnabled: Boolean = true,
    val spatial3dEnabled: Boolean = false
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
