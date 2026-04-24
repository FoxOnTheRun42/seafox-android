package com.seafox.nmea_dashboard.data

import java.util.Locale

enum class SubscriptionTier {
    FREE,
    PRO,
    NAVIGATOR,
    FLEET,
}

enum class MonetizedFeature {
    simulator,
    basicDashboard,
    onlineCharts,
    mob,
    offlinePackages,
    fullWidgetConfig,
    routesAndTracks,
    aisCpa,
    anchorWatch,
    laylines,
    trendCurves,
    safetyContour,
    advancedAlarms,
    importExport,
    supportDiagnostics,
    fleetManagement,
}

data class EntitlementSnapshot(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val ownedChartPackIds: Set<String> = emptySet(),
    val licensedChartProviderIds: Set<String> = emptySet(),
    val validUntilEpochMs: Long? = null,
)

object EntitlementPolicy {
    private val freeFeatures = setOf(
        MonetizedFeature.simulator,
        MonetizedFeature.basicDashboard,
        MonetizedFeature.onlineCharts,
        MonetizedFeature.mob,
    )

    private val proFeatures = freeFeatures + setOf(
        MonetizedFeature.offlinePackages,
        MonetizedFeature.fullWidgetConfig,
        MonetizedFeature.routesAndTracks,
        MonetizedFeature.aisCpa,
        MonetizedFeature.anchorWatch,
        MonetizedFeature.laylines,
        MonetizedFeature.trendCurves,
    )

    private val navigatorFeatures = proFeatures + setOf(
        MonetizedFeature.safetyContour,
        MonetizedFeature.advancedAlarms,
        MonetizedFeature.importExport,
        MonetizedFeature.supportDiagnostics,
    )

    private val fleetFeatures = navigatorFeatures + setOf(
        MonetizedFeature.fleetManagement,
    )

    fun hasFeature(
        snapshot: EntitlementSnapshot,
        feature: MonetizedFeature,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (isExpired(snapshot, nowEpochMs)) return false
        return feature in featuresFor(snapshot.tier)
    }

    fun isChartProviderLicensed(
        snapshot: EntitlementSnapshot,
        providerId: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (isExpired(snapshot, nowEpochMs)) return false
        val normalizedProviderId = providerId.trim().lowercase(Locale.ROOT)
        return normalizedProviderId in snapshot.licensedChartProviderIds.map { it.trim().lowercase(Locale.ROOT) }
    }

    fun isChartPackOwned(
        snapshot: EntitlementSnapshot,
        chartPackId: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (isExpired(snapshot, nowEpochMs)) return false
        val normalizedChartPackId = chartPackId.trim().lowercase(Locale.ROOT)
        return normalizedChartPackId in snapshot.ownedChartPackIds.map { it.trim().lowercase(Locale.ROOT) }
    }

    fun featuresFor(tier: SubscriptionTier): Set<MonetizedFeature> {
        return when (tier) {
            SubscriptionTier.FREE -> freeFeatures
            SubscriptionTier.PRO -> proFeatures
            SubscriptionTier.NAVIGATOR -> navigatorFeatures
            SubscriptionTier.FLEET -> fleetFeatures
        }
    }

    fun isExpired(snapshot: EntitlementSnapshot, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val validUntil = snapshot.validUntilEpochMs ?: return false
        return validUntil <= nowEpochMs
    }
}
