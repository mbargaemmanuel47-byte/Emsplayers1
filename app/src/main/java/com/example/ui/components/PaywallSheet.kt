package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.billing.BillingProductOption
import com.example.billing.BillingUiState
import com.example.ui.theme.BorderGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkSurface
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.Obsidian
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ProPaywallDialog(
    billingState: BillingUiState,
    onDismiss: () -> Unit,
    onPurchaseOption: (Activity, BillingProductOption) -> Unit,
    onRestorePurchases: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = CardDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGold, RoundedCornerShape(26.dp))
                .testTag("pro_paywall_dialog")
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(DeepBlue.copy(alpha = 0.75f), CardDarkSurface)))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GoldBright, GoldMetallic))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Em Player Pro",
                                tint = Obsidian,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EM PLAYER PRO",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldBright
                            )
                            Text(
                                text = "Studio Musical IA",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("paywall_close_btn")) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = TextMuted)
                    }
                }

                Text(
                    text = "La lecture audio locale, l'égaliseur et le lecteur restent gratuits. Les mixes et recommandations intelligentes sont inclus dans Em Player Pro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Obsidian.copy(alpha = 0.65f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaywallBenefitRow("Création de playlists et mixes sur mesure")
                    PaywallBenefitRow("Sélections intelligentes à partir de vos musiques")
                    PaywallBenefitRow("Emma, votre IA musicale en direct")
                }

                if (billingState.isConnecting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GoldBright,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Connexion sécurisée à Google Play Billing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoldLight
                        )
                    }
                } else if (billingState.products.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        billingState.products.forEach { option ->
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        onPurchaseOption(activity, option)
                                    }
                                },
                                enabled = activity != null && !billingState.isPendingPurchase,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldBright,
                                    contentColor = Obsidian
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("buy_product_${option.productId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = option.billingPeriodLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Obsidian.copy(alpha = 0.8f)
                                        )
                                    }
                                    Text(
                                        text = option.formattedPrice,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoseError.copy(alpha = 0.12f))
                            .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = billingState.statusMessage
                                ?: "Offres d'abonnement actuellement indisponibles.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (billingState.statusMessage != null && billingState.products.isNotEmpty()) {
                    Text(
                        text = billingState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (billingState.isPendingPurchase) GoldLight else TextMuted
                    )
                }

                OutlinedButton(
                    onClick = onRestorePurchases,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_purchases_btn")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurer mes achats Google Play", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PaywallBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = EmeraldAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = Color.White)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
