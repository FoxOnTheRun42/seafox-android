package com.seafox.nmea_dashboard.data

import java.util.Locale

data class AutopilotSafetyDecision(
    val state: AutopilotSafetyState,
    val canDispatch: Boolean,
    val message: String,
)

object AutopilotSafetyGate {

    fun evaluate(
        request: AutopilotDispatchRequest,
        nowMs: Long = System.currentTimeMillis(),
    ): AutopilotSafetyDecision {
        if (!request.safetyGateArmed) {
            return AutopilotSafetyDecision(
                state = AutopilotSafetyState.disabled,
                canDispatch = false,
                message = "Autopilot Safety Gate ist nicht armiert.",
            )
        }

        if (!isVerifiedHost(request.host)) {
            return AutopilotSafetyDecision(
                state = AutopilotSafetyState.failed,
                canDispatch = false,
                message = "Autopilot-Gateway muss ein expliziter Host sein; Broadcast/leer ist gesperrt.",
            )
        }

        if (request.port !in 1..65535) {
            return AutopilotSafetyDecision(
                state = AutopilotSafetyState.failed,
                canDispatch = false,
                message = "Autopilot-Gateway-Port ist ungueltig.",
            )
        }

        if (request.requiresConfirmation) {
            val confirmedAtMs = request.confirmedAtMs
                ?: return AutopilotSafetyDecision(
                    state = AutopilotSafetyState.commandPending,
                    canDispatch = false,
                    message = "Autopilot-Befehl wartet auf explizite Bestaetigung.",
                )
            val ageMs = nowMs - confirmedAtMs
            if (ageMs < 0L || ageMs > request.timeoutMs.coerceAtLeast(1_000L)) {
                return AutopilotSafetyDecision(
                    state = AutopilotSafetyState.failed,
                    canDispatch = false,
                    message = "Autopilot-Bestaetigung ist abgelaufen.",
                )
            }
        }

        return AutopilotSafetyDecision(
            state = AutopilotSafetyState.acknowledged,
            canDispatch = true,
            message = "Autopilot-Befehl vom Safety Gate akzeptiert.",
        )
    }

    fun isVerifiedHost(host: String): Boolean {
        val normalized = host.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return false
        if (normalized == "0.0.0.0") return false
        if (normalized == "255.255.255.255") return false
        if (normalized == "broadcast") return false
        if (normalized == "localhost" || normalized == "127.0.0.1") return false
        return true
    }
}
