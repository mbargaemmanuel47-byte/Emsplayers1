package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.ui.components.AuraBottomNavBar
import com.example.ui.components.AuraMiniPlayer
import com.example.ui.components.AuraNavigationRail
import com.example.ui.components.AuraRoutes
import com.example.ui.components.AuraToastOverlay
import com.example.ui.components.AuraTopHeader
import com.example.ui.components.ProPaywallDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.AuraAITheme
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.Midnight
import com.example.ui.theme.Obsidian
import com.example.viewmodel.AuraViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge with dark luxury system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val incomingAudioUri = extractAudioUri(intent)

        setContent {
            AuraAITheme {
                val viewModel: AuraViewModel = viewModel()

                LaunchedEffect(incomingAudioUri) {
                    if (incomingAudioUri != null) {
                        viewModel.importAudioUriSaf(incomingAudioUri)
                    }
                }

                AuraMainApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }

    private fun extractAudioUri(intent: Intent?): android.net.Uri? {
        return if (intent?.action == Intent.ACTION_VIEW) intent.data else null
    }
}

@Composable
fun AuraMainApp(viewModel: AuraViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AuraRoutes.HOME

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionSec by viewModel.currentPositionSec.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by viewModel.isRepeat.collectAsStateWithLifecycle()
    val spectrumBars by viewModel.spectrumBars.collectAsStateWithLifecycle()
    val dspStatus by viewModel.dspStatus.collectAsStateWithLifecycle()
    val homeMoodFilter by viewModel.homeMoodFilter.collectAsStateWithLifecycle()
    val globalSearch by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
    val aiPlaylistState by viewModel.aiPlaylistState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isCopilotTyping by viewModel.isCopilotTyping.collectAsStateWithLifecycle()
    val permissionStatuses by viewModel.permissionStatuses.collectAsStateWithLifecycle()
    val toastBanner by viewModel.toastBanner.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val isProUnlocked by viewModel.isProUnlocked.collectAsStateWithLifecycle()
    val showPaywallDialog by viewModel.showPaywallDialog.collectAsStateWithLifecycle()
    val isRemixGenerating by viewModel.isRemixGenerating.collectAsStateWithLifecycle()

    val allPermissionsGranted = permissionStatuses.all { it.isGranted }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        var skippedFirstResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skippedFirstResume) {
                    viewModel.refreshBilling()
                } else {
                    skippedFirstResume = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun navigateTo(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(AuraRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlue.copy(alpha = 0.45f), Obsidian, Midnight)
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (!isCompact) {
                    AuraNavigationRail(
                        currentRoute = currentRoute,
                        onNavigate = ::navigateTo,
                        onOpenPermissions = { navigateTo(AuraRoutes.PERMISSIONS) },
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        AuraTopHeader(
                            searchQuery = globalSearch,
                            onSearchChange = { q ->
                                viewModel.setGlobalSearchQuery(q)
                                if (q.isNotBlank() && currentRoute != AuraRoutes.LIBRARY) {
                                    navigateTo(AuraRoutes.LIBRARY)
                                }
                            },
                            onInstantAiMix = {
                                viewModel.triggerInstantAiMix {
                                    navigateTo(AuraRoutes.PLAYER)
                                }
                            },
                            onOpenPermissions = { navigateTo(AuraRoutes.PERMISSIONS) },
                            allPermissionsGranted = allPermissionsGranted,
                            isProUnlocked = isProUnlocked,
                            showSearchBar = !isCompact
                        )
                    },
                    bottomBar = {
                        Column {
                            if (currentRoute != AuraRoutes.PLAYER) {
                                AuraMiniPlayer(
                                    track = currentTrack,
                                    isPlaying = isPlaying,
                                    positionSec = positionSec,
                                    onTogglePlay = viewModel::togglePlayPause,
                                    onNext = viewModel::nextTrack,
                                    onPrevious = viewModel::previousTrack,
                                    onOpenPlayer = { navigateTo(AuraRoutes.PLAYER) }
                                )
                            }
                            if (isCompact) {
                                AuraBottomNavBar(
                                    currentRoute = currentRoute,
                                    onNavigate = ::navigateTo
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AuraRoutes.HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(AuraRoutes.HOME) {
                            HomeScreen(
                                tracks = tracks,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                isProUnlocked = isProUnlocked,
                                selectedMood = homeMoodFilter,
                                onMoodSelected = viewModel::setHomeMoodFilter,
                                onPlayTrack = { track -> viewModel.playTrack(track) },
                                onPlayTrackById = { id -> viewModel.playTrackById(id) },
                                onToggleLike = viewModel::toggleLike,
                                onOpenStudio = { navigateTo(AuraRoutes.STUDIO) },
                                onOpenLibrary = { navigateTo(AuraRoutes.LIBRARY) }
                            )
                        }

                        composable(AuraRoutes.PLAYER) {
                            PlayerScreen(
                                track = currentTrack,
                                isPlaying = isPlaying,
                                positionSec = positionSec,
                                volume = volume,
                                isMuted = isMuted,
                                isShuffle = isShuffle,
                                isRepeat = isRepeat,
                                isProUnlocked = isProUnlocked,
                                isRemixGenerating = isRemixGenerating,
                                spectrumBars = spectrumBars,
                                onTogglePlay = viewModel::togglePlayPause,
                                onNext = viewModel::nextTrack,
                                onPrevious = viewModel::previousTrack,
                                onSeekFraction = viewModel::seekToFraction,
                                onSeekSeconds = viewModel::seekToSeconds,
                                onVolumeChange = viewModel::setVolume,
                                onToggleMute = viewModel::toggleMute,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onToggleRepeat = viewModel::toggleRepeat,
                                onToggleLike = { viewModel.toggleLike(currentTrack) },
                                onExtendBeat60s = viewModel::extendBeat60s,
                                onIsolateVocals = viewModel::isolateVocals,
                                onHarmonicTransition = viewModel::harmonicDjTransition,
                                onGenerateRemix = viewModel::generateSimilarRemix
                            )
                        }

                        composable(AuraRoutes.STUDIO) {
                            StudioScreen(
                                tracks = tracks,
                                aiPlaylistState = aiPlaylistState,
                                chatMessages = chatMessages,
                                isCopilotTyping = isCopilotTyping,
                                isProUnlocked = isProUnlocked,
                                onOpenPaywall = viewModel::openPaywall,
                                onGeneratePlaylist = viewModel::generateAiPlaylist,
                                onPlayGeneratedMix = {
                                    viewModel.playGeneratedMix()
                                    navigateTo(AuraRoutes.PLAYER)
                                },
                                onPlayTrackById = { id ->
                                    viewModel.playTrackById(id)
                                    navigateTo(AuraRoutes.PLAYER)
                                },
                                onSendChatMessage = viewModel::sendCopilotMessage,
                                onClearChat = viewModel::clearCopilotChat
                            )
                        }

                        composable(AuraRoutes.LIBRARY) {
                            LibraryScreen(
                                tracks = tracks,
                                currentTrack = currentTrack,
                                globalSearch = globalSearch,
                                onPlayTrack = { track -> viewModel.playTrack(track) },
                                onPlayTrackById = { id -> viewModel.playTrackById(id) },
                                onToggleLike = viewModel::toggleLike,
                                onScanDeviceAudio = viewModel::scanDeviceAudioFiles,
                                onImportSafUri = { uri ->
                                    viewModel.importAudioUriSaf(uri)
                                    navigateTo(AuraRoutes.PLAYER)
                                },
                                onRefreshPermissions = viewModel::refreshPermissions
                            )
                        }

                        composable(AuraRoutes.EQUALIZER) {
                            EqualizerScreen(
                                settings = audioSettings,
                                dspStatus = dspStatus,
                                onBandChange = viewModel::updateEqBand,
                                onSelectPreset = viewModel::applyEqPreset,
                                onToggleCrossfade = viewModel::toggleCrossfade,
                                onToggleNormalize = viewModel::toggleNormalize,
                                onToggleSpatial3d = viewModel::toggleSpatial3d
                            )
                        }

                        composable(AuraRoutes.PERMISSIONS) {
                            PermissionsScreen(
                                permissionStatuses = permissionStatuses,
                                onRefreshPermissions = viewModel::refreshPermissions,
                                onImportSafUri = { uri ->
                                    viewModel.importAudioUriSaf(uri)
                                    navigateTo(AuraRoutes.PLAYER)
                                },
                                onScanDeviceAudio = viewModel::scanDeviceAudioFiles
                            )
                        }
                    }
                }
            }
        }

        AuraToastOverlay(
            toast = toastBanner,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        if (showPaywallDialog) {
            ProPaywallDialog(
                billingState = billingState,
                onDismiss = viewModel::dismissPaywall,
                onPurchaseOption = viewModel::launchProPurchase,
                onRestorePurchases = viewModel::restoreProPurchases
            )
        }
    }
}
