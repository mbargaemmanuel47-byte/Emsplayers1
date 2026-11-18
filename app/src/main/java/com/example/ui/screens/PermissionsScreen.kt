package com.example.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.permissions.AndroidPermissionStatus
import com.example.permissions.PermissionManager
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Obsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionsScreen(
    permissionStatuses: List<AndroidPermissionStatus>,
    onRefreshPermissions: () -> Unit,
    onImportSafUri: (Uri) -> Unit,
    onScanDeviceAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val singlePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val multiPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshPermissions()
    }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportSafUri(uri)
    }

    val pendingPermissions = PermissionManager.getPendingRuntimePermissions(context)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(DeepBlue, CardDarkSurface)))
                    .border(1.dp, BorderGold, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = GoldBright, modifier = Modifier.size(28.dp))
                    Column {
                        Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            text = "Appareil : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • minSdk 24 / targetSdk 36",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldLight
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.perm_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pendingPermissions.isNotEmpty()) {
                        Button(
                            onClick = { multiPermLauncher.launch(pendingPermissions) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("grant_all_permissions_btn")
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tout autoriser (${pendingPermissions.size})", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { PermissionManager.openAppSettings(context) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_app_settings_btn")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paramètres OS", fontSize = 12.sp)
                    }
                }
            }
        }

        items(permissionStatuses) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkSurface.copy(alpha = 0.92f))
                    .border(
                        1.dp,
                        if (item.isGranted) EmeraldAccent.copy(alpha = 0.45f) else BorderGold,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (item.isGranted) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = if (item.isGranted) EmeraldAccent else GoldBright,
                    modifier = Modifier.size(26.dp)
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(item.titleResId),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = item.minApiNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(item.description, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = if (item.isGranted) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isGranted) EmeraldAccent else GoldBright
                    )
                }

                if (!item.isGranted && item.manifestPermission != null) {
                    Button(
                        onClick = { singlePermLauncher.launch(item.manifestPermission) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("grant_perm_${item.kind.name}")
                    ) {
                        Text(stringResource(R.string.perm_grant_cta), fontSize = 12.sp)
                    }
                }
            }
        }

        // Zero-permission SAF & Play Store Compliance Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Conformité Google Play & Accès Sans Permission", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    "• Aucune permission large de photos/vidéos (READ_MEDIA_IMAGES / READ_MEDIA_VIDEO) déclarée.\n" +
                        "• Service Foreground déclaré avec foregroundServiceType=\"mediaPlayback\" (Android 14–15).\n" +
                        "• Sélecteur SAF système (OpenDocument) disponible sans autorisation de stockage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { safLauncher.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Obsidian),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import SAF Audio", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onScanDeviceAudio,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scanner Audio", fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
