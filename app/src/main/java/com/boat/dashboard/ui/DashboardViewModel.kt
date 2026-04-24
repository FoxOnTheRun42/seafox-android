package com.seafox.nmea_dashboard.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seafox.nmea_dashboard.data.DashboardPage
import com.seafox.nmea_dashboard.data.DashboardRepository
import com.seafox.nmea_dashboard.data.DashboardState
import com.seafox.nmea_dashboard.data.DashboardWidget
import com.seafox.nmea_dashboard.data.MobMarker
import com.seafox.nmea_dashboard.data.NmeaSourceProfile
import com.seafox.nmea_dashboard.data.SerializedRoute
import com.seafox.nmea_dashboard.data.AutopilotControlMode
import com.seafox.nmea_dashboard.data.AutopilotDispatchRequest
import com.seafox.nmea_dashboard.data.AutopilotGatewayBackend
import com.seafox.nmea_dashboard.data.AutopilotSafetyGate
import com.seafox.nmea_dashboard.data.BackupPrivacyMode
import com.seafox.nmea_dashboard.data.NmeaNetworkService
import com.seafox.nmea_dashboard.data.NmeaUpdate
import com.seafox.nmea_dashboard.data.WidgetFrameStyle
import com.seafox.nmea_dashboard.data.DashboardLayoutOrientation
import com.seafox.nmea_dashboard.data.UiFont
import com.seafox.nmea_dashboard.data.WidgetKind
import com.seafox.nmea_dashboard.data.widgetDefaultDataKeys
import com.seafox.nmea_dashboard.data.widgetDefaultMinGridUnits
import com.seafox.nmea_dashboard.data.widgetDefaultSizePx
import com.seafox.nmea_dashboard.data.widgetTitleForKind
import com.seafox.nmea_dashboard.data.buildNmea0183AutopilotBundle
import com.seafox.nmea_dashboard.data.DEFAULT_NMEA_ROUTER_HOST
import com.seafox.nmea_dashboard.data.DEFAULT_NMEA_ROUTER_PORT
import com.seafox.nmea_dashboard.data.NmeaRouterProtocol
import com.seafox.nmea_dashboard.data.toUdpJson
import com.seafox.nmea_dashboard.data.BoatProfile
import com.seafox.nmea_dashboard.data.NmeaPgnHistoryEntry
import com.seafox.nmea_dashboard.data.Nmea0183HistoryEntry
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.roundToInt
import com.seafox.nmea_dashboard.data.DalyBmsBleManager
import com.seafox.nmea_dashboard.ui.widgets.chart.GeoCalc
import com.seafox.nmea_dashboard.ui.widgets.chart.Route
import com.seafox.nmea_dashboard.ui.widgets.chart.RouteLeg
import com.seafox.nmea_dashboard.ui.widgets.chart.Waypoint

