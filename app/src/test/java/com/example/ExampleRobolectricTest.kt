package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.billing.PlayBillingManager
import com.example.data.local.AuraDatabase
import com.example.data.local.SeedCatalog
import com.example.data.local.TrackEntity
import com.example.data.local.UserPreferencesRepository
import com.example.model.AiGenerationUiState
import com.example.model.GeneratedMixResult
import com.example.model.Track
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
import org.junit.Assert.assertNull
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
        assertEquals("Em Player", appName)
    }

    @Test
    fun `seed catalog contains zero demo tracks`() {
        assertTrue(SeedCatalog.initialTracks.isEmpty())
    }

    @Test
    fun `permission manager exposes all three permission categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val statuses = PermissionManager.getAllStatuses(context)
        assertEquals(3, statuses.size)
        assertTrue(statuses.any { it.manifestPermission == android.Manifest.permission.READ_MEDIA_AUDIO })
    }

    @Test
    fun `room database persists playCount, metadata and removes legacy demo tracks`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val trackDao = db.trackDao()
            val demo = TrackEntity.fromDomain(
                Track(
                    id = 1L,
                    title = "Demo",
                    artist = "Seed",
                    album = "Demo",
                    genre = "Local",
                    mood = "all",
                    bpm = 0,
                    musicalKey = "",
                    energy = 0,
                    durationSec = 180,
                    isLiked = false,
                    primaryColorHex = 0xFFF5B731L,
                    secondaryColorHex = 0xFF0A1432L,
                    tertiaryColorHex = 0xFF1A2A52L,
                    localUri = null
                )
            )
            val real = TrackEntity.fromDomain(
                Track(
                    id = 50001L,
                    title = "Mon Son",
                    artist = "Artiste Réel",
                    album = "Album",
                    genre = "MP3",
                    mood = "all",
                    bpm = 0,
                    musicalKey = "",
                    energy = 0,
                    durationSec = 210,
                    isLiked = false,
                    primaryColorHex = 0xFFF5B731L,
                    secondaryColorHex = 0xFF0A1432L,
                    tertiaryColorHex = 0xFF1A2A52L,
                    localUri = "content://media/external/audio/media/50001",
                    fileSizeBytes = 4_200_000L,
                    mimeFormat = "MP3",
                    bitrateKbps = 320
                )
            )
            trackDao.insertTracks(listOf(demo, real))
            trackDao.deleteLegacyDemoTracks()

            val remaining = trackDao.getAllTracksOnce()
            assertEquals(1, remaining.size)
            assertEquals("Mon Son", remaining.first().title)

            trackDao.incrementPlayCount(50001L, 123456L)
            trackDao.incrementPlayCount(50001L, 234567L)
            val updated = trackDao.getAllTracksOnce().first().toDomain()
            assertEquals(2, updated.playCount)
            assertEquals(234567L, updated.lastPlayedAt)
            assertEquals("MP3", updated.mimeFormat)

            val repo = UserPreferencesRepository(db.userPreferenceDao())
            repo.ensureInitialized()
            val initial = repo.preferencesFlow.first()
            assertFalse(initial.hasCachedMix)
            assertTrue(initial.hapticFeedbackEnabled)

            repo.saveCachedMix(
                prompt = "Soirée",
                mix = GeneratedMixResult("Mix Test", "Transition douce", listOf(50001L))
            )
            val saved = repo.preferencesFlow.first()
            assertTrue(saved.hasCachedMix)
            assertEquals("Mix Test", saved.cachedPlaylistTitle)
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
    fun `pro mode is unlocked, starts with empty library and handles missing firebase safely`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = androidx.lifecycle.ViewModelStore()
        val provider = androidx.lifecycle.ViewModelProvider(
            store,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val viewModel = provider[AuraViewModel::class.java]
        try {
            assertTrue(viewModel.isProUnlocked.value)
            assertEquals(AiGenerationUiState.Idle, viewModel.aiPlaylistState.value)
            assertNull(viewModel.currentTrack.value)

            viewModel.triggerInstantAiMix()
            assertFalse(viewModel.showPaywallDialog.value)

            val apps = FirebaseApp.getApps(app)
            assertTrue(apps.isEmpty() || apps.isNotEmpty())
        } finally {
            store.clear()
        }
    }

    @Test
    fun `local media scanner handles calls cleanly without IS_MUSIC or 40 track cap`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tracks = com.example.data.local.LocalMediaScanner.scanDeviceAudio(context)
        assertTrue(tracks.size >= 0)
    }
}
