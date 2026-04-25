package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.data.FeatureAvailability
import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import java.io.File

enum class ChartSourceBadgeTone {
    neutral,
    info,
    success,
    warning,
    danger,
}

data class ChartSourceBadge(
    val label: String,
    val tone: ChartSourceBadgeTone = ChartSourceBadgeTone.neutral,
)

data class ChartSourceUiState(
    val primaryLabel: String,
    val statusLabel: String,
    val detailLabel: String,
    val badges: List<ChartSourceBadge>,
    val attributionLabel: String?,
    val navigationNotice: String,
    val isRenderable: Boolean,
)

object ChartSourceUiModels {

    fun build(
        mapProvider: SeaChartMapProvider,
        activeMapSourceLabel: String?,
        activeMapSourcePath: String?,
        showOpenSeaMapOverlay: Boolean,
        offlinePackages: List<OfflineChartPackage> = emptyList(),
    ): ChartSourceUiState {
        val descriptor = ChartProviderRegistry.descriptor(mapProvider)
        val trimmedPath = activeMapSourcePath?.trim().orEmpty()
        val effectiveSeamarkOverlay = showOpenSeaMapOverlay ||
            FreeRasterChartProviders.shouldForceSeamarkOverlay(mapProvider)
        val matchedPackage = offlinePackages.firstOrNull { pkg ->
            !pkg.localPath.isNullOrBlank() &&
                pkg.localPath == trimmedPath &&
                pkg.licenseStatus == ChartPackageLicenseStatus.licensed &&
                pkg.validationStatus == ChartPackageValidationStatus.valid
        }
        val onlineLayer = FreeRasterChartProviders.baseLayerFor(mapProvider)
        val sourceLabel = activeMapSourceLabel
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: matchedPackage?.displayName
            ?: onlineLayer?.displayName
            ?: "${mapProvider.label}-Quelle"
        val attributions = buildList {
            onlineLayer?.attribution?.takeIf { it.isNotBlank() }?.let(::add)
            if (effectiveSeamarkOverlay) {
                add("OpenSeaMap Seamark Overlay")
            }
        }.distinct()

        return when {
            matchedPackage != null -> ChartSourceUiState(
                primaryLabel = matchedPackage.displayName,
                statusLabel = "Premium-Paket aktiv",
                detailLabel = "Lizenziert und lokal validiert",
                badges = listOf(
                    ChartSourceBadge("PREMIUM", ChartSourceBadgeTone.success),
                    ChartSourceBadge(matchedPackage.format.uppercase(), ChartSourceBadgeTone.info),
                ) + overlayBadge(effectiveSeamarkOverlay),
                attributionLabel = attributions.joinToString(" / ").takeIf { it.isNotBlank() },
                navigationNotice = "Kartenpaket ist App-Entitlement-getrennt; nicht ECDIS-zertifiziert.",
                isRenderable = true,
            )

            trimmedPath.endsWith(".mbtiles", ignoreCase = true) ||
                trimmedPath.endsWith(".gpkg", ignoreCase = true) ||
                trimmedPath.endsWith(".geopackage", ignoreCase = true) -> ChartSourceUiState(
                    primaryLabel = sourceLabel,
                    statusLabel = "Lokaler Import",
                    detailLabel = File(trimmedPath).name.ifBlank { "Offline-Paket" },
                    badges = listOf(
                        ChartSourceBadge("IMPORT", ChartSourceBadgeTone.info),
                        ChartSourceBadge("OFFLINE", ChartSourceBadgeTone.success),
                    ) + overlayBadge(effectiveSeamarkOverlay),
                    attributionLabel = attributions.joinToString(" / ").takeIf { it.isNotBlank() },
                    navigationNotice = "Nutzerimport; Lizenz, Aktualitaet und Quelle muessen vor Nutzung geprueft werden.",
                    isRenderable = true,
                )

            trimmedPath.isNotBlank() -> ChartSourceUiState(
                primaryLabel = sourceLabel,
                statusLabel = "ENC/Dateiquelle",
                detailLabel = File(trimmedPath).name.ifBlank { mapProvider.label },
                badges = listOf(
                    ChartSourceBadge(availabilityBadgeLabel(descriptor.availability), availabilityTone(descriptor.availability)),
                    ChartSourceBadge("DATEI", ChartSourceBadgeTone.info),
                ) + overlayBadge(effectiveSeamarkOverlay),
                attributionLabel = attributions.joinToString(" / ").takeIf { it.isNotBlank() },
                navigationNotice = descriptor.userNotice,
                isRenderable = descriptor.canSelect,
            )

            onlineLayer != null -> ChartSourceUiState(
                primaryLabel = onlineLayer.displayName,
                statusLabel = if (descriptor.availability == FeatureAvailability.beta) "Online Beta" else "Online aktiv",
                detailLabel = onlineLayer.userNotice,
                badges = listOf(
                    ChartSourceBadge("FREI", ChartSourceBadgeTone.success),
                    ChartSourceBadge("ONLINE", ChartSourceBadgeTone.info),
                    ChartSourceBadge(availabilityBadgeLabel(descriptor.availability), availabilityTone(descriptor.availability)),
                ) + overlayBadge(effectiveSeamarkOverlay),
                attributionLabel = attributions.joinToString(" / ").takeIf { it.isNotBlank() },
                navigationNotice = onlineLayer.navigationDisclaimer,
                isRenderable = true,
            )

            else -> unavailableState(
                mapProvider = mapProvider,
                descriptor = descriptor,
                sourceLabel = sourceLabel,
                showOpenSeaMapOverlay = effectiveSeamarkOverlay,
            )
        }
    }

