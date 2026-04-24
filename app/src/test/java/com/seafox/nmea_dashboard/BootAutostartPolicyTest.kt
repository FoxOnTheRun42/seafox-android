package com.seafox.nmea_dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootAutostartPolicyTest {

    @Test
    fun disabledOrMissingStateNeverLaunchesFromBootPaths() {
        assertEquals(
            BootAutostartDecision.skipDisabled,
            BootAutostartPolicy.decide("android.intent.action.BOOT_COMPLETED", """{"bootAutostartEnabled":false}"""),
        )
        assertEquals(
            BootAutostartDecision.skipDisabled,
            BootAutostartPolicy.decide("android.intent.action.LOCKED_BOOT_COMPLETED", null),
        )
    }

    @Test
    fun disabledStateBlocksUnlockAndInternalDelayedLaunch() {
        assertEquals(
            BootAutostartDecision.skipDisabled,
            BootAutostartPolicy.decide("android.intent.action.USER_UNLOCKED", """{"bootAutostartEnabled":false}"""),
        )
        assertEquals(
            BootAutostartDecision.skipDisabled,
            BootAutostartPolicy.decide(BootAutostartPolicy.ACTION_AUTOSTART_INTERNAL, """{"bootAutostartEnabled":false}"""),
        )
    }

    @Test
    fun enabledBootSchedulesAndEnabledUnlockLaunches() {
        val state = """{"backupPrivacyMode":"privateOnly","bootAutostartEnabled":true}"""

        assertEquals(
            BootAutostartDecision.scheduleDelayedLaunch,
            BootAutostartPolicy.decide("android.intent.action.BOOT_COMPLETED", state),
        )
        assertEquals(
            BootAutostartDecision.launchNow,
            BootAutostartPolicy.decide("android.intent.action.USER_UNLOCKED", state),
        )
        assertEquals(
            BootAutostartDecision.launchNow,
            BootAutostartPolicy.decide(BootAutostartPolicy.ACTION_AUTOSTART_INTERNAL, state),
        )
    }

    @Test
    fun ignoresUnrelatedActionsAndMalformedState() {
        assertEquals(
            BootAutostartDecision.ignore,
            BootAutostartPolicy.decide("android.intent.action.MY_PACKAGE_REPLACED", """{"bootAutostartEnabled":true}"""),
        )
        assertFalse(BootAutostartPolicy.isEnabled("not-json"))
        assertFalse(BootAutostartPolicy.isEnabled("""{"bootAutostartEnabled":"true"}"""))
        assertTrue(BootAutostartPolicy.isEnabled("""{ "bootAutostartEnabled" : true }"""))
    }
}
