package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PlayBillingManager"

data class BillingProductOption(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriodLabel: String,
    val isSubscription: Boolean,
    val productDetails: ProductDetails,
    val offerToken: String? = null
)

data class BillingUiState(
    val isProUnlocked: Boolean = false,
    val isConnecting: Boolean = true,
    val isStoreAvailable: Boolean = false,
    val isPendingPurchase: Boolean = false,
    val products: List<BillingProductOption> = emptyList(),
    val statusMessage: String? = null
)

/**
 * Production Google Play Billing Manager (PBL 8.0.0) strictly enforcing paid entitlement
 * for AI music generation. Never grants unpaid or simulated Pro access.
 */
class PlayBillingManager(
    context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val SUBSCRIPTION_PRODUCT_ID = "ems_player_ai_pro"
        const val LIFETIME_PRODUCT_ID = "ems_player_ai_lifetime"
        val AI_PRO_PRODUCT_IDS = setOf(SUBSCRIPTION_PRODUCT_ID, LIFETIME_PRODUCT_ID)
    }

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch {
            handlePurchasesResult(billingResult, purchases)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    @Volatile
    private var isConnectingInFlight = false

    init {
        scope.launch {
            delay(350L)
            connectAndQuery()
        }
    }

    fun connectAndQuery() {
        if (billingClient.isReady) {
            scope.launch {
                queryCatalogAndActivePurchases()
            }
            return
        }
        if (isConnectingInFlight) return
        isConnectingInFlight = true

        _uiState.update { it.copy(isConnecting = true, statusMessage = null) }

        runCatching {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnectingInFlight = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isStoreAvailable = true,
                                statusMessage = null
                            )
                        }
                        scope.launch {
                            queryCatalogAndActivePurchases()
                        }
                    } else {
                        Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isStoreAvailable = false,
                                statusMessage = mapBillingErrorMessage(billingResult)
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnectingInFlight = false
                    Log.w(TAG, "Google Play Billing service disconnected.")
                    _uiState.update {
                        it.copy(isConnecting = false)
                    }
                }
            })
        }.onFailure { error ->
            isConnectingInFlight = false
            Log.e(TAG, "Failed to start Google Play Billing connection", error)
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isStoreAvailable = false,
                    statusMessage = "Google Play Billing indisponible sur cet appareil."
                )
            }
        }
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) {
            connectAndQuery()
            return
        }
        scope.launch {
            queryActivePurchasesOnly()
        }
    }

    fun launchPurchaseFlow(activity: Activity, option: BillingProductOption) {
        if (!billingClient.isReady) {
            _uiState.update {
                it.copy(statusMessage = "Connexion au Google Play Store requise pour finaliser l'achat.")
            }
            connectAndQuery()
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(option.productDetails)

        if (option.isSubscription && !option.offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(option.offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val launchResult = billingClient.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.update {
                it.copy(statusMessage = mapBillingErrorMessage(launchResult))
            }
        }
    }

    private suspend fun queryCatalogAndActivePurchases() {
        queryAvailableProducts()
        queryActivePurchasesOnly()
    }

    private suspend fun queryAvailableProducts() {
        val loadedOptions = mutableListOf<BillingProductOption>()

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val subsResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(subsParams)
        }
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            subsResult.productDetailsList.orEmpty().forEach { details ->
                val offer = details.subscriptionOfferDetails?.firstOrNull()
                val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                if (offer != null && phase != null) {
                    loadedOptions.add(
                        BillingProductOption(
                            productId = details.productId,
                            title = details.title.ifBlank { "Em Player Pro (Abonnement)" },
                            description = details.description.ifBlank {
                                "Playlists intelligentes et assistant musical."
                            },
                            formattedPrice = phase.formattedPrice,
                            billingPeriodLabel = formatBillingPeriod(phase.billingPeriod),
                            isSubscription = true,
                            productDetails = details,
                            offerToken = offer.offerToken
                        )
                    )
                }
            }
        }

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(LIFETIME_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        val inAppResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(inAppParams)
        }
        if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            inAppResult.productDetailsList.orEmpty().forEach { details ->
                val offer = details.oneTimePurchaseOfferDetails
                if (offer != null) {
                    loadedOptions.add(
                        BillingProductOption(
                            productId = details.productId,
                            title = details.title.ifBlank { "Em Player Pro (Accès À Vie)" },
                            description = details.description.ifBlank {
                                "Accès complet au Studio musical intelligent."
                            },
                            formattedPrice = offer.formattedPrice,
                            billingPeriodLabel = "Achat unique",
                            isSubscription = false,
                            productDetails = details
                        )
                    )
                }
            }
        }

        _uiState.update { state ->
            state.copy(
                products = loadedOptions,
                statusMessage = if (loadedOptions.isEmpty() && state.isStoreAvailable) {
                    "Offres momentanément indisponibles."
                } else {
                    state.statusMessage
                }
            )
        }
    }

    private suspend fun queryActivePurchasesOnly() {
        val subsPurchases = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }
        val inAppPurchases = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        }

        val combined = mutableListOf<Purchase>()
        if (subsPurchases.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            combined.addAll(subsPurchases.purchasesList)
        }
        if (inAppPurchases.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            combined.addAll(inAppPurchases.purchasesList)
        }

        processVerifiedPurchases(combined)
    }

    private suspend fun handlePurchasesResult(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                processVerifiedPurchases(purchases.orEmpty())
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.update {
                    it.copy(statusMessage = "Achat annulé par l'utilisateur.")
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryActivePurchasesOnly()
            }
            else -> {
                _uiState.update {
                    it.copy(statusMessage = mapBillingErrorMessage(billingResult))
                }
            }
        }
    }

    private suspend fun processVerifiedPurchases(purchases: List<Purchase>) {
        var unlocked = false
        var pending = false

        for (purchase in purchases) {
            val matchesAiProduct = purchase.products.any { it in AI_PRO_PRODUCT_IDS }
            if (!matchesAiProduct) continue

            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        val ackResult = withContext(Dispatchers.IO) {
                            billingClient.acknowledgePurchase(ackParams)
                        }
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            unlocked = true
                        } else {
                            Log.e(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                            _uiState.update {
                                it.copy(statusMessage = "Erreur de confirmation Play Store : ${ackResult.debugMessage}")
                            }
                        }
                    } else {
                        unlocked = true
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    pending = true
                }
            }
        }

        _uiState.update { state ->
            state.copy(
                isProUnlocked = unlocked,
                isPendingPurchase = pending,
                statusMessage = when {
                    unlocked -> "Em Player Pro actif."
                    pending -> "Paiement en attente de validation par Google Play."
                    else -> state.statusMessage
                }
            )
        }
    }

    private fun mapBillingErrorMessage(result: BillingResult): String {
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "Facturation Google Play indisponible sur cet appareil (Play Store requis)."
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                "Service Google Play temporairement inaccessible. Vérifiez votre connexion."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "Produit non disponible sur votre compte Play Store."
            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                "Configuration Play Console requise pour cet identifiant d'application."
            else ->
                result.debugMessage.ifBlank { "Erreur Google Play Billing (code ${result.responseCode})." }
        }
    }

    private fun formatBillingPeriod(isoPeriod: String): String {
        return when (isoPeriod) {
            "P1W" -> "par semaine"
            "P1M" -> "par mois"
            "P3M" -> "tous les 3 mois"
            "P6M" -> "tous les 6 mois"
            "P1Y" -> "par an"
            else -> isoPeriod
        }
    }

    fun destroy() {
        runCatching { billingClient.endConnection() }
    }
}
