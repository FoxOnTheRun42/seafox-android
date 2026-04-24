package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCatalogTest {

    @Test
    fun productIdsAreUnique() {
        assertTrue(BillingCatalog.hasUniqueProductIds())
    }

    @Test
    fun mapsActiveAppSubscriptionIdsToTiers() {
        assertEquals(SubscriptionTier.PRO, BillingCatalog.tierForProductId("seafox.pro.monthly"))
        assertEquals(SubscriptionTier.NAVIGATOR, BillingCatalog.tierForProductId("seafox.navigator.yearly"))
        assertEquals(SubscriptionTier.FLEET, BillingCatalog.tierForProductId(" SEAFOX.FLEET.MONTHLY "))
    }

    @Test
    fun freeTierHasNoBillingProduct() {
        assertNull(BillingCatalog.tierForProductId("seafox.free.monthly"))
    }

    @Test
    fun inactiveChartPlaceholdersDoNotGrantAppTiersOrChartLicenses() {
        assertNull(BillingCatalog.tierForProductId("seafox.chart.cmap.external"))
        assertNull(BillingCatalog.chartPackForProductId("seafox.chart.cmap.external"))
        assertNull(BillingCatalog.chartProviderForProductId("seafox.chart.cmap.external"))
        assertNull(BillingCatalog.chartProviderForProductId("seafox.chart.s63.external"))
    }

    @Test
    fun activeCatalogContainsAppSubscriptionsAndFirstPartyChartPacksOnly() {
        val activeKinds = BillingCatalog.activeProducts().map { product -> product.kind }.toSet()

        assertEquals(
            setOf(BillingProductKind.appSubscription, BillingProductKind.firstPartyChartPack),
            activeKinds,
        )
    }

    @Test
    fun mapsPremiumChartPackToInAppProductWithoutGrantingAppTierOrExternalLicense() {
        assertEquals(
            BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID,
            BillingCatalog.chartPackForProductId(" SEAFOX.CHARTPACK.DE_COAST "),
        )
        assertNull(BillingCatalog.tierForProductId(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID))
        assertNull(BillingCatalog.chartProviderForProductId(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID))
    }

    @Test
    fun exposesPlayProductTypesForRestoreQueries() {
        assertTrue("seafox.pro.monthly" in BillingCatalog.activeSubscriptionProductIds())
        assertTrue(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID in BillingCatalog.activeInAppProductIds())
        assertTrue(
            BillingCatalog.activeProductsForPlayType(BillingPlayProductType.external).isEmpty(),
        )
    }
}
