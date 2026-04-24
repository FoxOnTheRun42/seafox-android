package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingEntitlementMapperTest {

    @Test
    fun verifiedPurchasedSubscriptionRestoresHighestTier() {
        val result = BillingEntitlementMapper.restoreFromPurchases(
            listOf(
                purchase("seafox.pro.monthly", token = "pro-token"),
                purchase("seafox.navigator.yearly", token = "nav-token"),
            ),
        )

        assertEquals(SubscriptionTier.NAVIGATOR, result.entitlementSnapshot.tier)
        assertTrue("pro-token" in result.unacknowledgedPurchaseTokens)
        assertTrue("nav-token" in result.unacknowledgedPurchaseTokens)
        assertFalse(result.needsBackendVerification)
    }

    @Test
    fun pendingAndUnverifiedPurchasesDoNotGrantEntitlements() {
        val result = BillingEntitlementMapper.restoreFromPurchases(
            listOf(
                purchase(
                    "seafox.fleet.monthly",
                    state = BillingPurchaseState.pending,
                    verification = PurchaseVerificationStatus.verified,
                ),
                purchase(
                    "seafox.navigator.monthly",
                    verification = PurchaseVerificationStatus.unverified,
                ),
            ),
        )

        assertEquals(SubscriptionTier.FREE, result.entitlementSnapshot.tier)
        assertTrue("seafox.fleet.monthly" in result.pendingProductIds)
        assertTrue(result.needsBackendVerification)
    }

    @Test
    fun rejectedPurchasesAreTrackedButNotGranted() {
        val result = BillingEntitlementMapper.restoreFromPurchases(
            listOf(
                purchase(
                    "seafox.fleet.yearly",
                    verification = PurchaseVerificationStatus.rejected,
                )
            ),
        )

        assertEquals(SubscriptionTier.FREE, result.entitlementSnapshot.tier)
        assertTrue("seafox.fleet.yearly" in result.rejectedProductIds)
    }

    @Test
    fun inactiveChartPlaceholdersDoNotGrantChartLicenses() {
        val result = BillingEntitlementMapper.restoreFromPurchases(
            listOf(purchase("seafox.chart.cmap.external")),
        )

        assertEquals(emptySet<String>(), result.entitlementSnapshot.licensedChartProviderIds)
        assertEquals(SubscriptionTier.FREE, result.entitlementSnapshot.tier)
    }

    @Test
    fun acknowledgedPurchasesDoNotNeedClientAcknowledgeAgain() {
        val result = BillingEntitlementMapper.restoreFromPurchases(
            listOf(purchase("seafox.pro.yearly", token = "already-acked", acknowledged = true)),
        )

        assertEquals(SubscriptionTier.PRO, result.entitlementSnapshot.tier)
        assertTrue(result.unacknowledgedPurchaseTokens.isEmpty())
    }

    private fun purchase(
        productId: String,
        token: String = "token-$productId",
        state: BillingPurchaseState = BillingPurchaseState.purchased,
        verification: PurchaseVerificationStatus = PurchaseVerificationStatus.verified,
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
}
