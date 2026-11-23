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
    val hapticFeedbackEnabled: Boolean = true,
    val proactiveNotificationsEnabled: Boolean = true
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

enum class ListeningTimeSlot(
    val code: String,
    val label: String,
    val hoursLabel: String
) {
    MATIN("MATIN", "Matin", "06h–12h"),
    APRES_MIDI("APRES_MIDI", "Après-midi", "12h–18h"),
    SOIREE("SOIREE", "Soirée", "18h–23h"),
    NUIT("NUIT", "Nuit", "23h–06h");

    companion object {
        fun fromHour(hourOfDay: Int): ListeningTimeSlot {
            val h = ((hourOfDay % 24) + 24) % 24
            return when (h) {
                in 6..11 -> MATIN
                in 12..17 -> APRES_MIDI
                in 18..22 -> SOIREE
                else -> NUIT
            }
        }

        fun approachingFromHour(hourOfDay: Int): ListeningTimeSlot {
            return fromHour((hourOfDay + 1) % 24)
        }

        fun fromCode(code: String): ListeningTimeSlot {
            return entries.firstOrNull { it.code == code } ?: SOIREE
        }
    }
}

data class SavedPlaylist(
    val id: Long,
    val title: String,
    val description: String,
    val trackIds: List<Long>,
    val createdAtMs: Long,
    val isAiGenerated: Boolean,
    val playCount: Int
)

data class TrackTransitionSummary(
    val fromTrackId: Long,
    val fromTitle: String,
    val toTrackId: Long,
    val toTitle: String,
    val count: Int
)

data class TimeSlotHabit(
    val slot: ListeningTimeSlot,
    val playCount: Int,
    val topTrackId: Long?,
    val topTrackTitle: String?,
    val topTrackArtist: String?,
    val peakHour: Int?
)

data class ListeningBehaviorProfile(
    val totalTrackedPlays: Int = 0,
    val totalLibraryTracks: Int = 0,
    val topTracks: List<Track> = emptyList(),
    val likedTracks: List<Track> = emptyList(),
    val playlists: List<SavedPlaylist> = emptyList(),
    val topTransitions: List<TrackTransitionSummary> = emptyList(),
    val slotHabits: List<TimeSlotHabit> = emptyList(),
    val currentSlot: ListeningTimeSlot = ListeningTimeSlot.SOIREE,
    val approachingSlot: ListeningTimeSlot = ListeningTimeSlot.SOIREE,
    val dominantHabitForWindow: TimeSlotHabit? = null,
    val systemPromptBlock: String = "",
    val compactBadgeSummary: String = "En attente de vos premières écoutes"
)

data class ProactiveNotificationDraft(
    val message: String,
    val suggestedTrackId: Long? = null,
    val suggestedPlaylistId: Long? = null,
    val slot: ListeningTimeSlot
)

sealed interface AiGenerationUiState<out T> {
    data object Idle : AiGenerationUiState<Nothing>
    data object Loading : AiGenerationUiState<Nothing>
    data class Success<T>(val data: T) : AiGenerationUiState<T>
    data class Error(val message: String) : AiGenerationUiState<Nothing>
}
