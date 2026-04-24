package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeEntitlementGateTest {
    @Test
    fun freeTierCanAddCoreChartButNotPremiumNavigationWidget() {
        val free = EntitlementSnapshot(tier = SubscriptionTier.FREE)

        assertTrue(RuntimeEntitlementGate.canAddWidget(free, WidgetKind.KARTEN).allowed)

        val ais = RuntimeEntitlementGate.canAddWidget(free, WidgetKind.AIS)
        assertFalse(ais.allowed)
        assertEquals(MonetizedFeature.aisCpa, ais.requiredFeature)
        assertEquals(SubscriptionTier.PRO, ais.requiredTier)
    }

    @Test
    fun chartPackOwnershipDoesNotUnlockAppWidgets() {
        val chartPackOnly = EntitlementSnapshot(
            ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
        )

        val decision = RuntimeEntitlementGate.canAddWidget(chartPackOnly, WidgetKind.ANCHOR_WATCH)

        assertFalse(decision.allowed)
        assertEquals(SubscriptionTier.PRO, decision.requiredTier)
    }

    @Test
    fun navigatorTierCanAddDiagnosticsWidgets() {
        val navigator = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)

        assertTrue(RuntimeEntitlementGate.canAddWidget(navigator, WidgetKind.NMEA0183).allowed)
        assertTrue(RuntimeEntitlementGate.canAddWidget(navigator, WidgetKind.SYSTEM_PERFORMANCE).allowed)
    }

    @Test
    fun expiredEntitlementProducesExpiredDecision() {
        val expired = EntitlementSnapshot(
            tier = SubscriptionTier.FLEET,
            validUntilEpochMs = 100L,
        )

        val decision = RuntimeEntitlementGate.canAddWidget(
            snapshot = expired,
            kind = WidgetKind.AIS,
            nowEpochMs = 101L,
        )

        assertFalse(decision.allowed)
        assertTrue(decision.expired)
    }

    @Test
    fun denialMessageNamesRequiredTierAndSeparatesChartLicenses() {
        val decision = RuntimeEntitlementGate.canAddWidget(
            snapshot = EntitlementSnapshot(tier = SubscriptionTier.FREE),
            kind = WidgetKind.AIS,
        )

        val message = RuntimeEntitlementGate.denialMessage(WidgetKind.AIS, decision)

        assertTrue(message.contains("Pro"))
        assertTrue(message.contains("Kartenlizenzen"))
    }
}
