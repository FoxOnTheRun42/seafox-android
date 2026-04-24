package com.seafox.nmea_dashboard.data

enum class FeatureAvailability(val label: String) {
    hidden("Ausgeblendet"),
    available("Aktiv"),
    beta("Beta"),
    licensedRequired("Lizenz erforderlich"),
    notImplemented("Nicht verfuegbar"),
}

enum class BackupPrivacyMode(val label: String) {
    privateOnly("Nur intern"),
    manualExport("Manueller Export"),
    cloudAllowed("Cloud-Backup erlaubt"),
}

enum class AutopilotSafetyState(val label: String) {
    disabled("Deaktiviert"),
    armed("Armiert"),
    commandPending("Bestaetigung ausstehend"),
    acknowledged("Safety Gate akzeptiert"),
    failed("Blockiert"),
}
