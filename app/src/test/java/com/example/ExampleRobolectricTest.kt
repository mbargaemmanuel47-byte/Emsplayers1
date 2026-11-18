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
    fun `permission manager exposes all three Android 7 to 15 categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val statuses = PermissionManager.getAllStatuses(context)
        assertEquals(3, statuses.size)
        assertTrue(statuses.any { it.manifestPermission == android.Manifest.permission.READ_MEDIA_AUDIO })
    }

    @Test
    fun `room database starts clean without fake mix and persists real mix`() = runTest {
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

            repo.saveLastTrackId(4L)
            repo.savePreferredMood("zouk")
            repo.saveEqGains(listOf(6f, 4f, -1f, 1f, 3f, 5f, 4f), "zouk")
            repo.saveCachedMix(
                prompt = "Zouk doux",
                mix = GeneratedMixResult("Mix Test", "Transition douce", listOf(1L, 5L))
            )

            val saved = repo.preferencesFlow.first()
            assertEquals(4L, saved.lastTrackId)
            assertEquals("zouk", saved.preferredMoodFilter)
            assertEquals("zouk", saved.activeEqPreset)
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
    fun `ai music generation is blocked and opens paywall when unpaid`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuraViewModel(app)
        assertFalse(viewModel.isProUnlocked.value)
        assertEquals(AiGenerationUiState.Idle, viewModel.aiPlaylistState.value)

        viewModel.generateAiPlaylist("Zouk doux", 100, 70)
        assertTrue(viewModel.showPaywallDialog.value)
        assertEquals(AiGenerationUiState.Idle, viewModel.aiPlaylistState.value)

        viewModel.dismissPaywall()
        assertFalse(viewModel.showPaywallDialog.value)

        var callbackInvoked = false
        viewModel.triggerInstantAiMix { callbackInvoked = true }
        assertFalse(callbackInvoked)
        assertTrue(viewModel.showPaywallDialog.value)
    }
}
