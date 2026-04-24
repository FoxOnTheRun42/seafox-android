package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessPolicyTest {

    @Test
    fun freeTierAllowsCoreDashboardAndOnlineCharts() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.FREE)

        assertTrue(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.BATTERY).allowed)
        assertTrue(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.KARTEN).allowed)
        assertTrue(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.SEA_CHART).allowed)
        assertEquals(
            MonetizedFeature.onlineCharts,
            FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.SEA_CHART_PIXEL).requiredFeature,
        )
    }

    @Test
    fun freeTierDoesNotAllowPremiumNavigationAndDiagnosticsWidgets() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.FREE)

        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.AIS).allowed)
        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.ANCHOR_WATCH).allowed)
        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.NMEA_PGN).allowed)
        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.NMEA0183).allowed)
        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.SYSTEM_PERFORMANCE).allowed)
    }

    @Test
    fun proTierAllowsNavigationWidgetsButNotSupportDiagnostics() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.PRO)

        assertTrue(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.AIS).allowed)
        assertTrue(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.ANCHOR_WATCH).allowed)
        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.SYSTEM_PERFORMANCE).allowed)
    }

    @Test
    fun navigatorTierAllowsDiagnosticsAndSafetyContourFeature() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)

        assertTrue(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.NMEA0183).allowed)
        assertTrue(FeatureAccessPolicy.canUseFeature(snapshot, MonetizedFeature.safetyContour).allowed)
        assertFalse(FeatureAccessPolicy.canUseFeature(snapshot, MonetizedFeature.fleetManagement).allowed)
    }

    @Test
    fun premiumChartPackDoesNotUnlockAppFeatureAccess() {
        val snapshot = EntitlementSnapshot(
            ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
        )

        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.AIS).allowed)
        assertFalse(FeatureAccessPolicy.canUseFeature(snapshot, MonetizedFeature.offlinePackages).allowed)
    }

    @Test
    fun expiredEntitlementBlocksPremiumWidgetAccess() {
        val snapshot = EntitlementSnapshot(
            tier = SubscriptionTier.FLEET,
            validUntilEpochMs = 100L,
        )

        assertFalse(FeatureAccessPolicy.canUseWidget(snapshot, WidgetKind.AIS, nowEpochMs = 101L).allowed)
    }
}
