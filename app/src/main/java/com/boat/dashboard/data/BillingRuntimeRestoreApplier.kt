package com.seafox.nmea_dashboard.data

enum class BillingRuntimeRestoreStatus {
    applied,
    noActivePurchases,
    pending,
    requiresBackendValidation,
    expired,
}

data class BillingRuntimeRestoreUpdate(
    val applySnapshot: Boolean,
    val entitlementSnapshot: EntitlementSnapshot,
    val status: BillingRuntimeRestoreStatus,
    val message: String,
    val unacknowledgedPurchaseTokens: Set<String>,
    val pendingProductIds: Set<String>,
    val rejectedProductIds: Set<String>,
    val missingValidationTokens: Set<String>,
)

object BillingRuntimeRestoreApplier {
    fun reduce(
        currentSnapshot: EntitlementSnapshot,
        coordinatedResult: BillingCoordinatedRestoreResult,
        activePurchaseCount: Int,
        backendConfigured: Boolean,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): BillingRuntimeRestoreUpdate {
        val restore = coordinatedResult.restoreResult
        val restoredSnapshot = restore.entitlementSnapshot
        val hasRestoredEntitlement = restoredSnapshot.hasAnyPaidEntitlement()

        if (coordinatedResult.missingValidationTokens.isNotEmpty() || restore.needsBackendVerification) {
            return BillingRuntimeRestoreUpdate(
                applySnapshot = false,
                entitlementSnapshot = currentSnapshot,
                status = BillingRuntimeRestoreStatus.requiresBackendValidation,
                message = if (backendConfigured) {
                    "Play-Käufe gefunden, aber nicht vollständig servervalidiert. Bestehende Freischaltung bleibt unverändert."
                } else {
                    "Play-Käufe gefunden, aber kein Billing-Validation-Endpoint ist konfiguriert. Es wurde nichts freigeschaltet."
                },
                unacknowledgedPurchaseTokens = emptySet(),
                pendingProductIds = restore.pendingProductIds,
                rejectedProductIds = restore.rejectedProductIds,
                missingValidationTokens = coordinatedResult.missingValidationTokens,
            )
        }

        if (restore.pendingProductIds.isNotEmpty() && !hasRestoredEntitlement) {
            return BillingRuntimeRestoreUpdate(
                applySnapshot = false,
                entitlementSnapshot = currentSnapshot,
                status = BillingRuntimeRestoreStatus.pending,
                message = "Kauf ist bei Google Play noch ausstehend. Bestehende Freischaltung bleibt unverändert.",
                unacknowledgedPurchaseTokens = emptySet(),
                pendingProductIds = restore.pendingProductIds,
                rejectedProductIds = restore.rejectedProductIds,
                missingValidationTokens = emptySet(),
            )
        }

        val status = when {
            hasRestoredEntitlement && EntitlementPolicy.isExpired(restoredSnapshot, nowEpochMs) -> {
                BillingRuntimeRestoreStatus.expired
            }
            activePurchaseCount == 0 || !hasRestoredEntitlement -> BillingRuntimeRestoreStatus.noActivePurchases
            else -> BillingRuntimeRestoreStatus.applied
        }

        return BillingRuntimeRestoreUpdate(
            applySnapshot = true,
            entitlementSnapshot = restoredSnapshot,
            status = status,
            message = restoreMessage(status, restoredSnapshot),
            unacknowledgedPurchaseTokens = restore.unacknowledgedPurchaseTokens,
            pendingProductIds = restore.pendingProductIds,
            rejectedProductIds = restore.rejectedProductIds,
            missingValidationTokens = emptySet(),
        )
    }

    private fun restoreMessage(
        status: BillingRuntimeRestoreStatus,
        snapshot: EntitlementSnapshot,
    ): String {
        return when (status) {
            BillingRuntimeRestoreStatus.applied -> {
                val packs = snapshot.ownedChartPackIds.size
                val packSuffix = if (packs > 0) " und $packs Kartenpaket(e)" else ""
                "Freischaltung wiederhergestellt: ${snapshot.tier.displayName()}$packSuffix."
            }
            BillingRuntimeRestoreStatus.noActivePurchases -> {
                "Keine aktiven Play-Käufe gefunden. seaFOX läuft im Free-Modus."
            }
            BillingRuntimeRestoreStatus.expired -> {
                "Play-Kauf wurde wiederhergestellt, ist aber abgelaufen. Premium-Funktionen bleiben gesperrt."
            }
            BillingRuntimeRestoreStatus.pending -> {
                "Kauf ist bei Google Play noch ausstehend. Bestehende Freischaltung bleibt unverändert."
            }
            BillingRuntimeRestoreStatus.requiresBackendValidation -> {
                "Play-Kauf benötigt Servervalidierung. Bestehende Freischaltung bleibt unverändert."
            }
        }
    }

    private fun EntitlementSnapshot.hasAnyPaidEntitlement(): Boolean {
        return tier != SubscriptionTier.FREE ||
            ownedChartPackIds.isNotEmpty() ||
            licensedChartProviderIds.isNotEmpty()
    }

    private fun SubscriptionTier.displayName(): String {
        return when (this) {
            SubscriptionTier.FREE -> "Free"
            SubscriptionTier.PRO -> "Pro"
            SubscriptionTier.NAVIGATOR -> "Navigator"
            SubscriptionTier.FLEET -> "Fleet"
        }
    }
}
