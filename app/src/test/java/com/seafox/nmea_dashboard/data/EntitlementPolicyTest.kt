package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementPolicyTest {

    @Test
    fun freeTierKeepsCoreSafetyAndSimulatorAvailable() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.FREE)

        assertTrue(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.simulator))
        assertTrue(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.mob))
        assertFalse(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.offlinePackages))
    }

    @Test
    fun proUnlocksDashboardNavigationFeaturesButNotNavigatorSafetyContour() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.PRO)

        assertTrue(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.routesAndTracks))
        assertTrue(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.aisCpa))
        assertFalse(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.safetyContour))
    }

    @Test
    fun navigatorUnlocksSafetyContourAndDiagnostics() {
        val snapshot = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)

        assertTrue(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.safetyContour))
        assertTrue(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.supportDiagnostics))
        assertFalse(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.fleetManagement))
    }

    @Test
    fun appTierDoesNotGrantCommercialChartLicenses() {
        val navigator = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)
        val licensed = EntitlementSnapshot(
            tier = SubscriptionTier.PRO,
            licensedChartProviderIds = setOf("c-map"),
        )

        assertFalse(EntitlementPolicy.isChartProviderLicensed(navigator, "c-map"))
        assertTrue(EntitlementPolicy.isChartProviderLicensed(licensed, "C-MAP"))
    }

    @Test
    fun chartPackEntitlementsStaySeparateFromAppTiersAndExternalLicenses() {
        val navigator = EntitlementSnapshot(tier = SubscriptionTier.NAVIGATOR)
        val chartPackOnly = EntitlementSnapshot(
            ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
        )

        assertFalse(
            EntitlementPolicy.isChartPackOwned(navigator, BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
        )
        assertTrue(
            EntitlementPolicy.isChartPackOwned(chartPackOnly, "SEAFOX-PREMIUM-DE-COAST"),
        )
        assertFalse(EntitlementPolicy.isChartProviderLicensed(chartPackOnly, "c-map"))
        assertFalse(EntitlementPolicy.hasFeature(chartPackOnly, MonetizedFeature.offlinePackages))
    }

    @Test
    fun expiredSnapshotGrantsNothing() {
        val snapshot = EntitlementSnapshot(
            tier = SubscriptionTier.FLEET,
            ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
            licensedChartProviderIds = setOf("s63"),
            validUntilEpochMs = 1_000L,
        )

        assertFalse(EntitlementPolicy.hasFeature(snapshot, MonetizedFeature.fleetManagement, nowEpochMs = 1_001L))
        assertFalse(
            EntitlementPolicy.isChartPackOwned(
                snapshot,
                BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID,
                nowEpochMs = 1_001L,
            ),
        )
        assertFalse(EntitlementPolicy.isChartProviderLicensed(snapshot, "s63", nowEpochMs = 1_001L))
    }
}