data class PendingNmea0183Classification(
    val sentence: String,
    val rawLine: String,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private data class SimulatedAisTarget(
        val index: Int,
        val mmsi: Float,
        val name: String,
        var dxNm: Float,
        var dyNm: Float,
        var speedKn: Float,
        var courseDeg: Float,
        var bearingDeg: Float = 0f,
    )

    private data class TabletLocationSample(
        val latitude: Float,
        val longitude: Float,
        val speedKn: Float,
        val courseDeg: Float,
        val altitudeM: Float = Float.NaN,
        val accuracyM: Float = Float.NaN,
        val satellites: Float = Float.NaN,
        val utcTime: String? = null,
    )

    private val repository = DashboardRepository(application.applicationContext)
    private val network = NmeaNetworkService()
    private val dalyBmsManager = DalyBmsBleManager(application.applicationContext)
    private var routerSimulationJob: Job? = null
    private var simulationPhase = 0f
    private var simulationDepthM = 1f
    private var simulationDepthDirection = 1f
    private var simulationDepthModeStepCounter = 0
    private var simulationWaterTankPercent = 30f
    private var simulationSocPercent = 70f
    private var simulationBatteryCurrentAmp = -1.5f
    private var simulationEngineRpm = 1200f
    private var simulationTrueWindSpeed = 9f
    private var simulationTrueWindDirection = 45f
    private var simulationHeadingDeg = 230f
    private var simulationAisTargets: List<SimulatedAisTarget> = emptyList()
    private val appContext = getApplication<Application>().applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private var tabletGpsLatitude = Float.NaN
    private var tabletGpsLongitude = Float.NaN
    private var tabletGpsSpeedKn = Float.NaN
    private var tabletGpsCourseDeg = Float.NaN
    private var tabletGpsAltitudeM = Float.NaN
    private var tabletGpsAccuracyM = Float.NaN
    private var tabletGpsSatellites = Float.NaN
    private var tabletGpsUtcTime: String? = null
    private var tabletLocationListener: LocationListener? = null
    private var isTabletGpsListening = false
    private val tabletGpsUtcFormatter = SimpleDateFormat("HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private companion object {
        private const val SIM_LOOP_DELAY_MS = 1000L
        private const val AIS_TRACK_MAX_ENTRIES = 500
        private const val AIS_TRACK_MAX_TTL_MS = 5L * 60_000L
        private const val AIS_FALLBACK_MMSI_BASE = 900_000_000L
        private const val SIM_FIXED_SOG_KN = 5f
        private const val SIM_TANK_SLEW_PERCENT_PER_STEP = 0.4f
        private const val SIM_SOC_SLEW_PERCENT_PER_STEP = 0.2f
        private const val SIM_RPM_SLEW_PER_STEP = 20f
        private const val SIM_BATTERY_CURRENT_SLEW_PER_STEP = 0.5f
        private const val SIM_WIND_SPEED_SLEW_PER_STEP = 0.6f
        private const val SIM_WIND_ANGLE_SLEW_PER_STEP = 2.5f
        private const val SIM_HEADING_SLEW_PER_STEP = 1.0f
        private const val MAX_NMEA_PGN_HISTORY = 200
        private const val MAX_NMEA_0183_HISTORY = 200
        private val AVAILABLE_NMEA0183_SENTENCE_CATEGORY_OPTIONS = listOf(
            "AIS", "GPS", "WIND", "DEPTH", "HEADING", "ENGINE", "OTHER", "IGNORE",
        )
    }

    private data class AisCacheEntry(
        val numeric: MutableMap<String, Float> = mutableMapOf(),
        val text: MutableMap<String, String> = mutableMapOf(),
        var lastSeenMs: Long = 0L,
    )

    private val positionTelemetryKeys = setOf(
        "navigation.position.latitude",
        "position.latitude",
        "latitude",
        "lat",
        "navigation.position.longitude",
        "position.longitude",
        "longitude",
        "lon",
        "lng",
    )

    private val aisTargetCache = linkedMapOf<String, AisCacheEntry>()

    private val aisNumericFieldKeys: Set<String> = setOf(
        "target_mmsi",
        "mmsi",
        "ship_mmsi",
        "ais_target_mmsi",
        "ais_mmsi",
        "distance_nm",
        "relative_distance_nm",
        "range_nm",
        "distance_m",
        "distance_meters",
        "distance_meter",
        "range_m",
        "range_meters",
        "range_meter",
        "distance_km",
        "distance_kilometers",
        "range_km",
        "range_kilometers",
        "range",
        "distance",
        "cpa_nm",
        "cpa",
        "closest_point_of_approach",
        "time_to_cpa",
        "time_to_cpa_min",
        "time_to_cpa_minutes",
        "cpa_time",
        "ttcpa",
        "ttcpa_min",
        "cog",
        "course",
        "course_over_ground",
        "speed",
        "sog",
        "speed_kn",
        "speed_knots",
        "target_speed",
        "target_speed_kn",
        "ais_speed",
        "ais_target_speed",
        "ais_sog",
        "heading",
        "relative_bearing_deg",
        "relative_bearing",
        "bearing_deg",
        "bearing",
        "target_bearing",
        "target_bearing_deg",
        "target_latitude",
        "ais_target_latitude",
        "target_lat",
        "ais_lat",
        "latitude",
        "ais_latitude",
        "position.latitude",
        "position_latitude",
        "position.lat",
        "target_longitude",
        "ais_target_longitude",
        "target_lon",
        "ais_lon",
        "longitude",
        "ais_longitude",
        "position.longitude",
        "position_longitude",
        "position.lon",
        "nav_status",
        "navigation_status",
        "target_nav_status",
        "ais_nav_status",
    )

    private val aisTextFieldKeys: Set<String> = setOf(
        "name",
        "ship_name",
        "vessel_name",
        "ais_name",
        "target_name",
        "mmsi",
        "ais_mmsi",
        "target_mmsi",
        "ship_mmsi",
        "ais_target_mmsi",
    )

    private val AIS_CACHE_MMSI_TEXT_KEYS = listOf(
        "ais_mmsi",
        "target_mmsi",
        "ship_mmsi",
        "ais_target_mmsi",
        "mmsi",
    )
    private val AIS_CACHE_MMSI_NUMERIC_KEYS = listOf(
        "ais_mmsi",
        "target_mmsi",
        "ship_mmsi",
        "ais_target_mmsi",
        "mmsi",
    )

    private val AIS_CACHE_INDEXED_FIELD_PREFIXES = listOf(
        "ais",
        "targets",
        "target",
        "field",
        "fields",
        "data_fields",
        "n2kfields",
        "n2k_fields",
        "ais_targets",
        "targets_",
    )
    private val AIS_CACHE_INDEXED_FIELD_PATTERNS = AIS_CACHE_INDEXED_FIELD_PREFIXES.flatMap { prefix ->
        listOf(
            Regex("^(?:${Regex.escape(prefix)}(?:[._]targets)?)(?:\\[(\\d+)\\]|[._](\\d+))[._](.+)$"),
            Regex("(?:^|[._])(?:${Regex.escape(prefix)}(?:[._]targets)?)(?:\\[(\\d+)\\]|[._](\\d+))[._](.+)$"),
        )
    }
    private val AIS_CACHE_INDEXED_FIELD_CATCH_ALL_PATTERNS = arrayOf(
        Regex("^ais_(\\d+)_(.+)$"),
        Regex("^ais\\[(\\d+)\\](.+)$"),
        Regex("^ais\\.(\\d+)\\.(.+)$"),
    )
    private val AIS_CACHE_INDEXED_REST_PATTERNS = arrayOf(
        Regex("^_(\\d+)[._](.+)$"),
        Regex("^\\[(\\d+)\\][._](.+)$"),
        Regex("^\\.(\\d+)\\.(.+)$"),
    )
    private val AIS_CACHE_INDEXED_DIRECT_PATTERNS = arrayOf(
        Regex("^(\\d+)[._](.+)$"),
        Regex("^\\[(\\d+)\\](.+)$"),
        Regex("^\\.(\\d+)\\.(.+)$"),
    )
    private val AIS_CACHE_CANONICALIZE_FIELD_PATTERN = Regex("[^a-z0-9_\\.]+")
    private val AIS_CACHE_CANONICALIZE_UNDERSCORE_PATTERN = Regex("_+")
    private val NMEA0183_SENTENCE_CLEANUP_PATTERN = Regex("[^A-Z0-9]")

    private val aisUnscopedNumericSignalKeys = aisNumericFieldKeys
        .filterNot { positionTelemetryKeys.contains(it) }
        .toSet()

    private val aisUnscopedTextSignalKeys = aisTextFieldKeys
        .filterNot { it == "mmsi" }
        .toSet()

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _telemetry = MutableStateFlow<Map<String, Float>>(emptyMap())
    val telemetry: StateFlow<Map<String, Float>> = _telemetry.asStateFlow()
    private val _telemetryText = MutableStateFlow<Map<String, String>>(emptyMap())
    val telemetryText: StateFlow<Map<String, String>> = _telemetryText.asStateFlow()
    private val _aisTelemetry = MutableStateFlow<Map<String, Float>>(emptyMap())
    val aisTelemetry: StateFlow<Map<String, Float>> = _aisTelemetry.asStateFlow()
    private val _aisTelemetryText = MutableStateFlow<Map<String, String>>(emptyMap())
    val aisTelemetryText: StateFlow<Map<String, String>> = _aisTelemetryText.asStateFlow()
    private var lastAisTelemetryNumeric = emptyMap<String, Float>()
    private var lastAisTelemetryText = emptyMap<String, String>()
    private val _recentNmeaPgnHistory = MutableStateFlow<List<NmeaPgnHistoryEntry>>(emptyList())
    val recentNmeaPgnHistory: StateFlow<List<NmeaPgnHistoryEntry>> = _recentNmeaPgnHistory.asStateFlow()
    private val _recentNmea0183History = MutableStateFlow<List<Nmea0183HistoryEntry>>(emptyList())
    val recentNmea0183History: StateFlow<List<Nmea0183HistoryEntry>> = _recentNmea0183History.asStateFlow()
    private val _pendingNmeaSourceNotice = MutableStateFlow<String?>(null)
    val pendingNmeaSourceNotice: StateFlow<String?> = _pendingNmeaSourceNotice.asStateFlow()
    private val unknownNmeaSourceFallback = "unbekannte quelle"
    private val _pendingNmea0183Classification = MutableStateFlow<PendingNmea0183Classification?>(null)
    val pendingNmea0183Classification: StateFlow<PendingNmea0183Classification?> = _pendingNmea0183Classification.asStateFlow()
    private val nmea0183ClassificationCooldown = ConcurrentHashMap<String, Long>()
    val dalyDebugEvents: StateFlow<List<String>> = dalyBmsManager.debugEvents
    private val nmea0183PendingCooldownMs = 90_000L
    val availableNmea0183SentenceCategoryOptions: List<String>
        get() = AVAILABLE_NMEA0183_SENTENCE_CATEGORY_OPTIONS
    private val widgetMinGridUnits = mutableMapOf<String, Pair<Int, Int>>()
    private var hasLoggedGpsSourceConflict = false
    private var layoutPersistJob: Job? = null

    init {
        network.setRouterHost(_state.value.nmeaRouterHost)
        network.setRouterProtocol(_state.value.nmeaRouterProtocol)
        network.start(_state.value.udpPort)
        if (_state.value.simulationEnabled) {
            startRouterSimulation()
        }
        startTabletGpsLocationUpdates()
        dalyBmsManager.stop()

        viewModelScope.launch(Dispatchers.Default) {
            dalyBmsManager.telemetry.collectLatest { latestTelemetry ->
                withContext(Dispatchers.Main) {
                    _telemetry.update { current ->
                        current.toMutableMap().also { merged ->
                            merged.putAll(latestTelemetry)
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            dalyBmsManager.telemetryText.collectLatest { latestTelemetryText ->
                withContext(Dispatchers.Main) {
                    _telemetryText.update { current ->
                        current.toMutableMap().also { merged ->
                            merged.putAll(latestTelemetryText)
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            dalyBmsManager.connectionStatus.collectLatest { status ->
                withContext(Dispatchers.Main) {
                    val statusText = status.ifBlank { "Nicht verbunden" }
                    val connectionCode = if (statusText.contains("verbunden", ignoreCase = true)) 1f else 0f
                    _telemetry.update { current -> current.toMutableMap().also { merged ->
                        merged["daly_connection_status_code"] = connectionCode
                    } }
                    _telemetryText.update { current -> current.toMutableMap().also { merged ->
                        merged["daly_connection_status"] = statusText
                    } }
                    if (statusText.contains("fehler", ignoreCase = true) || statusText.contains("nicht", ignoreCase = true)) {
                        _telemetryText.update { current -> current.toMutableMap().also { merged ->
                            merged["daly_battery_state"] = statusText
                        } }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            network.updates.collect { update ->
                processNmeaNetworkUpdate(update, isAisSourceUpdate = false)
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            network.aisUpdates.collect { update ->
                processNmeaNetworkUpdate(update, isAisSourceUpdate = true)
            }
        }
    }

    private suspend fun processNmeaNetworkUpdate(update: NmeaUpdate, isAisSourceUpdate: Boolean) {
        val sanitizedUpdate = runCatching { sanitizePositionDataSources(update) }
            .getOrElse { update }
        val classifiedUpdate = classifyNmea0183SentenceUsingUserProfile(sanitizedUpdate)
        val nowMs = System.currentTimeMillis()
        val shouldRefreshAisCache = isAisSourceUpdate || isAisContextUpdate(classifiedUpdate)
        val (cachedAisNumeric, cachedAisText) = if (shouldRefreshAisCache) {
            val cached = buildNamespacedAisCache(classifiedUpdate, nowMs)
            lastAisTelemetryNumeric = cached.first
            lastAisTelemetryText = cached.second
            cached
        } else {
            lastAisTelemetryNumeric to lastAisTelemetryText
        }
        val currentPgn = extractNmeaPgn(sanitizedUpdate)
        val currentSource = extractNmeaSourceKey(sanitizedUpdate)

        withContext(Dispatchers.Main) {
            requestNmea0183ClassificationPrompt(classifiedUpdate)
            if (currentPgn != null) {
                detectAndTrackNmeaSource(sanitizedUpdate, currentPgn, currentSource)
                addNmeaPgnHistoryEntry(
                    sourceKey = currentSource,
                    pgn = currentPgn,
                    receivedUpdate = sanitizedUpdate,
                )
            }
            if (isNmea0183Update(classifiedUpdate)) {
                addNmea0183HistoryEntry(
                    sourceKey = currentSource,
                    receivedUpdate = classifiedUpdate,
                )
            }
            _telemetry.update { current ->
                val merged = current.toMutableMap()
                var changed = false
                sanitizedUpdate.numericValues.forEach { (key, value) ->
                    if (merged[key] != value) {
                        merged[key] = value
                        changed = true
                    }
                }
                if (changed) merged else current
            }
            _telemetryText.update { current ->
                val mergedText = current.toMutableMap()
                var changedText = false
                sanitizedUpdate.textValues.forEach { (key, value) ->
                    if (mergedText[key] != value) {
                        mergedText[key] = value
                        changedText = true
                    }
                }
                if (changedText) mergedText else current
            }
            if (shouldRefreshAisCache) {
                _aisTelemetry.update { current ->
                    if (current == cachedAisNumeric) current else cachedAisNumeric
                }
                _aisTelemetryText.update { current ->
                    if (current == cachedAisText) current else cachedAisText
                }
                lastAisTelemetryNumeric = _aisTelemetry.value
                lastAisTelemetryText = _aisTelemetryText.value
            }
        }
    }

    private fun isAisContextUpdate(update: NmeaUpdate): Boolean {
        if (isAisUpdate(update)) return true
        if (hasUnscopedAisContext(update)) return true

        val textLooksAis = update.textValues.keys.any { key ->
            isLikelyAisInputKey(key)
        }
        if (textLooksAis) return true

        val numericLooksAis = update.numericValues.keys.any { key ->
            isLikelyAisInputKey(key)
        }
        return numericLooksAis
    }

    private fun isLikelyAisInputKey(rawKey: String): Boolean {
        val normalized = normalizeAisFieldKey(rawKey)

        if (normalized.isBlank()) return false
        if (aisNumericFieldKeys.contains(normalized) || aisTextFieldKeys.contains(normalized)) return true

        return normalized.startsWith("ais_") ||
            normalized.startsWith("ais.") ||
            normalized.startsWith("target_") ||
            normalized.startsWith("target.") ||
            normalized.startsWith("targets_") ||
            normalized.startsWith("targets.")
    }

    private fun sanitizePositionDataSources(update: NmeaUpdate): NmeaUpdate {
        if (!_state.value.simulationEnabled) return update

        val nmeaType = update.textValues["nmea_type"] ?: "UNKNOWN"
        if (nmeaType.equals("JSON", ignoreCase = true)) return update

        val hasPositionValues = update.numericValues.keys.any { positionTelemetryKeys.contains(it) }
        if (!hasPositionValues) return update

        val filteredNumeric = update.numericValues.filterKeys { !positionTelemetryKeys.contains(it) }
        if (!hasLoggedGpsSourceConflict && filteredNumeric.size != update.numericValues.size) {
            hasLoggedGpsSourceConflict = true
            Log.w(
                "DashboardViewModel",
                "Position-Quelle liegt im Konflikt: interne Router-Simulation ist aktiv, aber eingehende Daten vom Typ $nmeaType enthalten Positionswerte. " +
                    "Externe Latitude/Longitude werden jetzt ignoriert."
            )
        }

        return update.copy(numericValues = filteredNumeric)
    }

    private fun normalizeNmea0183Sentence(sentence: String?): String {
        return sentence
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.replace(NMEA0183_SENTENCE_CLEANUP_PATTERN, "")
            ?: ""
    }

    private fun classifyNmea0183SentenceUsingUserProfile(update: NmeaUpdate): NmeaUpdate {
        if ((update.textValues["nmea_type"]?.uppercase(Locale.ROOT) != "NMEA0183")) return update

        val sentence = normalizeNmea0183Sentence(update.textValues["nmea_sentence"])
        if (sentence.isBlank()) return update

        val override = _state.value.nmea0183SentenceCategoryOverrides[sentence] ?: return update

        val nextText = update.textValues.toMutableMap()
        nextText["nmea0183_category"] = override
        nextText["nmea0183_user_category"] = override
        return update.copy(textValues = nextText)
    }

    private fun requestNmea0183ClassificationPrompt(update: NmeaUpdate) {
        if (update.textValues["nmea_type"]?.uppercase(Locale.ROOT) != "NMEA0183") return
        val autoCategory = update.textValues["nmea0183_category"]?.trim()?.uppercase(Locale.ROOT) ?: return
        if (autoCategory != "UNRESOLVED") return

        val sentence = normalizeNmea0183Sentence(update.textValues["nmea_sentence"])
        if (sentence.isBlank()) return
        if (_state.value.nmea0183SentenceCategoryOverrides[sentence]?.isNotBlank() == true) return

        val nowMs = System.currentTimeMillis()
        val cooldownUntil = nmea0183ClassificationCooldown[sentence]
        if (cooldownUntil != null && nowMs < cooldownUntil) return
        if (_pendingNmea0183Classification.value != null) return

        val rawLine = update.textValues["nmea0183_raw_line"]?.trim()?.takeIf { it.isNotBlank() }
            ?: sentence

        val activePending = _pendingNmea0183Classification.value
        if (activePending?.sentence == sentence) return

        nmea0183ClassificationCooldown[sentence] = nowMs + nmea0183PendingCooldownMs
        _pendingNmea0183Classification.value = PendingNmea0183Classification(
            sentence = sentence,
            rawLine = rawLine,
        )
    }

    private fun isNmea0183Update(update: NmeaUpdate): Boolean {
        val declaredType = update.textValues["nmea_type"]?.uppercase(Locale.ROOT) ?: ""
        if (declaredType == "NMEA0183") return true
        if (declaredType == "NMEA2000" || declaredType == "N2K") return false

        val hasNmea0183Sentence = update.textValues["nmea_sentence"]?.isNotBlank() == true ||
            update.textValues["nmea0183_sentence"]?.isNotBlank() == true
        val hasNmea0183Raw = update.textValues["nmea0183_raw_line"]?.isNotBlank() == true
        val hasNmea0183Namespace = update.textValues.keys.any { it.startsWith("nmea0183_") }

        return hasNmea0183Sentence || hasNmea0183Raw || hasNmea0183Namespace
    }

    private fun isAisUpdate(update: NmeaUpdate): Boolean {
        val nmea0183Category = update.textValues["nmea0183_category"]?.uppercase(Locale.ROOT)?.trim()
        if (nmea0183Category == "AIS") return true

        val nmeaSentence = update.textValues["nmea_sentence"]?.uppercase(Locale.ROOT)?.trim()
        val nmea0183Sentence = update.textValues["nmea0183_sentence"]?.uppercase(Locale.ROOT)?.trim()
        if (
            nmeaSentence == "VDM" || nmeaSentence == "VDO" ||
            nmea0183Sentence == "VDM" || nmea0183Sentence == "VDO"
        ) {
            return true
        }

        val detectedPgn = update.textValues["n2k_detected_pgn"] ?: update.textValues["n2k_pgn"]
        val pgn = detectedPgn?.trim()?.toIntOrNull() ?: return false
        return pgn == 129038 || pgn == 129039
    }

    private fun buildNamespacedAisCache(
        update: NmeaUpdate,
        nowMs: Long,
    ): Pair<Map<String, Float>, Map<String, String>> {
        val updatedMmsiKeys = mutableSetOf<String>()
        val directMmsi = extractAisMmsi(update)
        val hasDirectAisContextForCache = directMmsi != null || isAisUpdate(update)

        fun mergeAisCacheEntry(
            mmsi: String,
            numericValues: Map<String, Float>,
            textValues: Map<String, String>,
            allowPositionCoordinates: Boolean,
        ) {
            val entry = aisTargetCache.getOrPut(mmsi) { AisCacheEntry() }

            numericValues.forEach { rawKey, value ->
                if (!value.isFinite()) return@forEach
                val key = normalizeAisFieldKey(rawKey)
                if (isAisNumericFieldForCache(key) || (allowPositionCoordinates && positionTelemetryKeys.contains(key))) {
                    entry.numeric[key] = value
                }
            }
            textValues.forEach { rawKey, rawValue ->
                val key = normalizeAisFieldKey(rawKey)
                if (!rawValue.isBlank() && isAisTextFieldForCache(key)) {
                    entry.text[key] = rawValue.trim()
                }
            }

            if (!entry.numeric.containsKey("mmsi") && !entry.text.containsKey("mmsi") && !entry.text.containsKey("ais_mmsi")) {
                parseAisMmsiFromNumeric(mmsi.toFloatOrNull())?.let { parsedMmsi ->
                    entry.numeric["mmsi"] = parsedMmsi.toFloatOrNull()?.toFloat() ?: 0f
                    entry.text["mmsi"] = parsedMmsi
                }
            }
            entry.text["last_seen_ms"] = nowMs.toString()
            entry.lastSeenMs = nowMs
            updatedMmsiKeys.add(mmsi)
        }

        val indexedNumericGroups = linkedMapOf<String, MutableMap<String, Float>>()
        val indexedTextGroups = linkedMapOf<String, MutableMap<String, String>>()

        update.numericValues.forEach { rawKey, rawValue ->
            val indexedField = parseAisIndexedFieldName(rawKey)
            if (indexedField != null) {
                val (index, suffix) = indexedField
                val normalizedSuffix = normalizeAisFieldKey(suffix)
                val normalizedLeafSuffix = normalizeAisFieldKey(suffix.substringAfterLast('.'))
                val bucket = indexedNumericGroups.getOrPut(index) { mutableMapOf() }
                if (normalizedSuffix.isNotBlank()) {
                    bucket[normalizedSuffix] = rawValue
                }
                if (normalizedLeafSuffix.isNotBlank() && normalizedLeafSuffix != normalizedSuffix) {
                    bucket[normalizedLeafSuffix] = rawValue
                }
            }
        }
        update.textValues.forEach { rawKey, rawValue ->
            val indexedField = parseAisIndexedFieldName(rawKey)
            if (indexedField != null) {
                val (index, suffix) = indexedField
                val normalizedSuffix = normalizeAisFieldKey(suffix)
                val normalizedLeafSuffix = normalizeAisFieldKey(suffix.substringAfterLast('.'))
                val bucket = indexedTextGroups.getOrPut(index) { mutableMapOf() }
                if (!rawValue.isBlank()) {
                    if (normalizedSuffix.isNotBlank()) {
                        bucket[normalizedSuffix] = rawValue.trim()
                    }
                    if (normalizedLeafSuffix.isNotBlank() && normalizedLeafSuffix != normalizedSuffix) {
                        bucket[normalizedLeafSuffix] = rawValue.trim()
                    }
                }
            }
        }

        indexedNumericGroups.forEach { (index, indexedNumeric) ->
            val indexedText = indexedTextGroups[index].orEmpty()
            val indexedMmsi = resolveIndexedAisMmsi(indexedNumeric, indexedText)
            if (indexedNumeric.isNotEmpty() || indexedText.isNotEmpty()) {
                val cacheMmsi = indexedMmsi ?: resolveIndexedAisFallbackMmsi(index)
                mergeAisCacheEntry(
                    mmsi = cacheMmsi,
                    numericValues = indexedNumeric,
                    textValues = indexedText,
                    allowPositionCoordinates = true,
                )
            }
        }

        indexedTextGroups.forEach { (index, indexedText) ->
            if (indexedNumericGroups.containsKey(index)) return@forEach
            val indexedMmsi = resolveIndexedAisMmsi(emptyMap(), indexedText)
            if (indexedText.isNotEmpty()) {
                val cacheMmsi = indexedMmsi ?: resolveIndexedAisFallbackMmsi(index)
                mergeAisCacheEntry(
                    mmsi = cacheMmsi,
                    numericValues = emptyMap(),
                    textValues = indexedText,
                    allowPositionCoordinates = true,
                )
            }
        }

        val fallbackDirectMmsi = extractAisMmsi(update)
        if (fallbackDirectMmsi != null && !updatedMmsiKeys.contains(fallbackDirectMmsi)) {
            val directNumeric = mutableMapOf<String, Float>()
            update.numericValues.forEach { key, value ->
                if (!value.isFinite()) return@forEach
                val normalized = normalizeAisFieldKey(key)
                if (isAisNumericFieldForCache(normalized) ||
                    (hasDirectAisContextForCache && positionTelemetryKeys.contains(normalized))
                ) {
                    directNumeric[normalized] = value
                }
            }

            val directText = mutableMapOf<String, String>()
            update.textValues.forEach { key, value ->
                if (value.isBlank()) return@forEach
                val normalized = normalizeAisFieldKey(key)
                if (isAisTextFieldForCache(normalized)) {
                    directText[normalized] = value.trim()
                }
            }
            mergeAisCacheEntry(
                mmsi = fallbackDirectMmsi,
                numericValues = directNumeric,
                textValues = directText,
                allowPositionCoordinates = hasDirectAisContextForCache,
            )
        }

        pruneAisTargetCache(nowMs)

        val flatNumeric = mutableMapOf<String, Float>()
        val flatText = mutableMapOf<String, String>()
        aisTargetCache.forEach { (cachedMmsi, entry) ->
            entry.numeric.forEach { (key, value) ->
                flatNumeric["ais_${cachedMmsi}_$key"] = value
            }
            flatText["ais_${cachedMmsi}_mmsi"] = cachedMmsi
            entry.text.forEach { (key, value) ->
                flatText["ais_${cachedMmsi}_$key"] = value
            }
            flatText["ais_${cachedMmsi}_last_seen_ms"] = entry.lastSeenMs.toString()
        }
        return flatNumeric to flatText
    }

    private fun resolveIndexedAisMmsi(
        indexedNumericValues: Map<String, Float>,
        indexedTextValues: Map<String, String>,
    ): String? {
        val textCandidates = AIS_CACHE_MMSI_TEXT_KEYS
        textCandidates.forEach { key ->
            val normalized = normalizeAisMmsiText(indexedTextValues[key])
            if (!normalized.isNullOrBlank()) return normalized
        }

        val numericCandidates = AIS_CACHE_MMSI_NUMERIC_KEYS
        numericCandidates.forEach { key ->
            val candidate = parseAisMmsiFromNumeric(indexedNumericValues[key])
            if (!candidate.isNullOrBlank()) return candidate
        }

        return null
    }

    private fun resolveIndexedAisFallbackMmsi(index: String): String {
        val indexNumber = index.filter { it.isDigit() }.toLongOrNull() ?: 0L
        return ((AIS_FALLBACK_MMSI_BASE + (indexNumber % 100_000_000L)) % 1_000_000_000L).toString()
    }

    private fun pruneAisTargetCache(nowMs: Long) {
        val cutoffMs = nowMs - AIS_TRACK_MAX_TTL_MS
        aisTargetCache.entries.removeIf { it.value.lastSeenMs <= cutoffMs }
        if (aisTargetCache.size > AIS_TRACK_MAX_ENTRIES) {
            val extra = aisTargetCache.entries.toList()
                .sortedBy { it.value.lastSeenMs }
                .take(aisTargetCache.size - AIS_TRACK_MAX_ENTRIES)
            extra.forEach { (mmsi, _) ->
                aisTargetCache.remove(mmsi)
            }
        }
    }

    private fun hasAisContext(update: NmeaUpdate): Boolean {
        val numericHasContext = update.numericValues.keys.any { key ->
            val normalized = normalizeAisFieldKey(key)
            normalized.startsWith("ais_") ||
                normalized.startsWith("target_") ||
                normalized.startsWith("targets_") ||
                hasIndexedAisContextKey(normalized, key)
        }
        val textHasContext = update.textValues.keys.any { key ->
            val normalized = normalizeAisFieldKey(key)
            normalized.startsWith("ais_") ||
                normalized.startsWith("target_") ||
                normalized.startsWith("targets_") ||
                hasIndexedAisContextKey(normalized, key)
        }
        return numericHasContext || textHasContext
    }

    private fun hasIndexedAisContextKey(normalized: String, raw: String): Boolean {
        if (!normalized.startsWith("ais") && !normalized.startsWith("target") && !normalized.startsWith("targets")) {
            return false
        }
        if (raw.indexOf('[') < 0 && raw.indexOf('.') < 0 && raw.indexOf('_') < 0) {
            return false
        }
        return parseAisIndexedFieldName(raw) != null
    }

    private fun extractAisMmsi(update: NmeaUpdate): String? {
        val hasAisContext = hasAisContext(update)
        val hasUnscopedAisClues = hasUnscopedAisContext(update)
        val textCandidates = AIS_CACHE_MMSI_TEXT_KEYS
        textCandidates.forEach { key ->
            val raw = update.textValues[key]
            val normalized = normalizeAisMmsiText(raw)
            if (!normalized.isNullOrBlank()) return normalized
        }
        val numericCandidates = AIS_CACHE_MMSI_NUMERIC_KEYS
        numericCandidates.forEach { key ->
            val normalized = parseAisMmsiFromNumeric(update.numericValues[key])
            if (!normalized.isNullOrBlank()) {
                return normalized
            }
        }
        return null
    }

    private fun hasUnscopedAisContext(update: NmeaUpdate): Boolean {
        val hasAisSignal = update.numericValues.keys.any { key ->
            aisUnscopedNumericSignalKeys.contains(normalizeAisFieldKey(key))
        }
        if (hasAisSignal) return true

        return update.textValues.keys.any { key ->
            aisUnscopedTextSignalKeys.contains(normalizeAisFieldKey(key))
        }
    }

    private fun isAisNumericFieldForCache(key: String): Boolean {
        val normalized = normalizeAisFieldKey(key)
        return if (normalized.startsWith("ais_") || normalized.startsWith("target_")) {
            true
        } else {
            aisNumericFieldKeys.contains(normalized) && !positionTelemetryKeys.contains(normalized)
        }
    }

    private fun isAisTextFieldForCache(key: String): Boolean {
        val normalized = normalizeAisFieldKey(key)
        return aisTextFieldKeys.contains(normalized) || normalized.startsWith("ais_") || normalized.startsWith("target_")
    }

    private fun parseAisMmsiFromNumeric(value: Float?): String? {
        val mmsi = value?.takeIf { it.isFinite() && it > 0f } ?: return null
        if (mmsi >= 1_000_000_000f) return null
        val rounded = kotlin.math.round(mmsi.toDouble()).toLong()
        return if (rounded in 1L..999_999_999L) rounded.toString() else null
    }

    private fun parseAisIndexedFieldName(rawKey: String): Pair<String, String>? {
        val normalized = rawKey.lowercase()

        AIS_CACHE_INDEXED_FIELD_CATCH_ALL_PATTERNS.forEach { pattern ->
            val match = pattern.find(normalized)
            if (match != null) {
                val index = match.groupValues[1]
                val field = match.groupValues[2].trimStart('_', '.', '[' , ']').trim()
                if (isLikelyAisCacheIndexedField(field)) {
                    return index to field
                }
            }
        }

        var rest = ""

        val directMatch = AIS_CACHE_INDEXED_FIELD_PREFIXES.firstNotNullOfOrNull { prefix ->
            AIS_CACHE_INDEXED_FIELD_PATTERNS.firstNotNullOfOrNull { pattern ->
                val match = pattern.find(normalized) ?: return@firstNotNullOfOrNull null
                val index = match.groupValues[1].ifEmpty { match.groupValues[2] }
                val field = match.groupValues[3].trim().trimStart('_', '.', '[' , ']')
                if (index.isNotEmpty() && isLikelyAisCacheIndexedField(field)) {
                    index to field
                } else {
                    null
                }
            }
        }
        if (directMatch != null) return directMatch

        val matchedPrefix = AIS_CACHE_INDEXED_FIELD_PREFIXES.firstOrNull {
            normalized.startsWith("${it}.") || normalized.startsWith("${it}[") || normalized.startsWith("${it}_") || normalized == it
        } ?: return null

        rest = when {
            normalized.startsWith("${matchedPrefix}.") -> normalized.removePrefix("${matchedPrefix}.")
            normalized.startsWith("${matchedPrefix}[") -> normalized.substring(matchedPrefix.length)
            normalized.startsWith("${matchedPrefix}_") -> normalized.substring(matchedPrefix.length)
            else -> normalized.removePrefix(matchedPrefix)
        }

        if (rest.startsWith(".targets")) {
            rest = rest.removePrefix(".targets")
        }
        if (rest.startsWith("_targets")) {
            rest = rest.removePrefix("_targets")
        }
        if (rest.startsWith("[targets]")) {
            rest = rest.removePrefix("[targets]")
        }
        for (pattern in AIS_CACHE_INDEXED_REST_PATTERNS) {
            val match = pattern.find(rest) ?: continue
            val index = match.groupValues[1]
            val field = match.groupValues[2]
                .trimStart('_', '.')
                .trim()
            if (field.isBlank() || !isLikelyAisCacheIndexedField(field)) continue
            return index to field
        }
        for (pattern in AIS_CACHE_INDEXED_DIRECT_PATTERNS) {
            val match = pattern.find(normalized) ?: continue
            val index = match.groupValues[1]
            val field = match.groupValues[2]
                .trimStart('_', '.', '[' , ']')
                .trim()
            if (field.isBlank() || !isLikelyAisCacheIndexedField(field)) continue
            return index to field
        }
        return null
    }

    private fun isLikelyAisCacheIndexedField(field: String): Boolean {
        val normalized = normalizeAisFieldKey(field)
        if (normalized.isBlank()) return false
        val candidate = normalized.substringAfterLast('.')
        return aisTextFieldKeys.contains(candidate) ||
            aisNumericFieldKeys.contains(candidate) ||
            candidate.contains("distance") ||
            candidate.contains("bearing") ||
            candidate.contains("latitude") ||
            candidate.contains("longitude") ||
            candidate.contains("position") ||
            candidate.contains("mmsi")
    }

    private fun normalizeAisFieldKey(raw: String): String {
        return raw
            .lowercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')
            .replace(AIS_CACHE_CANONICALIZE_FIELD_PATTERN, "_")
            .replace(AIS_CACHE_CANONICALIZE_UNDERSCORE_PATTERN, "_")
            .trim('_')
    }

    private fun isAisCacheTelemetryKey(key: String): Boolean {
        return if (key.startsWith("ais_")) {
            val afterPrefix = key.removePrefix("ais_")
            val index = afterPrefix.substringBefore("_", missingDelimiterValue = "")
            index.isNotBlank() && index.all { it.isDigit() }
        } else if (key.startsWith("ais[")) {
            val afterPrefix = key.removePrefix("ais[")
            val index = afterPrefix.substringBefore("]", missingDelimiterValue = "")
            index.isNotBlank() && index.all { it.isDigit() }
        } else {
            false
        }
    }

    private fun normalizeAisMmsiText(raw: String?): String? {
        val digits = raw
            ?.trim()
            ?.filter { it.isDigit() }
            ?: return null
        return digits.ifBlank { null }
    }

    fun refreshTabletGpsLocationSource() {
        startTabletGpsLocationUpdates()
    }

    fun refreshDalyBleSource() {
        dalyBmsManager.stop()
    }

    private fun hasTabletLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun currentTabletPositionOrNull(): Pair<Float, Float>? {
        val latitude = tabletGpsLatitude
        val longitude = tabletGpsLongitude
        return if (latitude.isFinite() && longitude.isFinite()) {
            Pair(latitude, longitude)
        } else {
            null
        }
    }

    private fun currentTabletLocationSampleOrNull(): TabletLocationSample? {
        val latitude = tabletGpsLatitude
        val longitude = tabletGpsLongitude
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        return TabletLocationSample(
            latitude = latitude,
            longitude = longitude,
            speedKn = tabletGpsSpeedKn,
            courseDeg = tabletGpsCourseDeg,
            altitudeM = tabletGpsAltitudeM,
            accuracyM = tabletGpsAccuracyM,
            satellites = tabletGpsSatellites,
            utcTime = tabletGpsUtcTime,
        )
    }

    private fun extractTabletGpsSatelliteCount(location: Location): Float? {
        val extras = location.extras ?: return null
        val candidateKeys = listOf(
            "satellites",
            "satellites_in_view",
            "satellite_count",
            "satellites_in_use",
            "satellitesInView",
            "satellitesUsed",
        )
        for (key in candidateKeys) {
            if (!extras.containsKey(key)) continue
            val raw = extras.get(key)
            val satellites = when (raw) {
                is Int -> raw
                is Float -> raw.toInt()
                is Double -> raw.toInt()
                is Long -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            }
            if (satellites != null && satellites >= 0) return satellites.toFloat()
        }
        return null
    }

    private fun formatTabletGpsUtcTime(timestampMs: Long): String? {
        if (timestampMs <= 0L) return null
        return try {
            tabletGpsUtcFormatter.format(Date(timestampMs))
        } catch (_: Exception) {
            null
        }
    }

    private fun applyTabletTelemetryFallback(satellites: Float?, utcTime: String?) {
        if (satellites?.isFinite() == true) {
            _telemetry.update { current ->
                current.toMutableMap().also { it["gps_satellites"] = satellites }
            }
        }
        if (utcTime != null) {
            _telemetryText.update { current ->
                current.toMutableMap().apply {
                    this["gps_utc_time"] = utcTime
                    this["nmea0183_time"] = utcTime
                    this["gps_time"] = utcTime
                }
            }
        }
    }

    private fun startTabletGpsLocationUpdates() {
        if (!hasTabletLocationPermission() || isTabletGpsListening) return
        val manager = locationManager ?: return

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.allProviders.contains(LocationManager.PASSIVE_PROVIDER) -> LocationManager.PASSIVE_PROVIDER
            else -> return
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                applyTabletLocationSample(location)
            }
        }
        try {
            manager.requestLocationUpdates(
                provider,
                1000L,
                1f,
                listener,
                Looper.getMainLooper(),
            )
            tabletLocationListener = listener
            isTabletGpsListening = true
            val lastLocation = manager.getLastKnownLocation(provider)
            if (lastLocation != null) {
                applyTabletLocationSample(lastLocation)
            }
        } catch (_: SecurityException) {
            isTabletGpsListening = false
            tabletLocationListener = null
        }
    }

    private fun applyTabletLocationSample(location: Location) {
        tabletGpsLatitude = location.latitude.toFloat()
        tabletGpsLongitude = location.longitude.toFloat()
        tabletGpsSpeedKn = if (location.hasSpeed()) {
            (location.speed * 1.943844f).coerceAtLeast(0f)
        } else {
            Float.NaN
        }
        tabletGpsCourseDeg = if (location.hasBearing()) {
            normalizeAngle360(location.bearing)
        } else {
            Float.NaN
        }
        tabletGpsAltitudeM = if (location.hasAltitude()) {
            location.altitude.toFloat()
        } else {
            Float.NaN
        }
        tabletGpsAccuracyM = if (location.hasAccuracy()) {
            location.accuracy
        } else {
            Float.NaN
        }
        tabletGpsSatellites = extractTabletGpsSatelliteCount(location) ?: Float.NaN
        tabletGpsUtcTime = formatTabletGpsUtcTime(location.time)

        val latitude = tabletGpsLatitude
        val longitude = tabletGpsLongitude
        _telemetry.update { current ->
            val merged = current.toMutableMap()
            if (latitude.isFinite()) {
                merged["navigation.position.latitude"] = latitude
                merged["navigation.position.lat"] = latitude
                merged["position.latitude"] = latitude
                merged["latitude"] = latitude
                merged["lat"] = latitude
            }
            if (longitude.isFinite()) {
                merged["navigation.position.longitude"] = longitude
                merged["position.longitude"] = longitude
                merged["longitude"] = longitude
                merged["lon"] = longitude
                merged["lng"] = longitude
            }
            if (tabletGpsSpeedKn.isFinite()) {
                merged["navigation.speed_over_ground"] = tabletGpsSpeedKn
                merged["speed_over_ground"] = tabletGpsSpeedKn
                merged["sog"] = tabletGpsSpeedKn
                merged["gps_speed_kn"] = tabletGpsSpeedKn
            }
            if (tabletGpsCourseDeg.isFinite()) {
                merged["navigation.course_over_ground"] = tabletGpsCourseDeg
                merged["navigation.course_over_ground_true"] = tabletGpsCourseDeg
                merged["course_over_ground"] = tabletGpsCourseDeg
                merged["course_over_ground_true"] = tabletGpsCourseDeg
                merged["cog"] = tabletGpsCourseDeg
                merged["course"] = tabletGpsCourseDeg
                merged["heading"] = tabletGpsCourseDeg
                merged["compass_heading"] = tabletGpsCourseDeg
                merged["autopilot_heading"] = tabletGpsCourseDeg
            }
            merged
        }

        applyTabletTelemetryFallback(
            satellites = tabletGpsSatellites.takeIf { it.isFinite() },
            utcTime = tabletGpsUtcTime,
        )
    }

    private fun stopTabletGpsLocationUpdates() {
        if (!isTabletGpsListening) return
        val listener = tabletLocationListener ?: return
        val manager = locationManager ?: return
        try {
            manager.removeUpdates(listener)
        } catch (_: SecurityException) {
            // no-op
        } finally {
            tabletLocationListener = null
            isTabletGpsListening = false
        }
    }

    private fun resolveOwnPositionFromTabletOrNull(): Pair<Float, Float>? =
        currentTabletPositionOrNull()

    private fun currentOwnPositionOrNull(): Pair<Float, Float>? {
        resolveOwnPositionFromTabletOrNull()?.let { return it }
        val current = _state.value
        return if (current.simulationEnabled) {
            Pair(current.simulationStartLatitude, current.simulationStartLongitude)
        } else {
            null
        }
    }

    fun setActiveRoute(route: SerializedRoute?) {
        _state.update { it.copy(activeRoute = route) }
        persist()
    }

    fun clearActiveRoute() = setActiveRoute(null)

    fun triggerMob() {
        val ownPosition = currentOwnPositionOrNull() ?: return
        _state.update {
            it.copy(
                mobPosition = MobMarker(
                    lat = ownPosition.first.toDouble(),
                    lon = ownPosition.second.toDouble(),
                )
            )
        }
        persist()
    }

    fun clearMob() {
        _state.update { it.copy(mobPosition = null) }
        persist()
    }

    fun activeRouteAsNavRoute(): Route? {
        val serialized = _state.value.activeRoute ?: return null
        val waypoints = serialized.waypoints.map {
            Waypoint(
                id = it.id,
                name = it.name,
                lat = it.lat,
                lon = it.lon,
                passRadius = it.passRadiusNm,
            )
        }
        if (waypoints.size < 2) return null
        return Route(
            id = serialized.id,
            name = serialized.name,
            waypoints = waypoints,
            legs = waypoints.zipWithNext { from, to ->
                RouteLeg(
                    from = from,
                    to = to,
                    bearingDeg = GeoCalc.bearingDeg(from.lat, from.lon, to.lat, to.lon).toFloat(),
                    distanceNm = GeoCalc.distanceNm(from.lat, from.lon, to.lat, to.lon).toFloat(),
                )
            },
            activeLegIndex = serialized.activeLegIndex,
        )
    }

    fun toggleBackgroundColor() {
        _state.update { current -> current.copy(darkBackground = !current.darkBackground) }
        persist()
    }

    fun updateUiFont(font: UiFont) {
        _state.update { current -> current.copy(uiFont = font) }
        persist()
    }

    fun updateFontScale(scale: Float) {
        _state.update { current -> current.copy(fontScale = scale.coerceIn(0.7f, 1.6f)) }
        persist()
    }

    fun updateGridStepPercent(percent: Float) {
        _state.update { current -> current.copy(gridStepPercent = percent.coerceIn(0.5f, 20f)) }
        persist()
    }

    fun updateLayoutOrientation(orientation: DashboardLayoutOrientation) {
        _state.update { current -> current.copy(layoutOrientation = orientation) }
        persist()
    }

    fun updateAlarmToneVolume(volume: Float) {
        _state.update { current ->
            current.copy(alarmToneVolume = volume.coerceIn(0f, 2f))
        }
        persist()
    }

    fun updateAlarmRepeatIntervalSeconds(seconds: Int) {
        _state.update { current ->
            current.copy(alarmRepeatIntervalSeconds = seconds.coerceIn(2, 10))
        }
        persist()
    }

    fun updateAlarmVoiceAnnouncementsEnabled(enabled: Boolean) {
        _state.update { current ->
            current.copy(alarmVoiceAnnouncementsEnabled = enabled)
        }
        persist()
    }

    fun updateAlarmVoiceProfileIndex(index: Int) {
        _state.update { current ->
            current.copy(alarmVoiceProfileIndex = index.coerceIn(0, 3))
        }
        persist()
    }

    fun updateBackupPrivacyMode(mode: BackupPrivacyMode) {
        _state.update { current -> current.copy(backupPrivacyMode = mode) }
        persist()
    }

    fun updateBootAutostartEnabled(enabled: Boolean) {
        _state.update { current -> current.copy(bootAutostartEnabled = enabled) }
        persist()
    }

    fun completeOnboarding() {
        _state.update { current -> current.copy(onboardingCompleted = true) }
        persist()
    }

    fun updateBoatProfile(profile: BoatProfile) {
        _state.update { current ->
            current.copy(boatProfile = profile)
        }
        persist()
    }

    fun updateWidgetFrameStyleGrayOffset(offset: Int) {
        _state.update { current -> current.copy(widgetFrameStyleGrayOffset = offset.coerceIn(-10, 10)) }
        persist()
    }

    fun updateWidgetFrameStyle(style: WidgetFrameStyle) {
        _state.update { current -> current.copy(widgetFrameStyle = style) }
        persist()
    }

    fun updateNmeaRouter(host: String, port: Int, protocol: NmeaRouterProtocol) {
        val normalizedHost = host.trim()
        val normalizedPort = port.coerceIn(1, 65535)
        val previousHost = _state.value.nmeaRouterHost
        val previousPort = _state.value.udpPort
        val previousProtocol = _state.value.nmeaRouterProtocol
        val normalizedProtocol = protocol

        _state.update { current ->
            current.copy(
                nmeaRouterHost = normalizedHost,
                udpPort = normalizedPort,
                nmeaRouterProtocol = normalizedProtocol,
            )
        }
        network.setRouterHost(normalizedHost)
        network.setRouterProtocol(normalizedProtocol)
        if (previousPort != normalizedPort || previousHost != normalizedHost || previousProtocol != normalizedProtocol) {
            network.restart(normalizedPort)
        }
        persist()
    }

    fun updateRouterSimulationOrigin(latitude: Float, longitude: Float) {
        val normalizedLatitude = latitude.coerceIn(-90f, 90f)
        val normalizedLongitude = longitude.coerceIn(-180f, 180f)
        _state.update { current ->
            if (
                current.simulationStartLatitude == normalizedLatitude &&
                current.simulationStartLongitude == normalizedLongitude
            ) {
                current
            } else {
                current.copy(
                    simulationStartLatitude = normalizedLatitude,
                    simulationStartLongitude = normalizedLongitude
                )
            }
        }
        persist()
    }

    fun toggleRouterSimulation() {
        if (_state.value.simulationEnabled) {
            stopRouterSimulation()
        } else {
            startRouterSimulation()
        }
    }

    fun startRouterSimulation() {
        if (routerSimulationJob?.isActive == true) return

        if (!_state.value.simulationEnabled) {
            _state.update { current -> current.copy(simulationEnabled = true) }
            persist()
        }

        simulationPhase = 0f
        simulationDepthM = 1f
        simulationDepthDirection = 1f
        simulationDepthModeStepCounter = 0
        simulationWaterTankPercent = 30f
        simulationSocPercent = 70f
        simulationBatteryCurrentAmp = -1.5f
        simulationEngineRpm = 1200f
        simulationTrueWindSpeed = 9f
        simulationTrueWindDirection = 45f
        simulationHeadingDeg = 90f
        simulationAisTargets = createSimulatedAisTargets()
        routerSimulationJob?.cancel()
        routerSimulationJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val phase = simulationPhase
                simulationPhase += 0.2f
                val stateSnapshot = _state.value
        val tabletPosition = resolveOwnPositionFromTabletOrNull()
        val ownLatitude = tabletPosition?.first ?: stateSnapshot.simulationStartLatitude
        val ownLongitude = tabletPosition?.second ?: stateSnapshot.simulationStartLongitude
        val host = stateSnapshot.nmeaRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }
        val port = stateSnapshot.udpPort.coerceIn(1, 65535)
                val rawHeading = normalizeAngle360(90f + 1.6f * sin(phase / 10f))
                val rawTrueWindSpeed = 9f + 0.5f * sin(phase / 16f)
                val rawWindDirection = normalizeAngle360(phase * 2.2f + 45f)

                simulationTrueWindSpeed = slewValue(
                    simulationTrueWindSpeed,
                    rawTrueWindSpeed,
                    SIM_WIND_SPEED_SLEW_PER_STEP
                )
                simulationTrueWindDirection = slewValue(
                    simulationTrueWindDirection,
                    rawWindDirection,
                    SIM_WIND_ANGLE_SLEW_PER_STEP
                )

                val tabletSample = currentTabletLocationSampleOrNull()
                val desiredHeadingDeg = if (tabletSample?.courseDeg?.isFinite() == true) {
                    normalizeAngle360(tabletSample.courseDeg)
                } else {
                    rawHeading
                }
                val desiredSogKn = tabletSample?.speedKn?.takeIf { it.isFinite() } ?: SIM_FIXED_SOG_KN

                simulationHeadingDeg = slewValue(simulationHeadingDeg, desiredHeadingDeg, SIM_HEADING_SLEW_PER_STEP)
                val heading = normalizeAngle360(simulationHeadingDeg)
                val ownSogKn = desiredSogKn.coerceIn(0f, 40f)
                val effectiveHeadingDeg = heading

                val trueWindSpeed = simulationTrueWindSpeed.coerceIn(0f, 60f)
                val trueWindDirection = normalizeAngle360(simulationTrueWindDirection)
                val desiredWaterLevel = (30f + 5f * sin(phase / 20f)).coerceIn(0f, 100f)
                val desiredSoc = (70f + 15f * (sin(phase / 40f) + 1f) / 2f).coerceIn(0f, 100f)
                val desiredRpm = (1200f + 250f * sin(phase / 25f)).coerceIn(600f, 4000f)
                val desiredBatteryCurrent = (-1.5f + 2f * sin(phase / 6f)).coerceIn(-200f, 200f)
                val rudder = 6f * sin(phase / 12f)
                val isFastDepthMode = (simulationDepthModeStepCounter / 15) % 2 == 1
                val depthStepPerSecond = if (isFastDepthMode) 2f else 0.1f
                simulationDepthM += simulationDepthDirection * depthStepPerSecond
                if (simulationDepthM > 30f) {
                    simulationDepthM = 30f
                    simulationDepthDirection = -1f
                } else if (simulationDepthM < 1f) {
                    simulationDepthM = 1f
                    simulationDepthDirection = 1f
                }
                simulationDepthModeStepCounter = (simulationDepthModeStepCounter + 1) % 30
                simulationWaterTankPercent = slewValue(
                    simulationWaterTankPercent,
                    desiredWaterLevel,
                    SIM_TANK_SLEW_PERCENT_PER_STEP
                )
                simulationSocPercent = slewValue(
                    simulationSocPercent,
                    desiredSoc,
                    SIM_SOC_SLEW_PERCENT_PER_STEP
                )
                simulationBatteryCurrentAmp = slewValue(
                    simulationBatteryCurrentAmp,
                    desiredBatteryCurrent,
                    SIM_BATTERY_CURRENT_SLEW_PER_STEP
                )
                simulationEngineRpm = slewValue(
                    simulationEngineRpm,
                    desiredRpm,
                    SIM_RPM_SLEW_PER_STEP
                )
                val waterDepthM = simulationDepthM
                val anchorChainLengthM = (20f + 5f * sin(simulationPhase / 18f)).coerceIn(2f, 40f)

                val ownDxNmPerSec = (sin(Math.toRadians(effectiveHeadingDeg.toDouble()).toFloat()) * ownSogKn) / 3600f
                val ownDyNmPerSec = (cos(Math.toRadians(effectiveHeadingDeg.toDouble()).toFloat()) * ownSogKn) / 3600f
                val trueWindAngleRelative = normalizeSignedDegrees(trueWindDirection - heading)
                val payloadObj = JSONObject().apply {
                    put("heading", heading.toDouble())
                    put("autopilot_heading", heading.toDouble())
                    put("wind_speed_true", trueWindSpeed.toDouble())
                    put("true_wind_speed", trueWindSpeed.toDouble())
                    put("tws", trueWindSpeed.toDouble())
                    put("wind_speed", trueWindSpeed.toDouble())
                    put("wind_angle", normalizeAngle360(trueWindAngleRelative).toDouble())
                    put("wind_angle_true", normalizeAngle360(trueWindAngleRelative).toDouble())
                    put("wind_direction_true", normalizeAngle360(trueWindDirection).toDouble())
                    put("water_tank", simulationWaterTankPercent.coerceIn(0f, 100f).toDouble())
                    put("black_water_tank", (22f + 2f * sin(phase / 30f)).toDouble())
                    put("grey_water_tank", (18f + 2f * cos(phase / 30f)).toDouble())
                    put("battery_soc", simulationSocPercent.toDouble())
                    put("battery_voltage", (12.4f + simulationSocPercent / 200f).coerceIn(10f, 16f).toDouble())
                    put("battery_current", simulationBatteryCurrentAmp.toDouble())
                    put("battery_power_w", (80f + 30f * cos(phase / 8f)).coerceIn(-300f, 300f).toDouble())
                    put("engine_rpm", simulationEngineRpm.toDouble())
                    put("rudder", rudder.toDouble())
                    put("rudder_angle", rudder.toDouble())
                    put("rudder_position", rudder.toDouble())
                    put("autopilot_mode", (if (phase.toInt() % 3 == 0) 2f else 1f).toDouble())
                    put("autopilot_heading_target", ((heading + 120f) % 360f).toDouble())
                    put("navigation.speed_over_ground", ownSogKn.toDouble())
                    put("speed_over_ground", ownSogKn.toDouble())
                    put("sog", ownSogKn.toDouble())
                    put("water_depth_m", waterDepthM.toDouble())
                    put("water_depth", waterDepthM.toDouble())
                    put("depth_m", waterDepthM.toDouble())
                    put("navigation.course_over_ground_true", effectiveHeadingDeg.toDouble())
                    put("course_over_ground_true", effectiveHeadingDeg.toDouble())
                    put("course_over_ground", effectiveHeadingDeg.toDouble())
                    put("cog", effectiveHeadingDeg.toDouble())
                    put("anchor_chain_length", anchorChainLengthM.toDouble())
                    put("chain_length", anchorChainLengthM.toDouble())
                    put("ais_max_range_nm", 10.0)
                }

                tabletPosition?.let { (tabletLatitude, tabletLongitude) ->
                    payloadObj.put("navigation.position.latitude", tabletLatitude.toDouble())
                    payloadObj.put("position.latitude", tabletLatitude.toDouble())
                    payloadObj.put("latitude", tabletLatitude.toDouble())
                    payloadObj.put("lat", tabletLatitude.toDouble())
                    payloadObj.put("navigation.position.longitude", tabletLongitude.toDouble())
                    payloadObj.put("position.longitude", tabletLongitude.toDouble())
                    payloadObj.put("longitude", tabletLongitude.toDouble())
                    payloadObj.put("lon", tabletLongitude.toDouble())
                    payloadObj.put("lng", tabletLongitude.toDouble())
                    if (tabletSample?.accuracyM?.isFinite() == true) {
                        payloadObj.put("gps_hdop", tabletSample.accuracyM.toDouble())
                    }
                    if (tabletSample?.altitudeM?.isFinite() == true) {
                        payloadObj.put("gps_altitude_m", tabletSample.altitudeM.toDouble())
                    }
                    if (tabletSample?.speedKn?.isFinite() == true) {
                        payloadObj.put("gps_speed_kn", tabletSample.speedKn.toDouble())
                    }
                    if (tabletSample?.courseDeg?.isFinite() == true) {
                        payloadObj.put("gps_course_deg", tabletSample.courseDeg.toDouble())
                    }
                    if (tabletSample?.satellites?.isFinite() == true) {
                        payloadObj.put("gps_satellites", tabletSample.satellites.toDouble())
                    }
                    val tabletUtcTime = tabletSample?.utcTime
                    if (!tabletUtcTime.isNullOrBlank()) {
                        payloadObj.put("gps_utc_time", tabletUtcTime)
                        payloadObj.put("nmea0183_time", tabletUtcTime)
                        payloadObj.put("gps_time", tabletUtcTime)
                    }
                    payloadObj.put("gps_fix_quality", 1)
                }

                simulationAisTargets = simulationAisTargets.mapIndexed { index, target ->
                    val targetHeadingRad = Math.toRadians(target.courseDeg.toDouble()).toFloat()
                    val targetDxNmPerSec = sin(targetHeadingRad) * target.speedKn / 3600f
                    val targetDyNmPerSec = cos(targetHeadingRad) * target.speedKn / 3600f
                    val relDxNmPerSec = targetDxNmPerSec - ownDxNmPerSec
                    val relDyNmPerSec = targetDyNmPerSec - ownDyNmPerSec
                    val nextDxNm = target.dxNm + relDxNmPerSec
                    val nextDyNm = target.dyNm + relDyNmPerSec
                    val distanceNm = hypot(nextDxNm, nextDyNm).coerceAtLeast(0.1f)
                    val absoluteBearingDeg = normalizeAngle360(
                        Math.toDegrees(atan2(nextDxNm.toDouble(), nextDyNm.toDouble())).toFloat()
                    )
                    val relativeBearingDeg = normalizeSignedDegrees(absoluteBearingDeg - heading).coerceIn(-180f, 180f)

                    val relSpeedSq = relDxNmPerSec * relDxNmPerSec + relDyNmPerSec * relDyNmPerSec
                    val timeToClosestSec = if (relSpeedSq > 1e-6f) {
                        -((nextDxNm * relDxNmPerSec + nextDyNm * relDyNmPerSec) / relSpeedSq)
                    } else {
                        -1f
                    }
                    val cpaDistanceNm = if (timeToClosestSec > 0f) {
                        val cpaDxNm = nextDxNm + relDxNmPerSec * timeToClosestSec
                        val cpaDyNm = nextDyNm + relDyNmPerSec * timeToClosestSec
                        hypot(cpaDxNm, cpaDyNm)
                    } else {
                        distanceNm
                    }
                    val cpaTimeMinutes = if (timeToClosestSec > 0f) timeToClosestSec / 60f else 0f

                    val keyPrefix = "ais_${index}_"
                    val targetLatitude = ownLatitude + (nextDyNm / 60f)
                    val targetLongitude = ownLongitude + (nextDxNm / 60f)

                    payloadObj.put("${keyPrefix}mmsi", target.mmsi.toDouble())
                    payloadObj.put("${keyPrefix}name", target.name)
                    payloadObj.put("${keyPrefix}distance_nm", distanceNm.toDouble())
                    payloadObj.put("${keyPrefix}relative_distance_nm", distanceNm.toDouble())
                    payloadObj.put("${keyPrefix}bearing", relativeBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}relative_bearing", relativeBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}relative_bearing_deg", relativeBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}ais_relative_bearing_deg", relativeBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}bearing_deg", absoluteBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}bearing_true", absoluteBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}target_bearing", relativeBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}target_bearing_deg", relativeBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}ais_bearing", absoluteBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}ais_bearing_deg", absoluteBearingDeg.toDouble())
                    payloadObj.put("${keyPrefix}cpa_nm", cpaDistanceNm.toDouble())
                    payloadObj.put("${keyPrefix}cpa", cpaDistanceNm.toDouble())
                    payloadObj.put("${keyPrefix}time_to_cpa", cpaTimeMinutes.toDouble())
                    payloadObj.put("${keyPrefix}time_to_cpa_min", cpaTimeMinutes.toDouble())
                    payloadObj.put("${keyPrefix}time_to_cpa_minutes", cpaTimeMinutes.toDouble())
                    payloadObj.put("${keyPrefix}ttcpa", cpaTimeMinutes.toDouble())
                    payloadObj.put("${keyPrefix}cog", target.courseDeg.toDouble())
                    payloadObj.put("${keyPrefix}course", target.courseDeg.toDouble())
                    payloadObj.put("${keyPrefix}course_over_ground", target.courseDeg.toDouble())
                    payloadObj.put("${keyPrefix}heading", target.courseDeg.toDouble())
                    payloadObj.put("${keyPrefix}sog", target.speedKn.toDouble())
                    payloadObj.put("${keyPrefix}speed", target.speedKn.toDouble())
                    payloadObj.put("${keyPrefix}speed_kn", target.speedKn.toDouble())
                    payloadObj.put("${keyPrefix}target_course", target.courseDeg.toDouble())
                    payloadObj.put("${keyPrefix}target_speed", target.speedKn.toDouble())
                    payloadObj.put("${keyPrefix}target_speed_kn", target.speedKn.toDouble())
                    payloadObj.put("${keyPrefix}latitude", targetLatitude.toDouble())
                    payloadObj.put("${keyPrefix}target_latitude", targetLatitude.toDouble())
                    payloadObj.put("${keyPrefix}longitude", targetLongitude.toDouble())
                    payloadObj.put("${keyPrefix}target_longitude", targetLongitude.toDouble())
                    payloadObj.put("${keyPrefix}nav_status", if (target.speedKn < 2f) 1 else 0)
                    payloadObj.put("${keyPrefix}target_nav_status", if (target.speedKn < 2f) 1 else 0)
                    payloadObj.put("${keyPrefix}maneuver_restriction", if (index % 2 == 0) 0 else 3)

                    val nextCourse = normalizeAngle360(target.courseDeg + 0.3f * sin(phase / 25f + index))
                    target.courseDeg = nextCourse
                    val nextSpeed = (target.speedKn + 0.05f * cos(phase / 16f + index)).coerceIn(2f, 18f)
                    target.speedKn = nextSpeed
                    target.dxNm = nextDxNm
                    target.dyNm = nextDyNm
                    target
                }

                network.sendUdpText(
                    payload = payloadObj.toString(),
                    host = host,
                    port = port
                )
                delay(SIM_LOOP_DELAY_MS)
            }
        }
    }

    private fun createSimulatedAisTargets(): List<SimulatedAisTarget> {
        return (0 until 10).map { index ->
            val baseDistanceNm = 1.2f + (index * 0.9f)
            val initialBearingDeg = (index * 36f + 20f + (index % 2) * 12f) % 360f
            val initialRad = Math.toRadians(initialBearingDeg.toDouble()).toFloat()
            val speedKn = 4f + (index % 4) * 1.1f
            val courseDeg = (index * 18f + 60f) % 360f
            SimulatedAisTarget(
                index = index,
                mmsi = (210000000f + index),
                name = "AIS Vessel ${index + 1}",
                dxNm = baseDistanceNm * sin(initialRad),
                dyNm = baseDistanceNm * cos(initialRad),
                speedKn = speedKn,
                courseDeg = courseDeg,
                bearingDeg = initialBearingDeg,
            )
        }
    }

    private fun normalizeSignedDegrees(value: Float): Float {
        val wrapped = normalizeAngle360(value)
        return if (wrapped > 180f) wrapped - 360f else wrapped
    }

    private fun detectAndTrackNmeaSource(
        _update: NmeaUpdate,
        forcedPgn: Int,
        forcedSource: String?,
    ) {
        val sourceKey = normalizeNmeaSourceKey(
            forcedSource.orEmpty().ifBlank { unknownNmeaSourceFallback },
        )
        val pgn = forcedPgn

        val profileIndex = _state.value.detectedNmeaSources.indexOfFirst { it.sourceKey.lowercase() == sourceKey.lowercase() }
        val now = System.currentTimeMillis()

        _state.update { current ->
            val existing = current.detectedNmeaSources.toMutableList()
            val normalizedSource = normalizeNmeaSourceKey(sourceKey)

            if (profileIndex < 0) {
                existing.add(
                    NmeaSourceProfile(
                        sourceKey = normalizedSource,
                        displayName = autoLabelForPgn(pgn),
                        pgns = listOf(pgn),
                        lastSeenMs = now,
                        createdMs = now,
                    )
                )
                _pendingNmeaSourceNotice.value = normalizedSource
            } else {
                val previous = existing[profileIndex]
                val mergedPgns = (previous.pgns + pgn).distinct().distinctBy { it }
                existing[profileIndex] = previous.copy(
                    displayName = previous.displayName.ifBlank { autoLabelForPgn(pgn) },
                    pgns = mergedPgns,
                    lastSeenMs = now,
                )
            }

            current.copy(detectedNmeaSources = existing)
        }
    }

    private fun addNmeaPgnHistoryEntry(
        sourceKey: String?,
        pgn: Int,
        receivedUpdate: NmeaUpdate,
    ) {
        val normalizedSource = normalizeNmeaSourceKey(
            sourceKey.orEmpty().ifBlank { unknownNmeaSourceFallback },
        )
        val nowMs = System.currentTimeMillis()
        val payloadLength = receivedUpdate.textValues["n2k_payload_len"]?.trim()
            ?: receivedUpdate.textValues["payload_len"]?.trim()
            ?: receivedUpdate.textValues["payloadlength"]?.trim()
            ?: receivedUpdate.textValues["payload_length"]?.trim()
        val payloadHex = receivedUpdate.textValues["n2k_payload_hex"]?.trim()
            ?: receivedUpdate.textValues["payload"]?.trim()
        val rawLine = receivedUpdate.textValues["n2k_raw_payload"]?.trim()
            ?: receivedUpdate.textValues["n2k_raw_line"]?.trim()
            ?: receivedUpdate.textValues["n2k_debug_preview"]?.trim()
        val entry = NmeaPgnHistoryEntry(
            sourceKey = normalizedSource,
            pgn = pgn,
            receivedAtMs = nowMs,
            detectedPgnText = receivedUpdate.textValues["n2k_detected_pgn"]?.trim(),
            payloadLength = payloadLength,
            payloadHex = payloadHex,
            rawLine = rawLine,
        )
        _recentNmeaPgnHistory.update { current ->
            val next = mutableListOf<NmeaPgnHistoryEntry>()
            next.add(entry)
            next.addAll(current)
            if (next.size > MAX_NMEA_PGN_HISTORY) {
                next.subList(MAX_NMEA_PGN_HISTORY, next.size).clear()
            }
            next
        }
    }

    private fun addNmea0183HistoryEntry(
        sourceKey: String?,
        receivedUpdate: NmeaUpdate,
    ) {
        val normalizedSource = normalizeNmeaSourceKey(
            sourceKey.orEmpty().ifBlank { unknownNmeaSourceFallback },
        )
        val nowMs = System.currentTimeMillis()
        val sentence = receivedUpdate.textValues["nmea0183_sentence"]?.trim()
            ?: receivedUpdate.textValues["nmea_sentence"]?.trim()
            ?: "UNBEKANNT"
        val fullSentence = receivedUpdate.textValues["nmea0183_sentence_full"]?.trim()
            ?: receivedUpdate.textValues["nmea0183_sentence"]?.trim()
        val category = receivedUpdate.textValues["nmea0183_category"]?.trim()
            ?: receivedUpdate.textValues["nmea0183_user_category"]?.trim()
        val rawLine = receivedUpdate.textValues["nmea0183_raw_line"]?.trim()
            ?: receivedUpdate.textValues["nmea0183_raw"]?.trim()
        val fields = buildNmea0183HistoryFields(receivedUpdate)

        val entry = Nmea0183HistoryEntry(
            sourceKey = normalizedSource,
            sentence = sentence,
            fullSentence = fullSentence,
            receivedAtMs = nowMs,
            category = category,
            rawLine = rawLine,
            fields = fields,
        )
        _recentNmea0183History.update { current ->
            val next = mutableListOf<Nmea0183HistoryEntry>()
            next.add(entry)
            next.addAll(current)
            if (next.size > MAX_NMEA_0183_HISTORY) {
                next.subList(MAX_NMEA_0183_HISTORY, next.size).clear()
            }
            next
        }
    }

    private fun buildNmea0183HistoryFields(update: NmeaUpdate): List<String> {
        val fields = mutableListOf<String>()
        update.textValues["nmea0183_fields"]?.split(",")?.mapIndexed { index, value ->
            val item = value.trim()
            if (item.isNotBlank()) {
                "F${index + 1}=$item"
            } else {
                "F${index + 1}=<leer>"
            }
        }?.let { splitFields ->
            fields.addAll(splitFields)
        }

        update.textValues.forEach { (key, value) ->
            if (key.equals("nmea0183_fields", ignoreCase = true) || key.equals("nmea0183_raw_line", ignoreCase = true)) {
                return@forEach
            }
            val normalizedKey = key.trim()
            if (normalizedKey.isBlank()) return@forEach
            if (value.isBlank()) return@forEach
            fields.add("${normalizedKey}=$value")
        }

        val numericEntries = update.numericValues
            .filter { it.value.isFinite() }
            .map { (key, value) ->
                val normalizedKey = key.trim()
                if (normalizedKey.isBlank()) {
                    null
                } else {
                    "${normalizedKey}=${formatTelemetryValue(value)}"
                }
            }
            .filterNotNull()

        fields.addAll(numericEntries)
        return fields.distinct().sorted()
    }

    private fun formatTelemetryValue(value: Float): String {
        if (!value.isFinite()) return value.toString()
        return if (value % 1f == 0f) value.toInt().toString() else "%.3f".format(Locale.ROOT, value)
    }

    private fun extractNmeaSourceKey(update: NmeaUpdate): String? {
        return findMapValue(update.textValues, update.numericValues, listOf(
            "n2k_source",
            "source",
            "source_address",
            "sourceaddress",
            "src",
            "src_id",
            "srcid",
            "source_id",
            "source-id",
            "sourceaddressname",
            "source_name",
            "header.source",
            "header.src",
            "from",
            "srcaddress",
        ))
    }

    private fun extractNmeaPgn(update: NmeaUpdate): Int? {
        val direct = findMapValue(update.textValues, update.numericValues, listOf(
            "n2k_pgn",
            "pgn",
            "header.pgn",
            "message.pgn",
            "nmea2000.pgn",
        ))?.let(::parseNmeaPgnValue)
            ?: findAnyPgnCandidate(update.textValues, update.numericValues)?.let(::parseNmeaPgnValue)

        if (direct == null) return null
        return if (direct in 0..0xFFFF) direct else null
    }

    private fun findMapValue(
        textValues: Map<String, String>,
        numericValues: Map<String, Float>,
        keys: List<String>
    ): String? {
        val directText = keys.asSequence()
            .mapNotNull { textValues[it] }
            .firstOrNull { it.isNotBlank() }
            ?.trim()
        if (directText != null) return directText

        val directNumeric = keys.asSequence()
            .mapNotNull { numericValues[it] }
            .firstOrNull { it.isFinite() }
            ?.toInt()
            ?.toString()
        if (directNumeric != null) return directNumeric

        return scanKeysForPgnOrSource(textValues, numericValues, keys)
    }

    private fun parseNmeaPgnValue(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("0x", ignoreCase = true)) {
            val hex = trimmed.substring(2).trim()
            val parsedHex = hex.toIntOrNull(16)
            return parsedHex
        }

        val parsedFloat = trimmed.toFloatOrNull()
        if (parsedFloat != null) {
            return parsedFloat.toInt()
        }

        return trimmed.toIntOrNull()
    }

    private fun findAnyPgnCandidate(
        textValues: Map<String, String>,
        numericValues: Map<String, Float>,
    ): String? {
        val candidateText = textValues.entries.asSequence()
            .firstOrNull { (key, value) ->
                val normalizedKey = key.lowercase()
                value.isNotBlank() && (
                    normalizedKey == "pgn" ||
                    normalizedKey.endsWith(".pgn") ||
                    normalizedKey.endsWith("/pgn") ||
                    normalizedKey.endsWith("_pgn") ||
                    normalizedKey.endsWith(".n2k_pgn") ||
                    normalizedKey.endsWith("/n2k_pgn") ||
                    normalizedKey.endsWith("_n2k_pgn") ||
                    normalizedKey == "n2k_pgn"
                )
            }
            ?.value
            ?.trim()

        if (!candidateText.isNullOrBlank()) return candidateText

        val candidateNumeric = numericValues.keys.asSequence()
            .firstOrNull { key ->
                val normalizedKey = key.lowercase()
                normalizedKey == "pgn" ||
                    normalizedKey.endsWith(".pgn") ||
                    normalizedKey.endsWith("/pgn") ||
                    normalizedKey.endsWith("_pgn") ||
                    normalizedKey.endsWith(".n2k_pgn") ||
                    normalizedKey.endsWith("/n2k_pgn") ||
                    normalizedKey.endsWith("_n2k_pgn") ||
                    normalizedKey == "n2k_pgn"
            }
            ?.let { numericValues[it]?.toInt()?.toString() }

        return candidateNumeric
    }

    private fun scanKeysForPgnOrSource(
        textValues: Map<String, String>,
        numericValues: Map<String, Float>,
        allowedPrefix: List<String>,
    ): String? {
        val candidateText = textValues.entries.asSequence()
            .firstOrNull { (key, value) ->
                val keyLower = key.lowercase()
                allowedPrefix.any { keyLower == it || keyLower.endsWith(".$it") || keyLower.endsWith("/$it") }
            }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (candidateText != null) return candidateText

        val candidateNumeric = numericValues.keys.asSequence()
            .firstOrNull { key ->
                val keyLower = key.lowercase()
                allowedPrefix.any { keyLower == it || keyLower.endsWith(".$it") || keyLower.endsWith("/$it") }
            }
            ?.let { numericValues[it]?.toInt()?.toString() }
            ?: return null

        return candidateNumeric
    }

    private fun normalizeNmeaSourceKey(rawSource: String): String {
        val source = rawSource.trim().lowercase()
        if (source.isBlank()) return source
        if (source.startsWith("0x", ignoreCase = true)) return source
        val withoutQuotes = source.trim('"', '\'')
        val digitsOnly = withoutQuotes.trim()
            .trimStart('s', 'r', 'c', '-', '_')
        return if (digitsOnly.all { it.isDigit() } && digitsOnly.isNotBlank()) {
            "src-$digitsOnly"
        } else {
            withoutQuotes
        }
    }

    fun clearPendingNmeaSourceNotice() {
        _pendingNmeaSourceNotice.value = null
    }

    fun clearPendingNmea0183Classification() {
        _pendingNmea0183Classification.value = null
    }

    fun applyNmea0183Classification(sentence: String, category: String) {
        val normalizedSentence = normalizeNmea0183Sentence(sentence)
        if (normalizedSentence.isBlank()) {
            clearPendingNmea0183Classification()
            return
        }

        val normalizedCategory = category.trim().uppercase(Locale.ROOT)
        if (normalizedCategory.isBlank()) return

        if (!availableNmea0183SentenceCategoryOptions.contains(normalizedCategory)) return
        _state.update { current ->
            val next = current.nmea0183SentenceCategoryOverrides.toMutableMap()
            next.entries.removeIf { normalizeNmea0183Sentence(it.key) == normalizedSentence && it.key != normalizedSentence }
            next[normalizedSentence] = normalizedCategory
            current.copy(nmea0183SentenceCategoryOverrides = next)
        }
        _pendingNmea0183Classification.value = null
        nmea0183ClassificationCooldown.remove(normalizedSentence)
        persist()
    }

    fun postponeNmea0183ClassificationPrompt(sentence: String) {
        val normalizedSentence = normalizeNmea0183Sentence(sentence)
        if (normalizedSentence.isBlank()) {
            clearPendingNmea0183Classification()
            return
        }
        nmea0183ClassificationCooldown[normalizedSentence] = System.currentTimeMillis() + 3 * 60_000L
        clearPendingNmea0183Classification()
    }

    fun updateNmeaSourceDisplayName(sourceKey: String, displayName: String) {
        val normalizedSourceKey = sourceKey.trim().lowercase()
        if (normalizedSourceKey.isBlank()) return
        _state.update { current ->
            val next = current.detectedNmeaSources.toMutableList()
            val index = next.indexOfFirst { it.sourceKey.lowercase() == normalizedSourceKey }
            if (index < 0) return@update current
            next[index] = next[index].copy(displayName = displayName.trim())
            current.copy(detectedNmeaSources = next)
        }
        persist()
    }

    fun clearNmeaSourceDisplayName(sourceKey: String) {
        val normalizedSourceKey = sourceKey.trim().lowercase()
        if (normalizedSourceKey.isBlank()) return
        _state.update { current ->
            val next = current.detectedNmeaSources.toMutableList()
            val index = next.indexOfFirst { it.sourceKey.lowercase() == normalizedSourceKey }
            if (index < 0) return@update current
            val existing = next[index]
            next[index] = existing.copy(displayName = autoLabelForPgn(existing.pgns.firstOrNull() ?: -1))
            current.copy(detectedNmeaSources = next)
        }
        persist()
    }

    fun removeNmeaSourceProfile(sourceKey: String) {
        val normalizedSourceKey = sourceKey.trim().lowercase()
        if (normalizedSourceKey.isBlank()) return
        _state.update { current ->
            val next = current.detectedNmeaSources.filterNot { it.sourceKey.lowercase() == normalizedSourceKey }
            current.copy(detectedNmeaSources = next)
        }
        persist()
    }

    private fun autoLabelForPgn(pgn: Int): String {
        return when (pgn) {
            130306 -> "Windsensor"
            127250 -> "Kompass"
            127245 -> "Ruder / Autopilot"
            129026 -> "COG/SOG"
            129025, 129029 -> "GPS Position"
            127489 -> "Motordrehzahl"
            127506 -> "Tank (Wasser/Grau/Schwarz)"
            127508 -> "Batterie"
            127237 -> "Autopilot"
            128267 -> "Wassertiefe"
            129038, 129039 -> "AIS Ziel"
            60928 -> "ISO Address Claim"
            else -> ""
        }
    }

    private fun slewValue(current: Float, target: Float, maxDelta: Float): Float {
        val delta = target - current
        return when {
            delta > maxDelta -> current + maxDelta
            delta < -maxDelta -> current - maxDelta
            else -> target
        }
    }

    private fun stopRouterSimulation() {
        routerSimulationJob?.cancel()
        routerSimulationJob = null
        _state.update { current -> current.copy(simulationEnabled = false) }
        persist()
    }

    suspend fun sendAisCallMmsi(
        mmsi: String,
        host: String,
        port: Int,
    ): String? {
        val normalizedMmsi = mmsi.trim().filter { it.isDigit() }
        if (normalizedMmsi.isBlank()) {
            return "MMSI fehlt oder ist ungültig."
        }
        if (normalizedMmsi.length > 9) {
            return "Ungültige MMSI '$mmsi' (mehr als 9 Ziffern)."
        }
        val mmsiNumber = normalizedMmsi.toLongOrNull()
        if (mmsiNumber == null || mmsiNumber <= 0L) {
            return "Ungültige MMSI '$mmsi'."
        }

        val payload = JSONObject().apply {
            put("type", "ais_call")
            put("command", "call")
            put("mmsi", normalizedMmsi)
            put("source", "seafox")
        }
        val normalizedHost = host.ifBlank { DEFAULT_NMEA_ROUTER_HOST }
        val normalizedPort = port.coerceIn(1, 65535)

        return network.sendUdpTextWithResult(
            payload = payload.toString(),
            host = normalizedHost,
            port = normalizedPort,
        )
    }

    fun sendAutopilotCommand(request: AutopilotDispatchRequest) {
        val command = request.command
        val safetyDecision = AutopilotSafetyGate.evaluate(request)
        if (!safetyDecision.canDispatch) {
            _telemetryText.update { current ->
                current.toMutableMap().apply {
                    put("autopilot_safety_state", safetyDecision.state.name)
                    put("autopilot_command_status", safetyDecision.message)
                    put("autopilot_command_id", request.commandId)
                }
            }
            return
        }
        _telemetryText.update { current ->
            current.toMutableMap().apply {
                put("autopilot_safety_state", safetyDecision.state.name)
                put("autopilot_command_status", "Safety Gate akzeptiert; Befehl wird gesendet.")
                put("autopilot_command_id", request.commandId)
            }
        }
        when (request.backend) {
            AutopilotGatewayBackend.DIRECT_UDP_JSON -> {
                network.sendCommandJson(
                    payload = command.toUdpJson(),
                    host = request.host.ifBlank { "255.255.255.255" },
                    port = request.port
                )
            }
            AutopilotGatewayBackend.SIGNALK_V2 -> {
                sendSignalKCommand(request)
            }
            AutopilotGatewayBackend.YACHT_DEVICES_0183,
            AutopilotGatewayBackend.ACTISENSE_0183 -> {
                val payload = buildNmea0183AutopilotBundle(
                    command = command,
                    latDeg = request.latDeg,
                    lonDeg = request.lonDeg,
                    sogKn = request.sogKn,
                    cogDeg = request.cogDeg
                )
                if (payload.isNotBlank()) {
                    network.sendUdpText(
                        payload = payload,
                        host = request.host,
                        port = request.port
                    )
                }
            }
        }

        val modeCode = when (command.mode) {
            AutopilotControlMode.STBY -> 0f
            AutopilotControlMode.KURS -> 1f
            AutopilotControlMode.WIND -> 2f
        }
        _telemetry.update { current ->
            val merged = current.toMutableMap()
            merged["autopilot_mode"] = modeCode
            command.targetHeadingDeg?.let { merged["autopilot_heading_target"] = it.toFloat() }
            merged
        }
    }

    private fun sendSignalKCommand(request: AutopilotDispatchRequest) {
        val baseUrl = "http://${request.host}:${request.port}/signalk/v2/api/vessels/self/autopilots/${request.signalKAutopilotId}"
        val command = request.command
        val headers = buildAutopilotHttpHeaders(request)
        when (command.mode) {
            AutopilotControlMode.STBY -> {
                network.sendHttpJson(
                    method = "POST",
                    url = "$baseUrl/disengage",
                    payload = null,
                    headers = headers
                )
            }
            AutopilotControlMode.KURS -> {
                network.sendHttpJson(
                    method = "PUT",
                    url = "$baseUrl/mode",
                    payload = JSONObject().put("value", "compass").toString(),
                    headers = headers,
                )
                network.sendHttpJson(
                    method = "POST",
                    url = "$baseUrl/engage",
                    payload = null,
                    headers = headers
                )
                val target = command.targetHeadingDeg ?: command.sourceHeadingDeg
                network.sendHttpJson(
                    method = "PUT",
                    url = "$baseUrl/target",
                    payload = JSONObject().put("value", target).put("units", "deg").toString(),
                    headers = headers,
                )
            }
            AutopilotControlMode.WIND -> {
                network.sendHttpJson(
                    method = "PUT",
                    url = "$baseUrl/mode",
                    payload = JSONObject().put("value", "wind").toString(),
                    headers = headers,
                )
                network.sendHttpJson(
                    method = "POST",
                    url = "$baseUrl/engage",
                    payload = null,
                    headers = headers
                )
                val windTarget = command.windAngleRelativeDeg ?: command.targetHeadingDeg ?: command.sourceHeadingDeg
                network.sendHttpJson(
                    method = "PUT",
                    url = "$baseUrl/target",
                    payload = JSONObject().put("value", windTarget).put("units", "deg").toString(),
                    headers = headers,
                )
            }
        }
    }

    private fun buildAutopilotHttpHeaders(request: AutopilotDispatchRequest): Map<String, String> {
        val headers = linkedMapOf("Content-Type" to "application/json")
        request.authToken
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { token -> headers["Authorization"] = "Bearer $token" }
        return headers
    }

    fun selectPage(index: Int) {
        _state.update { current ->
            if (current.pages.isEmpty()) return@update current.copy(selectedPage = 0)
            current.copy(selectedPage = index.coerceIn(0, current.pages.lastIndex))
        }
        persist()
    }

    fun renamePage(index: Int, name: String) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (index !in pages.indices) return@update current
            pages[index] = pages[index].copy(name = name.ifBlank { "Seite ${index + 1}" })
            current.copy(pages = pages)
        }
        persist()
    }

    fun addPage(name: String) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            pages.add(createEmptyPage(name.ifBlank { "Seite ${current.pages.size + 1}" }))
            current.copy(pages = pages, selectedPage = pages.lastIndex)
        }
        persist()
    }

    private fun createEmptyPage(name: String): DashboardPage {
        return DashboardPage(
            name = name,
            widgets = emptyList(),
        )
    }

    fun addWidget(kind: WidgetKind, pageWidthPx: Float, pageHeightPx: Float, gridStepPx: Float): Boolean {
        val currentPage = _state.value.selectedPage
        if (pageWidthPx <= 0f || pageHeightPx <= 0f || gridStepPx <= 0f) return false

        val step = gridStepPx.coerceAtLeast(1f)
        val pageWidth = pageWidthPx.coerceAtLeast(1f)
        val pageHeight = pageHeightPx.coerceAtLeast(1f)

        var added = false
        _state.update { current ->
            if (current.pages.isEmpty() || currentPage !in current.pages.indices) return@update current

            val pages = current.pages.toMutableList()
            val page = pages[currentPage]
            val widgets = page.widgets.toMutableList()
            val prepared = defaultWidgetFor(kind, widgets.size)
            ensureWidgetMinimumGridUnits(prepared, widgetMinGridUnits)
            val (minWidth, minHeight) = resolveMinimumWidgetSize(prepared, step, widgetMinGridUnits)
            val effectiveWidthPx = prepared.widthPx.coerceIn(minWidth, pageWidth)
            val effectiveHeightPx = prepared.heightPx.coerceIn(minHeight, pageHeight)

            if (effectiveWidthPx > pageWidth || effectiveHeightPx > pageHeight) {
                return@update current
            }

            val maxX = pageWidth - effectiveWidthPx
            val maxY = pageHeight - effectiveHeightPx
            val baseX = snapToGrid(prepared.xPx, step).coerceIn(0f, maxX)
            val baseY = snapToGrid(prepared.yPx, step).coerceIn(0f, maxY)

            val candidate = prepared.copy(
                widthPx = effectiveWidthPx,
                heightPx = effectiveHeightPx,
                xPx = baseX,
                yPx = baseY,
            )
            val placed = findFreePlacementForNewWidget(candidate, widgets, pageWidth, pageHeight, step)
                ?: return@update current

            widgets.add(placed)
            pages[currentPage] = page.copy(widgets = widgets)
            added = true
            current.copy(pages = pages)
        }
        if (added) persist()
        return added
    }

    fun enforceMinimumWidgetSizesForPage(
        pageId: String,
        pageWidthPx: Float,
        pageHeightPx: Float,
        gridStepPx: Float,
    ) {
        val step = gridStepPx.coerceAtLeast(1f)
        var didChange = false
        _state.update { current ->
            val pages = current.pages.toMutableList()
            val pageIndex = pages.indexOfFirst { it.id == pageId }
            if (pageIndex < 0) return@update current

            val page = pages[pageIndex]
            val adjustedWidgets = page.widgets.map { widget ->
                ensureWidgetMinimumGridUnits(widget, widgetMinGridUnits)
                val (minWidth, minHeight) = resolveMinimumWidgetSize(widget, step, widgetMinGridUnits)
                val maxWidth = (pageWidthPx - widget.xPx).coerceAtLeast(minWidth)
                val maxHeight = (pageHeightPx - widget.yPx).coerceAtLeast(minHeight)
                val widthPx = widget.widthPx.coerceIn(minWidth, maxWidth)
                val heightPx = widget.heightPx.coerceIn(minHeight, maxHeight)
                val maxX = (pageWidthPx - widthPx).coerceAtLeast(0f)
                val maxY = (pageHeightPx - heightPx).coerceAtLeast(0f)
                val xPx = widget.xPx.coerceIn(0f, maxX)
                val yPx = widget.yPx.coerceIn(0f, maxY)
                val adjusted = widget.copy(
                    xPx = xPx,
                    yPx = yPx,
                    widthPx = widthPx,
                    heightPx = heightPx,
                )
                if (adjusted != widget) didChange = true
                adjusted
            }

            if (!didChange) return@update current

            pages[pageIndex] = page.copy(widgets = adjustedWidgets)
            current.copy(pages = pages)
        }
        if (didChange) persist()
    }

    fun moveWidget(
        widgetId: String,
        dx: Float,
        dy: Float,
        gridStepPx: Float,
        pageWidthPx: Float,
        pageHeightPx: Float,
        persistLayout: Boolean = true,
    ) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (current.selectedPage !in pages.indices) return@update current
            val page = pages[current.selectedPage]
            val widgets = page.widgets.toMutableList()
            val i = widgets.indexOfFirst { it.id == widgetId }
            if (i < 0) return@update current

            val w = widgets[i]
            ensureWidgetMinimumGridUnits(w, widgetMinGridUnits)
            val maxX = (pageWidthPx - w.widthPx).coerceAtLeast(0f)
            val maxY = (pageHeightPx - w.heightPx).coerceAtLeast(0f)
            val xRaw = (w.xPx + dx).coerceIn(0f, maxX)
            val yRaw = (w.yPx + dy).coerceIn(0f, maxY)
            widgets[i] = w.copy(
                xPx = xRaw,
                yPx = yRaw,
            )
            pages[current.selectedPage] = page.copy(
                widgets = widgets
            )
            current.copy(pages = pages)
        }
        if (persistLayout) {
            persist()
        } else {
            scheduleLayoutPersist()
        }
    }

    fun resizeWidget(
        widgetId: String,
        dw: Float,
        dh: Float,
        gridStepPx: Float,
        pageWidthPx: Float,
        pageHeightPx: Float,
        persistLayout: Boolean = true,
    ) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (current.selectedPage !in pages.indices) return@update current
            val page = pages[current.selectedPage]
            val widgets = page.widgets.toMutableList()
            val i = widgets.indexOfFirst { it.id == widgetId }
            if (i < 0) return@update current

            val w = widgets[i]
            val (minWidth, minHeight) = resolveMinimumWidgetSize(w, gridStepPx, widgetMinGridUnits)
            val maxWidth = (pageWidthPx - w.xPx).coerceAtLeast(minWidth)
            val maxHeight = (pageHeightPx - w.yPx).coerceAtLeast(minHeight)

            val widthRaw = (w.widthPx + dw).coerceIn(minWidth, maxWidth)
            val heightRaw = (w.heightPx + dh).coerceIn(minHeight, maxHeight)
            widgets[i] = w.copy(
                widthPx = widthRaw,
                heightPx = heightRaw,
            )
            pages[current.selectedPage] = page.copy(
                widgets = widgets
            )
            current.copy(pages = pages)
        }
        if (persistLayout) {
            persist()
        } else {
            scheduleLayoutPersist()
        }
    }

    private fun scheduleLayoutPersist() {
        layoutPersistJob?.cancel()
        layoutPersistJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            persist()
        }
    }

    fun snapWidgetToGrid(
        widgetId: String,
        gridStepPx: Float,
        pageWidthPx: Float,
        pageHeightPx: Float,
    ) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (current.selectedPage !in pages.indices) return@update current
            val page = pages[current.selectedPage]
            val widgets = page.widgets.toMutableList()
            val i = widgets.indexOfFirst { it.id == widgetId }
            if (i < 0) return@update current

            val w = widgets[i]
            val (minWidth, minHeight) = resolveMinimumWidgetSize(w, gridStepPx, widgetMinGridUnits)
            val snappedWidth = snapToGrid(w.widthPx, gridStepPx).coerceIn(minWidth, pageWidthPx.coerceAtLeast(minWidth))
            val snappedHeight = snapToGrid(w.heightPx, gridStepPx).coerceIn(minHeight, pageHeightPx.coerceAtLeast(minHeight))

            val maxX = (pageWidthPx - snappedWidth).coerceAtLeast(0f)
            val maxY = (pageHeightPx - snappedHeight).coerceAtLeast(0f)
            widgets[i] = w.copy(
                widthPx = snappedWidth,
                heightPx = snappedHeight,
                xPx = snapToGrid(w.xPx, gridStepPx).coerceIn(0f, maxX),
                yPx = snapToGrid(w.yPx, gridStepPx).coerceIn(0f, maxY),
            )

            pages[current.selectedPage] = page.copy(
                widgets = resolveCollisionsForFixedWidget(
                    widgets = widgets,
                    pageWidthPx = pageWidthPx,
                    pageHeightPx = pageHeightPx,
                    gridStepPx = gridStepPx,
                    fixedWidgetId = widgetId,
                )
            )
            current.copy(pages = pages)
        }
        persist()
    }

    fun autoArrangeCurrentPage(
        pageWidthPx: Float,
        pageHeightPx: Float,
        gridStepPx: Float,
    ) {
        val step = gridStepPx.coerceAtLeast(1f)
        if (pageWidthPx <= 0f || pageHeightPx <= 0f || step <= 0f) return

        var didChange = false
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (current.selectedPage !in pages.indices) return@update current

            val page = pages[current.selectedPage]
            if (page.widgets.isEmpty()) return@update current

            val arranged = autoArrangeWidgets(
                widgets = page.widgets,
                pageWidthPx = pageWidthPx,
                pageHeightPx = pageHeightPx,
                gridStepPx = step,
                widgetMinGridUnits = widgetMinGridUnits,
            )
            if (arranged == page.widgets) return@update current

            didChange = true
            pages[current.selectedPage] = page.copy(widgets = arranged)
            current.copy(pages = pages)
        }
        if (didChange) {
            persist()
        }
    }

    fun removeWidget(widgetId: String) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (current.selectedPage !in pages.indices) return@update current
            val page = pages[current.selectedPage]
            pages[current.selectedPage] = page.copy(widgets = page.widgets.filterNot { it.id == widgetId })
            val windWidgetSettings = current.windWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val batteryWidgetSettings = current.batteryWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val aisWidgetSettings = current.aisWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val echosounderWidgetSettings = current.echosounderWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val autopilotWidgetSettings = current.autopilotWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val logWidgetSettings = current.logWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val anchorWatchWidgetSettings = current.anchorWatchWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val temperatureWidgetSettings = current.temperatureWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            val seaChartWidgetSettings = current.seaChartWidgetSettings.toMutableMap().apply {
                remove(widgetId)
            }
            current.copy(
                pages = pages,
                windWidgetSettings = windWidgetSettings,
                batteryWidgetSettings = batteryWidgetSettings,
                aisWidgetSettings = aisWidgetSettings,
                echosounderWidgetSettings = echosounderWidgetSettings,
                autopilotWidgetSettings = autopilotWidgetSettings,
                logWidgetSettings = logWidgetSettings,
                anchorWatchWidgetSettings = anchorWatchWidgetSettings,
                temperatureWidgetSettings = temperatureWidgetSettings,
                seaChartWidgetSettings = seaChartWidgetSettings,
            )
        }
        widgetMinGridUnits.remove(widgetId)
        layoutPersistJob?.cancel()
        persist()
    }

    fun updateWindWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val windWidgetSettings = current.windWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(windWidgetSettings = windWidgetSettings)
        }
        persist()
    }

    fun updateBatteryWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val batteryWidgetSettings = current.batteryWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(batteryWidgetSettings = batteryWidgetSettings)
        }
        persist()
    }

    fun updateAisWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val aisWidgetSettings = current.aisWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(aisWidgetSettings = aisWidgetSettings)
        }
        persist()
    }

    fun updateEchosounderWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val echosounderWidgetSettings = current.echosounderWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(echosounderWidgetSettings = echosounderWidgetSettings)
        }
        persist()
    }

    fun updateAutopilotWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val autopilotWidgetSettings = current.autopilotWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(autopilotWidgetSettings = autopilotWidgetSettings)
        }
        persist()
    }

    fun updateLogWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val logWidgetSettings = current.logWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(logWidgetSettings = logWidgetSettings)
        }
        persist()
    }

    fun updateAnchorWatchWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val anchorWatchWidgetSettings = current.anchorWatchWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(anchorWatchWidgetSettings = anchorWatchWidgetSettings)
        }
        persist()
    }

    fun updateTemperatureWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val temperatureWidgetSettings = current.temperatureWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(temperatureWidgetSettings = temperatureWidgetSettings)
        }
        persist()
    }

    fun updateSeaChartWidgetSettings(widgetId: String, serializedSettings: String) {
        val payload = serializedSettings.trim()
        _state.update { current ->
            val seaChartWidgetSettings = current.seaChartWidgetSettings.toMutableMap().also { map ->
                if (payload.isBlank()) {
                    map.remove(widgetId)
                } else {
                    map[widgetId] = payload
                }
            }
            current.copy(seaChartWidgetSettings = seaChartWidgetSettings)
        }
        persist()
    }

    fun renameWidget(widgetId: String, newTitle: String) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            if (current.selectedPage !in pages.indices) return@update current
            val page = pages[current.selectedPage]
            val widgets = page.widgets.toMutableList()
            val index = widgets.indexOfFirst { it.id == widgetId }
            if (index < 0) return@update current
            val fallback = widgets[index].title.ifBlank { "Widget" }
            widgets[index] = widgets[index].copy(title = newTitle.trim().ifBlank { fallback })
            pages[current.selectedPage] = page.copy(widgets = widgets)
            current.copy(pages = pages)
        }
        layoutPersistJob?.cancel()
        persist()
    }

    fun clearAllStoredData() {
        repository.clearAllData()
        stopRouterSimulation()
        widgetMinGridUnits.clear()
        _state.value = loadInitialState()
        network.setRouterHost(_state.value.nmeaRouterHost)
        network.setRouterProtocol(_state.value.nmeaRouterProtocol)
        network.restart(_state.value.udpPort)
        persist()
    }

    fun restoreStoredData(): Boolean {
        val restored = repository.restoreLatestBackup()
        if (restored == null || restored.pages.isEmpty()) return false

        stopRouterSimulation()
        layoutPersistJob?.cancel()
        widgetMinGridUnits.clear()
        val realtimeState = withLiveNmeaConnection(restored)
        _state.value = realtimeState
        network.setRouterHost(realtimeState.nmeaRouterHost)
        network.setRouterProtocol(realtimeState.nmeaRouterProtocol)
        network.restart(realtimeState.udpPort)
        persist()
        return true
    }

    fun loadReleaseNotes(): String {
        return repository.loadReleaseNotes()
    }

    fun loadReleaseNoteCount(): Int {
        return repository.loadReleaseNoteCount()
    }

    override fun onCleared() {
        stopTabletGpsLocationUpdates()
        stopRouterSimulation()
        dalyBmsManager.stop()
        super.onCleared()
        network.stop()
    }

    private fun persist() {
        repository.save(_state.value)
    }

    private fun loadInitialState(): DashboardState {
        val loaded = repository.load()
        val effectiveState = if (loaded.pages.isNotEmpty()) loaded else DashboardState(
            pages = listOf(createSeedPage("Navigation")),
            selectedPage = 0,
            udpPort = DEFAULT_NMEA_ROUTER_PORT,
            simulationEnabled = false,
            gridStepPercent = 2.5f,
            darkBackground = true,
            uiFont = UiFont.ORBITRON,
            fontScale = 1.0f,
        )
        return withLiveNmeaConnection(ensureNavigationPageHasBaselineMap(effectiveState))
    }

    private fun withLiveNmeaConnection(baseState: DashboardState): DashboardState {
        val normalizedHost = baseState.nmeaRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }.trim()
        val normalizedPort = baseState.udpPort.coerceIn(1, 65535)
        val normalizedCategoryOverrides = baseState.nmea0183SentenceCategoryOverrides.entries
            .mapNotNull { (sentence, category) ->
                val normalizedSentence = normalizeNmea0183Sentence(sentence)
                val normalizedCategory = category.trim().uppercase(Locale.ROOT)
                if (normalizedSentence.isBlank()) return@mapNotNull null
                if (!availableNmea0183SentenceCategoryOptions.contains(normalizedCategory)) return@mapNotNull null
                normalizedSentence to normalizedCategory
            }
            .toMap()
        return baseState.copy(
            nmeaRouterHost = normalizedHost,
            udpPort = normalizedPort,
            nmeaRouterProtocol = baseState.nmeaRouterProtocol,
            simulationEnabled = false,
            nmea0183SentenceCategoryOverrides = normalizedCategoryOverrides,
        )
    }

    private fun createSeedPage(name: String): DashboardPage {
        return DashboardPage(
            name = name,
            widgets = listOf(
                defaultWidgetFor(WidgetKind.KARTEN, 0),
                defaultWidgetFor(WidgetKind.BATTERY, 1),
                defaultWidgetFor(WidgetKind.WIND, 2),
            )
        )
    }

    private fun ensureNavigationPageHasBaselineMap(state: DashboardState): DashboardState {
        if (state.pages.isEmpty()) return state
        val selectedIndex = state.selectedPage.coerceIn(0, state.pages.lastIndex)
        val updatedPages = state.pages.toMutableList()
        var changed = false

        state.pages.forEachIndexed { index, page ->
            if (!page.name.contains("Navigation", ignoreCase = true)) return@forEachIndexed
            if (page.widgets.isNotEmpty()) return@forEachIndexed
            updatedPages[index] = page.copy(
                widgets = listOf(
                    defaultWidgetFor(WidgetKind.KARTEN, 0),
                ),
            )
            changed = true
        }

        if (!changed) return state
        return state.copy(
            pages = updatedPages,
            selectedPage = selectedIndex,
        )
    }

    private fun defaultWidgetFor(kind: WidgetKind, index: Int): DashboardWidget {
        val columns = listOf(0.12f, 0.42f, 0.72f)
        val xBase = columns[index % columns.size] * 1080f
        val yBase = 120f + (index / 3) * 320f
        return DashboardWidget(
            kind = kind,
            title = widgetTitleForKind(kind),
            xPx = xBase,
            yPx = yBase,
            widthPx = widgetDefaultSizePx(kind).first,
            heightPx = widgetDefaultSizePx(kind).second,
            dataKeys = widgetDefaultDataKeys(kind)
        )
    }
}

