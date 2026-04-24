package com.seafox.nmea_dashboard.data

import java.util.Locale

enum class BillingProductKind {
    appSubscription,
    firstPartyChartPack,
    chartLicense,
}

enum class BillingPlayProductType {
    subscription,
    inAppProduct,
    external,
}

data class BillingProductDescriptor(
    val productId: String,
    val kind: BillingProductKind,
    val playProductType: BillingPlayProductType,
    val displayName: String = productId,
    val tier: SubscriptionTier? = null,
    val chartPackId: String? = null,
    val chartProviderId: String? = null,
    val active: Boolean = true,
)

object BillingCatalog {
    const val SEAFOX_PREMIUM_DE_COAST_PACK_ID = "seafox-premium-de-coast"
    const val SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID = "seafox.chartpack.de_coast"

    val products: List<BillingProductDescriptor> = listOf(
        BillingProductDescriptor(
            productId = "seafox.pro.monthly",
            kind = BillingProductKind.appSubscription,
            playProductType = BillingPlayProductType.subscription,
            displayName = "seaFOX Pro monatlich",
            tier = SubscriptionTier.PRO,
        ),
        BillingProductDescriptor(
            productId = "seafox.pro.yearly",
            kind = BillingProductKind.appSubscription,
            playProductType = BillingPlayProductType.subscription,
            displayName = "seaFOX Pro jährlich",
            tier = SubscriptionTier.PRO,
        ),
        BillingProductDescriptor(
            productId = "seafox.navigator.monthly",
            kind = BillingProductKind.appSubscription,
            playProductType = BillingPlayProductType.subscription,
            displayName = "seaFOX Navigator monatlich",
            tier = SubscriptionTier.NAVIGATOR,
        ),
        BillingProductDescriptor(
            productId = "seafox.navigator.yearly",
            kind = BillingProductKind.appSubscription,
            playProductType = BillingPlayProductType.subscription,
            displayName = "seaFOX Navigator jährlich",
            tier = SubscriptionTier.NAVIGATOR,
        ),
        BillingProductDescriptor(
            productId = "seafox.fleet.monthly",
            kind = BillingProductKind.appSubscription,
            playProductType = BillingPlayProductType.subscription,
            displayName = "seaFOX Fleet monatlich",
            tier = SubscriptionTier.FLEET,
        ),
        BillingProductDescriptor(
            productId = "seafox.fleet.yearly",
            kind = BillingProductKind.appSubscription,
            playProductType = BillingPlayProductType.subscription,
            displayName = "seaFOX Fleet jährlich",
            tier = SubscriptionTier.FLEET,
        ),
        BillingProductDescriptor(
            productId = SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID,
            kind = BillingProductKind.firstPartyChartPack,
            playProductType = BillingPlayProductType.inAppProduct,
            displayName = "seaFOX Premium DE-Küste",
            chartPackId = SEAFOX_PREMIUM_DE_COAST_PACK_ID,
        ),
        BillingProductDescriptor(
            productId = "seafox.chart.cmap.external",
            kind = BillingProductKind.chartLicense,
            playProductType = BillingPlayProductType.external,
            chartProviderId = "c-map",
            active = false,
        ),
        BillingProductDescriptor(
            productId = "seafox.chart.s63.external",
            kind = BillingProductKind.chartLicense,
            playProductType = BillingPlayProductType.external,
            chartProviderId = "s-63",
            active = false,
        ),
    )

    fun activeProducts(): List<BillingProductDescriptor> {
        return products.filter { product -> product.active }
    }

    fun activeProductsForPlayType(playProductType: BillingPlayProductType): List<BillingProductDescriptor> {
        return activeProducts().filter { product -> product.playProductType == playProductType }
    }

    fun activeProductForProductId(productId: String): BillingProductDescriptor? {
        val product = findProduct(productId) ?: return null
        return product.takeIf { it.active }
    }

    fun activeSubscriptionProductIds(): Set<String> {
        return activeProductsForPlayType(BillingPlayProductType.subscription)
            .map { product -> product.productId }
            .toSet()
    }

    fun activeInAppProductIds(): Set<String> {
        return activeProductsForPlayType(BillingPlayProductType.inAppProduct)
            .map { product -> product.productId }
            .toSet()
    }

    fun tierForProductId(productId: String): SubscriptionTier? {
        val product = findProduct(productId) ?: return null
        if (!product.active || product.kind != BillingProductKind.appSubscription) return null
        return product.tier
    }

    fun chartPackForProductId(productId: String): String? {
        val product = findProduct(productId) ?: return null
        if (!product.active || product.kind != BillingProductKind.firstPartyChartPack) return null
        return product.chartPackId
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
