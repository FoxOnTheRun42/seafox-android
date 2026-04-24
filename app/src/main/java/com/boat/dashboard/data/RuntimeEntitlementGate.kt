package com.seafox.nmea_dashboard.data

data class RuntimeEntitlementDecision(
    val allowed: Boolean,
    val requiredFeature: MonetizedFeature,
    val requiredTier: SubscriptionTier?,
    val expired: Boolean,
)

object RuntimeEntitlementGate {
    fun canAddWidget(
        snapshot: EntitlementSnapshot,
        kind: WidgetKind,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): RuntimeEntitlementDecision = evaluateWidgetAccess(
        snapshot = snapshot,
        kind = kind,
        nowEpochMs = nowEpochMs,
    )

    fun canUseWidget(
        snapshot: EntitlementSnapshot,
        kind: WidgetKind,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): RuntimeEntitlementDecision = evaluateWidgetAccess(
        snapshot = snapshot,
        kind = kind,
        nowEpochMs = nowEpochMs,
    )

    fun canUseFeature(
        snapshot: EntitlementSnapshot,
        feature: MonetizedFeature,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): RuntimeEntitlementDecision {
        val access = FeatureAccessPolicy.canUseFeature(
            snapshot = snapshot,
            feature = feature,
            nowEpochMs = nowEpochMs,
        )
        return RuntimeEntitlementDecision(
            allowed = access.allowed,
            requiredFeature = access.requiredFeature,
            requiredTier = requiredTierFor(access.requiredFeature),
            expired = EntitlementPolicy.isExpired(snapshot, nowEpochMs),
        )
    }

    private fun evaluateWidgetAccess(
        snapshot: EntitlementSnapshot,
        kind: WidgetKind,
        nowEpochMs: Long,
    ): RuntimeEntitlementDecision {
        val access = FeatureAccessPolicy.canUseWidget(
            snapshot = snapshot,
            kind = kind,
            nowEpochMs = nowEpochMs,
        )
        return RuntimeEntitlementDecision(
            allowed = access.allowed,
            requiredFeature = access.requiredFeature,
            requiredTier = requiredTierFor(access.requiredFeature),
            expired = EntitlementPolicy.isExpired(snapshot, nowEpochMs),
        )
    }

    fun denialMessage(
        kind: WidgetKind,
        decision: RuntimeEntitlementDecision,
    ): String {
        if (decision.allowed) return ""
        val title = widgetTitleForKind(kind)
        if (decision.expired) {
            return "$title ist gesperrt, weil dein seaFOX-Zugang abgelaufen ist. Bitte Abo wiederherstellen oder erneuern."
        }
        val tier = decision.requiredTier?.label ?: "eine hoehere Stufe"
        return "$title braucht $tier. Kartenlizenzen und Kartenpakete schalten App-Funktionen nicht automatisch frei."
    }

    fun denialMessage(
        feature: MonetizedFeature,
        decision: RuntimeEntitlementDecision,
    ): String {
        if (decision.allowed) return ""
        val title = featureTitleFor(feature)
        if (decision.expired) {
            return "$title ist gesperrt, weil dein seaFOX-Zugang abgelaufen ist. Bitte Abo wiederherstellen oder erneuern."
        }
        val tier = decision.requiredTier?.label ?: "eine hoehere Stufe"
        return "$title braucht $tier. Kartenlizenzen und Kartenpakete schalten App-Funktionen nicht automatisch frei."
    }

    private fun requiredTierFor(feature: MonetizedFeature): SubscriptionTier? {
        return when {
            feature in EntitlementPolicy.featuresFor(SubscriptionTier.FREE) -> SubscriptionTier.FREE
            feature in EntitlementPolicy.featuresFor(SubscriptionTier.PRO) -> SubscriptionTier.PRO
            feature in EntitlementPolicy.featuresFor(SubscriptionTier.NAVIGATOR) -> SubscriptionTier.NAVIGATOR
            feature in EntitlementPolicy.featuresFor(SubscriptionTier.FLEET) -> SubscriptionTier.FLEET
            else -> null
        }
    }

    private val SubscriptionTier.label: String
        get() = when (this) {
            SubscriptionTier.FREE -> "Free"
            SubscriptionTier.PRO -> "Pro"
            SubscriptionTier.NAVIGATOR -> "Navigator"
            SubscriptionTier.FLEET -> "Fleet"
        }

    private fun featureTitleFor(feature: MonetizedFeature): String {
        return when (feature) {
            MonetizedFeature.simulator -> "Simulator"
            MonetizedFeature.basicDashboard -> "Basis-Dashboard"
            MonetizedFeature.onlineCharts -> "Online-Karten"
            MonetizedFeature.mob -> "MOB"
            MonetizedFeature.offlinePackages -> "Offline-Pakete"
            MonetizedFeature.fullWidgetConfig -> "Widget-Konfiguration"
            MonetizedFeature.routesAndTracks -> "Routen und Tracks"
            MonetizedFeature.aisCpa -> "AIS CPA"
            MonetizedFeature.anchorWatch -> "Ankerwache"
            MonetizedFeature.laylines -> "Laylines"
            MonetizedFeature.trendCurves -> "Trendkurven"
            MonetizedFeature.safetyContour -> "Safety Contour"
            MonetizedFeature.advancedAlarms -> "Erweiterte Alarme"
            MonetizedFeature.importExport -> "Import/Export"
            MonetizedFeature.supportDiagnostics -> "Support-Diagnose"
            MonetizedFeature.fleetManagement -> "Fleet Management"
        }
    }
}
