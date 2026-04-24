package com.seafox.nmea_dashboard.ui.widgets.chart

import org.maplibre.android.geometry.LatLngBounds

/**
 * Describes an offline chart bundle that can be discovered, listed or downloaded.
 */
data class OfflineChartPackage(
    val id: String,
    val displayName: String,
    val providerId: String,
    val providerType: ChartProviderType,
    val format: String,
    val localPath: String? = null,
    val downloadUri: String? = null,
    val coverageBounds: LatLngBounds? = null,
    val minZoom: Int? = null,
    val maxZoom: Int? = null,
    val sizeBytes: Long? = null,
    val description: String? = null,
    val lastModifiedEpochMs: Long? = null,
    val licenseStatus: ChartPackageLicenseStatus = ChartPackageLicenseStatus.unknown,
    val expiresAtEpochMs: Long? = null,
    val capabilities: Set<ChartProviderCapability> = emptySet(),
    val validationStatus: ChartPackageValidationStatus = ChartPackageValidationStatus.unknown,
)
