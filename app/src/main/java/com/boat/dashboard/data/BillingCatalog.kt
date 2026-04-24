package com.seafox.nmea_dashboard.data

import java.util.Locale

enum class BillingProductKind {
    appSubscription,
    chartLicense,
}

data class BillingProductDescriptor(
    val productId: String,
    val kind: BillingProductKind,
    val tier: SubscriptionTier? = null,
    val chartProviderId: String? = null,
    val active: Boolean = true,
)

object BillingCatalog {
    val products: List<BillingProductDescriptor> = listOf(
        BillingProductDescriptor(
            productId = "seafox.pro.monthly",
            kind = BillingProductKind.appSubscription,
            tier = SubscriptionTier.PRO,
        ),
        BillingProductDescriptor(
            productId = "seafox.pro.yearly",
            kind = BillingProductKind.appSubscription,
            tier = SubscriptionTier.PRO,
        ),
        BillingProductDescriptor(
            productId = "seafox.navigator.monthly",
            kind = BillingProductKind.appSubscription,
            tier = SubscriptionTier.NAVIGATOR,
        ),
        BillingProductDescriptor(
            productId = "seafox.navigator.yearly",
            kind = BillingProductKind.appSubscription,
            tier = SubscriptionTier.NAVIGATOR,
        ),
        BillingProductDescriptor(
            productId = "seafox.fleet.monthly",
            kind = BillingProductKind.appSubscription,
            tier = SubscriptionTier.FLEET,
        ),
        BillingProductDescriptor(
            productId = "seafox.fleet.yearly",
            kind = BillingProductKind.appSubscription,
            tier = SubscriptionTier.FLEET,
        ),
        BillingProductDescriptor(
            productId = "seafox.chart.cmap.external",
            kind = BillingProductKind.chartLicense,
            chartProviderId = "c-map",
            active = false,
        ),
        BillingProductDescriptor(
            productId = "seafox.chart.s63.external",
            kind = BillingProductKind.chartLicense,
            chartProviderId = "s-63",
            active = false,
        ),
    )

    fun activeProducts(): List<BillingProductDescriptor> {
        return products.filter { product -> product.active }
    }

    fun tierForProductId(productId: String): SubscriptionTier? {
        val product = findProduct(productId) ?: return null
        if (!product.active || product.kind != BillingProductKind.appSubscription) return null
        return product.tier
    }

    fun chartProviderForProductId(productId: String): String? {
        val product = findProduct(productId) ?: return null
        if (!product.active || product.kind != BillingProductKind.chartLicense) return null
        return product.chartProviderId
    }

    fun hasUniqueProductIds(): Boolean {
        val normalizedIds = products.map { product -> normalize(product.productId) }
        return normalizedIds.size == normalizedIds.toSet().size
    }

    private fun findProduct(productId: String): BillingProductDescriptor? {
        val normalized = normalize(productId)
        return products.firstOrNull { product -> normalize(product.productId) == normalized }
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
}
