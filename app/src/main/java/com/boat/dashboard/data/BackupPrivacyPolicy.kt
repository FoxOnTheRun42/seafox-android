package com.seafox.nmea_dashboard.data

import java.util.Locale

object BackupPrivacyPolicy {
    fun allowsPublicBackup(mode: BackupPrivacyMode): Boolean {
        return mode == BackupPrivacyMode.manualExport || mode == BackupPrivacyMode.cloudAllowed
    }

    fun treatsAsSensitive(key: String): Boolean {
        val normalized = key.trim().lowercase(Locale.ROOT)
        return normalized.contains("mmsi") ||
            normalized.contains("route") ||
            normalized.contains("mob") ||
            normalized.contains("routerhost") ||
            normalized.contains("router_host") ||
            normalized.contains("gatewayhost") ||
            normalized.contains("gateway_host")
    }
}
