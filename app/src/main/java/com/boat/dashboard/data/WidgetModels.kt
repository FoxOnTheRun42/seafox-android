package com.seafox.nmea_dashboard.data

import java.util.UUID

const val DEFAULT_NMEA_ROUTER_HOST = "192.168.1.100"
const val DEFAULT_NMEA_ROUTER_PORT = 2000
const val DEFAULT_SIMULATION_START_LATITUDE = 36.14f
const val DEFAULT_SIMULATION_START_LONGITUDE = -5.35f

enum class WidgetKind {
    BATTERY,
    WATER_TANK,
    BLACK_WATER_TANK,
    GREY_WATER_TANK,
    TEMPERATURE,
    WIND,
    COMPASS,
    SEA_CHART,
    SEA_CHART_PIXEL,
    KARTEN,
    GPS,
    LOG,
    AIS,
    SYSTEM_PERFORMANCE,
    AUTOPILOT,
    DALY_BMS,
    ENGINE_RPM,
    ECHOSOUNDER,
    ANCHOR_WATCH,
    NMEA_PGN,
    NMEA0183,
}

enum class UiFont {
    FUTURA,
    ORBITRON,
    PT_MONO,
    ELECTROLIZE,
    DOT_GOTHIC,
}

enum class WidgetFrameStyle {
    BORDER,
    BACKGROUND,
}

enum class DashboardLayoutOrientation {
    LANDSCAPE,
    PORTRAIT,
}

enum class NmeaRouterProtocol {
    UDP,
    TCP,
}

data class DashboardWidget(
    val id: String = UUID.randomUUID().toString(),
    val kind: WidgetKind,
    val title: String,
    val xPx: Float,
    val yPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val dataKeys: List<String> = emptyList()
)

data class DashboardPage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val widgets: List<DashboardWidget> = emptyList()
)

data class DashboardState(
    val pages: List<DashboardPage> = emptyList(),
    val selectedPage: Int = 0,
    val udpPort: Int = DEFAULT_NMEA_ROUTER_PORT,
    val nmeaRouterProtocol: NmeaRouterProtocol = NmeaRouterProtocol.TCP,
    val nmeaRouterHost: String = DEFAULT_NMEA_ROUTER_HOST,
    val simulationStartLatitude: Float = DEFAULT_SIMULATION_START_LATITUDE,
    val simulationStartLongitude: Float = DEFAULT_SIMULATION_START_LONGITUDE,
    val simulationEnabled: Boolean = false,
    val darkBackground: Boolean = true,
    val gridStepPercent: Float = 2.5f,
    val uiFont: UiFont = UiFont.ORBITRON,
    val fontScale: Float = 1.0f,
    val layoutOrientation: DashboardLayoutOrientation = DashboardLayoutOrientation.PORTRAIT,
    val widgetFrameStyle: WidgetFrameStyle = WidgetFrameStyle.BORDER,
    val widgetFrameStyleGrayOffset: Int = 0,
    val windWidgetSettings: Map<String, String> = emptyMap(),
    val batteryWidgetSettings: Map<String, String> = emptyMap(),
    val aisWidgetSettings: Map<String, String> = emptyMap(),
    val echosounderWidgetSettings: Map<String, String> = emptyMap(),
    val autopilotWidgetSettings: Map<String, String> = emptyMap(),
    val logWidgetSettings: Map<String, String> = emptyMap(),
    val seaChartWidgetSettings: Map<String, String> = emptyMap(),
    val anchorWatchWidgetSettings: Map<String, String> = emptyMap(),
    val temperatureWidgetSettings: Map<String, String> = emptyMap(),
    val alarmToneVolume: Float = 0.7f,
    val alarmRepeatIntervalSeconds: Int = 5,
    val alarmVoiceAnnouncementsEnabled: Boolean = true,
    val alarmVoiceProfileIndex: Int = 0,
    val detectedNmeaSources: List<NmeaSourceProfile> = emptyList(),
    val nmea0183SentenceCategoryOverrides: Map<String, String> = emptyMap(),
    val boatProfile: BoatProfile = BoatProfile(),
    val activeRoute: SerializedRoute? = null,
    val mobPosition: MobMarker? = null,
    val backupPrivacyMode: BackupPrivacyMode = BackupPrivacyMode.privateOnly,
    val bootAutostartEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val entitlementSnapshot: EntitlementSnapshot = EntitlementSnapshot(),
)

data class SerializedWaypoint(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val passRadiusNm: Float = 0.1f,
)

data class SerializedRoute(
    val id: String,
    val name: String,
    val waypoints: List<SerializedWaypoint>,
    val activeLegIndex: Int = 0,
)

data class MobMarker(
    val lat: Double,
    val lon: Double,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class NmeaSourceProfile(
    val sourceKey: String,
    val displayName: String = "",
    val pgns: List<Int> = emptyList(),
    val lastSeenMs: Long = System.currentTimeMillis(),
    val createdMs: Long = System.currentTimeMillis(),
)

data class NmeaPgnHistoryEntry(
    val sourceKey: String,
    val pgn: Int,
    val receivedAtMs: Long,
    val detectedPgnText: String? = null,
    val payloadLength: String? = null,
    val payloadHex: String? = null,
    val rawLine: String? = null,
)

data class Nmea0183HistoryEntry(
    val sourceKey: String,
    val sentence: String,
    val fullSentence: String? = null,
    val receivedAtMs: Long,
    val category: String? = null,
    val rawLine: String? = null,
    val fields: List<String> = emptyList(),
)

data class ParsedNmeaValue(
    val key: String,
    val value: Float
)

enum class BoatType {
    MOTORBOOT,
    SEEGELBOOT,
}

data class BoatProfile(
    val lengthMeters: Float = 0f,
    val widthMeters: Float = 0f,
    val name: String = "",
    val homePort: String = "",
    val mmsi: String = "",
    val draftMeters: Float = 0f,
    val type: BoatType = BoatType.MOTORBOOT,
)
