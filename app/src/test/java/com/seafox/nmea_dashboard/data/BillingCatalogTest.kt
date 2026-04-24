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
        assertNull(BillingCatalog.chartProviderForProductId("seafox.chart.cmap.external"))
        assertNull(BillingCatalog.chartProviderForProductId("seafox.chart.s63.external"))
    }

    @Test
    fun activeCatalogContainsOnlySellableAppProductsForNow() {
        val activeKinds = BillingCatalog.activeProducts().map { product -> product.kind }.toSet()

        assertEquals(setOf(BillingProductKind.appSubscription), activeKinds)
    }
}
