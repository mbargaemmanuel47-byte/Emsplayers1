package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val lastTrackId: Long = 1L,
    val preferredMoodFilter: String = "all",
    val hasCachedMix: Boolean = false,
    val lastAiPrompt: String = "",
    val cachedPlaylistTitle: String = "",
    val cachedPlaylistRationale: String = "",
    val cachedPlaylistTrackIdsCsv: String = "",
    val eqGainsCsv: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0",
    val activeEqPreset: String = "flat",
    val crossfadeEnabled: Boolean = true,
    val normalizeEnabled: Boolean = true,
    val spatial3dEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val proactiveNotificationsEnabled: Boolean = true,
    val lastProactiveNotifAtMs: Long = 0L,
    val lastProactiveNotifSlot: String = "",
    val lastProactiveNotifMessage: String = "",
    val hasDismissedOnboarding: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)
