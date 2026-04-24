package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.data.FeatureAvailability
import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider

enum class ChartProviderCapability {
    rasterTiles,
    vectorTiles,
    s57Enc,
    s63Encrypted,
    safetyContour,
    offlinePackages,
}

enum class ChartPackageLicenseStatus {
    free,
    licenseRequired,
    licensed,
    expired,
    unknown,
}

enum class ChartPackageValidationStatus {
    unknown,
    valid,
    incomplete,
    unsupported,
    expired,
}

data class ChartProviderDescriptor(
    val provider: SeaChartMapProvider,
    val availability: FeatureAvailability,
    val capabilities: Set<ChartProviderCapability>,
    val userNotice: String,
) {
    val canSelect: Boolean
        get() = availability == FeatureAvailability.available || availability == FeatureAvailability.beta
}

object ChartProviderRegistry {

    fun descriptor(provider: SeaChartMapProvider): ChartProviderDescriptor {
        return when (provider) {
            SeaChartMapProvider.NOAA -> ChartProviderDescriptor(
                provider = provider,
                availability = FeatureAvailability.beta,
                capabilities = setOf(
                    ChartProviderCapability.s57Enc,
                    ChartProviderCapability.offlinePackages,
                ),
                userNotice = "NOAA/S-57 ist als Beta verfuegbar; Darstellung ist nicht ECDIS-zertifiziert.",
            )
            SeaChartMapProvider.S57 -> ChartProviderDescriptor(
                provider = provider,
                availability = FeatureAvailability.beta,
                capabilities = setOf(
                    ChartProviderCapability.s57Enc,
                    ChartProviderCapability.offlinePackages,
                ),
                userNotice = "S-57 Import ist Beta und braucht passende ENC-Zellen.",
            )
            SeaChartMapProvider.OPEN_SEA_CHARTS -> ChartProviderDescriptor(
                provider = provider,
                availability = FeatureAvailability.available,
                capabilities = setOf(
                    ChartProviderCapability.rasterTiles,
                    ChartProviderCapability.offlinePackages,
                ),
                userNotice = "OpenSeaCharts/OpenSeaMap ist als Raster-/Overlay-Quelle verfuegbar.",
            )
            SeaChartMapProvider.C_MAP -> ChartProviderDescriptor(
                provider = provider,
                availability = FeatureAvailability.licensedRequired,
                capabilities = setOf(
                    ChartProviderCapability.rasterTiles,
                    ChartProviderCapability.vectorTiles,
                ),
                userNotice = "C-Map wird erst nach Lizenz-/Entitlement-Anbindung aktiviert.",
            )
            SeaChartMapProvider.S63 -> ChartProviderDescriptor(
                provider = provider,
                availability = FeatureAvailability.notImplemented,
                capabilities = setOf(
                    ChartProviderCapability.s63Encrypted,
                ),
                userNotice = "S-63 ist technisch und rechtlich noch nicht produktiv implementiert.",
            )
        }
    }

    fun selectableProviders(): List<SeaChartMapProvider> {
        return SeaChartMapProvider.entries.filter { descriptor(it).canSelect }
    }

    fun normalizedSelectableProvider(
        provider: SeaChartMapProvider,
        fallback: SeaChartMapProvider = SeaChartMapProvider.NOAA,
    ): SeaChartMapProvider {
        val selectable = selectableProviders()
        return when {
            provider in selectable -> provider
            fallback in selectable -> fallback
            else -> selectable.firstOrNull() ?: SeaChartMapProvider.NOAA
        }
    }
}
