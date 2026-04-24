package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.data.BillingCatalog
import com.seafox.nmea_dashboard.data.EntitlementPolicy
import com.seafox.nmea_dashboard.data.EntitlementSnapshot
import java.io.File
import java.util.Locale

data class FirstPartyChartPackageDescriptor(
    val id: String,
    val productId: String,
    val displayName: String,
    val providerId: String,
    val format: String,
    val description: String,
    val capabilities: Set<ChartProviderCapability>,
    val fileNames: Set<String>,
    val downloadUri: String? = null,
)

object FirstPartyChartPackages {
    const val PROVIDER_ID = "seafox-premium"

    val descriptors: List<FirstPartyChartPackageDescriptor> = listOf(
        FirstPartyChartPackageDescriptor(
            id = BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID,
            productId = BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID,
            displayName = "seaFOX Premium Pack DE Coast",
            providerId = PROVIDER_ID,
            format = "Raster MBTiles",
            description = "First-party Offline-Kartenpaket fuer den spaeteren seaFOX Premium-Pack-Vertrieb.",
            capabilities = setOf(
                ChartProviderCapability.rasterTiles,
                ChartProviderCapability.offlinePackages,
            ),
            fileNames = setOf(
                "seafox-premium-de-coast.mbtiles",
                "de-coast.mbtiles",
            ),
        ),
    )

    fun descriptor(packageId: String): FirstPartyChartPackageDescriptor? {
        val normalizedId = packageId.trim().lowercase(Locale.ROOT)
        return descriptors.firstOrNull { descriptor ->
            descriptor.id.lowercase(Locale.ROOT) == normalizedId
        }
    }

    fun offlinePackage(
        descriptor: FirstPartyChartPackageDescriptor,
        entitlementSnapshot: EntitlementSnapshot,
        localPath: String? = null,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): OfflineChartPackage {
        val owned = EntitlementPolicy.isChartPackOwned(
            snapshot = entitlementSnapshot,
            chartPackId = descriptor.id,
            nowEpochMs = nowEpochMs,
        )
        val licenseStatus = when {
            EntitlementPolicy.isExpired(entitlementSnapshot, nowEpochMs) -> ChartPackageLicenseStatus.expired
            owned -> ChartPackageLicenseStatus.licensed
            else -> ChartPackageLicenseStatus.licenseRequired
        }
        val validationStatus = when {
            !owned -> ChartPackageValidationStatus.unknown
            localPath.isNullOrBlank() -> ChartPackageValidationStatus.incomplete
            else -> ChartPackageValidationStatus.valid
        }

        return OfflineChartPackage(
            id = descriptor.id,
            displayName = descriptor.displayName,
            providerId = descriptor.providerId,
            providerType = ChartProviderType.RASTER_TILES,
            format = descriptor.format,
            localPath = localPath,
            downloadUri = descriptor.downloadUri,
            description = descriptor.description,
            licenseStatus = licenseStatus,
            expiresAtEpochMs = entitlementSnapshot.validUntilEpochMs,
            capabilities = descriptor.capabilities,
            validationStatus = validationStatus,
        )
    }

    fun offlinePackages(
        entitlementSnapshot: EntitlementSnapshot,
        localPathsByPackageId: Map<String, String> = emptyMap(),
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<OfflineChartPackage> {
        return descriptors.map { descriptor ->
            offlinePackage(
                descriptor = descriptor,
                entitlementSnapshot = entitlementSnapshot,
                localPath = localPathsByPackageId[descriptor.id],
                nowEpochMs = nowEpochMs,
            )
        }
    }

    fun discoverLocalPaths(
        roots: List<File>,
    ): Map<String, String> {
        if (roots.isEmpty()) return emptyMap()
        return descriptors.mapNotNull { descriptor ->
            val file = roots
                .asSequence()
                .flatMap { root ->
                    descriptor.fileNames.asSequence().map { fileName -> File(root, fileName) }
                }
                .firstOrNull { candidate -> candidate.isFile && candidate.length() > 0L }
            file?.let { descriptor.id to it.absolutePath }
        }.toMap()
    }
}
