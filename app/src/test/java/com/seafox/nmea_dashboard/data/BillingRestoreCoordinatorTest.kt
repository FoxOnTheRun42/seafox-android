package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingRestoreCoordinatorTest {
    @Test
    fun missingBackendValidationDoesNotGrantEntitlements() {
        val result = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(purchase("seafox.navigator.yearly", token = "nav-token")),
            validationDecisions = emptyList(),
        )

        assertEquals(SubscriptionTier.FREE, result.restoreResult.entitlementSnapshot.tier)
        assertTrue(result.restoreResult.needsBackendVerification)
        assertEquals(setOf("nav-token"), result.missingValidationTokens)
        assertEquals(
            listOf("nav-token"),
            result.validationRequests.map { request -> request.purchaseToken },
        )
    }

    @Test
    fun verifiedSubscriptionRestoresTierAndUnacknowledgedToken() {
        val result = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(purchase("seafox.navigator.yearly", token = "nav-token")),
            validationDecisions = listOf(
                decision("nav-token", PurchaseVerificationStatus.verified, validUntilEpochMs = 2_000L),
            ),
        )

        assertEquals(SubscriptionTier.NAVIGATOR, result.restoreResult.entitlementSnapshot.tier)
        assertEquals(2_000L, result.restoreResult.entitlementSnapshot.validUntilEpochMs)
        assertEquals(setOf("nav-token"), result.restoreResult.unacknowledgedPurchaseTokens)
        assertTrue(result.missingValidationTokens.isEmpty())
    }

    @Test
    fun verifiedPremiumChartPackRestoresOnlyOwnedChartPack() {
        val result = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(purchase(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID, token = "pack-token")),
            validationDecisions = listOf(decision("pack-token", PurchaseVerificationStatus.verified)),
        )

        assertEquals(SubscriptionTier.FREE, result.restoreResult.entitlementSnapshot.tier)
        assertEquals(
            setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
            result.restoreResult.entitlementSnapshot.ownedChartPackIds,
        )
        assertEquals(emptySet<String>(), result.restoreResult.entitlementSnapshot.licensedChartProviderIds)
    }

    @Test
    fun rejectedOrInactiveProductsDoNotGrantFeaturesOrChartLicenses() {
        val result = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(
                purchase("seafox.fleet.monthly", token = "fleet-token"),
                purchase("seafox.chart.cmap.external", token = "cmap-token"),
            ),
            validationDecisions = listOf(
                decision("fleet-token", PurchaseVerificationStatus.rejected),
                decision("cmap-token", PurchaseVerificationStatus.verified),
            ),
        )

        assertEquals(SubscriptionTier.FREE, result.restoreResult.entitlementSnapshot.tier)
        assertEquals(emptySet<String>(), result.restoreResult.entitlementSnapshot.ownedChartPackIds)
        assertEquals(emptySet<String>(), result.restoreResult.entitlementSnapshot.licensedChartProviderIds)
        assertTrue("seafox.fleet.monthly" in result.restoreResult.rejectedProductIds)
    }

    @Test
    fun pendingPurchasesAreNotSentForBackendVerification() {
        val result = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(
                purchase(
                    "seafox.pro.monthly",
                    token = "pending-token",
                    state = BillingPurchaseState.pending,
                ),
            ),
            validationDecisions = emptyList(),
        )

        assertEquals(emptyList<BillingValidationRequest>(), result.validationRequests)
        assertEquals(SubscriptionTier.FREE, result.restoreResult.entitlementSnapshot.tier)
        assertTrue("seafox.pro.monthly" in result.restoreResult.pendingProductIds)
    }

    private fun purchase(
        productId: String,
        token: String,
        state: BillingPurchaseState = BillingPurchaseState.purchased,
        verification: PurchaseVerificationStatus = PurchaseVerificationStatus.unverified,
        acknowledged: Boolean = false,
    ): BillingPurchaseRecord {
        return BillingPurchaseRecord(
            productIds = setOf(productId),
            purchaseToken = token,
            purchaseState = state,
            verificationStatus = verification,
            acknowledged = acknowledged,
            purchasedAtEpochMs = 1_000L,
        )
    }

    private fun decision(
        token: String,
        status: PurchaseVerificationStatus,
        validUntilEpochMs: Long? = null,
    ): BillingValidationDecision {
        return BillingValidationDecision(
            purchaseToken = token,
            verificationStatus = status,
            validUntilEpochMs = validUntilEpochMs,
        )
    }
}
