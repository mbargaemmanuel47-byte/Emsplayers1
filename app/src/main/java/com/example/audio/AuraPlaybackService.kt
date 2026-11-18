package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.R

object PlaybackNotificationBridge {
    var onPlayPauseAction: (() -> Unit)? = null
    var onNextAction: (() -> Unit)? = null
    var onPrevAction: (() -> Unit)? = null
}

class AuraPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "ems_player_playback_channel"
        const val NOTIFICATION_ID = 4201

        const val ACTION_UPDATE = "com.example.emsplayer.ACTION_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.emsplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.emsplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.example.emsplayer.ACTION_PREV"
        const val ACTION_STOP = "com.example.emsplayer.ACTION_STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun updateNotification(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            val intent = Intent(context, AuraPlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.startService(intent) }
            }
        }

        fun stopService(context: Context) {
            runCatching { context.stopService(Intent(context, AuraPlaybackService::class.java)) }
        }
    }

    private var mediaSession: MediaSession? = null
    private var currentTitle: String = "Ems Player"
    private var currentArtist: String = "Co-Pilote Audio Neuronal"
    private var currentlyPlaying: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSession(this, "EmsPlayerSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    PlaybackNotificationBridge.onPlayPauseAction?.invoke()
                }

                override fun onPause() {
                    PlaybackNotificationBridge.onPlayPauseAction?.invoke()
                }

                override fun onSkipToNext() {
                    PlaybackNotificationBridge.onNextAction?.invoke()
                }

                override fun onSkipToPrevious() {
                    PlaybackNotificationBridge.onPrevAction?.invoke()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlaybackNotificationBridge.onPlayPauseAction?.invoke()
            ACTION_NEXT -> PlaybackNotificationBridge.onNextAction?.invoke()
            ACTION_PREV -> PlaybackNotificationBridge.onPrevAction?.invoke()
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE, null -> {
                currentTitle = intent?.getStringExtra(EXTRA_TITLE) ?: currentTitle
                currentArtist = intent?.getStringExtra(EXTRA_ARTIST) ?: currentArtist
                currentlyPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, currentlyPlaying) ?: currentlyPlaying
            }
        }

        updateMediaSessionState()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun updateMediaSessionState() {
        val stateCode = if (currentlyPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val state = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(stateCode, 0L, 1.0f)
            .build()

        mediaSession?.setPlaybackState(state)
        mediaSession?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                .build()
        )
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = servicePendingIntent(ACTION_PREV, 101)
        val playPausePending = servicePendingIntent(ACTION_PLAY_PAUSE, 102)
        val nextPending = servicePendingIntent(ACTION_NEXT, 103)

        val playPauseIcon = if (currentlyPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseLabel = if (currentlyPlaying) "Pause" else "Lecture"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setContentIntent(contentIntent)
            .setOngoing(currentlyPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Précédent", prevPending)
            .addAction(playPauseIcon, playPauseLabel, playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Suivant", nextPending)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun servicePendingIntent(action: String, reqCode: Int): PendingIntent {
        val intent = Intent(this, AuraPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
