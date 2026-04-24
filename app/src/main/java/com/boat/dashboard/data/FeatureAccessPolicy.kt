package com.seafox.nmea_dashboard.data

data class FeatureAccessResult(
    val allowed: Boolean,
    val requiredFeature: MonetizedFeature,
)

object FeatureAccessPolicy {
    fun requiredFeatureForWidget(kind: WidgetKind): MonetizedFeature {
        return when (canonicalWidgetKind(kind)) {
            WidgetKind.KARTEN -> MonetizedFeature.onlineCharts
            WidgetKind.AIS -> MonetizedFeature.aisCpa
            WidgetKind.ANCHOR_WATCH -> MonetizedFeature.anchorWatch
            WidgetKind.NMEA_PGN,
            WidgetKind.NMEA0183,
            WidgetKind.SYSTEM_PERFORMANCE -> MonetizedFeature.supportDiagnostics
            else -> MonetizedFeature.basicDashboard
        }
    }

    fun canUseWidget(
        snapshot: EntitlementSnapshot,
        kind: WidgetKind,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): FeatureAccessResult {
        val requiredFeature = requiredFeatureForWidget(kind)
        return FeatureAccessResult(
            allowed = EntitlementPolicy.hasFeature(snapshot, requiredFeature, nowEpochMs),
            requiredFeature = requiredFeature,
        )
    }

    fun canUseFeature(
        snapshot: EntitlementSnapshot,
        feature: MonetizedFeature,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): FeatureAccessResult {
        return FeatureAccessResult(
            allowed = EntitlementPolicy.hasFeature(snapshot, feature, nowEpochMs),
            requiredFeature = feature,
        )
    }

    private fun canonicalWidgetKind(kind: WidgetKind): WidgetKind {
        return when (kind) {
            WidgetKind.SEA_CHART,
            WidgetKind.SEA_CHART_PIXEL -> WidgetKind.KARTEN
            else -> kind
        }
    }
}
