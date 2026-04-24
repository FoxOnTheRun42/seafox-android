package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPrivacyPolicyTest {

    @Test
    fun privateOnlyBlocksPublicBackupCopies() {
        assertFalse(BackupPrivacyPolicy.allowsPublicBackup(BackupPrivacyMode.privateOnly))
    }

    @Test
    fun manualExportAllowsPublicBackupOnlyAfterUserChoice() {
        assertTrue(BackupPrivacyPolicy.allowsPublicBackup(BackupPrivacyMode.manualExport))
    }

    @Test
    fun classifiesBoatIdentityAndRouteKeysAsSensitive() {
        assertTrue(BackupPrivacyPolicy.treatsAsSensitive("ais_mmsi"))
        assertTrue(BackupPrivacyPolicy.treatsAsSensitive("activeRoute"))
        assertTrue(BackupPrivacyPolicy.treatsAsSensitive("mob_latitude"))
        assertTrue(BackupPrivacyPolicy.treatsAsSensitive("router_host"))
        assertFalse(BackupPrivacyPolicy.treatsAsSensitive("fontScale"))
    }
}
