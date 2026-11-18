package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AuraAiRepository
import com.example.audio.AuraAudioController
import com.example.audio.PlaybackNotificationBridge
import com.example.billing.BillingProductOption
import com.example.billing.BillingUiState
import com.example.billing.PlayBillingManager
import com.example.data.local.AuraDatabase
import com.example.data.local.LocalMediaScanner
import com.example.data.local.SeedCatalog
import com.example.data.local.TrackEntity
import com.example.data.local.UserPreferencesRepository
import com.example.model.AiGenerationUiState
import com.example.model.AudioSettingsState
import com.example.model.ChatMessage
import com.example.model.GeneratedMixResult
import com.example.model.Track
import com.example.permissions.AndroidPermissionStatus
import com.example.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = AuraDatabase.getInstance(appContext)
    private val trackDao = database.trackDao()
    private val preferencesRepo = UserPreferencesRepository(database.userPreferenceDao())
    private val aiRepository = AuraAiRepository()
    private val audioController = AuraAudioController(appContext, viewModelScope)
    private val billingManager = PlayBillingManager(appContext, viewModelScope)

    val billingState: StateFlow<BillingUiState> = billingManager.uiState
    val isProUnlocked: StateFlow<Boolean> = billingState
        .map { it.isProUnlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showPaywallDialog = MutableStateFlow(false)
    val showPaywallDialog: StateFlow<Boolean> = _showPaywallDialog.asStateFlow()

    private val _isRemixGenerating = MutableStateFlow(false)
    val isRemixGenerating: StateFlow<Boolean> = _isRemixGenerating.asStateFlow()

    val tracks: StateFlow<List<Track>> = trackDao.observeAllTracks()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeedCatalog.initialTracks)

    val audioSettings: StateFlow<AudioSettingsState> = preferencesRepo.audioSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioSettingsState())

    private val _currentTrackId = MutableStateFlow(1L)
    val currentTrack: StateFlow<Track> = combine(tracks, _currentTrackId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull() ?: SeedCatalog.initialTracks.first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SeedCatalog.initialTracks.first())

    val isPlaying: StateFlow<Boolean> = audioController.isPlaying
    val currentPositionSec: StateFlow<Float> = audioController.currentPositionSec
    val volume: StateFlow<Float> = audioController.volume
    val isMuted: StateFlow<Boolean> = audioController.isMuted
    val isShuffle: StateFlow<Boolean> = audioController.isShuffle
    val isRepeat: StateFlow<Boolean> = audioController.isRepeat
    val spectrumBars: StateFlow<List<Float>> = audioController.spectrumBars
    val dspStatus: StateFlow<String> = audioController.dspStatus

    private val _homeMoodFilter = MutableStateFlow("all")
    val homeMoodFilter: StateFlow<String> = _homeMoodFilter.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _aiPlaylistState = MutableStateFlow<AiGenerationUiState<GeneratedMixResult>>(
        AiGenerationUiState.Idle
    )
    val aiPlaylistState: StateFlow<AiGenerationUiState<GeneratedMixResult>> = _aiPlaylistState.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Bonjour ! Je suis Aura, votre co-pilote musical propulsé par Gemini. Débloquez Ems Player Pro pour composer des mixes et remixes IA sur mesure."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isCopilotTyping = MutableStateFlow(false)
    val isCopilotTyping: StateFlow<Boolean> = _isCopilotTyping.asStateFlow()

    private val _permissionStatuses = MutableStateFlow(PermissionManager.getAllStatuses(appContext))
    val permissionStatuses: StateFlow<List<AndroidPermissionStatus>> = _permissionStatuses.asStateFlow()

    private val _toastBanner = MutableStateFlow<Pair<String, String>?>(null)
    val toastBanner: StateFlow<Pair<String, String>?> = _toastBanner.asStateFlow()

    private var toastJob: Job? = null
    private var copilotJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (trackDao.getTrackCount() == 0) {
                val seeded = SeedCatalog.initialTracks.mapIndexed { idx, track ->
                    TrackEntity.fromDomain(track, sortOrder = idx + 10)
                }
                trackDao.insertTracks(seeded)
            }
            preferencesRepo.ensureInitialized()
            database.userPreferenceDao().getPreferencesOnce()?.let { saved ->
                _currentTrackId.value = saved.lastTrackId
                _homeMoodFilter.value = saved.preferredMoodFilter
                if (saved.hasCachedMix && saved.cachedPlaylistTitle.isNotBlank()) {
                    val cachedIds = saved.cachedPlaylistTrackIdsCsv
                        .split(",")
                        .mapNotNull { it.toLongOrNull() }
                    if (cachedIds.isNotEmpty()) {
                        _aiPlaylistState.value = AiGenerationUiState.Success(
                            GeneratedMixResult(
                                title = saved.cachedPlaylistTitle,
                                rationale = saved.cachedPlaylistRationale,
                                trackIds = cachedIds
                            )
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            audioSettings.collect { settings ->
                audioController.updateAudioSettings(settings)
            }
        }

        audioController.onTrackFinishedCallback = {
            viewModelScope.launch {
                nextTrack()
            }
        }

        audioController.onPlaybackErrorCallback = { message ->
            showToast("Erreur de lecture", message)
        }

        PlaybackNotificationBridge.onPlayPauseAction = {
            viewModelScope.launch { togglePlayPause() }
        }
        PlaybackNotificationBridge.onNextAction = {
            viewModelScope.launch { nextTrack() }
        }
        PlaybackNotificationBridge.onPrevAction = {
            viewModelScope.launch { previousTrack() }
        }
    }

    fun openPaywall() {
        _showPaywallDialog.value = true
    }

    fun dismissPaywall() {
        _showPaywallDialog.value = false
    }

    fun launchProPurchase(activity: Activity, option: BillingProductOption) {
        billingManager.launchPurchaseFlow(activity, option)
    }

    fun restoreProPurchases() {
        billingManager.refreshPurchases()
        showToast("Vérification Google Play", "Recherche de vos achats et abonnements actifs...")
    }

    fun refreshBilling() {
        billingManager.refreshPurchases()
    }

    private fun requireProEntitlement(): Boolean {
        if (billingState.value.isProUnlocked) return true
        _showPaywallDialog.value = true
        showToast(
            "Ems Player Pro requis",
            "La génération musicale par IA est réservée aux membres Pro."
        )
        return false
    }

    fun showToast(title: String, subtitle: String = "") {
        toastJob?.cancel()
        _toastBanner.value = title to subtitle
        toastJob = viewModelScope.launch {
            delay(2800L)
            _toastBanner.value = null
        }
    }

    fun playTrack(track: Track) {
        _currentTrackId.value = track.id
        audioController.playTrack(track)
        viewModelScope.launch {
            preferencesRepo.saveLastTrackId(track.id)
        }
        val tempoInfo = if (track.bpm > 0) "${track.bpm} BPM" else track.genre
        showToast("Lecture : ${track.title}", "${track.artist} • $tempoInfo")
    }

    fun playTrackById(trackId: Long) {
        val found = tracks.value.find { it.id == trackId } ?: return
        playTrack(found)
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioController.pause()
        } else {
            audioController.resume(currentTrack.value)
        }
    }

    fun nextTrack() {
        val list = tracks.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _currentTrackId.value }.coerceAtLeast(0)
        val nextIdx = if (isShuffle.value && list.size > 1) {
            val candidates = list.indices.filter { it != currentIdx }
            candidates.shuffled().first()
        } else {
            (currentIdx + 1) % list.size
        }
        playTrack(list[nextIdx])
    }

    fun previousTrack() {
        if (currentPositionSec.value > 4f) {
            audioController.seekToSeconds(0f)
            return
        }
        val list = tracks.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _currentTrackId.value }.coerceAtLeast(0)
        val prevIdx = (currentIdx - 1 + list.size) % list.size
        playTrack(list[prevIdx])
    }

    fun seekToFraction(fraction: Float) = audioController.seekToFraction(fraction)
    fun seekToSeconds(sec: Float) = audioController.seekToSeconds(sec)
    fun setVolume(vol: Float) = audioController.setVolume(vol)
    fun toggleMute() {
        audioController.toggleMute()
        showToast(if (isMuted.value) "Son coupé" else "Son rétabli")
    }
    fun toggleShuffle() {
        audioController.toggleShuffle()
        showToast(if (isShuffle.value) "Lecture aléatoire activée" else "Lecture aléatoire désactivée")
    }
    fun toggleRepeat() {
        audioController.toggleRepeat()
        showToast(if (isRepeat.value) "Répétition activée" else "Répétition désactivée")
    }

    fun toggleLike(track: Track) {
        val nextLiked = !track.isLiked
        viewModelScope.launch {
            trackDao.updateLiked(track.id, nextLiked)
        }
        showToast(
            if (nextLiked) "Ajouté aux favoris : ${track.title}" else "Retiré des favoris : ${track.title}"
        )
    }

    fun setHomeMoodFilter(mood: String) {
        _homeMoodFilter.value = mood
        viewModelScope.launch {
            preferencesRepo.savePreferredMood(mood)
        }
    }

    fun setGlobalSearchQuery(query: String) {
        _globalSearchQuery.value = query
    }

    // Paid AI Music Generation: Instant Mix via Gemini 3.8 Flash
    fun triggerInstantAiMix(onMixReady: () -> Unit = {}) {
        if (!requireProEntitlement()) return
        val active = currentTrack.value
        val targetBpm = if (active.bpm > 0) active.bpm else 104
        val prompt = "Sélectionne un mix immédiat cohérent autour de ${active.genre} (${active.mood})"
        viewModelScope.launch {
            _aiPlaylistState.value = AiGenerationUiState.Loading
            try {
                val result = aiRepository.generateCustomPlaylist(
                    prompt = prompt,
                    targetBpm = targetBpm,
                    emotionalIntensity = active.energy.coerceIn(40, 95),
                    catalog = tracks.value
                )
                _aiPlaylistState.value = AiGenerationUiState.Success(result)
                preferencesRepo.saveCachedMix(prompt, result)
                result.trackIds.firstOrNull()?.let { playTrackById(it) }
                onMixReady()
            } catch (e: Exception) {
                _aiPlaylistState.value = AiGenerationUiState.Error(
                    e.localizedMessage ?: "Échec du Mix IA Gemini"
                )
                showToast("Erreur Mix IA", e.localizedMessage ?: "Vérifiez votre connexion.")
            }
        }
    }

    // Synth DSP Loop Extension (non-AI for synth tracks; honest block for fixed-length local files)
    fun extendBeat60s() {
        val track = currentTrack.value
        if (track.localUri != null) {
            showToast(
                "Fichier audio local",
                "La durée d'un fichier audio importé est fixe."
            )
            return
        }
        val newDuration = track.durationSec + 60
        viewModelScope.launch {
            trackDao.updateDuration(track.id, newDuration)
            audioController.updateActiveTrackDuration(newDuration)
        }
        if (!isPlaying.value) togglePlayPause()
        showToast("Boucle synthétiseur étendue (+60s)", "Durée prolongée sur ${track.title}")
    }

    fun isolateVocals() {
        applyEqPreset("vocal")
        if (!isPlaying.value) togglePlayPause()
        showToast("Isolation Vocale / Lead activée", "Courbe DSP centrée sur 1kHz – 3kHz")
    }

    // Real BPM-compatible Harmonic Transition with stepped audio fade
    fun harmonicDjTransition() {
        val list = tracks.value
        if (list.size <= 1) return
        val current = currentTrack.value
        val candidates = list.filter { it.id != current.id }
        val bestMatch = if (current.bpm > 0) {
            candidates.minByOrNull { candidate ->
                val bpmDiff = if (candidate.bpm > 0) abs(candidate.bpm - current.bpm) else 40
                val keyBonus = if (candidate.musicalKey == current.musicalKey) -6 else 0
                bpmDiff + keyBonus
            }
        } else {
            candidates.firstOrNull()
        } ?: return

        val matchLabel = if (bestMatch.bpm > 0) "${bestMatch.bpm} BPM • ${bestMatch.musicalKey}" else bestMatch.genre
        showToast("Transition harmonique vers ${bestMatch.title}", matchLabel)
        audioController.fadeOutAndSwitch {
            playTrack(bestMatch)
        }
    }

    // Paid AI Music Generation: Real Gemini 3.8 Flash Track Remix Composition
    fun generateSimilarRemix() {
        if (!requireProEntitlement()) return
        if (_isRemixGenerating.value) return

        val base = currentTrack.value
        viewModelScope.launch {
            _isRemixGenerating.value = true
            showToast("Composition Remix IA...", "Gemini 3.8 Flash génère un arrangement sur mesure")
            try {
                val remix = aiRepository.generateTrackRemix(base)
                trackDao.insertTrack(TrackEntity.fromDomain(remix, sortOrder = 0))
                playTrack(remix)
                showToast("Remix IA généré !", "${remix.title} • ${remix.bpm} BPM")
            } catch (e: Exception) {
                showToast("Erreur Remix IA", e.localizedMessage ?: "Échec de génération.")
            } finally {
                _isRemixGenerating.value = false
            }
        }
    }

    // Paid AI Music Generation: Studio Gemini Playlist Generator
    fun generateAiPlaylist(prompt: String, targetBpm: Int, emotionalIntensity: Int) {
        if (!requireProEntitlement()) return
        val cleanPrompt = prompt.trim().ifBlank { "Ambiance nocturne équilibrée Zouk et R&B" }
        viewModelScope.launch {
            _aiPlaylistState.value = AiGenerationUiState.Loading
            try {
                val result = aiRepository.generateCustomPlaylist(
                    prompt = cleanPrompt,
                    targetBpm = targetBpm,
                    emotionalIntensity = emotionalIntensity,
                    catalog = tracks.value
                )
                _aiPlaylistState.value = AiGenerationUiState.Success(result)
                preferencesRepo.saveCachedMix(cleanPrompt, result)
                showToast("Playlist Gemini générée !", "${result.trackIds.size} pistes sélectionnées")
            } catch (e: Exception) {
                _aiPlaylistState.value = AiGenerationUiState.Error(
                    e.localizedMessage ?: "Échec de la génération IA"
                )
            }
        }
    }

    fun playGeneratedMix() {
        val state = _aiPlaylistState.value
        if (state is AiGenerationUiState.Success) {
            state.data.trackIds.firstOrNull()?.let { playTrackById(it) }
        }
    }

    // Paid AI Co-Pilot Streaming Chat
    fun sendCopilotMessage(userText: String) {
        if (!requireProEntitlement()) return
        val clean = userText.trim()
        if (clean.isEmpty()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = clean
        )
        val aiMsgId = UUID.randomUUID().toString()
        val placeholderAi = ChatMessage(
            id = aiMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )
        _chatMessages.value = _chatMessages.value + userMsg + placeholderAi
        _isCopilotTyping.value = true

        copilotJob?.cancel()
        copilotJob = viewModelScope.launch {
            try {
                val catalog = tracks.value
                var latestText = ""
                aiRepository.streamCopilotReply(
                    userMessage = clean,
                    currentTrack = currentTrack.value,
                    catalog = catalog
                ).collect { partial ->
                    latestText = partial
                    val matchedTrack = catalog.firstOrNull {
                        partial.contains(it.title, ignoreCase = true)
                    }
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) {
                            msg.copy(
                                text = partial,
                                recommendedTrackId = matchedTrack?.id,
                                isStreaming = true
                            )
                        } else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) msg.copy(text = latestText, isStreaming = false) else msg
                }
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(
                            text = "Erreur Co-Pilote IA : ${e.localizedMessage ?: "Vérifiez votre connexion et App Check."}",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isCopilotTyping.value = false
            }
        }
    }

    fun clearCopilotChat() {
        copilotJob?.cancel()
        _isCopilotTyping.value = false
        _chatMessages.value = listOf(
            ChatMessage(
                id = "reset",
                isUser = false,
                text = "Conversation réinitialisée. Comment puis-je accompagner votre écoute ?"
            )
        )
    }

    // Equalizer & DSP Settings (Room-persisted)
    fun updateEqBand(bandIndex: Int, gainDb: Float) {
        val current = audioSettings.value.eqGainsDb.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb.coerceIn(-12f, 12f)
            viewModelScope.launch {
                preferencesRepo.saveEqGains(current, "custom")
            }
        }
    }

    fun applyEqPreset(presetName: String) {
        val gains = when (presetName) {
            "bass" -> listOf(8f, 6f, 2f, 0f, 0f, 2f, 3f)
            "zouk" -> listOf(6f, 4f, -1f, 1f, 3f, 5f, 4f)
            "studio" -> listOf(3f, 2f, 0f, 1f, 4f, 5f, 6f)
            "vocal" -> listOf(-2f, -1f, 2f, 6f, 5f, 2f, 0f)
            else -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
        viewModelScope.launch {
            preferencesRepo.saveEqGains(gains, presetName)
        }
        showToast("Préréglage Égaliseur : ${presetName.uppercase()}")
    }

    fun toggleCrossfade(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveCrossfade(enabled) }
        showToast("Fondu enchaîné audio : ${if (enabled) "Activé" else "Désactivé"}")
    }

    fun toggleNormalize(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveNormalize(enabled) }
        showToast("Normalisation (-14 LUFS) : ${if (enabled) "Activée" else "Désactivée"}")
    }

    fun toggleSpatial3d(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.saveSpatial3d(enabled) }
        showToast("Spatial Audio 3D Binaural : ${if (enabled) "Activé" else "Désactivé"}")
    }

    // Permissions & Local Device Audio Scanning (Android 7 to 15)
    fun refreshPermissions() {
        _permissionStatuses.value = PermissionManager.getAllStatuses(appContext)
    }

    fun scanDeviceAudioFiles() {
        refreshPermissions()
        if (!PermissionManager.hasAudioLibraryPermission(appContext)) {
            showToast("Permission requise", "Autorisez l'accès audio ou utilisez l'import SAF")
            return
        }
        viewModelScope.launch {
            val found = LocalMediaScanner.scanDeviceAudio(appContext)
            if (found.isEmpty()) {
                showToast("Scan terminé", "Aucun fichier audio détecté sur l'appareil")
            } else {
                trackDao.insertTracks(found.mapIndexed { idx, t -> TrackEntity.fromDomain(t, sortOrder = 50 + idx) })
                showToast("${found.size} pistes locales importées", "Ajoutées à votre bibliothèque")
            }
        }
    }

    fun importAudioUriSaf(uri: Uri) {
        viewModelScope.launch {
            val track = LocalMediaScanner.extractTrackFromUri(appContext, uri)
            trackDao.insertTrack(TrackEntity.fromDomain(track, sortOrder = 1))
            playTrack(track)
        }
    }

    override fun onCleared() {
        audioController.release()
        billingManager.destroy()
        super.onCleared()
    }
}
