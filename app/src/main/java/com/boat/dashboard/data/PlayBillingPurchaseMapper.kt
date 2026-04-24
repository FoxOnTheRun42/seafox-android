package com.seafox.nmea_dashboard.data

import com.android.billingclient.api.Purchase

object PlayBillingPurchaseMapper {
    fun toPurchaseRecord(
        purchase: Purchase,
        verificationStatus: PurchaseVerificationStatus = PurchaseVerificationStatus.unverified,
    ): BillingPurchaseRecord {
        return BillingPurchaseRecord(
            productIds = purchase.products.map { productId -> productId.trim() }
                .filter { productId -> productId.isNotBlank() }
                .toSet(),
            purchaseToken = purchase.purchaseToken,
            purchaseState = toBillingPurchaseState(purchase.purchaseState),
            verificationStatus = verificationStatus,
            acknowledged = purchase.isAcknowledged,
            purchasedAtEpochMs = purchase.purchaseTime.coerceAtLeast(0L),
        )
    }

    fun toPurchaseRecords(
        purchases: List<Purchase>,
        verificationStatus: PurchaseVerificationStatus = PurchaseVerificationStatus.unverified,
    ): List<BillingPurchaseRecord> {
        return purchases.map { purchase ->
            toPurchaseRecord(
                purchase = purchase,
                verificationStatus = verificationStatus,
            )
        }
    }

    internal fun toBillingPurchaseState(purchaseState: Int): BillingPurchaseState {
        return when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> BillingPurchaseState.purchased
            Purchase.PurchaseState.PENDING -> BillingPurchaseState.pending
            else -> BillingPurchaseState.unspecified
        }
    }
}