private fun snapToGrid(value: Float, gridStepPx: Float): Float {
    if (gridStepPx <= 1f) return value
    return (value / gridStepPx).roundToInt() * gridStepPx
}

private fun resolveMinimumWidgetSize(
    widget: DashboardWidget,
    gridStepPx: Float,
    widgetMinGridUnits: MutableMap<String, Pair<Int, Int>>,
): Pair<Float, Float> {
    val step = gridStepPx.coerceAtLeast(1f)
    ensureWidgetMinimumGridUnits(widget, widgetMinGridUnits)
    val units = widgetMinGridUnits.getOrPut(widget.id) {
        widgetDefaultMinGridUnits(widget.kind)
    }
    return units.first.toFloat() * step to units.second.toFloat() * step
}

private fun autoArrangeWidgets(
    widgets: List<DashboardWidget>,
    pageWidthPx: Float,
    pageHeightPx: Float,
    gridStepPx: Float,
    widgetMinGridUnits: MutableMap<String, Pair<Int, Int>>,
): List<DashboardWidget> {
        if (widgets.isEmpty()) return widgets

        val step = gridStepPx.coerceAtLeast(1f)
        val arranged = mutableListOf<DashboardWidget>()
        val maxWidth = pageWidthPx.coerceAtLeast(1f)
        val maxHeight = pageHeightPx.coerceAtLeast(1f)

        val ordered = widgets
            .map { widget ->
                ensureWidgetMinimumGridUnits(widget, widgetMinGridUnits)
                val (minWidth, minHeight) = resolveMinimumWidgetSize(widget, step, widgetMinGridUnits)
                val widthPx = minOf(widget.widthPx.coerceIn(minWidth, maxWidth), maxWidth)
                val heightPx = minOf(widget.heightPx.coerceIn(minHeight, maxHeight), maxHeight)
                val effectiveWidth = snapToGrid(widthPx, step).coerceIn(minWidth, maxWidth)
                val effectiveHeight = snapToGrid(heightPx, step).coerceIn(minHeight, maxHeight)
                widget.copy(
                    widthPx = effectiveWidth,
                    heightPx = effectiveHeight,
                    xPx = snapToGrid(widget.xPx, step).coerceIn(0f, maxWidth - effectiveWidth),
                    yPx = snapToGrid(widget.yPx, step).coerceIn(0f, maxHeight - effectiveHeight),
                )
            }
            .sortedWith(compareByDescending<DashboardWidget> { it.widthPx * it.heightPx }
                .thenBy { it.yPx }
                .thenBy { it.xPx }
            )

        ordered.forEach { candidate ->
            val placed = findFreePlacementForNewWidget(
                widget = candidate,
                existingWidgets = arranged,
                pageWidthPx = maxWidth,
                pageHeightPx = maxHeight,
                gridStepPx = step,
            ) ?: candidate
            arranged.add(placed)
        }

        return resolveCollisions(
            widgets = arranged,
            pageWidthPx = maxWidth,
            pageHeightPx = maxHeight,
            gridStepPx = step,
        )
    }

