package com.seafox.nmea_dashboard.data

enum class BillingPurchaseState {
    purchased,
    pending,
    unspecified,
}

enum class PurchaseVerificationStatus {
    unverified,
    verified,
    rejected,
}

data class BillingPurchaseRecord(
    val productIds: Set<String>,
    val purchaseToken: String,
    val purchaseState: BillingPurchaseState,
    val verificationStatus: PurchaseVerificationStatus,
    val acknowledged: Boolean,
    val purchasedAtEpochMs: Long,
)

data class BillingRestoreResult(
    val entitlementSnapshot: EntitlementSnapshot,
    val needsBackendVerification: Boolean,
    val pendingProductIds: Set<String>,
    val rejectedProductIds: Set<String>,
    val unacknowledgedPurchaseTokens: Set<String>,
)

object BillingEntitlementMapper {
    fun restoreFromPurchases(
        purchases: List<BillingPurchaseRecord>,
        validUntilEpochMs: Long? = null,
    ): BillingRestoreResult {
        val pendingProductIds = linkedSetOf<String>()
        val rejectedProductIds = linkedSetOf<String>()
        val unacknowledgedTokens = linkedSetOf<String>()
        var needsBackendVerification = false
        var bestTier = SubscriptionTier.FREE
        val licensedChartProviderIds = linkedSetOf<String>()

        purchases.forEach { purchase ->
            when (purchase.purchaseState) {
                BillingPurchaseState.pending -> {
                    pendingProductIds += purchase.productIds
                    return@forEach
                }
                BillingPurchaseState.unspecified -> {
                    needsBackendVerification = true
                    return@forEach
                }
                BillingPurchaseState.purchased -> Unit
            }

            when (purchase.verificationStatus) {
                PurchaseVerificationStatus.unverified -> {
                    needsBackendVerification = true
                    return@forEach
                }
                PurchaseVerificationStatus.rejected -> {
                    rejectedProductIds += purchase.productIds
                    return@forEach
                }
                PurchaseVerificationStatus.verified -> Unit
            }

            if (!purchase.acknowledged && purchase.purchaseToken.isNotBlank()) {
                unacknowledgedTokens += purchase.purchaseToken
            }

            purchase.productIds.forEach { productId ->
                BillingCatalog.tierForProductId(productId)?.let { tier ->
                    bestTier = maxTier(bestTier, tier)
                }
                BillingCatalog.chartProviderForProductId(productId)?.let { providerId ->
                    licensedChartProviderIds += providerId
                }
            }
        }

        return BillingRestoreResult(
            entitlementSnapshot = EntitlementSnapshot(
                tier = bestTier,
                licensedChartProviderIds = licensedChartProviderIds,
                validUntilEpochMs = validUntilEpochMs,
            ),
            needsBackendVerification = needsBackendVerification,
            pendingProductIds = pendingProductIds,
            rejectedProductIds = rejectedProductIds,
            unacknowledgedPurchaseTokens = unacknowledgedTokens,
        )
    }

    private fun maxTier(left: SubscriptionTier, right: SubscriptionTier): SubscriptionTier {
        return if (tierRank(right) > tierRank(left)) right else left
    }

    private fun tierRank(tier: SubscriptionTier): Int {
        return when (tier) {
            SubscriptionTier.FREE -> 0
            SubscriptionTier.PRO -> 1
            SubscriptionTier.NAVIGATOR -> 2
            SubscriptionTier.FLEET -> 3
        }
    }
}
