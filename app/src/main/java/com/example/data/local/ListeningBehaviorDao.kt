package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningBehaviorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListeningEvent(event: ListeningEventEntity): Long

    @Query("SELECT * FROM listening_events ORDER BY playedAtMs DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<ListeningEventEntity>>

    @Query("SELECT * FROM listening_events ORDER BY playedAtMs DESC LIMIT :limit")
    suspend fun getRecentEventsOnce(limit: Int): List<ListeningEventEntity>

    @Query("SELECT COUNT(*) FROM listening_events")
    suspend fun getEventCount(): Int

    @Query(
        "DELETE FROM listening_events WHERE id NOT IN " +
            "(SELECT id FROM listening_events ORDER BY playedAtMs DESC LIMIT :keepLatest)"
    )
    suspend fun pruneOldEvents(keepLatest: Int)

    @Query("SELECT * FROM track_transitions WHERE fromTrackId = :fromId AND toTrackId = :toId LIMIT 1")
    suspend fun getTransition(fromId: Long, toId: Long): TrackTransitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransition(transition: TrackTransitionEntity)

    @Query("SELECT * FROM track_transitions ORDER BY transitionCount DESC, lastTransitionAtMs DESC LIMIT :limit")
    fun observeTopTransitions(limit: Int): Flow<List<TrackTransitionEntity>>

    @Query("SELECT * FROM track_transitions ORDER BY transitionCount DESC, lastTransitionAtMs DESC LIMIT :limit")
    suspend fun getTopTransitionsOnce(limit: Int): List<TrackTransitionEntity>

    @Query("SELECT * FROM saved_playlists ORDER BY createdAtMs DESC")
    fun observeSavedPlaylists(): Flow<List<SavedPlaylistEntity>>

    @Query("SELECT * FROM saved_playlists ORDER BY createdAtMs DESC")
    suspend fun getSavedPlaylistsOnce(): List<SavedPlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: SavedPlaylistEntity): Long

    @Query("UPDATE saved_playlists SET playCount = playCount + 1 WHERE id = :playlistId")
    suspend fun incrementPlaylistPlayCount(playlistId: Long)

    @Query("DELETE FROM saved_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)
}