private fun ensureWidgetMinimumGridUnits(
    widget: DashboardWidget,
    widgetMinGridUnits: MutableMap<String, Pair<Int, Int>>,
) {
    widgetMinGridUnits.putIfAbsent(widget.id, widgetDefaultMinGridUnits(widget.kind))
}

private fun normalizeAngle360(value: Float): Float {
    var wrapped = value % 360f
    if (wrapped < 0f) wrapped += 360f
    return wrapped
}

private fun resolveCollisions(
    widgets: List<DashboardWidget>,
    pageWidthPx: Float,
    pageHeightPx: Float,
    gridStepPx: Float,
    preferredFixedId: String? = null,
): List<DashboardWidget> {
    if (widgets.size < 2) return widgets

    val step = gridStepPx.coerceAtLeast(1f)
    val arranged = widgets.toMutableList()
    var pass = 0

    while (pass < 32) {
        var changed = false

        for (i in arranged.indices) {
            for (j in i + 1 until arranged.size) {
                val a = arranged[i]
                val b = arranged[j]
                if (!widgetsOverlap(a, b)) continue

                val moveIndex = when {
                    preferredFixedId != null && a.id == preferredFixedId -> j
                    preferredFixedId != null && b.id == preferredFixedId -> i
                    a.widthPx * a.heightPx <= b.widthPx * b.heightPx -> i
                    else -> j
                }
                val keepWidget = if (moveIndex == i) b else a
                val movedWidget = arranged[moveIndex]
                val nudged = nudgeWidgetOut(movedWidget, keepWidget, pageWidthPx, pageHeightPx, step)

                arranged[moveIndex] = if (widgetsOverlap(nudged, keepWidget)) {
                    placeInNearestFreeSpot(nudged, arranged, moveIndex, pageWidthPx, pageHeightPx, step)
                } else {
                    nudged
                }
                changed = true
            }
        }

        if (!changed) break
        pass++
    }

    return arranged
}

