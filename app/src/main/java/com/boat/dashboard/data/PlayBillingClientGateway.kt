package com.seafox.nmea_dashboard.data

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams

class PlayBillingClientGateway(
    context: Context,
    private val onPurchasesUpdated: (BillingResult, List<Purchase>) -> Unit,
) {
    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        onPurchasesUpdated(billingResult, purchases.orEmpty())
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(listener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun startConnection(
        onReady: (BillingResult) -> Unit,
        onDisconnected: () -> Unit = {},
    ) {
        if (billingClient.isReady) {
            onReady(
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .setDebugMessage("BillingClient already connected")
                    .build(),
            )
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                onReady(billingResult)
            }

            override fun onBillingServiceDisconnected() {
                onDisconnected()
            }
        })
    }

    fun queryActiveAppSubscriptions(
        onResult: (BillingResult, List<Purchase>) -> Unit,
    ) {
        queryActivePurchases(BillingClient.ProductType.SUBS, onResult)
    }

    fun queryActiveInAppProducts(
        onResult: (BillingResult, List<Purchase>) -> Unit,
    ) {
        queryActivePurchases(BillingClient.ProductType.INAPP, onResult)
    }

    fun queryActiveEntitlementPurchases(
        onResult: (BillingResult, List<Purchase>) -> Unit,
    ) {
        queryActiveAppSubscriptions { subscriptionResult, subscriptions ->
            if (subscriptionResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(subscriptionResult, subscriptions)
                return@queryActiveAppSubscriptions
            }

            queryActiveInAppProducts { inAppResult, inAppPurchases ->
                val result = if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    subscriptionResult
                } else {
                    inAppResult
                }
                onResult(result, subscriptions + inAppPurchases)
            }
        }
    }

    fun acknowledgePurchase(
        purchaseToken: String,
        onResult: (BillingResult) -> Unit,
    ) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            onResult(billingResult)
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    private fun queryActivePurchases(
        productType: String,
        onResult: (BillingResult, List<Purchase>) -> Unit,
    ) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            onResult(billingResult, purchases)
        }
    }
}
