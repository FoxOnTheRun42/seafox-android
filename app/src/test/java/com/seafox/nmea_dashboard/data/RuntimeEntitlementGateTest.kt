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
    fun existingPremiumWidgetIsLockedAfterEntitlementExpires() {
        val expired = EntitlementSnapshot(
            tier = SubscriptionTier.PRO,
            validUntilEpochMs = 100L,
        )

        val decision = RuntimeEntitlementGate.canUseWidget(
            snapshot = expired,
            kind = WidgetKind.AIS,
            nowEpochMs = 101L,
        )

        assertFalse(decision.allowed)
        assertTrue(decision.expired)
        assertEquals(MonetizedFeature.aisCpa, decision.requiredFeature)
        assertEquals(SubscriptionTier.PRO, decision.requiredTier)
        assertTrue(RuntimeEntitlementGate.denialMessage(WidgetKind.AIS, decision).contains("abgelaufen"))
    }

    @Test
    fun existingNavigatorWidgetIsLockedAfterFreeRestore() {
        val freeAfterRestore = EntitlementSnapshot(tier = SubscriptionTier.FREE)

        val nmea0183 = RuntimeEntitlementGate.canUseWidget(freeAfterRestore, WidgetKind.NMEA0183)
        val system = RuntimeEntitlementGate.canUseWidget(freeAfterRestore, WidgetKind.SYSTEM_PERFORMANCE)

        assertFalse(nmea0183.allowed)
        assertFalse(system.allowed)
        assertFalse(nmea0183.expired)
        assertEquals(MonetizedFeature.supportDiagnostics, nmea0183.requiredFeature)
        assertEquals(SubscriptionTier.NAVIGATOR, nmea0183.requiredTier)
        assertEquals(SubscriptionTier.NAVIGATOR, system.requiredTier)
    }

    @Test
    fun supportDiagnosticsFeatureRequiresNavigatorTier() {
        val free = EntitlementSnapshot(tier = SubscriptionTier.FREE)
        val navigator = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)

        val denied = RuntimeEntitlementGate.canUseFeature(free, MonetizedFeature.supportDiagnostics)
        val allowed = RuntimeEntitlementGate.canUseFeature(navigator, MonetizedFeature.supportDiagnostics)

        assertFalse(denied.allowed)
        assertEquals(MonetizedFeature.supportDiagnostics, denied.requiredFeature)
        assertEquals(SubscriptionTier.NAVIGATOR, denied.requiredTier)
        assertTrue(RuntimeEntitlementGate.denialMessage(MonetizedFeature.supportDiagnostics, denied).contains("Navigator"))
        assertTrue(allowed.allowed)
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