private fun resolveCollisionsForFixedWidget(
    widgets: List<DashboardWidget>,
    pageWidthPx: Float,
    pageHeightPx: Float,
    gridStepPx: Float,
    fixedWidgetId: String,
): List<DashboardWidget> {
    if (widgets.size < 2) return widgets

    val fixedIndex = widgets.indexOfFirst { it.id == fixedWidgetId }
    if (fixedIndex < 0) return widgets

    val step = gridStepPx.coerceAtLeast(1f)
    val maxX = (pageWidthPx - widgets[fixedIndex].widthPx).coerceAtLeast(0f)
    val maxY = (pageHeightPx - widgets[fixedIndex].heightPx).coerceAtLeast(0f)
    val clampedFixed = widgets[fixedIndex].copy(
        xPx = snapToGrid(widgets[fixedIndex].xPx, step).coerceIn(0f, maxX),
        yPx = snapToGrid(widgets[fixedIndex].yPx, step).coerceIn(0f, maxY),
    )

    if (!widgets.indices.any { it != fixedIndex && widgetsOverlap(clampedFixed, widgets[it]) }) {
        return widgets
    }

    return widgets.mapIndexed { index, widget ->
        if (index == fixedIndex) {
            placeInNearestFreeSpot(
                widget = clampedFixed,
                widgets = widgets,
                selfIndex = fixedIndex,
                pageWidthPx = pageWidthPx,
                pageHeightPx = pageHeightPx,
                gridStepPx = step,
            )
        } else {
            widget
        }
    }
}

