package com.seafox.nmea_dashboard.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.seafox.nmea_dashboard.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

private const val PREF_NAME = "dashboard_state"
private const val KEY_JSON = "dashboard_state_json"

private const val BACKUP_FILE_NAME_PREFIX = "seaFOX-NMEA2000-dashboard-state"
private const val BACKUP_FILE_NAME_LEGACY = "seaFOX-dashboard-state.json"
private const val BACKUP_FILE_NAME_LEGACY_CANONICAL = "seaFOX-NMEA2000-dashboard-state.json"
private const val BACKUP_FILE_NAME_LEGACY_PREFIX = "seaFOX-dashboard-state"
private const val BACKUP_FILE_MIME = "application/json"
private const val BACKUP_SUBFOLDER = "seaFOX"
private const val LOG_TAG = "DashboardRepository"
private const val STATE_SCHEMA_VERSION = 1

private const val KEY_STATE_SCHEMA_VERSION = "stateSchemaVersion"
private const val KEY_APP_VERSION_CODE = "appVersionCode"
private const val KEY_APP_VERSION_NAME = "appVersionName"
private const val KEY_BACKUP_CREATED_AT = "backupCreatedAt"
private const val KEY_BACKUP_REVISION = "backupRevision"
private const val KEY_BACKUP_STRATEGY = "backupStrategy"
private const val KEY_LAST_RELEASE_STATE = "release_state_last"
private const val KEY_LAST_APP_UPDATE_TIME = "app_last_update_time"
private const val KEY_DETECTED_NMEA_SOURCES = "detectedNmeaSources"
private const val KEY_NMEA0183_SENTENCE_CATEGORY_OVERRIDES = "nmea0183SentenceCategoryOverrides"
private const val RELEASE_NOTES_FILE_NAME = "release-notes.jsonl"

private const val KEY_BACKUP_SEQUENCE = "backup_sequence"
private const val KEY_BACKUP_SEQUENCE_VERSION_NAME = "backup_sequence_version_name"
private const val KEY_BACKUP_SEQUENCE_VERSION_CODE = "backup_sequence_version_code"

private const val BACKUP_STRATEGY_VERSION = 2
private const val MAX_BACKUP_FILES = 20

private data class BackupStrategyRevision(
    val version: Int,
    val title: String,
    val details: String,
)

private val BACKUP_STRATEGY_HISTORY = listOf(
    BackupStrategyRevision(
        version = 1,
        title = "Versionierte Dateinamen + Sequenz",
        details = "Backup-Dateinamen enthalten App-Version, Revisionsnummer und Timestamp; MediaStore/Public/Legacy werden konsistent gesucht.",
    ),
    BackupStrategyRevision(
        version = 2,
        title = "Eindeutige Backup-Metadaten im Header",
        details = "Backup-Metadaten erhalten eine stabile Strategie-Version im Header; Ladepfad loggt Migrationshinweise.",
    )
)

private fun backupStrategyDescription(version: Int): String {
    return BACKUP_STRATEGY_HISTORY
        .firstOrNull { it.version == version }
        ?.let { "${it.version}: ${it.title} — ${it.details}" }
        ?: "Unbekannte Strategie $version"
}

private fun backupStrategyChangelog(): String {
    return BACKUP_STRATEGY_HISTORY.joinToString(" | ") { "${it.version}: ${it.title}" }
}

private val BACKUP_VERSIONED_FILE_NAME = Regex(
    "^${Regex.escape(BACKUP_FILE_NAME_PREFIX)}-v(.+)-c(\\d+)-r(\\d+)-t(\\d+)\\.json$"
)

private data class ReleaseState(
    val versionName: String,
    val versionCode: Int,
    val backupStrategy: Int,
)

class DashboardRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val appVersionName by lazy { resolveAppVersionName() }
    private val appVersionCode by lazy { resolveAppVersionCode() }

    fun save(state: DashboardState) {
        val now = System.currentTimeMillis()
        val backupRevision = nextBackupRun()
        val json = buildJson(state, backupRevision, now).toString()
        preferences.edit().putString(KEY_JSON, json).apply()
        persistBackupCopy(json, backupRevision, now, state.backupPrivacyMode)
    }

    fun load(): DashboardState {
        val storedStateRaw = preferences.getString(KEY_JSON, null)
        val storedState = parseState(storedStateRaw)
        val storedReleaseState = parseReleaseStateFromJson(storedStateRaw)
        ensureReleaseNoteForAppUpdate(storedReleaseState)
        if (isUsableState(storedState)) {
            Log.i(LOG_TAG, "State loaded from SharedPreferences")
            return storedState!!
        }

        Log.i(LOG_TAG, "No SharedPreferences state found, trying Backup")

        val backupJson = readBackupJson()
        val backupState = parseState(backupJson)
        ensureReleaseNoteForAppUpdate(parseReleaseStateFromJson(backupJson))
        if (isUsableState(backupState)) {
            preferences.edit().putString(KEY_JSON, backupJson).apply()
            Log.i(LOG_TAG, "Restored state from latest backup")
            return backupState!!
        }

        Log.i(LOG_TAG, "No usable backup found. Falling back to app defaults.")

        return DashboardState()
    }

    fun restoreLatestBackup(): DashboardState? {
        val backupJson = readBackupJson()
        val backupState = parseState(backupJson)
        if (!isUsableState(backupState) || backupJson.isNullOrBlank()) return null

        preferences.edit().putString(KEY_JSON, backupJson).apply()
        return backupState
    }

    fun loadReleaseNotes(): String {
        val file = File(context.filesDir, RELEASE_NOTES_FILE_NAME)
        if (!file.exists()) {
            return writeInitialReleaseNote(file)
        }

        return try {
            val entries = file.readLines().filter { it.isNotBlank() }
            val total = entries.size
            if (total == 0) {
                writeInitialReleaseNote(file)
            } else {
                entries
                    .asReversed()
                    .mapIndexed { index, raw ->
                        val note = runCatching { JSONObject(raw) }.getOrNull()
                        val sequence = total - index
                        if (note == null) {
                            "($sequence) $raw"
                        } else {
                            formatReleaseNoteLine(note, sequence)
                        }
                    }
                    .joinToString("\n\n")
            }
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Release-Notizen konnten nicht gelesen werden", ex)
            writeInitialReleaseNote(file, "Release-Notizen konnten nicht gelesen werden.")
        }
    }

    fun loadReleaseNoteCount(): Int {
        val file = File(context.filesDir, RELEASE_NOTES_FILE_NAME)
        if (!file.exists()) {
            return 1
        }
        return try {
            val entries = file.readLines().filter { it.isNotBlank() }
            if (entries.isEmpty()) 1 else entries.size
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Release-Notiz-Zähler konnte nicht gelesen werden", ex)
            0
        }
    }

    private fun formatReleaseNoteLine(note: JSONObject, releaseSequence: Int): String {
        val from = note.optJSONObject("from")
        val to = note.optJSONObject("to")
        val fromVersion = from?.optString("versionName", "-") ?: "-"
        val toVersion = to?.optString("versionName", "-") ?: "-"
        val changes = note.optJSONArray("changes")
        val changeText = if (changes == null || changes.length() == 0) {
            ""
        } else {
            val items = buildList {
                repeat(changes.length()) { index ->
                    val change = changes.optString(index)
                    if (change.isNotBlank()) add(change)
                }
            }
            if (items.isEmpty()) {
                ""
            } else {
                "\n- " + items.joinToString("\n- ")
            }
        }
        return if (fromVersion.isBlank() || toVersion.isBlank()) {
            "($releaseSequence) $note"
        } else {
            "$fromVersion -> $toVersion ($releaseSequence)$changeText"
        }
    }

    private fun writeInitialReleaseNote(
        file: File,
        noteFallback: String = "Erste installierte Version dieser App.",
    ): String {
        val event = JSONObject()
            .put("event", "release_notes_baseline")
            .put("timestamp", System.currentTimeMillis())
            .put(
                "from",
                JSONObject()
                    .put("versionName", "unbekannt")
                    .put("versionCode", -1)
                    .put("backupStrategy", BACKUP_STRATEGY_VERSION)
            )
            .put(
                "to",
                JSONObject()
                    .put("versionName", appVersionName)
                    .put("versionCode", appVersionCode)
                    .put("backupStrategy", BACKUP_STRATEGY_VERSION)
            )
            .put(
                "changes",
                JSONArray().put(noteFallback)
                    .put("Backup-Strategie: ${backupStrategyDescription(BACKUP_STRATEGY_VERSION)}")
            )
            .put("changelog", backupStrategyChangelog())
        return try {
            file.writeText("${event}\n")
            formatReleaseNoteLine(event, 1)
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Release-Notes Datei konnte nicht initialisiert werden", ex)
            "Keine Release-Notizen gespeichert (Initialisierung fehlgeschlagen)."
        }
    }

    private fun isUsableState(state: DashboardState?): Boolean = state != null && state.pages.isNotEmpty()

    fun clearAllData() {
        preferences.edit().clear().apply()
        clearBackupFile()
    }

    private fun buildJson(state: DashboardState, backupRevision: Int, createdAt: Long): JSONObject {
        val root = JSONObject()
        root.put(KEY_STATE_SCHEMA_VERSION, STATE_SCHEMA_VERSION)
        root.put(KEY_APP_VERSION_CODE, appVersionCode)
        root.put(KEY_APP_VERSION_NAME, appVersionName)
        root.put(KEY_BACKUP_CREATED_AT, createdAt)
        root.put(KEY_BACKUP_REVISION, backupRevision)
        root.put(KEY_BACKUP_STRATEGY, BACKUP_STRATEGY_VERSION)
        root.put("selectedPage", state.selectedPage)
        root.put("nmeaRouterProtocol", state.nmeaRouterProtocol.name)
        root.put("nmeaRouterHost", state.nmeaRouterHost)
        root.put("udpPort", state.udpPort)
        root.put("simulationStartLatitude", state.simulationStartLatitude)
        root.put("simulationStartLongitude", state.simulationStartLongitude)
        root.put("simulationEnabled", state.simulationEnabled)
        root.put("gridStepPercent", state.gridStepPercent)
        root.put("darkBackground", state.darkBackground)
        root.put("widgetFrameStyle", state.widgetFrameStyle.name)
        root.put("widgetFrameStyleGrayOffset", state.widgetFrameStyleGrayOffset)
        root.put("uiFont", state.uiFont.name)
        root.put("fontScale", state.fontScale)
        root.put("layoutOrientation", state.layoutOrientation.name)
        root.put("windWidgetSettings", JSONObject(state.windWidgetSettings))
        root.put("batteryWidgetSettings", JSONObject(state.batteryWidgetSettings))
        root.put("aisWidgetSettings", JSONObject(state.aisWidgetSettings))
        root.put("echosounderWidgetSettings", JSONObject(state.echosounderWidgetSettings))
        root.put("autopilotWidgetSettings", JSONObject(state.autopilotWidgetSettings))
        root.put("logWidgetSettings", JSONObject(state.logWidgetSettings))
        root.put("anchorWatchWidgetSettings", JSONObject(state.anchorWatchWidgetSettings))
        root.put("temperatureWidgetSettings", JSONObject(state.temperatureWidgetSettings))
        root.put("seaChartWidgetSettings", JSONObject(state.seaChartWidgetSettings))
        root.put("alarmToneVolume", state.alarmToneVolume)
        root.put("alarmRepeatIntervalSeconds", state.alarmRepeatIntervalSeconds)
        root.put("alarmVoiceAnnouncementsEnabled", state.alarmVoiceAnnouncementsEnabled)
        root.put("alarmVoiceProfileIndex", state.alarmVoiceProfileIndex)
        root.put("backupPrivacyMode", state.backupPrivacyMode.name)
        root.put("bootAutostartEnabled", state.bootAutostartEnabled)
        root.put("onboardingCompleted", state.onboardingCompleted)
        root.put("boatProfile", JSONObject().apply {
            put("lengthMeters", state.boatProfile.lengthMeters)
            put("widthMeters", state.boatProfile.widthMeters)
            put("name", state.boatProfile.name)
            put("homePort", state.boatProfile.homePort)
            put("mmsi", state.boatProfile.mmsi)
            put("draftMeters", state.boatProfile.draftMeters)
            put("type", state.boatProfile.type.name)
        })
        root.put(KEY_DETECTED_NMEA_SOURCES, serializeNmeaSources(state.detectedNmeaSources))
        root.put(
            KEY_NMEA0183_SENTENCE_CATEGORY_OVERRIDES,
            JSONObject(state.nmea0183SentenceCategoryOverrides),
        )
        state.activeRoute?.let { root.put("activeRoute", serializeRoute(it)) }
        state.mobPosition?.let { root.put("mobPosition", serializeMobMarker(it)) }

        val pages = JSONArray()
        state.pages.forEach { page ->
            val pageJson = JSONObject()
            pageJson.put("id", page.id)
            pageJson.put("name", page.name)

            val widgets = JSONArray()
            page.widgets.forEach { widget ->
                val w = JSONObject()
                w.put("id", widget.id)
                w.put("kind", widget.kind.name)
                w.put("title", widget.title)
                w.put("xPx", widget.xPx)
                w.put("yPx", widget.yPx)
                w.put("widthPx", widget.widthPx)
                w.put("heightPx", widget.heightPx)

                val keys = JSONArray()
                widget.dataKeys.forEach { keys.put(it) }
                w.put("dataKeys", keys)
                widgets.put(w)
            }

            pageJson.put("widgets", widgets)
            pages.put(pageJson)
        }
        root.put("pages", pages)
        return root
    }

    private fun parseState(json: String?): DashboardState? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val selectedPage = root.optInt("selectedPage", 0)
            val schemaVersion = root.optInt(KEY_STATE_SCHEMA_VERSION, 1)
            val backupAppVersionCode = root.optInt(KEY_APP_VERSION_CODE, -1)
            val backupAppVersionName = root.optString(KEY_APP_VERSION_NAME, "unknown")
            val backupRevision = root.optInt(KEY_BACKUP_REVISION, -1)
            val backupCreatedAt = root.optLong(KEY_BACKUP_CREATED_AT, -1)
            val backupStrategy = root.optInt(KEY_BACKUP_STRATEGY, 1)
            val udpPort = root.optInt("udpPort", DEFAULT_NMEA_ROUTER_PORT).coerceIn(1, 65535)
            val nmeaRouterProtocol = runCatching {
                NmeaRouterProtocol.valueOf(
                    root.optString("nmeaRouterProtocol", NmeaRouterProtocol.TCP.name).uppercase()
                )
            }.getOrDefault(NmeaRouterProtocol.TCP)
            val nmeaRouterHost = root
                .optString("nmeaRouterHost", DEFAULT_NMEA_ROUTER_HOST)
                .trim()
                .ifBlank { DEFAULT_NMEA_ROUTER_HOST }
            val simulationEnabled = root.optBoolean("simulationEnabled", false)
            val gridStepPercent = root.optDouble("gridStepPercent", 2.5).toFloat()
            val simulationStartLatitude = root.optDouble(
                "simulationStartLatitude",
                DEFAULT_SIMULATION_START_LATITUDE.toDouble()
            ).toFloat()
            val simulationStartLongitude = root.optDouble(
                "simulationStartLongitude",
                DEFAULT_SIMULATION_START_LONGITUDE.toDouble()
            ).toFloat()
            val darkBackground = root.optBoolean("darkBackground", true)
            val widgetFrameStyleGrayOffset = root.optInt("widgetFrameStyleGrayOffset", 0)
            val widgetFrameStyle = runCatching {
                WidgetFrameStyle.valueOf(
                    root.optString("widgetFrameStyle", WidgetFrameStyle.BORDER.name).uppercase()
                )
            }.getOrDefault(WidgetFrameStyle.BORDER)
            val uiFont = root.optString("uiFont", UiFont.ORBITRON.name)
                .let {
                    try { UiFont.valueOf(it) } catch (_: Exception) { UiFont.ORBITRON }
                }
            val fontScale = root.optDouble("fontScale", 1.0).toFloat().coerceIn(0.7f, 1.6f)
            val layoutOrientation = runCatching {
                DashboardLayoutOrientation.valueOf(root.optString("layoutOrientation", DashboardLayoutOrientation.PORTRAIT.name))
            }.getOrDefault(DashboardLayoutOrientation.PORTRAIT)
            val windWidgetSettings = parseStringMap(root.optJSONObject("windWidgetSettings"))
            val batteryWidgetSettings = parseStringMap(root.optJSONObject("batteryWidgetSettings"))
            val aisWidgetSettings = parseStringMap(root.optJSONObject("aisWidgetSettings"))
            val echosounderWidgetSettings = parseStringMap(root.optJSONObject("echosounderWidgetSettings"))
            val autopilotWidgetSettings = parseStringMap(root.optJSONObject("autopilotWidgetSettings"))
            val logWidgetSettings = parseStringMap(root.optJSONObject("logWidgetSettings"))
            val anchorWatchWidgetSettings = parseStringMap(root.optJSONObject("anchorWatchWidgetSettings"))
            val temperatureWidgetSettings = parseStringMap(root.optJSONObject("temperatureWidgetSettings"))
            val seaChartWidgetSettings = parseStringMap(root.optJSONObject("seaChartWidgetSettings"))
            val alarmToneVolume = root.optDouble("alarmToneVolume", 0.7).toFloat().coerceIn(0f, 2f)
            val alarmRepeatIntervalSeconds = root.optInt("alarmRepeatIntervalSeconds", 5).coerceIn(2, 10)
            val alarmVoiceAnnouncementsEnabled = root.optBoolean("alarmVoiceAnnouncementsEnabled", true)
            val alarmVoiceProfileIndex = root.optInt("alarmVoiceProfileIndex", 0).coerceIn(0, 3)
            val backupPrivacyMode = runCatching {
                BackupPrivacyMode.valueOf(
                    root.optString("backupPrivacyMode", BackupPrivacyMode.privateOnly.name)
                )
            }.getOrDefault(BackupPrivacyMode.privateOnly)
            val bootAutostartEnabled = root.optBoolean("bootAutostartEnabled", false)
            val onboardingCompleted = root.optBoolean("onboardingCompleted", false)
            val detectedNmeaSources = parseNmeaSourceProfiles(root.optJSONArray(KEY_DETECTED_NMEA_SOURCES))
            val nmea0183SentenceCategoryOverrides = parseStringMap(
                root.optJSONObject(KEY_NMEA0183_SENTENCE_CATEGORY_OVERRIDES)
            )
            val boatProfile = parseBoatProfile(root.optJSONObject("boatProfile"))
            val activeRoute = parseRoute(root.optJSONObject("activeRoute"))
            val mobPosition = parseMobMarker(root.optJSONObject("mobPosition"))

            if (
                backupAppVersionCode <= 0 ||
                backupAppVersionName == "unknown" ||
                backupAppVersionCode != appVersionCode ||
                backupAppVersionName != appVersionName
            ) {
                Log.w(
                    LOG_TAG,
                    "Backup stammt aus älterer Version v$backupAppVersionName ($backupAppVersionCode), " +
                        "Revision $backupRevision, erstellt ${if (backupCreatedAt > 0L) backupCreatedAt else 0L}, " +
                    "und wird auf App-Version $appVersionName ($appVersionCode) migriert."
                )
            }
            if (backupStrategy != BACKUP_STRATEGY_VERSION) {
                Log.w(
                    LOG_TAG,
                    "Backup nutzt Backup-Strategie $backupStrategy, aktuelle Strategie ist " +
                        "$BACKUP_STRATEGY_VERSION (${backupStrategyDescription(BACKUP_STRATEGY_VERSION)}); " +
                        "Backup-Strategie ${backupStrategyDescription(backupStrategy)} wird migriert."
                )
                Log.i(LOG_TAG, "Bekannte Backup-Strategien: ${backupStrategyChangelog()}")
            }
            recordReleaseNoteIfNeeded(
                sourceVersionName = backupAppVersionName,
                sourceVersionCode = backupAppVersionCode,
                sourceStrategy = backupStrategy,
            )
            Log.d(
                LOG_TAG,
                "State schema v$schemaVersion loaded for backup v$backupAppVersionName " +
                    "($backupAppVersionCode), revision $backupRevision, app running " +
                    "v$appVersionName ($appVersionCode)"
            )

            val pages = mutableListOf<DashboardPage>()
            val pagesJson = root.optJSONArray("pages") ?: JSONArray()
            for (i in 0 until pagesJson.length()) {
                val p = pagesJson.getJSONObject(i)
                val widgetsJson = p.optJSONArray("widgets") ?: JSONArray()
                val widgets = mutableListOf<DashboardWidget>()

                for (j in 0 until widgetsJson.length()) {
                    val w = widgetsJson.getJSONObject(j)
                    val dataKeys = mutableListOf<String>()
                    val keysArray = w.optJSONArray("dataKeys") ?: JSONArray()
                    for (k in 0 until keysArray.length()) {
                        dataKeys.add(keysArray.optString(k))
                    }

                    val rawKind = runCatching {
                        WidgetKind.valueOf(w.optString("kind", WidgetKind.BATTERY.name))
                    }.getOrElse { WidgetKind.BATTERY }
                    val kind = when (rawKind) {
                        WidgetKind.SEA_CHART,
                        WidgetKind.SEA_CHART_PIXEL -> WidgetKind.KARTEN
                        else -> rawKind
                    }
                    val storedTitle = w.optString("title")
                    val title = if (rawKind == WidgetKind.SEA_CHART || rawKind == WidgetKind.SEA_CHART_PIXEL) {
                        if (storedTitle.contains("OpenSea", ignoreCase = true) || storedTitle.contains("SeaChart", ignoreCase = true)) {
                            widgetTitleForKind(kind)
                        } else {
                            storedTitle.ifBlank { widgetTitleForKind(kind) }
                        }
                    } else {
                        storedTitle.ifBlank { widgetTitleForKind(kind) }
                    }

                    widgets.add(
                        DashboardWidget(
                            id = w.optString("id"),
                            kind = kind,
                            title = title,
                            xPx = w.optDouble("xPx", 80.0).toFloat(),
                            yPx = w.optDouble("yPx", 80.0).toFloat(),
                            widthPx = w.optDouble("widthPx", 340.0).toFloat(),
                            heightPx = w.optDouble("heightPx", 220.0).toFloat(),
                            dataKeys = dataKeys,
                        )
                    )
                }

                pages.add(
                    DashboardPage(
                        id = p.optString("id"),
                        name = p.optString("name", "Seite ${i + 1}"),
                        widgets = widgets,
                    )
                )
            }

            DashboardState(
                pages = pages,
                selectedPage = selectedPage,
                udpPort = udpPort,
                nmeaRouterProtocol = nmeaRouterProtocol,
                nmeaRouterHost = nmeaRouterHost,
                simulationStartLatitude = simulationStartLatitude.coerceIn(-90f, 90f),
                simulationStartLongitude = simulationStartLongitude.coerceIn(-180f, 180f),
                simulationEnabled = simulationEnabled,
                gridStepPercent = gridStepPercent.coerceIn(0.5f, 20f),
                darkBackground = darkBackground,
                widgetFrameStyle = widgetFrameStyle,
                widgetFrameStyleGrayOffset = widgetFrameStyleGrayOffset.coerceIn(-10, 10),
                uiFont = uiFont,
                fontScale = fontScale,
                layoutOrientation = layoutOrientation,
                windWidgetSettings = windWidgetSettings,
                batteryWidgetSettings = batteryWidgetSettings,
                aisWidgetSettings = aisWidgetSettings,
                echosounderWidgetSettings = echosounderWidgetSettings,
                autopilotWidgetSettings = autopilotWidgetSettings,
                logWidgetSettings = logWidgetSettings,
                anchorWatchWidgetSettings = anchorWatchWidgetSettings,
                temperatureWidgetSettings = temperatureWidgetSettings,
                seaChartWidgetSettings = seaChartWidgetSettings,
                alarmToneVolume = alarmToneVolume,
                alarmRepeatIntervalSeconds = alarmRepeatIntervalSeconds,
                alarmVoiceAnnouncementsEnabled = alarmVoiceAnnouncementsEnabled,
                alarmVoiceProfileIndex = alarmVoiceProfileIndex,
                backupPrivacyMode = backupPrivacyMode,
                bootAutostartEnabled = bootAutostartEnabled,
                onboardingCompleted = onboardingCompleted,
                boatProfile = boatProfile,
                detectedNmeaSources = detectedNmeaSources,
                nmea0183SentenceCategoryOverrides = nmea0183SentenceCategoryOverrides,
                activeRoute = activeRoute,
                mobPosition = mobPosition,
            ).copy(
                simulationEnabled = false,
            )
        } catch (_: Exception) {
            Log.w(LOG_TAG, "Konfiguration konnte nicht geparst werden")
            null
        }
    }

    private fun parseBoatProfile(raw: JSONObject?): BoatProfile {
        if (raw == null) return BoatProfile()

        val type = runCatching {
            BoatType.valueOf(raw.optString("type").ifBlank { BoatType.MOTORBOOT.name })
        }.getOrDefault(BoatType.MOTORBOOT)

        val lengthMeters = raw.optDouble("lengthMeters", 0.0).toFloat().coerceAtLeast(0f)
        val widthMeters = raw.optDouble("widthMeters", 0.0).toFloat().coerceAtLeast(0f)
        val draftMeters = raw.optDouble("draftMeters", 0.0).toFloat().coerceAtLeast(0f)

        return BoatProfile(
            lengthMeters = lengthMeters,
            widthMeters = widthMeters,
            name = raw.optString("name", ""),
            homePort = raw.optString("homePort", ""),
            mmsi = raw.optString("mmsi", ""),
            draftMeters = draftMeters,
            type = type,
        )
    }

    private fun parseStringMap(jsonObject: JSONObject?): Map<String, String> {
        if (jsonObject == null) return emptyMap()
        val values = mutableMapOf<String, String>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = jsonObject.opt(key)) {
                is String -> values[key] = value
                is JSONObject -> values[key] = value.toString()
            }
        }
        return values
    }

    private fun serializeRoute(route: SerializedRoute): JSONObject {
        return JSONObject().apply {
            put("id", route.id)
            put("name", route.name)
            put("activeLegIndex", route.activeLegIndex)
            put(
                "waypoints",
                JSONArray().apply {
                    route.waypoints.forEach { waypoint ->
                        put(
                            JSONObject().apply {
                                put("id", waypoint.id)
                                put("name", waypoint.name)
                                put("lat", waypoint.lat)
                                put("lon", waypoint.lon)
                                put("passRadiusNm", waypoint.passRadiusNm)
                            }
                        )
                    }
                }
            )
        }
    }

    private fun parseRoute(raw: JSONObject?): SerializedRoute? {
        if (raw == null) return null
        val id = raw.optString("id").trim()
        if (id.isBlank()) return null
        val waypointsJson = raw.optJSONArray("waypoints") ?: JSONArray()
        val waypoints = buildList {
            for (index in 0 until waypointsJson.length()) {
                val item = waypointsJson.optJSONObject(index) ?: continue
                val waypointId = item.optString("id").trim()
                if (waypointId.isBlank()) continue
                add(
                    SerializedWaypoint(
                        id = waypointId,
                        name = item.optString("name", ""),
                        lat = item.optDouble("lat"),
                        lon = item.optDouble("lon"),
                        passRadiusNm = item.optDouble("passRadiusNm", 0.1).toFloat().coerceAtLeast(0f),
                    )
                )
            }
        }
        return SerializedRoute(
            id = id,
            name = raw.optString("name", ""),
            waypoints = waypoints,
            activeLegIndex = raw.optInt("activeLegIndex", 0).coerceAtLeast(0),
        )
    }

    private fun serializeMobMarker(marker: MobMarker): JSONObject {
        return JSONObject().apply {
            put("lat", marker.lat)
            put("lon", marker.lon)
            put("timestampMs", marker.timestampMs)
        }
    }

    private fun parseMobMarker(raw: JSONObject?): MobMarker? {
        if (raw == null) return null
        return MobMarker(
            lat = raw.optDouble("lat"),
            lon = raw.optDouble("lon"),
            timestampMs = raw.optLong("timestampMs", System.currentTimeMillis()),
        )
    }

    private fun serializeNmeaSources(profiles: List<NmeaSourceProfile>): JSONArray {
        val array = JSONArray()
        val seen = linkedSetOf<String>()
        profiles.forEach { profile ->
            val normalizedSource = profile.sourceKey.trim()
            if (normalizedSource.isBlank() || !seen.add(normalizedSource.lowercase(Locale.ROOT))) return@forEach
            array.put(
                JSONObject().apply {
                    put("sourceKey", normalizedSource)
                    put("displayName", profile.displayName)
                    put("lastSeenMs", profile.lastSeenMs)
                    put("createdMs", profile.createdMs)
                    val pgns = JSONArray()
                    profile.pgns.forEach { pgns.put(it) }
                    put("pgns", pgns)
                }
            )
        }
        return array
    }

    private fun parseNmeaSourceProfiles(jsonArray: JSONArray?): List<NmeaSourceProfile> {
        if (jsonArray == null || jsonArray.length() == 0) return emptyList()
        val now = System.currentTimeMillis()
        val profiles = linkedMapOf<String, NmeaSourceProfile>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val sourceKey = item.optString("sourceKey").trim()
            if (sourceKey.isBlank()) continue

            val pgnArray = item.optJSONArray("pgns")
            val pgns = linkedSetOf<Int>()
            if (pgnArray != null) {
                for (j in 0 until pgnArray.length()) {
                    val pgn = pgnArray.optInt(j)
                    if (pgn > 0) pgns.add(pgn)
                }
            }

            val createdMs = item.optLong("createdMs", now)
            val lastSeenMs = item.optLong("lastSeenMs", createdMs)
            profiles[sourceKey.lowercase(Locale.ROOT)] = NmeaSourceProfile(
                sourceKey = sourceKey,
                displayName = item.optString("displayName", ""),
                pgns = pgns.toList(),
                createdMs = createdMs,
                lastSeenMs = lastSeenMs,
            )
        }

        return profiles.values.toList()
    }

    private fun recordReleaseNoteIfNeeded(
        sourceVersionName: String,
        sourceVersionCode: Int,
        sourceStrategy: Int,
    ) {
        if (sourceVersionCode <= 0 || sourceVersionName == "unknown") return

        val sourceState = ReleaseState(
            sourceVersionName,
            sourceVersionCode,
            sourceStrategy,
        )
        val targetState = ReleaseState(
            appVersionName,
            appVersionCode,
            BACKUP_STRATEGY_VERSION,
        )

        val currentStateKey = releaseStateKey(targetState)
        val lastState = parseReleaseState(preferences.getString(KEY_LAST_RELEASE_STATE, null))
        if (lastState == null) {
            preferences.edit().putString(KEY_LAST_RELEASE_STATE, currentStateKey).apply()
            return
        }

        if (releaseStateKey(lastState) == currentStateKey) return

        val note = JSONObject()
            .put("event", "release_note")
            .put("timestamp", System.currentTimeMillis())
            .put(
                "from",
                JSONObject()
                    .put("versionName", sourceState.versionName)
                    .put("versionCode", sourceState.versionCode)
                    .put("backupStrategy", sourceState.backupStrategy)
            )
            .put(
                "to",
                JSONObject()
                    .put("versionName", targetState.versionName)
                    .put("versionCode", targetState.versionCode)
                    .put("backupStrategy", targetState.backupStrategy)
            )
            .put(
                "changes",
                JSONArray().apply {
                    put("App-Version: ${sourceState.versionName} (${sourceState.versionCode}) -> ${targetState.versionName} (${targetState.versionCode})")
                    currentBuildKeywordLine()?.let { put(it) }
                    if (sourceState.backupStrategy != targetState.backupStrategy) {
                        put(
                            "Backup-Strategie: ${backupStrategyDescription(sourceState.backupStrategy)} -> " +
                                backupStrategyDescription(targetState.backupStrategy)
                        )
                    }
                }
            )
            .put("changelog", backupStrategyChangelog())

        Log.i(LOG_TAG, "Release-Note: $note")
        persistReleaseNoteToFile(note)
        preferences.edit().putString(KEY_LAST_RELEASE_STATE, currentStateKey).apply()
    }

    private fun ensureReleaseNoteForAppUpdate(sourceState: ReleaseState?) {
        val updateTime = resolveAppUpdateTime()
        if (updateTime <= 0L) return

        val lastUpdateTime = preferences.getLong(KEY_LAST_APP_UPDATE_TIME, -1L)
        if (updateTime == lastUpdateTime) return

        val currentState = ReleaseState(
            appVersionName,
            appVersionCode,
            BACKUP_STRATEGY_VERSION,
        )

        if (lastUpdateTime <= 0L) {
            if (hasExistingReleaseNotes() || sourceState != null) {
                val note = JSONObject()
                    .put("event", "app_update")
                    .put("timestamp", System.currentTimeMillis())
                    .put(
                        "from",
                        JSONObject()
                            .put("versionName", sourceState?.versionName ?: currentState.versionName)
                            .put("versionCode", sourceState?.versionCode ?: -1)
                            .put("backupStrategy", sourceState?.backupStrategy ?: currentState.backupStrategy)
                    )
                    .put(
                        "to",
                        JSONObject()
                            .put("versionName", currentState.versionName)
                            .put("versionCode", currentState.versionCode)
                            .put("backupStrategy", currentState.backupStrategy)
                            .put("appUpdateTime", updateTime)
                    )
                    .put(
                        "changes",
                        JSONArray().apply {
                            put("App-Paket wurde als Update/Neuinstallation erkannt.")
                            currentBuildKeywordLine()?.let { put(it) }
                        }
                    )
                    .put("changelog", backupStrategyChangelog())

                Log.i(LOG_TAG, "Release-Note (erster App-Updatelauf): $note")
                persistReleaseNoteToFile(note)
            }
            preferences.edit().putString(KEY_LAST_RELEASE_STATE, releaseStateKey(currentState)).apply()
            preferences.edit().putLong(KEY_LAST_APP_UPDATE_TIME, updateTime).apply()
            return
        }

        val source = sourceState
            ?: parseReleaseState(preferences.getString(KEY_LAST_RELEASE_STATE, null))
            ?: ReleaseState(
                currentState.versionName,
                currentState.versionCode,
                currentState.backupStrategy,
            )

        val note = JSONObject()
            .put("event", "app_update")
            .put("timestamp", System.currentTimeMillis())
            .put(
                "from",
                JSONObject()
                    .put("versionName", source.versionName)
                    .put("versionCode", source.versionCode)
                    .put("backupStrategy", source.backupStrategy)
                    .put("appUpdateTime", lastUpdateTime)
            )
            .put(
                "to",
                JSONObject()
                    .put("versionName", currentState.versionName)
                    .put("versionCode", currentState.versionCode)
                    .put("backupStrategy", currentState.backupStrategy)
                    .put("appUpdateTime", updateTime)
            )
            .put(
                "changes",
                JSONArray().apply {
                    put("App-Paket wurde aktualisiert/neu installiert.")
                    currentBuildKeywordLine()?.let { put(it) }
                }
            )
            .put("changelog", backupStrategyChangelog())

        Log.i(LOG_TAG, "Release-Note (App-Update): $note")
        persistReleaseNoteToFile(note)
        preferences.edit()
            .putString(KEY_LAST_RELEASE_STATE, releaseStateKey(currentState))
            .putLong(KEY_LAST_APP_UPDATE_TIME, updateTime)
            .apply()
    }

    private fun hasExistingReleaseNotes(): Boolean {
        val file = File(context.filesDir, RELEASE_NOTES_FILE_NAME)
        return try {
            file.exists() && file.readLines().any { it.isNotBlank() }
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Release-Notizdatei konnte nicht gelesen werden", ex)
            false
        }
    }

    private fun parseReleaseState(raw: String?): ReleaseState? {
        val parts = raw?.split("|") ?: return null
        if (parts.size != 3) return null
        val code = parts[1].toIntOrNull() ?: return null
        val strategy = parts[2].toIntOrNull() ?: return null
        return ReleaseState(parts[0], code, strategy)
    }

    private fun parseReleaseStateFromJson(raw: String?): ReleaseState? {
        if (raw.isNullOrBlank()) return null
        return try {
            val root = JSONObject(raw)
            val versionName = root.optString(KEY_APP_VERSION_NAME, "unbekannt")
            val versionCode = root.optInt(KEY_APP_VERSION_CODE, -1)
            val strategy = root.optInt(KEY_BACKUP_STRATEGY, BACKUP_STRATEGY_VERSION)
            if (versionCode <= 0) null else ReleaseState(versionName, versionCode, strategy)
        } catch (_: Exception) {
            null
        }
    }

    private fun releaseStateKey(state: ReleaseState): String =
        "${state.versionName}|${state.versionCode}|${state.backupStrategy}"

    private fun persistReleaseNoteToFile(note: JSONObject) {
        try {
            val file = File(context.filesDir, RELEASE_NOTES_FILE_NAME)
            file.appendText(note.toString() + "\n")
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Release-Note konnte nicht gespeichert werden", ex)
        }
    }

    private fun currentBuildKeywordLine(): String? {
        val raw = BuildConfig.APP_BUILD_KEYWORDS.trim()
        if (raw.isBlank()) return null
        return "Build-Stichwoerter: $raw"
    }

    private fun nextBackupRun(): Int {
        val versionName = appVersionName
        val versionCode = appVersionCode
        val storedName = preferences.getString(KEY_BACKUP_SEQUENCE_VERSION_NAME, null)
        val storedCode = preferences.getInt(KEY_BACKUP_SEQUENCE_VERSION_CODE, -1)
        val next = if (storedName != versionName || storedCode != versionCode) {
            1
        } else {
            preferences.getInt(KEY_BACKUP_SEQUENCE, 0) + 1
        }.coerceAtLeast(1)

        preferences.edit()
            .putInt(KEY_BACKUP_SEQUENCE, next)
            .putString(KEY_BACKUP_SEQUENCE_VERSION_NAME, versionName)
            .putInt(KEY_BACKUP_SEQUENCE_VERSION_CODE, versionCode)
            .apply()
        return next
    }

    private fun safeVersionName(): String =
        appVersionName
            .replace(Regex("[^A-Za-z0-9._-]"), "-")
            .ifBlank { "0.0.0" }

    private fun buildBackupFileName(run: Int, createdAt: Long): String {
        val safeVersion = safeVersionName().lowercase(Locale.US)
        return "${BACKUP_FILE_NAME_PREFIX}-v$safeVersion-c$appVersionCode-r$run-t$createdAt.json"
    }

    private fun currentBackupFilePrefixCandidates(): List<String> = listOf(
        BACKUP_FILE_NAME_PREFIX,
        BACKUP_FILE_NAME_LEGACY_PREFIX,
        BACKUP_FILE_NAME_LEGACY.substringBefore(".json"),
        BACKUP_FILE_NAME_LEGACY_CANONICAL.substringBefore(".json"),
    )

    private fun resolveAppVersionName(): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "0.0.0"
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "App-Versionsname konnte nicht ermittelt werden", ex)
            "0.0.0"
        }
    }

    private fun resolveAppVersionCode(): Int {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(info).toInt()
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "App-Versionscode konnte nicht ermittelt werden", ex)
            0
        }
    }

    private fun resolveAppUpdateTime(): Long {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "App-Updatezeit konnte nicht ermittelt werden", ex)
            -1L
        }
    }

    private fun persistBackupCopy(
        json: String,
        backupRevision: Int,
        createdAt: Long,
        privacyMode: BackupPrivacyMode,
    ) {
        val fileName = buildBackupFileName(backupRevision, createdAt)
        persistBackupInPrivateFile(json, fileName)
        if (!BackupPrivacyPolicy.allowsPublicBackup(privacyMode)) {
            trimPrivateBackupHistory()
            return
        }
        try {
            val uri = ensureBackupUri(fileName)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(json.toByteArray())
                }
                trimMediaStoreBackupHistory()
                return
            }
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Fehler beim Speichern des Backup-Exports", ex)
        }
        if (persistBackupInPublicFolder(json, fileName)) {
            trimPublicBackupHistory()
            return
        }
        persistBackupInLegacyFile(json, fileName)
    }

    private fun readBackupJson(): String? {
        return readPrivateBackupFile() ?: readFromMediaStoreBackup() ?: readPublicBackupFile() ?: readLegacyBackup()
    }

    private fun persistBackupInPrivateFile(json: String, fileName: String) {
        try {
            val backupDir = privateBackupDirectory()
            if (!backupDir.exists() && !backupDir.mkdirs()) return
            File(backupDir, fileName).writeText(json)
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Internes Backup konnte nicht gespeichert werden", ex)
        }
    }

    private fun readPrivateBackupFile(): String? {
        val backupDir = privateBackupDirectory()
        if (!backupDir.exists() || !backupDir.isDirectory) return null
        val backupFiles = backupDir.listFiles()
            ?.filter { it.isFile && isLikelyBackupFileName(it.name) }
            ?.sortedByDescending { parseBackupTimeFromFileName(it.name).coerceAtLeast(it.lastModified()) }
            ?: return null
        for (file in backupFiles) {
            val text = try {
                file.readText()
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Internes Backup konnte nicht gelesen werden: ${file.absolutePath}", ex)
                null
            }
            if (text != null && parseState(text) != null) return text
        }
        return null
    }

    private fun trimPrivateBackupHistory() {
        val backupDir = privateBackupDirectory()
        if (!backupDir.exists() || !backupDir.isDirectory) return
        val backupFiles = backupDir.listFiles()
            ?.filter { it.isFile && isLikelyBackupFileName(it.name) }
            ?.sortedByDescending { parseBackupTimeFromFileName(it.name).coerceAtLeast(it.lastModified()) }
            ?: return
        backupFiles.drop(MAX_BACKUP_FILES).forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun privateBackupDirectory(): File = File(context.filesDir, BACKUP_SUBFOLDER)

    private fun readFromMediaStoreBackup(): String? {
        val backupUris = findBackupUris()
        Log.i(LOG_TAG, "Gefundene Backup-Einträge im MediaStore: ${backupUris.size}")
        for (uri in backupUris) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input).readText()
                }
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Fehler beim Lesen einer Backup-Datei", ex)
                null
            }
            if (text == null) continue
            val parsedState = parseState(text)
            if (parsedState != null && parsedState.pages.isNotEmpty()) {
                Log.i(LOG_TAG, "Backup wiederhergestellt (mit Seiten): $uri")
                return text
            }
            Log.w(LOG_TAG, "Backup JSON ohne Seiten übersehen: $uri")
        }
        for (uri in backupUris) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input).readText()
                }
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Fehler beim Lesen einer Backup-Datei", ex)
                null
            }
            if (text != null && parseState(text) != null) {
                Log.i(LOG_TAG, "Backup wiederhergestellt (Fallback): $uri")
                return text
            }
        }
        return null
    }

    private fun readPublicBackupFile(): String? {
        val backupFiles = publicBackupFiles()
        if (backupFiles.isEmpty()) return null

        for (file in backupFiles) {
            val text = try {
                file.readText()
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Direkter Lesezugriff auf Backup-Datei fehlgeschlagen: ${file.absolutePath}", ex)
                null
            }
            if (text.isNullOrBlank()) continue
            if (parseState(text) != null) {
                Log.i(LOG_TAG, "Backup direkt aus Datei wiederhergestellt: ${file.absolutePath}")
                return text
            }
        }
        return null
    }

    private fun clearBackupFile() {
        clearPrivateBackupFiles()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uris = findBackupUris()
                for (uri in uris) {
                    context.contentResolver.delete(uri, null, null)
                }
                clearPublicBackupFiles()
                return
            }
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Fehler beim Löschen des Backup-Exports", ex)
        }
        clearPublicBackupFiles()
        clearLegacyBackup()
    }

    private fun clearPrivateBackupFiles() {
        val backupDir = privateBackupDirectory()
        if (!backupDir.exists() || !backupDir.isDirectory) return
        backupDir.listFiles()
            ?.filter { it.isFile && isLikelyBackupFileName(it.name) }
            ?.forEach { file -> runCatching { file.delete() } }
    }

    private fun ensureBackupUri(fileName: String): Uri? {
        val existing = findBackupUri(fileName)
        if (existing != null) return existing

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val collection = backupCollectionUri() ?: return null
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_FILE_MIME)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_SUBFOLDER"
                    )
                }
                context.contentResolver.insert(collection, values)
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Backup-Datei konnte nicht angelegt werden", ex)
                null
            }
        } else {
            null
        }
    }

    private fun findBackupUri(fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            discoverMediaStoreBackups().firstOrNull { it.fileName == fileName }?.uri
        } else {
            null
        }
    }

    private data class MediaStoreBackupCandidate(
        val uri: Uri,
        val fileName: String,
        val sortTime: Long,
    )

    private fun discoverMediaStoreBackups(): List<MediaStoreBackupCandidate> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
            val candidates = mutableListOf<MediaStoreBackupCandidate>()

            for (collection in backupCollectionUris()) {
                context.contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                    arrayOf("%$BACKUP_SUBFOLDER%"),
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val candidateName = cursor.getString(displayNameIndex).orEmpty()
                        if (!isLikelyBackupFileName(candidateName)) continue

                        val rawDate = cursor.getLong(dateIndex)
                        val normalizedDate = when {
                            rawDate <= 0L -> 0L
                            rawDate > 10_000_000_000L -> rawDate
                            else -> rawDate * 1000L
                        }
                        val sortTime = parseBackupTimeFromFileName(candidateName).coerceAtLeast(normalizedDate)
                        val uri = ContentUris.withAppendedId(collection, cursor.getLong(idIndex))
                        Log.i(
                            LOG_TAG,
                            "Backup-Kandidat gefunden: $candidateName " +
                                "uri=$uri date=${cursor.getLong(dateIndex)} sort=$sortTime"
                        )
                        candidates.add(MediaStoreBackupCandidate(uri, candidateName, sortTime))
                    }
                }
            }
            candidates
                .groupBy { it.fileName }
                .mapValues { (_, values) -> values.maxByOrNull { it.sortTime }!! }
                .values
                .sortedWith(
                    compareByDescending<MediaStoreBackupCandidate> { it.sortTime }
                        .thenByDescending { it.fileName }
                )
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Backup-Dateien im MediaStore nicht auffindbar", ex)
            emptyList()
        }
    }

    private fun findBackupUris(): List<Uri> = discoverMediaStoreBackups().map { it.uri }.distinct()

    private fun isLikelyBackupFileName(fileName: String): Boolean {
        if (currentBackupFilePrefixCandidates().any { prefix ->
                fileName == "$prefix.json"
            }
        ) return true
        if (BACKUP_VERSIONED_FILE_NAME.matchEntire(fileName) != null) return true

        return currentBackupFilePrefixCandidates().any { prefix ->
            fileName.startsWith("$prefix-")
                || fileName.startsWith("${prefix}-v")
        }
    }

    private fun parseBackupTimeFromFileName(fileName: String): Long {
        val match = BACKUP_VERSIONED_FILE_NAME.matchEntire(fileName) ?: return -1L
        return match.groupValues.getOrNull(4)?.toLongOrNull() ?: -1L
    }

    private fun backupCollectionUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            null
        }

    private fun backupCollectionUris(): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            )
        } else {
            emptyList()
        }

    private fun trimMediaStoreBackupHistory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val candidates = discoverMediaStoreBackups()
        if (candidates.size <= MAX_BACKUP_FILES) return
        for (old in candidates.drop(MAX_BACKUP_FILES)) {
            try {
                context.contentResolver.delete(old.uri, null, null)
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Ältere MediaStore-Backups konnten nicht gelöscht werden: ${old.uri}", ex)
            }
        }
    }

    private fun persistBackupInLegacyFile(json: String, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        try {
            val legacyDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return
            val folder = File(legacyDir, BACKUP_SUBFOLDER)
            if (!folder.exists() && !folder.mkdirs()) return

            val targets = listOf(
                File(folder, fileName),
                File(folder, BACKUP_FILE_NAME_LEGACY),
                File(folder, BACKUP_FILE_NAME_LEGACY_CANONICAL),
            ).distinctBy { it.absolutePath }
            for (target in targets) {
                target.writeText(json)
            }
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Legacy-Backup nicht möglich", ex)
        }
    }

    private fun persistBackupInPublicFolder(json: String, fileName: String): Boolean {
        return try {
            val backupDir = publicBackupDirectory() ?: return false
            if (!backupDir.exists() && !backupDir.mkdirs()) return false
            File(backupDir, fileName).writeText(json)
            true
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Backup-Text konnte nicht direkt gespeichert werden", ex)
            false
        }
    }

    private fun publicBackupDirectory(): File? {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloads, BACKUP_SUBFOLDER)
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Öffentliches Backup-Verzeichnis konnte nicht ermittelt werden", ex)
            null
        }
    }

    private fun parseBackupTime(file: File): Long =
        parseBackupTimeFromFileName(file.name).let { if (it > 0L) it else file.lastModified() }

    private fun publicBackupFiles(): List<File> {
        val backupDir = publicBackupDirectory() ?: return emptyList()
        if (!backupDir.exists() || !backupDir.isDirectory) return emptyList()
        return backupDir.listFiles()
            ?.asSequence()
            ?.filter { isLikelyBackupFileName(it.name) }
            ?.sortedWith(
                compareByDescending<File> { parseBackupTime(it) }
                    .thenByDescending { it.lastModified() }
            )
            ?.toList()
            ?: emptyList()
    }

    private fun legacyBackupFiles(legacyDir: File): List<File> {
        return File(legacyDir, BACKUP_SUBFOLDER)
            .takeIf { it.exists() && it.isDirectory }
            ?.listFiles()
            ?.asSequence()
            ?.filter { isLikelyBackupFileName(it.name) }
            ?.sortedWith(
                compareByDescending<File> { parseBackupTime(it) }
                    .thenByDescending { it.lastModified() }
            )
            ?.toList()
            ?: emptyList()
    }

    private fun trimPublicBackupHistory() {
        val files = publicBackupFiles()
        if (files.size <= MAX_BACKUP_FILES) return
        for (file in files.drop(MAX_BACKUP_FILES)) {
            try {
                file.delete()
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Öffentliche Backup-Datei konnte nicht gelöscht werden: ${file.absolutePath}", ex)
            }
        }
    }

    private fun clearPublicBackupFiles() {
        publicBackupFiles().forEach { file ->
            try {
                file.delete()
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Öffentliche Backup-Datei konnte nicht gelöscht werden: ${file.absolutePath}", ex)
            }
        }
    }

    private fun readLegacyBackup(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            null
        } else {
            try {
                val legacyDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
                legacyBackupFiles(legacyDir).firstOrNull()?.readText()
            } catch (ex: Exception) {
                Log.w(LOG_TAG, "Legacy-Backup nicht lesbar", ex)
                null
            }
        }
    }

    private fun clearLegacyBackup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        try {
            val legacyDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return
            legacyBackupFiles(legacyDir).forEach { it.delete() }
        } catch (ex: Exception) {
            Log.w(LOG_TAG, "Legacy-Backup konnte nicht entfernt werden", ex)
        }
    }
}
