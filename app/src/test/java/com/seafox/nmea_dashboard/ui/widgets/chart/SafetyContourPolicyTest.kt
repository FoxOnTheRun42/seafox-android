package com.seafox.nmea_dashboard.ui.widgets.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyContourPolicyTest {

    @Test
    fun calculatesSafetyDepthFromDraftMarginAndConfiguredDepth() {
        val depth = SafetyContourPolicy.calculateSafetyDepthMeters(
            draftMeters = 1.8,
            marginMeters = 1.2,
            configuredSafetyDepthMeters = 2.5,
        )

        assertEquals(3.0, depth, 0.001)
    }

    @Test
    fun keepsConfiguredDepthWhenItIsMoreConservative() {
        val depth = SafetyContourPolicy.calculateSafetyDepthMeters(
            draftMeters = 1.2,
            marginMeters = 0.8,
            configuredSafetyDepthMeters = 4.0,
        )

        assertEquals(4.0, depth, 0.001)
    }

    @Test
    fun keepsShallowDepthFeaturesAndDropsDeepOnes() {
        assertTrue(
            SafetyContourPolicy.shouldKeepDepthFeature(
                objectCode = "DEPARE",
                depthMeters = 2.0,
                safetyDepthMeters = 3.0,
            )
        )
        assertFalse(
            SafetyContourPolicy.shouldKeepDepthFeature(
                objectCode = "DEPARE",
                depthMeters = 8.0,
                safetyDepthMeters = 3.0,
            )
        )
    }

    @Test
    fun treatsContoursAndSoundingsAsDepthFeatures() {
        assertTrue(SafetyContourPolicy.shouldKeepDepthFeature("DEPCNT", depthMeters = 3.0, safetyDepthMeters = 3.5))
        assertTrue(SafetyContourPolicy.shouldKeepDepthFeature("SOUNDG", depthMeters = 1.4, safetyDepthMeters = 3.5))
        assertFalse(SafetyContourPolicy.shouldKeepDepthFeature("SOUNDG", depthMeters = 12.0, safetyDepthMeters = 3.5))
    }
}