private fun widgetsOverlap(a: DashboardWidget, b: DashboardWidget): Boolean {
    val ax2 = a.xPx + a.widthPx
    val ay2 = a.yPx + a.heightPx
    val bx2 = b.xPx + b.widthPx
    val by2 = b.yPx + b.heightPx

    return a.xPx < bx2 && ax2 > b.xPx && a.yPx < by2 && ay2 > b.yPx
}

private fun nudgeWidgetOut(
    small: DashboardWidget,
    other: DashboardWidget,
    pageWidthPx: Float,
    pageHeightPx: Float,
    gridStepPx: Float,
): DashboardWidget {
    val smallRight = small.xPx + small.widthPx
    val smallBottom = small.yPx + small.heightPx
    val otherRight = other.xPx + other.widthPx
    val otherBottom = other.yPx + other.heightPx

    val overlapX = min(smallRight - other.xPx, otherRight - small.xPx).coerceAtLeast(0f)
    val overlapY = min(smallBottom - other.yPx, otherBottom - small.yPx).coerceAtLeast(0f)

    val smallCenterX = small.xPx + small.widthPx / 2f
    val smallCenterY = small.yPx + small.heightPx / 2f
    val otherCenterX = other.xPx + other.widthPx / 2f
    val otherCenterY = other.yPx + other.heightPx / 2f

    var newX = small.xPx
    var newY = small.yPx
    if (overlapX <= overlapY) {
        val direction = if (smallCenterX < otherCenterX) -1f else 1f
        newX += direction * (overlapX + gridStepPx)
    } else {
        val direction = if (smallCenterY < otherCenterY) -1f else 1f
        newY += direction * (overlapY + gridStepPx)
    }

    val maxX = (pageWidthPx - small.widthPx).coerceAtLeast(0f)
    val maxY = (pageHeightPx - small.heightPx).coerceAtLeast(0f)
    return small.copy(
        xPx = snapToGrid(newX, gridStepPx).coerceIn(0f, maxX),
        yPx = snapToGrid(newY, gridStepPx).coerceIn(0f, maxY),
    )
}

