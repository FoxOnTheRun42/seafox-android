package com.seafox.nmea_dashboard.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
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

    fun queryProductDetails(
        productId: String,
        productType: String,
        onResult: (BillingResult, ProductDetails?) -> Unit,
    ) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(productType)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            onResult(billingResult, result.productDetailsList.firstOrNull())
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                if (productDetails.productType == BillingClient.ProductType.SUBS) {
                    val offerToken = productDetails.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.offerToken
                        .orEmpty()
                    if (offerToken.isNotBlank()) {
                        setOfferToken(offerToken)
                    }
                }
            }
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        return billingClient.launchBillingFlow(activity, flowParams)
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
