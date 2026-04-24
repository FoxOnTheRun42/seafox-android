package com.seafox.nmea_dashboard.ui.widgets.chart.enc

import com.seafox.nmea_dashboard.data.FeatureAvailability
import com.seafox.nmea_dashboard.ui.widgets.chart.s57.S57Dataset
import com.seafox.nmea_dashboard.ui.widgets.chart.s57.S57Feature

enum class EncChartFormat {
    plainS57,
    oeSencEncrypted,
}

enum class EncGeometryKind {
    point,
    line,
    area,
    unknown,
}

enum class EncRenderLayerRole {
    land,
    shoreline,
    depthArea,
    depthContour,
    sounding,
    danger,
    aidToNavigation,
    restrictedArea,
    trafficRoute,
    portInfrastructure,
    infrastructure,
    otherNavigational,
}

data class EncRendererCapability(
    val format: EncChartFormat,
    val availability: FeatureAvailability,
    val canReadPlainCells: Boolean,
    val canReadEncryptedCells: Boolean,
    val supportsS52Symbols: Boolean,
    val supportsSafetyContourExtraction: Boolean,
    val userNotice: String,
)

data class EncRenderFeaturePlan(
    val objectCode: String,
    val role: EncRenderLayerRole,
    val geometryKind: EncGeometryKind,
    val minZoom: Int?,
    val safetyRelevant: Boolean,
)

data class EncRenderPlan(
    val format: EncChartFormat,
    val availability: FeatureAvailability,
    val featurePlans: List<EncRenderFeaturePlan>,
    val unsupportedObjectCodes: Set<String>,
    val userNotice: String,
)

object EncRendererSkeleton {

    fun capability(format: EncChartFormat): EncRendererCapability {
        return when (format) {
            EncChartFormat.plainS57 -> EncRendererCapability(
                format = format,
                availability = FeatureAvailability.beta,
                canReadPlainCells = true,
                canReadEncryptedCells = false,
                supportsS52Symbols = false,
                supportsSafetyContourExtraction = false,
                userNotice = "S-57 Beta: einfache GeoJSON-/MapLibre-Darstellung, nicht S-52/ECDIS-konform.",
            )
            EncChartFormat.oeSencEncrypted -> EncRendererCapability(
                format = format,
                availability = FeatureAvailability.notImplemented,
                canReadPlainCells = false,
                canReadEncryptedCells = false,
                supportsS52Symbols = false,
                supportsSafetyContourExtraction = false,
                userNotice = "oeSENC ist ein lizenz-/permit-pflichtiger, verschluesselter Renderer-Pfad und noch nicht implementiert.",
            )
        }
    }

    fun planS57Dataset(
        dataset: S57Dataset,
        zoomLevel: Int? = null,
    ): EncRenderPlan {
        val capability = capability(EncChartFormat.plainS57)
        val featurePlans = mutableListOf<EncRenderFeaturePlan>()
        val unsupportedObjectCodes = linkedSetOf<String>()

        dataset.features.forEach { feature ->
            val objectCode = feature.objectCode.trim().uppercase()
            if (objectCode.isBlank() || objectCode.startsWith("M_") || objectCode.startsWith("C_")) {
                return@forEach
            }
            val role = roleForObjectCode(objectCode)
            if (role == null) {
                unsupportedObjectCodes += objectCode
                return@forEach
            }
            val minZoom = feature.minZoom()
            if (zoomLevel != null && minZoom != null && minZoom > zoomLevel) {
                return@forEach
            }

            featurePlans += EncRenderFeaturePlan(
                objectCode = objectCode,
                role = role,
                geometryKind = geometryKindForPrimitive(feature.primitiveType),
                minZoom = minZoom,
                safetyRelevant = role in SAFETY_RELEVANT_ROLES,
            )
        }

        return EncRenderPlan(
            format = capability.format,
            availability = capability.availability,
            featurePlans = featurePlans,
            unsupportedObjectCodes = unsupportedObjectCodes,
            userNotice = capability.userNotice,
        )
    }

    fun roleForObjectCode(objectCode: String): EncRenderLayerRole? {
        return when (objectCode.trim().uppercase()) {
            "LNDARE", "LAKARE", "RIVERS", "BUAARE", "LNDRGN", "LNDELV" -> EncRenderLayerRole.land
            "COALNE", "SLCONS" -> EncRenderLayerRole.shoreline
            "DEPARE", "DRGARE", "SBDARE", "SWPARE" -> EncRenderLayerRole.depthArea
            "DEPCNT" -> EncRenderLayerRole.depthContour
            "SOUNDG" -> EncRenderLayerRole.sounding
            "OBSTRN", "WRECKS", "UWTROC", "WATTUR" -> EncRenderLayerRole.danger
            "BOYCAR", "BOYLAT", "BOYSAW", "BOYSPP", "BOYISD", "BOYINB",
            "BCNCAR", "BCNLAT", "BCNSAW", "BCNSPP", "BCNISD",
            "LIGHTS", "LITFLT", "LITVES", "FOGSIG", "TOPMAR", "DAYMAR",
            "RTPBCN", "RADRFL", "RDOCAL", "NOTMRK", "LNDMRK" -> EncRenderLayerRole.aidToNavigation
            "ACHARE", "ACHBRT", "RESARE", "CTNARE", "MIPARE", "PRCARE",
            "DMPGRD", "FSHZNE", "FSHGRD", "ISTZNE", "OSPARE", "PILBOP" -> EncRenderLayerRole.restrictedArea
            "NAVLNE", "FAIRWY", "TSSLPT", "TSSRON", "TSSBND", "TSSCRS",
            "TSELNE", "DWRTCL", "DWRTPT", "FERYRT" -> EncRenderLayerRole.trafficRoute
            "HRBARE", "HRBFAC", "MORFAC", "BERTHS", "DOCARE", "DRYDOC",
            "FLODOC", "LOKBSN", "CRANES", "PYLONS", "OFSPLF" -> EncRenderLayerRole.portInfrastructure
            "BRIDGE", "CAUSWY", "DAMCON", "DYKCON", "CANALS", "TUNNEL",
            "PIPARE", "PIPSOL", "PIPOHD", "CBLARE", "CBLOHD",
            "BUISGL", "VEGATN", "PRDARE", "ROADWY", "RUNWAY" -> EncRenderLayerRole.infrastructure
            "SNDWAV", "SPRING", "SEAARE" -> EncRenderLayerRole.otherNavigational
            else -> null
        }
    }

    fun geometryKindForPrimitive(primitiveType: Int): EncGeometryKind {
        return when (primitiveType) {
            1 -> EncGeometryKind.point
            2 -> EncGeometryKind.line
            3 -> EncGeometryKind.area
            else -> EncGeometryKind.unknown
        }
    }

    private fun S57Feature.minZoom(): Int? {
        val scamin = attributes[133]?.trim()?.toIntOrNull() ?: return null
        return scaminToMinZoom(scamin)
    }

    private fun scaminToMinZoom(scamin: Int): Int = when {
        scamin <= 10_000 -> 14
        scamin <= 25_000 -> 12
        scamin <= 50_000 -> 10
        scamin <= 100_000 -> 8
        scamin <= 250_000 -> 6
        scamin <= 500_000 -> 4
        else -> 2
    }

    private val SAFETY_RELEVANT_ROLES = setOf(
        EncRenderLayerRole.depthArea,
        EncRenderLayerRole.depthContour,
        EncRenderLayerRole.sounding,
        EncRenderLayerRole.danger,
    )
}
