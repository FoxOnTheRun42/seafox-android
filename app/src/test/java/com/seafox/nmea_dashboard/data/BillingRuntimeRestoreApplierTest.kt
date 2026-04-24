package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingRuntimeRestoreApplierTest {
    @Test
    fun missingBackendValidationPreservesCurrentSnapshot() {
        val current = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)
        val coordinated = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(purchase("seafox.pro.monthly", token = "pro-token")),
            validationDecisions = emptyList(),
        )

        val update = BillingRuntimeRestoreApplier.reduce(
            currentSnapshot = current,
            coordinatedResult = coordinated,
            activePurchaseCount = 1,
            backendConfigured = false,
        )

        assertFalse(update.applySnapshot)
        assertEquals(current, update.entitlementSnapshot)
        assertEquals(BillingRuntimeRestoreStatus.requiresBackendValidation, update.status)
        assertEquals(setOf("pro-token"), update.missingValidationTokens)
    }

    @Test
    fun verifiedSubscriptionAppliesRestoredSnapshotAndAckTokens() {
        val coordinated = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(purchase("seafox.navigator.yearly", token = "nav-token")),
            validationDecisions = listOf(
                decision("nav-token", PurchaseVerificationStatus.verified, validUntilEpochMs = 2_000L),
            ),
        )

        val update = BillingRuntimeRestoreApplier.reduce(
            currentSnapshot = EntitlementSnapshot(),
            coordinatedResult = coordinated,
            activePurchaseCount = 1,
            backendConfigured = true,
            nowEpochMs = 1_000L,
        )

        assertTrue(update.applySnapshot)
        assertEquals(SubscriptionTier.NAVIGATOR, update.entitlementSnapshot.tier)
        assertEquals(2_000L, update.entitlementSnapshot.validUntilEpochMs)
        assertEquals(setOf("nav-token"), update.unacknowledgedPurchaseTokens)
        assertEquals(BillingRuntimeRestoreStatus.applied, update.status)
    }

    @Test
    fun successfulRestoreWithNoPurchasesClearsOldEntitlementToFree() {
        val coordinated = BillingRestoreCoordinator.restoreWithValidation(
            purchases = emptyList(),
            validationDecisions = emptyList(),
        )

        val update = BillingRuntimeRestoreApplier.reduce(
            currentSnapshot = EntitlementSnapshot(tier = SubscriptionTier.FLEET),
            coordinatedResult = coordinated,
            activePurchaseCount = 0,
            backendConfigured = true,
        )

        assertTrue(update.applySnapshot)
        assertEquals(SubscriptionTier.FREE, update.entitlementSnapshot.tier)
        assertEquals(BillingRuntimeRestoreStatus.noActivePurchases, update.status)
    }

    @Test
    fun pendingOnlyRestoreDoesNotDowngradeExistingEntitlement() {
        val current = EntitlementSnapshot(tier = SubscriptionTier.PRO)
        val coordinated = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(
                purchase(
                    "seafox.navigator.monthly",
                    token = "pending-token",
                    state = BillingPurchaseState.pending,
                ),
            ),
            validationDecisions = emptyList(),
        )

        val update = BillingRuntimeRestoreApplier.reduce(
            currentSnapshot = current,
            coordinatedResult = coordinated,
            activePurchaseCount = 1,
            backendConfigured = true,
        )

        assertFalse(update.applySnapshot)
        assertEquals(current, update.entitlementSnapshot)
        assertEquals(BillingRuntimeRestoreStatus.pending, update.status)
        assertEquals(setOf("seafox.navigator.monthly"), update.pendingProductIds)
    }

    @Test
    fun verifiedChartPackAppliesOnlyOwnedPack() {
        val coordinated = BillingRestoreCoordinator.restoreWithValidation(
            purchases = listOf(purchase(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID, token = "pack-token")),
            validationDecisions = listOf(decision("pack-token", PurchaseVerificationStatus.verified)),
        )

        val update = BillingRuntimeRestoreApplier.reduce(
            currentSnapshot = EntitlementSnapshot(),
            coordinatedResult = coordinated,
            activePurchaseCount = 1,
            backendConfigured = true,
        )

        assertTrue(update.applySnapshot)
        assertEquals(SubscriptionTier.FREE, update.entitlementSnapshot.tier)
        assertEquals(
            setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
            update.entitlementSnapshot.ownedChartPackIds,
        )
    }

    private fun purchase(
        productId: String,
        token: String,
        state: BillingPurchaseState = BillingPurchaseState.purchased,
        acknowledged: Boolean = false,
    ): BillingPurchaseRecord {
        return BillingPurchaseRecord(
            productIds = setOf(productId),
            purchaseToken = token,
            purchaseState = state,
            verificationStatus = PurchaseVerificationStatus.unverified,
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
