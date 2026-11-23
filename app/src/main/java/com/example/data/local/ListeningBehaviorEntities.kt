package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.SavedPlaylist

@Entity(tableName = "listening_events")
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: Long,
    val trackTitle: String,
    val trackArtist: String,
    val trackAlbum: String,
    val genre: String,
    val playedAtMs: Long,
    val hourOfDay: Int,
    val timeSlot: String,
    val dayOfWeek: Int
)

@Entity(
    tableName = "track_transitions",
    primaryKeys = ["fromTrackId", "toTrackId"]
)
data class TrackTransitionEntity(
    val fromTrackId: Long,
    val fromTitle: String,
    val toTrackId: Long,
    val toTitle: String,
    val transitionCount: Int,
    val lastTransitionAtMs: Long
)

@Entity(tableName = "saved_playlists")
data class SavedPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val trackIdsCsv: String,
    val createdAtMs: Long,
    val isAiGenerated: Boolean = false,
    val playCount: Int = 0
) {
    fun toDomain(): SavedPlaylist {
        val ids = trackIdsCsv
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        return SavedPlaylist(
            id = id,
            title = title,
            description = description,
            trackIds = ids,
            createdAtMs = createdAtMs,
            isAiGenerated = isAiGenerated,
            playCount = playCount
        )
    }
}
