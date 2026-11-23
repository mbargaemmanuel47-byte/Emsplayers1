package com.example.data.local

import com.example.model.AudioSettingsState
import com.example.model.GeneratedMixResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val dao: UserPreferenceDao) {

    val preferencesFlow: Flow<UserPreferenceEntity> = dao.observePreferences()
        .map { it ?: UserPreferenceEntity() }

    val audioSettingsFlow: Flow<AudioSettingsState> = preferencesFlow.map { entity ->
        val gains = entity.eqGainsCsv
            .split(",")
            .mapNotNull { it.toFloatOrNull() }
            .takeIf { it.size == 7 }
            ?: listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)

        AudioSettingsState(
            eqGainsDb = gains,
            activePreset = entity.activeEqPreset,
            crossfadeEnabled = entity.crossfadeEnabled,
            normalizeEnabled = entity.normalizeEnabled,
            spatial3dEnabled = entity.spatial3dEnabled,
            hapticFeedbackEnabled = entity.hapticFeedbackEnabled,
            proactiveNotificationsEnabled = entity.proactiveNotificationsEnabled
        )
    }

    suspend fun ensureInitialized() {
        if (dao.getPreferencesOnce() == null) {
            dao.upsertPreferences(UserPreferenceEntity())
        }
    }

    suspend fun saveLastTrackId(trackId: Long) {
        ensureInitialized()
        dao.updateLastTrackId(trackId)
    }

    suspend fun savePreferredMood(mood: String) {
        ensureInitialized()
        dao.updatePreferredMood(mood)
    }

    suspend fun saveCachedMix(prompt: String, mix: GeneratedMixResult) {
        ensureInitialized()
        dao.updateCachedPlaylist(
            prompt = prompt,
            title = mix.title,
            rationale = mix.rationale,
            trackIdsCsv = mix.trackIds.joinToString(",")
        )
    }

    suspend fun saveEqGains(gains: List<Float>, presetName: String) {
        val current = dao.getPreferencesOnce() ?: UserPreferenceEntity()
        dao.upsertPreferences(
            current.copy(
                eqGainsCsv = gains.joinToString(","),
                activeEqPreset = presetName,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveCrossfade(enabled: Boolean) {
        val current = dao.getPreferencesOnce() ?: UserPreferenceEntity()
        dao.upsertPreferences(current.copy(crossfadeEnabled = enabled, updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun saveNormalize(enabled: Boolean) {
        val current = dao.getPreferencesOnce() ?: UserPreferenceEntity()
        dao.upsertPreferences(current.copy(normalizeEnabled = enabled, updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun saveSpatial3d(enabled: Boolean) {
        val current = dao.getPreferencesOnce() ?: UserPreferenceEntity()
        dao.upsertPreferences(current.copy(spatial3dEnabled = enabled, updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun saveHapticFeedback(enabled: Boolean) {
        val current = dao.getPreferencesOnce() ?: UserPreferenceEntity()
        dao.upsertPreferences(current.copy(hapticFeedbackEnabled = enabled, updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun saveOnboardingDismissed(dismissed: Boolean) {
        ensureInitialized()
        dao.updateOnboardingDismissed(dismissed)
    }

    suspend fun saveProactiveNotificationsEnabled(enabled: Boolean) {
        ensureInitialized()
        dao.updateProactiveNotificationsEnabled(enabled)
    }

    suspend fun recordProactiveNotificationSent(sentAtMs: Long, slot: String, message: String) {
        ensureInitialized()
        dao.recordProactiveNotificationSent(sentAtMs, slot, message)
    }
}
