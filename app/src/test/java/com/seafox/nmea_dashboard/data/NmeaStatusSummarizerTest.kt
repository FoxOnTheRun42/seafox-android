package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NmeaStatusSummarizerTest {
    @Test
    fun simulationTakesPrecedenceOverLiveSources() {
        val state = DashboardState(
            simulationEnabled = true,
            detectedNmeaSources = listOf(
                NmeaSourceProfile(
                    sourceKey = "router",
                    pgns = listOf(129025, 130306),
                    lastSeenMs = 1_000L,
                ),
            ),
        )

        val summary = NmeaStatusSummarizer.summarize(state, nowEpochMs = 2_000L)

        assertEquals(NmeaConnectionStatus.DEMO, summary.status)
        assertEquals("Demo aktiv", summary.label)
        assertEquals(2, summary.detectedPgnCount)
    }

    @Test
    fun noSourcesIsDisconnected() {
        val state = DashboardState(
            nmeaRouterProtocol = NmeaRouterProtocol.UDP,
            udpPort = 41449,
        )

        val summary = NmeaStatusSummarizer.summarize(state, nowEpochMs = 10_000L)

        assertEquals(NmeaConnectionStatus.DISCONNECTED, summary.status)
        assertEquals("NMEA getrennt", summary.label)
        assertTrue(summary.detail.contains("41449"))
    }

    @Test
    fun freshSourcesAreLiveAndCountDistinctPgns() {
        val state = DashboardState(
            nmeaRouterProtocol = NmeaRouterProtocol.TCP,
            nmeaRouterHost = "192.168.1.20",
            udpPort = 2000,
            detectedNmeaSources = listOf(
                NmeaSourceProfile("src-a", pgns = listOf(129025, 130306), lastSeenMs = 9_500L),
                NmeaSourceProfile("src-b", pgns = listOf(129025, 127250), lastSeenMs = 9_800L),
            ),
        )

        val summary = NmeaStatusSummarizer.summarize(state, nowEpochMs = 10_000L)

        assertEquals(NmeaConnectionStatus.LIVE, summary.status)
        assertEquals(3, summary.detectedPgnCount)
        assertEquals(2, summary.detectedSourceCount)
        assertTrue(summary.label.contains("TCP"))
        assertTrue(summary.label.contains("3 PGNs"))
    }

    @Test
    fun oldSourcesBecomeStaleThenDisconnected() {
        val state = DashboardState(
            detectedNmeaSources = listOf(
                NmeaSourceProfile("src-a", pgns = listOf(129025), lastSeenMs = 1_000L),
            ),
        )

        val stale = NmeaStatusSummarizer.summarize(state, nowEpochMs = 7_000L)
        val disconnected = NmeaStatusSummarizer.summarize(state, nowEpochMs = 40_000L)

        assertEquals(NmeaConnectionStatus.STALE, stale.status)
        assertEquals("Daten stocken", stale.label)
        assertEquals(NmeaConnectionStatus.DISCONNECTED, disconnected.status)
    }
}
