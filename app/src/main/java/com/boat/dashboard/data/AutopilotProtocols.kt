package com.seafox.nmea_dashboard.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToInt

const val DEFAULT_AUTOPILOT_COMMAND_TIMEOUT_MS = 5_000L

enum class AutopilotTargetDevice(val label: String) {
    GARMIN("Garmin"),
    RAYMARINE("Raymarine"),
    SIMRAD("Simrad"),
}

enum class AutopilotControlMode(val label: String) {
    WIND("WIND"),
    STBY("STBY"),
    KURS("KURS"),
}

enum class AutopilotGatewayBackend(val label: String, val defaultPort: Int) {
    SIGNALK_V2("SignalK v2", 3000),
    YACHT_DEVICES_0183("Yacht Devices 0183", 1456),
    ACTISENSE_0183("Actisense 0183", 60001),
    DIRECT_UDP_JSON("Direkt UDP JSON", 14550),
}

data class AutopilotWidgetSettings(
    val targetDevice: AutopilotTargetDevice = AutopilotTargetDevice.GARMIN,
    val gatewayBackend: AutopilotGatewayBackend = AutopilotGatewayBackend.SIGNALK_V2,
    val gatewayHost: String = "192.168.4.1",
    val gatewayPort: Int = 3000,
    val signalKAutopilotId: String = "_default",
    val rudderAverageSeconds: Int = 30,
    val safetyGateArmed: Boolean = false,
    val commandTimeoutMs: Long = DEFAULT_AUTOPILOT_COMMAND_TIMEOUT_MS,
    val authToken: String = "",
)

data class AutopilotProtocolFrame(
    val pgn: Int,
    val name: String,
    val fields: Map<String, Any>,
)

data class AutopilotProtocolCommand(
    val targetDevice: AutopilotTargetDevice,
    val mode: AutopilotControlMode,
    val sourceHeadingDeg: Int,
    val targetHeadingDeg: Int?,
    val tackingAngleDeg: Int?,
    val windAngleRelativeDeg: Int?,
    val frames: List<AutopilotProtocolFrame>,
)

data class AutopilotDispatchRequest(
    val commandId: String = UUID.randomUUID().toString(),
    val command: AutopilotProtocolCommand,
    val backend: AutopilotGatewayBackend,
    val host: String,
    val port: Int,
    val signalKAutopilotId: String,
    val latDeg: Float?,
    val lonDeg: Float?,
    val sogKn: Float?,
    val cogDeg: Float?,
    val timeoutMs: Long = DEFAULT_AUTOPILOT_COMMAND_TIMEOUT_MS,
    val requiresConfirmation: Boolean = true,
    val confirmedAtMs: Long? = null,
    val safetyGateArmed: Boolean = false,
    val authToken: String? = null,
)

fun buildAutopilotProtocolCommand(
    targetDevice: AutopilotTargetDevice,
    mode: AutopilotControlMode,
    sourceHeadingDeg: Float,
    targetHeadingDeg: Float?,
    tackingAngleDeg: Int?,
    windAngleRelativeDeg: Float?,
): AutopilotProtocolCommand {
    val baseFields = linkedMapOf<String, Any>(
        "mode" to mode.label,
        "source_heading_deg" to sourceHeadingDeg.roundToInt(),
    )
    targetHeadingDeg?.let { baseFields["target_heading_deg"] = it.roundToInt() }
    tackingAngleDeg?.let { baseFields["tacking_angle_deg"] = it }
    windAngleRelativeDeg?.let { baseFields["wind_angle_relative_deg"] = it.roundToInt() }

    val standardFrame = AutopilotProtocolFrame(
        pgn = 127237,
        name = "Heading/Track Control",
        fields = baseFields,
    )

    val frames = when (targetDevice) {
        AutopilotTargetDevice.GARMIN -> listOf(standardFrame)
        AutopilotTargetDevice.RAYMARINE -> listOf(standardFrame)
        AutopilotTargetDevice.SIMRAD -> listOf(
            standardFrame,
            AutopilotProtocolFrame(
                pgn = 65341,
                name = "Autopilot Mode (SimNet/Navico)",
                fields = mapOf("mode" to mode.label),
            ),
            AutopilotProtocolFrame(
                pgn = 65480,
                name = "Autopilot Mode (SimNet/Navico)",
                fields = mapOf("mode" to mode.label),
            ),
        )
    }

    return AutopilotProtocolCommand(
        targetDevice = targetDevice,
        mode = mode,
        sourceHeadingDeg = sourceHeadingDeg.roundToInt(),
        targetHeadingDeg = targetHeadingDeg?.roundToInt(),
        tackingAngleDeg = tackingAngleDeg,
        windAngleRelativeDeg = windAngleRelativeDeg?.roundToInt(),
        frames = frames,
    )
}

fun autopilotProtocolHint(targetDevice: AutopilotTargetDevice): String {
    return when (targetDevice) {
        AutopilotTargetDevice.GARMIN ->
            "NMEA2000: PGN 127237 (Heading/Track Control)"
        AutopilotTargetDevice.RAYMARINE ->
            "SeaTalkNG/NMEA2000: PGN 127237 (Heading/Track Control)"
        AutopilotTargetDevice.SIMRAD ->
            "NMEA2000: PGN 127237, optional SimNet PGN 65341/65480"
    }
}

