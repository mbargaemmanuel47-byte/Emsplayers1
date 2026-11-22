package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun observePreferences(): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(preferences: UserPreferenceEntity)

    @Query("UPDATE user_preferences SET lastTrackId = :trackId, updatedAtMs = :updatedAt WHERE id = 1")
    suspend fun updateLastTrackId(trackId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_preferences SET preferredMoodFilter = :mood, updatedAtMs = :updatedAt WHERE id = 1")
    suspend fun updatePreferredMood(mood: String, updatedAt: Long = System.currentTimeMillis())

    @Query(
        "UPDATE user_preferences SET hasCachedMix = 1, lastAiPrompt = :prompt, cachedPlaylistTitle = :title, " +
            "cachedPlaylistRationale = :rationale, cachedPlaylistTrackIdsCsv = :trackIdsCsv, " +
            "updatedAtMs = :updatedAt WHERE id = 1"
    )
    suspend fun updateCachedPlaylist(
        prompt: String,
        title: String,
        rationale: String,
        trackIdsCsv: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_preferences SET hasDismissedOnboarding = :dismissed, updatedAtMs = :updatedAt WHERE id = 1")
    suspend fun updateOnboardingDismissed(dismissed: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_preferences")
    suspend fun clearAll()
}
