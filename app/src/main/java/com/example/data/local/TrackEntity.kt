package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.LyricLine
import com.example.model.SynthProfile
import com.example.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
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
    val scaleHzCsv: String,
    val bassHzCsv: String,
    val waveType: String,
    val rhythmCsv: String,
    val groove: Int,
    val warmth: Int,
    val clarity: Int,
    val aiInsight: String,
    val lyricsEncoded: String,
    val localUri: String? = null,
    val isAiGenerated: Boolean = false,
    val sortOrder: Int = 0,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val bitrateKbps: Int = 0,
    val mimeFormat: String = "",
    val year: String = ""
) {
    fun toDomain(): Track {
        val parsedLyrics = if (lyricsEncoded.isBlank()) {
            emptyList()
        } else {
            lyricsEncoded.split("\n").mapNotNull { line ->
                val parts = line.split("||", limit = 2)
                if (parts.size == 2) {
                    LyricLine(parts[0].toIntOrNull() ?: 0, parts[1])
                } else null
            }
        }
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            mood = mood,
            bpm = bpm,
            musicalKey = musicalKey,
            energy = energy,
            durationSec = durationSec,
            isLiked = isLiked,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex,
            tertiaryColorHex = tertiaryColorHex,
            synthProfile = SynthProfile(
                scaleHz = scaleHzCsv.split(",").mapNotNull { it.toDoubleOrNull() },
                bassHz = bassHzCsv.split(",").mapNotNull { it.toDoubleOrNull() },
                waveType = waveType,
                rhythmPattern = rhythmCsv.split(",").mapNotNull { it.toFloatOrNull() },
                groove = groove,
                warmth = warmth,
                clarity = clarity
            ),
            aiInsight = aiInsight,
            lyrics = parsedLyrics,
            localUri = localUri,
            isAiGenerated = isAiGenerated,
            playCount = playCount,
            lastPlayedAt = lastPlayedAt,
            fileSizeBytes = fileSizeBytes,
            bitrateKbps = bitrateKbps,
            mimeFormat = mimeFormat,
            year = year
        )
    }

    companion object {
        fun fromDomain(track: Track, sortOrder: Int = 0): TrackEntity {
            return TrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                genre = track.genre,
                mood = track.mood,
                bpm = track.bpm,
                musicalKey = track.musicalKey,
                energy = track.energy,
                durationSec = track.durationSec,
                isLiked = track.isLiked,
                primaryColorHex = track.primaryColorHex,
                secondaryColorHex = track.secondaryColorHex,
                tertiaryColorHex = track.tertiaryColorHex,
                scaleHzCsv = track.synthProfile.scaleHz.joinToString(","),
                bassHzCsv = track.synthProfile.bassHz.joinToString(","),
                waveType = track.synthProfile.waveType,
                rhythmCsv = track.synthProfile.rhythmPattern.joinToString(","),
                groove = track.synthProfile.groove,
                warmth = track.synthProfile.warmth,
                clarity = track.synthProfile.clarity,
                aiInsight = track.aiInsight,
                lyricsEncoded = track.lyrics.joinToString("\n") { "${it.timeSec}||${it.text}" },
                localUri = track.localUri,
                isAiGenerated = track.isAiGenerated,
                sortOrder = sortOrder,
                playCount = track.playCount,
                lastPlayedAt = track.lastPlayedAt,
                fileSizeBytes = track.fileSizeBytes,
                bitrateKbps = track.bitrateKbps,
                mimeFormat = track.mimeFormat,
                year = track.year
            )
        }
    }
}
