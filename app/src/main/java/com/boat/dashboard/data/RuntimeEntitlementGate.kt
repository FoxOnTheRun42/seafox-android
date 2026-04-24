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
}
