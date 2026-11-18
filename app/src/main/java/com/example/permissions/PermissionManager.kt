package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class PermissionKind {
    AUDIO_LIBRARY,
    NOTIFICATIONS,
    BLUETOOTH_AUDIO
}

data class AndroidPermissionStatus(
    val kind: PermissionKind,
    val manifestPermission: String?,
    val titleResId: Int,
    val description: String,
    val isGranted: Boolean,
    val requiresRuntimeRequest: Boolean,
    val minApiNote: String
)

object PermissionManager {

    fun audioLibraryPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun notificationsPermissionOrNull(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    fun bluetoothPermissionOrNull(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            null
        }
    }

    fun isPermissionGranted(context: Context, permission: String?): Boolean {
        if (permission == null) return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioLibraryPermission(context: Context): Boolean {
        return isPermissionGranted(context, audioLibraryPermission())
    }

    fun getAllStatuses(context: Context): List<AndroidPermissionStatus> {
        val audioPerm = audioLibraryPermission()
        val notifPerm = notificationsPermissionOrNull()
        val btPerm = bluetoothPermissionOrNull()

        return listOf(
            AndroidPermissionStatus(
                kind = PermissionKind.AUDIO_LIBRARY,
                manifestPermission = audioPerm,
                titleResId = com.example.R.string.perm_audio_title,
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "READ_MEDIA_AUDIO (Android 13–15) : accès granulaire à votre musique locale (MP3, FLAC, WAV) sans accès aux photos/vidéos."
                } else {
                    "READ_EXTERNAL_STORAGE (Android 7–12, maxSdkVersion=32) : scan sécurisé des dossiers audio locaux."
                },
                isGranted = isPermissionGranted(context, audioPerm),
                requiresRuntimeRequest = true,
                minApiNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "API 33+" else "API 24–32"
            ),
            AndroidPermissionStatus(
                kind = PermissionKind.NOTIFICATIONS,
                manifestPermission = notifPerm,
                titleResId = com.example.R.string.perm_notif_title,
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "POST_NOTIFICATIONS (Android 13–15) : lecteur média persistant, contrôles écran verrouillé et alertes Mix IA."
                } else {
                    "Notifications média actives automatiquement sur Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})."
                },
                isGranted = isPermissionGranted(context, notifPerm),
                requiresRuntimeRequest = notifPerm != null,
                minApiNote = "API 33+ (Auto < 33)"
            ),
            AndroidPermissionStatus(
                kind = PermissionKind.BLUETOOTH_AUDIO,
                manifestPermission = btPerm,
                titleResId = com.example.R.string.perm_bt_title,
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "BLUETOOTH_CONNECT (Android 12–15) : détection des casques sans fil et calibration Spatial Audio 3D."
                } else {
                    "Routage Bluetooth actif automatiquement via BLUETOOTH (maxSdkVersion=30) sur Android ${Build.VERSION.RELEASE}."
                },
                isGranted = isPermissionGranted(context, btPerm),
                requiresRuntimeRequest = btPerm != null,
                minApiNote = "API 31+ (Auto < 31)"
            )
        )
    }

    fun getPendingRuntimePermissions(context: Context): Array<String> {
        val required = buildList {
            add(audioLibraryPermission())
            notificationsPermissionOrNull()?.let { add(it) }
            bluetoothPermissionOrNull()?.let { add(it) }
        }
        return required.filter { !isPermissionGranted(context, it) }.toTypedArray()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