private fun placeInNearestFreeSpot(
    widget: DashboardWidget,
    widgets: List<DashboardWidget>,
    selfIndex: Int,
    pageWidthPx: Float,
    pageHeightPx: Float,
    gridStepPx: Float,
): DashboardWidget {
    val maxX = (pageWidthPx - widget.widthPx).coerceAtLeast(0f)
    val maxY = (pageHeightPx - widget.heightPx).coerceAtLeast(0f)
    var best = widget
    var bestDist = Float.MAX_VALUE

    var y = 0f
    while (y <= maxY) {
        var x = 0f
        while (x <= maxX) {
            val candidate = widget.copy(xPx = x, yPx = y)
            val collides = widgets.indices
                .filter { it != selfIndex }
                .any { widgetsOverlap(candidate, widgets[it]) }
            if (!collides) {
                val distance = abs(candidate.xPx - widget.xPx) + abs(candidate.yPx - widget.yPx)
                if (distance < bestDist) {
                    bestDist = distance
                    best = candidate
                }
            }
            x += gridStepPx
        }
        y += gridStepPx
    }

    return best
}

private fun findFreePlacementForNewWidget(
    widget: DashboardWidget,
    existingWidgets: List<DashboardWidget>,
    pageWidthPx: Float,
    pageHeightPx: Float,
    gridStepPx: Float,
): DashboardWidget? {
    val maxX = (pageWidthPx - widget.widthPx).coerceAtLeast(0f)
    val maxY = (pageHeightPx - widget.heightPx).coerceAtLeast(0f)

    val snappedCandidate = widget.copy(
        xPx = snapToGrid(widget.xPx, gridStepPx).coerceIn(0f, maxX),
        yPx = snapToGrid(widget.yPx, gridStepPx).coerceIn(0f, maxY),
    )

    var best: DashboardWidget? = null
    var bestDist = Float.MAX_VALUE

    var y = 0f
    while (y <= maxY) {
        var x = 0f
        while (x <= maxX) {
            val candidate = snappedCandidate.copy(xPx = x, yPx = y)
            val collides = existingWidgets.any { widgetsOverlap(candidate, it) }
            if (!collides) {
                val distance = abs(candidate.xPx - snappedCandidate.xPx) + abs(candidate.yPx - snappedCandidate.yPx)
                if (distance < bestDist) {
                    bestDist = distance
                    best = candidate
                }
            }
            x += gridStepPx
        }
        y += gridStepPx
    }

    return best
}
