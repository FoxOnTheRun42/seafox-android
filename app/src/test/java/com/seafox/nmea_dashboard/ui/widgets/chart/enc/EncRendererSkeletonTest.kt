package com.seafox.nmea_dashboard.ui.widgets.chart.enc

import com.seafox.nmea_dashboard.data.FeatureAvailability
import com.seafox.nmea_dashboard.ui.widgets.chart.s57.S57Dataset
import com.seafox.nmea_dashboard.ui.widgets.chart.s57.S57Feature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncRendererSkeletonTest {

    @Test
    fun plainS57CapabilityIsBetaAndNonCertified() {
        val capability = EncRendererSkeleton.capability(EncChartFormat.plainS57)

        assertEquals(FeatureAvailability.beta, capability.availability)
        assertTrue(capability.canReadPlainCells)
        assertFalse(capability.canReadEncryptedCells)
        assertFalse(capability.supportsS52Symbols)
        assertFalse(capability.supportsSafetyContourExtraction)
        assertTrue(capability.userNotice.contains("nicht S-52/ECDIS-konform"))
    }

    @Test
    fun oeSencCapabilityStaysExplicitlyUnavailable() {
        val capability = EncRendererSkeleton.capability(EncChartFormat.oeSencEncrypted)

        assertEquals(FeatureAvailability.notImplemented, capability.availability)
        assertFalse(capability.canReadPlainCells)
        assertFalse(capability.canReadEncryptedCells)
        assertFalse(capability.supportsS52Symbols)
        assertTrue(capability.userNotice.contains("noch nicht implementiert"))
    }

    @Test
    fun planGroupsSafetyRelevantS57FeaturesWithoutClaimingUnsupportedObjects() {
        val plan = EncRendererSkeleton.planS57Dataset(
            S57Dataset(
                features = listOf(
                    feature("DEPARE", primitiveType = 3, scamin = 50_000),
                    feature("DEPCNT", primitiveType = 2),
                    feature("SOUNDG", primitiveType = 1),
                    feature("BOYLAT", primitiveType = 1),
                    feature("M_COVR", primitiveType = 3),
                    feature("CUSTOM", primitiveType = 2),
                ),
            ),
            zoomLevel = 10,
        )

        assertEquals(EncChartFormat.plainS57, plan.format)
        assertEquals(FeatureAvailability.beta, plan.availability)
        assertEquals(setOf("CUSTOM"), plan.unsupportedObjectCodes)
        assertEquals(
            listOf(
                EncRenderLayerRole.depthArea,
                EncRenderLayerRole.depthContour,
                EncRenderLayerRole.sounding,
                EncRenderLayerRole.aidToNavigation,
            ),
            plan.featurePlans.map { it.role },
        )
        assertTrue(plan.featurePlans.filter { it.safetyRelevant }.map { it.objectCode }.containsAll(listOf("DEPARE", "DEPCNT", "SOUNDG")))
        assertFalse(plan.featurePlans.first { it.objectCode == "BOYLAT" }.safetyRelevant)
    }

    @Test
    fun planRespectsScaminDerivedZoomGate() {
        val lowZoomPlan = EncRendererSkeleton.planS57Dataset(
            S57Dataset(features = listOf(feature("DEPARE", primitiveType = 3, scamin = 10_000))),
            zoomLevel = 10,
        )
        val highZoomPlan = EncRendererSkeleton.planS57Dataset(
            S57Dataset(features = listOf(feature("DEPARE", primitiveType = 3, scamin = 10_000))),
            zoomLevel = 14,
        )

        assertTrue(lowZoomPlan.featurePlans.isEmpty())
        assertEquals(1, highZoomPlan.featurePlans.size)
        assertEquals(14, highZoomPlan.featurePlans.single().minZoom)
    }

    private fun feature(
        objectCode: String,
        primitiveType: Int,
        scamin: Int? = null,
    ): S57Feature {
        return S57Feature(
            objectCode = objectCode,
            objectClassCode = 0,
            primitiveType = primitiveType,
            attributes = scamin?.let { mapOf(133 to it.toString()) }.orEmpty(),
            spatialRefs = emptyList(),
        )
    }
}
