package com.example.notifications

import android.content.Context
import com.example.ai.AuraAiRepository
import com.example.data.local.AuraDatabase
import com.example.data.local.ListeningBehaviorRepository
import com.example.data.local.UserPreferencesRepository
import com.example.model.ListeningBehaviorProfile
import com.example.model.ProactiveNotificationDraft
import com.example.permissions.PermissionManager

sealed interface ProactiveEvaluationOutcome {
    data class Notified(val draft: ProactiveNotificationDraft) : ProactiveEvaluationOutcome
    data object SkippedDisabled : ProactiveEvaluationOutcome
    data object SkippedOffline : ProactiveEvaluationOutcome
    data object SkippedNoPermission : ProactiveEvaluationOutcome
    data object SkippedCooldown : ProactiveEvaluationOutcome
    data object SkippedNoHabitSignal : ProactiveEvaluationOutcome
    data object SkippedAiUnavailable : ProactiveEvaluationOutcome
}

class ProactiveEmmaCoordinator(
    private val context: Context,
    private val behaviorRepository: ListeningBehaviorRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val preferenceDao: com.example.data.local.UserPreferenceDao,
    private val isOnlineProvider: (Context) -> Boolean = NetworkStatusHelper::isInternetAvailable,
    private val hasNotifPermissionProvider: (Context) -> Boolean = { ctx ->
        PermissionManager.isPermissionGranted(ctx, PermissionManager.notificationsPermissionOrNull())
    },
    private val copyGenerator: suspend (ListeningBehaviorProfile) -> ProactiveNotificationDraft?,
    private val notificationDispatcher: (Context, ProactiveNotificationDraft) -> Boolean = ProactiveNotificationHelper::showProactiveNotification,
    private val clockMs: () -> Long = System::currentTimeMillis
) {

    companion object {
        private const val COOLDOWN_MS = 4L * 60L * 60L * 1000L

        fun createDefault(context: Context): ProactiveEmmaCoordinator {
            val appCtx = context.applicationContext
            val db = AuraDatabase.getInstance(appCtx)
            val prefDao = db.userPreferenceDao()
            val behaviorRepo = ListeningBehaviorRepository(db.trackDao(), db.listeningBehaviorDao())
            val prefRepo = UserPreferencesRepository(prefDao)
            val aiRepo = AuraAiRepository(appCtx)
            return ProactiveEmmaCoordinator(
                context = appCtx,
                behaviorRepository = behaviorRepo,
                preferencesRepository = prefRepo,
                preferenceDao = prefDao,
                copyGenerator = { profile -> aiRepo.generateProactiveNotificationCopy(profile) }
            )
        }
    }

    suspend fun evaluateAndNotifyIfDue(forceTest: Boolean = false): ProactiveEvaluationOutcome {
        preferencesRepository.ensureInitialized()
        val prefs = preferenceDao.getPreferencesOnce()

        if (!forceTest && prefs != null && !prefs.proactiveNotificationsEnabled) {
            return ProactiveEvaluationOutcome.SkippedDisabled
        }
        if (!isOnlineProvider(context)) {
            return ProactiveEvaluationOutcome.SkippedOffline
        }
        if (!hasNotifPermissionProvider(context)) {
            return ProactiveEvaluationOutcome.SkippedNoPermission
        }

        val nowMs = clockMs()
        if (!forceTest && prefs != null && prefs.lastProactiveNotifAtMs > 0L) {
            val elapsed = nowMs - prefs.lastProactiveNotifAtMs
            if (elapsed in 0 until COOLDOWN_MS) {
                return ProactiveEvaluationOutcome.SkippedCooldown
            }
        }

        val profile = behaviorRepository.buildProfileSnapshot(nowMs)
        val hasHabitSignal = hasActionableHabitSignal(profile, forceTest)
        if (!hasHabitSignal) {
            return ProactiveEvaluationOutcome.SkippedNoHabitSignal
        }

        val draft = copyGenerator(profile)
            ?: return ProactiveEvaluationOutcome.SkippedAiUnavailable

        val posted = notificationDispatcher(context, draft)
        if (!posted) {
            return ProactiveEvaluationOutcome.SkippedNoPermission
        }

        preferencesRepository.recordProactiveNotificationSent(
            sentAtMs = nowMs,
            slot = draft.slot.code,
            message = draft.message
        )
        return ProactiveEvaluationOutcome.Notified(draft)
    }

    internal fun hasActionableHabitSignal(profile: ListeningBehaviorProfile, forceTest: Boolean): Boolean {
        if (forceTest) {
            return profile.totalLibraryTracks > 0 || profile.totalTrackedPlays > 0
        }
        val currentHabit = profile.slotHabits.firstOrNull { it.slot == profile.currentSlot }
        val approachingHabit = profile.slotHabits.firstOrNull { it.slot == profile.approachingSlot }
        val slotPatternDetected = (currentHabit?.playCount ?: 0) >= 2 || (approachingHabit?.playCount ?: 0) >= 2
        val sequenceOrPlaylistDetected = profile.topTransitions.isNotEmpty() || profile.playlists.isNotEmpty()
        val sufficientOverallActivity = profile.totalTrackedPlays >= 2 || profile.likedTracks.isNotEmpty()
        return slotPatternDetected || (sufficientOverallActivity && (sequenceOrPlaylistDetected || profile.topTracks.isNotEmpty()))
    }
}
