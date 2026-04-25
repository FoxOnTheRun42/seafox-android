package com.seafox.nmea_dashboard.data

enum class NmeaConnectionStatus {
    DEMO,
    LIVE,
    STALE,
    DISCONNECTED,
}

data class NmeaStatusSummary(
    val status: NmeaConnectionStatus,
    val label: String,
    val detail: String,
    val detectedSourceCount: Int,
    val detectedPgnCount: Int,
)

object NmeaStatusSummarizer {
    fun summarize(
        state: DashboardState,
        nowEpochMs: Long = System.currentTimeMillis(),
        staleAfterMs: Long = 5_000L,
        disconnectedAfterMs: Long = 30_000L,
    ): NmeaStatusSummary {
        if (state.simulationEnabled) {
            return NmeaStatusSummary(
                status = NmeaConnectionStatus.DEMO,
                label = "Demo aktiv",
                detail = "Simulator liefert Cockpit-Daten.",
                detectedSourceCount = state.detectedNmeaSources.size,
                detectedPgnCount = state.detectedNmeaSources.distinctPgnCount(),
            )
        }

        if (state.detectedNmeaSources.isEmpty()) {
            return NmeaStatusSummary(
                status = NmeaConnectionStatus.DISCONNECTED,
                label = "NMEA getrennt",
                detail = routerEndpointLabel(state),
                detectedSourceCount = 0,
                detectedPgnCount = 0,
            )
        }

        val newestSeenMs = state.detectedNmeaSources.maxOf { it.lastSeenMs }
        val ageMs = (nowEpochMs - newestSeenMs).coerceAtLeast(0L)
        val pgnCount = state.detectedNmeaSources.distinctPgnCount()
        val sourceCount = state.detectedNmeaSources.size
        val pgnLabel = if (pgnCount == 1) "1 PGN" else "$pgnCount PGNs"
        val sourceLabel = if (sourceCount == 1) "1 Quelle" else "$sourceCount Quellen"

        return when {
            ageMs <= staleAfterMs -> NmeaStatusSummary(
                status = NmeaConnectionStatus.LIVE,
                label = "${state.nmeaRouterProtocol.name} · $pgnLabel",
                detail = "$sourceLabel aktiv über ${routerEndpointLabel(state)}",
                detectedSourceCount = sourceCount,
                detectedPgnCount = pgnCount,
            )
            ageMs <= disconnectedAfterMs -> NmeaStatusSummary(
                status = NmeaConnectionStatus.STALE,
                label = "Daten stocken",
                detail = "Zuletzt vor ${ageMs / 1000L}s · $pgnLabel",
                detectedSourceCount = sourceCount,
                detectedPgnCount = pgnCount,
            )
            else -> NmeaStatusSummary(
                status = NmeaConnectionStatus.DISCONNECTED,
                label = "NMEA getrennt",
                detail = "Zuletzt vor ${ageMs / 1000L}s · $sourceLabel bekannt",
                detectedSourceCount = sourceCount,
                detectedPgnCount = pgnCount,
            )
        }
    }

    private fun List<NmeaSourceProfile>.distinctPgnCount(): Int {
        return flatMap { it.pgns }.distinct().size
    }

    private fun routerEndpointLabel(state: DashboardState): String {
        return when (state.nmeaRouterProtocol) {
            NmeaRouterProtocol.TCP -> "${state.nmeaRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }}:${state.udpPort}"
            NmeaRouterProtocol.UDP -> "UDP :${state.udpPort}"
        }
    }
}
