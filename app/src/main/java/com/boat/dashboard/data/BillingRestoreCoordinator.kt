package com.seafox.nmea_dashboard.data

data class BillingValidationRequest(
    val productIds: Set<String>,
    val purchaseToken: String,
    val purchaseState: BillingPurchaseState,
    val acknowledged: Boolean,
    val purchasedAtEpochMs: Long,
)

data class BillingValidationDecision(
    val purchaseToken: String,
    val verificationStatus: PurchaseVerificationStatus,
    val validUntilEpochMs: Long? = null,
)

data class BillingCoordinatedRestoreResult(
    val restoreResult: BillingRestoreResult,
    val validationRequests: List<BillingValidationRequest>,
    val missingValidationTokens: Set<String>,
)

object BillingRestoreCoordinator {
    fun validationRequestsFor(
        purchases: List<BillingPurchaseRecord>,
    ): List<BillingValidationRequest> {
        return purchases
            .filter { purchase ->
                purchase.purchaseState == BillingPurchaseState.purchased &&
                    purchase.purchaseToken.isNotBlank()
            }
            .map { purchase ->
                BillingValidationRequest(
                    productIds = purchase.productIds,
                    purchaseToken = purchase.purchaseToken,
                    purchaseState = purchase.purchaseState,
                    acknowledged = purchase.acknowledged,
                    purchasedAtEpochMs = purchase.purchasedAtEpochMs,
                )
            }
    }

    fun restoreWithValidation(
        purchases: List<BillingPurchaseRecord>,
        validationDecisions: List<BillingValidationDecision>,
    ): BillingCoordinatedRestoreResult {
        val decisionsByToken = validationDecisions
            .filter { decision -> decision.purchaseToken.isNotBlank() }
            .associateBy { decision -> decision.purchaseToken }
        val missingValidationTokens = linkedSetOf<String>()
        var validUntilEpochMs: Long? = null

        val mergedPurchases = purchases.map { purchase ->
            if (purchase.purchaseState != BillingPurchaseState.purchased || purchase.purchaseToken.isBlank()) {
                purchase
            } else {
                val decision = decisionsByToken[purchase.purchaseToken]
                if (decision == null) {
                    missingValidationTokens += purchase.purchaseToken
                    purchase.copy(verificationStatus = PurchaseVerificationStatus.unverified)
                } else {
                    if (decision.verificationStatus == PurchaseVerificationStatus.verified) {
                        validUntilEpochMs = chooseEarliestValidUntil(
                            current = validUntilEpochMs,
                            candidate = decision.validUntilEpochMs,
                        )
                    }
                    purchase.copy(verificationStatus = decision.verificationStatus)
                }
            }
        }

        return BillingCoordinatedRestoreResult(
            restoreResult = BillingEntitlementMapper.restoreFromPurchases(
                purchases = mergedPurchases,
                validUntilEpochMs = validUntilEpochMs,
            ),
            validationRequests = validationRequestsFor(purchases),
            missingValidationTokens = missingValidationTokens,
        )
    }

    private fun chooseEarliestValidUntil(current: Long?, candidate: Long?): Long? {
        if (candidate == null) return current
        if (current == null) return candidate
        return minOf(current, candidate)
    }
}
