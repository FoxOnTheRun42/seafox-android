package com.seafox.nmea_dashboard.ui.widgets.chart

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class HazardOverlayBuilderTest {

    @Test
    fun filtersDepareDepcntAndSoundgFixturesBySafetyDepth() {
        val features = listOf(
            depthFeature("depare-shallow", "DRVAL1" to 2.4, "DRVAL2" to 5.0, objCode = "DEPARE"),
            depthFeature("depare-deep", "DRVAL1" to 7.2, "DRVAL2" to 9.0, objCode = "DEPARE"),
            depthFeature("depcnt-shallow", "VALDCO" to "3.5", objCode = "DEPCNT"),
            depthFeature("depcnt-deep", "VALDCO" to 6.0, objCode = "DEPCNT"),
            depthFeature("soundg-shallow", "VALSOU" to 1.2, objCode = "SOUNDG"),
            depthFeature("soundg-deep", "VALSOU" to "12.0", objCode = "SOUNDG"),
        )

        val kept = HazardOverlayBuilder.filterDepthAreaFeatures(features, safetyDepthMeters = 3.5)

        assertEquals(
            listOf("depare-shallow", "depcnt-shallow", "soundg-shallow"),
            kept.map { it.optString(FIXTURE_ID_KEY) },
        )
    }

    @Test
    fun acceptsDepthObjectCodeFromKindAndDepthAliasFields() {
        val soundgFromKind = depthFeature(
            "soundg-kind-shallow",
            "soundingDepthMeters" to 2.7,
            kind = "SOUNDG",
        )
        val depcntFromKind = depthFeature(
            "depcnt-kind-deep",
            "contourDepthMeters" to 4.4,
            kind = "DEPCNT",
        )
        val depareFromKind = depthFeature(
            "depare-kind-shallow",
            "minDepth" to "3.0",
            kind = "DEPARE",
        )

        val kept = HazardOverlayBuilder.filterDepthAreaFeatures(
            listOf(soundgFromKind, depcntFromKind, depareFromKind),
            safetyDepthMeters = 3.0,
        )

        assertEquals(
            listOf("soundg-kind-shallow", "depare-kind-shallow"),
            kept.map { it.optString(FIXTURE_ID_KEY) },
        )
    }

    @Test
    fun fallsBackToDepthAreaUpperBoundWhenShallowBoundIsMissing() {
        val features = listOf(
            depthFeature("depare-max-shallow", "DRVAL2" to 3.8, objCode = "DEPARE"),
            depthFeature("depare-max-deep", "DRVAL2" to 6.1, objCode = "DEPARE"),
        )

        val kept = HazardOverlayBuilder.filterDepthAreaFeatures(features, safetyDepthMeters = 4.0)

        assertEquals(
            listOf("depare-max-shallow"),
            kept.map { it.optString(FIXTURE_ID_KEY) },
        )
    }

    @Test
    fun rejectsInvalidSafetyDepthAndNonDepthFeatures() {
        val depthFeature = depthFeature("depare-shallow", "DRVAL1" to 2.0, objCode = "DEPARE")
        val nonDepthFeature = depthFeature("light", objCode = "LIGHTS")

        assertEquals(
            emptyList<JSONObject>(),
            HazardOverlayBuilder.filterDepthAreaFeatures(
                listOf(depthFeature),
                safetyDepthMeters = Double.NaN,
            ),
        )
        assertEquals(
            emptyList<JSONObject>(),
            HazardOverlayBuilder.filterDepthAreaFeatures(
                listOf(nonDepthFeature),
                safetyDepthMeters = 3.0,
            ),
        )
    }

    private fun depthFeature(
        id: String,
        vararg depthAttributes: Pair<String, Any?>,
        objCode: String? = null,
        kind: String? = null,
    ): JSONObject {
        return JsonFixture(
            buildMap {
                put("type", "Feature")
                put(FIXTURE_ID_KEY, id)
                put("geometryType", "Point")
                objCode?.let { put("objCode", it) }
                kind?.let { put("kind", it) }
                for ((key, value) in depthAttributes) {
                    put(key, value)
                }
            }
        )
    }

    private class JsonFixture(
        private val values: Map<String, Any?>,
    ) : JSONObject() {
        override fun optString(name: String?): String {
            return values[name]?.toString().orEmpty()
        }

        override fun has(name: String?): Boolean {
            return values.containsKey(name)
        }

        override fun opt(name: String?): Any? {
            return values[name]
        }
    }

    private companion object {
        const val FIXTURE_ID_KEY = "fixtureId"
    }
}
