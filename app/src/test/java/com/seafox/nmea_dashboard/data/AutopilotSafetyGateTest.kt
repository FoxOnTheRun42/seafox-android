package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutopilotSafetyGateTest {

    @Test
    fun blocksDispatchWhenSafetyGateIsNotArmed() {
        val decision = AutopilotSafetyGate.evaluate(
            request(
                safetyGateArmed = false,
                confirmedAtMs = 1_000L,
            ),
            nowMs = 1_500L,
        )

        assertEquals(AutopilotSafetyState.disabled, decision.state)
        assertFalse(decision.canDispatch)
    }

    @Test
    fun requiresExplicitConfirmationBeforeDispatch() {
        val decision = AutopilotSafetyGate.evaluate(
            request(safetyGateArmed = true),
            nowMs = 1_500L,
        )

        assertEquals(AutopilotSafetyState.commandPending, decision.state)
        assertFalse(decision.canDispatch)
    }

    @Test
    fun rejectsBroadcastGatewayHosts() {
        val decision = AutopilotSafetyGate.evaluate(
            request(
                host = "255.255.255.255",
                safetyGateArmed = true,
                confirmedAtMs = 1_000L,
            ),
            nowMs = 1_500L,
        )

        assertEquals(AutopilotSafetyState.failed, decision.state)
        assertFalse(decision.canDispatch)
    }

    @Test
    fun acceptsConfirmedCommandForExplicitGatewayHost() {
        val decision = AutopilotSafetyGate.evaluate(
            request(
                safetyGateArmed = true,
                confirmedAtMs = 1_000L,
                timeoutMs = 5_000L,
            ),
            nowMs = 1_500L,
        )

        assertEquals(AutopilotSafetyState.acknowledged, decision.state)
        assertTrue(decision.canDispatch)
    }

    @Test
    fun rejectsExpiredConfirmation() {
        val decision = AutopilotSafetyGate.evaluate(
            request(
                safetyGateArmed = true,
                confirmedAtMs = 1_000L,
                timeoutMs = 1_000L,
            ),
            nowMs = 2_500L,
        )

        assertEquals(AutopilotSafetyState.failed, decision.state)
        assertFalse(decision.canDispatch)
    }

    private fun request(
        host: String = "192.168.4.1",
        safetyGateArmed: Boolean,
        confirmedAtMs: Long? = null,
        timeoutMs: Long = DEFAULT_AUTOPILOT_COMMAND_TIMEOUT_MS,
    ): AutopilotDispatchRequest {
        return AutopilotDispatchRequest(
            command = buildAutopilotProtocolCommand(
                targetDevice = AutopilotTargetDevice.GARMIN,
                mode = AutopilotControlMode.KURS,
                sourceHeadingDeg = 90f,
                targetHeadingDeg = 95f,
                tackingAngleDeg = null,
                windAngleRelativeDeg = null,
            ),
            backend = AutopilotGatewayBackend.SIGNALK_V2,
            host = host,
            port = 3000,
            signalKAutopilotId = "_default",
            latDeg = 54f,
            lonDeg = 10f,
            sogKn = 5f,
            cogDeg = 90f,
            timeoutMs = timeoutMs,
            requiresConfirmation = true,
            confirmedAtMs = confirmedAtMs,
            safetyGateArmed = safetyGateArmed,
        )
    }
}
