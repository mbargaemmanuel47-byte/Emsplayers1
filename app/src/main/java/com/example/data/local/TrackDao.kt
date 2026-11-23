package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY sortOrder ASC, id DESC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isLiked = :liked WHERE id = :trackId")
    suspend fun updateLiked(trackId: Long, liked: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, playedAt: Long)

    @Query("UPDATE tracks SET bitrateKbps = :bitrateKbps, year = :year WHERE id = :trackId")
    suspend fun updateAudioDetails(trackId: Long, bitrateKbps: Int, year: String)

    @Query("UPDATE tracks SET durationSec = :durationSec WHERE id = :trackId")
    suspend fun updateDuration(trackId: Long, durationSec: Int)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: Long)

    @Query("DELETE FROM tracks WHERE localUri IS NULL")
    suspend fun deleteLegacyDemoTracks()
}