fun autopilotGatewayHint(backend: AutopilotGatewayBackend): String {
    return when (backend) {
        AutopilotGatewayBackend.SIGNALK_V2 ->
            "SignalK v2 REST: /signalk/v2/api/vessels/self/autopilots/{id}"
        AutopilotGatewayBackend.YACHT_DEVICES_0183 ->
            "NMEA0183 (APB/RMB/RMC/HSC) per UDP an Yacht Devices Gateway"
        AutopilotGatewayBackend.ACTISENSE_0183 ->
            "NMEA0183 (APB/RMB/RMC/HSC) per UDP an Actisense Gateway"
        AutopilotGatewayBackend.DIRECT_UDP_JSON ->
            "Direktes UDP-JSON (nur mit passendem Übersetzer nutzbar)"
    }
}

fun AutopilotProtocolCommand.toUdpJson(): String {
    val root = JSONObject()
    root.put("type", "autopilot_command")
    root.put("target_device", targetDevice.label)
    root.put("mode", mode.label)
    root.put("source_heading_deg", sourceHeadingDeg)
    if (targetHeadingDeg != null) root.put("target_heading_deg", targetHeadingDeg)
    if (tackingAngleDeg != null) root.put("tacking_angle_deg", tackingAngleDeg)
    if (windAngleRelativeDeg != null) root.put("wind_angle_relative_deg", windAngleRelativeDeg)

    val framesArray = JSONArray()
    frames.forEach { frame ->
        val frameObject = JSONObject()
        frameObject.put("pgn", frame.pgn)
        frameObject.put("name", frame.name)
        val fieldsObject = JSONObject()
        frame.fields.forEach { (key, value) ->
            fieldsObject.put(key, value)
        }
        frameObject.put("fields", fieldsObject)
        framesArray.put(frameObject)
    }
    root.put("frames", framesArray)
    return root.toString()
}

fun buildNmea0183AutopilotBundle(
    command: AutopilotProtocolCommand,
    latDeg: Float?,
    lonDeg: Float?,
    sogKn: Float?,
    cogDeg: Float?,
): String {
    if (command.mode == AutopilotControlMode.STBY) return ""

    val target = command.targetHeadingDeg?.toFloat() ?: command.sourceHeadingDeg.toFloat()
    val bearing = ((target % 360f) + 360f) % 360f
    val lat = latDeg ?: 0f
    val lon = lonDeg ?: 0f
    val sog = sogKn ?: 0f
    val cog = cogDeg ?: bearing

    val (latValue, latHemisphere) = formatNmeaLat(lat)
    val (lonValue, lonHemisphere) = formatNmeaLon(lon)

    val now = Date()
    val time = SimpleDateFormat("HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(now)
    val date = SimpleDateFormat("ddMMyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(now)

    val sentences = mutableListOf<String>()

    if (command.mode == AutopilotControlMode.KURS) {
        sentences += nmeaSentence(
            "IIHSC",
            "${fmt1(bearing)}",
            "T",
            "",
            "M"
        )
        sentences += nmeaSentence(
            "GPAPB",
            "A",
            "A",
            "0.00",
            "R",
            "N",
            "V",
            "V",
            fmt1(bearing),
            "T",
            "001",
            fmt1(bearing),
            "T",
            fmt1(bearing),
            "T"
        )
        sentences += nmeaSentence(
            "GPRMB",
            "A",
            "0.00",
            "R",
            "000",
            "001",
            latValue,
            latHemisphere,
            lonValue,
            lonHemisphere,
            "0.10",
            fmt1(bearing),
            "0.0",
            "V",
            "A"
        )
    }

    if (command.mode == AutopilotControlMode.WIND && command.windAngleRelativeDeg != null) {
        val windAngle = kotlin.math.abs(command.windAngleRelativeDeg).coerceIn(0, 180)
        sentences += nmeaSentence(
            "IIMWV",
            windAngle.toString(),
            "R",
            "10.0",
            "N",
            "A"
        )
    }

    sentences += nmeaSentence(
        "GPRMC",
        time,
        "A",
        latValue,
        latHemisphere,
        lonValue,
        lonHemisphere,
        fmt1(sog),
        fmt1(cog),
        date,
        "",
        "",
        "A"
    )

    return if (sentences.isEmpty()) "" else sentences.joinToString(separator = "\r\n", postfix = "\r\n")
}

private fun nmeaSentence(talkerType: String, vararg fields: String): String {
    val body = buildString {
        append(talkerType)
        fields.forEach {
            append(',')
            append(it)
        }
    }
    val checksum = body.fold(0) { acc, c -> acc xor c.code }
    return "\$$body*${checksum.toString(16).uppercase().padStart(2, '0')}"
}

private fun formatNmeaLat(value: Float): Pair<String, String> {
    val hemisphere = if (value >= 0f) "N" else "S"
    val absValue = kotlin.math.abs(value.toDouble())
    val deg = absValue.toInt()
    val minutes = (absValue - deg) * 60.0
    return String.format(Locale.US, "%02d%06.3f", deg, minutes) to hemisphere
}

private fun formatNmeaLon(value: Float): Pair<String, String> {
    val hemisphere = if (value >= 0f) "E" else "W"
    val absValue = kotlin.math.abs(value.toDouble())
    val deg = absValue.toInt()
    val minutes = (absValue - deg) * 60.0
    return String.format(Locale.US, "%03d%06.3f", deg, minutes) to hemisphere
}

private fun fmt1(value: Float): String = String.format(Locale.US, "%.1f", value)
