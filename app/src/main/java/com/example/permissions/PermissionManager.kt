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
    val requiresRuntimeRequest: Boolean
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
        return runCatching {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
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
                description = "Permet de détecter et lire les sons de votre stockage.",
                isGranted = isPermissionGranted(context, audioPerm),
                requiresRuntimeRequest = true
            ),
            AndroidPermissionStatus(
                kind = PermissionKind.NOTIFICATIONS,
                manifestPermission = notifPerm,
                titleResId = com.example.R.string.perm_notif_title,
                description = "Affiche le lecteur sur l'écran de verrouillage.",
                isGranted = isPermissionGranted(context, notifPerm),
                requiresRuntimeRequest = notifPerm != null
            ),
            AndroidPermissionStatus(
                kind = PermissionKind.BLUETOOTH_AUDIO,
                manifestPermission = btPerm,
                titleResId = com.example.R.string.perm_bt_title,
                description = "Détecte vos écouteurs et enceintes Bluetooth.",
                isGranted = isPermissionGranted(context, btPerm),
                requiresRuntimeRequest = btPerm != null
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
