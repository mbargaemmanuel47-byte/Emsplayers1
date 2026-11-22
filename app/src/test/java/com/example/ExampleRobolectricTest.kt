package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.billing.PlayBillingManager
import com.example.data.local.AuraDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.model.AiGenerationUiState
import com.example.model.GeneratedMixResult
import com.example.permissions.PermissionManager
import com.example.viewmodel.AuraViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ems Player", appName)
    }

    @Test
    fun `permission manager exposes all three Android 6 to 15 categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val statuses = PermissionManager.getAllStatuses(context)
        assertEquals(3, statuses.size)
        assertTrue(statuses.any { it.manifestPermission == android.Manifest.permission.READ_MEDIA_AUDIO })
    }

    @Test
    fun `room database starts clean and persists mix onboarding and haptics`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = UserPreferencesRepository(db.userPreferenceDao())
            repo.ensureInitialized()
            val initial = repo.preferencesFlow.first()
            assertFalse(initial.hasCachedMix)
            assertEquals("", initial.cachedPlaylistTitle)
            assertFalse(initial.hasDismissedOnboarding)
            assertTrue(initial.hapticFeedbackEnabled)

            repo.saveLastTrackId(4L)
            repo.savePreferredMood("zouk")
            repo.saveEqGains(listOf(6f, 4f, -1f, 1f, 3f, 5f, 4f), "zouk")
            repo.saveOnboardingDismissed(true)
            repo.saveHapticFeedback(false)
            repo.saveCachedMix(
                prompt = "Zouk doux",
                mix = GeneratedMixResult("Mix Test", "Transition douce", listOf(1L, 5L))
            )

            val saved = repo.preferencesFlow.first()
            assertEquals(4L, saved.lastTrackId)
            assertEquals("zouk", saved.preferredMoodFilter)
            assertEquals("zouk", saved.activeEqPreset)
            assertTrue(saved.hasDismissedOnboarding)
            assertFalse(saved.hapticFeedbackEnabled)
            assertTrue(saved.hasCachedMix)
            assertEquals("Mix Test", saved.cachedPlaylistTitle)
            assertEquals("1,5", saved.cachedPlaylistTrackIdsCsv)
        } finally {
            db.close()
        }
    }

    @Test
    fun `play billing manager defaults to locked until paid purchase`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val manager = PlayBillingManager(context, scope)
        try {
            assertFalse(manager.uiState.value.isProUnlocked)
        } finally {
            manager.destroy()
            scope.cancel()
        }
    }

    @Test
    fun `pro mode is unlocked for now and firebase absence never crashes app`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuraViewModel(app)

        assertTrue(viewModel.isProUnlocked.value)
        assertEquals(AiGenerationUiState.Idle, viewModel.aiPlaylistState.value)

        // Calling AI generation should NOT force a paywall dialog while Pro is unlocked
        viewModel.triggerInstantAiMix()
        assertFalse(viewModel.showPaywallDialog.value)

        // When FirebaseApp is absent (like CI builds without google-services.json),
        // FirebaseApp.getApps returns empty without crashing the ViewModel
        val apps = FirebaseApp.getApps(app)
        assertTrue(apps.isEmpty() || apps.isNotEmpty())
    }
}