    private fun unavailableState(
        mapProvider: SeaChartMapProvider,
        descriptor: ChartProviderDescriptor,
        sourceLabel: String,
        showOpenSeaMapOverlay: Boolean,
    ): ChartSourceUiState {
        val statusLabel = when (descriptor.availability) {
            FeatureAvailability.licensedRequired -> "Lizenz erforderlich"
            FeatureAvailability.notImplemented -> "Nicht verfuegbar"
            FeatureAvailability.hidden -> "Ausgeblendet"
            FeatureAvailability.beta -> "Keine lokale Quelle"
            FeatureAvailability.available -> "Keine Quelle"
        }
        return ChartSourceUiState(
            primaryLabel = sourceLabel.ifBlank { mapProvider.label },
            statusLabel = statusLabel,
            detailLabel = descriptor.userNotice,
            badges = listOf(
                ChartSourceBadge(availabilityBadgeLabel(descriptor.availability), availabilityTone(descriptor.availability)),
            ) + overlayBadge(showOpenSeaMapOverlay),
            attributionLabel = null,
            navigationNotice = descriptor.userNotice,
            isRenderable = false,
        )
    }

    private fun overlayBadge(enabled: Boolean): List<ChartSourceBadge> {
        return if (enabled) {
            listOf(ChartSourceBadge("OVERLAY", ChartSourceBadgeTone.info))
        } else {
            emptyList()
        }
    }

    private fun availabilityBadgeLabel(availability: FeatureAvailability): String {
        return when (availability) {
            FeatureAvailability.hidden -> "HIDDEN"
            FeatureAvailability.available -> "AKTIV"
            FeatureAvailability.beta -> "BETA"
            FeatureAvailability.licensedRequired -> "LIZENZ"
            FeatureAvailability.notImplemented -> "NICHT FERTIG"
        }
    }

    private fun availabilityTone(availability: FeatureAvailability): ChartSourceBadgeTone {
        return when (availability) {
            FeatureAvailability.available -> ChartSourceBadgeTone.success
            FeatureAvailability.beta -> ChartSourceBadgeTone.warning
            FeatureAvailability.licensedRequired -> ChartSourceBadgeTone.warning
            FeatureAvailability.notImplemented -> ChartSourceBadgeTone.danger
            FeatureAvailability.hidden -> ChartSourceBadgeTone.neutral
        }
    }
}
