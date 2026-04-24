package com.seafox.nmea_dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.DownloadManager
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.SystemClock
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import android.speech.tts.TextToSpeech
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.database.Cursor
import android.net.Uri
import java.net.UnknownHostException
import android.util.Log
import android.util.Base64
import java.util.UUID
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Typography
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import com.seafox.nmea_dashboard.data.DashboardPage
import com.seafox.nmea_dashboard.data.DashboardWidget
import com.seafox.nmea_dashboard.data.DashboardState
import com.seafox.nmea_dashboard.data.BoatProfile
import com.seafox.nmea_dashboard.data.BoatType
import com.seafox.nmea_dashboard.data.NmeaSourceProfile
import com.seafox.nmea_dashboard.data.NmeaPgnHistoryEntry
import com.seafox.nmea_dashboard.data.Nmea0183HistoryEntry
import com.seafox.nmea_dashboard.data.AutopilotControlMode
import com.seafox.nmea_dashboard.data.AutopilotDispatchRequest
import com.seafox.nmea_dashboard.data.AutopilotTargetDevice
import com.seafox.nmea_dashboard.data.AutopilotGatewayBackend
import com.seafox.nmea_dashboard.data.BackupPrivacyMode
import com.seafox.nmea_dashboard.data.DEFAULT_NMEA_ROUTER_HOST
import com.seafox.nmea_dashboard.data.WidgetFrameStyle
import com.seafox.nmea_dashboard.data.DashboardLayoutOrientation
import com.seafox.nmea_dashboard.data.UiFont
import com.seafox.nmea_dashboard.data.WidgetKind
import com.seafox.nmea_dashboard.data.SerializedRoute
import com.seafox.nmea_dashboard.data.SerializedWaypoint
import com.seafox.nmea_dashboard.data.NmeaRouterProtocol
import com.seafox.nmea_dashboard.data.autopilotProtocolHint
import com.seafox.nmea_dashboard.data.autopilotGatewayHint
import com.seafox.nmea_dashboard.data.widgetHelpLines
import com.seafox.nmea_dashboard.data.AutopilotWidgetSettings
import com.seafox.nmea_dashboard.data.widgetCatalogSections
import com.seafox.nmea_dashboard.data.buildAutopilotProtocolCommand
import com.seafox.nmea_dashboard.ui.MENU_SPACING
import com.seafox.nmea_dashboard.ui.DashboardViewModel
import com.seafox.nmea_dashboard.ui.widgets.AutopilotWidget
import com.seafox.nmea_dashboard.ui.widgets.BatteryChemistry
import com.seafox.nmea_dashboard.ui.widgets.BatteryWidget
import com.seafox.nmea_dashboard.ui.widgets.BatteryWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.BlackWaterWidget
import com.seafox.nmea_dashboard.ui.widgets.DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS
import com.seafox.nmea_dashboard.ui.widgets.DEFAULT_ANCHOR_WATCH_TOLERANCE_PERCENT
import com.seafox.nmea_dashboard.ui.widgets.DEFAULT_TACKING_ANGLE_DEG
import com.seafox.nmea_dashboard.ui.widgets.CompassWidget
import com.seafox.nmea_dashboard.ui.widgets.DalyBmsWidget
import com.seafox.nmea_dashboard.ui.widgets.AisWidget
import com.seafox.nmea_dashboard.ui.widgets.AisTargetData
import com.seafox.nmea_dashboard.ui.widgets.AisTargetWithAge
import com.seafox.nmea_dashboard.ui.widgets.AisWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.AnchorWatchTelemetry
import com.seafox.nmea_dashboard.ui.widgets.AnchorWatchDistanceUnit
import com.seafox.nmea_dashboard.ui.widgets.AnchorWatchChainUnit
import com.seafox.nmea_dashboard.ui.widgets.AnchorWatchWidget
import com.seafox.nmea_dashboard.ui.widgets.AnchorWatchWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.anchorWatchChainStrengthOptions
import com.seafox.nmea_dashboard.ui.widgets.anchorWatchNormalizeChainStrengthLabel
import com.seafox.nmea_dashboard.ui.widgets.EchosounderDepthUnit
import com.seafox.nmea_dashboard.ui.widgets.EchosounderWidget
import com.seafox.nmea_dashboard.ui.widgets.EchosounderWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.ECHO_SOUNDER_TONES
import com.seafox.nmea_dashboard.ui.widgets.playWidgetAlarmTone
import com.seafox.nmea_dashboard.ui.widgets.EngineRpmWidget
import com.seafox.nmea_dashboard.ui.widgets.GpsWidget
import com.seafox.nmea_dashboard.ui.widgets.GpsWidgetData
import com.seafox.nmea_dashboard.ui.widgets.NmeaPgnWidget
import com.seafox.nmea_dashboard.ui.widgets.Nmea0183Widget
import com.seafox.nmea_dashboard.ui.widgets.LogWidget
import com.seafox.nmea_dashboard.ui.widgets.LogWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.LogWidgetSpeedSource
import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import com.seafox.nmea_dashboard.ui.widgets.SeaChartSpeedSource
import com.seafox.nmea_dashboard.ui.widgets.SeaChartWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.chart.ChartWidget
import com.seafox.nmea_dashboard.ui.widgets.chart.ChartProviderRegistry
import com.seafox.nmea_dashboard.ui.widgets.chart.Catalog031Parser
import com.seafox.nmea_dashboard.ui.widgets.chart.GeoBounds
import com.seafox.nmea_dashboard.ui.widgets.chart.NauticalOverlayOptions
import com.seafox.nmea_dashboard.ui.widgets.chart.NavigationVectorSettings
import com.seafox.nmea_dashboard.ui.widgets.chart.Route
import com.seafox.nmea_dashboard.ui.widgets.GreyWaterWidget
import com.seafox.nmea_dashboard.ui.widgets.WaterTankWidget
import com.seafox.nmea_dashboard.ui.widgets.TemperatureSensor
import com.seafox.nmea_dashboard.ui.widgets.TemperatureUnit
import com.seafox.nmea_dashboard.ui.widgets.TemperatureWidget
import com.seafox.nmea_dashboard.ui.widgets.TemperatureWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.WindSpeedUnit
import com.seafox.nmea_dashboard.ui.widgets.WindWidget
import com.seafox.nmea_dashboard.ui.widgets.WindWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.SystemWidget
import com.seafox.nmea_dashboard.ui.widgets.SystemWidgetLoadEntry
import com.seafox.nmea_dashboard.ui.widgets.SystemWidgetLoadSnapshot
import com.seafox.nmea_dashboard.ui.widgets.TEMPERATURE_SENSOR_COUNT
import com.seafox.nmea_dashboard.ui.widgets.normalizeTemperatureSensorNames
import com.seafox.nmea_dashboard.ui.widgets.pickTemperatureSensorValue
import com.seafox.nmea_dashboard.ui.widgets.parseAnchorWatchWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeAnchorWatchWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseAisWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeAisWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseAutopilotWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeAutopilotWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseBatteryWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeBatteryWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseEchosounderWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeEchosounderWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseLogWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeLogWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseSeaChartWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeSeaChartWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseWindWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeWindWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.parseTemperatureWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.serializeTemperatureWidgetSettings
import com.seafox.nmea_dashboard.ui.widgets.pickValue
import com.seafox.nmea_dashboard.ui.widgets.pickValueOrNull
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.floor
import java.net.URL
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import java.util.zip.ZipFile
import java.util.zip.CRC32
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.coroutines.coroutineContext
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private val LocalMinimumTouchTargetEnforcement = staticCompositionLocalOf { true }

private data class AlarmVoiceProfile(
    val label: String,
    val pitch: Float,
    val rate: Float,
)

private val ALARM_VOICE_PROFILES = listOf(
    AlarmVoiceProfile("weiblich 1", 1.20f, 0.96f),
    AlarmVoiceProfile("weiblich 2", 1.35f, 1.00f),
    AlarmVoiceProfile("männlich 1", 0.88f, 0.96f),
    AlarmVoiceProfile("männlich 2", 0.74f, 1.02f),
)

private const val ALARM_TEST_CLICK_GUARD_MS = 900L
private val alarmTestClickLock = Any()
private var lastAlarmTestClickAtMs = 0L

private fun runAlarmTestOnce(block: () -> Unit) {
    val now = System.currentTimeMillis()
    synchronized(alarmTestClickLock) {
        if (now - lastAlarmTestClickAtMs < ALARM_TEST_CLICK_GUARD_MS) {
            return
        }
        lastAlarmTestClickAtMs = now
    }
    block()
}

private class AnchorWatchAlarmAnnouncer(context: Context) {
    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var initialized = false

    init {
        textToSpeech = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                textToSpeech?.language = Locale.getDefault()
            } else {
                initialized = false
            }
        }
    }

    fun announce(
        widgetName: String,
        enabled: Boolean,
        profileIndex: Int,
    ) {
        if (!initialized || !enabled) return
        val sanitizedName = widgetName.trim().ifBlank { "Alarm" }
        val hasAnchorWatchName = sanitizedName.contains("anker", ignoreCase = true)
        val strippedAnchorName = if (hasAnchorWatchName) {
            sanitizedName
                .replaceFirst(
                    Regex("^\\s*(?i)(Ankerwächter|Ankerwache)\\s*"),
                    "",
                )
                .trim()
                .ifBlank { "Ankerwache" }
        } else {
            sanitizedName
        }
        val message = if (hasAnchorWatchName) {
            "Alarm $strippedAnchorName"
        } else {
            "Alarm $sanitizedName"
        }
        val profile = ALARM_VOICE_PROFILES.getOrNull(profileIndex.coerceIn(0, ALARM_VOICE_PROFILES.lastIndex))
            ?: ALARM_VOICE_PROFILES.first()
        textToSpeech?.setPitch(profile.pitch)
        textToSpeech?.setSpeechRate(profile.rate)
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "anchorWatchAlarm")
    }

    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        initialized = false
    }
}

private const val SYSTEM_PERFORMANCE_SAMPLE_INTERVAL_MS = 1400L

private fun normalizeChartWidgetKind(kind: WidgetKind): WidgetKind = when (kind) {
    WidgetKind.SEA_CHART,
    WidgetKind.SEA_CHART_PIXEL -> WidgetKind.KARTEN
    else -> kind
}

private fun calculateSystemWidgetLoadWeight(kind: WidgetKind): Float = when (normalizeChartWidgetKind(kind)) {
    WidgetKind.BATTERY -> 7f
    WidgetKind.WATER_TANK -> 4f
    WidgetKind.BLACK_WATER_TANK -> 4f
    WidgetKind.GREY_WATER_TANK -> 4f
    WidgetKind.WIND -> 8f
    WidgetKind.COMPASS -> 4f
    WidgetKind.SEA_CHART, WidgetKind.SEA_CHART_PIXEL -> 18f
    WidgetKind.KARTEN -> 18f
    WidgetKind.GPS -> 4f
    WidgetKind.TEMPERATURE -> 5f
    WidgetKind.AIS -> 12f
    WidgetKind.DALY_BMS -> 8f
    WidgetKind.AUTOPILOT -> 10f
    WidgetKind.ENGINE_RPM -> 3f
    WidgetKind.ECHOSOUNDER -> 6f
    WidgetKind.LOG -> 5f
    WidgetKind.ANCHOR_WATCH -> 7f
    WidgetKind.NMEA_PGN -> 3f
    WidgetKind.NMEA0183 -> 4f
    WidgetKind.SYSTEM_PERFORMANCE -> 0f
}

private fun isLegacySeaChartWidgetKind(kind: WidgetKind): Boolean =
    kind == WidgetKind.SEA_CHART || kind == WidgetKind.SEA_CHART_PIXEL

private fun isSeaChartWidgetKind(kind: WidgetKind): Boolean =
    isLegacySeaChartWidgetKind(kind) || kind == WidgetKind.KARTEN

private fun estimateSystemWidgetLoadEntries(widgets: List<DashboardWidget>): List<SystemWidgetLoadEntry> {
    val weighted = widgets
        .asSequence()
        .filter { it.kind != WidgetKind.SYSTEM_PERFORMANCE }
        .map { widget ->
            val baseWeight = calculateSystemWidgetLoadWeight(widget.kind)
            val areaFactor = ((widget.widthPx * widget.heightPx) / 40000f).coerceIn(0.45f, 1.9f)
            widget to (baseWeight * areaFactor)
        }
        .mapNotNull { (widget, rawWeight) ->
            val percent = rawWeight.toFloat()
            if (percent <= 0f) return@mapNotNull null
            SystemWidgetLoadEntry(
                widgetId = widget.id,
                widgetTitle = widget.title.ifBlank { widgetDefaultTitleFromKind(widget.kind) },
                widgetKindLabel = widgetKindUiLabel(widget.kind),
                estimatedLoadPercent = percent,
            )
        }
        .toList()

    val total = weighted.sumOf { it.estimatedLoadPercent.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    return weighted
        .map { entry ->
            entry.copy(estimatedLoadPercent = (entry.estimatedLoadPercent / total) * 100f)
        }
        .sortedByDescending { it.estimatedLoadPercent }
}

private fun widgetKindUiLabel(kind: WidgetKind): String = when (normalizeChartWidgetKind(kind)) {
    WidgetKind.KARTEN -> "Karten"
    WidgetKind.SYSTEM_PERFORMANCE -> "System"
    else -> kind.name.replace("_", " ")
}

private fun widgetDefaultTitleFromKind(kind: WidgetKind): String = when (normalizeChartWidgetKind(kind)) {
    WidgetKind.BATTERY -> "Batterie"
    WidgetKind.WATER_TANK -> "Wassertank"
    WidgetKind.BLACK_WATER_TANK -> "Schwarzwasser"
    WidgetKind.GREY_WATER_TANK -> "Grauwasser"
    WidgetKind.WIND -> "Wind"
    WidgetKind.COMPASS -> "Kompass"
    WidgetKind.SEA_CHART, WidgetKind.SEA_CHART_PIXEL -> "Karten"
    WidgetKind.KARTEN -> "Karten"
    WidgetKind.GPS -> "GPS"
    WidgetKind.TEMPERATURE -> "Temperatur"
    WidgetKind.AIS -> "AIS"
    WidgetKind.DALY_BMS -> "DALY BMS"
    WidgetKind.AUTOPILOT -> "Autopilot"
    WidgetKind.ENGINE_RPM -> "Motordrehzahl"
    WidgetKind.ECHOSOUNDER -> "Deep"
    WidgetKind.LOG -> "Speed"
    WidgetKind.ANCHOR_WATCH -> "Ankerwache"
    WidgetKind.NMEA_PGN -> "PGN Empfang"
    WidgetKind.NMEA0183 -> "NMEA0183 Empfang"
    WidgetKind.SYSTEM_PERFORMANCE -> "System"
}

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var tabletLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var dalyBlePermissionLauncher: ActivityResultLauncher<Array<String>>
    private val userWarningDialogState = mutableStateOf<Pair<String, String>?>(null)

    private fun postUserWarning(
        message: String,
        title: String = "Warnung",
    ) {
        runOnUiThread {
            userWarningDialogState.value = title to message
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureSeaChartInternalDirectoryStructure(this)
        tabletLocationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                viewModel.refreshTabletGpsLocationSource()
            }
        }
        dalyBlePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            if (!hasRequiredDalyBlePermissions()) {
                userWarningDialogState.value = "Warnung" to "DALY-BMS Berechtigungen fehlen. Bitte in den App-Einstellungen aktivieren."
            }
        }
        requestTabletLocationPermission()
        // DALY-BMS-Verbindungen sind deaktiviert.

        setContent {
            val state by viewModel.state.collectAsState()
            val data by viewModel.telemetry.collectAsState()
            val textData by viewModel.telemetryText.collectAsState()
            val aisData by viewModel.aisTelemetry.collectAsState()
            val aisTextData by viewModel.aisTelemetryText.collectAsState()
            val recentNmeaPgnHistory by viewModel.recentNmeaPgnHistory.collectAsState()
            val recentNmea0183History by viewModel.recentNmea0183History.collectAsState()
            val dalyDebugEvents by viewModel.dalyDebugEvents.collectAsState()
            val pendingSourceNotice by viewModel.pendingNmeaSourceNotice.collectAsState()
            val pendingNmea0183Classification by viewModel.pendingNmea0183Classification.collectAsState()
            val pagerState = rememberPagerState(initialPage = state.selectedPage) { state.pages.size }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val anchorWatchAlarmAnnouncer = remember(context) { AnchorWatchAlarmAnnouncer(context) }
            var dashboardPageWidthPx by remember { mutableFloatStateOf(0f) }
            var dashboardPageHeightPx by remember { mutableFloatStateOf(0f) }
            var dashboardGridStepPx by remember { mutableFloatStateOf(0f) }
            val userWarningState = userWarningDialogState.value
            val dismissUserWarning = { userWarningDialogState.value = null }
            val showUserWarning: (String, String) -> Unit = { message, title ->
                postUserWarning(message = message, title = title)
            }
            var showPendingNmeaSourceNotice by remember { mutableStateOf(false) }
            val activePendingSource = state.detectedNmeaSources.firstOrNull {
                it.sourceKey.equals(pendingSourceNotice, ignoreCase = true)
            }
            var pendingSourceName by remember(pendingSourceNotice) { mutableStateOf(activePendingSource?.displayName ?: "") }
            var pendingNmea0183Category by remember(pendingNmea0183Classification?.sentence) {
                mutableStateOf("OTHER")
            }

            LaunchedEffect(pendingSourceNotice) {
                if (pendingSourceNotice != null) {
                    showPendingNmeaSourceNotice = true
                    pendingSourceName = activePendingSource?.displayName ?: ""
                }
            }

            LaunchedEffect(pendingNmea0183Classification?.sentence) {
                pendingNmea0183Category = "OTHER"
            }

            LaunchedEffect(state.layoutOrientation) {
                requestedOrientation = when (state.layoutOrientation) {
                    DashboardLayoutOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    DashboardLayoutOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }

            DisposableEffect(Unit) {
                onDispose { anchorWatchAlarmAnnouncer.release() }
            }

            val showWidgetAddError = { message: String ->
                showUserWarning(message, "Warnung")
            }
            val triggerAnchorWatchAlarmTest = {
                runAlarmTestOnce {
                    applySystemAlarmPresetVolume(
                        context = context,
                        preset = state.alarmToneVolume,
                    )
                    anchorWatchAlarmAnnouncer.announce(
                        widgetName = "Ankerwächter Test",
                        enabled = state.alarmVoiceAnnouncementsEnabled,
                        profileIndex = state.alarmVoiceProfileIndex,
                    )
                    val toneFrequency = ECHO_SOUNDER_TONES.firstOrNull()?.frequencyHz ?: 1000
                    playWidgetAlarmTone(
                        frequencyHz = toneFrequency,
                        volume = state.alarmToneVolume,
                    )
                }
            }
            val triggerAlarmSettingsTest = { volume: Float, voiceEnabled: Boolean, profileIndex: Int ->
                runAlarmTestOnce {
                    val normalizedVolume = volume.coerceIn(0f, 2f)
                    applySystemAlarmPresetVolume(
                        context = context,
                        preset = normalizedVolume,
                    )
                    anchorWatchAlarmAnnouncer.announce(
                        widgetName = "Alarmeinstellungen Test",
                        enabled = voiceEnabled,
                        profileIndex = profileIndex,
                    )
                    val toneFrequency = ECHO_SOUNDER_TONES.firstOrNull()?.frequencyHz ?: 1000
                    playWidgetAlarmTone(
                        frequencyHz = toneFrequency,
                        volume = normalizedVolume,
                    )
                }
            }

            LaunchedEffect(state.selectedPage) {
                if (pagerState.pageCount > 0 && state.selectedPage != pagerState.currentPage) {
                    pagerState.scrollToPage(state.selectedPage)
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                snapshotFlow { pagerState.currentPage }
                    .collect { index ->
                        if (state.selectedPage != index) {
                            viewModel.selectPage(index)
                        }
                    }
            }

            val menuTypography = createModernTypography(state.uiFont)
            val menuInputFieldTextStyle = menuTypography.titleMedium.copy(
                fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp,
                lineHeight = SeaFoxDesignTokens.Size.menuLineHeight,
                color = if (state.darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
            )

            MaterialTheme(
                colorScheme = seaFoxColorScheme(state.darkBackground),
                typography = menuTypography,
            ) {
                val noticeTextStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp,
                    lineHeight = SeaFoxDesignTokens.Size.menuLineHeight,
                    color = Color.White,
                )
                if (!state.onboardingCompleted && userWarningState == null) {
                    CompactMenuDialog(
                        onDismissRequest = {},
                        isDarkMenu = true,
                        title = { Text("seaFOX Erststart", style = noticeTextStyle) },
                        text = {
                            CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                ProvideTextStyle(value = noticeTextStyle) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                        Text("Safety: seaFOX ist ein Marine-Assistent, kein zertifiziertes ECDIS.")
                                        Text("Autopilot: Kommandos bleiben gesperrt, bis das Safety Gate bewusst armiert und jeder Befehl bestätigt wird.")
                                        Text("Datenschutz: Backups bleiben standardmäßig im privaten App-Speicher; MMSI, Route, MOB und Router-Hosts gelten als sensibel.")
                                        Text("Datenquelle: Nutze Live-NMEA über WLAN Router oder starte zuerst den Simulator.")
                                        Text("Karten: C-Map/S-63 bleiben verborgen bzw. gesperrt, bis Lizenz und Implementierung wirklich bereit sind.")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            CompactMenuTextButton(
                                text = "Verstanden",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = { viewModel.completeOnboarding() },
                            )
                        },
                        dismissButton = {
                            CompactMenuTextButton(
                                text = "Simulator starten",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = {
                                    if (!state.simulationEnabled) {
                                        viewModel.toggleRouterSimulation()
                                    }
                                    viewModel.completeOnboarding()
                                },
                            )
                        },
                    )
                }
                if (userWarningState != null) {
                    AlertDialog(
                        onDismissRequest = {},
                        containerColor = SeaFoxDesignTokens.Color.surfaceRaisedDark,
                        titleContentColor = Color.White,
                        textContentColor = Color.White,
                        title = { Text(userWarningState.first, style = noticeTextStyle) },
                        text = { Text(userWarningState.second, style = noticeTextStyle) },
                        confirmButton = {
                            CompactMenuTextButton(
                                text = "OK",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = { dismissUserWarning() },
                            )
                        },
                    )
                }
                if (showPendingNmeaSourceNotice && pendingSourceNotice != null) {
                    val pendingSourceKey = pendingSourceNotice.orEmpty()
                    val detected = activePendingSource
                val pgnList = detected
                        ?.pgns
                        ?.sorted()
                        ?.joinToString(", ") { sourcePgnLine(it) }
                        ?.ifBlank { "unbekannt" }
                        ?: "keine PGN-Daten"
                    val recognized = detected?.let { sourceDetectedStatus(it) } ?: "unbekannt"
                    val sourceKeyLabel = detected?.sourceKey ?: pendingSourceKey
                    CompactMenuDialog(
                        onDismissRequest = {
                            showPendingNmeaSourceNotice = false
                            viewModel.clearPendingNmeaSourceNotice()
                        },
                        isDarkMenu = state.darkBackground,
                        title = { Text("Neue Quelle erkannt", style = noticeTextStyle) },
                        text = {
                            CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                ProvideTextStyle(value = noticeTextStyle) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                        Text("Quelle: $sourceKeyLabel")
                                        Text("Status: $recognized")
                                        Text("PGN: $pgnList")
                if (detected != null) {
                            MenuCompactTextField(
                                value = pendingSourceName,
                                onValueChange = { pendingSourceName = it },
                                label = { Text("Benennung / Zuordnung", style = noticeTextStyle) },
                                singleLine = true,
                                    modifier = Modifier
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                    textStyle = menuInputFieldTextStyle,

                        colors = menuOutlinedTextFieldColors(state.darkBackground),
                        shape = MENU_INPUT_FIELD_SHAPE
                    )
                }
                                        if (detected == null) {
                                            Text("Neue Quelle wurde nicht gespeichert.")
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            CompactMenuTextButton(
                                text = "Zuordnung speichern",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = {
                                    if (detected != null) {
                                        viewModel.updateNmeaSourceDisplayName(pendingSourceKey, pendingSourceName)
                                    }
                                    showPendingNmeaSourceNotice = false
                                    viewModel.clearPendingNmeaSourceNotice()
                                }
                            )
                        },
                        dismissButton = {
                            CompactMenuTextButton(
                                text = "Später",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = {
                                    showPendingNmeaSourceNotice = false
                                    viewModel.clearPendingNmeaSourceNotice()
                                }
                            )
                        },
                    )
                }
                if (pendingNmea0183Classification != null && !showPendingNmeaSourceNotice) {
                    pendingNmea0183Classification?.let { pending ->
                    CompactMenuDialog(
                        onDismissRequest = {
                            pendingNmea0183Category = "OTHER"
                            viewModel.postponeNmea0183ClassificationPrompt(pending.sentence)
                        },
                        isDarkMenu = state.darkBackground,
                        title = { Text("NMEA0183 Satz klassifizieren", style = noticeTextStyle) },
                        text = {
                            CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                ProvideTextStyle(value = noticeTextStyle) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                        Text("Satz: ${pending.sentence}")
                                        Text("Rohdaten: ${pending.rawLine}")
                                        Text("Kategorie auswählen")
                                        OptionSelectorRow(
                                            selectedOption = pendingNmea0183Category,
                                            options = viewModel.availableNmea0183SentenceCategoryOptions,
                                            optionLabel = { it },
                                            onOptionSelected = { pendingNmea0183Category = it },
                                            style = noticeTextStyle,
                                            enabled = true,
                                            darkBackground = state.darkBackground,
                                            triggerThreshold = 4,
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            CompactMenuTextButton(
                                text = "Übernehmen",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = {
                                    viewModel.applyNmea0183Classification(
                                        sentence = pending.sentence,
                                        category = pendingNmea0183Category,
                                    )
                                    pendingNmea0183Category = "OTHER"
                                }
                            )
                        },
                        dismissButton = {
                            CompactMenuTextButton(
                                text = "Später",
                                style = noticeTextStyle,
                                fillWidth = false,
                                onClick = {
                                    viewModel.postponeNmea0183ClassificationPrompt(pending.sentence)
                                    pendingNmea0183Category = "OTHER"
                                }
                            )
                        },
                    )
                }
                }
                Scaffold(topBar = {
                        DashboardTopBar(
                state = state,
                darkBackground = state.darkBackground,
                onSelectPage = { index ->
                            scope.launch { pagerState.scrollToPage(index) }
                        },
                        onAddPage = viewModel::addPage,
                        onRenamePage = { index, name -> viewModel.renamePage(index, name) },
                        onAddWidget = { kind, _, _, _ ->
                            val success = viewModel.addWidget(
                                kind = kind,
                                pageWidthPx = dashboardPageWidthPx,
                                pageHeightPx = dashboardPageHeightPx,
                                gridStepPx = dashboardGridStepPx,
                            )
                            if (!success) {
                                showWidgetAddError("Nicht genug Platz für ein neues Widget.")
                            }
                            success
                        },
                        onAddWidgetError = showWidgetAddError,
                        onToggleBackground = viewModel::toggleBackgroundColor,
                        onToggleRouterSimulation = viewModel::toggleRouterSimulation,
                        onUpdateUiFont = viewModel::updateUiFont,
                        onUpdateFontScale = viewModel::updateFontScale,
                        onUpdateGridStepPercent = viewModel::updateGridStepPercent,
                        onUpdateLayoutOrientation = viewModel::updateLayoutOrientation,
                        onUpdateAlarmToneVolume = viewModel::updateAlarmToneVolume,
                        onUpdateAlarmRepeatIntervalSeconds = viewModel::updateAlarmRepeatIntervalSeconds,
                        onUpdateAlarmVoiceAnnouncementsEnabled = viewModel::updateAlarmVoiceAnnouncementsEnabled,
                        onUpdateAlarmVoiceProfileIndex = viewModel::updateAlarmVoiceProfileIndex,
                        onUpdateWidgetFrameStyle = viewModel::updateWidgetFrameStyle,
                        onUpdateWidgetFrameStyleGrayOffset = viewModel::updateWidgetFrameStyleGrayOffset,
                        onUpdateNmeaRouter = viewModel::updateNmeaRouter,
                        onUpdateNmeaSourceDisplayName = viewModel::updateNmeaSourceDisplayName,
                        onClearNmeaSourceDisplayName = viewModel::clearNmeaSourceDisplayName,
                        onRemoveNmeaSourceProfile = viewModel::removeNmeaSourceProfile,
                        onUpdateBoatProfile = viewModel::updateBoatProfile,
                        onUpdateBackupPrivacyMode = viewModel::updateBackupPrivacyMode,
                        onUpdateBootAutostartEnabled = viewModel::updateBootAutostartEnabled,
                        onUpdateRouterSimulationOrigin = viewModel::updateRouterSimulationOrigin,
                        onClearStoredData = viewModel::clearAllStoredData,
                        onRestoreStoredData = { viewModel.restoreStoredData() },
                        onLoadReleaseNotes = { viewModel.loadReleaseNotes() },
                        onLoadReleaseNoteCount = { viewModel.loadReleaseNoteCount() },
                        onExitApp = { finish() },
                        onTriggerAnchorWatchAlarm = triggerAnchorWatchAlarmTest,
                        onTriggerAlarmSettingsTest = triggerAlarmSettingsTest,
                    )
}) { padding ->
                    if (state.pages.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = false,
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                        ) { page ->
                            val pageState = state.pages[page]
                            DashboardPageLayout(
                                page = pageState,
                                telemetry = data,
                                telemetryText = textData,
                                aisTelemetry = aisData,
                                aisTelemetryText = aisTextData,
                                recentNmeaPgnHistory = recentNmeaPgnHistory,
                                recentNmea0183History = recentNmea0183History,
                                dalyDebugEvents = dalyDebugEvents,
                                detectedNmeaSources = state.detectedNmeaSources,
                                titleScale = state.fontScale,
                uiFont = state.uiFont,
                darkBackground = state.darkBackground,
                gridStepPercent = state.gridStepPercent,
                widgetFrameStyle = state.widgetFrameStyle,
                widgetFrameStyleGrayOffset = state.widgetFrameStyleGrayOffset,
                onMove = { id, dx, dy, gridStepPx, pageWidthPx, pageHeightPx, persistLayout ->
                    viewModel.moveWidget(
                        widgetId = id,
                                        dx = dx,
                                        dy = dy,
                                        gridStepPx = gridStepPx,
                                        pageWidthPx = pageWidthPx,
                                        pageHeightPx = pageHeightPx,
                                        persistLayout = persistLayout,
                                    )
                                },
                                onResize = { id, dw, dh, gridStepPx, pageWidthPx, pageHeightPx, persistLayout ->
                                    viewModel.resizeWidget(
                                        widgetId = id,
                                        dw = dw,
                                        dh = dh,
                                        gridStepPx = gridStepPx,
                                        pageWidthPx = pageWidthPx,
                                        pageHeightPx = pageHeightPx,
                                        persistLayout = persistLayout,
                                    )
                                },
                               onSnap = { id, gridStepPx, pageWidthPx, pageHeightPx ->
                                    viewModel.snapWidgetToGrid(id, gridStepPx, pageWidthPx, pageHeightPx)
                                },
                                onRemove = viewModel::removeWidget,
                                onRename = viewModel::renameWidget,
                                storedWindWidgetSettings = state.windWidgetSettings,
                                onAddWidget = { kind, widthPx, heightPx, stepPx ->
                                    val success = viewModel.addWidget(
                                        kind = kind,
                                        pageWidthPx = widthPx,
                                        pageHeightPx = heightPx,
                                        gridStepPx = stepPx,
                                    )
                                    if (!success) {
                                        showWidgetAddError("Nicht genug Platz für ein neues Widget.")
                                    }
                                    success
                                },
                                onAddWidgetError = showWidgetAddError,
                                onPageMetrics = { widthPx, heightPx, stepPx ->
                                    dashboardPageWidthPx = widthPx
                                    dashboardPageHeightPx = heightPx
                                    dashboardGridStepPx = stepPx
                                },
                                onApplyMinimums = { pageWidthPx, pageHeightPx, gridStepPx ->
                                    viewModel.enforceMinimumWidgetSizesForPage(
                                        pageState.id,
                                        pageWidthPx,
                                        pageHeightPx,
                                        gridStepPx
                                    )
                                },
                                onSendAutopilotCommand = viewModel::sendAutopilotCommand,
                                onUpdateWindWidgetSettings = viewModel::updateWindWidgetSettings,
                                onUpdateBatteryWidgetSettings = viewModel::updateBatteryWidgetSettings,
                                onUpdateAisWidgetSettings = viewModel::updateAisWidgetSettings,
                                onUpdateEchosounderWidgetSettings = viewModel::updateEchosounderWidgetSettings,
                                onUpdateAutopilotWidgetSettings = viewModel::updateAutopilotWidgetSettings,
                                onUpdateLogWidgetSettings = viewModel::updateLogWidgetSettings,
                                onUpdateAnchorWatchWidgetSettings = viewModel::updateAnchorWatchWidgetSettings,
                                onUpdateTemperatureWidgetSettings = viewModel::updateTemperatureWidgetSettings,
                                onUpdateSeaChartWidgetSettings = viewModel::updateSeaChartWidgetSettings,
                                alarmToneVolume = state.alarmToneVolume,
                                alarmRepeatIntervalSeconds = state.alarmRepeatIntervalSeconds,
                                onAnchorWatchAlarm = { widgetTitle ->
                                    anchorWatchAlarmAnnouncer.announce(
                                        widgetName = widgetTitle,
                                        enabled = state.alarmVoiceAnnouncementsEnabled,
                                        profileIndex = state.alarmVoiceProfileIndex,
                                    )
                                },
                                onCallAisMmsi = { mmsi ->
                                    scope.launch {
                                        val errorMessage = viewModel.sendAisCallMmsi(
                                            mmsi = mmsi,
                                            host = state.nmeaRouterHost,
                                            port = state.udpPort,
                                        )
                                        if (errorMessage != null) {
                                            showWidgetAddError(errorMessage)
                                        }
                                    }
                                },
                                storedBatteryWidgetSettings = state.batteryWidgetSettings,
                                storedAisWidgetSettings = state.aisWidgetSettings,
                                storedEchosounderWidgetSettings = state.echosounderWidgetSettings,
                                storedAutopilotWidgetSettings = state.autopilotWidgetSettings,
                                storedLogWidgetSettings = state.logWidgetSettings,
                                storedAnchorWatchWidgetSettings = state.anchorWatchWidgetSettings,
                                storedTemperatureWidgetSettings = state.temperatureWidgetSettings,
                                storedSeaChartWidgetSettings = state.seaChartWidgetSettings,
                                activeRoute = viewModel.activeRouteAsNavRoute(),
                                mobLat = state.mobPosition?.lat,
                                mobLon = state.mobPosition?.lon,
                                mobTimestampMs = state.mobPosition?.timestampMs,
                                boatDraftMeters = state.boatProfile.draftMeters,
                                onTriggerMob = viewModel::triggerMob,
                                onClearMob = viewModel::clearMob,
                                onShowSeaChartNotice = { title, message ->
                                    postUserWarning(message = message, title = title)
                                },
                            )
                        }
                    } else {
                        EmptyState(
                            onCreatePage = { viewModel.addPage("Navigation") },
                            darkBackground = state.darkBackground,
                            modifier = Modifier.fillMaxSize().padding(padding)
                        )
                    }
                }
            }
        }
    }

    private fun requestTabletLocationPermission() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissions.isEmpty()) return
        tabletLocationPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestDalyBlePermissions() {
        // Intentionally disabled to prevent any DALY-BMS BLE reconnect attempts.
    }

    private fun hasRequiredDalyBlePermissions(): Boolean {
        return requiredDalyBlePermissions().isEmpty()
    }

    private fun requiredDalyBlePermissions(): List<String> {
        val requiredPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            requiredPermissions.add(Manifest.permission.BLUETOOTH)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
    }
}

private val headingWithCourseOverGroundKeys = listOf(
    "heading",
    "compass_heading",
    "autopilot_heading",
    "navigation.heading",
    "navigation.heading_true",
    "navigation.heading_magnetic",
    "heading_true",
    "heading_magnetic",
    "navigation.course_over_ground_true",
    "navigation.course_over_ground",
    "course_over_ground_true",
    "course_over_ground",
    "course",
    "cog_true",
    "cog_magnetic",
    "cog",
)

private const val AIS_TARGET_CACHE_TTL_DEFAULT_MINUTES = 5
private const val AIS_TARGET_CACHE_TTL_MAX_MINUTES = 5
private const val AIS_TARGET_STALE_WARNING_MINUTES = 3
private const val AIS_TARGET_CACHE_MAX_ENTRIES = 500
private const val AIS_TARGET_LOG_INTERVAL_MS = 2_000L
private const val AIS_TARGET_LOG_PREFIX = "AISTrack"

private data class CachedAisTargetState(
    val target: AisTargetData,
    val receivedAtMs: Long,
)

private fun aisTargetCacheKey(target: AisTargetData): String {
    return aisTargetIdentityKey(target)
}

private fun aisTargetIdentityKey(target: AisTargetData): String {
    resolveAisMmsiText(target)?.let { mmsi ->
        return "mmsi_$mmsi"
    }
    if (target.id.isNotBlank()) {
        return "id_${target.id}"
    }
    val nameHint = target.name?.trim()?.ifBlank { null }
    if (!nameHint.isNullOrBlank()) {
        return "name_${nameHint.lowercase()}"
    }
    return "fallback_${target.lastSeenMs}_${target.hashCode()}"
}

private fun mergeCachedAisTarget(
    existing: AisTargetData,
    incoming: AisTargetData,
): AisTargetData = incoming.copy(
    id = incoming.id.ifBlank { existing.id },
    name = incoming.name ?: existing.name,
    mmsi = incoming.mmsi ?: existing.mmsi,
    mmsiText = incoming.mmsiText ?: existing.mmsiText,
    latitude = incoming.latitude ?: existing.latitude,
    longitude = incoming.longitude ?: existing.longitude,
    lastSeenMs = if (incoming.lastSeenMs > 0L) {
        maxOf(existing.lastSeenMs, incoming.lastSeenMs)
    } else {
        existing.lastSeenMs
    },
    distanceNm = incoming.distanceNm ?: existing.distanceNm,
    cpaNm = incoming.cpaNm ?: existing.cpaNm,
    cpaTimeMinutes = incoming.cpaTimeMinutes ?: existing.cpaTimeMinutes,
    courseDeg = incoming.courseDeg ?: existing.courseDeg,
    speedKn = incoming.speedKn ?: existing.speedKn,
    maneuverRestriction = incoming.maneuverRestriction ?: existing.maneuverRestriction,
    relativeBearingDeg = incoming.relativeBearingDeg ?: existing.relativeBearingDeg,
    absoluteBearingDeg = incoming.absoluteBearingDeg ?: existing.absoluteBearingDeg,
)

private fun aisTargetLogLabel(target: AisTargetData): String {
    val name = target.name?.trim()?.ifBlank { null }
    val mmsi = resolveAisMmsiText(target)
    val label = name?.let { "Boot: $it" }
        ?: mmsi?.let {
        "MMSI $it"
    } ?: if (target.id.isNotBlank()) {
        "ID ${target.id}"
    } else {
        "unbekannt"
    }
    val nameSuffix = target.name?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
    val distance = target.distanceNm?.takeIf { it.isFinite() }?.let {
        " dist=${"%.2f".format(Locale.US, it)}nm"
    } ?: ""
    val bearing = target.relativeBearingDeg?.takeIf { it.isFinite() }?.let { " rel=${"%.1f".format(Locale.US, it)}°" } ?: ""
    return "$label$nameSuffix$distance$bearing"
}

private fun normalizeAisMmsi(value: Float?): String? {
    val mmsi = value?.takeIf { it.isFinite() && it > 0f && it < 1_000_000_000f } ?: return null
    val normalized = kotlin.math.round(mmsi.toDouble()).toLong()
    return if (normalized in 1L..999_999_999L) {
        normalized.toString()
    } else {
        null
    }
}

private fun resolveAisMmsiText(target: AisTargetData): String? {
    return target.mmsiText?.trim()?.takeIf { it.isNotBlank() && it.any { ch -> ch.isDigit() } }
        ?: target.mmsi?.let { mmsi -> normalizeAisMmsi(mmsi) }
}

private fun resolveAisTargetVisibilityMinutes(
    target: AisTargetData,
    settings: AisWidgetSettings,
): Int {
    val perMmsi = resolveAisMmsiText(target)?.let { settings.targetVisibilityMinutesByMmsi[it] }
    return (perMmsi ?: settings.targetVisibilityMinutes).coerceIn(
        AIS_TARGET_STALE_WARNING_MINUTES,
        AIS_TARGET_CACHE_TTL_MAX_MINUTES,
    )
}

private fun resolveAisTargetStaleMinutes(
    target: AisTargetData,
    settings: AisWidgetSettings,
): Int {
    return minOf(
        resolveAisTargetVisibilityMinutes(target, settings),
        AIS_TARGET_STALE_WARNING_MINUTES,
    )
}

private val autoNmeaSourcePgnLabels = mapOf(
    130306 to "Windrichtung",
    127250 to "Kurs/Heading",
    127245 to "Ruder / Autopilot",
    129026 to "Kurs über Grund / Geschwindigkeit",
    129025 to "GPS Position",
    129029 to "GPS Position",
    127489 to "Motordrehzahl",
    127506 to "Tankwerte",
    127508 to "Batterie",
    127237 to "Autopilot",
    128267 to "Wassertiefe",
    129038 to "AIS Ziel",
    129039 to "AIS Ziel",
    60928 to "Gerätekennung / ISO Adresse",
)

private fun sourcePgnLine(pgn: Int): String {
    val label = autoNmeaSourcePgnLabels[pgn]?.let { " ($it)" } ?: ""
    return "${pgn}$label"
}

private fun sourceDetectedStatus(profile: NmeaSourceProfile): String {
    return if (profile.displayName.isNotBlank()) {
        "erkannt"
    } else {
        "nicht erkannt"
    }
}

private fun uiFontLabel(font: UiFont): String {
    return when (font) {
        UiFont.FUTURA -> "Futura"
        UiFont.ORBITRON -> "Orbitron"
        UiFont.PT_MONO -> "PT Mono"
        UiFont.ELECTROLIZE -> "Electrolize"
        UiFont.DOT_GOTHIC -> "Dot"
    }
}

private fun backupPrivacyModeDescription(mode: BackupPrivacyMode): String {
    return when (mode) {
        BackupPrivacyMode.privateOnly -> "Backups bleiben im privaten App-Speicher."
        BackupPrivacyMode.manualExport -> "Öffentliche Exporte nur nach manueller Nutzeraktion."
        BackupPrivacyMode.cloudAllowed -> "Cloud-/System-Backup ist erlaubt, nur mit bewusstem Opt-in."
    }
}

private val windHistoryWindowOptions = listOf(
    5 to "5 Min",
    30 to "30 Minuten",
    60 to "1 Stunde",
    1440 to "24 Stunden",
    10080 to "1 Woche",
    43200 to "1 Monat",
)

private val ECHO_SOUNDER_TONE_SECTION_GROUPS = listOf(
    "Nebelhorn- und Warnklänge" to listOf(0, 1),
    "Sirenenklänge" to listOf(2, 3),
    "Warn-/Wecksignale" to listOf(4, 5),
    "Marine- und Sicherheitsalarm" to listOf(6, 7),
    "Reservealarm" to listOf(8, 9),
)
private val MENU_INPUT_FIELD_HEIGHT = SeaFoxDesignTokens.Size.menuInputFieldHeight
private val MENU_INPUT_FIELD_SHAPE = SeaFoxDesignTokens.Shape.menuInputFieldShape
private val MENU_INPUT_FIELD_BACKGROUND_LIGHT = SeaFoxDesignTokens.Color.menuInputFieldBackgroundLight
private val MENU_INPUT_FIELD_BACKGROUND_DARK = SeaFoxDesignTokens.Color.menuInputFieldBackgroundDark
private val MENU_INPUT_FIELD_TEXT_LIGHT = SeaFoxDesignTokens.Color.menuInputFieldTextLight
private val MENU_INPUT_FIELD_TEXT_DARK = SeaFoxDesignTokens.Color.menuInputFieldTextDark
private val NON_MENU_BUTTON_COLOR = SeaFoxDesignTokens.NonMenuButton.color

private val SeaFoxDarkColorScheme = darkColorScheme(
    primary = SeaFoxDesignTokens.Color.cyan,
    onPrimary = Color(0xFF03131B),
    secondary = SeaFoxDesignTokens.Color.brass,
    onSecondary = Color(0xFF1E1708),
    tertiary = SeaFoxDesignTokens.Color.emerald,
    background = SeaFoxDesignTokens.Color.dashboardDark,
    onBackground = Color(0xFFEAF7FF),
    surface = SeaFoxDesignTokens.Color.surfaceDark,
    onSurface = Color(0xFFEAF7FF),
    surfaceVariant = SeaFoxDesignTokens.Color.surfaceRaisedDark,
    onSurfaceVariant = SeaFoxDesignTokens.Color.mutedDark,
    outline = SeaFoxDesignTokens.Color.hairlineDark,
    error = SeaFoxDesignTokens.Color.coral,
)

private val SeaFoxLightColorScheme = lightColorScheme(
    primary = Color(0xFF006985),
    onPrimary = Color.White,
    secondary = Color(0xFF7A5C16),
    onSecondary = Color.White,
    tertiary = Color(0xFF006D4F),
    background = SeaFoxDesignTokens.Color.dashboardLight,
    onBackground = SeaFoxDesignTokens.Color.ink,
    surface = SeaFoxDesignTokens.Color.surfaceLight,
    onSurface = SeaFoxDesignTokens.Color.ink,
    surfaceVariant = SeaFoxDesignTokens.Color.surfaceRaisedLight,
    onSurfaceVariant = SeaFoxDesignTokens.Color.slateText,
    outline = SeaFoxDesignTokens.Color.hairlineLight,
    error = Color(0xFFB3261E),
)

private fun seaFoxColorScheme(darkBackground: Boolean) = if (darkBackground) {
    SeaFoxDarkColorScheme
} else {
    SeaFoxLightColorScheme
}

private fun premiumDashboardBrush(darkBackground: Boolean): Brush = if (darkBackground) {
    Brush.linearGradient(
        listOf(
            Color(0xFF04090E),
            SeaFoxDesignTokens.Color.dashboardDark,
            Color(0xFF0D1C27),
            Color(0xFF071017),
        )
    )
} else {
    Brush.linearGradient(
        listOf(
            Color(0xFFF8FAF8),
            SeaFoxDesignTokens.Color.dashboardLight,
            Color(0xFFDCE7EA),
            Color(0xFFF4F7F5),
        )
    )
}

private fun premiumTopBarBrush(darkBackground: Boolean): Brush = if (darkBackground) {
    Brush.verticalGradient(
        listOf(
            Color(0xFA122230),
            Color(0xF2081119),
        )
    )
} else {
    Brush.verticalGradient(
        listOf(
            Color(0xFFFFFFFF),
            Color(0xF1E6EFF1),
        )
    )
}

@Composable
private fun PremiumDashboardBackdrop(
    darkBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val gridColor = if (darkBackground) {
        Color(0xFF9DEBFF).copy(alpha = 0.07f)
    } else {
        Color(0xFF42616F).copy(alpha = 0.08f)
    }
    val contourColor = if (darkBackground) {
        SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.09f)
    } else {
        Color(0xFF4E7480).copy(alpha = 0.08f)
    }
    val vignetteColor = if (darkBackground) {
        Color.Black.copy(alpha = 0.20f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }

    Box(modifier = modifier.background(premiumDashboardBrush(darkBackground))) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val minorStroke = 0.55.dp.toPx()
            val majorStroke = 1.dp.toPx()

            for (index in 0..12) {
                val x = size.width * index / 12f
                val y = size.height * index / 12f
                val isMajor = index % 4 == 0
                drawLine(
                    color = gridColor.copy(alpha = if (isMajor) gridColor.alpha * 1.7f else gridColor.alpha),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (isMajor) majorStroke else minorStroke,
                )
                drawLine(
                    color = gridColor.copy(alpha = if (isMajor) gridColor.alpha * 1.7f else gridColor.alpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (isMajor) majorStroke else minorStroke,
                )
            }

            for (index in 0..5) {
                val yBase = size.height * (0.14f + index * 0.15f)
                val xOffset = size.width * (index * 0.035f)
                val path = Path().apply {
                    moveTo(-size.width * 0.08f, yBase)
                    cubicTo(
                        size.width * 0.18f + xOffset,
                        yBase - size.height * 0.12f,
                        size.width * 0.44f,
                        yBase + size.height * 0.11f,
                        size.width * 0.70f,
                        yBase - size.height * 0.03f,
                    )
                    cubicTo(
                        size.width * 0.92f,
                        yBase - size.height * 0.12f,
                        size.width * 1.04f,
                        yBase + size.height * 0.10f,
                        size.width * 1.10f,
                        yBase,
                    )
                }
                drawPath(
                    path = path,
                    color = contourColor.copy(alpha = contourColor.alpha * (1f - index * 0.09f)),
                    style = Stroke(width = (1.2f + index * 0.18f).dp.toPx()),
                )
            }

            drawCircle(
                color = if (darkBackground) {
                    SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.07f)
                } else {
                    Color.White.copy(alpha = 0.32f)
                },
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.80f, size.height * 0.10f),
            )
            drawRect(color = vignetteColor)
        }
    }
}

@Composable
private fun nonMenuTextButtonColors() = SeaFoxDesignTokens.NonMenuButton.textButtonColors()

@Composable
private fun nonMenuElevatedButtonColors() = SeaFoxDesignTokens.NonMenuButton.elevatedButtonColors()

private fun menuInputFieldBackgroundColor(darkBackground: Boolean): Color = if (darkBackground) {
    MENU_INPUT_FIELD_BACKGROUND_DARK
} else {
    MENU_INPUT_FIELD_BACKGROUND_LIGHT
}

@Composable
private fun menuOutlinedTextFieldColors(darkBackground: Boolean) = OutlinedTextFieldDefaults.colors().copy(
    focusedTextColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    unfocusedTextColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    disabledTextColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK.copy(alpha = 0.6f) else MENU_INPUT_FIELD_TEXT_LIGHT.copy(alpha = 0.6f),
    errorTextColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    focusedLabelColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    unfocusedLabelColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK.copy(alpha = 0.75f) else MENU_INPUT_FIELD_TEXT_LIGHT.copy(alpha = 0.75f),
    disabledLabelColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK.copy(alpha = 0.45f) else MENU_INPUT_FIELD_TEXT_LIGHT.copy(alpha = 0.45f),
    errorLabelColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    focusedPlaceholderColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK.copy(alpha = 0.55f) else MENU_INPUT_FIELD_TEXT_LIGHT.copy(alpha = 0.55f),
    unfocusedPlaceholderColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK.copy(alpha = 0.55f) else MENU_INPUT_FIELD_TEXT_LIGHT.copy(alpha = 0.55f),
    disabledPlaceholderColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK.copy(alpha = 0.45f) else MENU_INPUT_FIELD_TEXT_LIGHT.copy(alpha = 0.45f),
    errorPlaceholderColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedContainerColor = menuInputFieldBackgroundColor(darkBackground),
    unfocusedContainerColor = menuInputFieldBackgroundColor(darkBackground),
    disabledContainerColor = menuInputFieldBackgroundColor(darkBackground).copy(alpha = 0.8f),
    errorContainerColor = menuInputFieldBackgroundColor(darkBackground),
    cursorColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    errorCursorColor = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
)

@Composable
private fun MenuCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: Shape = MENU_INPUT_FIELD_SHAPE,
) {
    val fieldColor = textStyle.color
    val backgroundColor = if (fieldColor == MENU_INPUT_FIELD_TEXT_DARK) {
        MENU_INPUT_FIELD_BACKGROUND_DARK
    } else {
        MENU_INPUT_FIELD_BACKGROUND_LIGHT
    }
    val activeTextColor = if (enabled) fieldColor else fieldColor.copy(alpha = 0.55f)
    val disabledBackgroundColor = backgroundColor.copy(alpha = 0.8f)
    val inactivePlaceholderColor = fieldColor.copy(alpha = 0.65f)
    val effectivePlaceholderColor = if (enabled) inactivePlaceholderColor else inactivePlaceholderColor.copy(alpha = 0.7f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = activeTextColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(activeTextColor),
        modifier = modifier
            .height(MENU_INPUT_FIELD_HEIGHT)
            .defaultMinSize(minHeight = MENU_INPUT_FIELD_HEIGHT),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (enabled) backgroundColor else disabledBackgroundColor,
                        shape = shape
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    CompositionLocalProvider(
                        LocalContentColor provides effectivePlaceholderColor
                    ) {
                        if (placeholder != null) {
                            placeholder()
                        } else {
                            label?.let { it() }
                        }
                    }
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun AlarmToneSectionSelector(
    selectedToneIndex: Int,
    onToneSelected: (Int) -> Unit,
    labelStyle: TextStyle,
) {
    if (ECHO_SOUNDER_TONES.size > 4) {
        var showMenu by remember { mutableStateOf(false) }
        val selectedIndex = selectedToneIndex.coerceIn(0, ECHO_SOUNDER_TONES.lastIndex.coerceAtLeast(0))
        val selectedToneName = ECHO_SOUNDER_TONES.getOrNull(selectedIndex)?.name ?: "Alarmton"

        CompactMenuTextButton(
            text = "${selectedToneName} ▾",
            style = labelStyle,
            fillWidth = false,
            onClick = { showMenu = true }
        )
        DropdownMenu(
            modifier = Modifier.background(Color.White),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            ECHO_SOUNDER_TONES.forEachIndexed { index, tone ->
                DropdownMenuItem(
                    text = { Text(tone.name, style = labelStyle) },
                    onClick = {
                        onToneSelected(index)
                        showMenu = false
                    }
                )
            }
        }
        return
    }

    ECHO_SOUNDER_TONE_SECTION_GROUPS.forEach { (sectionTitle, toneIndexes) ->
        Text(
            sectionTitle,
            style = labelStyle.copy(fontWeight = FontWeight.Bold),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SeaFoxDesignTokens.Size.menuSmallSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            toneIndexes.forEach { toneIndex ->
                val tone = ECHO_SOUNDER_TONES.getOrNull(toneIndex)
                if (tone != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                    modifier = Modifier.weight(1f),
                ) {
                    RadioButton(
                        selected = selectedToneIndex == toneIndex,
                        onClick = { onToneSelected(toneIndex) },
                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                    )
                        Text(tone.name, style = labelStyle)
                    }
                }
            }
            if (toneIndexes.size < 2) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider()
    }
}

private fun anchorWatchChainPocketsFromText(
    text: String,
): Int {
    return text.toIntOrNull()?.coerceIn(1, 999) ?: 1
}

private fun anchorWatchSignalLengthMm(
    unit: AnchorWatchChainUnit,
    strengthLabel: String,
    pocketsText: String,
): Float {
    val pockets = anchorWatchChainPocketsFromText(pocketsText)
    val innerSpacingMm = anchorWatchChainStrengthOptions(unit).firstOrNull { it.label == strengthLabel }?.innerSpacingMm
        ?: anchorWatchChainStrengthOptions(unit).firstOrNull()?.innerSpacingMm
        ?: 0f
    return (innerSpacingMm * pockets.toFloat()).coerceAtLeast(0.1f)
}

private fun anchorWatchChainSpacingMeters(
    unit: AnchorWatchChainUnit,
    strengthLabel: String,
    pocketsText: String,
): Float {
    return anchorWatchSignalLengthMm(unit, strengthLabel, pocketsText) / 1000f
}

private const val TAG_WIDGET_INTERACTION = "seaFOX.WidgetInteraction"
private const val APP_RELEASE_HISTORY_URL =
    "https://github.com/frankfox/seafox/releases"
private const val OPENSEAMAP_OPENCPN_PAGE_URL = "https://www.openseamap.org/index.php?L=1&id=33"
private const val OPENSEAMAP_DOWNLOAD_CHART_URL = "https://www.openseamap.org/index.php?L=1&id=kartendownload"
private const val OPENSEAMAP_KAP_CHARTS_URL = "https://wiki.openstreetmap.org/wiki/KAP-charts_from_OpenSeaMap"
private const val OPENSEAMAP_AT5_CHARTS_URL = "https://wiki.openstreetmap.org/wiki/AT5-OpenSeaMap-Chart_for_Lowrance_Simrad_B%26G"
private const val OPENSEAMAP_AT5_RAWDATA_URL = "https://ftp.gwdg.de/pub/misc/openstreetmap/openseamap/charts/at5/rawdata/"
private const val OPENSEAMAP_MBTILES_BASE_URL = "https://ftp.gwdg.de/pub/misc/openstreetmap/openseamap/charts/mbtiles/"
private const val APP_AUTHOR = "Frank Fox AUTRIA"
private const val APP_COPYRIGHT_TEXT =
    "Copyright © Frank Fox AUTRIA. Alle Rechte vorbehalten. " +
    "Diese App ist proprietäre, nicht offene Software. " +
    "Jede Vervielfältigung, Verbreitung, Dekompilierung, " +
    "Reverse Engineering, Umkehrung der Quellcodes, " +
    "oder kommerzielle Weiterverwendung ist ohne schriftliche Zustimmung des Rechteinhabers unzulässig."
private const val SEA_CHART_UNPACK_CHANNEL_ID = "seafox_seachart_unpack"
private const val SEA_CHART_UNPACK_CHANNEL_NAME = "seaCHART Kartenimport"
private const val SEA_CHART_UNPACK_CHANNEL_DESCRIPTION = "Statusmeldungen zum Entpacken von seaCHART-Zip-Dateien"
private const val SEA_CHART_UNPACK_NOTIFICATION_ID = 9012
private const val SEA_CHART_VISIBLE_EXTERNAL_ROOT_NAME = "seaFOX"
private const val SEA_CHART_VISIBLE_EXTERNAL_ROOT_NAME_ALT = "seaCHART"
private const val SEA_CHART_WEB_TILE_HOST = "seafox.local"
private const val SEA_CHART_WEB_TILE_ROUTE = "seachart"
private const val SEA_CHART_DISCOVERY_MAX_DEPTH = 12
private val SEA_CHART_TILE_FILE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
private val SEA_CHART_MBTILES_EXTENSIONS = setOf("mbtiles", "mvt")
private val SEA_CHART_RENDERABLE_FILE_EXTENSIONS = SEA_CHART_TILE_FILE_EXTENSIONS + SEA_CHART_MBTILES_EXTENSIONS
private val SEA_CHART_OPEN_SEA_CHARTS_RECOGNIZED_EXTENSIONS = setOf(
    "at5",
    "shp",
    "shx",
    "dbf",
    "prj",
    "cpg",
    "mbtiles",
    "pbf",
)
private val SEA_CHART_C_MAP_RECOGNIZED_EXTENSIONS = setOf(
    "kap",
    "img",
    "mbtiles",
)
private val SEA_CHART_NOAA_RECOGNIZED_EXTENSIONS = SEA_CHART_OPEN_SEA_CHARTS_RECOGNIZED_EXTENSIONS
    .plus(setOf("000", "001", "002", "txt"))
private const val NOAA_ENC_CATALOG_URL = "https://charts.noaa.gov/ENCs/ENCs.shtml"
private const val NOAA_ENC_HOME_URL = "https://nauticalcharts.noaa.gov/charts/noaa-enc.html"
private const val NOAA_RNC_KMZ_URL = "https://marineregions.org/encdirect?source=noaa"
private const val NOAA_ENC_ZIP_BASE_URL = "https://charts.noaa.gov/ENCs/"
private const val C_MAP_DOCUMENTATION_URL = "https://www.c-map.com/all-charts/"
// S-57 freie Quellen weltweit
private const val CHS_ENC_URL = "https://www.charts.gc.ca/charts-cartes/enc-cnc-eng.html"
private const val LINZ_ENC_URL = "https://www.linz.govt.nz/products-services/hydrographic/hydrographic-charts"
private const val BSH_ENC_URL = "https://www.bsh.de/DE/DATEN/Seekarten/seekarten_node.html"
// S-63
private const val PRIMAR_URL = "https://www.primar.org"
private const val IC_ENC_URL = "https://www.ic-enc.org"
private const val AVCS_URL = "https://www.admiralty.co.uk/digital-services/digital-charts/admiralty-vector-chart-service"
private enum class SeaChartDownloadCatalog(val buttonLabel: String) {
    C_MAP("C-Map"),
    OPEN_SEA_CHARTS("OpenSeaCharts"),
    NOAA("NOAA"),
    S57("S-57"),
    S63("S-63"),
    US_FREE_CHARTS("kostenlose US-Karten"),
}
private val SEA_CHART_OPEN_SEA_CHARTS_DOWNLOAD_RESOURCES = listOf(
    Triple("MBTiles-FTP (alle Regionen)", OPENSEAMAP_MBTILES_BASE_URL, "Direktes Verzeichnis aller MBTiles-Kartenpakete"),
    Triple("OpenSeaMap Karten-Download", OPENSEAMAP_DOWNLOAD_CHART_URL, "Direkte Downloadquellen für OpenSeaMap-Karten"),
    Triple("OpenSeaMap OpenCPN", OPENSEAMAP_OPENCPN_PAGE_URL, "Offizieller Überblick mit Install- und Integrationshinweisen"),
)
private val SEA_CHART_C_MAP_DOWNLOAD_RESOURCES = emptyList<Triple<String, String, String>>()
private val SEA_CHART_NOAA_DOWNLOAD_RESOURCES = listOf(
    Triple("NOAA ENC-Kartenportal", NOAA_ENC_CATALOG_URL, "Offizielles NOAA-Portal mit ENC-Katalog und Downloadoptionen"),
    Triple("NOAA ENC-Hinweise", NOAA_ENC_HOME_URL, "Übersicht zu NOAA ENCs, Verfügbarkeit und Nutzungshinweisen"),
    Triple("NOAA RNC/KMZ (indirekte Quellen)", NOAA_RNC_KMZ_URL, "Mögliche NOAA-Kartendatenquellen für zusätzliche Formate"),
)
private val SEA_CHART_S57_DOWNLOAD_RESOURCES = listOf(
    Triple("Kanada (CHS)", CHS_ENC_URL, "Canadian Hydrographic Service — kostenlose S-57 ENCs für kanadische Gewässer"),
    Triple("Neuseeland (LINZ)", LINZ_ENC_URL, "Land Information New Zealand — kostenlose ENCs"),
    Triple("Deutschland (BSH)", BSH_ENC_URL, "Bundesamt für Seeschifffahrt und Hydrographie"),
)
private val SEA_CHART_S57_REGION_DOWNLOADS = emptyList<Triple<String, String, String>>()
private val SEA_CHART_S63_DOWNLOAD_RESOURCES = listOf(
    Triple("PRIMAR", PRIMAR_URL, "Internationaler ENC-Verteiler — S-63 verschlüsselte Karten"),
    Triple("IC-ENC", IC_ENC_URL, "International Centre for ENCs"),
    Triple("AVCS (Admiralty)", AVCS_URL, "Admiralty Vector Chart Service — weltweite Abdeckung"),
)
private val SEA_CHART_S63_REGION_DOWNLOADS = emptyList<Triple<String, String, String>>()
private val SEA_CHART_OPEN_SEA_CHARTS_REGION_DOWNLOADS = listOf(
    // Europa
    Triple("Europa (gesamt)", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Europa1.mbtiles", "Gesamteuropäische Küste inkl. Seezeichen (~3,9 GB)"),
    Triple("Nordsee", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-NorthSea.mbtiles", "Nordsee inkl. dt./nl./brit. Küste (~1,8 GB)"),
    Triple("Ostsee", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Baltic.mbtiles", "Ostsee komplett (~1,6 GB)"),
    Triple("Ärmelkanal", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Channel.mbtiles", "Englischer Kanal (~580 MB)"),
    Triple("Adria", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Adria.mbtiles", "Adriatisches Meer (~650 MB)"),
    Triple("Mittelmeer West", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-MediWest.mbtiles", "Westliches Mittelmeer (~1,2 GB)"),
    Triple("Mittelmeer Ost", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-MediEast.mbtiles", "Östliches Mittelmeer (~1,6 GB)"),
    Triple("Golf von Biskaya", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-GulfOfBiscay.mbtiles", "Biskaya (~420 MB)"),
    Triple("Nordatlantik", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-NorthernAtlantic.mbtiles", "Nordatlantik (~640 MB)"),
    // Binnengewässer
    Triple("Bodensee", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-LakeConstance.mbtiles", "Bodensee (~100 MB)"),
    Triple("Niederlande Binnen", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Niederlande-Binnen.mbtiles", "Niederländische Binnengewässer (~260 MB)"),
    Triple("Saimaa (Finnland)", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Saimaa.mbtiles", "Saimaa-Seengebiet (~450 MB)"),
    Triple("Balaton", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Lake_Balaton.mbtiles", "Plattensee (~70 MB)"),
    // Amerika
    Triple("Karibik", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-Caribbean.mbtiles", "Karibische Gewässer (~980 MB)"),
    Triple("US-Westküste", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-USWestCoast.mbtiles", "US West Coast (~670 MB)"),
    Triple("Große Seen", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-GreatLakes.mbtiles", "Great Lakes (~260 MB)"),
    Triple("Magellanstraße", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-MagellanStrait.mbtiles", "Magellanstraße (~85 MB)"),
    Triple("Nordwestpassage", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-NorthWestPassage.mbtiles", "Arktische Nordwestpassage (~310 MB)"),
    // Asien / Pazifik
    Triple("Arabisches Meer", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-ArabianSea.mbtiles", "Arabisches Meer (~680 MB)"),
    Triple("Golf von Bengalen", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-GulfOfBengal.mbtiles", "Golf von Bengalen (~150 MB)"),
    Triple("Ostchinesisches Meer", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-EastChineseSea.mbtiles", "Ostchinesisches Meer (~510 MB)"),
    Triple("Südchinesisches Meer", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-SouthChineseSea.mbtiles", "Südchinesisches Meer (~1,1 GB)"),
    Triple("Südpazifik-Inseln", "${OPENSEAMAP_MBTILES_BASE_URL}OSM-OpenCPN2-SouthPacificIslands.mbtiles", "Südpazifische Inseln (~720 MB)"),
)
private val SEA_CHART_NOAA_REGION_DOWNLOADS = listOf(
    Triple("NOAA - Alle ENCs", "${NOAA_ENC_ZIP_BASE_URL}All_ENCs.zip", "Kompletter Paketdownload aller verfügbaren NOAA-ENCs"),
    Triple("NOAA - 10 Tage", "${NOAA_ENC_ZIP_BASE_URL}TenDays_ENCs.zip", "Letzte 10 Tage aktualisierter NOAA-ENC-Datensatz"),
    Triple("NOAA - 1 Woche", "${NOAA_ENC_ZIP_BASE_URL}OneWeek_ENCs.zip", "Wöchentlicher NOAA-ENC-Pack mit aktuellen Updates"),
    Triple("NOAA - 2 Tage", "${NOAA_ENC_ZIP_BASE_URL}TwoDays_ENCs.zip", "Kurzer aktualisierter NOAA-ENC-Pack"),
    Triple("NOAA - 1 Tag", "${NOAA_ENC_ZIP_BASE_URL}OneDay_ENCs.zip", "Aktualisiertes Tagespaket"),
    Triple("NOAA - 01 CGD", "${NOAA_ENC_ZIP_BASE_URL}01CGD_ENCs.zip", "Coast Guard District 01"),
    Triple("NOAA - 05 CGD", "${NOAA_ENC_ZIP_BASE_URL}05CGD_ENCs.zip", "Coast Guard District 05"),
    Triple("NOAA - 07 CGD", "${NOAA_ENC_ZIP_BASE_URL}07CGD_ENCs.zip", "Coast Guard District 07"),
    Triple("NOAA - 08 CGD", "${NOAA_ENC_ZIP_BASE_URL}08CGD_ENCs.zip", "Coast Guard District 08"),
    Triple("NOAA - 09 CGD", "${NOAA_ENC_ZIP_BASE_URL}09CGD_ENCs.zip", "Coast Guard District 09"),
    Triple("NOAA - 11 CGD", "${NOAA_ENC_ZIP_BASE_URL}11CGD_ENCs.zip", "Coast Guard District 11"),
    Triple("NOAA - 13 CGD", "${NOAA_ENC_ZIP_BASE_URL}13CGD_ENCs.zip", "Coast Guard District 13"),
    Triple("NOAA - 14 CGD", "${NOAA_ENC_ZIP_BASE_URL}14CGD_ENCs.zip", "Coast Guard District 14"),
    Triple("NOAA - 17 CGD", "${NOAA_ENC_ZIP_BASE_URL}17CGD_ENCs.zip", "Coast Guard District 17"),
    Triple("NOAA - 22 Region", "${NOAA_ENC_ZIP_BASE_URL}22Region_ENCs.zip", "NOAA-Region 22"),
    Triple("NOAA - 30 Region", "${NOAA_ENC_ZIP_BASE_URL}30Region_ENCs.zip", "NOAA-Region 30"),
    Triple("NOAA - 32 Region", "${NOAA_ENC_ZIP_BASE_URL}32Region_ENCs.zip", "NOAA-Region 32"),
    Triple("NOAA - 34 Region", "${NOAA_ENC_ZIP_BASE_URL}34Region_ENCs.zip", "NOAA-Region 34"),
    Triple("NOAA - 36 Region", "${NOAA_ENC_ZIP_BASE_URL}36Region_ENCs.zip", "NOAA-Region 36"),
    Triple("NOAA - 40 Region", "${NOAA_ENC_ZIP_BASE_URL}40Region_ENCs.zip", "NOAA-Region 40"),
)
private val SEA_CHART_US_FREE_REGION_DOWNLOADS = SEA_CHART_OPEN_SEA_CHARTS_REGION_DOWNLOADS.filter {
    it.first.contains("US", ignoreCase = true)
}
private val SEA_CHART_US_CHART_DOWNLOAD_RESOURCES = listOf(
    Triple("AT5-FTP (manuelle US-Regionen)", OPENSEAMAP_AT5_RAWDATA_URL, "Direktes Verzeichnis für US-AT5-Zip-Dateien im Dateimanager"),
)
private val SEA_CHART_C_MAP_REGION_DOWNLOADS = emptyList<Triple<String, String, String>>()

private fun seaChartDownloadCatalogRegions(catalog: SeaChartDownloadCatalog): List<Triple<String, String, String>> = when (catalog) {
    SeaChartDownloadCatalog.C_MAP -> SEA_CHART_C_MAP_REGION_DOWNLOADS
    SeaChartDownloadCatalog.OPEN_SEA_CHARTS -> SEA_CHART_OPEN_SEA_CHARTS_REGION_DOWNLOADS
    SeaChartDownloadCatalog.NOAA -> SEA_CHART_NOAA_REGION_DOWNLOADS
    SeaChartDownloadCatalog.S57 -> SEA_CHART_S57_REGION_DOWNLOADS
    SeaChartDownloadCatalog.S63 -> SEA_CHART_S63_REGION_DOWNLOADS
    SeaChartDownloadCatalog.US_FREE_CHARTS -> SEA_CHART_US_FREE_REGION_DOWNLOADS
}

private fun seaChartDownloadCatalogResources(catalog: SeaChartDownloadCatalog): List<Triple<String, String, String>> = when (catalog) {
    SeaChartDownloadCatalog.C_MAP -> SEA_CHART_C_MAP_DOWNLOAD_RESOURCES
    SeaChartDownloadCatalog.OPEN_SEA_CHARTS -> SEA_CHART_OPEN_SEA_CHARTS_DOWNLOAD_RESOURCES
    SeaChartDownloadCatalog.NOAA -> SEA_CHART_NOAA_DOWNLOAD_RESOURCES
    SeaChartDownloadCatalog.S57 -> SEA_CHART_S57_DOWNLOAD_RESOURCES
    SeaChartDownloadCatalog.S63 -> SEA_CHART_S63_DOWNLOAD_RESOURCES
    SeaChartDownloadCatalog.US_FREE_CHARTS -> SEA_CHART_US_CHART_DOWNLOAD_RESOURCES
}

private fun seaChartDownloadCatalogProvider(catalog: SeaChartDownloadCatalog): SeaChartMapProvider = when (catalog) {
    SeaChartDownloadCatalog.C_MAP -> SeaChartMapProvider.C_MAP
    SeaChartDownloadCatalog.OPEN_SEA_CHARTS -> SeaChartMapProvider.OPEN_SEA_CHARTS
    SeaChartDownloadCatalog.NOAA -> SeaChartMapProvider.NOAA
    SeaChartDownloadCatalog.S57 -> SeaChartMapProvider.S57
    SeaChartDownloadCatalog.S63 -> SeaChartMapProvider.S63
    SeaChartDownloadCatalog.US_FREE_CHARTS -> SeaChartMapProvider.OPEN_SEA_CHARTS
}

private fun seaChartDocumentationUrl(mapProvider: SeaChartMapProvider): String = when (mapProvider) {
    SeaChartMapProvider.C_MAP -> C_MAP_DOCUMENTATION_URL
    SeaChartMapProvider.OPEN_SEA_CHARTS -> OPENSEAMAP_OPENCPN_PAGE_URL
    SeaChartMapProvider.NOAA -> NOAA_ENC_HOME_URL
    SeaChartMapProvider.S57 -> CHS_ENC_URL
    SeaChartMapProvider.S63 -> PRIMAR_URL
}

private fun seaChartDownloadCatalogForProvider(mapProvider: SeaChartMapProvider): SeaChartDownloadCatalog = when (mapProvider) {
    SeaChartMapProvider.C_MAP -> SeaChartDownloadCatalog.C_MAP
    SeaChartMapProvider.OPEN_SEA_CHARTS -> SeaChartDownloadCatalog.OPEN_SEA_CHARTS
    SeaChartMapProvider.NOAA -> SeaChartDownloadCatalog.NOAA
    SeaChartMapProvider.S57 -> SeaChartDownloadCatalog.S57
    SeaChartMapProvider.S63 -> SeaChartDownloadCatalog.S63
}

private fun seaChartDownloadCatalogDialogTitle(catalog: SeaChartDownloadCatalog): String = when (catalog) {
    SeaChartDownloadCatalog.C_MAP -> "C-Map Karten herunterladen"
    SeaChartDownloadCatalog.OPEN_SEA_CHARTS -> "OpenSeaCharts Karten herunterladen"
    SeaChartDownloadCatalog.NOAA -> "NOAA Karten herunterladen"
    SeaChartDownloadCatalog.S57 -> "S-57 Karten herunterladen"
    SeaChartDownloadCatalog.S63 -> "S-63 Karten herunterladen"
    SeaChartDownloadCatalog.US_FREE_CHARTS -> "kostenlose US-Karten herunterladen"
}

private fun seaChartDownloadCatalogDescription(catalog: SeaChartDownloadCatalog): String = when (catalog) {
    SeaChartDownloadCatalog.C_MAP ->
        "Für C-Map werden lokale Kacheldaten oder kompatible Kartendateien benötigt. " +
            "Im Moment gibt es hierfür keine integrierten frei downloadbaren Quellen."
    SeaChartDownloadCatalog.OPEN_SEA_CHARTS ->
        "OpenSeaCharts bietet kostenlose MBTiles-Seekarten mit Basiskarte und Seezeichen. " +
            "Wähle eine Region zum direkten Download. Die Dateien sind sofort offline nutzbar. " +
            "Hinweis: Tiefen- und Höhenangaben können je nach Region unterschiedlich vollständig sein."
    SeaChartDownloadCatalog.NOAA ->
        "NOAA-ENCs stehen als direkte Paket-Downloads bereit. " +
            "Wähle eines der NOAA-Pakete oder öffne bei Bedarf die offiziellen Links für einzelne Updates. " +
            "Für das seaCHART-Widget werden lokale Kachelkarten ({z}/{x}/{y}) mit .png/.jpg/.jpeg/.webp/.pbf sowie MBTiles unterstützt."
    SeaChartDownloadCatalog.S57 ->
        "Freie S-57 ENCs sind bei einigen nationalen Hydrographiediensten verfügbar. " +
            "Lade die Karten direkt von der jeweiligen Webseite herunter und importiere die .000-Dateien."
    SeaChartDownloadCatalog.S63 ->
        "S-63 verschlüsselte Seekarten werden in einer zukünftigen Version unterstützt. " +
            "Für den Erwerb von S-63 Karten besuche einen der offiziellen Anbieter."
    SeaChartDownloadCatalog.US_FREE_CHARTS ->
        "Für kostenfreie US-Karten stehen US-Regionen als AT5-ähnliche Downloadpakete zur Verfügung."
}

private const val SEA_CHART_OFFLINE_CACHE_SUBFOLDER = "seaCHART"
private const val SEA_CHART_OPEN_SEA_CHARTS_PROVIDER_SUBFOLDER = "openSeaChart"
private const val SEA_CHART_C_MAP_PROVIDER_SUBFOLDER = "c-map"
private const val SEA_CHART_NOAA_PROVIDER_SUBFOLDER = "noaa"
private const val SEA_CHART_S57_PROVIDER_SUBFOLDER = "s57"
private const val SEA_CHART_S63_PROVIDER_SUBFOLDER = "s63"
private const val SEA_CHART_DOWNLOAD_PREFS_NAME = "seachart_download_session"
private const val SEA_CHART_DOWNLOAD_SESSION_ID = "downloadId"
private const val SEA_CHART_DOWNLOAD_SESSION_PROVIDER = "provider"
private const val SEA_CHART_DOWNLOAD_SESSION_REGION_LABEL = "regionLabel"
private const val SEA_CHART_DOWNLOAD_SESSION_REGION_URL = "regionUrl"
private const val SEA_CHART_DOWNLOAD_SESSION_FILE_NAME = "fileName"
private const val SEA_CHART_DOWNLOAD_SESSION_REGION_FOLDER = "regionFolder"
private const val SEA_CHART_DOWNLOAD_SESSION_STARTED_AT = "startedAt"
private const val SEA_CHART_DOWNLOAD_METADATA_FILE_NAME = ".seafox-download-metadata.json"
private const val SEA_CHART_NO_ACTIVE_DOWNLOAD_ID = -1L
private const val SEA_CHART_SOURCE_SCAN_CACHE_TTL_MS = 10 * 60_000L
private data class SeaChartSourceScanCacheEntry(
    val createdAtMs: Long,
    val sourceCandidatePaths: List<String>,
    val renderableSourceCandidateEntries: List<Pair<String, String>>,
)
private data class SeaChartTileTemplateCacheEntry(
    val createdAtMs: Long,
    val template: String?,
)
private data class SeaChartProviderRootCacheEntry(
    val createdAtMs: Long,
    val roots: List<File>,
)
private val seaChartSourceScanCache = ConcurrentHashMap<SeaChartMapProvider, SeaChartSourceScanCacheEntry>()
private val seaChartTileTemplateCache = ConcurrentHashMap<SeaChartMapProvider, SeaChartTileTemplateCacheEntry>()
private val seaChartProviderRootCache = ConcurrentHashMap<SeaChartMapProvider, SeaChartProviderRootCacheEntry>()

private fun seaChartInvalidateSeaChartCaches(mapProvider: SeaChartMapProvider? = null) {
    if (mapProvider == null) {
        seaChartSourceScanCache.clear()
        seaChartTileTemplateCache.clear()
        seaChartProviderRootCache.clear()
        return
    }

    seaChartSourceScanCache.remove(mapProvider)
    seaChartTileTemplateCache.remove(mapProvider)
    seaChartProviderRootCache.remove(mapProvider)
}

private fun currentSeaChartCacheStampMs(): Long = SystemClock.elapsedRealtime()

private val SEA_CHART_FILE_EXTENSIONS = (
    SEA_CHART_RENDERABLE_FILE_EXTENSIONS +
        SEA_CHART_OPEN_SEA_CHARTS_RECOGNIZED_EXTENSIONS +
        SEA_CHART_C_MAP_RECOGNIZED_EXTENSIONS +
        SEA_CHART_NOAA_RECOGNIZED_EXTENSIONS +
        setOf("zip")
).toList()

private fun seaChartVisibleDownloadRootCandidates(): List<File> = runCatching {
    val externalStorage = Environment.getExternalStorageDirectory()
    listOf(
        File(externalStorage, SEA_CHART_VISIBLE_EXTERNAL_ROOT_NAME),
        File(externalStorage, SEA_CHART_VISIBLE_EXTERNAL_ROOT_NAME_ALT),
        File(externalStorage, "$SEA_CHART_VISIBLE_EXTERNAL_ROOT_NAME/$SEA_CHART_OFFLINE_CACHE_SUBFOLDER"),
        File(externalStorage, SEA_CHART_OFFLINE_CACHE_SUBFOLDER),
    )
}.getOrDefault(emptyList())

private fun seaChartExternalAppRoot(context: Context): File? {
    val externalBase = context.getExternalFilesDir(null) ?: return null
    return externalBase.resolve(SEA_CHART_OFFLINE_CACHE_SUBFOLDER)
}
private data class SeaChartDownloadUiState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val unpackedBytes: Long = 0L,
    val unpackTotalBytes: Long = -1L,
    val isUnpacking: Boolean = false,
    val statusText: String = "",
    val errorText: String? = null,
    val downloadedFileName: String = "",
    val completed: Boolean = false,
)
private data class SeaChartDownloadRequest(
    val regionLabel: String,
    val regionUrl: String,
    val mapProvider: SeaChartMapProvider,
)
private data class SeaChartRemoteArchiveInfo(
    val etag: String? = null,
    val lastModified: String? = null,
    val contentLength: Long = -1L,
)
private data class SeaChartDownloadMetadata(
    val fileName: String,
    val downloadedAtMillis: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val contentLength: Long = -1L,
)
private data class SeaChartExistingDataPromptState(
    val request: SeaChartDownloadRequest,
    val title: String,
    val message: String,
    val confirmText: String,
)
private enum class SeaChartRemoteUpdateStatus {
    UPDATED,
    UNCHANGED,
    UNKNOWN,
}
private enum class SeaChartRemoteCheckStatus {
    OK,
    OFFLINE,
    UNREACHABLE,
    UNKNOWN,
}
private data class SeaChartRemoteCheckResult(
    val status: SeaChartRemoteCheckStatus,
    val info: SeaChartRemoteArchiveInfo? = null,
    val message: String? = null,
)
private data class SeaChartDownloadSession(
    val downloadId: Long,
    val providerName: String,
    val regionLabel: String,
    val regionUrl: String,
    val fileName: String,
    val regionFolder: String,
    val startedAtMillis: Long = System.currentTimeMillis(),
)
private fun seaChartDownloadSessionPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(SEA_CHART_DOWNLOAD_PREFS_NAME, Context.MODE_PRIVATE)
}
private fun seaChartStoreDownloadSession(context: Context, session: SeaChartDownloadSession) {
    seaChartDownloadSessionPrefs(context).edit()
        .putLong(SEA_CHART_DOWNLOAD_SESSION_ID, session.downloadId)
        .putString(SEA_CHART_DOWNLOAD_SESSION_PROVIDER, session.providerName)
        .putString(SEA_CHART_DOWNLOAD_SESSION_REGION_LABEL, session.regionLabel)
        .putString(SEA_CHART_DOWNLOAD_SESSION_REGION_URL, session.regionUrl)
        .putString(SEA_CHART_DOWNLOAD_SESSION_FILE_NAME, session.fileName)
        .putString(SEA_CHART_DOWNLOAD_SESSION_REGION_FOLDER, session.regionFolder)
        .putLong(SEA_CHART_DOWNLOAD_SESSION_STARTED_AT, session.startedAtMillis)
        .apply()
}
private fun seaChartLoadDownloadSession(context: Context): SeaChartDownloadSession? {
    val prefs = seaChartDownloadSessionPrefs(context)
    val downloadId = prefs.getLong(SEA_CHART_DOWNLOAD_SESSION_ID, SEA_CHART_NO_ACTIVE_DOWNLOAD_ID)
    if (downloadId <= 0L) return null

    val providerName = prefs.getString(SEA_CHART_DOWNLOAD_SESSION_PROVIDER, "") ?: ""
    val regionLabel = prefs.getString(SEA_CHART_DOWNLOAD_SESSION_REGION_LABEL, "") ?: ""
    val regionUrl = prefs.getString(SEA_CHART_DOWNLOAD_SESSION_REGION_URL, "") ?: ""
    val fileName = prefs.getString(SEA_CHART_DOWNLOAD_SESSION_FILE_NAME, "") ?: ""
    val regionFolder = prefs.getString(SEA_CHART_DOWNLOAD_SESSION_REGION_FOLDER, "") ?: ""
    if (providerName.isBlank() || regionLabel.isBlank() || regionUrl.isBlank() || fileName.isBlank() || regionFolder.isBlank()) {
        seaChartClearDownloadSession(context)
        return null
    }

    return SeaChartDownloadSession(
        downloadId = downloadId,
        providerName = providerName,
        regionLabel = regionLabel,
        regionUrl = regionUrl,
        fileName = fileName,
        regionFolder = regionFolder,
        startedAtMillis = prefs.getLong(SEA_CHART_DOWNLOAD_SESSION_STARTED_AT, System.currentTimeMillis()),
    )
}
private fun seaChartClearDownloadSession(context: Context) {
    seaChartDownloadSessionPrefs(context).edit()
        .remove(SEA_CHART_DOWNLOAD_SESSION_ID)
        .remove(SEA_CHART_DOWNLOAD_SESSION_PROVIDER)
        .remove(SEA_CHART_DOWNLOAD_SESSION_REGION_LABEL)
        .remove(SEA_CHART_DOWNLOAD_SESSION_REGION_URL)
        .remove(SEA_CHART_DOWNLOAD_SESSION_FILE_NAME)
        .remove(SEA_CHART_DOWNLOAD_SESSION_REGION_FOLDER)
        .remove(SEA_CHART_DOWNLOAD_SESSION_STARTED_AT)
        .apply()
}
private data class SeaChartDownloadErrorState(
    val message: String,
    val canRetry: Boolean = false,
    val provider: SeaChartMapProvider? = null,
    val regionLabel: String? = null,
    val regionUrl: String? = null,
    val fileName: String? = null,
    val filePath: String? = null,
)
private data class SeaChartDownloadedRegionInfo(
    val regionName: String,
    val lastLoadedAtMillis: Long,
    val fileName: String,
    val filePath: String,
    val isRenderable: Boolean = true,
    val formatNote: String? = null,
)
private data class SeaChartPendingZipInfo(
    val provider: SeaChartMapProvider,
    val fileName: String,
    val filePath: String,
    val destinationPath: String,
    val sizeBytes: Long,
)

private data class SeaChartLayerFilterCapabilities(
    val supportsRoadLayers: Boolean = false,
    val supportsDepthLines: Boolean = false,
    val supportsContourLines: Boolean = false,
) {
    fun hasAnyLayer(): Boolean {
        return supportsRoadLayers || supportsDepthLines || supportsContourLines
    }

    fun merged(other: SeaChartLayerFilterCapabilities): SeaChartLayerFilterCapabilities {
        return SeaChartLayerFilterCapabilities(
            supportsRoadLayers = supportsRoadLayers || other.supportsRoadLayers,
            supportsDepthLines = supportsDepthLines || other.supportsDepthLines,
            supportsContourLines = supportsContourLines || other.supportsContourLines,
        )
    }
}

private val SEA_CHART_VECTOR_STYLE_HINT_EXTENSIONS = setOf(
    "json",
    "js",
    "txt",
    "yml",
    "yaml",
    "xml",
    "style",
    "mss",
)
private val SEA_CHART_RASTER_TILE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
private val SEA_CHART_VECTOR_TILE_EXTENSIONS = setOf("mvt", "pbf")
private const val SEA_CHART_TEST_MODE_DISABLE_MAP_LOADING = false
private const val SEA_CHART_TEST_MODE_DISABLE_SOURCE_WARNINGS = false
private val SEA_CHART_LAYER_FILTER_ROAD_KEYWORDS = setOf(
    "road",
    "roads",
    "street",
    "streets",
    "coastline",
    "coast_line",
    "waterway",
    "bridge",
    "coast",
    "line",
    "lines",
)
private val SEA_CHART_LAYER_FILTER_DEPTH_KEYWORDS = setOf(
    "depth",
    "sounding",
    "soundings",
    "bathym",
    "bathymetry",
    "depare",
    "hydrography",
    "hydro",
    "deptharea",
    "depth_area",
)
private val SEA_CHART_LAYER_FILTER_CONTOUR_KEYWORDS = setOf(
    "contour",
    "contour_line",
    "contour_lines",
    "contours",
    "isobath",
    "isobaths",
    "depthcontour",
    "depth_contour",
    "depthContours",
    "depth_contours",
)
private const val SEA_CHART_LAYER_HINT_MAX_BYTES = 2 * 1024 * 1024

private fun seaChartLayerFilterCapabilities(context: Context, mapProvider: SeaChartMapProvider): SeaChartLayerFilterCapabilities {
    val renderableSources = seaChartRenderableSourceCandidates(context, mapProvider)
    if (renderableSources.isEmpty()) return SeaChartLayerFilterCapabilities()

    val candidateRoots = renderableSources
        .mapNotNull { (source, _) ->
            if (source.isDirectory) source else source.parentFile
        }.distinctBy { it.absolutePath }

    var discoveredCapabilities = SeaChartLayerFilterCapabilities()
    var hasVectorSource = false

    for (root in candidateRoots) {
        if (!root.exists() || !root.isDirectory) continue

        val rootFiles = runCatching {
            root.walkTopDown().maxDepth(9).filter { it.isFile }.toList()
        }.getOrDefault(emptyList())

        val rootHasVectorTiles = rootFiles.any { candidate ->
            val extension = candidate.extension.lowercase()
            extension in SEA_CHART_VECTOR_TILE_EXTENSIONS || extension == "mbtiles"
        }
        if (!rootHasVectorTiles) {
            continue
        }
        hasVectorSource = true

        var rootCapabilities = SeaChartLayerFilterCapabilities()

        for (candidate in rootFiles) {
            val extension = candidate.extension.lowercase()
            val name = candidate.name.lowercase()
            if (extension == "mbtiles") {
                rootCapabilities = rootCapabilities.merged(
                    seaChartInferLayerFilterCapabilitiesFromMbTiles(candidate),
                )
                continue
            }
            rootCapabilities = rootCapabilities.merged(seaChartInferLayerFilterCapabilitiesFromText(name))
            if (!SEA_CHART_VECTOR_STYLE_HINT_EXTENSIONS.contains(extension)) {
                continue
            }
            if (candidate.length() > SEA_CHART_LAYER_HINT_MAX_BYTES) {
                continue
            }
            val content = runCatching { candidate.readText() }.getOrNull() ?: continue
            rootCapabilities = rootCapabilities.merged(
                seaChartInferLayerFilterCapabilitiesFromStyleContent(content),
            )
            if (rootCapabilities.hasAnyLayer()) {
                break
            }
        }

        discoveredCapabilities = discoveredCapabilities.merged(rootCapabilities)
    }

    if (!hasVectorSource) return SeaChartLayerFilterCapabilities()
    return if (discoveredCapabilities.hasAnyLayer()) {
        discoveredCapabilities
    } else {
        SeaChartLayerFilterCapabilities()
    }
}

private fun seaChartInferLayerFilterCapabilitiesFromMbTiles(file: File): SeaChartLayerFilterCapabilities {
    if (!file.exists() || !file.isFile || file.extension.lowercase() != "mbtiles") {
        return SeaChartLayerFilterCapabilities()
    }

    val database = runCatching {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }.getOrNull() ?: return SeaChartLayerFilterCapabilities()

    return try {
        val metadataQuery = """
            SELECT value FROM metadata
            WHERE name IN ('json', 'json_data', 'vector_layers')
        """.trimIndent()

        runCatching {
            database.rawQuery(metadataQuery, null)
        }.getOrNull()?.use { cursor ->
            val valueIndex = cursor.getColumnIndex("value")
            if (valueIndex < 0) return@use SeaChartLayerFilterCapabilities()

            var capabilities = SeaChartLayerFilterCapabilities()
            while (cursor.moveToNext()) {
                val metadataValue = runCatching { cursor.getString(valueIndex) }.getOrNull() ?: continue
                capabilities = capabilities.merged(
                    seaChartInferLayerFilterCapabilitiesFromText(metadataValue.lowercase()),
                )
            }
            capabilities
        } ?: SeaChartLayerFilterCapabilities()
    } finally {
        runCatching { database.close() }
    }
}

private fun seaChartInferLayerFilterCapabilitiesFromStyleContent(text: String): SeaChartLayerFilterCapabilities {
    val normalized = text.lowercase()
    val textBased = seaChartInferLayerFilterCapabilitiesFromText(normalized)
    val jsonBased = runCatching { JSONObject(text) }.mapCatching { style ->
        val layers = style.optJSONArray("layers") ?: return@mapCatching textBased
        var capabilities = SeaChartLayerFilterCapabilities()
        for (index in 0 until layers.length()) {
            val layer = layers.optJSONObject(index) ?: continue
            val layerId = layer.optString("id", "").lowercase()
            val sourceLayer = layer.optString("source-layer", "").lowercase()
            val sourceLayerAlt = layer.optString("sourceLayer", "").lowercase()
            val type = layer.optString("type", "").lowercase()
            val className = layer.optString("class", "").lowercase()
            val candidateText = listOf(layerId, sourceLayer, sourceLayerAlt, type, className).joinToString(" ")
            capabilities = capabilities.merged(seaChartInferLayerFilterCapabilitiesFromText(candidateText))
        }
        capabilities
    }.getOrElse { textBased }

    return if (jsonBased.hasAnyLayer()) jsonBased else textBased
}

private fun seaChartInferLayerFilterCapabilitiesFromText(text: String): SeaChartLayerFilterCapabilities {
    val normalizedText = text.lowercase()
    return SeaChartLayerFilterCapabilities(
        supportsRoadLayers = SEA_CHART_LAYER_FILTER_ROAD_KEYWORDS.any { keyword ->
            normalizedText.contains(keyword)
        },
        supportsDepthLines = SEA_CHART_LAYER_FILTER_DEPTH_KEYWORDS.any { keyword ->
            normalizedText.contains(keyword)
        },
        supportsContourLines = SEA_CHART_LAYER_FILTER_CONTOUR_KEYWORDS.any { keyword ->
            normalizedText.contains(keyword)
        },
    )
}

private fun seaChartDownloadFileName(label: String, url: String): String {
    val nameFromUrl = runCatching {
        URL(url).path.substringAfterLast('/').substringBefore('?')
    }.getOrNull()?.trim()?.ifBlank { null }
    val baseName = if (!nameFromUrl.isNullOrBlank()) nameFromUrl else sanitizeSeaChartDownloadName(label)
    return if (baseName.contains('.')) baseName else "${baseName}.zip"
}

private fun seaChartProviderSubfolder(mapProvider: SeaChartMapProvider): String {
    return when (mapProvider) {
        SeaChartMapProvider.C_MAP -> SEA_CHART_C_MAP_PROVIDER_SUBFOLDER
        SeaChartMapProvider.OPEN_SEA_CHARTS -> SEA_CHART_OPEN_SEA_CHARTS_PROVIDER_SUBFOLDER
        SeaChartMapProvider.NOAA -> SEA_CHART_NOAA_PROVIDER_SUBFOLDER
        SeaChartMapProvider.S57 -> SEA_CHART_S57_PROVIDER_SUBFOLDER
        SeaChartMapProvider.S63 -> SEA_CHART_S63_PROVIDER_SUBFOLDER
    }
}

private fun ensureSeaChartUnpackNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(SEA_CHART_UNPACK_CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        SEA_CHART_UNPACK_CHANNEL_ID,
        SEA_CHART_UNPACK_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = SEA_CHART_UNPACK_CHANNEL_DESCRIPTION
    }
    manager.createNotificationChannel(channel)
}

private fun canPostSeaChartNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

@SuppressLint("MissingPermission")
private fun notifySeaChartUnpackStatus(context: Context, statusText: String) {
    if (!canPostSeaChartNotifications(context)) return
    ensureSeaChartUnpackNotificationChannel(context)
    val title = "seaCHART Karten"
    val notification = NotificationCompat.Builder(context.applicationContext, SEA_CHART_UNPACK_CHANNEL_ID)
        .setSmallIcon(R.drawable.nmea2000_icon)
        .setContentTitle(title)
        .setContentText(statusText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(Notification.CATEGORY_STATUS)
        .setAutoCancel(false)
        .build()
    NotificationManagerCompat.from(context).notify(SEA_CHART_UNPACK_NOTIFICATION_ID, notification)
}

private fun announceSeaChartUnpackStatus(
    context: Context,
    statusText: String,
    onPersistentNotice: ((String) -> Unit)? = null,
) {
    if (
        statusText.startsWith("Entpacken gestartet:") ||
        statusText.startsWith("Karten entpackt nach:") ||
        statusText.startsWith("Entpacken abgeschlossen")
    ) {
        onPersistentNotice?.invoke(statusText)
        notifySeaChartUnpackStatus(context, statusText)
    }
}

private fun seaChartDownloadRegionFolder(label: String, url: String): String {
    val normalizedLabel = sanitizeSeaChartDownloadName(label)
    val normalizedUrlPart = runCatching {
        URL(url).path.substringAfterLast('/').substringBefore('?')
            .substringBeforeLast('.')
    }.getOrNull()?.trim()
    val fallback = "region"
    val suffix = if (normalizedUrlPart.isNullOrBlank()) {
        fallback
    } else {
        sanitizeSeaChartDownloadName(normalizedUrlPart)
    }
    return if (normalizedLabel.contains(suffix)) {
        normalizedLabel
    } else {
        "${normalizedLabel}_${suffix}"
    }
}

private fun sanitizeSeaChartDownloadName(value: String): String {
    return value
        .replace(" ", "_")
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .trim('_', ' ', '.')
        .ifBlank { "seachart" }
}

private suspend fun seaChartZipArchiveExpectedBytes(archiveFile: File): Long {
    return runCatching {
        ZipFile(archiveFile).use { zip ->
            zip.entries().asSequence().fold(0L) { total, entry ->
                if (entry.isDirectory) total else total + maxOf(entry.size, 0L)
            }
        }
    }.getOrNull() ?: -1L
}

private suspend fun validateSeaChartZipArchive(
    archiveFile: File,
    onProgress: suspend (Long, Long, String) -> Unit = { _, _, _ -> },
): Boolean {
    if (!archiveFile.exists()) return false
    val expectedBytes = seaChartZipArchiveExpectedBytes(archiveFile)
    onProgress(0L, expectedBytes, "Vorbereitung")
    try {
        ZipFile(archiveFile).use { zip ->
            val buffer = ByteArray(32 * 1024)
            var validatedBytes = 0L
            var currentEntry = "Vorbereitung"
            var lastProgressUpdatePercent = -1
            var lastProgressBytes = 0L
            zip.entries().asSequence().forEach { entry ->
                coroutineContext.ensureActive()
                val entryName = entry.name
                    .replace("\\", "/")
                    .trimStart('/')
                currentEntry = entryName
                if (entry.isDirectory || entryName.isBlank()) {
                    return@forEach
                }
                zip.getInputStream(entry).use { input ->
                    while (true) {
                        coroutineContext.ensureActive()
                        val bytesRead = try {
                            input.read(buffer)
                        } catch (e: EOFException) {
                            throw IOException("ZIP-Daten unvollständig für Eintrag $entryName", e)
                        }
                        if (bytesRead < 0) break
                        validatedBytes += bytesRead.toLong()
                        val percent = if (expectedBytes > 0L) {
                            ((validatedBytes * 100L) / expectedBytes).coerceIn(0L, 100L).toInt()
                        } else {
                            -1
                        }
                        val shouldReportProgress = if (expectedBytes > 0L) {
                            percent != lastProgressUpdatePercent
                        } else {
                            validatedBytes - lastProgressBytes >= 1024L * 1024L
                        }
                        if (shouldReportProgress) {
                            onProgress(validatedBytes, expectedBytes, currentEntry)
                            lastProgressUpdatePercent = percent
                            lastProgressBytes = validatedBytes
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        throw e
    } catch (e: Exception) {
        throw IOException("ZIP-Datei beschädigt: ${archiveFile.name}", e)
    }
    onProgress(expectedBytes.coerceAtLeast(0L), expectedBytes, "abgeschlossen")
    return true
}

private data class SeaChartCatalogCrcEntry(
    val relativePath: String,
    val expectedCrcHex: String?,
)

private data class SeaChartCatalogIntegrityTarget(
    val file: File,
    val relativePath: String,
    val expectedCrcHex: String?,
)

private val SEA_CHART_CATALOG_RECORD_PREFIX_REGEX = Regex("""^CD\d{10}""")
private val SEA_CHART_CATALOG_CRC_REGEX = Regex("""^[0-9A-Fa-f]{8}$""")

private fun loadSeaChartCatalogCrcEntries(catalogFile: File): List<SeaChartCatalogCrcEntry> {
    val rawText = runCatching { catalogFile.readBytes().toString(Charsets.ISO_8859_1) }
        .getOrNull()
        ?: return emptyList()
    return rawText
        .split('\u001e')
        .mapNotNull { record ->
            val fields = record.split('\u001f')
            val pathField = fields.getOrNull(0)?.trim().orEmpty()
            if (!pathField.startsWith("CD")) return@mapNotNull null
            val relativePath = pathField
                .replace(SEA_CHART_CATALOG_RECORD_PREFIX_REGEX, "")
                .replace('\\', '/')
                .trimStart('/')
                .trim()
            if (relativePath.isBlank()) return@mapNotNull null
            SeaChartCatalogCrcEntry(
                relativePath = relativePath,
                expectedCrcHex = fields.getOrNull(7)
                    ?.trim()
                    ?.takeIf { candidate -> SEA_CHART_CATALOG_CRC_REGEX.matches(candidate) }
                    ?.uppercase(Locale.ROOT),
            )
        }
}

private suspend fun seaChartComputeCrc32Hex(file: File): String {
    val crc32 = CRC32()
    val buffer = ByteArray(64 * 1024)
    BufferedInputStream(FileInputStream(file)).use { input ->
        while (true) {
            coroutineContext.ensureActive()
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) break
            crc32.update(buffer, 0, bytesRead)
        }
    }
    return crc32.value.toString(16).uppercase(Locale.ROOT).padStart(8, '0')
}

private suspend fun validateSeaChartExtractedCatalogIntegrity(
    extractionDir: File,
    onProgress: suspend (Long, Long, String) -> Unit = { _, _, _ -> },
): Boolean {
    val catalogFiles = runCatching {
        extractionDir.walkTopDown()
            .maxDepth(4)
            .filter { file -> file.isFile && file.name.equals("CATALOG.031", ignoreCase = true) }
            .toList()
    }.getOrDefault(emptyList())
    if (catalogFiles.isEmpty()) {
        throw IOException("CATALOG.031 nach dem Entpacken nicht gefunden.")
    }

    val targets = buildList {
        catalogFiles.forEach { catalogFile ->
            val encRoot = catalogFile.parentFile ?: return@forEach
            val entries = loadSeaChartCatalogCrcEntries(catalogFile)
            if (entries.isEmpty()) {
                throw IOException("CATALOG.031 konnte nicht gelesen werden: ${catalogFile.absolutePath}")
            }
            entries.forEach { entry ->
                add(
                    SeaChartCatalogIntegrityTarget(
                        file = encRoot.resolve(entry.relativePath),
                        relativePath = entry.relativePath,
                        expectedCrcHex = entry.expectedCrcHex,
                    ),
                )
            }
        }
    }
    if (targets.isEmpty()) {
        throw IOException("CATALOG.031 enthält keine prüfbaren Dateieinträge.")
    }

    val totalWeight = targets.sumOf { target ->
        target.file.takeIf { it.isFile }?.length()?.coerceAtLeast(1L) ?: 1L
    }.coerceAtLeast(1L)
    onProgress(0L, totalWeight, "Vorbereitung")

    var processedWeight = 0L
    var lastReportedPercent = -1
    val missingFiles = mutableListOf<String>()
    val mismatchedFiles = mutableListOf<String>()

    targets.forEach { target ->
        coroutineContext.ensureActive()
        val currentEntry = target.relativePath
        val weight = target.file.takeIf { it.isFile }?.length()?.coerceAtLeast(1L) ?: 1L
        if (!target.file.isFile) {
            missingFiles += currentEntry
        } else if (!target.expectedCrcHex.isNullOrBlank()) {
            val actualCrcHex = seaChartComputeCrc32Hex(target.file)
            if (!actualCrcHex.equals(target.expectedCrcHex, ignoreCase = true)) {
                mismatchedFiles += "$currentEntry (erwartet ${target.expectedCrcHex}, ist $actualCrcHex)"
            }
        }
        processedWeight += weight
        val percent = ((processedWeight * 100L) / totalWeight).coerceIn(0L, 100L).toInt()
        if (percent != lastReportedPercent) {
            onProgress(processedWeight, totalWeight, currentEntry)
            lastReportedPercent = percent
        }
    }

    onProgress(totalWeight, totalWeight, "abgeschlossen")

    if (missingFiles.isNotEmpty() || mismatchedFiles.isNotEmpty()) {
        val summaryParts = buildList {
            if (missingFiles.isNotEmpty()) {
                add(
                    "Fehlende Dateien: " +
                        missingFiles.take(5).joinToString(", ") +
                        if (missingFiles.size > 5) " (+${missingFiles.size - 5} weitere)" else "",
                )
            }
            if (mismatchedFiles.isNotEmpty()) {
                add(
                    "CRC-Abweichungen: " +
                        mismatchedFiles.take(3).joinToString(", ") +
                        if (mismatchedFiles.size > 3) " (+${mismatchedFiles.size - 3} weitere)" else "",
                )
            }
        }
        throw IOException(summaryParts.joinToString(" | "))
    }

    return true
}

private suspend fun extractZipSeaChartArchive(
    archiveFile: File,
    destinationDir: File,
    associatedMapProvider: SeaChartMapProvider? = null,
    onProgress: suspend (Long, Long, String) -> Unit = { _, _, _ -> },
): Boolean {
    if (!archiveFile.exists()) return false
    val expectedBytes = seaChartZipArchiveExpectedBytes(archiveFile)
    onProgress(0L, expectedBytes, "Vorbereitung")
    val destinationCanonical = destinationDir.canonicalFile.absolutePath
    try {
        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zip ->
            val buffer = ByteArray(32 * 1024)
            var extractedBytes = 0L
            var currentEntry = "Vorbereitung"
            var lastProgressUpdatePercent = -1
            var lastProgressBytes = 0L
            while (true) {
                coroutineContext.ensureActive()
                val entry = try {
                    zip.nextEntry
                } catch (e: EOFException) {
                    throw IOException("ZIP-Datei beschädigt: ${archiveFile.name}", e)
                }
                if (entry == null) break
                val entryName = entry.name
                    .replace("\\", "/")
                    .trimStart('/')
                currentEntry = entryName
                if (entryName.isBlank() || entryName.startsWith("../") || entryName.contains("/../")) {
                    zip.closeEntry()
                    continue
                }
                val outputFile = File(destinationDir, entryName)
                if (!outputFile.canonicalFile.absolutePath.startsWith(destinationCanonical)) {
                    zip.closeEntry()
                    continue
                }
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                    zip.closeEntry()
                    continue
                }
                outputFile.parentFile?.mkdirs()
                try {
                    FileOutputStream(outputFile).use { output ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val bytesRead = try {
                                zip.read(buffer)
                            } catch (e: EOFException) {
                                throw IOException("ZIP-Daten unvollständig für Eintrag $entryName", e)
                            }
                            if (bytesRead < 0) break
                            output.write(buffer, 0, bytesRead)
                            extractedBytes += bytesRead.toLong()
                            val percent = if (expectedBytes > 0L) {
                                ((extractedBytes * 100L) / expectedBytes).coerceIn(0L, 100L).toInt()
                            } else {
                                -1
                            }
                            val shouldReportProgress = if (expectedBytes > 0L) {
                                percent != lastProgressUpdatePercent
                            } else {
                                extractedBytes - lastProgressBytes >= 1024L * 1024L
                            }
                            if (shouldReportProgress) {
                                onProgress(extractedBytes, expectedBytes, currentEntry)
                                lastProgressUpdatePercent = percent
                                lastProgressBytes = extractedBytes
                            }
                        }
                        output.flush()
                    }
                } catch (e: IOException) {
                    throw e
                }
            }
        }
    } catch (e: IOException) {
        throw e
    }
    onProgress(expectedBytes.coerceAtLeast(0L), expectedBytes, "abgeschlossen")
    return true
}

private fun cleanupSeaChartDownloadArtifacts(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionUrl: String,
) {
    val regionFolder = seaChartDownloadTargetDirectory(
        context = context,
        mapProvider = mapProvider,
        regionFolderName = seaChartDownloadRegionFolder(regionLabel, regionUrl),
    )
    runCatching {
        if (regionFolder.exists()) {
            regionFolder.deleteRecursively()
        }
        if (!regionFolder.exists() && !regionFolder.mkdirs()) {
            Log.w(TAG_WIDGET_INTERACTION, "SeaCHART-Regionsordner konnte nicht neu angelegt werden: ${regionFolder.absolutePath}")
        }
    }
    seaChartInvalidateSeaChartCaches(mapProvider)
}

private fun seaChartRegionHasExistingFiles(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionUrl: String,
): Boolean {
    return runCatching {
        val regionFolder = seaChartDownloadTargetDirectory(
            context = context,
            mapProvider = mapProvider,
            regionFolderName = seaChartDownloadRegionFolder(regionLabel, regionUrl),
        )
        regionFolder.listFiles()?.isNotEmpty() == true
    }.getOrDefault(false)
}

private fun seaChartDownloadMetadataFile(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionUrl: String,
): File {
    val regionFolder = seaChartDownloadTargetDirectory(
        context = context,
        mapProvider = mapProvider,
        regionFolderName = seaChartDownloadRegionFolder(regionLabel, regionUrl),
    )
    return regionFolder.resolve(SEA_CHART_DOWNLOAD_METADATA_FILE_NAME)
}

private fun seaChartLoadDownloadMetadata(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionUrl: String,
): SeaChartDownloadMetadata? {
    return runCatching {
        val file = seaChartDownloadMetadataFile(
            context = context,
            mapProvider = mapProvider,
            regionLabel = regionLabel,
            regionUrl = regionUrl,
        )
        if (!file.exists() || !file.isFile) return null
        val json = JSONObject(file.readText())
        SeaChartDownloadMetadata(
            fileName = json.optString("fileName", ""),
            downloadedAtMillis = json.optLong("downloadedAtMillis", 0L),
            etag = json.optString("etag", "").ifBlank { null },
            lastModified = json.optString("lastModified", "").ifBlank { null },
            contentLength = json.optLong("contentLength", -1L),
        )
    }.getOrNull()
}

private fun seaChartStoreDownloadMetadata(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionUrl: String,
    metadata: SeaChartDownloadMetadata,
) {
    runCatching {
        val file = seaChartDownloadMetadataFile(
            context = context,
            mapProvider = mapProvider,
            regionLabel = regionLabel,
            regionUrl = regionUrl,
        )
        file.parentFile?.mkdirs()
        val json = JSONObject().apply {
            put("fileName", metadata.fileName)
            put("downloadedAtMillis", metadata.downloadedAtMillis)
            put("etag", metadata.etag ?: JSONObject.NULL)
            put("lastModified", metadata.lastModified ?: JSONObject.NULL)
            put("contentLength", metadata.contentLength)
        }
        file.writeText(json.toString())
    }.onFailure { error ->
        Log.w(TAG_WIDGET_INTERACTION, "Download-Metadaten konnten nicht gespeichert werden.", error)
    }
}

private fun seaChartHasUsableInternetConnection(context: Context): Boolean {
    return runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasUsableTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        hasInternet && hasUsableTransport
    }.getOrElse { error ->
        Log.w(TAG_WIDGET_INTERACTION, "Netzstatus konnte nicht gelesen werden.", error)
        false
    }
}

private fun seaChartCheckRemoteArchiveInfo(
    context: Context,
    url: String,
): SeaChartRemoteCheckResult {
    if (!seaChartHasUsableInternetConnection(context)) {
        return SeaChartRemoteCheckResult(
            status = SeaChartRemoteCheckStatus.OFFLINE,
            message = "Keine Internetverbindung erkannt.",
        )
    }

    fun readInfo(connection: HttpURLConnection): SeaChartRemoteArchiveInfo? {
        val etag = connection.getHeaderField("ETag")?.trim()?.ifBlank { null }
        val lastModified = connection.getHeaderField("Last-Modified")?.trim()?.ifBlank { null }
        val contentLength = runCatching { connection.getHeaderFieldLong("Content-Length", -1L) }.getOrDefault(-1L)
        if (etag == null && lastModified == null && contentLength <= 0L) return null
        return SeaChartRemoteArchiveInfo(
            etag = etag,
            lastModified = lastModified,
            contentLength = contentLength,
        )
    }

    fun open(method: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = method
            setRequestProperty("Accept-Encoding", "identity")
            if (method == "GET") {
                setRequestProperty("Range", "bytes=0-0")
            }
        }
    }

    fun fetch(method: String): SeaChartRemoteArchiveInfo? {
        val connection = open(method)
        return try {
            val responseCode = runCatching { connection.responseCode }.getOrDefault(-1)
            if (responseCode in 200..399 || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                readInfo(connection)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    return try {
        val info = fetch("HEAD") ?: fetch("GET")
        if (info != null) {
            SeaChartRemoteCheckResult(
                status = SeaChartRemoteCheckStatus.OK,
                info = info,
            )
        } else {
            SeaChartRemoteCheckResult(
                status = SeaChartRemoteCheckStatus.UNKNOWN,
                message = "Der Serverstand konnte nicht sicher gelesen werden.",
            )
        }
    } catch (_: SocketTimeoutException) {
        SeaChartRemoteCheckResult(
            status = SeaChartRemoteCheckStatus.UNREACHABLE,
            message = "Server antwortet nicht oder Verbindung zu schwach.",
        )
    } catch (_: ConnectException) {
        SeaChartRemoteCheckResult(
            status = SeaChartRemoteCheckStatus.UNREACHABLE,
            message = "Server nicht erreichbar.",
        )
    } catch (_: UnknownHostException) {
        SeaChartRemoteCheckResult(
            status = SeaChartRemoteCheckStatus.UNREACHABLE,
            message = "Server nicht erreichbar.",
        )
    } catch (_: IOException) {
        SeaChartRemoteCheckResult(
            status = SeaChartRemoteCheckStatus.UNREACHABLE,
            message = "Server nicht erreichbar oder Verbindung zu schwach.",
        )
    } catch (_: Exception) {
        SeaChartRemoteCheckResult(
            status = SeaChartRemoteCheckStatus.UNKNOWN,
            message = "Serverstand konnte nicht sicher geprüft werden.",
        )
    }
}

private fun seaChartDetermineRemoteUpdateStatus(
    localMetadata: SeaChartDownloadMetadata?,
    remoteInfo: SeaChartRemoteArchiveInfo?,
): SeaChartRemoteUpdateStatus {
    if (remoteInfo == null || localMetadata == null) return SeaChartRemoteUpdateStatus.UNKNOWN
    val remoteEtag = remoteInfo.etag
    val localEtag = localMetadata.etag
    if (!remoteEtag.isNullOrBlank() && !localEtag.isNullOrBlank()) {
        return if (remoteEtag == localEtag) {
            SeaChartRemoteUpdateStatus.UNCHANGED
        } else {
            SeaChartRemoteUpdateStatus.UPDATED
        }
    }
    val remoteLastModified = remoteInfo.lastModified
    val localLastModified = localMetadata.lastModified
    if (!remoteLastModified.isNullOrBlank() && !localLastModified.isNullOrBlank()) {
        if (remoteLastModified != localLastModified) return SeaChartRemoteUpdateStatus.UPDATED
        if (remoteInfo.contentLength > 0L && localMetadata.contentLength > 0L) {
            return if (remoteInfo.contentLength == localMetadata.contentLength) {
                SeaChartRemoteUpdateStatus.UNCHANGED
            } else {
                SeaChartRemoteUpdateStatus.UPDATED
            }
        }
        return SeaChartRemoteUpdateStatus.UNCHANGED
    }
    if (remoteInfo.contentLength > 0L && localMetadata.contentLength > 0L) {
        return if (remoteInfo.contentLength == localMetadata.contentLength) {
            SeaChartRemoteUpdateStatus.UNCHANGED
        } else {
            SeaChartRemoteUpdateStatus.UPDATED
        }
    }
    return SeaChartRemoteUpdateStatus.UNKNOWN
}

private fun seaChartDeleteStaleDownloadFile(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionUrl: String,
    fileName: String?,
) {
    val cleanedFileName = fileName?.trim()?.ifBlank { null } ?: return
    runCatching {
        val regionFolder = seaChartDownloadTargetDirectory(
            context = context,
            mapProvider = mapProvider,
            regionFolderName = seaChartDownloadRegionFolder(regionLabel, regionUrl),
        )
        val targetFile = regionFolder.resolve(cleanedFileName)
        if (targetFile.exists() && !targetFile.delete()) {
            Log.w(
                TAG_WIDGET_INTERACTION,
                "SeaCHART-Download-Datei konnte nicht gelöscht werden: ${targetFile.absolutePath}",
            )
        }
    }
    seaChartInvalidateSeaChartCaches(mapProvider)
}

private fun seaChartDeleteFileAtPath(filePath: String?): Boolean {
    val cleanedPath = filePath?.trim()?.ifBlank { null } ?: return false
    val deleted = runCatching {
        val targetFile = File(cleanedPath)
        targetFile.exists() && targetFile.delete()
    }.getOrDefault(false)
    if (deleted) {
        seaChartInvalidateSeaChartCaches()
    }
    return deleted
}

private fun seaChartDeletePathRecursively(filePath: String?): Boolean {
    val cleanedPath = filePath?.trim()?.ifBlank { null } ?: return false
    val deleted = runCatching {
        val targetFile = File(cleanedPath)
        if (!targetFile.exists()) {
            false
        } else {
            targetFile.deleteRecursively()
        }
    }.getOrDefault(false)
    if (deleted) {
        seaChartInvalidateSeaChartCaches()
    }
    return deleted
}

private fun seaChartDownloadTargetFilePath(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionFolderName: String,
    fileName: String,
): String {
    return seaChartDownloadTargetDirectory(
        context = context,
        mapProvider = mapProvider,
        regionFolderName = regionFolderName,
    )
        .resolve(fileName)
        .absolutePath
}

private fun seaChartDownloadTargetFolderName(
    regionLabel: String,
    regionUrl: String,
): String {
    return seaChartDownloadRegionFolder(regionLabel, regionUrl)
}

private fun seaChartDownloadTargetDirectory(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionFolderName: String,
): File {
    val cacheDir = seaChartExternalAppRoot(context) ?: context.filesDir.resolve(SEA_CHART_OFFLINE_CACHE_SUBFOLDER)
    val providerFolder = cacheDir
        .resolve(seaChartProviderSubfolder(mapProvider))
    val regionFolder = providerFolder.resolve(regionFolderName)
    if (!providerFolder.exists() && !providerFolder.mkdirs()) {
        throw IOException("Provider-Ordner konnte nicht angelegt werden.")
    }
    if (!regionFolder.exists() && !regionFolder.mkdirs()) {
        throw IOException("Regions-Ordner konnte nicht angelegt werden.")
    }
    return regionFolder
}

private fun seaChartPendingZipRegionFolderName(
    mapProvider: SeaChartMapProvider,
    zipFile: File,
): String {
    val fallbackName = sanitizeSeaChartDownloadName(
        zipFile.name.substringBeforeLast(".").ifBlank { "seachart" },
    )
    val parent = zipFile.parentFile ?: return fallbackName
    val grandParent = parent.parentFile
    val providerFolder = seaChartProviderSubfolder(mapProvider)
    return when {
        parent.name.equals(providerFolder, ignoreCase = true) ->
            fallbackName
        grandParent?.name.equals(providerFolder, ignoreCase = true) ->
            sanitizeSeaChartDownloadName(parent.name)
        seaChartDirectoryLooksLikeProviderDirectory(parent.name, mapProvider) ->
            sanitizeSeaChartDownloadName(parent.name)
        grandParent != null &&
            seaChartDirectoryLooksLikeProviderDirectory(grandParent.name, mapProvider) ->
            sanitizeSeaChartDownloadName(parent.name)
        else -> fallbackName
    }
}

private fun seaChartNextAvailableZipFile(target: File): File {
    if (!target.exists()) return target
    val fileName = target.nameWithoutExtension.ifBlank { "seachart" }
    val extension = target.extension
    val parent = target.parentFile ?: return target
    var suffix = 1
    while (suffix < 100) {
        val candidateName = if (extension.isBlank()) {
            "${fileName}_${suffix}"
        } else {
            "${fileName}_${suffix}.$extension"
        }
        val candidate = parent.resolve(candidateName)
        if (!candidate.exists()) {
            return candidate
        }
        suffix += 1
    }
    return parent.resolve("${fileName}_${System.currentTimeMillis()}.$extension")
}

private fun seaChartNormalizePendingZipLocation(
    context: Context,
    mapProvider: SeaChartMapProvider,
    zipFile: File,
): File {
    if (!zipFile.exists() || !zipFile.isFile) return zipFile

    val regionFolderName = seaChartPendingZipRegionFolderName(mapProvider, zipFile)
    val targetDir = runCatching {
        seaChartDownloadTargetDirectory(
            context = context,
            mapProvider = mapProvider,
            regionFolderName = regionFolderName,
        )
    }.getOrNull() ?: return zipFile

    val desiredTarget = seaChartNextAvailableZipFile(targetDir.resolve(zipFile.name))
    val sourceCanonical = runCatching { zipFile.canonicalPath }.getOrNull()
    val targetCanonical = runCatching { desiredTarget.canonicalPath }.getOrNull()
    if (sourceCanonical != null && targetCanonical != null && sourceCanonical == targetCanonical) return zipFile

    if (runCatching { zipFile.renameTo(desiredTarget) }.getOrDefault(false)) {
        return desiredTarget
    }

    val copiedFile = runCatching {
        zipFile.copyTo(desiredTarget, overwrite = false)
    }.getOrNull()
    if (copiedFile != null) {
        runCatching { zipFile.delete() }
        return copiedFile
    }

    Log.w(
        TAG_WIDGET_INTERACTION,
        "ZIP-Datei konnte nicht in Standardpfad verschoben werden: ${zipFile.absolutePath} -> ${desiredTarget.absolutePath}",
    )
    return zipFile
}

private fun seaChartDownloadTargetFile(
    context: Context,
    mapProvider: SeaChartMapProvider,
    regionFolderName: String,
    fileName: String,
): File {
    val regionFolder = seaChartDownloadTargetDirectory(
        context = context,
        mapProvider = mapProvider,
        regionFolderName = regionFolderName,
    )
    return regionFolder.resolve(fileName)
}

private fun seaChartDescribeDownloadFailureReason(reason: Int): String {
    return when (reason) {
        DownloadManager.ERROR_CANNOT_RESUME -> "Download kann nicht fortgesetzt werden."
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Download-Server nicht erreichbar."
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Zieldatei existiert bereits."
        DownloadManager.ERROR_FILE_ERROR -> "Dateisystem-Fehler beim Speichern."
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP-Fehler beim Download."
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Nicht genug Speicherplatz."
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Zu viele Weiterleitungen."
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server hat einen nicht unterstützten HTTP-Code gesendet."
        DownloadManager.ERROR_UNKNOWN -> "Unbekannter Download-Fehler."
        else -> "Unbekannter Fehler (Code $reason)."
    }
}

private fun seaChartDownloadRetryHintMessage(
    message: String,
    canRetry: Boolean = true,
): String {
    val normalized = message.lowercase(Locale.ROOT)
    return if (
        normalized.contains("zip") &&
        (normalized.contains("unvollständig") ||
            normalized.contains("beschädigt") ||
            normalized.contains("corrupt") ||
            normalized.contains("crc") ||
            normalized.contains("zlib") ||
            normalized.contains("entpack") ||
            normalized.contains("fehler"))
    ) {
        if (canRetry) {
            "$message\n\nHinweis: Die Datei war nicht vollständig/korrupt. Bitte \"Download wiederholen\" drücken."
        } else {
            "$message\n\nHinweis: Die Datei war nicht vollständig/korrupt. Bitte Datei entfernen und den Download im Kartenmenü erneut starten."
        }
    } else {
        message
    }
}

private fun logSeaChartError(message: String, throwable: Throwable? = null) {
    if (throwable != null) {
        Log.e(TAG_WIDGET_INTERACTION, message, throwable)
    } else {
        Log.e(TAG_WIDGET_INTERACTION, message)
    }
}

private fun logSeaChartInfo(message: String) {
    Log.i(TAG_WIDGET_INTERACTION, message)
}

private suspend fun seaChartMonitorDownloadManager(
    context: Context,
    downloadId: Long,
    onProgress: suspend (downloaded: Long, total: Long) -> Unit,
    onStatus: suspend (String) -> Unit,
): String? {
    val manager = context.getSystemService(DownloadManager::class.java) ?: return "DownloadManager nicht verfügbar."
    val query = DownloadManager.Query().setFilterById(downloadId)
    while (true) {
        val cursor = try {
            manager.query(query)
        } catch (exception: Exception) {
            Log.w(
                TAG_WIDGET_INTERACTION,
                "seaCHART-Downloadmonitor: Fehler beim Abfragen des Status (${downloadId})",
            )
            delay(800)
            continue
        }
        if (cursor == null) {
            delay(800)
            continue
        }
        cursor.use { c ->
            if (!c.moveToFirst()) {
                return "Download nicht gefunden."
            }
            val statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloadedIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON)
            if (statusIndex < 0 || downloadedIndex < 0) {
                return "Download-Zustand unvollständig."
            }
            val status = c.getInt(statusIndex)
            val downloaded = c.getLong(downloadedIndex)
            val total = if (totalIndex >= 0) c.getLong(totalIndex) else -1L
            when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                    val message = if (total > 0L) {
                        "Lädt: ${formatSeaChartMegabytes(downloaded)} von ${formatSeaChartMegabytes(total)}"
                    } else {
                        "Lädt: ${formatSeaChartMegabytes(downloaded)}"
                    }
                    onProgress(downloaded, total)
                    onStatus(message)
                }
                DownloadManager.STATUS_PAUSED -> {
                    val message = if (reasonIndex >= 0) {
                        val reason = c.getInt(reasonIndex)
                        "Download pausiert: ${seaChartDescribeDownloadFailureReason(reason)}"
                    } else {
                        "Download pausiert."
                    }
                    onProgress(downloaded, total)
                    onStatus(message)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    onProgress(downloaded, total)
                    onStatus("Download abgeschlossen.")
                    return null
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = if (reasonIndex >= 0) c.getInt(reasonIndex) else DownloadManager.ERROR_UNKNOWN
                    return seaChartDescribeDownloadFailureReason(reason)
                }
            }
        }
        delay(800)
    }
}

private fun seaChartCancelDownloadManager(context: Context, downloadId: Long) {
    if (downloadId <= SEA_CHART_NO_ACTIVE_DOWNLOAD_ID) return
    val manager = context.getSystemService(DownloadManager::class.java) ?: return
    runCatching {
        manager.remove(downloadId)
    }
}

private suspend fun downloadSeaChartFileWithDownloadManager(
    context: Context,
    url: String,
    fileName: String,
    mapProvider: SeaChartMapProvider,
    regionLabel: String,
    regionFolderName: String? = null,
    existingDownloadId: Long? = null,
    onDownloadId: (Long) -> Unit,
    onPersistentNotice: ((String) -> Unit)? = null,
    onProgress: suspend (downloaded: Long, total: Long) -> Unit,
    onUnpackProgress: (extracted: Long, total: Long) -> Unit = { _, _ -> },
    onStatus: suspend (String) -> Unit,
): File {
    return withContext(Dispatchers.IO) {
        val regionFolder = seaChartDownloadTargetFolderName(regionLabel, url)
        val resolvedRegionFolder = regionFolderName ?: regionFolder
        val destination = seaChartDownloadTargetFile(
            context = context,
            mapProvider = mapProvider,
            regionFolderName = resolvedRegionFolder,
            fileName = fileName,
        )
        val downloadId = existingDownloadId ?: run {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("seaCHART Karten")
                setDescription("Karte: $fileName")
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or
                        DownloadManager.Request.NETWORK_MOBILE,
                )
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationUri(Uri.fromFile(destination))
            }
            val manager = context.getSystemService(DownloadManager::class.java)
                ?: throw IOException("DownloadManager nicht verfügbar.")
            manager.enqueue(request).also { id ->
                onDownloadId(id)
                seaChartStoreDownloadSession(
                    context = context,
                    session = SeaChartDownloadSession(
                        downloadId = id,
                        providerName = mapProvider.name,
                        regionLabel = regionLabel,
                        regionUrl = url,
                        fileName = fileName,
                        regionFolder = resolvedRegionFolder,
                    ),
                )
            }
        }

        withContext(Dispatchers.Main) { onDownloadId(downloadId) }
        val monitorError = seaChartMonitorDownloadManager(
            context = context,
            downloadId = downloadId,
            onProgress = { downloadedBytes, totalBytes ->
                withContext(Dispatchers.Main) {
                    onProgress(downloadedBytes, totalBytes)
                }
            },
            onStatus = { statusText ->
                withContext(Dispatchers.Main) {
                    onStatus(statusText)
                }
            },
        )
        if (monitorError != null) {
            logSeaChartError("seaCHART DownloadManager Fehler: id=$downloadId, message=$monitorError")
            throw IOException(monitorError)
        }
        if (!destination.exists()) {
            logSeaChartError("seaCHART Download-Zieldatei fehlt: ${destination.absolutePath}")
            throw IOException("Download-Datei konnte nicht gefunden werden.")
        }
        if (destination.length() == 0L) {
            logSeaChartError("seaCHART Download-Zieldatei ist leer: ${destination.absolutePath}")
            throw IOException("Download-Datei ist leer.")
        }
        if (destination.extension.equals("zip", ignoreCase = true)) {
            val extractTarget = destination.parentFile?.absolutePath.orEmpty()
            withContext(Dispatchers.Main) { onStatus("ZIP-Prüfung gestartet:\n${destination.name}") }
            runCatching {
                validateSeaChartZipArchive(archiveFile = destination) { validatedBytes, totalBytes, currentEntry ->
                    onUnpackProgress(validatedBytes, totalBytes)
                    val entryLabel = if (currentEntry.isBlank() || currentEntry == "abgeschlossen" || currentEntry == "Vorbereitung") {
                        "Datei: wird ermittelt"
                    } else {
                        "Datei: ${currentEntry.substringAfterLast('/')}"
                    }
                    val statusText = if (totalBytes > 0L) {
                        val percent = ((validatedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                        "Prüfe ZIP: ${formatSeaChartMegabytes(validatedBytes)} von ${formatSeaChartMegabytes(totalBytes)} (${percent.toInt()} %) $entryLabel"
                    } else {
                        "Prüfe ZIP: ${formatSeaChartMegabytes(validatedBytes)} ($entryLabel)"
                    }
                    runCatching {
                        withContext(Dispatchers.Main) {
                            onStatus(statusText)
                            announceSeaChartUnpackStatus(
                                context = context,
                                statusText = statusText,
                                onPersistentNotice = onPersistentNotice,
                            )
                        }
                    }
                }
            }.getOrElse { error ->
                logSeaChartError(
                    "seaCHART ZIP-Prüfung fehlgeschlagen: file=${destination.absolutePath}",
                    error,
                )
                runCatching {
                    withContext(Dispatchers.Main) {
                        val statusText = seaChartDownloadRetryHintMessage(
                            "ZIP-Prüfung fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}",
                        )
                        onStatus(statusText)
                        announceSeaChartUnpackStatus(
                            context = context,
                            statusText = statusText,
                            onPersistentNotice = onPersistentNotice,
                        )
                    }
                }
                throw error
            }
            withContext(Dispatchers.Main) { onStatus("Entpacken gestartet:\n$extractTarget") }
            val extracted = runCatching {
                extractZipSeaChartArchive(
                    archiveFile = destination,
                    destinationDir = destination.parentFile ?: context.filesDir,
                    associatedMapProvider = mapProvider,
                ) { extractedBytes, totalBytes, currentEntry ->
                    onUnpackProgress(extractedBytes, totalBytes)
                    val entryLabel = if (currentEntry.isBlank() || currentEntry == "abgeschlossen" || currentEntry == "Vorbereitung") {
                        "Datei: wird ermittelt"
                    } else {
                        "Datei: ${currentEntry.substringAfterLast('/')}"
                    }
                    val statusText = if (totalBytes > 0L) {
                        val percent = ((extractedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                        "Entpacke: ${formatSeaChartMegabytes(extractedBytes)} von ${formatSeaChartMegabytes(totalBytes)} (${percent.toInt()} %) $entryLabel"
                    } else {
                        "Entpacke: ${formatSeaChartMegabytes(extractedBytes)} ($entryLabel)"
                    }
                    runCatching {
                        withContext(Dispatchers.Main) {
                            onStatus(statusText)
                            announceSeaChartUnpackStatus(
                                context = context,
                                statusText = statusText,
                                onPersistentNotice = onPersistentNotice,
                            )
                        }
                    }
                }
            }.getOrElse { error ->
                logSeaChartError(
                    "seaCHART Entpacken fehlgeschlagen: file=${destination.absolutePath}",
                    error,
                )
                runCatching {
                    withContext(Dispatchers.Main) {
                        val statusText = seaChartDownloadRetryHintMessage(
                            "Entpacken fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}",
                        )
                        onStatus(statusText)
                        announceSeaChartUnpackStatus(
                            context = context,
                            statusText = statusText,
                            onPersistentNotice = onPersistentNotice,
                        )
                    }
                }
                throw error
            }
            if (extracted) {
                withContext(Dispatchers.Main) { onStatus("ENC-Integritätsprüfung gestartet:\n$extractTarget") }
                runCatching {
                    validateSeaChartExtractedCatalogIntegrity(
                        extractionDir = destination.parentFile ?: context.filesDir,
                    ) { validatedBytes, totalBytes, currentEntry ->
                        onUnpackProgress(validatedBytes, totalBytes)
                        val entryLabel = if (
                            currentEntry.isBlank() ||
                            currentEntry == "abgeschlossen" ||
                            currentEntry == "Vorbereitung"
                        ) {
                            "Datei: wird ermittelt"
                        } else {
                            "Datei: ${currentEntry.substringAfterLast('/')}"
                        }
                        val statusText = if (totalBytes > 0L) {
                            val percent = ((validatedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                            "Prüfe ENC-Integrität: ${percent.toInt()} % ($entryLabel)"
                        } else {
                            "Prüfe ENC-Integrität: $entryLabel"
                        }
                        runCatching {
                            withContext(Dispatchers.Main) {
                                onStatus(statusText)
                                announceSeaChartUnpackStatus(
                                    context = context,
                                    statusText = statusText,
                                    onPersistentNotice = onPersistentNotice,
                                )
                            }
                        }
                    }
                }.getOrElse { error ->
                    logSeaChartError(
                        "seaCHART ENC-Integritätsprüfung fehlgeschlagen: dir=${destination.parentFile?.absolutePath.orEmpty()}",
                        error,
                    )
                    runCatching {
                        withContext(Dispatchers.Main) {
                            val statusText = seaChartDownloadRetryHintMessage(
                                "ENC-Integritätsprüfung fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}",
                            )
                            onStatus(statusText)
                            announceSeaChartUnpackStatus(
                                context = context,
                                statusText = statusText,
                                onPersistentNotice = onPersistentNotice,
                            )
                        }
                    }
                    throw error
                }
                withContext(Dispatchers.Main) {
                    onStatus("Karten entpackt und geprüft:\n$extractTarget")
                }
                seaChartClearDownloadSession(context)
                if (mapProvider != null) {
                    seaChartInvalidateSeaChartCaches(mapProvider)
                }
                if (!destination.delete()) {
                    Log.w(TAG_WIDGET_INTERACTION, "ZIP-Datei konnte nicht gelöscht werden: ${destination.absolutePath}")
                }
            }
        }
        if (destination.extension.equals("zip", ignoreCase = true).not()) {
            seaChartClearDownloadSession(context)
        }
        destination
    }
}

private fun formatSeaChartBytes(value: Long): String {
    if (value <= 0L) return "0 KB"
    val kb = value / 1024.0
    if (kb < 1024) return "${kb.roundToInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

private fun formatSeaChartMegabytes(value: Long): String {
    if (value <= 0L) return "0.00 MB"
    return String.format(Locale.US, "%.2f MB", value / 1024.0 / 1024.0)
}

private fun parseSeaChartUnpackPercent(statusText: String): Float? {
    val match = Regex("\\((\\d{1,3})\\s*%\\)").find(statusText) ?: return null
    val percentValue = match.groupValues.getOrNull(1)?.toFloatOrNull() ?: return null
    return percentValue.coerceIn(0f, 100f) / 100f
}

@Synchronized
private fun seaChartCachedSourceScan(
    context: Context,
    mapProvider: SeaChartMapProvider,
): SeaChartSourceScanCacheEntry {
    val now = currentSeaChartCacheStampMs()
    seaChartSourceScanCache[mapProvider]?.let { cached ->
        if (now - cached.createdAtMs < SEA_CHART_SOURCE_SCAN_CACHE_TTL_MS) {
            return cached
        }
    }

    val scanStartMs = android.os.SystemClock.elapsedRealtime()
    val roots = seaChartProviderCacheRoots(context, mapProvider)
    val sourceCandidatePaths = roots.flatMap { root ->
        discoverSeaChartFiles(root, mapProvider)
    }.distinctBy { file -> file.absolutePath.lowercase() }
        .let { files ->
            if (mapProvider == SeaChartMapProvider.NOAA || mapProvider == SeaChartMapProvider.S57 || mapProvider == SeaChartMapProvider.S63) {
                // ENC providers: skip expensive sort (no stat calls needed, catalog handles selection)
                files.sortedBy { it.absolutePath.lowercase() }
            } else {
                files.sortedWith(
                    compareByDescending<File> { file ->
                        isSeaChartRenderableFile(file, mapProvider)
                    }.thenByDescending { it.lastModified() }
                        .thenBy { it.absolutePath.lowercase() },
                )
            }
        }
        .map { it.absolutePath }

    val renderableSourceCandidateEntries = if (mapProvider == SeaChartMapProvider.NOAA || mapProvider == SeaChartMapProvider.S57 || mapProvider == SeaChartMapProvider.S63) {
        // ENC .000 files are not tile sources — skip expensive per-file checks
        emptyList()
    } else {
        sourceCandidatePaths
            .asSequence()
            .mapNotNull { path -> runCatching { File(path) }.getOrNull() }
            .mapNotNull { file ->
                seaChartLocalTileTemplateFromSource(file)?.let { template ->
                    file.absolutePath to template
                }
            }
            .sortedWith(
                compareByDescending<Pair<String, String>> { pair ->
                    isSeaChartRenderableFile(File(pair.first), mapProvider)
                }.thenByDescending { pair ->
                    File(pair.first).lastModified()
                },
            )
            .toList()
    }

    android.util.Log.i(
        "seaFOX.SeaChart.Trace",
        "sourceScan provider=$mapProvider roots=${roots.size} candidates=${sourceCandidatePaths.size} elapsedMs=${android.os.SystemClock.elapsedRealtime() - scanStartMs}",
    )

    return SeaChartSourceScanCacheEntry(
        createdAtMs = now,
        sourceCandidatePaths = sourceCandidatePaths,
        renderableSourceCandidateEntries = renderableSourceCandidateEntries,
    ).also { cached ->
        seaChartSourceScanCache[mapProvider] = cached
    }
}

private fun seaChartSourceCandidates(context: Context, mapProvider: SeaChartMapProvider): List<String> {
    return seaChartCachedSourceScan(context, mapProvider).sourceCandidatePaths
}

private fun seaChartRenderableSourceCandidates(
    context: Context,
    mapProvider: SeaChartMapProvider,
): List<Pair<File, String>> {
    return seaChartCachedSourceScan(context, mapProvider).renderableSourceCandidateEntries.map { (path, template) ->
        File(path) to template
    }
}

private fun seaChartHasRenderableData(context: Context, mapProvider: SeaChartMapProvider): Boolean {
    return seaChartSourceCandidates(context, mapProvider).asSequence().any { path ->
        runCatching { File(path) }.getOrNull()?.let { file ->
            isSeaChartRenderableFile(file, mapProvider) || isSeaChartNativePreviewFile(file, mapProvider)
        } ?: false
    }
}

private fun isSeaChartNativePreviewFile(file: File, mapProvider: SeaChartMapProvider): Boolean {
    val extension = file.extension.lowercase()
    return when (mapProvider) {
        SeaChartMapProvider.OPEN_SEA_CHARTS -> extension == "shp"
        SeaChartMapProvider.NOAA, SeaChartMapProvider.S57, SeaChartMapProvider.S63 ->
            extension == "txt" || isThreeDigitNoaaIndexExtension(extension)
        SeaChartMapProvider.C_MAP -> false
    }
}

private fun discoverSeaChartFiles(root: File, mapProvider: SeaChartMapProvider): List<File> {
    if (mapProvider == SeaChartMapProvider.NOAA || mapProvider == SeaChartMapProvider.S57 || mapProvider == SeaChartMapProvider.S63) {
        return discoverNoaaEncBaseFiles(root)
    }
    return runCatching {
        root.walkTopDown().maxDepth(SEA_CHART_DISCOVERY_MAX_DEPTH).filter { file ->
            file.isFile &&
                isSeaChartFile(file, mapProvider) &&
                seaChartFileBelongsToProvider(file, mapProvider)
        }.toList()
    }.getOrDefault(emptyList())
}

private fun seaChartFileBelongsToProvider(file: File, mapProvider: SeaChartMapProvider): Boolean {
    val providerFolder = seaChartProviderSubfolder(mapProvider)
    var current = file.parentFile
    while (current != null) {
        if (current.name.equals(providerFolder, ignoreCase = true)) {
            return true
        }
        current = current.parentFile
    }
    return false
}

private fun discoverNoaaEncBaseFiles(root: File): List<File> {
    if (!root.exists() || !root.isDirectory) return emptyList()

    val directEncRoots = buildList {
        if (root.name.equals("ENC_ROOT", ignoreCase = true)) {
            add(root)
        }
        root.resolve("ENC_ROOT")
            .takeIf { it.isDirectory }
            ?.let(::add)
        root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.map { candidate -> candidate.resolve("ENC_ROOT") }
            ?.filter { it.isDirectory }
            ?.forEach(::add)
    }.distinctBy { it.absolutePath.lowercase() }

    // Filesystem walk to find .000 ENC files
    fun listBaseFiles(encRoot: File): Sequence<File> {
        return encRoot.listFiles()
            ?.asSequence()
            ?.filter { cellDirectory -> cellDirectory.isDirectory }
            ?.flatMap { cellDirectory ->
                cellDirectory.listFiles()
                    ?.asSequence()
                    ?.filter { candidate ->
                        candidate.isFile &&
                            candidate.extension.equals("000", ignoreCase = true) &&
                            seaChartFileBelongsToProvider(candidate, SeaChartMapProvider.NOAA)
                    }
                    ?: emptySequence()
            }
            ?: emptySequence()
    }

    if (directEncRoots.isNotEmpty()) {
        return directEncRoots
            .asSequence()
            .flatMap(::listBaseFiles)
            .distinctBy { it.absolutePath.lowercase() }
            .toList()
    }

    return runCatching {
        root.walkTopDown()
            .maxDepth(SEA_CHART_DISCOVERY_MAX_DEPTH)
            .filter { entry -> entry.isDirectory && entry.name.equals("ENC_ROOT", ignoreCase = true) }
            .flatMap { encRoot ->
                listBaseFiles(encRoot)
            }
            .toList()
    }.getOrDefault(emptyList())
}

private fun seaChartSearchRoots(context: Context): List<File> {
    val base = ensureSeaChartInternalDirectoryStructure(context)
    val externalBase = ensureSeaChartExternalDirectoryStructure(context)
    val visibleRoots = seaChartVisibleDownloadRootCandidates()
    return buildList {
        add(base)
        SeaChartMapProvider.entries.forEach { provider ->
            add(base.resolve(seaChartProviderSubfolder(provider)))
        }
        if (externalBase != null) {
            add(externalBase)
            SeaChartMapProvider.entries.forEach { provider ->
                add(externalBase.resolve(seaChartProviderSubfolder(provider)))
            }
        }
        visibleRoots.forEach { visibleRoot ->
            if (visibleRoot.exists() && visibleRoot.isDirectory) {
                add(visibleRoot)
                SeaChartMapProvider.entries.forEach { provider ->
                    val providerRoot = visibleRoot.resolve(seaChartProviderSubfolder(provider))
                    if (providerRoot.exists() && providerRoot.isDirectory) {
                        add(providerRoot)
                    }
                }
            }
        }
    }.distinct()
        .filter { it.exists() && it.isDirectory }
}

fun ensureSeaChartInternalDirectoryStructure(context: Context): File {
    val baseFolder = context.filesDir.resolve(SEA_CHART_OFFLINE_CACHE_SUBFOLDER)
    val providerFolders = SeaChartMapProvider.entries.map { provider ->
        baseFolder.resolve(seaChartProviderSubfolder(provider))
    }
    (providerFolders + baseFolder).forEach { folder ->
        if (!folder.exists() && !folder.mkdirs()) {
            Log.w(TAG_WIDGET_INTERACTION, "SeaCHART-Ordner konnte nicht angelegt werden: ${folder.absolutePath}")
        }
    }
    return baseFolder
}

private fun ensureSeaChartExternalDirectoryStructure(context: Context): File? {
    val baseFolder = seaChartExternalAppRoot(context) ?: return null
    val providerFolders = SeaChartMapProvider.entries.map { provider ->
        baseFolder.resolve(seaChartProviderSubfolder(provider))
    }
    (providerFolders + baseFolder).forEach { folder ->
        if (!folder.exists() && !folder.mkdirs()) {
            Log.w(TAG_WIDGET_INTERACTION, "SeaCHART-External-Ordner konnte nicht angelegt werden: ${folder.absolutePath}")
        }
    }
    return baseFolder
}

private fun seaChartDirectoryLooksLikeProviderDirectory(
    directoryName: String,
    mapProvider: SeaChartMapProvider,
): Boolean {
    val name = directoryName.lowercase()
    return when (mapProvider) {
        SeaChartMapProvider.OPEN_SEA_CHARTS ->
            name.contains("opensea") ||
                name.contains("seachart") ||
                name.contains("open sea") ||
                name.contains("openchart") ||
                name.contains("opensea chart")
        SeaChartMapProvider.C_MAP ->
            name.contains("cmap") ||
                name.contains("c-map") ||
                name.contains("c_map")
        SeaChartMapProvider.NOAA ->
            name.contains("noaa") ||
                name.contains("nautical") ||
                name.contains("coast") ||
                name.contains("mariners")
        SeaChartMapProvider.S57 ->
            name.contains("s57") ||
                name.contains("s-57") ||
                name.contains("enc")
        SeaChartMapProvider.S63 ->
            name.contains("s63") ||
                name.contains("s-63")
    }
}

private fun isThreeDigitNoaaIndexExtension(extension: String): Boolean {
    return extension.length == 3 && extension.all { char -> char in '0'..'9' }
}

private fun isNoaaRecognizedFileExtension(extension: String): Boolean {
    return extension in SEA_CHART_NOAA_RECOGNIZED_EXTENSIONS || isThreeDigitNoaaIndexExtension(extension)
}

private fun isSeaChartFile(file: File, mapProvider: SeaChartMapProvider): Boolean {
    val name = file.name.lowercase()
    val extension = file.extension.lowercase()
    val matchesExtension = SEA_CHART_FILE_EXTENSIONS.any { ext -> name.endsWith(".$ext") } ||
        ((mapProvider == SeaChartMapProvider.NOAA || mapProvider == SeaChartMapProvider.S57 || mapProvider == SeaChartMapProvider.S63) && isThreeDigitNoaaIndexExtension(extension))
    if (!matchesExtension) return false
    return when (mapProvider) {
        SeaChartMapProvider.OPEN_SEA_CHARTS ->
            extension in SEA_CHART_OPEN_SEA_CHARTS_RECOGNIZED_EXTENSIONS || name.endsWith(".zip")
        SeaChartMapProvider.C_MAP ->
            extension in SEA_CHART_C_MAP_RECOGNIZED_EXTENSIONS || name.endsWith(".zip")
        SeaChartMapProvider.NOAA, SeaChartMapProvider.S57, SeaChartMapProvider.S63 ->
            isNoaaRecognizedFileExtension(extension) || name.endsWith(".zip")
    }
}

private fun isSeaChartRenderableFile(file: File, mapProvider: SeaChartMapProvider): Boolean {
    if (!file.isFile) return false
    val extension = file.extension.lowercase()
    return when (mapProvider) {
        SeaChartMapProvider.OPEN_SEA_CHARTS,
        SeaChartMapProvider.C_MAP,
        SeaChartMapProvider.NOAA,
        SeaChartMapProvider.S57,
        SeaChartMapProvider.S63 -> extension in SEA_CHART_RENDERABLE_FILE_EXTENSIONS
    }
}

private fun seaChartSourceCompatibilityNote(file: File, mapProvider: SeaChartMapProvider): String? {
    if (SEA_CHART_TEST_MODE_DISABLE_SOURCE_WARNINGS) return null
    val extension = file.extension.lowercase()
    if (extension.isBlank()) return null
    if (isSeaChartRenderableFile(file, mapProvider)) return null
    if (isSeaChartNativePreviewFile(file, mapProvider)) {
        return when (mapProvider) {
            SeaChartMapProvider.OPEN_SEA_CHARTS ->
                "Native Vorschau aus Shapefile-Basisdaten aktiv"
            SeaChartMapProvider.NOAA, SeaChartMapProvider.S57, SeaChartMapProvider.S63 ->
                "Native Vorschau aus ENC-Basisdaten aktiv"
            SeaChartMapProvider.C_MAP -> null
        }
    }
    if ((mapProvider == SeaChartMapProvider.NOAA || mapProvider == SeaChartMapProvider.S57 || mapProvider == SeaChartMapProvider.S63) && isThreeDigitNoaaIndexExtension(extension)) {
        return "Numerische ENC-Datei gefunden"
    }
    return when (extension) {
        "zip" -> "ZIP vor der finalen Verarbeitung"
        "at5" -> "AT5-Datei gefunden, wird vom nativen Viewer aktuell noch nicht gelesen"
        "shp", "shx", "dbf", "prj", "cpg" ->
            "Shapefile-Basisdaten erkannt"
        "kap", "img" ->
            "Datei gefunden, aber dieses Kartenformat hat noch keinen nativen Leser"
        else -> "Datei gefunden, aber Dateiformat \"$extension\" ist aktuell nicht angebunden"
    }
}

private fun seaChartDisplayStatusMessage(
    context: Context,
    mapProvider: SeaChartMapProvider,
    targetSummary: String,
): String {
    if (SEA_CHART_TEST_MODE_DISABLE_SOURCE_WARNINGS) return targetSummary

    val renderableCandidates = seaChartRenderableSourceCandidates(context, mapProvider)
    if (renderableCandidates.isNotEmpty()) return targetSummary

    val nativePreviewCandidate = seaChartSourceCandidates(context, mapProvider)
        .asSequence()
        .map(::File)
        .firstOrNull { file -> isSeaChartNativePreviewFile(file, mapProvider) }
    if (nativePreviewCandidate != null) {
        val previewReason = when (mapProvider) {
            SeaChartMapProvider.OPEN_SEA_CHARTS ->
                "Anzeigegrund: Shapefile-Basisdaten gefunden. Die App zeigt dafür aktuell eine native Vorschau."
            SeaChartMapProvider.NOAA, SeaChartMapProvider.S57, SeaChartMapProvider.S63 ->
                "Anzeigegrund: ENC-Basisdaten gefunden. Die App zeigt dafür aktuell eine native Vorschau."
            SeaChartMapProvider.C_MAP ->
                "Anzeigegrund: C-MAP-Basisdaten gefunden, aber noch ohne native Vorschau."
        }
        return "$targetSummary\n$previewReason"
    }

    val firstCandidate = seaChartSourceCandidates(context, mapProvider)
        .asSequence()
        .map(::File)
        .firstOrNull { file -> file.exists() }
    val reason = when {
        firstCandidate == null ->
            "Grund: Es wurde noch keine passende Kartendatei im erwarteten Ordner gefunden."
        else -> seaChartSourceCompatibilityNote(firstCandidate, mapProvider)
            ?.let { note -> "Grund: $note." }
            ?: "Grund: Die Kartendatei wurde gefunden, kann aber aktuell nicht zugeordnet werden."
    }
    return "$targetSummary\n$reason"
}

private fun activeSeaChartSourceLabel(
    context: Context,
    mapProvider: SeaChartMapProvider,
    preferredPath: String? = null,
): String {
    preferredPath
        ?.let(::File)
        ?.takeIf { it.exists() }
        ?.let { file ->
            val name = file.name.ifBlank { "unbekannte Karte" }
            val compatibilityNote = seaChartSourceCompatibilityNote(file, mapProvider)
            return if (compatibilityNote.isNullOrBlank()) name else "$name ($compatibilityNote)"
        }

    val renderableCandidates = seaChartRenderableSourceCandidates(context, mapProvider)
    if (renderableCandidates.isNotEmpty()) {
        val name = renderableCandidates.first().first.name
        return if (name.isBlank()) "unbekannte Karte" else name
    }

    val nativePreviewCandidate = seaChartSourceCandidates(context, mapProvider)
        .asSequence()
        .map(::File)
        .firstOrNull { file -> isSeaChartNativePreviewFile(file, mapProvider) }
    if (nativePreviewCandidate != null) {
        val name = nativePreviewCandidate.name.ifBlank { "unbekannte Karte" }
        val compatibilityNote = seaChartSourceCompatibilityNote(nativePreviewCandidate, mapProvider)
        return if (compatibilityNote.isNullOrBlank()) name else "$name ($compatibilityNote)"
    }

    val candidates = seaChartSourceCandidates(context, mapProvider)
    if (candidates.isEmpty()) return ""
    val primary = File(candidates.first())
    val name = if (primary.exists()) primary.name else candidates.first()
    val compatibilityNote = if (primary.exists()) {
        seaChartSourceCompatibilityNote(primary, mapProvider)
    } else {
        null
    }
    return if (name.isBlank()) {
        "unbekannte Karte"
    } else if (!compatibilityNote.isNullOrBlank()) {
        "$name ($compatibilityNote)"
    } else {
        name
    }
}

private fun activeSeaChartSourcePath(
    context: Context,
    mapProvider: SeaChartMapProvider,
    ownLatitude: Float? = null,
    ownLongitude: Float? = null,
    selectionCenterLatitude: Double? = null,
    selectionCenterLongitude: Double? = null,
    zoomLevel: Int? = null,
): String? {
    val cachedSources = seaChartCachedSourceScan(context, mapProvider)
    val catalogSelectedEncSource = if (
        mapProvider == SeaChartMapProvider.NOAA ||
        mapProvider == SeaChartMapProvider.S57 ||
        mapProvider == SeaChartMapProvider.S63
    ) {
        selectEncSourceFromCatalog(
            context = context,
            mapProvider = mapProvider,
            sourceCandidatePaths = cachedSources.sourceCandidatePaths,
            ownLatitude = ownLatitude,
            ownLongitude = ownLongitude,
            selectionCenterLatitude = selectionCenterLatitude,
            selectionCenterLongitude = selectionCenterLongitude,
            zoomLevel = zoomLevel,
        )
    } else {
        null
    }
    val preferredNoaaEncSource = if (mapProvider == SeaChartMapProvider.NOAA || mapProvider == SeaChartMapProvider.S57 || mapProvider == SeaChartMapProvider.S63) {
        preferredNoaaEncSourcePath(context, cachedSources.sourceCandidatePaths)
    } else {
        null
    }

    if (!catalogSelectedEncSource.isNullOrBlank()) {
        return catalogSelectedEncSource
    }

    if (!preferredNoaaEncSource.isNullOrBlank()) {
        return preferredNoaaEncSource
    }

    val renderableSource = seaChartRenderableSourceCandidates(context, mapProvider)
        .firstOrNull()
        ?.let { it.first.absolutePath }
    if (!renderableSource.isNullOrBlank()) return renderableSource

    val nativePreviewSource = seaChartSourceCandidates(context, mapProvider)
        .asSequence()
        .map(::File)
        .firstOrNull { file -> isSeaChartNativePreviewFile(file, mapProvider) }
        ?.absolutePath
    if (!nativePreviewSource.isNullOrBlank()) return nativePreviewSource

    if (!preferredNoaaEncSource.isNullOrBlank()) {
        return preferredNoaaEncSource
    }

    return cachedSources.sourceCandidatePaths
        .firstOrNull()
}

private fun selectEncSourceFromCatalog(
    context: Context,
    mapProvider: SeaChartMapProvider,
    sourceCandidatePaths: List<String>,
    ownLatitude: Float? = null,
    ownLongitude: Float? = null,
    selectionCenterLatitude: Double? = null,
    selectionCenterLongitude: Double? = null,
    zoomLevel: Int? = null,
): String? {
    if (
        mapProvider != SeaChartMapProvider.NOAA &&
        mapProvider != SeaChartMapProvider.S57 &&
        mapProvider != SeaChartMapProvider.S63
    ) {
        return null
    }

    val candidateFiles = sourceCandidatePaths.asSequence()
        .mapNotNull { path -> runCatching { File(path) }.getOrNull() }
        .filter { file -> file.isFile && file.extension.equals("000", ignoreCase = true) }
        .toList()
    if (candidateFiles.isEmpty()) return null

    val catalogFile = findCatalog031File(context, mapProvider, candidateFiles) ?: return null
    val document = Catalog031Parser.parse(catalogFile)
    if (document.entries.isEmpty()) return null

    val selectionBounds = if (
        selectionCenterLatitude != null &&
        selectionCenterLongitude != null
    ) {
        GeoBounds(
            west = selectionCenterLongitude,
            south = selectionCenterLatitude,
            east = selectionCenterLongitude,
            north = selectionCenterLatitude,
        )
    } else if (
        ownLatitude != null && ownLongitude != null &&
        ownLatitude.isFinite() && ownLongitude.isFinite()
    ) {
        GeoBounds(
            west = ownLongitude.toDouble(),
            south = ownLatitude.toDouble(),
            east = ownLongitude.toDouble(),
            north = ownLatitude.toDouble(),
        )
    } else {
        null
    }

    val matchedPath = document.preferredRelativePaths(bounds = selectionBounds, zoom = zoomLevel, limit = 20)
        .firstNotNullOfOrNull { relativePath ->
            candidateFiles.firstOrNull { file ->
                catalogPathMatchesCandidate(relativePath, file)
            }?.absolutePath
        }

    if (!matchedPath.isNullOrBlank()) {
        Log.i(
            TAG_WIDGET_INTERACTION,
            "ENC selection via CATALOG.031 provider=$mapProvider catalog=${catalogFile.absolutePath} selected=$matchedPath lat=$ownLatitude lon=$ownLongitude centerLat=$selectionCenterLatitude centerLon=$selectionCenterLongitude zoom=$zoomLevel",
        )
    }

    return matchedPath
}

private fun findCatalog031File(
    context: Context,
    mapProvider: SeaChartMapProvider,
    candidateFiles: List<File>,
): File? {
    val directCandidate = candidateFiles.asSequence()
        .flatMap { file ->
            generateSequence(file.parentFile) { current -> current.parentFile }
                .take(5)
        }
        .map { directory -> directory.resolve("CATALOG.031") }
        .firstOrNull { file -> file.isFile }
    if (directCandidate != null) return directCandidate

    return seaChartProviderCacheRoots(context, mapProvider).asSequence()
        .flatMap { root ->
            sequenceOf(
                root.resolve("CATALOG.031"),
                root.resolve("ENC_ROOT").resolve("CATALOG.031"),
            )
        }
        .firstOrNull { file -> file.isFile }
}

private fun catalogPathMatchesCandidate(relativePath: String, candidateFile: File): Boolean {
    val normalizedCatalogPath = relativePath
        .replace('\\', '/')
        .trimStart('/')
        .lowercase(Locale.ROOT)
    val normalizedCandidatePath = candidateFile.absolutePath
        .replace('\\', '/')
        .lowercase(Locale.ROOT)

    if (normalizedCandidatePath.endsWith(normalizedCatalogPath)) return true

    val encRootMarker = "/enc_root/"
    val encRootIndex = normalizedCandidatePath.indexOf(encRootMarker)
    if (encRootIndex >= 0) {
        val relativeCandidate = normalizedCandidatePath.substring(encRootIndex + encRootMarker.length)
        if (relativeCandidate == normalizedCatalogPath || relativeCandidate.endsWith("/$normalizedCatalogPath")) {
            return true
        }
    }

    val candidateTail = "${candidateFile.parentFile?.name}/${candidateFile.name}".lowercase(Locale.ROOT)
    return normalizedCatalogPath.endsWith(candidateTail)
}

private fun preferredNoaaEncSourcePath(
    context: Context,
    sourceCandidatePaths: List<String>,
): String? {
    val internalNoaaRoot = ensureSeaChartInternalDirectoryStructure(context)
        .resolve(seaChartProviderSubfolder(SeaChartMapProvider.NOAA))
    val internalRootPath = internalNoaaRoot.absolutePath

    return sourceCandidatePaths
        .asSequence()
        .mapNotNull { runCatching { File(it) }.getOrNull() }
        .filter { file ->
            file.isFile && file.extension.equals("000", ignoreCase = true)
        }
        .distinctBy { file -> file.absolutePath.lowercase(Locale.ROOT) }
        .map { file ->
            val regionRoot = noaaRegionRoot(file)
            val regionName = regionRoot.name.lowercase(Locale.ROOT)
            val cellLastModified = noaaCellLastModified(file)
            val regionLastModified = maxOf(regionRoot.lastModified(), cellLastModified)
            val hasUpdates = noaaCellHasUpdates(file)
            val updateCount = noaaCellUpdateCount(file)
            val isRegionSpecific = !regionName.contains("all_enc", ignoreCase = true)
            val isPreferredRegion07 =
                regionName.contains("noaa_-_07_", ignoreCase = true) ||
                    regionName.contains("07_cgd", ignoreCase = true) ||
                    regionName.contains("07cgd", ignoreCase = true)
            val isInternal = file.absolutePath.startsWith(internalRootPath, ignoreCase = true)
            val previewNameRank = noaaPreviewNameRank(file)
            val previewScaleRank = noaaPreviewScaleRank(file)
            NoaaEncSourceCandidate(
                file = file,
                isPreferredRegion07 = isPreferredRegion07,
                isRegionSpecific = isRegionSpecific,
                regionLastModified = regionLastModified,
                cellLastModified = cellLastModified,
                hasUpdates = hasUpdates,
                updateCount = updateCount,
                previewNameRank = previewNameRank,
                previewScaleRank = previewScaleRank,
                isInternal = isInternal,
            )
        }
        .sortedWith(
            compareByDescending<NoaaEncSourceCandidate> { it.isPreferredRegion07 }
                .thenByDescending { it.isRegionSpecific }
                .thenByDescending { it.hasUpdates }
                .thenByDescending { it.previewNameRank }
                .thenByDescending { it.previewScaleRank }
                .thenByDescending { it.updateCount }
                .thenByDescending { it.file.length() }
                .thenByDescending { it.regionLastModified }
                .thenByDescending { it.cellLastModified }
                .thenByDescending { it.isInternal }
                .thenBy { it.file.absolutePath.lowercase(Locale.ROOT) },
        )
        .map { it.file.absolutePath }
        .firstOrNull()
}

private data class NoaaEncSourceCandidate(
    val file: File,
    val isPreferredRegion07: Boolean,
    val isRegionSpecific: Boolean,
    val regionLastModified: Long,
    val cellLastModified: Long,
    val hasUpdates: Boolean,
    val updateCount: Int,
    val previewNameRank: Int,
    val previewScaleRank: Int,
    val isInternal: Boolean,
)

private fun noaaRegionRoot(file: File): File {
    var current = file.parentFile ?: return file
    while (current.parentFile != null) {
        if (current.name.equals("ENC_ROOT", ignoreCase = true)) {
            return current.parentFile ?: current
        }
        current = current.parentFile ?: break
    }
    return file.parentFile ?: file
}

private fun noaaCellHasUpdates(file: File): Boolean {
    val parent = file.parentFile ?: return false
    val stem = file.nameWithoutExtension
    return parent.listFiles()?.any { sibling ->
        sibling.isFile &&
            sibling.nameWithoutExtension.equals(stem, ignoreCase = true) &&
            sibling.extension.length == 3 &&
            sibling.extension.all(Char::isDigit) &&
            !sibling.extension.equals("000", ignoreCase = true)
    } == true
}

private fun noaaCellUpdateCount(file: File): Int {
    val parent = file.parentFile ?: return 0
    val stem = file.nameWithoutExtension
    return parent.listFiles()?.count { sibling ->
        sibling.isFile &&
            sibling.nameWithoutExtension.equals(stem, ignoreCase = true) &&
            sibling.extension.length == 3 &&
            sibling.extension.all(Char::isDigit) &&
            !sibling.extension.equals("000", ignoreCase = true)
    } ?: 0
}

private fun noaaPreviewScaleRank(file: File): Int {
    val usageBand = file.nameWithoutExtension
        .uppercase(Locale.ROOT)
        .getOrNull(2)
        ?.digitToIntOrNull()
        ?: return 0
    return when (usageBand) {
        2, 3, 4, 5 -> 2
        1 -> 1
        6 -> 0
        else -> 0
    }
}

private fun noaaPreviewNameRank(file: File): Int {
    val stem = file.nameWithoutExtension.uppercase(Locale.ROOT)
    return when {
        stem.endsWith("M") -> 2
        stem.lastOrNull()?.isLetter() == true -> 1
        else -> 0
    }
}

private fun noaaCellLastModified(file: File): Long {
    val parent = file.parentFile ?: return file.lastModified()
    val stem = file.nameWithoutExtension
    return parent.listFiles()
        ?.asSequence()
        ?.filter { sibling ->
            sibling.isFile &&
                sibling.nameWithoutExtension.equals(stem, ignoreCase = true) &&
                sibling.extension.length == 3 &&
                sibling.extension.all(Char::isDigit)
        }
        ?.maxOfOrNull { sibling -> sibling.lastModified() }
        ?: file.lastModified()
}

private fun activeSeaChartTileTemplate(context: Context, mapProvider: SeaChartMapProvider): String? {
    val now = currentSeaChartCacheStampMs()
    seaChartTileTemplateCache[mapProvider]?.let { cached ->
        if (now - cached.createdAtMs < SEA_CHART_SOURCE_SCAN_CACHE_TTL_MS) {
            return cached.template
        }
    }

    val sourcePath = activeSeaChartSourcePath(context, mapProvider)
    val tileTemplateFromSource = sourcePath?.let { path ->
        seaChartLocalTileTemplateFromSource(File(path))
    }
    val resolvedTemplate = tileTemplateFromSource ?: activeSeaChartTileTemplateFromProvider(context, mapProvider)
    val mappedTemplate = seaChartTileTemplateToSeafoxWebTemplate(resolvedTemplate)
    seaChartTileTemplateCache[mapProvider] = SeaChartTileTemplateCacheEntry(
        createdAtMs = now,
        template = mappedTemplate,
    )
    return mappedTemplate
}

private fun seaChartTileTemplateToSeafoxWebTemplate(template: String?): String? {
    if (template == null) return null
    if (!template.startsWith("file://")) return template
    val marker = "/{z}/{x}/{y}."
    val markerIndex = template.indexOf(marker)
    if (markerIndex < 0) return template
    val basePart = template.substring(0, markerIndex)
    val extension = template.substring(markerIndex + marker.length).trim().ifBlank { return null }
    val basePath = runCatching {
        URLDecoder.decode(basePart.removePrefix("file://"), "UTF-8")
    }.getOrNull()?.let { decodedPath ->
        File(decodedPath).absolutePath
    } ?: return null
    val encodedBase = runCatching {
        Base64.encodeToString(basePath.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
    }.getOrNull() ?: return null
    return "https://$SEA_CHART_WEB_TILE_HOST/$SEA_CHART_WEB_TILE_ROUTE/$encodedBase/{z}/{x}/{y}.$extension"
}

private fun activeSeaChartTileTemplateFromProvider(context: Context, mapProvider: SeaChartMapProvider): String? {
    val providerRoots = seaChartProviderCacheRoots(context, mapProvider)

    return providerRoots.firstNotNullOfOrNull { root ->
        seaChartTileTemplateFromDirectory(root)
    }
}

private fun seaChartProviderCacheRoots(context: Context, mapProvider: SeaChartMapProvider): List<File> {
    val now = currentSeaChartCacheStampMs()
    seaChartProviderRootCache[mapProvider]?.let { cached ->
        if (now - cached.createdAtMs < SEA_CHART_SOURCE_SCAN_CACHE_TTL_MS) {
            return cached.roots
        }
    }

    val providerFolder = seaChartProviderSubfolder(mapProvider)
    val candidateRoots = seaChartSearchRoots(context)

    val providerRoots = candidateRoots.flatMap { root ->
        buildList {
            if (root.name.equals(providerFolder, ignoreCase = true)) {
                add(root)
            }
            add(root.resolve(providerFolder))
        }
    }.distinct()
        .filter { it.exists() && it.isDirectory }

    seaChartProviderRootCache[mapProvider] = SeaChartProviderRootCacheEntry(
        createdAtMs = now,
        roots = providerRoots,
    )
    return providerRoots
}

private fun findSeaChartRegionDirectory(root: File, file: File): File {
    var regionRoot = file.parentFile ?: return root
    while (regionRoot.parentFile != null && regionRoot.parentFile != root) {
        regionRoot = regionRoot.parentFile ?: break
    }
    return regionRoot
}

private fun seaChartDownloadedRegionName(file: File, regionDirectory: File, root: File): String {
    return if (regionDirectory == root) {
        file.name.substringBeforeLast('.').ifBlank { file.name }
    } else {
        regionDirectory.name
    }
}

private fun seaChartLocalTileTemplateFromSource(source: File): String? {
    if (!source.exists()) return null
    if (source.isDirectory) {
        return seaChartTileTemplateFromDirectory(source)
    }
    if (!source.isFile) return null
    return detectSeaChartTileTemplateFromFile(source)
        ?: seaChartTileTemplateFromDirectory(source.parentFile ?: return null)
}

private fun seaChartProviderDisplayName(mapProvider: SeaChartMapProvider): String = when (mapProvider) {
    SeaChartMapProvider.C_MAP -> "C-Map"
    SeaChartMapProvider.OPEN_SEA_CHARTS -> "OpenSeaCharts"
    SeaChartMapProvider.NOAA -> "NOAA"
    SeaChartMapProvider.S57 -> "S-57"
    SeaChartMapProvider.S63 -> "S-63"
}

private fun selectableSeaChartProviders(): List<SeaChartMapProvider> {
    return ChartProviderRegistry.selectableProviders()
}

private fun normalizedSelectableSeaChartProvider(mapProvider: SeaChartMapProvider): SeaChartMapProvider {
    val selectable = selectableSeaChartProviders()
    return when {
        mapProvider in selectable -> mapProvider
        SeaChartMapProvider.NOAA in selectable -> SeaChartMapProvider.NOAA
        else -> selectable.firstOrNull() ?: SeaChartMapProvider.NOAA
    }
}

private fun seaChartProviderOptionLabel(mapProvider: SeaChartMapProvider): String {
    val descriptor = ChartProviderRegistry.descriptor(mapProvider)
    return "${mapProvider.label} (${descriptor.availability.label})"
}

private fun seaChartPendingZipFiles(
    context: Context,
    mapProvider: SeaChartMapProvider,
): List<SeaChartPendingZipInfo> {
    val providerRoots = seaChartProviderCacheRoots(context, mapProvider)
    return providerRoots.flatMap { root ->
        runCatching {
            root.walkTopDown().maxDepth(SEA_CHART_DISCOVERY_MAX_DEPTH).filter { file ->
                file.isFile && file.extension.equals("zip", ignoreCase = true)
            }.map { zip ->
                seaChartNormalizePendingZipLocation(context, mapProvider, zip)
            }.filter { zip ->
                zip.exists() && zip.isFile && zip.extension.equals("zip", ignoreCase = true)
            }.map { zip ->
                SeaChartPendingZipInfo(
                    provider = mapProvider,
                    fileName = zip.name,
                    filePath = zip.absolutePath,
                    destinationPath = (zip.parentFile ?: root).absolutePath,
                    sizeBytes = zip.length(),
                )
            }.toList()
        }.getOrDefault(emptyList())
    }.distinctBy { it.filePath.lowercase(Locale.ROOT) }
        .sortedWith(
            compareBy<SeaChartPendingZipInfo>(
                { seaChartProviderDisplayName(it.provider) },
                { it.fileName.lowercase(Locale.ROOT) },
            ),
        )
}

private fun seaChartRetryErrorStateForPendingZip(
    mapProvider: SeaChartMapProvider,
    zipFile: File,
    message: String,
): SeaChartDownloadErrorState? {
    val knownDownload = SeaChartDownloadCatalog.entries
        .asSequence()
        .filter { seaChartDownloadCatalogProvider(it) == mapProvider }
        .flatMap { seaChartDownloadCatalogRegions(it).asSequence() }
        .firstOrNull { (regionLabel, regionUrl, _) ->
            seaChartDownloadFileName(regionLabel, regionUrl).equals(zipFile.name, ignoreCase = true)
        } ?: return null

    return SeaChartDownloadErrorState(
        message = seaChartDownloadRetryHintMessage(message, canRetry = true),
        canRetry = true,
        provider = mapProvider,
        regionLabel = knownDownload.first,
        regionUrl = knownDownload.second,
        fileName = zipFile.name,
        filePath = zipFile.absolutePath,
    )
}

private val SEA_CHART_TILE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "mvt", "pbf")
private const val SEA_CHART_MBTILES_FILE_EXTENSION = "mbtiles"

private fun seaChartTileTemplateFromDirectory(sourceDir: File): String? {
    return runCatching {
        var rasterTemplate: String? = null
        var vectorTemplate: String? = null
        sourceDir.walkTopDown().maxDepth(8).forEach { file ->
            if (!file.isFile) return@forEach
            val extension = file.extension.lowercase().ifBlank { return@forEach }
            if (!SEA_CHART_TILE_EXTENSIONS.contains(extension)) return@forEach

            val template = detectSeaChartTileTemplateFromFile(file) ?: return@forEach
            when {
                extension in SEA_CHART_RASTER_TILE_EXTENSIONS && rasterTemplate == null -> rasterTemplate = template
                extension in SEA_CHART_VECTOR_TILE_EXTENSIONS && vectorTemplate == null -> vectorTemplate = template
            }
            if (rasterTemplate != null && vectorTemplate != null) {
                return@forEach
            }
        }
        rasterTemplate ?: vectorTemplate
    }.getOrNull()
}

private fun detectSeaChartTileTemplateFromFile(file: File): String? {
    if (!file.exists() || !file.isFile) return null
    val ext = file.extension.lowercase()
    if (ext == SEA_CHART_MBTILES_FILE_EXTENSION) {
        return "file://${file.absolutePath.replace("\\", "/").replace(" ", "%20")}/{z}/{x}/{y}.mvt"
    }
    if (!SEA_CHART_TILE_EXTENSIONS.contains(ext)) return null
    val yName = file.nameWithoutExtension
    val xDir = file.parentFile ?: return null
    val zDir = xDir.parentFile ?: return null
    if (yName.toIntOrNull() == null || xDir.name.toIntOrNull() == null || zDir.name.toIntOrNull() == null) {
        return null
    }
    val baseDir = zDir.parentFile ?: return null
    return "file://${baseDir.absolutePath.replace("\\", "/").replace(" ", "%20")}/{z}/{x}/{y}.$ext"
}

private suspend fun importSeaChartPendingZipFiles(
    context: Context,
    mapProvider: SeaChartMapProvider,
    onStatus: (String) -> Unit,
    onPersistentNotice: ((String) -> Unit)? = null,
    onRetryableError: ((SeaChartDownloadErrorState) -> Unit)? = null,
    alreadyProcessedZipPaths: MutableSet<String>? = null,
): Int = withContext(Dispatchers.IO) {
    val providerRoots = seaChartProviderCacheRoots(context, mapProvider)
    var importedCount = 0

    providerRoots.forEach { root ->
        val zipFiles = runCatching {
            root.walkTopDown().maxDepth(8).filter { file ->
                file.isFile && file.extension.equals("zip", ignoreCase = true)
            }.map { zip ->
                seaChartNormalizePendingZipLocation(context, mapProvider, zip)
            }.filter { zip ->
                zip.exists() && zip.isFile && zip.extension.equals("zip", ignoreCase = true)
            }.toList()
        }.getOrDefault(emptyList())

        zipFiles.forEach { zip ->
            val normalizedZip = zip
            val normalizedPathKey = normalizedZip.absolutePath.lowercase(Locale.ROOT)
            if (alreadyProcessedZipPaths != null && alreadyProcessedZipPaths.contains(normalizedPathKey)) {
                return@forEach
            }
            alreadyProcessedZipPaths?.add(normalizedPathKey)

            val destinationDir = normalizedZip.parentFile ?: return@forEach
            val destinationPath = destinationDir.absolutePath
            val destinationName = normalizedZip.name
            withContext(Dispatchers.Main) {
                onStatus("ZIP-Prüfung gestartet:\n$destinationName")
                onPersistentNotice?.invoke("ZIP-Prüfung gestartet:\n$destinationName")
            }
            val zipValidated = runCatching<Boolean> {
                validateSeaChartZipArchive(
                    archiveFile = normalizedZip,
                ) { validatedBytes, totalBytes, currentEntry ->
                    val entryLabel = if (currentEntry.isBlank() || currentEntry == "abgeschlossen" || currentEntry == "Vorbereitung") {
                        "Datei: wird ermittelt"
                    } else {
                        "Datei: ${currentEntry.substringAfterLast('/')}"
                    }
                    val statusText = if (totalBytes > 0L) {
                        val percent = ((validatedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                        "Prüfe ZIP: ${formatSeaChartMegabytes(validatedBytes)} von ${formatSeaChartMegabytes(totalBytes)} (${percent.toInt()} %) $entryLabel"
                    } else {
                        "Prüfe ZIP: ${formatSeaChartMegabytes(validatedBytes)} ($entryLabel)"
                    }
                    withContext(Dispatchers.Main) {
                        onStatus(statusText)
                        onPersistentNotice?.invoke(statusText)
                    }
                }
            }.getOrElse { error ->
                logSeaChartError(
                    "seaCHART Pending-ZIP Prüfung fehlgeschlagen: file=${normalizedZip.absolutePath}",
                    error,
                )
                withContext(Dispatchers.Main) {
                    val errorMessage = "ZIP-Prüfung fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}"
                    val retryErrorState = seaChartRetryErrorStateForPendingZip(
                        mapProvider = mapProvider,
                        zipFile = normalizedZip,
                        message = errorMessage,
                    )
                    val statusText = seaChartDownloadRetryHintMessage(
                        errorMessage,
                        canRetry = retryErrorState != null,
                    )
                    onStatus(statusText)
                    if (retryErrorState != null) {
                        onRetryableError?.invoke(retryErrorState)
                    } else {
                        onPersistentNotice?.invoke(statusText)
                    }
                }
                false
            }
            if (!zipValidated) {
                withContext(Dispatchers.Main) {
                    onStatus("Datei übersprungen (defekt): ${zip.name}")
                    Log.w(
                        TAG_WIDGET_INTERACTION,
                        "ZIP-Datei defekt, Überspringen nach Prüfung: ${zip.absolutePath} (${zip.length()} Bytes)",
                    )
                }
                return@forEach
            }
            withContext(Dispatchers.Main) {
                onStatus("Entpacken gestartet:\n$destinationName\nnach:\n$destinationPath")
                onPersistentNotice?.invoke("Entpacken gestartet:\n$destinationName\nnach:\n$destinationPath")
            }
            val extracted = runCatching<Boolean> {
                extractZipSeaChartArchive(
                    archiveFile = normalizedZip,
                    destinationDir = destinationDir,
                    associatedMapProvider = mapProvider,
                ) { extractedBytes, totalBytes, currentEntry ->
                    val entryLabel = if (currentEntry.isBlank() || currentEntry == "abgeschlossen" || currentEntry == "Vorbereitung") {
                        "Datei: wird ermittelt"
                    } else {
                        "Datei: ${currentEntry.substringAfterLast('/')}"
                    }
                    val statusText = if (totalBytes > 0L) {
                        val percent = ((extractedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                        "Entpacke: ${formatSeaChartMegabytes(extractedBytes)} von ${formatSeaChartMegabytes(totalBytes)} (${percent.toInt()} %) $entryLabel"
                    } else {
                        "Entpacke: ${formatSeaChartMegabytes(extractedBytes)} ($entryLabel)"
                    }
                    withContext(Dispatchers.Main) {
                        onStatus(statusText)
                        onPersistentNotice?.invoke(statusText)
                    }
                }
            }.getOrElse { error ->
                logSeaChartError(
                    "seaCHART Pending-ZIP Entpacken fehlgeschlagen: file=${normalizedZip.absolutePath}",
                    error,
                )
                withContext(Dispatchers.Main) {
                    val errorMessage = "Entpacken fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}"
                    val retryErrorState = seaChartRetryErrorStateForPendingZip(
                        mapProvider = mapProvider,
                        zipFile = normalizedZip,
                        message = errorMessage,
                    )
                    val statusText = seaChartDownloadRetryHintMessage(
                        errorMessage,
                        canRetry = retryErrorState != null,
                    )
                    onStatus(statusText)
                    if (retryErrorState != null) {
                        onRetryableError?.invoke(retryErrorState)
                    } else {
                        onPersistentNotice?.invoke(statusText)
                    }
                }
                false
            }
            if (!extracted) {
                withContext(Dispatchers.Main) {
                    onStatus("Datei übersprungen (defekt): ${zip.name}")
                    Log.w(
                        TAG_WIDGET_INTERACTION,
                        "ZIP-Datei defekt, Überspringen: ${zip.absolutePath} (${zip.length()} Bytes)",
                    )
                }
            } else {
                val integrityValidated = runCatching<Boolean> {
                    validateSeaChartExtractedCatalogIntegrity(
                        extractionDir = destinationDir,
                    ) { validatedBytes, totalBytes, currentEntry ->
                        val entryLabel = if (
                            currentEntry.isBlank() ||
                            currentEntry == "abgeschlossen" ||
                            currentEntry == "Vorbereitung"
                        ) {
                            "Datei: wird ermittelt"
                        } else {
                            "Datei: ${currentEntry.substringAfterLast('/')}"
                        }
                        val statusText = if (totalBytes > 0L) {
                            val percent = ((validatedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                            "Prüfe ENC-Integrität: ${percent.toInt()} % ($entryLabel)"
                        } else {
                            "Prüfe ENC-Integrität: $entryLabel"
                        }
                        withContext(Dispatchers.Main) {
                            onStatus(statusText)
                            onPersistentNotice?.invoke(statusText)
                        }
                    }
                }.getOrElse { error ->
                    logSeaChartError(
                        "seaCHART Pending-ZIP ENC-Integritätsprüfung fehlgeschlagen: dir=${destinationDir.absolutePath}",
                        error,
                    )
                    withContext(Dispatchers.Main) {
                        val errorMessage = "ENC-Integritätsprüfung fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}"
                        val retryErrorState = seaChartRetryErrorStateForPendingZip(
                            mapProvider = mapProvider,
                            zipFile = normalizedZip,
                            message = errorMessage,
                        )
                        val statusText = seaChartDownloadRetryHintMessage(
                            errorMessage,
                            canRetry = retryErrorState != null,
                        )
                        onStatus(statusText)
                        if (retryErrorState != null) {
                            onRetryableError?.invoke(retryErrorState)
                        } else {
                            onPersistentNotice?.invoke(statusText)
                        }
                    }
                    false
                }
                if (!integrityValidated) {
                    return@forEach
                }
                withContext(Dispatchers.Main) {
                    onStatus("Karten entpackt und geprüft:\n$destinationPath")
                    onPersistentNotice?.invoke("Karten entpackt und geprüft:\n$destinationPath")
                }
                seaChartInvalidateSeaChartCaches(mapProvider)
                if (!normalizedZip.delete()) {
                    Log.w(
                        TAG_WIDGET_INTERACTION,
                        "ZIP-Datei konnte nicht gelöscht werden: ${normalizedZip.absolutePath}",
                    )
                }
                importedCount += 1
            }
        }
    }
    importedCount
}

private fun seaChartDownloadedRegions(
    context: Context,
    mapProvider: SeaChartMapProvider,
): List<SeaChartDownloadedRegionInfo> {
    val providerRoots = seaChartProviderCacheRoots(context, mapProvider)
    val regionEntries = providerRoots.flatMap { root ->
        runCatching {
            discoverSeaChartFiles(root, mapProvider).mapNotNull { file ->
                val regionDirectory = findSeaChartRegionDirectory(root, file)
                SeaChartDownloadedRegionInfo(
                    regionName = seaChartDownloadedRegionName(file, regionDirectory, root),
                    lastLoadedAtMillis = file.lastModified(),
                    fileName = file.name,
                    filePath = regionDirectory.absolutePath,
                    isRenderable = isSeaChartRenderableFile(file, mapProvider),
                    formatNote = seaChartSourceCompatibilityNote(file, mapProvider),
                )
            }
        }.getOrDefault(emptyList())
    }
    return regionEntries
        .groupBy { it.filePath }
        .mapNotNull { (_, entries) ->
            entries.maxByOrNull { it.lastLoadedAtMillis }
        }.distinctBy { it.filePath }
        .sortedByDescending { it.lastLoadedAtMillis }
}

private fun formatSeaChartDownloadTimestamp(millis: Long): String {
    if (millis <= 0L) return "unbekannt"
    return runCatching {
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(millis))
    }.getOrDefault("unbekannt")
}

private fun formatSeaChartRegionLabel(regionName: String): String {
    return regionName
        .replace("_", " ")
        .replace("-", " ")
        .replace(".zip", "")
        .trim()
        .ifBlank { regionName }
}

@Composable
private fun <T> OptionSelectorRow(
    selectedOption: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    style: TextStyle,
    enabled: Boolean = true,
    darkBackground: Boolean = true,
    triggerThreshold: Int = 4,
) {
    if (options.isEmpty()) return

    val needsDropdown = options.size > triggerThreshold

    if (needsDropdown) {
        var showMenu by rememberSaveable { mutableStateOf(false) }
        Box {
            CompactMenuTextButton(
                text = "${optionLabel(selectedOption)} ▾",
                style = style,
                fillWidth = false,
                enabled = enabled,
                onClick = { if (enabled) showMenu = true },
            )
            DropdownMenu(
                modifier = Modifier.background(if (darkBackground) Color.Black else Color.White),
                expanded = enabled && showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), style = style) },
                        onClick = {
                            if (!enabled) return@DropdownMenuItem
                            onOptionSelected(option)
                            showMenu = false
                        }
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                modifier = Modifier.padding(end = MENU_SPACING)
            ) {
                RadioButton(
                    enabled = enabled,
                    selected = selectedOption == option,
                    onClick = {
                        if (!enabled) return@RadioButton
                        onOptionSelected(option)
                    },
                    colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                )
                Text(optionLabel(option), style = style)
            }
        }
    }
}


@Composable
private fun CompactMenuTextButton(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SeaFoxDesignTokens.Size.compactMenuItemPaddingHorizontal,
        vertical = SeaFoxDesignTokens.Size.compactMenuItemPaddingVertical,
    ),
    fillWidth: Boolean = true,
    minHeight: Dp = SeaFoxDesignTokens.Size.compactMenuItemHeight,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val resolvedColor = if (style.color != Color.Unspecified) style.color else contentColor
    val activeColor = if (enabled) resolvedColor else resolvedColor.copy(alpha = 0.4f)
    val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    Box(
        modifier = modifier
            .then(widthModifier)
            .heightIn(min = minHeight)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding)
    ) {
        Text(
            text,
            style = style.copy(
                color = activeColor,
                fontWeight = SeaFoxDesignTokens.Type.compactMenuButtonFontWeight
            ),
            color = activeColor,
            maxLines = 1
        )
    }
}

@Composable
private fun ScrollableMenuTextContent(
    state: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    val canScroll = state.maxValue > 0
    val thumbHeightPx = if (viewportHeightPx > 0f && canScroll) {
        (viewportHeightPx * viewportHeightPx / (viewportHeightPx + state.maxValue.toFloat())).coerceIn(16f, viewportHeightPx)
    } else {
        0f
    }
    val maxThumbOffset = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
    val thumbOffsetPx = if (canScroll && maxThumbOffset > 0f) {
        (state.value.toFloat() / state.maxValue.toFloat()).coerceIn(0f, 1f) * maxThumbOffset
    } else {
        0f
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { layoutCoordinates ->
                    viewportHeightPx = layoutCoordinates.size.height.toFloat()
                }
                .fillMaxWidth()
                .padding(end = if (canScroll) SeaFoxDesignTokens.Size.menuDropdownItemLeadingPaddingEnd else 0.dp)
                .verticalScroll(state)
        ) {
            content()
        }

        if (canScroll && viewportHeightPx > 0f) {
            val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            val thumbHeight = with(density) { thumbHeightPx.toDp() }
            val thumbOffset = with(density) { thumbOffsetPx.toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = SeaFoxDesignTokens.Size.menuDropdownScrollbarVerticalPadding)
                    .width(SeaFoxDesignTokens.Size.menuDropdownScrollbarWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(SeaFoxDesignTokens.Size.menuDropdownScrollbarTrackWidth)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(trackColor, shape = RoundedCornerShape(SeaFoxDesignTokens.Size.menuDropdownTrackRadius))
                )
                Box(
                    modifier = Modifier
                        .width(SeaFoxDesignTokens.Size.menuDropdownScrollbarWidth)
                        .height(thumbHeight)
                        .align(Alignment.TopEnd)
                        .offset(y = thumbOffset)
                        .background(thumbColor, shape = RoundedCornerShape(SeaFoxDesignTokens.Size.menuDropdownThumbRadius))
                )
            }
        }
    }
}

@Composable
private fun CompactMenuDropdownItem(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingSymbol: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SeaFoxDesignTokens.Size.compactMenuItemPaddingHorizontal,
        vertical = SeaFoxDesignTokens.Size.compactMenuItemPaddingVertical,
    ),
    minHeight: Dp = SeaFoxDesignTokens.Size.compactMenuItemHeight,
) {
    val resolvedColor = if (style.color != Color.Unspecified) style.color else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clickable(onClick = onClick)
            .padding(contentPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingSymbol != null) {
                Text(
                    text = leadingSymbol,
                    style = style.copy(
                        color = resolvedColor
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(SeaFoxDesignTokens.Size.menuDropdownLeadingSymbolSpacing))
            }
            Text(
                text,
                style = style.copy(
                    color = resolvedColor,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                color = resolvedColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactMenuDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    isDarkMenu: Boolean = true,
) {
    val menuShape = RoundedCornerShape(18.dp)
    val menuContainerColor = if (isDarkMenu) {
        SeaFoxDesignTokens.Color.surfaceRaisedDark
    } else {
        SeaFoxDesignTokens.Color.surfaceRaisedLight
    }
    val menuTextColor = if (isDarkMenu) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val menuBorderColor = if (isDarkMenu) {
        SeaFoxDesignTokens.Color.hairlineDark
    } else {
        SeaFoxDesignTokens.Color.hairlineLight
    }
    val menuTextScrollState = rememberScrollState()
    val menuContentPadding = SeaFoxDesignTokens.Size.menuContentPadding
    AlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Card(
                modifier = Modifier.border(1.dp, menuBorderColor, menuShape),
                shape = menuShape,
                colors = CardDefaults.cardColors(
                    containerColor = menuContainerColor,
                )
            ) {
                Column(
                    modifier = Modifier.padding(menuContentPadding),
                    verticalArrangement = Arrangement.spacedBy(MENU_SPACING)
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(color = menuTextColor)
                    ) {
                        title()
                    }
                    HorizontalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = SeaFoxDesignTokens.Size.menuDropdownContentMaxHeight)
                    ) {
                        ScrollableMenuTextContent(state = menuTextScrollState) {
                            CompositionLocalProvider(
                                LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(color = menuTextColor)
                            ) {
                                text()
                            }
                        }
                    }
                    if (menuTextScrollState.maxValue > 0) {
                        HorizontalDivider()
                    }
                    if (confirmButton != null || dismissButton != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = SeaFoxDesignTokens.Size.menuSectionSpacing),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (dismissButton != null) {
                                dismissButton()
                            }
                            if (confirmButton != null && dismissButton != null) {
                                Spacer(modifier = Modifier.width(SeaFoxDesignTokens.Size.menuSectionSpacing))
                            }
                            if (confirmButton != null) {
                                confirmButton()
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DashboardTopBar(
    state: DashboardState,
    darkBackground: Boolean,
    onSelectPage: (Int) -> Unit,
    onAddPage: (String) -> Unit,
    onRenamePage: (Int, String) -> Unit,
    onAddWidget: (WidgetKind, Float, Float, Float) -> Boolean,
    onAddWidgetError: (String) -> Unit,
    onToggleBackground: () -> Unit,
    onToggleRouterSimulation: () -> Unit,
    onUpdateUiFont: (UiFont) -> Unit,
    onUpdateFontScale: (Float) -> Unit,
    onUpdateGridStepPercent: (Float) -> Unit,
    onUpdateLayoutOrientation: (DashboardLayoutOrientation) -> Unit,
    onUpdateAlarmToneVolume: (Float) -> Unit,
    onUpdateAlarmRepeatIntervalSeconds: (Int) -> Unit,
    onUpdateAlarmVoiceAnnouncementsEnabled: (Boolean) -> Unit,
    onUpdateAlarmVoiceProfileIndex: (Int) -> Unit,
    onUpdateWidgetFrameStyle: (WidgetFrameStyle) -> Unit,
    onUpdateWidgetFrameStyleGrayOffset: (Int) -> Unit,
    onUpdateNmeaRouter: (String, Int, NmeaRouterProtocol) -> Unit,
    onUpdateNmeaSourceDisplayName: (String, String) -> Unit,
    onClearNmeaSourceDisplayName: (String) -> Unit,
    onRemoveNmeaSourceProfile: (String) -> Unit,
    onUpdateBoatProfile: (BoatProfile) -> Unit,
    onUpdateBackupPrivacyMode: (BackupPrivacyMode) -> Unit,
    onUpdateBootAutostartEnabled: (Boolean) -> Unit,
    onUpdateRouterSimulationOrigin: (Float, Float) -> Unit,
    onClearStoredData: () -> Unit,
    onRestoreStoredData: () -> Boolean,
    onLoadReleaseNotes: () -> String,
    onLoadReleaseNoteCount: () -> Int,
    onExitApp: () -> Unit,
    onTriggerAnchorWatchAlarm: () -> Unit,
    onTriggerAlarmSettingsTest: (Float, Boolean, Int) -> Unit = { _, _, _ -> },
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showPageSelectionDialog by rememberSaveable { mutableStateOf(false) }
    var menuStage by rememberSaveable { mutableStateOf("main") }
    var showAddPage by rememberSaveable { mutableStateOf(false) }
    var addPageName by rememberSaveable { mutableStateOf("") }
    var showSystemDialog by rememberSaveable { mutableStateOf(false) }
    var showRouterDialog by rememberSaveable { mutableStateOf(false) }
    var showRouterHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showAlarmSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showBoatProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showDetectedSourcesDialog by rememberSaveable { mutableStateOf(false) }
    var showUsedPgnsDialog by rememberSaveable { mutableStateOf(false) }
    var showAdapterProgrammingDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showWidgetsDialog by rememberSaveable { mutableStateOf(false) }
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreDataDialog by rememberSaveable { mutableStateOf(false) }
    var showReleaseNotesDialog by rememberSaveable { mutableStateOf(false) }
    var showUiFontMenu by rememberSaveable { mutableStateOf(false) }
    var releaseNotes by remember { mutableStateOf<String?>(null) }
    val headerReleaseCode = BuildConfig.VERSION_NAME
    var selectedUiFont by rememberSaveable { mutableStateOf(state.uiFont) }
    var selectedDarkBackground by rememberSaveable { mutableStateOf(state.darkBackground) }
    var selectedWidgetFrameStyle by rememberSaveable { mutableStateOf(state.widgetFrameStyle) }
    var selectedLayoutOrientation by rememberSaveable { mutableStateOf(state.layoutOrientation) }
    var selectedWidgetFrameStyleGrayOffset by rememberSaveable { mutableIntStateOf(state.widgetFrameStyleGrayOffset) }
    var selectedFontScale by remember { mutableFloatStateOf(state.fontScale) }
    var selectedGridStepPercent by remember { mutableFloatStateOf(state.gridStepPercent.coerceIn(0.5f, 20f)) }
    var selectedAlarmToneVolume by rememberSaveable { mutableFloatStateOf(state.alarmToneVolume) }
    var selectedAlarmRepeatIntervalSeconds by rememberSaveable { mutableIntStateOf(state.alarmRepeatIntervalSeconds.coerceIn(2, 10)) }
    var selectedAlarmVoiceAnnouncementsEnabled by rememberSaveable { mutableStateOf(state.alarmVoiceAnnouncementsEnabled) }
    var selectedAlarmVoiceProfileIndex by rememberSaveable { mutableIntStateOf(state.alarmVoiceProfileIndex.coerceIn(0, ALARM_VOICE_PROFILES.lastIndex)) }
    var selectedBackupPrivacyMode by rememberSaveable { mutableStateOf(state.backupPrivacyMode) }
    var selectedBootAutostartEnabled by rememberSaveable { mutableStateOf(state.bootAutostartEnabled) }
    var boatProfileLengthText by rememberSaveable { mutableStateOf(state.boatProfile.lengthMeters.toString()) }
    var boatProfileWidthText by rememberSaveable { mutableStateOf(state.boatProfile.widthMeters.toString()) }
    var boatProfileNameText by rememberSaveable { mutableStateOf(state.boatProfile.name) }
    var boatProfileHomePortText by rememberSaveable { mutableStateOf(state.boatProfile.homePort) }
    var boatProfileMmsiText by rememberSaveable { mutableStateOf(state.boatProfile.mmsi) }
    var boatProfileDraftText by rememberSaveable { mutableStateOf(state.boatProfile.draftMeters.toString()) }
    var selectedBoatProfileType by rememberSaveable { mutableStateOf(state.boatProfile.type) }
    var routerHost by rememberSaveable { mutableStateOf(state.nmeaRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }) }
    var routerPortText by rememberSaveable { mutableStateOf(state.udpPort.toString()) }
    var selectedRouterMode by rememberSaveable { mutableStateOf(if (state.simulationEnabled) "simulation" else "live") }
    var selectedRouterProtocol by rememberSaveable { mutableStateOf(state.nmeaRouterProtocol) }

    var renamePageIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.boatProfile) {
        boatProfileLengthText = state.boatProfile.lengthMeters.toString()
        boatProfileWidthText = state.boatProfile.widthMeters.toString()
        boatProfileNameText = state.boatProfile.name
        boatProfileHomePortText = state.boatProfile.homePort
        boatProfileMmsiText = state.boatProfile.mmsi
        boatProfileDraftText = state.boatProfile.draftMeters.toString()
        selectedBoatProfileType = state.boatProfile.type
    }

    LaunchedEffect(state.backupPrivacyMode, state.bootAutostartEnabled) {
        selectedBackupPrivacyMode = state.backupPrivacyMode
        selectedBootAutostartEnabled = state.bootAutostartEnabled
    }

    val menuBackgroundColor = if (darkBackground) {
        SeaFoxDesignTokens.Color.topBarDark
    } else {
        SeaFoxDesignTokens.Color.topBarLight
    }
    val menuContentColor = if (darkBackground) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val menuMutedColor = if (darkBackground) SeaFoxDesignTokens.Color.mutedDark else SeaFoxDesignTokens.Color.mutedLight
    val menuBorderColor = if (darkBackground) SeaFoxDesignTokens.Color.hairlineDark else SeaFoxDesignTokens.Color.hairlineLight
    val menuTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp,
        lineHeight = SeaFoxDesignTokens.Size.menuLineHeight,
        color = menuContentColor,
    )
    val menuInputFieldTextStyle = menuTextStyle.copy(
        color = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    )
    val linkTextStyle = menuTextStyle.merge(SeaFoxDesignTokens.Type.linkText)
    val menuTitleStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp,
        lineHeight = SeaFoxDesignTokens.Size.menuLineHeight,
        color = menuContentColor,
    )
    val menuHamburgerBottomOffset = with(LocalDensity.current) { SeaFoxDesignTokens.Size.menuHamburgerBottomOffsetSp.toDp() }
    val menuHamburgerHeight = SeaFoxDesignTokens.Size.menuHamburgerHeight
    val menuHamburgerWidth = menuHamburgerHeight
    val compactMenuItemHeight = SeaFoxDesignTokens.Size.compactMenuItemHeight
    val compactMenuItemModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = compactMenuItemHeight)
        .heightIn(min = compactMenuItemHeight)
    val selectedPageIndex = state.selectedPage.coerceIn(0, state.pages.lastIndex.coerceAtLeast(0))
    val selectedPageTitle = state.pages.getOrNull(selectedPageIndex)?.name ?: "Keine Seite"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(premiumTopBarBrush(darkBackground))
            .border(1.dp, menuBorderColor.copy(alpha = 0.55f), RoundedCornerShape(0.dp))
            .padding(SeaFoxDesignTokens.Size.menuPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = menuHamburgerBottomOffset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        menuStage = "main"
                        showMenu = true
                    },
                    modifier = Modifier.size(menuHamburgerWidth, menuHamburgerHeight),
                ) {
                    HamburgerMenuIcon(
                        color = menuContentColor,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = SeaFoxDesignTokens.Size.menuHamburgerStrokeWidth,
                    )
                }
                Text(
                    text = "($headerReleaseCode)",
                    color = menuMutedColor,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp,
                    )
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = SeaFoxDesignTokens.Size.menuSectionSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SeaFoxDesignTokens.Size.menuSectionSpacing),
            ) {
                Text(
                    text = selectedPageTitle,
                    modifier = Modifier
                        .clickable(enabled = state.pages.isNotEmpty()) {
                            showPageSelectionDialog = true
                        },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.5f,
                        fontWeight = FontWeight.SemiBold,
                        color = menuContentColor
                    ),
                    color = menuContentColor
                )
            }
            Text(
                text = headerReleaseCode,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp * 0.75f,
                    color = menuMutedColor,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = SeaFoxDesignTokens.Size.menuSectionSpacing)
            )
        }

        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
            DropdownMenu(
                modifier = Modifier
                    .background(menuBackgroundColor)
                    .border(1.dp, menuBorderColor, RoundedCornerShape(8.dp)),
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                    menuStage = "main"
                }
            ) {
                when (menuStage) {
                    "main" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            CompactMenuDropdownItem(
                                leadingSymbol = "📄",
                                text = "Seiten \u203A",
                                style = menuTextStyle,
                                onClick = { menuStage = "pages" },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                leadingSymbol = "▤",
                                text = "Widgets",
                                style = menuTextStyle,
                                onClick = {
                                    showWidgetsDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                leadingSymbol = "🚢",
                                text = "Bootsdaten",
                                style = menuTextStyle,
                                onClick = {
                                    boatProfileLengthText = state.boatProfile.lengthMeters.toString()
                                    boatProfileWidthText = state.boatProfile.widthMeters.toString()
                                    boatProfileNameText = state.boatProfile.name
                                    boatProfileHomePortText = state.boatProfile.homePort
                                    boatProfileMmsiText = state.boatProfile.mmsi
                                    boatProfileDraftText = state.boatProfile.draftMeters.toString()
                                    selectedBoatProfileType = state.boatProfile.type
                                    showBoatProfileDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                leadingSymbol = "🎛",
                                text = "Darstellung",
                                style = menuTextStyle,
                                onClick = {
                                    selectedUiFont = state.uiFont
                                    selectedFontScale = state.fontScale
                                    selectedDarkBackground = state.darkBackground
                                    selectedWidgetFrameStyle = state.widgetFrameStyle
                                    selectedLayoutOrientation = state.layoutOrientation
                                    selectedWidgetFrameStyleGrayOffset = state.widgetFrameStyleGrayOffset
                                    selectedGridStepPercent = state.gridStepPercent.coerceIn(0.5f, 20f)
                                    showSystemDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                leadingSymbol = "🚨",
                                text = "Alarmeinstellungen",
                                style = menuTextStyle,
                                onClick = {
                                    selectedAlarmToneVolume = state.alarmToneVolume.coerceIn(0f, 2f)
                                    selectedAlarmRepeatIntervalSeconds = state.alarmRepeatIntervalSeconds.coerceIn(2, 10)
                                    selectedAlarmVoiceAnnouncementsEnabled = state.alarmVoiceAnnouncementsEnabled
                                    selectedAlarmVoiceProfileIndex = state.alarmVoiceProfileIndex.coerceIn(0, ALARM_VOICE_PROFILES.lastIndex)
                                    showAlarmSettingsDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                leadingSymbol = "\u2699",
                                text = "System \u203A",
                                style = menuTextStyle,
                                onClick = { menuStage = "system" },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                leadingSymbol = "⏻",
                                text = "App beenden",
                                style = menuTextStyle,
                                onClick = onExitApp,
                                modifier = compactMenuItemModifier
                            )
                        }
                    }
                    "pages" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            CompactMenuDropdownItem(
                                text = "\u2039 Zur\u00fcck",
                                style = menuTextStyle,
                                onClick = { menuStage = "main" },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            state.pages.forEachIndexed { index, page ->
                                val marker = if (index == state.selectedPage) "• " else ""
                                CompactMenuDropdownItem(
                                    text = "$marker${page.name}",
                                    style = menuTextStyle,
                                    onClick = {
                                        onSelectPage(index)
                                        showMenu = false
                                        menuStage = "main"
                                    },
                                    modifier = compactMenuItemModifier
                                )
                            }
                            if (state.pages.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            CompactMenuDropdownItem(
                                text = "Neue Seite",
                                style = menuTextStyle,
                                onClick = {
                                    showAddPage = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            if (state.selectedPage in state.pages.indices) {
                                CompactMenuDropdownItem(
                                    text = "Seite umbenennen",
                                    style = menuTextStyle,
                                    onClick = {
                                        renamePageIndex = state.selectedPage
                                        renameText = state.pages[state.selectedPage].name
                                        showMenu = false
                                        menuStage = "main"
                                    },
                                    modifier = compactMenuItemModifier
                                )
                            }
                        }
                    }
                    "system" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            CompactMenuDropdownItem(
                                text = "\u2039 Zur\u00fcck",
                                style = menuTextStyle,
                                onClick = { menuStage = "main" },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "PGNs \u203A",
                                style = menuTextStyle,
                                onClick = {
                                    menuStage = "system_pgns"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "WLAN Router",
                                style = menuTextStyle,
                                onClick = {
                                    routerHost = state.nmeaRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }
                                    routerPortText = state.udpPort.toString()
                                    selectedRouterMode = if (state.simulationEnabled) "simulation" else "live"
                                    selectedRouterProtocol = state.nmeaRouterProtocol
                                    showRouterDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Daten \u203A",
                                style = menuTextStyle,
                                onClick = {
                                    menuStage = "system_data"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "App Info",
                                style = menuTextStyle,
                                onClick = {
                                    releaseNotes = onLoadReleaseNotes()
                                    showReleaseNotesDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Ankerwächter Testalarm",
                                style = menuTextStyle,
                                onClick = {
                                    onTriggerAnchorWatchAlarm()
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                        }
                    }
                    "system_pgns" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            CompactMenuDropdownItem(
                                text = "\u2039 Zur\u00fcck",
                                style = menuTextStyle,
                                onClick = { menuStage = "system" },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Verwendete PGNs anzeigen",
                                style = menuTextStyle,
                                onClick = {
                                    showUsedPgnsDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Erkannte Quellen",
                                style = menuTextStyle,
                                onClick = {
                                    showDetectedSourcesDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Adapter Programmieren",
                                style = menuTextStyle,
                                onClick = {
                                    showAdapterProgrammingDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                        }
                    }
                    "system_data" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            CompactMenuDropdownItem(
                                text = "\u2039 Zur\u00fcck",
                                style = menuTextStyle,
                                onClick = { menuStage = "system" },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Datenschutz & Bootmodus",
                                style = menuTextStyle,
                                onClick = {
                                    selectedBackupPrivacyMode = state.backupPrivacyMode
                                    selectedBootAutostartEnabled = state.bootAutostartEnabled
                                    showPrivacyDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            HorizontalDivider()
                            CompactMenuDropdownItem(
                                text = "Daten wiederherstellen",
                                style = menuTextStyle,
                                onClick = {
                                    showRestoreDataDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                            CompactMenuDropdownItem(
                                text = "Daten l\u00f6schen",
                                style = menuTextStyle,
                                onClick = {
                                    showClearDataDialog = true
                                    showMenu = false
                                    menuStage = "main"
                                },
                                modifier = compactMenuItemModifier
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPageSelectionDialog) {
        CompactMenuDialog(
            onDismissRequest = { showPageSelectionDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Seite auswählen", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        if (state.pages.isEmpty()) {
                            Text("Keine Seite vorhanden", style = menuTextStyle)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                state.pages.forEachIndexed { index, page ->
                                    CompactMenuTextButton(
                                        text = if (index == selectedPageIndex) "• ${page.name}" else page.name,
                                        style = menuTextStyle,
                                        onClick = {
                                            onSelectPage(index)
                                            showPageSelectionDialog = false
                                        },
                                        fillWidth = true,
                                        enabled = index != selectedPageIndex,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (state.pages.isNotEmpty()) {
                    CompactMenuTextButton(
                        text = "Schließen",
                        style = menuTextStyle,
                        fillWidth = false,
                        onClick = { showPageSelectionDialog = false },
                    )
                }
            },
        )
    }

    val tryAddWidget: (WidgetKind) -> Unit = { kind ->
        val added = onAddWidget(kind, 0f, 0f, 0f)
        if (!added) onAddWidgetError("Nicht genug Platz für ein neues Widget.")
    }
    var expandedWidgetSection by rememberSaveable { mutableStateOf<String?>("navigation") }

    if (showWidgetsDialog) {
        CompactMenuDialog(
            onDismissRequest = { showWidgetsDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Widgets", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        val sections = widgetCatalogSections()
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            sections.forEachIndexed { index, section ->
                                CompactMenuTextButton(
                                    text = if (expandedWidgetSection == section.id) "${section.title} ▼" else "${section.title} ►",
                                    style = menuTextStyle,
                                    onClick = {
                                        expandedWidgetSection = if (expandedWidgetSection == section.id) null else section.id
                                    },
                                    fillWidth = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (expandedWidgetSection == section.id) {
                                    section.entries.forEach { entry ->
                                        CompactMenuTextButton(
                                            text = entry.menuLabel,
                                            style = menuTextStyle,
                                            onClick = {
                                                tryAddWidget(entry.kind)
                                                showWidgetsDialog = false
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp),
                                        )
                                    }
                                }
                                if (index < sections.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Schließen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showWidgetsDialog = false }
                )
            },
        )
    }

    if (showBoatProfileDialog) {
        val parseBoatDecimal = { input: String ->
            input.trim().replace(",", ".").toFloatOrNull()?.coerceAtLeast(0f)
        }

        val parseBoatTextInput = { input: String ->
            if (input.isBlank()) "" else input.trim()
        }

        CompactMenuDialog(
            onDismissRequest = { showBoatProfileDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Bootsdaten", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            MenuCompactTextField(
                                value = boatProfileLengthText,
                                onValueChange = { boatProfileLengthText = it },
                                label = { Text("Länge (m)", style = menuTextStyle) },
                                singleLine = true,
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                            )
                            MenuCompactTextField(
                                value = boatProfileWidthText,
                                onValueChange = { boatProfileWidthText = it },
                                label = { Text("Breite (m)", style = menuTextStyle) },
                                singleLine = true,
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                            )
                            MenuCompactTextField(
                                value = boatProfileNameText,
                                onValueChange = { boatProfileNameText = it },
                                label = { Text("Name", style = menuTextStyle) },
                                singleLine = true,
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                            )
                            MenuCompactTextField(
                                value = boatProfileHomePortText,
                                onValueChange = { boatProfileHomePortText = it },
                                label = { Text("Heimathafen", style = menuTextStyle) },
                                singleLine = true,
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                            )
                            MenuCompactTextField(
                                value = boatProfileMmsiText,
                                onValueChange = { boatProfileMmsiText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("MMSI", style = menuTextStyle) },
                                singleLine = true,
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                            )
                            MenuCompactTextField(
                                value = boatProfileDraftText,
                                onValueChange = { boatProfileDraftText = it },
                                label = { Text("Tiefgang (m)", style = menuTextStyle) },
                                singleLine = true,
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                            )
                            Text("Typ", style = menuTextStyle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CompactMenuTextButton(
                                    text = if (selectedBoatProfileType == BoatType.MOTORBOOT) "• Motorboot" else "Motorboot",
                                    fillWidth = false,
                                    style = menuTextStyle,
                                    onClick = { selectedBoatProfileType = BoatType.MOTORBOOT },
                                    modifier = Modifier.weight(1f)
                                )
                                CompactMenuTextButton(
                                    text = if (selectedBoatProfileType == BoatType.SEEGELBOOT) "• Segelboot" else "Segelboot",
                                    fillWidth = false,
                                    style = menuTextStyle,
                                    onClick = { selectedBoatProfileType = BoatType.SEEGELBOOT },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Speichern",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        onUpdateBoatProfile(
                            BoatProfile(
                                lengthMeters = parseBoatDecimal(boatProfileLengthText) ?: 0f,
                                widthMeters = parseBoatDecimal(boatProfileWidthText) ?: 0f,
                                name = parseBoatTextInput(boatProfileNameText),
                                homePort = parseBoatTextInput(boatProfileHomePortText),
                                mmsi = parseBoatTextInput(boatProfileMmsiText),
                                draftMeters = parseBoatDecimal(boatProfileDraftText) ?: 0f,
                                type = selectedBoatProfileType,
                            )
                        )
                        showBoatProfileDialog = false
                    },
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        boatProfileLengthText = state.boatProfile.lengthMeters.toString()
                        boatProfileWidthText = state.boatProfile.widthMeters.toString()
                        boatProfileNameText = state.boatProfile.name
                        boatProfileHomePortText = state.boatProfile.homePort
                        boatProfileMmsiText = state.boatProfile.mmsi
                        boatProfileDraftText = state.boatProfile.draftMeters.toString()
                        selectedBoatProfileType = state.boatProfile.type
                        showBoatProfileDialog = false
                    },
                )
            },
        )
    }

    if (showDetectedSourcesDialog) {
        val editableSourceNames = remember(state.detectedNmeaSources) { mutableStateMapOf<String, String>() }
        LaunchedEffect(state.detectedNmeaSources) {
            val currentKeys = state.detectedNmeaSources.associateBy { it.sourceKey }
            editableSourceNames.keys.toList().forEach { key ->
                if (!currentKeys.containsKey(key)) {
                    editableSourceNames.remove(key)
                }
            }
            state.detectedNmeaSources.forEach { profile ->
                if (editableSourceNames[profile.sourceKey] == null || profile.displayName != editableSourceNames[profile.sourceKey]) {
                    editableSourceNames[profile.sourceKey] = profile.displayName
                }
            }
        }
        CompactMenuDialog(
            onDismissRequest = { showDetectedSourcesDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Erkannte Quellen", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        if (state.detectedNmeaSources.isEmpty()) {
                            Text("Noch keine Quellen empfangen.", style = menuTextStyle)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                state.detectedNmeaSources.forEach { profile ->
                                    val draftName = editableSourceNames[profile.sourceKey] ?: profile.displayName
                                    val pgnDescription = profile.pgns.sorted().joinToString(", ") { sourcePgnLine(it) }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = SeaFoxDesignTokens.Size.menuDropdownItemMinHeight),
                                        verticalArrangement = Arrangement.spacedBy(SeaFoxDesignTokens.Size.menuDropdownMiniSpacing)
                                    ) {
                                        Text("Quelle: ${profile.sourceKey}", style = menuTextStyle)
                                        Text(
                                            text = "Status: ${sourceDetectedStatus(profile)}",
                                            style = menuTextStyle,
                                        )
                                        Text(
                                            text = "PGN: ${pgnDescription.ifBlank { "unbekannt" }}",
                                            style = menuTextStyle,
                                        )
                                        MenuCompactTextField(
                                            value = draftName,
                                            onValueChange = { value ->
                                                editableSourceNames[profile.sourceKey] = value
                                            },
                                            label = { Text("Zuordnung", style = menuTextStyle) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .padding(0.dp)
                                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                            textStyle = menuInputFieldTextStyle,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                            shape = MENU_INPUT_FIELD_SHAPE
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)
                                        ) {
                                            CompactMenuTextButton(
                                                text = "Zuordnung speichern",
                                                style = menuTextStyle,
                                                fillWidth = false,
                                                onClick = {
                                                    onUpdateNmeaSourceDisplayName(profile.sourceKey, draftName)
                                                }
                                            )
                                            CompactMenuTextButton(
                                                text = "Automatisch",
                                                style = menuTextStyle,
                                                fillWidth = false,
                                                onClick = {
                                                    onClearNmeaSourceDisplayName(profile.sourceKey)
                                                    val pgnHint = profile.pgns.firstOrNull()?.let { autoNmeaSourcePgnLabels[it] } ?: ""
                                                    editableSourceNames[profile.sourceKey] = pgnHint
                                                }
                                            )
                                            CompactMenuTextButton(
                                                text = "Entfernen",
                                                style = menuTextStyle,
                                                fillWidth = false,
                                                onClick = { onRemoveNmeaSourceProfile(profile.sourceKey) }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Schließen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showDetectedSourcesDialog = false }
                )
            },
        )
    }

    if (showAddPage) {
        CompactMenuDialog(
            onDismissRequest = { showAddPage = false },
            isDarkMenu = darkBackground,
            title = { Text("Neue Seite", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                            MenuCompactTextField(
                                value = addPageName,
                                onValueChange = { addPageName = it },
                                label = { Text("Seitenname", style = menuTextStyle) },
                                singleLine = true,
                            modifier = Modifier
                                .padding(0.dp)
                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE
                            )
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Anlegen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        onAddPage(addPageName)
                        addPageName = ""
                        showAddPage = false
                    }
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showAddPage = false }
                )
            },
        )
    }

    if (renamePageIndex != null) {
        CompactMenuDialog(
            onDismissRequest = { renamePageIndex = null },
            isDarkMenu = darkBackground,
            title = { Text("Seite umbenennen", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                            MenuCompactTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                label = { Text("Neuer Name", style = menuTextStyle) },
                                singleLine = true,
                            modifier = Modifier
                                .padding(0.dp)
                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE
                            )
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Speichern",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        renamePageIndex?.let { onRenamePage(it, renameText) }
                        renamePageIndex = null
                        renameText = ""
                    }
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { renamePageIndex = null }
                )
            },
        )
    }

    if (showUsedPgnsDialog) {
        val usedPgnEntries = remember(state.detectedNmeaSources) {
            state.detectedNmeaSources
                .flatMap { profile -> profile.pgns.distinct().map { pgn -> pgn to profile.sourceKey } }
                .groupBy({ it.first }, { it.second })
                .toSortedMap()
        }.entries.toList()

        CompactMenuDialog(
            onDismissRequest = { showUsedPgnsDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Verwendete PGNs", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        if (usedPgnEntries.isEmpty()) {
                            Text("Noch keine PGNs empfangen.", style = menuTextStyle)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                usedPgnEntries.forEachIndexed { index, (pgn, sources) ->
                                    val pgnLabel = sourcePgnLine(pgn).ifBlank { "unbekannt" }
                                    val sortedSources = sources.distinct().sortedBy { it.lowercase() }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = SeaFoxDesignTokens.Size.menuDropdownItemMinHeight),
                                        verticalArrangement = Arrangement.spacedBy(SeaFoxDesignTokens.Size.menuDropdownMiniSpacing)
                                    ) {
                                        Text("PGN $pgnLabel", style = menuTextStyle)
                                        Text("Quellen: ${sortedSources.joinToString()}", style = menuTextStyle)
                                    }
                                    if (index < usedPgnEntries.lastIndex) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Schließen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showUsedPgnsDialog = false }
                )
            },
        )
    }

    if (showAdapterProgrammingDialog) {
        CompactMenuDialog(
            onDismissRequest = { showAdapterProgrammingDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Adapter Programmieren", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            Text(
                                "Für diese Funktion wird als Nächstes ein eigener Adapter-Programmierungsdialog eingebunden.",
                                style = menuTextStyle,
                            )
                            Text(
                                "Geplante Inhalte: PGN-Auswahl, Pinbelegung, Kabelverweise und Mapping.",
                                style = menuTextStyle,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Schließen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showAdapterProgrammingDialog = false }
                )
            },
        )
    }

    if (showPrivacyDialog) {
        CompactMenuDialog(
            onDismissRequest = { showPrivacyDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Datenschutz & Bootmodus", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            Text("Backup-Privatsphäre", style = menuTextStyle)
                            BackupPrivacyMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mode.label, style = menuTextStyle)
                                        Text(
                                            backupPrivacyModeDescription(mode),
                                            style = menuTextStyle.copy(color = menuMutedColor),
                                        )
                                    }
                                    RadioButton(
                                        selected = selectedBackupPrivacyMode == mode,
                                        onClick = { selectedBackupPrivacyMode = mode },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                    )
                                }
                            }
                            HorizontalDivider()
                            Text("Kiosk-/Boot-Modus", style = menuTextStyle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Autostart nach Gerätestart", style = menuTextStyle)
                                    Text(
                                        "Standard ist aus. Nur aktivieren, wenn das Tablet dauerhaft als Borddisplay läuft.",
                                        style = menuTextStyle.copy(color = menuMutedColor),
                                    )
                                }
                                Switch(
                                    checked = selectedBootAutostartEnabled,
                                    onCheckedChange = { selectedBootAutostartEnabled = it },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Speichern",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        onUpdateBackupPrivacyMode(selectedBackupPrivacyMode)
                        onUpdateBootAutostartEnabled(selectedBootAutostartEnabled)
                        showPrivacyDialog = false
                    }
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        selectedBackupPrivacyMode = state.backupPrivacyMode
                        selectedBootAutostartEnabled = state.bootAutostartEnabled
                        showPrivacyDialog = false
                    }
                )
            },
        )
    }

    if (showSystemDialog) {
        CompactMenuDialog(
            onDismissRequest = {
                showSystemDialog = false
                showUiFontMenu = false
            },
            isDarkMenu = darkBackground,
            title = { Text("System", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            Text("Darstellung", style = menuTextStyle)
                            Row(horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                CompactMenuTextButton(
                                    modifier = Modifier.weight(1f),
                                    fillWidth = false,
                                    text = if (!selectedDarkBackground) "• Hell" else "Hell",
                                    style = menuTextStyle,
                                    onClick = { selectedDarkBackground = false }
                                )
                                CompactMenuTextButton(
                                    modifier = Modifier.weight(1f),
                                    fillWidth = false,
                                    text = if (selectedDarkBackground) "• Dunkel" else "Dunkel",
                                    style = menuTextStyle,
                                    onClick = { selectedDarkBackground = true }
                                )
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(MENU_SPACING))
                            Text("Layout", style = menuTextStyle)
                            Row(horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                CompactMenuTextButton(
                                    modifier = Modifier.weight(1f),
                                    fillWidth = false,
                                    text = if (selectedLayoutOrientation == DashboardLayoutOrientation.PORTRAIT) "• Hochformat" else "Hochformat",
                                    style = menuTextStyle,
                                    onClick = { selectedLayoutOrientation = DashboardLayoutOrientation.PORTRAIT }
                                )
                                CompactMenuTextButton(
                                    modifier = Modifier.weight(1f),
                                    fillWidth = false,
                                    text = if (selectedLayoutOrientation == DashboardLayoutOrientation.LANDSCAPE) "• Querformat" else "Querformat",
                                    style = menuTextStyle,
                                    onClick = { selectedLayoutOrientation = DashboardLayoutOrientation.LANDSCAPE }
                                )
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(MENU_SPACING))
                            Text("Widget Style", style = menuTextStyle)
                            Row(horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = selectedWidgetFrameStyle == WidgetFrameStyle.BORDER,
                                        onClick = { selectedWidgetFrameStyle = WidgetFrameStyle.BORDER },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors()
                                    )
                                    Text("Rahmen", style = menuTextStyle)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = selectedWidgetFrameStyle == WidgetFrameStyle.BACKGROUND,
                                        onClick = { selectedWidgetFrameStyle = WidgetFrameStyle.BACKGROUND },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors()
                                    )
                                    Text("Hintergrundfarbe", style = menuTextStyle)
                                }
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(MENU_SPACING))
                            val baseBackgroundPercent = if (darkBackground) 10 else 95
                            val effectiveBackgroundPercent = (baseBackgroundPercent + selectedWidgetFrameStyleGrayOffset)
                                .coerceIn(0, 100)
                            Text(
                                "Hintergrundhelligkeit: $effectiveBackgroundPercent%",
                                style = menuTextStyle
                            )
                            Slider(
                                value = selectedWidgetFrameStyleGrayOffset.toFloat(),
                                onValueChange = { selectedWidgetFrameStyleGrayOffset = it.roundToInt() },
                                valueRange = -10f..10f,
                                steps = 20,
                            )
                            HorizontalDivider()
                            Spacer(Modifier.height(MENU_SPACING))
                            Text("Schriftart", style = menuTextStyle)
                            if (UiFont.entries.size > 4) {
                                Box {
                                    CompactMenuTextButton(
                                        fillWidth = false,
                                        text = "${uiFontLabel(selectedUiFont)} ▾",
                                        style = menuTextStyle,
                                        onClick = { showUiFontMenu = true },
                                    )
                                    DropdownMenu(
                                        modifier = Modifier.background(if (darkBackground) Color.Black else Color.White),
                                        expanded = showUiFontMenu,
                                        onDismissRequest = { showUiFontMenu = false }
                                    ) {
                                    UiFont.entries.forEach { font ->
                                        DropdownMenuItem(
                                                text = { Text(uiFontLabel(font), style = menuTextStyle) },
                                                onClick = {
                                                    selectedUiFont = font
                                                    showUiFontMenu = false
                                                }
                                            )
                                    }
                                    }
                                }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                    UiFont.entries.forEach { font ->
                                        CompactMenuTextButton(
                                            fillWidth = false,
                                            text = if (selectedUiFont == font) "• ${uiFontLabel(font)}" else uiFontLabel(font),
                                            style = menuTextStyle,
                                            onClick = { selectedUiFont = font }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(MENU_SPACING))
                            val percent = ((selectedFontScale - 1f) * 100f).roundToInt()
                            val sign = if (percent >= 0) "+" else ""
                            Text("Widget Schriftgröße: $sign$percent%")
                            Slider(
                                value = selectedFontScale,
                                onValueChange = { selectedFontScale = it },
                                valueRange = 0.7f..1.6f
                            )
                            HorizontalDivider()
                            Spacer(Modifier.height(MENU_SPACING))
                            val gridPercentRounded = (selectedGridStepPercent * 10f).roundToInt() / 10f
                            Text("Rastergröße: $gridPercentRounded% der Breite")
                            Slider(
                                value = selectedGridStepPercent,
                                onValueChange = {
                                    selectedGridStepPercent = it.coerceIn(0.5f, 20f)
                                },
                                valueRange = 0.5f..20f,
                                steps = 195
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Speichern",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        if (selectedDarkBackground != darkBackground) {
                            onToggleBackground()
                        }
                        onUpdateUiFont(selectedUiFont)
                        onUpdateFontScale(selectedFontScale.coerceIn(0.7f, 1.6f))
                        onUpdateGridStepPercent(selectedGridStepPercent)
                        onUpdateLayoutOrientation(selectedLayoutOrientation)
                        onUpdateWidgetFrameStyle(selectedWidgetFrameStyle)
                        onUpdateWidgetFrameStyleGrayOffset(selectedWidgetFrameStyleGrayOffset)
                        showSystemDialog = false
                        showUiFontMenu = false
                    }
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        showSystemDialog = false
                        showUiFontMenu = false
                    }
                )
            },
        )
    }

    if (showAlarmSettingsDialog) {
        val sliderColors = SliderDefaults.colors(
            activeTrackColor = Color(0xFF2A6BFF),
            inactiveTrackColor = Color(0x4D2A6BFF),
            thumbColor = Color(0xFF2A6BFF),
            activeTickColor = Color(0xFF2A6BFF),
            inactiveTickColor = Color(0x4D2A6BFF),
        )
        val alarmVolumePercent = (selectedAlarmToneVolume.coerceIn(0f, 2f) * 100f).roundToInt()
        val alarmRepeatSeconds = selectedAlarmRepeatIntervalSeconds.coerceIn(2, 10)
        CompactMenuDialog(
            onDismissRequest = {
                showAlarmSettingsDialog = false
            },
            isDarkMenu = darkBackground,
            title = { Text("Alarmeinstellungen", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            Text("Alarmlautstärke einstellen", style = menuTextStyle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    modifier = Modifier.weight(1f),
                                    colors = sliderColors,
                                    value = selectedAlarmToneVolume,
                                    onValueChange = { selectedAlarmToneVolume = it.coerceIn(0f, 2f) },
                                    valueRange = 0f..2f,
                                )
                                Text("$alarmVolumePercent%", style = menuTextStyle)
                            }
                            Text("Wiederholungsinterval", style = menuTextStyle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    modifier = Modifier.weight(1f),
                                    colors = sliderColors,
                                    value = alarmRepeatSeconds.toFloat(),
                                    onValueChange = { selectedAlarmRepeatIntervalSeconds = it.roundToInt().coerceIn(2, 10) },
                                    valueRange = 2f..10f,
                                    steps = 8,
                                )
                                Text("${alarmRepeatSeconds}s", style = menuTextStyle)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Ansage aktivieren", style = menuTextStyle)
                                Switch(
                                    checked = selectedAlarmVoiceAnnouncementsEnabled,
                                    onCheckedChange = { selectedAlarmVoiceAnnouncementsEnabled = it }
                                )
                            }
                            Text("Stimmauswahl", style = menuTextStyle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ALARM_VOICE_PROFILES.forEachIndexed { index, profile ->
                                    CompactMenuTextButton(
                                        modifier = Modifier.weight(1f),
                                        fillWidth = false,
                                        style = menuTextStyle,
                                        text = if (selectedAlarmVoiceProfileIndex == index) "• ${profile.label}" else profile.label,
                                        enabled = selectedAlarmVoiceAnnouncementsEnabled,
                                        onClick = {
                                            selectedAlarmVoiceProfileIndex = index
                                        },
                                    )
                                }
                            }
                            HorizontalDivider()
                            CompactMenuTextButton(
                                text = "Testalarm (Ton + Stimme)",
                                style = menuTextStyle,
                                fillWidth = false,
                                onClick = {
                                    onTriggerAlarmSettingsTest(
                                        selectedAlarmToneVolume,
                                        selectedAlarmVoiceAnnouncementsEnabled,
                                        selectedAlarmVoiceProfileIndex,
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Speichern",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        onUpdateAlarmToneVolume(selectedAlarmToneVolume.coerceIn(0f, 2f))
                        onUpdateAlarmRepeatIntervalSeconds(selectedAlarmRepeatIntervalSeconds.coerceIn(2, 10))
                        onUpdateAlarmVoiceAnnouncementsEnabled(selectedAlarmVoiceAnnouncementsEnabled)
                        onUpdateAlarmVoiceProfileIndex(selectedAlarmVoiceProfileIndex.coerceIn(0, ALARM_VOICE_PROFILES.lastIndex))
                        showAlarmSettingsDialog = false
                    }
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        selectedAlarmToneVolume = state.alarmToneVolume
                        selectedAlarmRepeatIntervalSeconds = state.alarmRepeatIntervalSeconds
                        selectedAlarmVoiceAnnouncementsEnabled = state.alarmVoiceAnnouncementsEnabled
                        selectedAlarmVoiceProfileIndex = state.alarmVoiceProfileIndex
                        showAlarmSettingsDialog = false
                    }
                )
            },
        )
    }

    if (showRouterDialog) {
        val parsedPort = routerPortText.toIntOrNull() ?: state.udpPort
        val normalizedRouterHost = routerHost.trim().ifBlank { DEFAULT_NMEA_ROUTER_HOST }
        val normalizedRouterPort = parsedPort.coerceIn(1, 65535)
        val applyRouterConfig = {
            onUpdateNmeaRouter(normalizedRouterHost, normalizedRouterPort, selectedRouterProtocol)
            routerHost = normalizedRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }
            routerPortText = normalizedRouterPort.toString()
        }
        val requestedSimulation = selectedRouterMode == "simulation"
        val saveRouterSettings = {
            applyRouterConfig()
            if (requestedSimulation != state.simulationEnabled) {
                if (requestedSimulation) {
                    onUpdateRouterSimulationOrigin(state.simulationStartLatitude, state.simulationStartLongitude)
                }
                onToggleRouterSimulation()
            }
            showRouterDialog = false
        }
        CompactMenuDialog(
            onDismissRequest = { showRouterDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("WLAN Router", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            MenuCompactTextField(
                                value = routerHost,
                                onValueChange = { routerHost = it },
                                label = { Text("Router IP/Host", style = menuTextStyle) },
                                singleLine = true,
                            modifier = Modifier
                                .padding(0.dp)
                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE
                            )
                            MenuCompactTextField(
                                value = routerPortText,
                                onValueChange = { newValue ->
                                    routerPortText = newValue.filter { it.isDigit() }.take(5)
                                },
                                label = { Text("Port", style = menuTextStyle) },
                                singleLine = true,
                                modifier = Modifier
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                textStyle = menuInputFieldTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { saveRouterSettings() }
                                ),

                                colors = menuOutlinedTextFieldColors(darkBackground),
                                shape = MENU_INPUT_FIELD_SHAPE
                            )
                            Text("Protokoll", style = menuTextStyle)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedRouterProtocol == NmeaRouterProtocol.TCP,
                                        onClick = { selectedRouterProtocol = NmeaRouterProtocol.TCP },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                    )
                                    Text("TCP", style = menuTextStyle)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedRouterProtocol == NmeaRouterProtocol.UDP,
                                        onClick = { selectedRouterProtocol = NmeaRouterProtocol.UDP },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                    )
                                    Text("UDP", style = menuTextStyle)
                                }
                            }
                            Text("NMEA Quelle", style = menuTextStyle)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = !requestedSimulation,
                                        onClick = { selectedRouterMode = "live" },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                    )
                                    Text("Live Daten", style = menuTextStyle)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = requestedSimulation,
                                        onClick = { selectedRouterMode = "simulation" },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                    )
                                    Text("Simulation", style = menuTextStyle)
                                }
                            }
                            CompactMenuTextButton(
                                text = "Hilfe",
                                style = linkTextStyle,
                                onClick = { showRouterHelpDialog = true }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Speichern",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        saveRouterSettings()
                    }
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showRouterDialog = false }
                )
            },
        )
    }

    if (showRouterHelpDialog) {
        CompactMenuDialog(
            onDismissRequest = { showRouterHelpDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("WLAN Router Hilfe", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            Text("1) Tablet mit Bord-WLAN verbinden (SSID/Passwort vom Boot).", style = menuTextStyle)
                            Text("2) Sicherstellen, dass NMEA2000-Daten im gewählten Protokoll im Bordnetz gesendet werden.", style = menuTextStyle)
                            Text("3) Router/Host-IP aus dem Bordnetz ermitteln (z. B. 192.168.0.208).", style = menuTextStyle)
                            Text("4) In System → WLAN Router Host/IP und Port eintragen.", style = menuTextStyle)
                            Text("5) Speichern tippen. Leeren Host verwenden, wenn alle Quellen akzeptiert werden sollen.", style = menuTextStyle)
                            Text("6) Für den NMEA2000-Empfang den eingestellten Router-Port eintragen (Standard: 2000).", style = menuTextStyle)
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Schließen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showRouterHelpDialog = false }
                )
            },
        )
    }

    if (showClearDataDialog) {
        CompactMenuDialog(
            onDismissRequest = { showClearDataDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Gespeicherte Daten löschen", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Text(
                            "Alle lokalen seaFOX-Daten (Seiten, Widgets, Einstellungen, Layout) unwiderruflich löschen?",
                            style = menuTextStyle,
                        )
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Löschen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        onClearStoredData()
                        showClearDataDialog = false
                    },
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showClearDataDialog = false },
                )
            },
        )
    }

    if (showRestoreDataDialog) {
        CompactMenuDialog(
            onDismissRequest = { showRestoreDataDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("Gespeicherte Daten wiederherstellen", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Text(
                            "Gespeicherte seaFOX-Daten aus dem letzten Backup laden und anwenden?",
                            style = menuTextStyle,
                        )
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Wiederherstellen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = {
                        val restored = onRestoreStoredData()
                        onAddWidgetError(
                            if (restored) {
                                "Daten erfolgreich wiederhergestellt."
                            } else {
                                "Keine wiederherstellbaren Daten gefunden."
                            }
                        )
                        showRestoreDataDialog = false
                    },
                )
            },
            dismissButton = {
                CompactMenuTextButton(
                    text = "Abbrechen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showRestoreDataDialog = false },
                )
            },
        )
    }

    if (showReleaseNotesDialog) {
        val uriHandler = LocalUriHandler.current
        CompactMenuDialog(
            onDismissRequest = { showReleaseNotesDialog = false },
            isDarkMenu = darkBackground,
            title = { Text("App Info", style = menuTitleStyle) },
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = menuTextStyle) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Release-Infos:", style = menuTextStyle)
                            Text(
                                releaseNotes ?: "Keine Release-Notizen vorhanden.",
                                style = menuTextStyle,
                            )
                            HorizontalDivider()
                            Text("Release-Verlauf:", style = menuTextStyle)
                            CompactMenuTextButton(
                                text = "Release-Verlauf öffnen",
                                style = linkTextStyle,
                                fillWidth = false,
                                onClick = {
                                    uriHandler.openUri(APP_RELEASE_HISTORY_URL)
                                }
                            )
                            HorizontalDivider()
                            Text("Autor: $APP_AUTHOR", style = menuTextStyle)
                            Text(APP_COPYRIGHT_TEXT, style = menuTextStyle)
                        }
                    }
                }
            },
            confirmButton = {
                CompactMenuTextButton(
                    text = "Schließen",
                    style = menuTextStyle,
                    fillWidth = false,
                    onClick = { showReleaseNotesDialog = false },
                )
            },
        )
    }

}

@Composable
private fun DashboardPageLayout(
    page: DashboardPage,
    telemetry: Map<String, Float>,
    telemetryText: Map<String, String>,
    aisTelemetry: Map<String, Float>,
    aisTelemetryText: Map<String, String>,
    recentNmeaPgnHistory: List<NmeaPgnHistoryEntry>,
    recentNmea0183History: List<Nmea0183HistoryEntry>,
    dalyDebugEvents: List<String>,
    detectedNmeaSources: List<NmeaSourceProfile>,
    titleScale: Float,
    uiFont: UiFont,
    darkBackground: Boolean,
    gridStepPercent: Float,
    widgetFrameStyle: WidgetFrameStyle,
    widgetFrameStyleGrayOffset: Int,
    onMove: (String, Float, Float, Float, Float, Float, Boolean) -> Unit,
    onResize: (String, Float, Float, Float, Float, Float, Boolean) -> Unit,
    onSnap: (String, Float, Float, Float) -> Unit,
    onRemove: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onAddWidget: (WidgetKind, Float, Float, Float) -> Boolean,
    onAddWidgetError: (String) -> Unit,
    onPageMetrics: (Float, Float, Float) -> Unit,
    onApplyMinimums: (Float, Float, Float) -> Unit,
    onSendAutopilotCommand: (AutopilotDispatchRequest) -> Unit,
    onUpdateWindWidgetSettings: (String, String) -> Unit,
    onUpdateBatteryWidgetSettings: (String, String) -> Unit,
    onUpdateAisWidgetSettings: (String, String) -> Unit,
    onUpdateEchosounderWidgetSettings: (String, String) -> Unit,
    onUpdateAutopilotWidgetSettings: (String, String) -> Unit,
    onUpdateLogWidgetSettings: (String, String) -> Unit,
    onUpdateAnchorWatchWidgetSettings: (String, String) -> Unit,
    onUpdateTemperatureWidgetSettings: (String, String) -> Unit,
    onUpdateSeaChartWidgetSettings: (String, String) -> Unit,
    alarmToneVolume: Float,
    alarmRepeatIntervalSeconds: Int,
    onAnchorWatchAlarm: (String) -> Unit,
    onCallAisMmsi: (String) -> Unit,
    storedWindWidgetSettings: Map<String, String>,
    storedBatteryWidgetSettings: Map<String, String>,
    storedAisWidgetSettings: Map<String, String>,
    storedEchosounderWidgetSettings: Map<String, String>,
    storedAutopilotWidgetSettings: Map<String, String>,
    storedLogWidgetSettings: Map<String, String>,
    storedAnchorWatchWidgetSettings: Map<String, String>,
    storedTemperatureWidgetSettings: Map<String, String>,
    storedSeaChartWidgetSettings: Map<String, String>,
    activeRoute: Route? = null,
    mobLat: Double? = null,
    mobLon: Double? = null,
    mobTimestampMs: Long? = null,
    boatDraftMeters: Float = 0f,
    onTriggerMob: () -> Unit = {},
    onClearMob: () -> Unit = {},
    onShowSeaChartNotice: (String, String) -> Unit = { _, _ -> },
) {
    val batterySettingsByWidget = remember(page.id) { mutableStateMapOf<String, BatteryWidgetSettings>() }
    val windSettingsByWidget = remember(page.id) { mutableStateMapOf<String, WindWidgetSettings>() }
    val aisSettingsByWidget = remember(page.id) { mutableStateMapOf<String, AisWidgetSettings>() }
    val echosounderSettingsByWidget = remember(page.id) { mutableStateMapOf<String, EchosounderWidgetSettings>() }
    val autopilotSettingsByWidget = remember(page.id) { mutableStateMapOf<String, AutopilotWidgetSettings>() }
    val logSettingsByWidget = remember(page.id) { mutableStateMapOf<String, LogWidgetSettings>() }
    val anchorWatchSettingsByWidget = remember(page.id) { mutableStateMapOf<String, AnchorWatchWidgetSettings>() }
    val temperatureSettingsByWidget = remember(page.id) { mutableStateMapOf<String, TemperatureWidgetSettings>() }
    val seaChartSettingsByWidget = remember(page.id) { mutableStateMapOf<String, SeaChartWidgetSettings>() }
    val seaChartSourceByWidget = remember(page.id) { mutableStateMapOf<String, String>() }
    val seaChartTileTemplateByProvider = remember(page.id) { mutableStateMapOf<SeaChartMapProvider, String?>() }
    val anchorWatchRuntimeByWidget = remember(page.id) { mutableStateMapOf<String, AnchorWatchRuntimeState>() }
    val seaChartMobByWidget = remember(page.id) { mutableStateMapOf<String, Pair<Float, Float>>() }
    val logSamplesByWidget = remember(page.id) { mutableStateMapOf<String, MutableList<LogSpeedSample>>() }
    val windSpeedHistoryByWidget = remember(page.id) { mutableStateMapOf<String, MutableList<WindSpeedSample>>() }
    val logTripByWidget = remember(page.id) { mutableStateMapOf<String, Float>() }
    val logLastSampleMsByWidget = remember(page.id) { mutableStateMapOf<String, Long>() }
    val widgetAlarmStateByWidget = remember(page.id) { mutableStateMapOf<String, Boolean>() }
    val widgetAlarmMutedByWidget = remember(page.id) { mutableStateMapOf<String, Boolean>() }
    val autopilotTargetByWidget = remember(page.id) { mutableStateMapOf<String, Float>() }
    val rudderHistoryByWidget = remember(page.id) { mutableStateMapOf<String, MutableList<Pair<Long, Float>>>() }
    val cachedAisTargetsByWidget = remember(page.id) { mutableStateMapOf<String, CachedAisTargetState>() }
    var lastAisTargetLogAtMs by remember { mutableLongStateOf(0L) }
    var widgetMenuWidgetId by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var widgetRenameText by rememberSaveable(page.id) { mutableStateOf("") }
    var activeInteractingWidgetId by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    var selectedBatteryChemistry by remember(page.id) { mutableStateOf(BatteryChemistry.LEAD_ACID) }
    var selectedAutopilotTargetDevice by remember(page.id) { mutableStateOf(AutopilotTargetDevice.GARMIN) }
    var selectedAutopilotGatewayBackend by remember(page.id) { mutableStateOf(AutopilotGatewayBackend.SIGNALK_V2) }
    var selectedAutopilotGatewayHost by remember(page.id) { mutableStateOf("192.168.4.1") }
    var selectedAutopilotGatewayPort by remember(page.id) { mutableStateOf("3000") }
    var selectedAutopilotSignalKId by remember(page.id) { mutableStateOf("_default") }
    var selectedAutopilotRudderAverageSeconds by remember { mutableIntStateOf(30) }
    var selectedAutopilotSafetyGateArmed by remember(page.id) { mutableStateOf(false) }
    var selectedAutopilotCommandTimeoutSeconds by remember(page.id) { mutableIntStateOf(5) }
    var selectedAutopilotAuthToken by remember(page.id) { mutableStateOf("") }
    var selectedWindShowBoatDirection by remember(page.id) { mutableStateOf(true) }
    var selectedWindShowNorthDirection by remember(page.id) { mutableStateOf(true) }
    var selectedWindShowWindSpeed by remember(page.id) { mutableStateOf(true) }
    var selectedWindSpeedUnit by remember(page.id) { mutableStateOf(WindSpeedUnit.KNOTS) }
    var selectedWindHistoryWindowMinutes by remember(page.id) { mutableIntStateOf(5) }
    var selectedWindMinMaxUsesTrueWind by remember(page.id) { mutableStateOf(true) }
    var selectedAisAlarmDistanceNm by remember(page.id) { mutableFloatStateOf(1f) }
    var selectedAisAlarmMinutes by remember(page.id) { mutableFloatStateOf(5f) }
    var selectedAisOrientationNorthUp by remember(page.id) { mutableStateOf(true) }
    var selectedAisFontSizeOffsetSp by remember(page.id) { mutableIntStateOf(0) }
    var selectedAisTargetVisibilityMinutes by remember(page.id) { mutableIntStateOf(5) }
    var selectedAisTargetVisibilityByMmsi by remember(page.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var selectedEchoMinDepth by remember(page.id) { mutableFloatStateOf(2f) }
    var selectedEchoDynamicRate by remember(page.id) { mutableFloatStateOf(0.8f) }
    var selectedEchoDepthUnit by remember(page.id) { mutableStateOf(EchosounderDepthUnit.METERS) }
    var selectedEchoAlarmTone by remember(page.id) { mutableIntStateOf(0) }
    var selectedLogWindowMinutes by remember(page.id) { mutableIntStateOf(10) }
    var selectedLogSpeedSource by remember(page.id) { mutableStateOf(LogWidgetSpeedSource.GPS_SOG) }
    var selectedSeaChartMapProvider by remember(page.id) { mutableStateOf(SeaChartMapProvider.NOAA) }
    var selectedSeaChartSpeedSource by remember(page.id) { mutableStateOf(SeaChartSpeedSource.GPS_SOG) }
    var selectedSeaChartShowAisOverlay by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartShowGribOverlay by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartShowOpenSeaMapOverlay by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartShowHeadingLine by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartHeadingLineLengthNm by remember(page.id) { mutableFloatStateOf(0.5f) }
    var selectedSeaChartShowCogVector by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartCogVectorMinutes by remember(page.id) { mutableIntStateOf(6) }
    var selectedSeaChartShowPredictor by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartPredictorMinutes by remember(page.id) { mutableIntStateOf(6) }
    var selectedSeaChartPredictorIntervalMinutes by remember(page.id) { mutableIntStateOf(1) }
    var selectedSeaChartShowPredictorLabels by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartShowBoatIcon by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartBoatIconSizeDp by remember(page.id) { mutableIntStateOf(24) }
    var selectedSeaChartShowCourseLine by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartShowRoadLayers by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartShowDepthLines by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartShowContourLines by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartShowTrack by remember(page.id) { mutableStateOf(true) }
    var selectedSeaChartTrackDurationMinutes by remember(page.id) { mutableIntStateOf(60) }
    var selectedSeaChartTrackRecordIntervalSeconds by remember(page.id) { mutableIntStateOf(10) }
    var selectedSeaChartGuardZoneEnabled by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartGuardZoneInnerNm by remember(page.id) { mutableFloatStateOf(0.5f) }
    var selectedSeaChartGuardZoneOuterNm by remember(page.id) { mutableFloatStateOf(2f) }
    var selectedSeaChartGuardZoneSectorStartDeg by remember(page.id) { mutableFloatStateOf(0f) }
    var selectedSeaChartGuardZoneSectorEndDeg by remember(page.id) { mutableFloatStateOf(360f) }
    var selectedSeaChartShowLaylines by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartTackingAngleDeg by remember(page.id) { mutableIntStateOf(45) }
    var selectedSeaChartLaylineLengthNm by remember(page.id) { mutableFloatStateOf(3f) }
    var selectedSeaChartShowSafetyContour by remember(page.id) { mutableStateOf(false) }
    var selectedSeaChartSafetyDepthMeters by remember(page.id) { mutableFloatStateOf(3f) }
    var selectedSeaChartCourseLineBearingDeg by remember(page.id) { mutableFloatStateOf(0f) }
    var selectedSeaChartCourseLineDistanceNm by remember(page.id) { mutableFloatStateOf(5f) }
    var selectedAnchorWatchEnabled by remember(page.id) { mutableStateOf(false) }
    var selectedAnchorWatchDeviation by remember(page.id) { mutableFloatStateOf(DEFAULT_ANCHOR_WATCH_TOLERANCE_PERCENT) }
    var selectedAnchorWatchChainCalibrationUnit by remember(page.id) { mutableStateOf(AnchorWatchChainUnit.METRIC) }
    var selectedAnchorWatchChainSensorEnabled by remember(page.id) { mutableStateOf(true) }
    var selectedAnchorWatchChainLengthCalibrationEnabled by remember(page.id) { mutableStateOf(false) }
    var selectedAnchorWatchCalibratedChainLengthMeters by remember(page.id) { mutableFloatStateOf(0f) }
    var selectedAnchorWatchChainStrengthLabel by remember(page.id) { mutableStateOf("10") }
    var selectedAnchorWatchChainPocketsText by remember(page.id) { mutableStateOf("6") }
    var selectedAnchorWatchAlarmTone by remember(page.id) { mutableIntStateOf(0) }
    var selectedTemperatureUnit by remember(page.id) { mutableStateOf(TemperatureUnit.CELSIUS) }
    var selectedTemperatureSensorNames by remember(page.id) { mutableStateOf(List(10) { index -> "Temperatursensor ${index + 1}" }) }
    var showEchosounderMinDepthDialog by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    var showEchosounderDynamicDialog by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    var sharedTackingAngleDeg by remember { mutableIntStateOf(DEFAULT_TACKING_ANGLE_DEG) }
    var autopilotMenuWidgetId by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    var widgetHelpWidgetId by rememberSaveable(page.id) { mutableStateOf<String?>(null) }
    var showAddWidgetFromSurface by rememberSaveable(page.id) { mutableStateOf(false) }
    var showWindHistoryWindowMenu by remember(page.id) { mutableStateOf(false) }
    var showAnchorWatchChainStrengthMenu by remember(page.id) { mutableStateOf(false) }
    var showAnchorWatchChainCalibrationDialog by remember(page.id) { mutableStateOf<String?>(null) }
    var showSeaChartDownloadDialog by remember(page.id) { mutableStateOf<String?>(null) }
    var seaChartDownloadCatalog by remember(page.id) { mutableStateOf<SeaChartDownloadCatalog?>(null) }
    var seaChartDownloadUiState by remember(page.id) { mutableStateOf(SeaChartDownloadUiState()) }
    var seaChartDownloadErrorDialog by remember(page.id) { mutableStateOf<SeaChartDownloadErrorState?>(null) }
    var seaChartExistingDataPrompt by remember(page.id) { mutableStateOf<SeaChartExistingDataPromptState?>(null) }
    var seaChartDownloadJob by remember(page.id) { mutableStateOf<Job?>(null) }
    var seaChartDownloadManagerId by remember(page.id) { mutableLongStateOf(SEA_CHART_NO_ACTIVE_DOWNLOAD_ID) }
    var seaChartDownloadResumeChecked by remember(page.id) { mutableStateOf(false) }
    var showSeaChartLayerFilterDialog by remember(page.id) { mutableStateOf<String?>(null) }
    var seaChartLayerFilterRoadLayers by remember(page.id) { mutableStateOf(true) }
    var seaChartLayerFilterDepthLines by remember(page.id) { mutableStateOf(true) }
    var seaChartLayerFilterContourLines by remember(page.id) { mutableStateOf(true) }
    var seaChartDownloadedRegionsByProvider by remember(page.id) {
        mutableStateOf<Map<SeaChartMapProvider, List<SeaChartDownloadedRegionInfo>>>(emptyMap())
    }
    var seaChartDownloadedRegionsRefresh by remember(page.id) { mutableIntStateOf(0) }
    var seaChartDownloadedRegionInfoDialog by remember(page.id) { mutableStateOf<SeaChartDownloadedRegionInfo?>(null) }
    var seaChartDeleteRegionPrompt by remember(page.id) { mutableStateOf<SeaChartDownloadedRegionInfo?>(null) }
    var seaChartAutoZipImportDone by rememberSaveable(page.id) { mutableStateOf(false) }
    var seaChartPendingZipApprovalFiles by remember(page.id) { mutableStateOf<List<SeaChartPendingZipInfo>>(emptyList()) }
    var seaChartPendingZipImportDeclined by rememberSaveable(page.id) { mutableStateOf(false) }
    var seaChartLayerFilterCapabilitiesRefresh by remember(page.id) { mutableIntStateOf(0) }
    var kartenOfflineReloadRequest by remember(page.id) { mutableIntStateOf(0) }
    var pendingAutopilotDispatch by remember(page.id) { mutableStateOf<AutopilotDispatchRequest?>(null) }
    val seaChartDownloadScope = rememberCoroutineScope()
    // Für alle Widget-Dialoge: bewusst keine Titelzeilen/Überschriften erzeugen.
    // Konsistent wird der Begriff "Widget" verwendet; "Container" ist nicht Teil der UI-Nomenklatur mehr.
    val menuTextColor = if (darkBackground) Color.White else Color.Black
    val widgetMenuTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = SeaFoxDesignTokens.Size.menuBodyTextSp,
        lineHeight = SeaFoxDesignTokens.Size.menuLineHeight,
        color = menuTextColor,
    )
    val widgetInputFieldTextStyle = widgetMenuTextStyle.copy(
        color = if (darkBackground) MENU_INPUT_FIELD_TEXT_DARK else MENU_INPUT_FIELD_TEXT_LIGHT,
    )
    val widgetInputTextStyle = widgetMenuTextStyle.copy(
        color = widgetMenuTextStyle.color.copy(alpha = 0.55f),
    )
    val seaChartWarningDialogTextStyle = widgetMenuTextStyle.copy(color = Color.White)
    val widgetLinkTextStyle = widgetMenuTextStyle.merge(SeaFoxDesignTokens.Type.linkText)
        val runPendingSeaChartZipImport = {
            val approvedZipCount = seaChartPendingZipApprovalFiles.size
            seaChartPendingZipApprovalFiles = emptyList()
            seaChartPendingZipImportDeclined = false
            val alreadyProcessedZipPaths = mutableSetOf<String>()
            seaChartDownloadScope.launch {
                seaChartDownloadErrorDialog = null
                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                    isRunning = true,
                    progress = 0f,
                statusText = "Entpacken freigegebener seaCHART-Zip-Dateien startet...",
                completed = false,
                errorText = null,
                downloadedFileName = "",
                isUnpacking = false,
                unpackedBytes = 0L,
                unpackTotalBytes = -1L,
            )

            var importedZipCount = 0
            SeaChartMapProvider.entries.forEach { mapProvider ->
                importedZipCount += importSeaChartPendingZipFiles(
                    context = context,
                    mapProvider = mapProvider,
                    onStatus = { statusText ->
                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                            isRunning = true,
                            statusText = statusText,
                            downloadedFileName = "",
                            completed = false,
                        )
                        announceSeaChartUnpackStatus(
                            context = context,
                            statusText = statusText,
                            onPersistentNotice = { message ->
                                onShowSeaChartNotice("seaCHART Status", message)
                            },
                        )
                    },
                    onPersistentNotice = { message ->
                        onShowSeaChartNotice("seaCHART Status", message)
                    },
                    onRetryableError = { errorState ->
                        seaChartDownloadErrorDialog = errorState
                    },
                    alreadyProcessedZipPaths = alreadyProcessedZipPaths,
                )
            }
            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                isRunning = false,
                completed = importedZipCount > 0,
                statusText = if (importedZipCount > 0) {
                    "$importedZipCount Zip-Datei(en) entpackt."
                } else {
                    "Keine Zip-Dateien entpackt."
                },
            )
            if (importedZipCount > 0) {
                val failedZipCount = (approvedZipCount - importedZipCount).coerceAtLeast(0)
                val reloadNotice = "Offlinekarten werden neu geladen. Das kann bis zu 3 Minuten dauern."
                SeaChartMapProvider.entries.forEach { mapProvider ->
                    seaChartInvalidateSeaChartCaches(mapProvider)
                }
                kartenOfflineReloadRequest += 1
                seaChartDownloadErrorDialog = null
                onShowSeaChartNotice(
                    "seaCHART Status",
                    if (failedZipCount > 0) {
                        "$importedZipCount von $approvedZipCount Zip-Datei(en) erfolgreich entpackt."
                    } else {
                        "$importedZipCount Zip-Datei(en) erfolgreich entpackt."
                    },
                )
                SeaChartMapProvider.entries.forEach { provider ->
                    val resolvedTemplate = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        activeSeaChartTileTemplate(
                            context,
                            provider,
                        )
                    }
                    seaChartTileTemplateByProvider[provider] = resolvedTemplate
                }
                page.widgets
                    .filter { isSeaChartWidgetKind(it.kind) }
                    .forEach { widget ->
                        val chartSettings = parseSeaChartWidgetSettings(storedSeaChartWidgetSettings[widget.id])
                        val resolvedSource = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            activeSeaChartSourceLabel(
                                context,
                                chartSettings.mapProvider,
                            )
                        }
                        seaChartSourceByWidget[widget.id] = resolvedSource
                    }
                onShowSeaChartNotice(
                    "seaCHART Status",
                    seaChartDownloadUiState.statusText,
                )
                onShowSeaChartNotice(
                    "Offlinekarten",
                    reloadNotice,
                )
                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                    statusText = reloadNotice,
                    completed = true,
                    isRunning = false,
                    isUnpacking = false,
                    errorText = null,
                )
                notifySeaChartUnpackStatus(
                    context,
                    seaChartDownloadUiState.statusText,
                )
            }
        }
    }

    LaunchedEffect(
        page.id,
        page.widgets.size,
        page.widgets.joinToString { it.id },
        storedWindWidgetSettings,
        storedBatteryWidgetSettings,
        storedAisWidgetSettings,
        storedEchosounderWidgetSettings,
        storedAutopilotWidgetSettings,
        storedLogWidgetSettings,
        storedAnchorWatchWidgetSettings,
        storedTemperatureWidgetSettings,
        storedSeaChartWidgetSettings,
    ) {
        val hasSeaChartWidgets = page.widgets.any { isSeaChartWidgetKind(it.kind) }
        if (hasSeaChartWidgets && !seaChartAutoZipImportDone) {
            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                isRunning = false,
                statusText = "Prüfe vorhandene seaCHART-Zip-Dateien...",
                completed = false,
            )
            val pendingZipFiles = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val seenPaths = mutableSetOf<String>()
                SeaChartMapProvider.entries.flatMap { mapProvider ->
                    seaChartPendingZipFiles(context, mapProvider).filter { zip ->
                        val key = zip.filePath.lowercase(Locale.ROOT)
                        if (seenPaths.contains(key)) {
                            false
                        } else {
                            seenPaths.add(key)
                            true
                        }
                    }
                }
            }
            seaChartAutoZipImportDone = true
            if (pendingZipFiles.isNotEmpty()) {
                seaChartPendingZipApprovalFiles = pendingZipFiles
                seaChartDownloadResumeChecked = true
                seaChartPendingZipImportDeclined = false
                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                    isRunning = false,
                    progress = 0f,
                    statusText = "${pendingZipFiles.size} Zip-Datei(en) stehen zum Entpacken bereit.",
                    completed = false,
                    errorText = null,
                    downloadedFileName = pendingZipFiles.firstOrNull()?.fileName.orEmpty(),
                    isUnpacking = false,
                    unpackedBytes = 0L,
                    unpackTotalBytes = -1L,
                )
            } else {
                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                    isRunning = false,
                    statusText = "",
                    completed = false,
                )
            }
        }

        val currentWind = windSettingsByWidget.toMap()
        val currentBattery = batterySettingsByWidget.toMap()
        val currentAis = aisSettingsByWidget.toMap()
        val currentEchosounder = echosounderSettingsByWidget.toMap()
        val currentAutopilot = autopilotSettingsByWidget.toMap()
        val currentLog = logSettingsByWidget.toMap()
        val currentAnchorWatch = anchorWatchSettingsByWidget.toMap()
        val currentTemperature = temperatureSettingsByWidget.toMap()
        val currentSeaChart = seaChartSettingsByWidget.toMap()
        val currentSeaChartSources = seaChartSourceByWidget.toMap()
        val currentSeaChartTemplates = seaChartTileTemplateByProvider.toMap()

        windSettingsByWidget.clear()
        batterySettingsByWidget.clear()
        aisSettingsByWidget.clear()
        echosounderSettingsByWidget.clear()
        autopilotSettingsByWidget.clear()
        logSettingsByWidget.clear()
        anchorWatchSettingsByWidget.clear()
        temperatureSettingsByWidget.clear()
        seaChartSettingsByWidget.clear()
        seaChartSourceByWidget.clear()

        val firstWindWidget = page.widgets.firstOrNull { it.kind == WidgetKind.WIND }
        val fallbackSettings = firstWindWidget?.let {
            parseWindWidgetSettings(storedWindWidgetSettings[it.id], defaultTackingAngle = sharedTackingAngleDeg)
        }

        page.widgets.filter { it.kind == WidgetKind.WIND }.forEach { widget ->
            windSettingsByWidget[widget.id] = currentWind[widget.id] ?: parseWindWidgetSettings(
                storedWindWidgetSettings[widget.id],
                defaultTackingAngle = sharedTackingAngleDeg,
            )
        }
        page.widgets.filter { it.kind == WidgetKind.BATTERY }.forEach { widget ->
            batterySettingsByWidget[widget.id] = currentBattery[widget.id] ?: parseBatteryWidgetSettings(
                storedBatteryWidgetSettings[widget.id]
            )
        }
        page.widgets.filter { it.kind == WidgetKind.AIS }.forEach { widget ->
            aisSettingsByWidget[widget.id] = currentAis[widget.id] ?: parseAisWidgetSettings(
                storedAisWidgetSettings[widget.id]
            )
        }
        page.widgets.filter { it.kind == WidgetKind.ECHOSOUNDER }.forEach { widget ->
            echosounderSettingsByWidget[widget.id] = currentEchosounder[widget.id] ?: parseEchosounderWidgetSettings(
                storedEchosounderWidgetSettings[widget.id]
            )
        }
        page.widgets.filter { it.kind == WidgetKind.AUTOPILOT }.forEach { widget ->
            autopilotSettingsByWidget[widget.id] = currentAutopilot[widget.id] ?: parseAutopilotWidgetSettings(
                storedAutopilotWidgetSettings[widget.id]
            )
        }
        page.widgets.filter { it.kind == WidgetKind.LOG }.forEach { widget ->
            logSettingsByWidget[widget.id] = currentLog[widget.id] ?: parseLogWidgetSettings(
                storedLogWidgetSettings[widget.id]
            )
        }
        page.widgets.filter { it.kind == WidgetKind.ANCHOR_WATCH }.forEach { widget ->
            anchorWatchSettingsByWidget[widget.id] = currentAnchorWatch[widget.id] ?: parseAnchorWatchWidgetSettings(
                storedAnchorWatchWidgetSettings[widget.id],
            )
        }
        page.widgets.filter { it.kind == WidgetKind.TEMPERATURE }.forEach { widget ->
            temperatureSettingsByWidget[widget.id] = currentTemperature[widget.id] ?: parseTemperatureWidgetSettings(
                storedTemperatureWidgetSettings[widget.id]
            )
        }
        page.widgets.filter { isSeaChartWidgetKind(it.kind) }.forEach { widget ->
            val chartSettings = currentSeaChart[widget.id] ?: parseSeaChartWidgetSettings(
                storedSeaChartWidgetSettings[widget.id]
            )
            seaChartSettingsByWidget[widget.id] = chartSettings
            seaChartSourceByWidget[widget.id] = currentSeaChartSources[widget.id]?.takeIf { it.isNotBlank() }
                ?: "Lokale Karte wird vorbereitet..."
            currentSeaChartTemplates[chartSettings.mapProvider]?.let { cachedTemplate ->
                if (cachedTemplate.isNotBlank()) {
                    seaChartTileTemplateByProvider[chartSettings.mapProvider] = cachedTemplate
                }
            }
        }

        if (fallbackSettings != null) {
            sharedTackingAngleDeg = fallbackSettings.tackingAngleDeg
        }
    }

    val seaChartHasWidgets = page.widgets.any { isSeaChartWidgetKind(it.kind) }
    LaunchedEffect(
        page.id,
        seaChartHasWidgets,
        page.widgets.joinToString { it.id },
        seaChartPendingZipApprovalFiles.size,
        seaChartPendingZipImportDeclined,
    ) {
        if (
            !seaChartHasWidgets ||
            seaChartDownloadResumeChecked ||
            seaChartPendingZipApprovalFiles.isNotEmpty() ||
            seaChartPendingZipImportDeclined
        ) {
            return@LaunchedEffect
        }
        seaChartDownloadResumeChecked = true
        if (seaChartDownloadJob?.isActive == true) return@LaunchedEffect

        val pendingDownloadSession = seaChartLoadDownloadSession(context) ?: return@LaunchedEffect
        val migratedProviderName = if (pendingDownloadSession.providerName == "OPEN_SEA_MAP") "OPEN_SEA_CHARTS" else pendingDownloadSession.providerName
        val sessionProvider = runCatching {
            SeaChartMapProvider.valueOf(migratedProviderName)
        }.getOrNull()
        if (sessionProvider == null) {
            seaChartClearDownloadSession(context)
            return@LaunchedEffect
        }
        if (pendingDownloadSession.downloadId <= SEA_CHART_NO_ACTIVE_DOWNLOAD_ID) {
            seaChartClearDownloadSession(context)
            return@LaunchedEffect
        }
        seaChartDownloadUiState = seaChartDownloadUiState.copy(
            isRunning = true,
            statusText = "Vorheriger seaCHART-Download wird nach App-Neustart fortgesetzt...",
            downloadedFileName = pendingDownloadSession.fileName,
            errorText = null,
            isUnpacking = false,
            unpackedBytes = 0L,
            unpackTotalBytes = -1L,
        )
        seaChartDownloadManagerId = pendingDownloadSession.downloadId
        seaChartDownloadJob = seaChartDownloadScope.launch {
            val selectedProvider = sessionProvider
            try {
                val downloadedFile = downloadSeaChartFileWithDownloadManager(
                    context = context,
                    url = pendingDownloadSession.regionUrl,
                    fileName = pendingDownloadSession.fileName,
                    mapProvider = selectedProvider,
                    regionLabel = pendingDownloadSession.regionLabel,
                    regionFolderName = pendingDownloadSession.regionFolder,
                    onDownloadId = { downloadId ->
                        seaChartDownloadManagerId = downloadId
                    },
                    onPersistentNotice = { statusText ->
                        onShowSeaChartNotice("seaCHART Status", statusText)
                    },
                    onProgress = { downloadedBytes, totalBytes ->
                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                            isRunning = true,
                            isUnpacking = false,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            unpackedBytes = 0L,
                            unpackTotalBytes = -1L,
                            progress = if (totalBytes > 0L) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                seaChartDownloadUiState.progress
                            },
                            statusText = if (totalBytes > 0L) {
                                "Lädt: ${formatSeaChartMegabytes(downloadedBytes)} von ${formatSeaChartMegabytes(totalBytes)}"
                            } else {
                                "Lädt: ${formatSeaChartMegabytes(downloadedBytes)}"
                            },
                            downloadedFileName = pendingDownloadSession.fileName,
                            errorText = null,
                        )
                    },
                    onUnpackProgress = { unpackedBytes, totalUnpackBytes ->
                        val unpackProgress = if (totalUnpackBytes > 0L) {
                            (unpackedBytes.toFloat() / totalUnpackBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            seaChartDownloadUiState.progress
                        }
                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                            isRunning = true,
                            isUnpacking = true,
                            unpackedBytes = unpackedBytes,
                            unpackTotalBytes = totalUnpackBytes,
                            progress = unpackProgress,
                            downloadedFileName = pendingDownloadSession.fileName,
                            errorText = null,
                        )
                    },
                    onStatus = { statusText ->
                        val unpackProgress = parseSeaChartUnpackPercent(statusText)
                        val isUnpackingPhase = statusText.startsWith("Entpacke:") ||
                            statusText.startsWith("Entpacken gestartet") ||
                            statusText.startsWith("Entpacken fehlgeschlagen") ||
                            statusText.startsWith("Prüfe ZIP:") ||
                            statusText.startsWith("ZIP-Prüfung gestartet") ||
                            statusText.startsWith("ZIP-Prüfung fehlgeschlagen")
                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                            isRunning = true,
                            statusText = statusText,
                            downloadedFileName = pendingDownloadSession.fileName,
                            isUnpacking = isUnpackingPhase,
                            progress = if (isUnpackingPhase && unpackProgress != null) {
                                unpackProgress
                            } else {
                                seaChartDownloadUiState.progress
                            },
                        )
                        announceSeaChartUnpackStatus(
                            context = context,
                            statusText = statusText,
                            onPersistentNotice = { message ->
                                onShowSeaChartNotice("seaCHART Status", message)
                            },
                        )
                    },
                    existingDownloadId = pendingDownloadSession.downloadId,
                )
                page.widgets
                    .filter { isSeaChartWidgetKind(it.kind) }
                    .forEach { widget ->
                        val resolvedSource = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            activeSeaChartSourceLabel(
                                context,
                                selectedProvider,
                            )
                        }
                        val resolvedTemplate = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            activeSeaChartTileTemplate(
                                context,
                                selectedProvider,
                            )
                        }
                        seaChartSourceByWidget[widget.id] = resolvedSource
                        seaChartTileTemplateByProvider[selectedProvider] = resolvedTemplate
                    }
                val extractedFolder = downloadedFile.parentFile?.absolutePath.orEmpty()
                val targetSummary = if (extractedFolder.isBlank()) {
                    "Download abgeschlossen"
                } else {
                    "Download abgeschlossen\nKarte entpackt nach:\n$extractedFolder"
                }
                val resolvedStatusText = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    seaChartDisplayStatusMessage(
                        context,
                        selectedProvider,
                        targetSummary,
                    )
                }
                seaChartDownloadUiState = SeaChartDownloadUiState(
                    isRunning = false,
                    progress = 1f,
                    downloadedBytes = downloadedFile.length(),
                    totalBytes = downloadedFile.length(),
                    statusText = resolvedStatusText,
                    downloadedFileName = downloadedFile.name,
                    completed = true,
                    isUnpacking = false,
                    unpackedBytes = downloadedFile.length(),
                    unpackTotalBytes = downloadedFile.length(),
                    errorText = null,
                )
                seaChartLayerFilterCapabilitiesRefresh++
                seaChartDownloadErrorDialog = null
            } catch (ex: kotlinx.coroutines.CancellationException) {
                logSeaChartError(
                    "seaCHART Resume-Download abgebrochen: provider=${selectedProvider.name}, file=${pendingDownloadSession.fileName}",
                    ex,
                )
                seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                    message = "Der Download/Entpack-Vorgang wurde abgebrochen.",
                    canRetry = false,
                    provider = selectedProvider,
                    regionLabel = pendingDownloadSession.regionLabel,
                    regionUrl = pendingDownloadSession.regionUrl,
                    fileName = pendingDownloadSession.fileName,
                    filePath = seaChartDownloadTargetFilePath(
                        context = context,
                        mapProvider = selectedProvider,
                        regionFolderName = pendingDownloadSession.regionFolder,
                        fileName = pendingDownloadSession.fileName,
                    ),
                )
                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                    isRunning = false,
                    progress = 0f,
                    statusText = "Download/Entpack abgebrochen",
                    isUnpacking = false,
                    unpackedBytes = 0L,
                    unpackTotalBytes = -1L,
                    errorText = "Download/Entpack wurde vom Nutzer abgebrochen.",
                    downloadedFileName = pendingDownloadSession.fileName,
                    completed = false,
                )
            } catch (ex: Exception) {
                seaChartClearDownloadSession(context)
                val errorText = seaChartDownloadRetryHintMessage(ex.message ?: "Unbekannter Fehler")
                logSeaChartError(
                    "seaCHART Resume-Download fehlgeschlagen: provider=${selectedProvider.name}, file=${pendingDownloadSession.fileName}, message=$errorText",
                    ex,
                )
                seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                    message = errorText,
                    canRetry = true,
                    provider = selectedProvider,
                    regionLabel = pendingDownloadSession.regionLabel,
                    regionUrl = pendingDownloadSession.regionUrl,
                    fileName = pendingDownloadSession.fileName,
                    filePath = seaChartDownloadTargetFilePath(
                        context = context,
                        mapProvider = selectedProvider,
                        regionFolderName = pendingDownloadSession.regionFolder,
                        fileName = pendingDownloadSession.fileName,
                    ),
                )
                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                    isRunning = false,
                    progress = 0f,
                    statusText = "Download fehlgeschlagen",
                    isUnpacking = false,
                    unpackedBytes = 0L,
                    unpackTotalBytes = -1L,
                    errorText = errorText,
                    downloadedFileName = pendingDownloadSession.fileName,
                    completed = false,
                )
            } finally {
                seaChartDownloadJob = null
                seaChartDownloadManagerId = SEA_CHART_NO_ACTIVE_DOWNLOAD_ID
            }
        }
    }

    fun setSharedTackingAngleDeg(value: Int) {
        val clamped = value.coerceIn(50, 140)
        sharedTackingAngleDeg = clamped
        page.widgets
            .filter { it.kind == WidgetKind.WIND }
            .forEach { windWidget ->
                val current = windSettingsByWidget.getOrPut(windWidget.id) {
                    WindWidgetSettings(tackingAngleDeg = clamped)
                }
                if (current.tackingAngleDeg != clamped) {
                    windSettingsByWidget[windWidget.id] = current.copy(tackingAngleDeg = clamped)
                }
            }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PremiumDashboardBackdrop(
            darkBackground = darkBackground,
            modifier = Modifier.matchParentSize(),
        )
        val density = LocalDensity.current
        val pageWidthPx = with(density) { maxWidth.toPx() }
        val pageHeightPx = with(density) { maxHeight.toPx() }
        val safeGridStepPercent = gridStepPercent.coerceIn(0.5f, 20f)
        val gridStepPx = (pageWidthPx * (safeGridStepPercent / 100f)).coerceAtLeast(1f)
        LaunchedEffect(page.id, page.widgets.size, pageWidthPx, pageHeightPx, gridStepPx) {
            onApplyMinimums(pageWidthPx, pageHeightPx, gridStepPx)
            onPageMetrics(pageWidthPx, pageHeightPx, gridStepPx)
        }
        val titleTotalCount = page.widgets.groupingBy { it.title }.eachCount()
        val titleCount = mutableMapOf<String, Int>()
        val apparentWindAngle = pickValueOrNull(
            telemetry,
            listOf("wind_angle_apparent", "apparent_wind_angle", "awa", "wind_angle", "wind_direction")
        )
        val selectedWindAngleForTack = apparentWindAngle ?: Float.NaN
        val aisOwnHeadingDeg = pickValueOrNull(
            telemetry,
            headingWithCourseOverGroundKeys
        )?.let { heading -> heading.takeIf { it.isFinite() }?.let(::wrap360Ui) }
        val aisOwnLatitude = pickValueOrNull(
            telemetry,
            listOf(
                "navigation.position.latitude",
                "position.latitude",
                "latitude",
                "navigation.position.lat",
                "position.lat",
                "lat",
            )
        )
        val aisOwnLongitude = pickValueOrNull(
            telemetry,
            listOf(
                "navigation.position.longitude",
                "position.longitude",
                "longitude",
                "navigation.position.lon",
                "position.lon",
                "lon",
                "lng",
            )
        )
        var nowForAisRenderMs by remember { mutableStateOf(System.currentTimeMillis()) }
        val systemWidgetSignature = page.widgets.joinToString("|") { "${it.id}:${it.kind}:${it.title}" }
        val activityManager = remember(context) {
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        }
        var systemPerformanceSnapshot by remember(page.id, systemWidgetSignature) {
            mutableStateOf(
                SystemWidgetLoadSnapshot(
                    cpuPercent = 0f,
                    memoryMb = 0f,
                    javaHeapMb = 0f,
                    maxHeapMb = Runtime.getRuntime().maxMemory().toFloat() / (1024f * 1024f),
                    loadEntries = estimateSystemWidgetLoadEntries(page.widgets),
                ),
            )
        }

        LaunchedEffect(systemWidgetSignature) {
            systemPerformanceSnapshot = systemPerformanceSnapshot.copy(
                loadEntries = estimateSystemWidgetLoadEntries(page.widgets),
            )
        }

        LaunchedEffect(page.id, systemWidgetSignature) {
            val runtime = Runtime.getRuntime()
            var lastWallClockMs = SystemClock.elapsedRealtime()
            var lastCpuMs = Process.getElapsedCpuTime()
            val processId = Process.myPid()

            while (true) {
                delay(SYSTEM_PERFORMANCE_SAMPLE_INTERVAL_MS)
                val nowWallClockMs = SystemClock.elapsedRealtime()
                val nowCpuMs = Process.getElapsedCpuTime()
                val deltaWallMs = (nowWallClockMs - lastWallClockMs).coerceAtLeast(1L)
                val deltaCpuMs = (nowCpuMs - lastCpuMs).coerceAtLeast(0L)
                val cpuPercent = (deltaCpuMs.toFloat() / deltaWallMs.toFloat()) * 100f
                val javaHeapBytes = runtime.totalMemory() - runtime.freeMemory()
                val maxHeapBytes = runtime.maxMemory()
                val processMemoryMb = runCatching {
                    activityManager
                        ?.getProcessMemoryInfo(intArrayOf(processId))
                        ?.firstOrNull()
                        ?.totalPss
                        ?.toFloat()
                        ?.div(1024f)
                }.getOrNull() ?: (javaHeapBytes.toFloat() / (1024f * 1024f))

                systemPerformanceSnapshot = systemPerformanceSnapshot.copy(
                    cpuPercent = cpuPercent,
                    memoryMb = processMemoryMb,
                    javaHeapMb = javaHeapBytes.toFloat() / (1024f * 1024f),
                    maxHeapMb = maxHeapBytes.toFloat() / (1024f * 1024f),
                    loadEntries = estimateSystemWidgetLoadEntries(page.widgets),
                )

                lastWallClockMs = nowWallClockMs
                lastCpuMs = nowCpuMs
            }
        }

        LaunchedEffect(
            aisTelemetry,
            aisTelemetryText,
            aisOwnHeadingDeg,
            aisOwnLatitude,
            aisOwnLongitude,
        ) {
            val nowMs = System.currentTimeMillis()
            val previousKeys = cachedAisTargetsByWidget.keys.toSet()
            val incomingKeys = mutableSetOf<String>()
                val parsedAis = parseAisTargetsFromTelemetry(
                    telemetry = aisTelemetry,
                    telemetryText = aisTelemetryText,
                    ownHeadingDeg = aisOwnHeadingDeg,
                    ownLat = aisOwnLatitude,
                    ownLon = aisOwnLongitude,
                    receivedAtMs = nowMs,
                )
            parsedAis.forEach { incoming ->
                val key = aisTargetCacheKey(incoming)
                incomingKeys.add(key)
                val merged = cachedAisTargetsByWidget[key]?.target?.let { existing ->
                    mergeCachedAisTarget(existing, incoming)
                } ?: incoming
                cachedAisTargetsByWidget[key] = CachedAisTargetState(
                    target = merged,
                    receivedAtMs = merged.lastSeenMs.takeIf { it > 0L } ?: nowMs,
                )
            }

            if (cachedAisTargetsByWidget.size > AIS_TARGET_CACHE_MAX_ENTRIES) {
                val oldest = cachedAisTargetsByWidget.entries
                    .sortedBy { it.value.receivedAtMs }
                    .take(cachedAisTargetsByWidget.size - AIS_TARGET_CACHE_MAX_ENTRIES)
                oldest.forEach { stale -> cachedAisTargetsByWidget.remove(stale.key) }
            }

            val hardDeleteTimeoutMs = AIS_TARGET_CACHE_TTL_MAX_MINUTES * 60_000L
            cachedAisTargetsByWidget.entries
                .filter { it.value.receivedAtMs <= nowMs - hardDeleteTimeoutMs }
                .forEach { stale -> cachedAisTargetsByWidget.remove(stale.key) }

            val updatedKeys = previousKeys.intersect(incomingKeys)
            val addedKeys = incomingKeys - previousKeys
            val removedKeys = previousKeys - cachedAisTargetsByWidget.keys.toSet()
            val currentKeys = cachedAisTargetsByWidget.keys.toSet()
            if (addedKeys.isNotEmpty() || removedKeys.isNotEmpty() || parsedAis.isNotEmpty()) {
                val shouldLog = nowMs - lastAisTargetLogAtMs >= AIS_TARGET_LOG_INTERVAL_MS
                if (shouldLog) {
                    val updatedFrames = parsedAis.joinToString { aisTargetLogLabel(it) }.ifBlank { "none" }
                    val addedFrames = addedKeys.joinToString { it.ifBlank { "unbekannt" } }.ifBlank { "none" }
                    val removedFrames = removedKeys.joinToString().ifBlank { "none" }
                    val updated = updatedKeys.joinToString().ifBlank { "none" }
                    lastAisTargetLogAtMs = nowMs
                Log.d(
                    AIS_TARGET_LOG_PREFIX,
                    "parsed=${parsedAis.size} cached=${currentKeys.size} " +
                        "added=$addedFrames " +
                        "updated=$updated " +
                            "removed=$removedFrames " +
                            "frames=$updatedFrames",
                    )
                }
            }

            if (parsedAis.isNotEmpty()) {
                nowForAisRenderMs = nowMs
            }
        }

        val cachedAisTargets: List<CachedAisTargetState> = cachedAisTargetsByWidget
            .values
            .toList()
        val aisVisibilitySettingsSignature = aisSettingsByWidget.entries
            .sortedBy { it.key }
            .joinToString("|") { (id, settings) ->
                val perTargetSettings = settings.targetVisibilityMinutesByMmsi.entries
                    .sortedBy { it.key }
                    .joinToString(",") { (mmsi, timeout) -> "$mmsi:$timeout" }
                    .ifBlank { "none" }
                "${id}:${settings.targetVisibilityMinutes}:$perTargetSettings"
            }
        val cachedAisTargetsSignature = cachedAisTargetsByWidget.entries
            .sortedBy { it.key }
            .joinToString("|") { "${it.key}:${it.value.receivedAtMs}" }
        LaunchedEffect(cachedAisTargetsSignature, aisVisibilitySettingsSignature) {
            val configuredSettings = aisSettingsByWidget.values.ifEmpty {
                listOf(AisWidgetSettings())
            }
            while (cachedAisTargetsByWidget.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                val configuredTargets = cachedAisTargetsByWidget.entries.toList()
                val expiredTargets = configuredTargets.filter { (_, cached) ->
                    val targetVisibilityMinutes = configuredSettings.maxOfOrNull { settings ->
                        resolveAisTargetVisibilityMinutes(cached.target, settings).toLong()
                    } ?: AIS_TARGET_CACHE_TTL_DEFAULT_MINUTES.toLong()
                    nowMs > cached.receivedAtMs + targetVisibilityMinutes * 60_000L
                }

                if (expiredTargets.isNotEmpty()) {
                    expiredTargets.forEach { (key, _) ->
                        cachedAisTargetsByWidget.remove(key)
                    }
                    nowForAisRenderMs = nowMs
                    if (cachedAisTargetsByWidget.isEmpty()) {
                        return@LaunchedEffect
                    }
                }

                val visibleTargets = cachedAisTargetsByWidget.entries
                    .map { it.value }
                    .filter { cached ->
                        val targetVisibilityMinutes = configuredSettings.maxOfOrNull { settings ->
                            resolveAisTargetVisibilityMinutes(cached.target, settings).toLong()
                        } ?: AIS_TARGET_CACHE_TTL_DEFAULT_MINUTES.toLong()
                        nowMs <= cached.receivedAtMs + targetVisibilityMinutes * 60_000L
                    }
                if (visibleTargets.isEmpty()) return@LaunchedEffect

                val nextExpiryMs = visibleTargets.minOfOrNull { cached ->
                    val targetVisibilityMinutes = configuredSettings.maxOfOrNull { settings ->
                        resolveAisTargetVisibilityMinutes(cached.target, settings).toLong()
                    } ?: AIS_TARGET_CACHE_TTL_DEFAULT_MINUTES.toLong()
                    cached.receivedAtMs + targetVisibilityMinutes * 60_000L
                } ?: return@LaunchedEffect

                val waitMs = (nextExpiryMs - System.currentTimeMillis()).coerceAtLeast(0L)
                if (waitMs > 0L) {
                    delay(waitMs)
                }
                nowForAisRenderMs = System.currentTimeMillis()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(page.id, page.widgets.size) {
                    awaitEachGesture {
                        val initialEvent = awaitPointerEvent()
                        val down = initialEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                        val pointerId = down.id
                        val startX = down.position.x
                        val startY = down.position.y

                        val interrupted = withTimeoutOrNull(1000L) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: return@withTimeoutOrNull true
                                val dx = change.position.x - startX
                                val dy = change.position.y - startY
                                val moved = (dx * dx + dy * dy) > (30f * 30f)
                                if (!change.pressed || moved) {
                                    return@withTimeoutOrNull true
                                }
                            }
                        }

                        if (interrupted == null) {
                            showAddWidgetFromSurface = true
                        }
                    }
                }
        )

        page.widgets.forEach { widget ->
            val count = (titleCount[widget.title] ?: 0) + 1
            titleCount[widget.title] = count
            val displayTitle = if ((titleTotalCount[widget.title] ?: 0) > 1) {
                "${widget.title} $count"
            } else {
                widget.title
            }
            val widgetAlarmActiveRaw = widgetAlarmStateByWidget[widget.id] == true
            val widgetAlarmMuted = if (widget.kind == WidgetKind.AIS) {
                aisSettingsByWidget[widget.id]?.collisionAlarmsMuted == true
            } else {
                widgetAlarmMutedByWidget[widget.id] == true
            }
            val widgetAlarmActive = widgetAlarmActiveRaw
            val onWidgetAlarmMute: () -> Unit = if (widget.kind == WidgetKind.AIS) {
                {
                    val current = aisSettingsByWidget.getOrPut(widget.id) { AisWidgetSettings() }
                    val updated = current.copy(collisionAlarmsMuted = !current.collisionAlarmsMuted)
                    aisSettingsByWidget[widget.id] = updated
                    onUpdateAisWidgetSettings(
                        widget.id,
                        serializeAisWidgetSettings(updated),
                    )
                    widgetAlarmMutedByWidget[widget.id] = updated.collisionAlarmsMuted
                }
            } else {
                {
                    if (widgetAlarmMuted) {
                        widgetAlarmMutedByWidget.remove(widget.id)
                    } else {
                        widgetAlarmMutedByWidget[widget.id] = true
                    }
                }
            }
            DraggableWidgetCard(
                title = displayTitle,
                titleScale = titleScale,
                gridStepPx = gridStepPx,
                widgetFrameStyle = widgetFrameStyle,
                widgetFrameStyleGrayOffset = widgetFrameStyleGrayOffset,
                darkBackground = darkBackground,
                x = widget.xPx,
                y = widget.yPx,
                width = widget.widthPx,
                height = widget.heightPx,
                onMove = { dx, dy, persistLayout ->
                    onMove(
                        widget.id,
                        dx,
                        dy,
                        gridStepPx,
                        pageWidthPx,
                        pageHeightPx,
                        persistLayout,
                    )
                },
                onResize = { dw, dh, persistLayout ->
                    onResize(
                        widget.id,
                        dw,
                        dh,
                        gridStepPx,
                        pageWidthPx,
                        pageHeightPx,
                        persistLayout,
                    )
                },
                canInteract = activeInteractingWidgetId == null || activeInteractingWidgetId == widget.id,
                onInteractionStart = {
                    activeInteractingWidgetId = widget.id
                },
                onInteractionEnd = {
                    if (activeInteractingWidgetId == widget.id) {
                        activeInteractingWidgetId = null
                    }
                },
                onSnap = {
                    onSnap(widget.id, gridStepPx, pageWidthPx, pageHeightPx)
                },
                onClose = {
                    onRemove(widget.id)
                    widgetAlarmStateByWidget.remove(widget.id)
                    widgetAlarmMutedByWidget.remove(widget.id)
                    seaChartSettingsByWidget.remove(widget.id)
                    seaChartMobByWidget.remove(widget.id)
                },
                hasSubmenu = widget.kind != WidgetKind.DALY_BMS,
                isAlarmActive = widgetAlarmActive,
                isAlarmMuted = widgetAlarmMuted,
                onMuteAlarm = onWidgetAlarmMute,
                onOpenSubmenu = {
                    widgetMenuWidgetId = widget.id
                    widgetRenameText = widget.title
                    if (widget.kind == WidgetKind.BATTERY) {
                        selectedBatteryChemistry = batterySettingsByWidget
                            .getOrPut(widget.id) { BatteryWidgetSettings() }
                            .chemistry
                    }
                    if (widget.kind == WidgetKind.AUTOPILOT) {
                        val settings = autopilotSettingsByWidget
                            .getOrPut(widget.id) { AutopilotWidgetSettings() }
                        selectedAutopilotTargetDevice = settings.targetDevice
                        selectedAutopilotGatewayBackend = settings.gatewayBackend
                        selectedAutopilotGatewayHost = settings.gatewayHost
                        selectedAutopilotGatewayPort = settings.gatewayPort.toString()
                        selectedAutopilotSignalKId = settings.signalKAutopilotId
                        selectedAutopilotRudderAverageSeconds = settings.rudderAverageSeconds
                        selectedAutopilotSafetyGateArmed = settings.safetyGateArmed
                        selectedAutopilotCommandTimeoutSeconds = (settings.commandTimeoutMs / 1000L)
                            .toInt()
                            .coerceIn(1, 30)
                        selectedAutopilotAuthToken = settings.authToken
                    }
                    if (widget.kind == WidgetKind.WIND) {
                        val settings = windSettingsByWidget.getOrPut(widget.id) {
                            WindWidgetSettings(tackingAngleDeg = sharedTackingAngleDeg)
                        }
                        selectedWindShowBoatDirection = settings.showBoatDirection
                        selectedWindShowNorthDirection = settings.showNorthDirection
                        selectedWindShowWindSpeed = settings.showWindSpeed
                        selectedWindSpeedUnit = settings.speedUnit
                        selectedWindHistoryWindowMinutes = settings.historyWindowMinutes
                        selectedWindMinMaxUsesTrueWind = settings.minMaxUsesTrueWind
                        sharedTackingAngleDeg = settings.tackingAngleDeg
                        showWindHistoryWindowMenu = false
                    }
                    if (widget.kind == WidgetKind.AIS) {
                        val settings = aisSettingsByWidget.getOrPut(widget.id) { AisWidgetSettings() }
                        selectedAisAlarmDistanceNm = settings.cpaAlarmDistanceNm.coerceIn(0.1f, 20f)
                        selectedAisAlarmMinutes = settings.cpaAlarmMinutes.coerceIn(0.5f, 60f)
                        selectedAisOrientationNorthUp = settings.northUp
                        selectedAisFontSizeOffsetSp = settings.fontSizeOffsetSp.coerceIn(-4, 2)
                        selectedAisTargetVisibilityMinutes = settings.targetVisibilityMinutes.coerceIn(1, 5)
                        selectedAisTargetVisibilityByMmsi = settings.targetVisibilityMinutesByMmsi
                            .filterKeys { key -> key.isNotBlank() }
                            .mapValues { it.value.coerceIn(1, 5) }
                    }
                    if (widget.kind == WidgetKind.ECHOSOUNDER) {
                        val settings = echosounderSettingsByWidget.getOrPut(widget.id) {
                            EchosounderWidgetSettings()
                        }
                        selectedEchoMinDepth = settings.minDepthMeters.coerceAtLeast(0.1f)
                        selectedEchoDynamicRate = settings.dynamicChangeRateMps.coerceAtLeast(0f)
                        selectedEchoDepthUnit = settings.depthUnit
                        selectedEchoAlarmTone = settings.alarmToneIndex
                    }
                    if (widget.kind == WidgetKind.LOG) {
                        val settings = logSettingsByWidget.getOrPut(widget.id) {
                            LogWidgetSettings(periodMinutes = 10)
                        }
                        selectedLogWindowMinutes = settings.periodMinutes.coerceIn(1, 120)
                        selectedLogSpeedSource = settings.speedSource
                    }
                    if (widget.kind == WidgetKind.ANCHOR_WATCH) {
                        val settings = anchorWatchSettingsByWidget.getOrPut(widget.id) {
                            AnchorWatchWidgetSettings()
                        }
                        selectedAnchorWatchEnabled = settings.monitoringEnabled
                        selectedAnchorWatchChainSensorEnabled = settings.chainSensorEnabled
                        selectedAnchorWatchChainLengthCalibrationEnabled = settings.chainLengthCalibrationEnabled
                        selectedAnchorWatchCalibratedChainLengthMeters = settings.calibratedChainLengthMeters.coerceAtLeast(0f)
                        selectedAnchorWatchDeviation = settings.maxDeviationPercent.coerceIn(0f, 30f)
                        selectedAnchorWatchChainCalibrationUnit = settings.chainCalibrationUnit
                        selectedAnchorWatchChainStrengthLabel = anchorWatchNormalizeChainStrengthLabel(
                            settings.chainCalibrationUnit,
                            settings.chainStrengthLabel,
                        )
                        selectedAnchorWatchChainPocketsText = settings.chainPockets.coerceAtLeast(1).toString()
                        selectedAnchorWatchAlarmTone = settings.alarmToneIndex.coerceIn(0, ECHO_SOUNDER_TONES.lastIndex)
                    }
                    if (widget.kind == WidgetKind.TEMPERATURE) {
                        val settings = temperatureSettingsByWidget.getOrPut(widget.id) {
                            TemperatureWidgetSettings()
                        }
                        selectedTemperatureUnit = settings.unit
                        selectedTemperatureSensorNames = normalizeTemperatureSensorNames(settings.sensors)
                    }
                    if (isSeaChartWidgetKind(widget.kind)) {
                        val settings = seaChartSettingsByWidget.getOrPut(widget.id) { SeaChartWidgetSettings() }
                        selectedSeaChartMapProvider = normalizedSelectableSeaChartProvider(settings.mapProvider)
                        selectedSeaChartSpeedSource = settings.speedSource
                        selectedSeaChartShowAisOverlay = settings.showAisOverlay
                        selectedSeaChartShowGribOverlay = settings.showGribOverlay
                        selectedSeaChartShowOpenSeaMapOverlay = settings.showOpenSeaMapOverlay
                        selectedSeaChartShowHeadingLine = settings.showHeadingLine
                        selectedSeaChartHeadingLineLengthNm = settings.headingLineLengthNm
                        selectedSeaChartShowCogVector = settings.showCogVector
                        selectedSeaChartCogVectorMinutes = settings.cogVectorMinutes
                        selectedSeaChartShowPredictor = settings.showPredictor
                        selectedSeaChartPredictorMinutes = settings.predictorMinutes
                        selectedSeaChartPredictorIntervalMinutes = settings.predictorIntervalMinutes
                        selectedSeaChartShowPredictorLabels = settings.showPredictorLabels
                        selectedSeaChartShowBoatIcon = settings.showBoatIcon
                        selectedSeaChartBoatIconSizeDp = settings.boatIconSizeDp
                        selectedSeaChartShowCourseLine = settings.showCourseLine
                        selectedSeaChartShowRoadLayers = settings.showRoadLayers
                        selectedSeaChartShowDepthLines = settings.showDepthLines
                        selectedSeaChartShowContourLines = settings.showContourLines
                        selectedSeaChartShowTrack = settings.showTrack
                        selectedSeaChartTrackDurationMinutes = settings.trackDurationMinutes
                        selectedSeaChartTrackRecordIntervalSeconds = settings.trackRecordIntervalSeconds
                        selectedSeaChartGuardZoneEnabled = settings.guardZoneEnabled
                        selectedSeaChartGuardZoneInnerNm = settings.guardZoneInnerNm
                        selectedSeaChartGuardZoneOuterNm = settings.guardZoneOuterNm
                        selectedSeaChartGuardZoneSectorStartDeg = settings.guardZoneSectorStartDeg
                        selectedSeaChartGuardZoneSectorEndDeg = settings.guardZoneSectorEndDeg
                        selectedSeaChartShowLaylines = settings.showLaylines
                        selectedSeaChartTackingAngleDeg = settings.tackingAngleDeg
                        selectedSeaChartLaylineLengthNm = settings.laylineLengthNm
                        selectedSeaChartShowSafetyContour = settings.showSafetyContour
                        selectedSeaChartSafetyDepthMeters = settings.safetyDepthMeters
                        selectedSeaChartCourseLineBearingDeg = settings.courseLineBearingDeg
                        selectedSeaChartCourseLineDistanceNm = settings.courseLineDistanceNm
                    }
                }
            ) {
                when (normalizeChartWidgetKind(widget.kind)) {
                    WidgetKind.BATTERY -> {
                        val settings = batterySettingsByWidget.getOrPut(widget.id) { BatteryWidgetSettings() }
                        val value = pickValue(telemetry, widget.dataKeys)
                        val powerWatts = pickValueOrNull(
                            telemetry,
                            listOf(
                                "battery_power",
                                "battery_power_w",
                                "charging_power",
                                "battery_charging_power",
                                "batt_power",
                            )
                        ) ?: run {
                            val voltage = pickValueOrNull(
                                telemetry,
                                listOf("battery_voltage", "batt_voltage", "voltage")
                            )
                            val current = pickValueOrNull(
                                telemetry,
                                listOf("battery_current", "batt_current", "current")
                            )
                            if (voltage != null && current != null) voltage * current else 0f
                        }

                        val cellMax = pickValueOrNull(
                            telemetry,
                            listOf(
                                "battery_cell_voltage_max",
                                "battery_cell_max",
                                "cell_voltage_max",
                                "lithium_cell_highest",
                            )
                        )
                        val cellMin = pickValueOrNull(
                            telemetry,
                            listOf(
                                "battery_cell_voltage_min",
                                "battery_cell_min",
                                "cell_voltage_min",
                                "lithium_cell_lowest",
                            )
                        )
                        val cellDeltaMillivolts = if (
                            settings.chemistry == BatteryChemistry.LITHIUM &&
                            cellMax != null &&
                            cellMin != null
                        ) {
                            val delta = abs(cellMax - cellMin)
                            if (cellMax <= 10f && cellMin <= 10f) delta * 1000f else delta
                        } else {
                            null
                        }

                        BatteryWidget(
                            level = value / 100f,
                            powerWatts = powerWatts,
                            chemistry = settings.chemistry,
                            cellDeltaMillivolts = cellDeltaMillivolts,
                            symbolBackgroundColor = if (darkBackground) {
                                Color(0xFF061A2C)
                            } else {
                                Color(0xFFEFF4FF)
                            },
                        )
                    }
                    WidgetKind.WATER_TANK -> {
                        val value = pickValue(telemetry, widget.dataKeys)
                        WaterTankWidget(level = value / 100f)
                    }
                    WidgetKind.BLACK_WATER_TANK -> {
                        val value = pickValue(telemetry, widget.dataKeys)
                        val tankSymbolBackground = if (darkBackground) {
                            Color(0xFF061A2C)
                        } else {
                            Color(0xFFEFF4FF)
                        }
                        BlackWaterWidget(
                            level = value / 100f,
                            tankBackgroundColor = tankSymbolBackground,
                        )
                    }
                    WidgetKind.GREY_WATER_TANK -> {
                        val value = pickValue(telemetry, widget.dataKeys)
                        GreyWaterWidget(level = value / 100f)
                    }
                    WidgetKind.TEMPERATURE -> {
                        val settings = temperatureSettingsByWidget.getOrPut(widget.id) {
                            TemperatureWidgetSettings()
                        }
                        val sensorNames = normalizeTemperatureSensorNames(settings.sensors)
                        val sensors = sensorNames.mapIndexed { index, name ->
                            TemperatureSensor(
                                name = name,
                                valueCelsius = pickTemperatureSensorValue(
                                    telemetry = telemetry,
                                    index = index,
                                    dataKeys = widget.dataKeys,
                                )
                            )
                        }
                        TemperatureWidget(
                            sensors = sensors,
                            unit = settings.unit,
                        )
                    }
                    WidgetKind.GPS -> {
                        val latitude = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.position.latitude",
                                "position.latitude",
                                "latitude",
                                "lat"
                            )
                        )
                        val longitude = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.position.longitude",
                                "position.longitude",
                                "longitude",
                                "lon",
                                "lng"
                            )
                        )
                        val courseOverGround = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.course_over_ground_true",
                                "course_over_ground",
                                "cog",
                                "course"
                            )
                        )
                        val speedOverGround = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.speed_over_ground",
                                "speed_over_ground",
                                "sog"
                            )
                        )
                        val heading = pickValueOrNull(
                            telemetry,
                            listOf(
                                "heading",
                                "compass_heading",
                                "autopilot_heading",
                                "navigation.course_over_ground_true",
                                "course_over_ground_true",
                                "course_over_ground",
                                "cog",
                            )
                        )
                        val altitude = pickValueOrNull(
                            telemetry,
                            listOf(
                                "gps_altitude_m",
                                "altitude_m",
                                "altitude"
                            )
                        )
                        val satellites = pickValueOrNull(
                            telemetry,
                            listOf(
                                "gps_satellites",
                                "satellites_in_view",
                                "satellites_in_use",
                                "satellites",
                                "satellite_count",
                                "satellite_count_in_view",
                                "gps_satellite_count",
                            )
                        )?.roundToInt()
                        val hdop = pickValueOrNull(
                            telemetry,
                            listOf("gps_hdop")
                        )
                        val fixQuality = pickValueOrNull(
                            telemetry,
                            listOf("gps_fix_quality")
                        )
                        val utcTime = telemetryText["gps_utc_time"]
                            ?: telemetryText["nmea0183_time"]
                            ?: telemetryText["gps_time"]
                            ?: telemetryText["time_utc"]
                            ?: telemetryText["utc_time"]
                            ?: telemetryText["utc"]

                        GpsWidget(
                            data = GpsWidgetData(
                                latitude = latitude,
                                longitude = longitude,
                                courseOverGround = courseOverGround,
                                speedOverGround = speedOverGround,
                                heading = heading,
                                altitudeM = altitude,
                                satellites = satellites,
                                hdop = hdop,
                                fixQuality = fixQuality,
                                utcTime = utcTime,
                            )
                        )
                    }
                    WidgetKind.NMEA_PGN -> {
                        NmeaPgnWidget(
                            detectedSources = detectedNmeaSources,
                            receivedPgnHistory = recentNmeaPgnHistory,
                            telemetryText = telemetryText,
                            darkBackground = darkBackground,
                        )
                    }
                    WidgetKind.NMEA0183 -> {
                        Nmea0183Widget(
                            detectedSources = detectedNmeaSources,
                            received0183History = recentNmea0183History,
                            telemetryText = telemetryText,
                            darkBackground = darkBackground,
                        )
                    }
                    WidgetKind.DALY_BMS -> {
                        DalyBmsWidget(
                            telemetry = telemetry,
                            telemetryText = telemetryText,
                            debugMessages = dalyDebugEvents,
                            darkBackground = darkBackground,
                        )
                    }
                    WidgetKind.LOG -> {
                        val settings = logSettingsByWidget.getOrPut(widget.id) { LogWidgetSettings() }
                        val currentSpeedKn = when (settings.speedSource) {
                            LogWidgetSpeedSource.GPS_SOG -> pickValueOrNull(
                                telemetry,
                                listOf(
                                    "navigation.speed_over_ground",
                                    "speed_over_ground",
                                    "sog",
                                    "sog_kn",
                                    "speed",
                                    "boat_speed",
                                )
                            )
                            LogWidgetSpeedSource.LOG_STW -> pickValueOrNull(
                                telemetry,
                                listOf(
                                    "navigation.speed_through_water",
                                    "speed_through_water",
                                    "stw",
                                    "water_speed",
                                    "boat_speed",
                                )
                            )
                        }
                        val samples = logSamplesByWidget.getOrPut(widget.id) { mutableListOf() }
                        val latestTimeMs = System.currentTimeMillis()

                        LaunchedEffect(telemetry, widget.id, settings.periodMinutes) {
                            val speed = currentSpeedKn
                            if (speed == null || !speed.isFinite()) return@LaunchedEffect
                            val nowMs = System.currentTimeMillis()
                            val lastSampleMs = logLastSampleMsByWidget[widget.id]
                            if (lastSampleMs != null) {
                                val dtH = (nowMs - lastSampleMs).coerceAtLeast(0L).toFloat() / 3_600_000f
                                if (dtH > 0f) {
                                    val previousTrip = logTripByWidget[widget.id] ?: 0f
                                    logTripByWidget[widget.id] = (previousTrip + speed * dtH).coerceAtLeast(0f)
                                }
                            }
                            logLastSampleMsByWidget[widget.id] = nowMs
                            samples.add(LogSpeedSample(timestampMs = nowMs, speedKn = speed))

                            val dayCutoff = nowMs - 24L * 60L * 60L * 1000L
                            samples.removeAll { it.timestampMs < dayCutoff }
                        }

                        val periodWindowMs = settings.periodMinutes.coerceIn(1, 120) * 60_000L
                        val periodCutoff = latestTimeMs - periodWindowMs
                        var periodTop: Float? = null
                        var periodMin: Float? = null
                        for (sample in samples) {
                            if (sample.timestampMs < periodCutoff) continue
                            if (!sample.speedKn.isFinite()) continue
                            if (periodTop == null || sample.speedKn > periodTop) {
                                periodTop = sample.speedKn
                            }
                            if (periodMin == null || sample.speedKn < periodMin) {
                                periodMin = sample.speedKn
                            }
                        }
                        val trend = when {
                            samples.size < 2 -> 0
                            samples.last().speedKn > samples[samples.size - 2].speedKn -> 1
                            samples.last().speedKn < samples[samples.size - 2].speedKn -> -1
                            else -> 0
                        }
                        val dailyDistanceNm = computeDistanceFromSamples(samples)
                        val tripNm = logTripByWidget[widget.id] ?: 0f
                        LogWidget(
                            currentSpeedKn = currentSpeedKn,
                            topSpeedKn = periodTop,
                            minSpeedKn = periodMin,
                            speedTrend = trend,
                            dailyNm24h = dailyDistanceNm,
                            tripNm = tripNm,
                            periodMinutes = settings.periodMinutes.coerceIn(1, 120),
                            speedSourceLabel = settings.speedSource.label,
                        )
                    }
                    WidgetKind.ANCHOR_WATCH -> {
                        val settings = anchorWatchSettingsByWidget.getOrPut(widget.id) {
                            AnchorWatchWidgetSettings()
                        }
                        val gpsLatitude = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.position.latitude",
                                "position.latitude",
                                "latitude",
                                "lat"
                            )
                        )
                        val gpsLongitude = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.position.longitude",
                                "position.longitude",
                                "longitude",
                                "lon",
                                "lng"
                            )
                        )
                        val chainTelemetry = if (settings.chainSensorEnabled) {
                            pickAnchorChainLengthFromTelemetry(telemetry)
                        } else {
                            null
                        }
                        val chainSignalDetected = settings.chainSensorEnabled && chainTelemetry != null
                        val chainSignalRaw = if (chainTelemetry?.isSignalCount == true) chainTelemetry.rawValue else null
                        val chainSourceDetected = chainTelemetry?.sourceKey
                        val runtime = anchorWatchRuntimeByWidget.getOrPut(widget.id) { AnchorWatchRuntimeState() }
                        var effectiveSettings = if (
                            settings.chainSensorEnabled &&
                            chainSignalDetected &&
                            !settings.monitoringEnabled
                        ) {
                            val autoEnabled = settings.copy(monitoringEnabled = true)
                            anchorWatchSettingsByWidget[widget.id] = autoEnabled
                            onUpdateAnchorWatchWidgetSettings(
                                widget.id,
                                serializeAnchorWatchWidgetSettings(autoEnabled),
                            )
                            autoEnabled
                        } else {
                            settings
                        }
                        val chainLengthMeters = chainTelemetry?.let { telemetryValue ->
                            if (telemetryValue.isSignalCount) {
                                val signalLengthMeters = anchorWatchChainSignalLengthToMeters(
                                    effectiveSettings.chainSignalLength,
                                )
                                telemetryValue.lengthMeters * signalLengthMeters
                            } else {
                                telemetryValue.lengthMeters
                            }
                        }
                        if (effectiveSettings.chainLengthCalibrationEnabled && chainLengthMeters != null) {
                            val candidateLength = chainLengthMeters.coerceAtLeast(effectiveSettings.calibratedChainLengthMeters)
                            if (candidateLength > effectiveSettings.calibratedChainLengthMeters) {
                                val calibratedSettings = effectiveSettings.copy(
                                    calibratedChainLengthMeters = candidateLength,
                                )
                                effectiveSettings = calibratedSettings
                                anchorWatchSettingsByWidget[widget.id] = calibratedSettings
                                onUpdateAnchorWatchWidgetSettings(
                                    widget.id,
                                    serializeAnchorWatchWidgetSettings(calibratedSettings),
                                )
                            }
                        }
                        val configuredChainLengthMeters = if (
                            effectiveSettings.chainSensorEnabled && effectiveSettings.chainLengthCalibrationEnabled
                        ) {
                            effectiveSettings.calibratedChainLengthMeters.coerceAtLeast(0f)
                        } else {
                            DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS
                        }
                        val chainLengthForRuntime = if (chainLengthMeters != null && effectiveSettings.chainLengthCalibrationEnabled) {
                            chainLengthMeters.coerceAtLeast(configuredChainLengthMeters)
                        } else {
                            configuredChainLengthMeters
                        }
                        val anchorWatchHeadingDeg = pickValueOrNull(
                            telemetry,
                            headingWithCourseOverGroundKeys,
                        )?.takeIf { it.isFinite() }
                        val nextSignalValue = chainSignalRaw ?: runtime.lastAnchorSignalValue
                        val chainLengthForLogic = chainLengthForRuntime?.let {
                            if (chainLengthMeters != null) {
                                (it - runtime.chainLengthResetOffsetMeters).coerceAtLeast(0f)
                            } else {
                                it
                            }
                        }
                        val updatedRuntime = runtime.copy(
                            isSignalDetected = chainSignalDetected,
                            lastKnownChainLengthMeters = chainLengthForRuntime,
                            lastAnchorChainSource = if (settings.chainSensorEnabled) {
                                chainSourceDetected ?: runtime.lastAnchorChainSource
                            } else {
                                null
                            },
                            lastAnchorSignalValue = nextSignalValue,
                            chainLengthResetOffsetMeters = runtime.chainLengthResetOffsetMeters,
                            wasAlarm = false,
                        )
                        val nextAnchorSet = if (
                            effectiveSettings.monitoringEnabled &&
                            chainLengthForLogic != null &&
                            chainLengthForLogic > 5f &&
                            !runtime.isAnchorSet
                        ) {
                            gpsLatitude != null && gpsLongitude != null
                        } else {
                            runtime.isAnchorSet
                        }
                        val nextRuntime = when {
                            runtime.isAnchorSet -> {
                                updatedRuntime
                            }

                            nextAnchorSet && gpsLatitude != null && gpsLongitude != null && chainLengthForLogic != null -> {
                                updatedRuntime.copy(
                                    anchorLatitude = gpsLatitude,
                                    anchorLongitude = gpsLongitude,
                                    isAnchorSet = true,
                                    lastChainLengthWhenSet = chainLengthForLogic,
                                )
                            }

                            else -> {
                                updatedRuntime
                            }
                        }
                        if (nextRuntime != runtime) {
                            anchorWatchRuntimeByWidget[widget.id] = nextRuntime
                        }
                        val anchorDistanceMeters = run {
                            val anchorLat = nextRuntime.anchorLatitude
                            val anchorLon = nextRuntime.anchorLongitude
                            if (anchorLat != null && anchorLon != null && gpsLatitude != null && gpsLongitude != null) {
                                computeDistanceMeters(gpsLatitude, gpsLongitude, anchorLat, anchorLon)
                            } else {
                                null
                            }
                        }
                        val anchorToBoatBearingDeg = run {
                            val anchorLat = nextRuntime.anchorLatitude
                            val anchorLon = nextRuntime.anchorLongitude
                            if (anchorLat != null && anchorLon != null && gpsLatitude != null && gpsLongitude != null) {
                                computeGpsBearingNm(anchorLat, anchorLon, gpsLatitude, gpsLongitude)
                            } else {
                                null
                            }
                        }
                        val alertDistanceMeters = chainLengthForLogic?.let {
                            val tolerancePercent = effectiveSettings.maxDeviationPercent.coerceIn(0f, 30f)
                            it * (1f + tolerancePercent / 100f)
                        }
                        val alarm = if (
                            effectiveSettings.monitoringEnabled &&
                            chainLengthForLogic != null &&
                            anchorDistanceMeters != null &&
                            alertDistanceMeters != null
                        ) {
                            anchorDistanceMeters > alertDistanceMeters
                        } else {
                            false
                        }
                        LaunchedEffect(
                            alarm,
                            widgetAlarmMuted,
                            effectiveSettings.alarmToneIndex,
                            widget.id,
                            alarmToneVolume,
                            alarmRepeatIntervalSeconds,
                        ) {
                            if (!alarm || widgetAlarmMuted) {
                                return@LaunchedEffect
                            }

                            val alarmFrequency = ECHO_SOUNDER_TONES
                                .getOrNull(effectiveSettings.alarmToneIndex)
                                ?.frequencyHz
                                ?: ECHO_SOUNDER_TONES.first().frequencyHz
                            val repeatIntervalMs = alarmRepeatIntervalSeconds.coerceIn(2, 10) * 1000L
                            applySystemAlarmPresetVolume(
                                context = context,
                                preset = alarmToneVolume,
                            )

                            while (true) {
                                playWidgetAlarmTone(
                                    frequencyHz = alarmFrequency,
                                    volume = alarmToneVolume.coerceIn(0f, 2f),
                                )
                                onAnchorWatchAlarm(widget.title)
                                delay(repeatIntervalMs)
                            }
                        }
                        val currentRadiusMeters = alertDistanceMeters
                        val finalRuntime = nextRuntime.copy(wasAlarm = alarm)
                        if (finalRuntime != nextRuntime) {
                            anchorWatchRuntimeByWidget[widget.id] = finalRuntime
                        }
                        widgetAlarmStateByWidget[widget.id] = alarm
                        if (!alarm) {
                            widgetAlarmMutedByWidget.remove(widget.id)
                        }

                            AnchorWatchWidget(
                                monitoringEnabled = effectiveSettings.monitoringEnabled,
                                anchorSet = nextRuntime.isAnchorSet,
                                chainLengthMeters = chainLengthForLogic,
                                currentDistanceMeters = anchorDistanceMeters,
                                alertRadiusMeters = currentRadiusMeters,
                                boatBearingDeg = anchorToBoatBearingDeg,
                                alarmActive = alarm,
                                displayUnit = effectiveSettings.maxDeviationUnit,
                                unitLabel = effectiveSettings.maxDeviationUnit.label,
                                ringCutoutStartAngleDeg = effectiveSettings.ringCutoutStartAngleDeg,
                                ringCutoutSweepAngleDeg = effectiveSettings.ringCutoutSweepAngleDeg,
                                onRingCutoutDrag = { startAngle, sweepAngle ->
                                    val normalizedStartAngle = ((startAngle % 360f) + 360f) % 360f
                                    val normalizedSweep = ((sweepAngle % 360f) + 360f) % 360f
                                    if (
                                        normalizedStartAngle == effectiveSettings.ringCutoutStartAngleDeg &&
                                        normalizedSweep == effectiveSettings.ringCutoutSweepAngleDeg
                                    ) {
                                        // no-op: values unchanged
                                    } else {
                                        val updated = effectiveSettings.copy(
                                            ringCutoutStartAngleDeg = normalizedStartAngle,
                                            ringCutoutSweepAngleDeg = normalizedSweep,
                                        )
                                        anchorWatchSettingsByWidget[widget.id] = updated
                                        onUpdateAnchorWatchWidgetSettings(
                                            widget.id,
                                            serializeAnchorWatchWidgetSettings(updated),
                                        )
                                    }
                                },
                                darkBackground = darkBackground,
                            )
                    }
                    WidgetKind.WIND -> {
                        val settings = windSettingsByWidget.getOrPut(widget.id) {
                            WindWidgetSettings(tackingAngleDeg = sharedTackingAngleDeg)
                        }
                        val historyWindowMinutes = settings.historyWindowMinutes.coerceIn(5, 43200)
                        val heading = pickValue(telemetry, headingWithCourseOverGroundKeys)
                        val trueSpeed = pickValueOrNull(
                            telemetry,
                            listOf(
                                "wind_speed_true",
                                "true_wind_speed",
                                "tws",
                            )
                        )
                        val trueAngle = pickValueOrNull(
                            telemetry,
                            listOf(
                                "wind_angle_true",
                                "true_wind_angle",
                                "wind_direction_true",
                                "true_wind_direction"
                            )
                        )
                        val trueSpeedForRender = trueSpeed?.takeIf { it.isFinite() } ?: Float.NaN
                        val boatSpeedKn = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.speed_over_ground",
                                "navigation.speed_through_water",
                                "speed_over_ground",
                                "speed_through_water",
                                "sog",
                                "boat_speed",
                                "sog_kn"
                            )
                        ) ?: 0f
                        // Simulator uses boat speed in opposite sign when converting apparent/true wind.
                        // Keep raw SOG/SOW sign information so reversed motion is represented correctly.
                        val windCalcBoatSpeedKn = -boatSpeedKn
                        val apparentSpeed = pickValue(
                            telemetry,
                            listOf("wind_speed_apparent", "apparent_wind_speed", "aws")
                        )
                        val apparentSpeedOrNull = pickValueOrNull(
                            telemetry,
                            listOf("wind_speed_apparent", "apparent_wind_speed", "aws")
                        )
                        val apparentAngle = pickValueOrNull(
                            telemetry,
                            listOf("wind_angle_apparent", "apparent_wind_angle", "awa")
                        )
                        var apparentAngleForRender = apparentAngle.takeIf { it?.isFinite() == true } ?: Float.NaN
                        var apparentSpeedForRender = apparentSpeed
                        var displaySpeed = apparentSpeedOrNull?.takeIf { it.isFinite() } ?: apparentSpeedForRender
                        var trueWindAngleDeg = Float.NaN
                        var trueWindSpeedKn = Float.NaN
                        val trueWindAvailable = trueSpeedForRender.isFinite()
                        if (trueWindAvailable) {
                            trueWindSpeedKn = trueSpeedForRender
                            trueWindAngleDeg = trueAngle.takeIf { it?.isFinite() == true } ?: calculateTrueWindAngleDeg(
                                apparentSpeed = apparentSpeedForRender,
                                apparentAngleDeg = apparentAngleForRender,
                                boatSpeedKn = windCalcBoatSpeedKn
                            )
                            val derivedApparent = calculateApparentWindFromTrue(
                                trueSpeed = trueWindSpeedKn,
                                trueAngleRelativeDeg = trueWindAngleDeg,
                                boatSpeedKn = windCalcBoatSpeedKn
                            )
                            if (derivedApparent != null) {
                                apparentSpeedForRender = derivedApparent.first
                                apparentAngleForRender = derivedApparent.second
                                displaySpeed = apparentSpeedForRender
                            }
                            if (!displaySpeed.isFinite()) {
                                displaySpeed = trueWindSpeedKn
                            }
                        } else {
                            trueWindAngleDeg = calculateTrueWindAngleDeg(
                                apparentSpeed = apparentSpeedForRender,
                                apparentAngleDeg = apparentAngleForRender,
                                boatSpeedKn = windCalcBoatSpeedKn
                            )
                            trueWindSpeedKn = calculateTrueWindSpeedKn(
                                apparentSpeed = apparentSpeedForRender,
                                apparentAngleDeg = apparentAngleForRender,
                                boatSpeedKn = windCalcBoatSpeedKn
                            )
                            displaySpeed = apparentSpeed
                        }

                        val selectedSpeedForHistory = if (settings.minMaxUsesTrueWind) {
                            trueWindSpeedKn.takeIf { it.isFinite() }
                        } else {
                            apparentSpeedOrNull?.takeIf { it.isFinite() } ?: displaySpeed.takeIf { it.isFinite() }
                        }
                        val selectedSpeedForHistoryFallback = displaySpeed
                        val windSpeedHistory = windSpeedHistoryByWidget.getOrPut(widget.id) {
                            mutableListOf()
                        }
                        val windowMs = historyWindowMinutes * 60_000L
                        val maxHistorySamples = ((windowMs / 500L) + 100L).toInt().coerceAtLeast(240)

                        LaunchedEffect(
                            selectedSpeedForHistory,
                            selectedSpeedForHistoryFallback,
                            settings.historyWindowMinutes
                        ) {
                            if (selectedSpeedForHistory?.isFinite() == true) {
                                val nowMs = System.currentTimeMillis()
                                windSpeedHistory.add(WindSpeedSample(nowMs, selectedSpeedForHistory))
                                val cutoff = nowMs - windowMs
                                var staleCount = 0
                                while (staleCount < windSpeedHistory.size &&
                                    windSpeedHistory[staleCount].timestampMs < cutoff
                                ) {
                                    staleCount++
                                }
                                if (staleCount > 0) {
                                    windSpeedHistory.subList(0, staleCount).clear()
                                }
                                val overLimit = windSpeedHistory.size - maxHistorySamples
                                if (overLimit > 0) {
                                    windSpeedHistory.subList(0, overLimit).clear()
                                }
                            }
                        }

                        val historyWindowCutoff = System.currentTimeMillis() - windowMs
                        var historyMinKn = selectedSpeedForHistoryFallback
                        var historyMaxKn = selectedSpeedForHistoryFallback
                        if (windSpeedHistory.isNotEmpty()) {
                            var minInWindow = Float.POSITIVE_INFINITY
                            var maxInWindow = Float.NEGATIVE_INFINITY
                            for (sample in windSpeedHistory) {
                                if (sample.speedKn.isFinite() && sample.timestampMs >= historyWindowCutoff) {
                                    if (sample.speedKn < minInWindow) {
                                        minInWindow = sample.speedKn
                                    }
                                    if (sample.speedKn > maxInWindow) {
                                        maxInWindow = sample.speedKn
                                    }
                                }
                            }
                            if (minInWindow.isFinite()) {
                                historyMinKn = minInWindow
                            }
                            if (maxInWindow.isFinite()) {
                                historyMaxKn = maxInWindow
                            }
                        }
                        WindWidget(
                            apparentAngleDeg = apparentAngleForRender,
                            apparentSpeedKn = displaySpeed,
                            absoluteSpeedKn = trueWindSpeedKn.takeIf { it.isFinite() } ?: apparentSpeedForRender.takeIf {
                                it.isFinite()
                            } ?: apparentSpeed,
                            headingDeg = heading,
                            trueWindAngleDeg = trueWindAngleDeg,
                            uiFont = uiFont,
                            settings = settings,
                            historyMinKn = historyMinKn,
                            historyMaxKn = historyMaxKn,
                            darkBackground = darkBackground,
                            onToggleMinMaxSource = {
                                val currentSettings = windSettingsByWidget.getOrPut(widget.id) {
                                    settings
                                }
                                val updatedSettings = currentSettings.copy(
                                    minMaxUsesTrueWind = !currentSettings.minMaxUsesTrueWind
                                )
                                windSettingsByWidget[widget.id] = updatedSettings
                                windSpeedHistoryByWidget[widget.id]?.clear()
                                if (widgetMenuWidgetId == widget.id) {
                                    selectedWindMinMaxUsesTrueWind = updatedSettings.minMaxUsesTrueWind
                                }
                                onUpdateWindWidgetSettings(
                                    widget.id,
                                    serializeWindWidgetSettings(updatedSettings),
                                )
                            },
                        )
                    }
                    WidgetKind.COMPASS -> {
                        val heading = pickValueOrNull(
                            telemetry,
                            headingWithCourseOverGroundKeys
                        )
                        val courseOverGround = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.course_over_ground_true",
                                "course_over_ground_true",
                                "course_over_ground",
                                "cog"
                            )
                        )

                        CompassWidget(
                            headingDeg = heading,
                            courseOverGroundDeg = courseOverGround,
                        )
                    }
                    WidgetKind.KARTEN -> {
                        val settings = seaChartSettingsByWidget.getOrPut(widget.id) { SeaChartWidgetSettings() }
                        val ownLongitude = aisOwnLongitude
                        val ownLatitude = aisOwnLatitude
                        val ownCourseOverGroundDeg = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.course_over_ground_true",
                                "course_over_ground_true",
                                "course_over_ground",
                                "cog",
                            )
                        )
                        val gpsSpeedKn = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.speed_over_ground",
                                "speed_over_ground",
                                "sog",
                            )
                        )
                        val stwSpeedKn = pickValueOrNull(
                            telemetry,
                            listOf(
                                "navigation.speed_through_water",
                                "speed_through_water",
                                "stw",
                            )
                        )
                        val ownSpeedKn = when (settings.speedSource) {
                            SeaChartSpeedSource.GPS_SOG -> gpsSpeedKn
                            SeaChartSpeedSource.LOG_STW -> stwSpeedKn ?: gpsSpeedKn
                        }
                        val absoluteTrueWindDirectionDeg = pickValueOrNull(
                            telemetry,
                            listOf(
                                "wind_direction_true",
                                "true_wind_direction",
                                "twd",
                                "true_wind_direction_deg",
                            )
                        ) ?: run {
                            val relativeTrueWindAngle = pickValueOrNull(
                                telemetry,
                                listOf(
                                    "wind_angle_true",
                                    "true_wind_angle",
                                    "twa",
                                    "true_wind_angle_deg",
                                )
                            )
                            val headingForWind = aisOwnHeadingDeg ?: ownCourseOverGroundDeg
                            if (relativeTrueWindAngle != null && headingForWind != null) {
                                wrap360Ui(headingForWind + relativeTrueWindAngle)
                            } else {
                                null
                            }
                        }
                        var kartenScreenReady by remember(widget.id, kartenOfflineReloadRequest) {
                            mutableStateOf(false)
                        }
                        LaunchedEffect(widget.id, kartenOfflineReloadRequest) {
                            kartenScreenReady = false
                            kotlinx.coroutines.delay(900L)
                            kartenScreenReady = true
                        }
                        if (!kartenScreenReady) {
                            val loadingText = if (kartenOfflineReloadRequest > 0) {
                                "Offlinekarten werden neu geladen.\nDas kann bis zu 3 Minuten dauern."
                            } else {
                                "Karten wird vorbereitet..."
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = loadingText,
                                    color = Color(0xFFE6F0F7),
                                )
                            }
                        } else {
                            val provider = settings.mapProvider
                            var kartenViewportCenterLat by remember(widget.id) { mutableStateOf<Double?>(null) }
                            var kartenViewportCenterLon by remember(widget.id) { mutableStateOf<Double?>(null) }
                            var kartenZoomBucket by remember(widget.id) { mutableIntStateOf(10) }
                            val initialKartenSourcePair = remember(widget.id, kartenOfflineReloadRequest, provider) {
                                if (provider == SeaChartMapProvider.OPEN_SEA_CHARTS) {
                                    return@remember "OpenSeaMap Seamarks" to null
                                }
                                // Fast path: grab first .000 candidate without expensive stat calls.
                                // Full preferred-path resolution happens in LaunchedEffect on IO thread.
                                val cachedSources = seaChartCachedSourceScan(context, provider)
                                val quickPath = cachedSources.sourceCandidatePaths.firstOrNull { path ->
                                    path.endsWith(".000", ignoreCase = true)
                                }
                                val sourceText = quickPath
                                    ?.let { File(it).nameWithoutExtension.ifBlank { "${provider.label}-Karte" } }
                                    ?: "${provider.label}-Karte wird geladen..."
                                sourceText to quickPath
                            }
                            var activeKartenSourceText by rememberSaveable(widget.id, kartenOfflineReloadRequest, provider.name) {
                                mutableStateOf(initialKartenSourcePair.first)
                            }
                            var activeKartenSourcePath by rememberSaveable(widget.id, kartenOfflineReloadRequest, provider.name) {
                                mutableStateOf(initialKartenSourcePair.second)
                            }

                            LaunchedEffect(
                                widget.id,
                                kartenOfflineReloadRequest,
                                provider,
                                ownLatitude,
                                ownLongitude,
                                kartenViewportCenterLat,
                                kartenViewportCenterLon,
                                kartenZoomBucket,
                            ) {
                                val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    if (provider == SeaChartMapProvider.OPEN_SEA_CHARTS) {
                                        return@withContext "OpenSeaMap Seamarks" to null
                                    }
                                    val sourcePath = activeSeaChartSourcePath(
                                        context = context,
                                        mapProvider = provider,
                                        ownLatitude = ownLatitude,
                                        ownLongitude = ownLongitude,
                                        selectionCenterLatitude = kartenViewportCenterLat,
                                        selectionCenterLongitude = kartenViewportCenterLon,
                                        zoomLevel = kartenZoomBucket,
                                    )
                                        ?: preferredNoaaEncSourcePath(context, seaChartCachedSourceScan(context, provider).sourceCandidatePaths)
                                        ?: activeKartenSourcePath
                                    val sourceText = activeSeaChartSourceLabel(
                                        context = context,
                                        mapProvider = provider,
                                        preferredPath = sourcePath,
                                    )
                                    sourceText to sourcePath
                                }
                                activeKartenSourceText = resolved.first.ifBlank { "Keine ${provider.label}-Karte erkannt" }
                                activeKartenSourcePath = resolved.second
                            }

                            ChartWidget(
                                ownLatitude = ownLatitude,
                                ownLongitude = ownLongitude,
                                ownHeadingDeg = aisOwnHeadingDeg ?: ownCourseOverGroundDeg,
                                ownCourseOverGroundDeg = ownCourseOverGroundDeg,
                                ownSpeedKn = ownSpeedKn,
                                activeMapSourceLabel = if (provider == SeaChartMapProvider.OPEN_SEA_CHARTS) {
                                    "OpenSeaMap Seamarks"
                                } else {
                                    activeKartenSourceText
                                },
                                activeMapSourcePath = if (provider == SeaChartMapProvider.OPEN_SEA_CHARTS) null else activeKartenSourcePath,
                                aisTargets = cachedAisTargetsByWidget.values.map { it.target },
                                showAisOverlay = settings.showAisOverlay,
                                showOpenSeaMapOverlay = settings.showOpenSeaMapOverlay || provider == SeaChartMapProvider.OPEN_SEA_CHARTS,
                                navigationSettings = NavigationVectorSettings(
                                    showHeadingLine = settings.showHeadingLine,
                                    headingLineLengthNm = settings.headingLineLengthNm,
                                    showCogVector = settings.showCogVector,
                                    cogVectorMinutes = settings.cogVectorMinutes,
                                    showPredictor = settings.showPredictor,
                                    predictorMinutes = settings.predictorMinutes,
                                    predictorIntervalMinutes = settings.predictorIntervalMinutes,
                                    showPredictorLabels = settings.showPredictorLabels,
                                    showCourseLine = settings.showCourseLine,
                                    courseLineBearingDeg = settings.courseLineBearingDeg,
                                    courseLineDistanceNm = settings.courseLineDistanceNm,
                                    showBoatIcon = settings.showBoatIcon,
                                    boatIconSizeDp = settings.boatIconSizeDp,
                                    showTrack = settings.showTrack,
                                    trackDurationMinutes = settings.trackDurationMinutes,
                                    trackRecordIntervalSeconds = settings.trackRecordIntervalSeconds,
                                    guardZoneEnabled = settings.guardZoneEnabled,
                                    guardZoneInnerNm = settings.guardZoneInnerNm,
                                    guardZoneOuterNm = settings.guardZoneOuterNm,
                                    guardZoneSectorStartDeg = settings.guardZoneSectorStartDeg,
                                    guardZoneSectorEndDeg = settings.guardZoneSectorEndDeg,
                                    showLaylines = settings.showLaylines,
                                    tackingAngleDeg = settings.tackingAngleDeg,
                                    laylineLengthNm = settings.laylineLengthNm,
                                    showSafetyContour = settings.showSafetyContour,
                                    safetyDepthMeters = settings.safetyDepthMeters,
                                ),
                                activeRoute = activeRoute,
                                mobLat = mobLat,
                                mobLon = mobLon,
                                mobTimestampMs = mobTimestampMs,
                                trueWindDirectionDeg = absoluteTrueWindDirectionDeg,
                                boatDraftMeters = boatDraftMeters.takeIf { it.isFinite() && it >= 0f },
                                mobActive = mobLat != null && mobLon != null,
                                onMobToggle = {
                                    if (mobLat != null && mobLon != null) {
                                        onClearMob()
                                    } else {
                                        onTriggerMob()
                                    }
                                },
                                nauticalOverlayOptions = NauticalOverlayOptions(
                                    showRoadLayers = settings.showRoadLayers,
                                    showDepthLayers = settings.showDepthLines,
                                    showContourLines = settings.showContourLines,
                                ),
                                onViewportChanged = { centerLat, centerLon, zoomBucket ->
                                    kartenViewportCenterLat = centerLat
                                    kartenViewportCenterLon = centerLon
                                    kartenZoomBucket = zoomBucket
                                },
                            )
                        }
                    }
                    WidgetKind.SEA_CHART, WidgetKind.SEA_CHART_PIXEL -> Unit
                    WidgetKind.ECHOSOUNDER -> {
                        val settings = echosounderSettingsByWidget.getOrPut(widget.id) {
                            EchosounderWidgetSettings()
                        }
                        val currentDepth = pickValueOrNull(
                            telemetry,
                            listOf("water_depth_m", "water_depth", "depth_m", "depth")
                        ) ?: 0f
                        val sogKn = pickValueOrNull(
                            telemetry,
                            listOf("navigation.speed_over_ground", "sog", "speed_over_ground")
                        )

                            EchosounderWidget(
                                currentDepthM = currentDepth,
                                depthSettings = settings,
                                widgetTitle = displayTitle,
                                sogKn = sogKn,
                                alarmToneVolume = alarmToneVolume,
                                alarmRepeatIntervalSeconds = alarmRepeatIntervalSeconds,
                                isMuted = widgetAlarmMuted,
                                onAlarmTonePlayed = onAnchorWatchAlarm,
                                onAlarmStateChange = { hasAlarm ->
                                    widgetAlarmStateByWidget[widget.id] = hasAlarm
                                if (!hasAlarm) {
                                    widgetAlarmMutedByWidget.remove(widget.id)
                                }
                            },
                            onEditMinDepth = {
                                selectedEchoMinDepth = settings.minDepthMeters.coerceAtLeast(0.1f)
                                selectedEchoDynamicRate = settings.dynamicChangeRateMps.coerceAtLeast(0f)
                                selectedEchoDepthUnit = settings.depthUnit
                                selectedEchoAlarmTone = settings.alarmToneIndex
                                showEchosounderMinDepthDialog = widget.id
                            },
                            onEditDynamic = {
                                selectedEchoMinDepth = settings.minDepthMeters.coerceAtLeast(0.1f)
                                selectedEchoDynamicRate = settings.dynamicChangeRateMps.coerceAtLeast(0f)
                                selectedEchoDepthUnit = settings.depthUnit
                                selectedEchoAlarmTone = settings.alarmToneIndex
                                showEchosounderDynamicDialog = widget.id
                            },
                        )
                    }
                    WidgetKind.AIS -> {
                        val ownHeadingDeg = aisOwnHeadingDeg
                        val ownSpeedKn = pickValueOrNull(
                            telemetry,
                            listOf("navigation.speed_over_ground", "speed_over_ground", "sog")
                        )
                        val ownLat = aisOwnLatitude
                        val ownLon = aisOwnLongitude
                        val settings = aisSettingsByWidget.getOrPut(widget.id) { AisWidgetSettings() }
                        val effectiveSettings = if (widgetMenuWidgetId == widget.id) {
                            settings.copy(
                                cpaAlarmDistanceNm = selectedAisAlarmDistanceNm.coerceIn(0.1f, 20f),
                                cpaAlarmMinutes = selectedAisAlarmMinutes.coerceIn(0.5f, 60f),
                                northUp = selectedAisOrientationNorthUp,
                                fontSizeOffsetSp = selectedAisFontSizeOffsetSp.coerceIn(-4, 2),
                                targetVisibilityMinutes = selectedAisTargetVisibilityMinutes.coerceIn(1, 5),
                                targetVisibilityMinutesByMmsi = selectedAisTargetVisibilityByMmsi
                                    .filterValues { value -> value in 1..5 }
                                    .filterKeys { key -> key.isNotBlank() },
                            )
                        } else {
                            settings
                        }
                        val aisSignals = cachedAisTargets.mapNotNull { cached ->
                            val visibilityMinutes = resolveAisTargetVisibilityMinutes(
                                cached.target,
                                effectiveSettings,
                            )
                            val expiresAtMs = cached.receivedAtMs + visibilityMinutes * 60_000L
                            if (nowForAisRenderMs <= expiresAtMs) {
                                AisTargetWithAge(
                                    target = cached.target,
                                    receivedAtMs = cached.receivedAtMs,
                                    visibilityTimeoutMinutes = visibilityMinutes,
                                )
                            } else {
                                null
                            }
                        }

                        AisWidget(
                            targets = aisSignals,
                            ownHeadingDeg = ownHeadingDeg,
                            ownLatitudeDeg = ownLat,
                            ownLongitudeDeg = ownLon,
                            ownSpeedKn = ownSpeedKn,
                            widgetTitle = displayTitle,
                            settings = effectiveSettings,
                            renderNowMs = nowForAisRenderMs,
                            darkBackground = darkBackground,
                            alarmToneVolume = alarmToneVolume,
                            alarmRepeatIntervalSeconds = alarmRepeatIntervalSeconds,
                            isMuted = widgetAlarmMuted,
                            onAlarmTonePlayed = onAnchorWatchAlarm,
                            onIncreaseRange = {
                                val currentRange = effectiveSettings.displayRangeNm.takeIf { it.isFinite() } ?: 10f
                                val newRange = currentRange.coerceAtLeast(5f) + 5f
                                if (newRange != effectiveSettings.displayRangeNm) {
                                    val updated = effectiveSettings.copy(displayRangeNm = newRange)
                                    aisSettingsByWidget[widget.id] = updated
                                    onUpdateAisWidgetSettings(
                                        widget.id,
                                        serializeAisWidgetSettings(updated),
                                    )
                            }
                        },
                            onDecreaseRange = {
                                val currentRange = effectiveSettings.displayRangeNm.takeIf { it.isFinite() } ?: 10f
                                val newRange = (currentRange.coerceAtLeast(5f) - 5f).coerceAtLeast(5f)
                                if (newRange != effectiveSettings.displayRangeNm) {
                                    val updated = effectiveSettings.copy(displayRangeNm = newRange)
                                    aisSettingsByWidget[widget.id] = updated
                                    onUpdateAisWidgetSettings(
                                        widget.id,
                                        serializeAisWidgetSettings(updated),
                                    )
                                }
                            },
                            onAlarmStateChange = { hasAlarm ->
                                widgetAlarmStateByWidget[widget.id] = hasAlarm
                                if (!hasAlarm) {
                                    widgetAlarmMutedByWidget.remove(widget.id)
                                }
                            },
                            onCallMmsi = onCallAisMmsi,
                        )
                    }
                    WidgetKind.AUTOPILOT -> {
                        val autopilotSettings = autopilotSettingsByWidget
                            .getOrPut(widget.id) { AutopilotWidgetSettings() }
                        val heading = pickValue(telemetry, headingWithCourseOverGroundKeys)
                        val rudderAngle = pickValueOrNull(
                            telemetry,
                            listOf("rudder", "rudder_angle", "rudder_position", "rudder_angle_deg")
                        )
                        val nowMs = System.currentTimeMillis()
                        val rudderHistory = rudderHistoryByWidget.getOrPut(widget.id) { mutableListOf() }
                        rudderAngle?.let { rudder ->
                            val last = rudderHistory.lastOrNull()
                            val shouldAppend = last == null ||
                                abs(last.second - rudder) > 0.05f ||
                                (nowMs - last.first) >= 250L
                            if (shouldAppend) {
                                rudderHistory.add(nowMs to rudder)
                            }
                        }
                        val averagingWindowMs = (autopilotSettings.rudderAverageSeconds.coerceIn(5, 300) * 1000L)
                        while (rudderHistory.isNotEmpty() && nowMs - rudderHistory.first().first > averagingWindowMs) {
                            rudderHistory.removeAt(0)
                        }
                        val avgRudderAngle = when {
                            rudderHistory.isNotEmpty() -> {
                                var total = 0f
                                for (point in rudderHistory) {
                                    total += point.second
                                }
                                total / rudderHistory.size
                            }
                            rudderAngle != null -> rudderAngle
                            else -> 0f
                        }
                        val latDeg = pickValueOrNull(
                            telemetry,
                            listOf("navigation.position.latitude", "position.latitude", "latitude", "lat")
                        )
                        val lonDeg = pickValueOrNull(
                            telemetry,
                            listOf("navigation.position.longitude", "position.longitude", "longitude", "lon", "lng")
                        )
                        val sogKn = pickValueOrNull(
                            telemetry,
                            listOf("navigation.speed_over_ground", "sog", "speed_over_ground")
                        )
                        val cogDeg = pickValueOrNull(
                            telemetry,
                            listOf("navigation.course_over_ground_true", "cog", "course_over_ground")
                        )
                        val modeCode = telemetry.entries
                            .firstOrNull { it.key.contains("autopilot_mode", ignoreCase = true) }
                            ?.value
                            ?.roundToInt()
                        val mode = when (modeCode) {
                            0 -> AutopilotControlMode.STBY
                            2 -> AutopilotControlMode.WIND
                            else -> AutopilotControlMode.KURS
                        }
                        AutopilotWidget(
                            heading = heading,
                            mode = mode,
                            targetHeading = autopilotTargetByWidget[widget.id],
                            rudderAngleDeg = rudderAngle,
                            averageRudderAngleDeg = avgRudderAngle,
                            onModeSelect = { requestedMode ->
                                val command = buildAutopilotProtocolCommand(
                                    targetDevice = autopilotSettings.targetDevice,
                                    mode = requestedMode,
                                    sourceHeadingDeg = heading,
                                    targetHeadingDeg = autopilotTargetByWidget[widget.id],
                                    tackingAngleDeg = sharedTackingAngleDeg,
                                    windAngleRelativeDeg = selectedWindAngleForTack
                                )
                                val request = AutopilotDispatchRequest(
                                    command = command,
                                    backend = autopilotSettings.gatewayBackend,
                                    host = autopilotSettings.gatewayHost,
                                    port = autopilotSettings.gatewayPort,
                                    signalKAutopilotId = autopilotSettings.signalKAutopilotId,
                                    latDeg = latDeg,
                                    lonDeg = lonDeg,
                                    sogKn = sogKn,
                                    cogDeg = cogDeg,
                                    timeoutMs = autopilotSettings.commandTimeoutMs.coerceIn(1_000L, 30_000L),
                                    requiresConfirmation = true,
                                    safetyGateArmed = autopilotSettings.safetyGateArmed,
                                    authToken = autopilotSettings.authToken.trim().takeIf { it.isNotBlank() },
                                )
                                if (!autopilotSettings.safetyGateArmed) {
                                    onShowSeaChartNotice(
                                        "Autopilot Safety Gate",
                                        "Autopilot-Befehle sind gesperrt. Öffne die Autopilot-Einstellungen, prüfe Gateway/Host und armiere das Safety Gate bewusst.",
                                    )
                                } else {
                                    pendingAutopilotDispatch = request
                                }
                            },
                            onAdjustTarget = { delta ->
                                val currentTarget = autopilotTargetByWidget[widget.id] ?: heading
                                autopilotTargetByWidget[widget.id] = wrap360Ui(currentTarget + delta)
                            },
                            onTack = {
                                if (!selectedWindAngleForTack.isFinite()) return@AutopilotWidget
                                val windRelative = normalizeSignedAngleUi(selectedWindAngleForTack)
                                val halfTack = (sharedTackingAngleDeg / 2f).coerceIn(25f, 70f)
                                val windNorth = wrap360Ui(heading + windRelative)
                                val desiredRelative = if (windRelative >= 0f) -halfTack else halfTack
                                autopilotTargetByWidget[widget.id] = wrap360Ui(windNorth - desiredRelative)
                            }
                        )
                    }
                    WidgetKind.SYSTEM_PERFORMANCE -> {
                        SystemWidget(
                            snapshot = systemPerformanceSnapshot,
                        )
                    }
                    WidgetKind.ENGINE_RPM -> {
                        val value = pickValue(telemetry, widget.dataKeys)
                        EngineRpmWidget(rpm = value)
                    }
                }
            }
        }

        if (showAddWidgetFromSurface) {
            CompactMenuDialog(
                onDismissRequest = { showAddWidgetFromSurface = false },
                isDarkMenu = darkBackground,
                // Keinen Titel-Text für Widget-Dialoge anzeigen.
                title = {},
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                ProvideTextStyle(value = widgetMenuTextStyle) {
                    val sections = widgetCatalogSections()
                    Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                            val tryAddWidgetFromSurface: (WidgetKind) -> Unit = { kind ->
                                val added = onAddWidget(kind, pageWidthPx, pageHeightPx, gridStepPx)
                                if (!added) onAddWidgetError("Nicht genug Platz für ein neues Widget.")
                            }
                            sections.forEachIndexed { index, section ->
                                Text(section.title, style = widgetMenuTextStyle, fontWeight = FontWeight.SemiBold)
                                section.entries.forEach { entry ->
                                    CompactMenuTextButton(
                                        text = entry.menuLabel,
                                        style = widgetMenuTextStyle,
                                        onClick = {
                                            tryAddWidgetFromSurface(entry.kind)
                                            showAddWidgetFromSurface = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (index < sections.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                            }
                        }
                    }
                },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Schließen",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = { showAddWidgetFromSurface = false }
                    )
                },
            )
        }

        val menuWidgetId = widgetMenuWidgetId
        if (menuWidgetId != null) {
            val menuWidget = page.widgets.firstOrNull { it.id == menuWidgetId }
            val anchorWatchSourceLabel = menuWidget?.takeIf { it.kind == WidgetKind.ANCHOR_WATCH }?.let { widget ->
                anchorWatchRuntimeByWidget[widget.id]?.lastAnchorChainSource
            }
        val chainCalibrationMenuWidgetId = showAnchorWatchChainCalibrationDialog
        if (chainCalibrationMenuWidgetId != null) {
            val targetWidget = page.widgets.firstOrNull {
                it.id == chainCalibrationMenuWidgetId && it.kind == WidgetKind.ANCHOR_WATCH
            }
            if (targetWidget != null) {
                CompactMenuDialog(
                    onDismissRequest = { showAnchorWatchChainCalibrationDialog = null },
                    isDarkMenu = darkBackground,
                    title = {},
                    text = {
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            ProvideTextStyle(value = widgetMenuTextStyle) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Text(
                                        "Dafür muss ein NMEA2000 Kettensensor angebracht sein.",
                                        style = widgetMenuTextStyle,
                                    )
                                    Text(
                                        "Bitte fahren Sie die gesamte Kettenlänge vollständig aus. "
                                            + "Die App merkt sich dabei die jeweils längste gemessene Kettenlänge als Kalibrierwert.",
                                        style = widgetMenuTextStyle,
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        CompactMenuTextButton(
                            text = "Kalibrierung aktivieren",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = {
                                selectedAnchorWatchChainLengthCalibrationEnabled = true
                                showAnchorWatchChainCalibrationDialog = null
                            },
                        )
                    },
                    dismissButton = {
                        CompactMenuTextButton(
                            text = "Abbrechen",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = { showAnchorWatchChainCalibrationDialog = null },
                        )
                    },
                )
            } else {
                showAnchorWatchChainCalibrationDialog = null
            }
        }
        if (menuWidget != null) {
            CompactMenuDialog(
                onDismissRequest = {
                    showWindHistoryWindowMenu = false
                    showAnchorWatchChainStrengthMenu = false
                    showAnchorWatchChainCalibrationDialog = null
                    widgetMenuWidgetId = null
                },
                isDarkMenu = darkBackground,
                // Keinen Titel für Widget-Dialoge anzeigen.
                title = {},
            text = {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    ProvideTextStyle(value = widgetMenuTextStyle) {
                        val anchorWatchDefaultDeviationPercent = DEFAULT_ANCHOR_WATCH_TOLERANCE_PERCENT
                        val menuAnchorWatchSettings = anchorWatchSettingsByWidget[menuWidget.id]
                        val menuAnchorWatchChainLengthMeters = menuAnchorWatchSettings?.calibratedChainLengthMeters
                            ?: selectedAnchorWatchCalibratedChainLengthMeters
                        val menuChainLengthEnabled = menuAnchorWatchSettings?.chainLengthCalibrationEnabled
                            ?: selectedAnchorWatchChainLengthCalibrationEnabled
                        val menuAnchorWatchChainSensorEnabled = menuAnchorWatchSettings?.chainSensorEnabled
                            ?: selectedAnchorWatchChainSensorEnabled
                        val anchorWatchChainLengthText = if (menuAnchorWatchChainSensorEnabled && menuChainLengthEnabled && menuAnchorWatchChainLengthMeters > 0f) {
                            "${(menuAnchorWatchChainLengthMeters * 10f).roundToInt() / 10f} m"
                        } else {
                            "${(DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS * 10f).roundToInt() / 10f} m"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Name", style = widgetMenuTextStyle)
                                    MenuCompactTextField(
                                        value = widgetRenameText,
                                        onValueChange = { widgetRenameText = it.take(16) },
                                        placeholder = {
                                            Text(menuWidget.title, style = widgetInputTextStyle)
                                        },
                                        singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(0.dp)
                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                        textStyle = widgetInputFieldTextStyle,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                        colors = menuOutlinedTextFieldColors(darkBackground),
                                        shape = MENU_INPUT_FIELD_SHAPE,
                                    )
                                }
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hilfe", style = widgetMenuTextStyle)
                                    CompactMenuTextButton(
                                        text = "Öffnen",
                                        style = widgetLinkTextStyle,
                                        onClick = {
                                            widgetHelpWidgetId = menuWidgetId
                                            widgetMenuWidgetId = null
                                        }
                                    )
                                }
                                if (menuWidget.kind == WidgetKind.WIND) {
                                    val windHistoryWindowDisplay = windHistoryWindowOptions.firstOrNull { it.first == selectedWindHistoryWindowMinutes }
                                        ?.second
                                        ?: "${selectedWindHistoryWindowMinutes} Min"
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Min/Max Messzeitraum", style = widgetMenuTextStyle)
                                        Box {
                                            CompactMenuTextButton(
                                                text = "$windHistoryWindowDisplay ▾",
                                                style = widgetMenuTextStyle,
                                                onClick = { showWindHistoryWindowMenu = true },
                                                fillWidth = false
                                            )
                                            DropdownMenu(
                                                modifier = Modifier.background(if (darkBackground) Color.Black else Color.White),
                                                expanded = showWindHistoryWindowMenu,
                                                onDismissRequest = { showWindHistoryWindowMenu = false }
                                            ) {
                                                windHistoryWindowOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.second, style = widgetMenuTextStyle) },
                                                        onClick = {
                                                            selectedWindHistoryWindowMinutes = option.first
                                                            showWindHistoryWindowMenu = false
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Min/Max basiert auf", style = widgetMenuTextStyle)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING / 2f),
                                                modifier = Modifier.padding(end = MENU_SPACING)
                                            ) {
                                                RadioButton(
                                                    selected = selectedWindMinMaxUsesTrueWind,
                                                    onClick = { selectedWindMinMaxUsesTrueWind = true },
                                                    colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                                )
                                                Text("wahrer Wind", style = widgetMenuTextStyle)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING / 2f)
                                            ) {
                                                RadioButton(
                                                    selected = !selectedWindMinMaxUsesTrueWind,
                                                    onClick = { selectedWindMinMaxUsesTrueWind = false },
                                                    colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                                )
                                                Text("scheinbarer Wind", style = widgetMenuTextStyle)
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Windrichtung zum Boot")
                                        Switch(
                                            checked = selectedWindShowBoatDirection,
                                            onCheckedChange = { selectedWindShowBoatDirection = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Windrichtung zu Nord")
                                        Switch(
                                            checked = selectedWindShowNorthDirection,
                                            onCheckedChange = { selectedWindShowNorthDirection = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Windgeschwindigkeit")
                                        Switch(
                                            checked = selectedWindShowWindSpeed,
                                            onCheckedChange = { selectedWindShowWindSpeed = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Einheiten", style = widgetMenuTextStyle)
                                        OptionSelectorRow(
                                            selectedOption = selectedWindSpeedUnit,
                                            options = WindSpeedUnit.entries,
                                            optionLabel = { it.label },
                                            onOptionSelected = { selectedWindSpeedUnit = it },
                                            style = widgetMenuTextStyle,
                                            darkBackground = darkBackground,
                                        )
                                    }
                                    Spacer(Modifier.height(MENU_SPACING))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Wendewinkel", style = widgetMenuTextStyle)
                                        Slider(
                                            modifier = Modifier.weight(1f),
                                            value = sharedTackingAngleDeg.toFloat(),
                                            onValueChange = { value ->
                                                setSharedTackingAngleDeg(value.roundToInt())
                                            },
                                            valueRange = 50f..140f,
                                            steps = 89
                                        )
                                        Text("${sharedTackingAngleDeg}°", style = widgetMenuTextStyle)
                                    }
                                }
                                if (menuWidget.kind == WidgetKind.TEMPERATURE) {
                                    HorizontalDivider()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                        Text("Einheit", style = widgetMenuTextStyle)
                                        OptionSelectorRow(
                                            selectedOption = selectedTemperatureUnit,
                                            options = TemperatureUnit.entries,
                                            optionLabel = { it.label },
                                            onOptionSelected = { selectedTemperatureUnit = it },
                                            style = widgetMenuTextStyle,
                                            darkBackground = darkBackground,
                                        )
                                    }

                                    Spacer(Modifier.height(MENU_SPACING))
                                    Text("Sensornamen", style = widgetMenuTextStyle)
                                    val temperatureScrollState = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 260.dp)
                                            .verticalScroll(temperatureScrollState),
                                        verticalArrangement = Arrangement.spacedBy(MENU_SPACING)
                                    ) {
                                        repeat(TEMPERATURE_SENSOR_COUNT) { index ->
                                            val label = "Sensor ${index + 1}"
                                            val value = selectedTemperatureSensorNames.getOrElse(index) {
                                                "Temperatursensor ${index + 1}"
                                            }
                                            Text(label, style = widgetMenuTextStyle)
                                            MenuCompactTextField(
                                            value = value,
                                            onValueChange = { newValue ->
                                                val updated = selectedTemperatureSensorNames.toMutableList()
                                                if (index < updated.size) {
                                                    updated[index] = newValue
                                                    } else {
                                                        while (updated.size <= index) {
                                                            updated.add("Temperatursensor ${updated.size + 1}")
                                                        }
                                                        updated[index] = newValue
                                                    }
                                                    selectedTemperatureSensorNames = normalizeTemperatureSensorNames(updated)
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(0.dp)
                                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                            textStyle = widgetInputFieldTextStyle,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                            shape = MENU_INPUT_FIELD_SHAPE
                                        )
                                        }
                                    }
                                }
                                if (menuWidget.kind == WidgetKind.AIS) {
                                    val sliderColors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF2A6BFF),
                                        inactiveTrackColor = Color(0x4D2A6BFF),
                                        thumbColor = Color(0xFF2A6BFF),
                                        activeTickColor = Color(0xFF2A6BFF),
                                        inactiveTickColor = Color(0x4D2A6BFF),
                                    )
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CPA Abstand", style = widgetMenuTextStyle)
                                        Text(
                                            "${(selectedAisAlarmDistanceNm * 10f).roundToInt() / 10f} NM",
                                            style = widgetMenuTextStyle,
                                        )
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = sliderColors,
                                        value = selectedAisAlarmDistanceNm,
                                        onValueChange = { selectedAisAlarmDistanceNm = it.coerceIn(0.1f, 20f) },
                                        valueRange = 0.1f..20f
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CPA-Zeit", style = widgetMenuTextStyle)
                                        Text("${selectedAisAlarmMinutes.roundToInt()} min", style = widgetMenuTextStyle)
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = sliderColors,
                                        value = selectedAisAlarmMinutes,
                                        onValueChange = { selectedAisAlarmMinutes = it.coerceIn(0.5f, 60f) },
                                        valueRange = 0.5f..60f
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sichtbarkeit", style = widgetMenuTextStyle)
                                        Text("${selectedAisTargetVisibilityMinutes} min", style = widgetMenuTextStyle)
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = sliderColors,
                                        value = selectedAisTargetVisibilityMinutes.toFloat(),
                                        onValueChange = { selectedAisTargetVisibilityMinutes = it.roundToInt().coerceIn(1, 5) },
                                        valueRange = 1f..5f,
                                        steps = 4,
                                    )
                                    val aisVisibilityRows = cachedAisTargets.mapNotNull {
                                        resolveAisMmsiText(it.target)
                                    }.distinct().sorted()
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sichtbarkeit je MMSI", style = widgetMenuTextStyle)
                                    }
                                    if (aisVisibilityRows.isEmpty()) {
                                        Text(
                                            "Noch keine MMSI gefunden",
                                            style = widgetMenuTextStyle,
                                        )
                                    } else {
                                        val aisVisibilityRowsScroll = rememberScrollState()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 180.dp)
                                                .verticalScroll(aisVisibilityRowsScroll),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            aisVisibilityRows.forEach { mmsi ->
                                                val timeoutMinutes = selectedAisTargetVisibilityByMmsi[mmsi]
                                                    ?: selectedAisTargetVisibilityMinutes
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "MMSI $mmsi",
                                                        style = widgetMenuTextStyle,
                                                    )
                                                    Text(
                                                        "${timeoutMinutes} min",
                                                        style = widgetMenuTextStyle,
                                                    )
                                                }
                                                Slider(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = sliderColors,
                                                    value = timeoutMinutes.toFloat(),
                                                    onValueChange = { timeout ->
                                                        selectedAisTargetVisibilityByMmsi = selectedAisTargetVisibilityByMmsi.toMutableMap().apply {
                                                            put(mmsi, timeout.roundToInt().coerceIn(1, 5))
                                                        }
                                                    },
                                                    valueRange = 1f..5f,
                                                    steps = 4,
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Ausrichtung", style = widgetMenuTextStyle)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = selectedAisOrientationNorthUp,
                                                    onClick = { selectedAisOrientationNorthUp = true },
                                                    colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                                )
                                                Text("Nord oben", style = widgetMenuTextStyle)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = !selectedAisOrientationNorthUp,
                                                    onClick = { selectedAisOrientationNorthUp = false },
                                                    colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                                )
                                                Text("Schiffausrichtung oben", style = widgetMenuTextStyle)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Schriftgröße", style = widgetMenuTextStyle)
                                        Text("${selectedAisFontSizeOffsetSp}sp", style = widgetMenuTextStyle)
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = sliderColors,
                                        value = selectedAisFontSizeOffsetSp.toFloat(),
                                        onValueChange = { selectedAisFontSizeOffsetSp = it.roundToInt().coerceIn(-4, 2) },
                                        valueRange = -4f..2f,
                                        steps = 5,
                                    )
                                }
                                if (menuWidget.kind == WidgetKind.ANCHOR_WATCH) {
                                    Text(
                                        "Hinweis: Bei aktivem Alarm wird der Ton alle ${alarmRepeatIntervalSeconds.coerceIn(2, 10)} Sekunden wiederholt.",
                                        style = widgetMenuTextStyle,
                                    )
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Ankerkettensensor verwenden", style = widgetMenuTextStyle)
                                        Switch(
                                            checked = selectedAnchorWatchChainSensorEnabled,
                                            onCheckedChange = { selectedAnchorWatchChainSensorEnabled = it },
                                            modifier = Modifier.scale(0.5f),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Quelle", style = widgetMenuTextStyle)
                                            Text(
                                                if (selectedAnchorWatchChainSensorEnabled) {
                                                    anchorWatchSourceLabel ?: "—"
                                                } else {
                                                    "deaktiviert"
                                                },
                                                style = widgetMenuTextStyle,
                                            )
                                        }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Kettenlänge", style = widgetMenuTextStyle)
                                        Spacer(modifier = Modifier.width(MENU_SPACING))
                                        Text(
                                            anchorWatchChainLengthText,
                                            style = widgetMenuTextStyle,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = MENU_SPACING),
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING / 2f)
                                        ) {
                                        CompactMenuTextButton(
                                            text = "Reset Kettenlänge",
                                                style = widgetLinkTextStyle,
                                                enabled = selectedAnchorWatchChainSensorEnabled,
                                                fillWidth = false,
                                                onClick = {
                                                    selectedAnchorWatchChainLengthCalibrationEnabled = true
                                                    selectedAnchorWatchCalibratedChainLengthMeters = 0f
                                                    val reset = anchorWatchSettingsByWidget.getOrPut(menuWidget.id) {
                                                        AnchorWatchWidgetSettings()
                                                    }.copy(
                                                        chainLengthCalibrationEnabled = true,
                                                        calibratedChainLengthMeters = 0f,
                                                    )
                                                    anchorWatchSettingsByWidget[menuWidget.id] = reset
                                                    onUpdateAnchorWatchWidgetSettings(
                                                        menuWidget.id,
                                                        serializeAnchorWatchWidgetSettings(reset),
                                                    )
                                                    showAnchorWatchChainCalibrationDialog = menuWidget.id
                                                },
                                            )
                                            CompactMenuTextButton(
                                                text = "Reset Kettenlänge auf 0",
                                                style = widgetLinkTextStyle,
                                                enabled = selectedAnchorWatchChainSensorEnabled,
                                                fillWidth = false,
                                                onClick = {
                                                    val resetRuntime = anchorWatchRuntimeByWidget[menuWidget.id]
                                                    anchorWatchRuntimeByWidget[menuWidget.id] = (resetRuntime ?: AnchorWatchRuntimeState()).copy(
                                                        chainLengthResetOffsetMeters = resetRuntime?.lastKnownChainLengthMeters ?: 0f
                                                    )
                                                },
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Maßeinheit", style = widgetMenuTextStyle)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING / 2f)
                                        ) {
                                            RadioButton(
                                                enabled = selectedAnchorWatchChainSensorEnabled,
                                                selected = selectedAnchorWatchChainCalibrationUnit == AnchorWatchChainUnit.METRIC,
                                                onClick = {
                                                    selectedAnchorWatchChainCalibrationUnit = AnchorWatchChainUnit.METRIC
                                                    selectedAnchorWatchChainStrengthLabel = anchorWatchNormalizeChainStrengthLabel(
                                                        AnchorWatchChainUnit.METRIC,
                                                        selectedAnchorWatchChainStrengthLabel,
                                                    )
                                                },
                                                colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                            )
                                            Text(AnchorWatchChainUnit.METRIC.label, style = widgetMenuTextStyle)
                                            RadioButton(
                                                enabled = selectedAnchorWatchChainSensorEnabled,
                                                selected = selectedAnchorWatchChainCalibrationUnit == AnchorWatchChainUnit.IMPERIAL,
                                                onClick = {
                                                    selectedAnchorWatchChainCalibrationUnit = AnchorWatchChainUnit.IMPERIAL
                                                    selectedAnchorWatchChainStrengthLabel = anchorWatchNormalizeChainStrengthLabel(
                                                        AnchorWatchChainUnit.IMPERIAL,
                                                        selectedAnchorWatchChainStrengthLabel,
                                                    )
                                                },
                                                colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                            )
                                            Text(AnchorWatchChainUnit.IMPERIAL.label, style = widgetMenuTextStyle)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Kettenstärke", style = widgetMenuTextStyle)
                                            Box {
                                                CompactMenuTextButton(
                                                    text = "${selectedAnchorWatchChainStrengthLabel} ▾",
                                                    style = widgetMenuTextStyle,
                                                    enabled = selectedAnchorWatchChainSensorEnabled,
                                                    onClick = { if (selectedAnchorWatchChainSensorEnabled) showAnchorWatchChainStrengthMenu = true },
                                                    fillWidth = false
                                                )
                                            DropdownMenu(
                                                modifier = Modifier.background(if (darkBackground) Color.Black else Color.White),
                                                expanded = showAnchorWatchChainStrengthMenu,
                                                onDismissRequest = { showAnchorWatchChainStrengthMenu = false }
                                            ) {
                                                    anchorWatchChainStrengthOptions(selectedAnchorWatchChainCalibrationUnit).forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option.label, style = widgetMenuTextStyle) },
                                                            onClick = {
                                                                if (!selectedAnchorWatchChainSensorEnabled) {
                                                                    showAnchorWatchChainStrengthMenu = false
                                                                    return@DropdownMenuItem
                                                                }
                                                                selectedAnchorWatchChainStrengthLabel = option.label
                                                                showAnchorWatchChainStrengthMenu = false
                                                            }
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kettentaschen der Ankerwinde", style = widgetMenuTextStyle)
                                        MenuCompactTextField(
                                            value = selectedAnchorWatchChainPocketsText,
                                            onValueChange = { input ->
                                                selectedAnchorWatchChainPocketsText = input.filter { it.isDigit() }
                                            },
                                            enabled = selectedAnchorWatchChainSensorEnabled,
                                            singleLine = true,
                                            textStyle = widgetInputFieldTextStyle,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                            shape = MENU_INPUT_FIELD_SHAPE,
                                            modifier = Modifier
                                                .width(100.dp)
                                                .padding(0.dp)
                                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                        )
                                    }
                                    HorizontalDivider()
                                    val anchorWatchDeviationRangeMax = 30f
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Toleranz", style = widgetMenuTextStyle)
                                        Text("${selectedAnchorWatchDeviation.roundToInt()} %")
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = selectedAnchorWatchDeviation,
                                        onValueChange = { selectedAnchorWatchDeviation = it.coerceIn(0f, anchorWatchDeviationRangeMax) },
                                        valueRange = 0f..anchorWatchDeviationRangeMax
                                    )
                                    CompactMenuTextButton(
                                        text = "Reset auf Voreinstellung ${anchorWatchDefaultDeviationPercent.roundToInt()} %",
                                        style = widgetMenuTextStyle,
                                        fillWidth = false,
                                        onClick = {
                                            selectedAnchorWatchDeviation = anchorWatchDefaultDeviationPercent
                                        },
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Alarmton", style = widgetMenuTextStyle)
                                        CompactMenuTextButton(
                                            text = "Test",
                                            style = widgetMenuTextStyle,
                                            onClick = {
                                                runAlarmTestOnce {
                                                    val testFrequency = ECHO_SOUNDER_TONES
                                                        .getOrNull(selectedAnchorWatchAlarmTone)
                                                        ?.frequencyHz
                                                        ?: ECHO_SOUNDER_TONES.first().frequencyHz
                                                    applySystemAlarmPresetVolume(
                                                        context = context,
                                                        preset = alarmToneVolume,
                                                    )
                                                    playWidgetAlarmTone(
                                                        frequencyHz = testFrequency,
                                                        volume = alarmToneVolume.coerceIn(0f, 2f),
                                                    )
                                                }
                                            },
                                            fillWidth = false,
                                        )
                                    }
                                    AlarmToneSectionSelector(
                                        selectedToneIndex = selectedAnchorWatchAlarmTone,
                                        onToneSelected = { selectedAnchorWatchAlarmTone = it },
                                        labelStyle = widgetMenuTextStyle,
                                    )
                                }
                                if (menuWidget.kind == WidgetKind.ECHOSOUNDER) {
                                    Text(
                                        "Hinweis: Bei aktivem Alarm wird der Ton alle ${alarmRepeatIntervalSeconds.coerceIn(2, 10)} Sekunden wiederholt.",
                                        style = widgetMenuTextStyle,
                                    )
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Mindesttiefe")
                                        Text(
                                            if (selectedEchoDepthUnit == EchosounderDepthUnit.METERS) {
                                                "${selectedEchoMinDepth} m"
                                            } else {
                                                "${(selectedEchoMinDepth * 39.3701f)} inch"
                                            }
                                        )
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = selectedEchoMinDepth,
                                        onValueChange = {
                                            selectedEchoMinDepth = it.coerceIn(0.1f, 40f)
                                        },
                                        valueRange = 0.1f..40f,
                                        steps = 199
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tiefendynamik")
                                        Text("${selectedEchoDynamicRate.roundToInt()} m/s")
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = selectedEchoDynamicRate,
                                        onValueChange = {
                                            selectedEchoDynamicRate = it.coerceIn(0f, 5f)
                                        },
                                        valueRange = 0f..5f,
                                        steps = 100
                                    )
                                    Text("Tiefeneinheit", style = widgetMenuTextStyle)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)
                                        ) {
                                            RadioButton(
                                                selected = selectedEchoDepthUnit == EchosounderDepthUnit.METERS,
                                                onClick = { selectedEchoDepthUnit = EchosounderDepthUnit.METERS },
                                                colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                            )
                                            Text(EchosounderDepthUnit.METERS.label, style = widgetMenuTextStyle)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)
                                        ) {
                                            RadioButton(
                                                selected = selectedEchoDepthUnit == EchosounderDepthUnit.INCH,
                                                onClick = { selectedEchoDepthUnit = EchosounderDepthUnit.INCH },
                                                colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                            )
                                            Text(EchosounderDepthUnit.INCH.label, style = widgetMenuTextStyle)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Alarmton", style = widgetMenuTextStyle)
                                        CompactMenuTextButton(
                                            text = "Test",
                                            style = widgetMenuTextStyle,
                                            onClick = {
                                                runAlarmTestOnce {
                                                    val testFrequency = ECHO_SOUNDER_TONES
                                                        .getOrNull(selectedEchoAlarmTone)
                                                        ?.frequencyHz
                                                        ?: ECHO_SOUNDER_TONES.first().frequencyHz
                                                    applySystemAlarmPresetVolume(
                                                        context = context,
                                                        preset = alarmToneVolume,
                                                    )
                                                    playWidgetAlarmTone(
                                                        frequencyHz = testFrequency,
                                                        volume = alarmToneVolume.coerceIn(0f, 2f),
                                                    )
                                                }
                                            },
                                            fillWidth = false,
                                        )
                                    }
                                    Text(
                                        "Test spielt den aktuellen Alarmton einmalig ab.",
                                        style = widgetMenuTextStyle,
                                    )
                                    AlarmToneSectionSelector(
                                        selectedToneIndex = selectedEchoAlarmTone,
                                        onToneSelected = { selectedEchoAlarmTone = it },
                                        labelStyle = widgetMenuTextStyle,
                                    )
                                }
                                if (menuWidget.kind == WidgetKind.BATTERY) {
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Batterietyp Bleisäure", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedBatteryChemistry == BatteryChemistry.LEAD_ACID,
                                            onClick = { selectedBatteryChemistry = BatteryChemistry.LEAD_ACID },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Batterietyp Lithium", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedBatteryChemistry == BatteryChemistry.LITHIUM,
                                            onClick = { selectedBatteryChemistry = BatteryChemistry.LITHIUM },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                }
                                if (menuWidget.kind == WidgetKind.LOG) {
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Datengrundlage", style = widgetMenuTextStyle)
                                        OptionSelectorRow(
                                            selectedOption = selectedLogSpeedSource,
                                            options = LogWidgetSpeedSource.entries,
                                            optionLabel = {
                                                when (it) {
                                                    LogWidgetSpeedSource.GPS_SOG -> "GPS (SOG)"
                                                    LogWidgetSpeedSource.LOG_STW -> "Geber (STW)"
                                                }
                                            },
                                            onOptionSelected = { selectedLogSpeedSource = it },
                                            style = widgetMenuTextStyle,
                                            darkBackground = darkBackground,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Speed-Zeitraum", style = widgetMenuTextStyle)
                                        Text("${selectedLogWindowMinutes} min", style = widgetMenuTextStyle)
                                    }
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = selectedLogWindowMinutes.toFloat(),
                                        onValueChange = { selectedLogWindowMinutes = it.roundToInt().coerceIn(1, 120) },
                                        valueRange = 1f..120f,
                                        steps = 119
                                    )
                                }
                                if (menuWidget.kind == WidgetKind.KARTEN) {
                                    HorizontalDivider()
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Karte", style = widgetMenuTextStyle)
                                        OptionSelectorRow(
                                            selectedOption = selectedSeaChartMapProvider,
                                            options = selectableSeaChartProviders(),
                                            optionLabel = ::seaChartProviderOptionLabel,
                                            onOptionSelected = { provider ->
                                                selectedSeaChartMapProvider = provider
                                            },
                                            style = widgetMenuTextStyle,
                                            darkBackground = darkBackground,
                                        )
                                    }
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Offlinekarten", style = widgetMenuTextStyle)
                                        CompactMenuTextButton(
                                            text = "${selectedSeaChartMapProvider.label} laden",
                                            style = widgetLinkTextStyle,
                                            fillWidth = false,
                                            onClick = {
                                                seaChartDownloadCatalog = seaChartDownloadCatalogForProvider(selectedSeaChartMapProvider)
                                                seaChartDownloadUiState = SeaChartDownloadUiState()
                                                seaChartDownloadErrorDialog = null
                                                showSeaChartDownloadDialog = menuWidgetId
                                                widgetMenuWidgetId = null
                                            },
                                        )
                                    }
                                }
                                if (isLegacySeaChartWidgetKind(menuWidget.kind)) {
                                    HorizontalDivider()
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Karte", style = widgetMenuTextStyle)
                                        OptionSelectorRow(
                                            selectedOption = selectedSeaChartMapProvider,
                                            options = selectableSeaChartProviders(),
                                            optionLabel = ::seaChartProviderOptionLabel,
                                            onOptionSelected = { provider ->
                                                selectedSeaChartMapProvider = provider
                                            },
                                            style = widgetMenuTextStyle,
                                            darkBackground = darkBackground,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Dokumentation", style = widgetMenuTextStyle)
                                        CompactMenuTextButton(
                                            text = "Link öffnen",
                                            style = widgetLinkTextStyle,
                                            fillWidth = false,
                                            onClick = {
                                                context.startActivity(
                                                    android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(
                                                            seaChartDocumentationUrl(selectedSeaChartMapProvider)
                                                        )
                                                    )
                                                )
                                            },
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Karten Download", style = widgetMenuTextStyle)
                                        CompactMenuTextButton(
                                            text = "Popup öffnen",
                                            style = widgetLinkTextStyle,
                                            fillWidth = false,
                                            onClick = {
                                                seaChartDownloadCatalog = seaChartDownloadCatalogForProvider(selectedSeaChartMapProvider)
                                                seaChartDownloadUiState = SeaChartDownloadUiState()
                                                seaChartDownloadErrorDialog = null
                                                showSeaChartDownloadDialog = menuWidgetId
                                                widgetMenuWidgetId = null
                                            },
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Kartendetails", style = widgetMenuTextStyle)
                                        CompactMenuTextButton(
                                            text = "Popup öffnen",
                                            style = widgetLinkTextStyle,
                                            fillWidth = false,
                                            onClick = {
                                                val chartSettings = seaChartSettingsByWidget.getOrPut(menuWidgetId) {
                                                    SeaChartWidgetSettings(
                                                        mapProvider = selectedSeaChartMapProvider,
                                                        speedSource = selectedSeaChartSpeedSource,
                                                        showAisOverlay = selectedSeaChartShowAisOverlay,
                                                        showGribOverlay = selectedSeaChartShowGribOverlay,
                                                        showOpenSeaMapOverlay = selectedSeaChartShowOpenSeaMapOverlay,
                                                        showHeadingLine = selectedSeaChartShowHeadingLine,
                                                        headingLineLengthNm = selectedSeaChartHeadingLineLengthNm.coerceIn(0.1f, 20f),
                                                        showCogVector = selectedSeaChartShowCogVector,
                                                        cogVectorMinutes = selectedSeaChartCogVectorMinutes.coerceIn(1, 60),
                                                        showPredictor = selectedSeaChartShowPredictor,
                                                        predictorMinutes = selectedSeaChartPredictorMinutes.coerceIn(1, 60),
                                                        predictorIntervalMinutes = selectedSeaChartPredictorIntervalMinutes.coerceIn(
                                                            1,
                                                            selectedSeaChartPredictorMinutes.coerceIn(1, 60),
                                                        ),
                                                        showPredictorLabels = selectedSeaChartShowPredictorLabels,
                                                        showBoatIcon = selectedSeaChartShowBoatIcon,
                                                        boatIconSizeDp = selectedSeaChartBoatIconSizeDp.coerceIn(12, 40),
                                                        showCourseLine = selectedSeaChartShowCourseLine,
                                                        showRoadLayers = selectedSeaChartShowRoadLayers,
                                                        showDepthLines = selectedSeaChartShowDepthLines,
                                                        showContourLines = selectedSeaChartShowContourLines,
                                                        showTrack = selectedSeaChartShowTrack,
                                                        trackDurationMinutes = selectedSeaChartTrackDurationMinutes.coerceIn(1, 24 * 60),
                                                        trackRecordIntervalSeconds = selectedSeaChartTrackRecordIntervalSeconds.coerceIn(1, 60),
                                                        guardZoneEnabled = selectedSeaChartGuardZoneEnabled,
                                                        guardZoneInnerNm = selectedSeaChartGuardZoneInnerNm.coerceIn(0f, 10f),
                                                        guardZoneOuterNm = selectedSeaChartGuardZoneOuterNm.coerceIn(
                                                            selectedSeaChartGuardZoneInnerNm.coerceIn(0f, 10f),
                                                            20f,
                                                        ),
                                                        guardZoneSectorStartDeg = wrap360Ui(selectedSeaChartGuardZoneSectorStartDeg),
                                                        guardZoneSectorEndDeg = selectedSeaChartGuardZoneSectorEndDeg.coerceIn(0f, 360f),
                                                        showLaylines = selectedSeaChartShowLaylines,
                                                        tackingAngleDeg = selectedSeaChartTackingAngleDeg.coerceIn(20, 80),
                                                        laylineLengthNm = selectedSeaChartLaylineLengthNm.coerceIn(0.1f, 20f),
                                                        showSafetyContour = selectedSeaChartShowSafetyContour,
                                                        safetyDepthMeters = selectedSeaChartSafetyDepthMeters.coerceIn(0.5f, 50f),
                                                        courseLineBearingDeg = selectedSeaChartCourseLineBearingDeg,
                                                        courseLineDistanceNm = selectedSeaChartCourseLineDistanceNm,
                                                    )
                                                }
                                                seaChartLayerFilterRoadLayers = chartSettings.showRoadLayers
                                                seaChartLayerFilterDepthLines = chartSettings.showDepthLines
                                                seaChartLayerFilterContourLines = chartSettings.showContourLines
                                                seaChartDownloadErrorDialog = null
                                                showSeaChartLayerFilterDialog = menuWidgetId
                                                widgetMenuWidgetId = null
                                            },
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Datengrundlage", style = widgetMenuTextStyle)
                                        OptionSelectorRow(
                                            selectedOption = selectedSeaChartSpeedSource,
                                            options = SeaChartSpeedSource.entries,
                                            optionLabel = { it.label },
                                            onOptionSelected = { selectedSeaChartSpeedSource = it },
                                            style = widgetMenuTextStyle,
                                            darkBackground = darkBackground,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("AIS-Overlay")
                                        Switch(
                                            checked = selectedSeaChartShowAisOverlay,
                                            onCheckedChange = { selectedSeaChartShowAisOverlay = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("GRIB-Overlay")
                                        Switch(
                                            checked = selectedSeaChartShowGribOverlay,
                                            onCheckedChange = { selectedSeaChartShowGribOverlay = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("OpenSeaMap Overlay")
                                        Switch(
                                            checked = selectedSeaChartShowOpenSeaMapOverlay,
                                            onCheckedChange = { selectedSeaChartShowOpenSeaMapOverlay = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Heading-Linie")
                                        Switch(
                                            checked = selectedSeaChartShowHeadingLine,
                                            onCheckedChange = { selectedSeaChartShowHeadingLine = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowHeadingLine) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Heading-Länge", style = widgetMenuTextStyle)
                                            Text(String.format(Locale.ROOT, "%.1f NM", selectedSeaChartHeadingLineLengthNm))
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartHeadingLineLengthNm,
                                            onValueChange = {
                                                selectedSeaChartHeadingLineLengthNm = it.coerceIn(0.1f, 20f)
                                            },
                                            valueRange = 0.1f..20f,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("COG-Vektor")
                                        Switch(
                                            checked = selectedSeaChartShowCogVector,
                                            onCheckedChange = { selectedSeaChartShowCogVector = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowCogVector) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("COG-Zeitraum", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartCogVectorMinutes} min")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartCogVectorMinutes.toFloat(),
                                            onValueChange = {
                                                selectedSeaChartCogVectorMinutes = it.roundToInt().coerceIn(1, 60)
                                            },
                                            valueRange = 1f..60f,
                                            steps = 58,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Prädiktor")
                                        Switch(
                                            checked = selectedSeaChartShowPredictor,
                                            onCheckedChange = { selectedSeaChartShowPredictor = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowPredictor) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Prädiktor-Zeitraum", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartPredictorMinutes} min")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartPredictorMinutes.toFloat(),
                                            onValueChange = {
                                                val minutes = it.roundToInt().coerceIn(1, 60)
                                                selectedSeaChartPredictorMinutes = minutes
                                                selectedSeaChartPredictorIntervalMinutes =
                                                    selectedSeaChartPredictorIntervalMinutes.coerceIn(1, minutes)
                                            },
                                            valueRange = 1f..60f,
                                            steps = 58,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Markenabstand", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartPredictorIntervalMinutes} min")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartPredictorIntervalMinutes.toFloat(),
                                            onValueChange = {
                                                selectedSeaChartPredictorIntervalMinutes = it.roundToInt().coerceIn(
                                                    1,
                                                    selectedSeaChartPredictorMinutes.coerceIn(1, 60),
                                                )
                                            },
                                            valueRange = 1f..selectedSeaChartPredictorMinutes.coerceAtLeast(1).toFloat(),
                                            steps = (selectedSeaChartPredictorMinutes - 2).coerceAtLeast(0),
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Prädiktor-Labels")
                                            Switch(
                                                checked = selectedSeaChartShowPredictorLabels,
                                                onCheckedChange = { selectedSeaChartShowPredictorLabels = it },
                                                modifier = Modifier.scale(0.5f)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Bootssymbol")
                                        Switch(
                                            checked = selectedSeaChartShowBoatIcon,
                                            onCheckedChange = { selectedSeaChartShowBoatIcon = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowBoatIcon) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Bootssymbol-Größe", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartBoatIconSizeDp} dp")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartBoatIconSizeDp.toFloat(),
                                            onValueChange = {
                                                selectedSeaChartBoatIconSizeDp = it.roundToInt().coerceIn(12, 40)
                                            },
                                            valueRange = 12f..40f,
                                            steps = 27,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Track")
                                        Switch(
                                            checked = selectedSeaChartShowTrack,
                                            onCheckedChange = { selectedSeaChartShowTrack = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowTrack) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Track-Dauer", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartTrackDurationMinutes} min")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartTrackDurationMinutes.toFloat(),
                                            onValueChange = {
                                                selectedSeaChartTrackDurationMinutes = it.roundToInt().coerceIn(1, 24 * 60)
                                            },
                                            valueRange = 1f..240f,
                                            steps = 239,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Track-Intervall", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartTrackRecordIntervalSeconds} s")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartTrackRecordIntervalSeconds.toFloat(),
                                            onValueChange = {
                                                selectedSeaChartTrackRecordIntervalSeconds = it.roundToInt().coerceIn(1, 60)
                                            },
                                            valueRange = 1f..60f,
                                            steps = 58,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Guard Zone")
                                        Switch(
                                            checked = selectedSeaChartGuardZoneEnabled,
                                            onCheckedChange = { selectedSeaChartGuardZoneEnabled = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartGuardZoneEnabled) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Innerer Radius", style = widgetMenuTextStyle)
                                            Text(String.format(Locale.ROOT, "%.1f NM", selectedSeaChartGuardZoneInnerNm))
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartGuardZoneInnerNm,
                                            onValueChange = { value ->
                                                selectedSeaChartGuardZoneInnerNm = value.coerceIn(0f, 10f)
                                                selectedSeaChartGuardZoneOuterNm =
                                                    selectedSeaChartGuardZoneOuterNm.coerceAtLeast(selectedSeaChartGuardZoneInnerNm)
                                            },
                                            valueRange = 0f..10f,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Äußerer Radius", style = widgetMenuTextStyle)
                                            Text(String.format(Locale.ROOT, "%.1f NM", selectedSeaChartGuardZoneOuterNm))
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartGuardZoneOuterNm,
                                            onValueChange = { value ->
                                                selectedSeaChartGuardZoneOuterNm = value.coerceIn(
                                                    selectedSeaChartGuardZoneInnerNm.coerceIn(0f, 10f),
                                                    20f,
                                                )
                                            },
                                            valueRange = selectedSeaChartGuardZoneInnerNm.coerceIn(0f, 10f)..20f,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Sektor Start", style = widgetMenuTextStyle)
                                            Text("${wrap360Ui(selectedSeaChartGuardZoneSectorStartDeg).roundToInt()}°")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartGuardZoneSectorStartDeg,
                                            onValueChange = { value ->
                                                selectedSeaChartGuardZoneSectorStartDeg = value.coerceIn(0f, 359.9f)
                                            },
                                            valueRange = 0f..359.9f,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Sektor Ende", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartGuardZoneSectorEndDeg.roundToInt()}°")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartGuardZoneSectorEndDeg,
                                            onValueChange = { value ->
                                                selectedSeaChartGuardZoneSectorEndDeg = value.coerceIn(0f, 360f)
                                            },
                                            valueRange = 0f..360f,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Laylines")
                                        Switch(
                                            checked = selectedSeaChartShowLaylines,
                                            onCheckedChange = { selectedSeaChartShowLaylines = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowLaylines) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Wendewinkel", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartTackingAngleDeg}°")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartTackingAngleDeg.toFloat(),
                                            onValueChange = { value ->
                                                selectedSeaChartTackingAngleDeg = value.roundToInt().coerceIn(20, 80)
                                            },
                                            valueRange = 20f..80f,
                                            steps = 59,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Layline-Länge", style = widgetMenuTextStyle)
                                            Text(String.format(Locale.ROOT, "%.1f NM", selectedSeaChartLaylineLengthNm))
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartLaylineLengthNm,
                                            onValueChange = { value ->
                                                selectedSeaChartLaylineLengthNm = value.coerceIn(0.1f, 20f)
                                            },
                                            valueRange = 0.1f..20f,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Safety Contour")
                                        Switch(
                                            checked = selectedSeaChartShowSafetyContour,
                                            onCheckedChange = { selectedSeaChartShowSafetyContour = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowSafetyContour) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Sicherheitstiefe", style = widgetMenuTextStyle)
                                            Text(String.format(Locale.ROOT, "%.1f m", selectedSeaChartSafetyDepthMeters))
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartSafetyDepthMeters,
                                            onValueChange = { value ->
                                                selectedSeaChartSafetyDepthMeters = value.coerceIn(0.5f, 50f)
                                            },
                                            valueRange = 0.5f..50f,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Kurslinie")
                                        Switch(
                                            checked = selectedSeaChartShowCourseLine,
                                            onCheckedChange = { selectedSeaChartShowCourseLine = it },
                                            modifier = Modifier.scale(0.5f)
                                        )
                                    }
                                    if (selectedSeaChartShowCourseLine) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Kurs (°) (zu rechtweisend Nord)", style = widgetMenuTextStyle)
                                            Text("${wrap360Ui(selectedSeaChartCourseLineBearingDeg).roundToInt()}°")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartCourseLineBearingDeg,
                                            onValueChange = { value ->
                                                selectedSeaChartCourseLineBearingDeg = value
                                            },
                                            valueRange = 0f..359.9f,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Distanz", style = widgetMenuTextStyle)
                                            Text("${selectedSeaChartCourseLineDistanceNm.roundToInt()} NM")
                                        }
                                        Slider(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = selectedSeaChartCourseLineDistanceNm,
                                            onValueChange = { value ->
                                                selectedSeaChartCourseLineDistanceNm = value.coerceIn(0.1f, 40f)
                                            },
                                            valueRange = 0.1f..40f,
                                            steps = 399
                                        )
                                    }
                                }
                                if (menuWidget.kind == WidgetKind.AUTOPILOT) {
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Autopilot-Einstellungen", style = widgetMenuTextStyle)
                                    CompactMenuTextButton(
                                        text = "Öffnen",
                                        style = widgetLinkTextStyle,
                                        onClick = {
                                            autopilotMenuWidgetId = menuWidgetId
                                            widgetMenuWidgetId = null
                                        }
                                    )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Wendewinkel ${sharedTackingAngleDeg}°", style = widgetMenuTextStyle)
                                        Slider(
                                            modifier = Modifier.weight(1f),
                                            value = sharedTackingAngleDeg.toFloat(),
                                            onValueChange = { value ->
                                                setSharedTackingAngleDeg(value.roundToInt())
                                            },
                                            valueRange = 50f..140f,
                                            steps = 89
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                    Text(
                                        "Ruder-Mittelwert ${selectedAutopilotRudderAverageSeconds}s",
                                        style = widgetMenuTextStyle
                                    )
                                        Slider(
                                            modifier = Modifier.weight(1f),
                                            value = selectedAutopilotRudderAverageSeconds.toFloat(),
                                            onValueChange = { value ->
                                                selectedAutopilotRudderAverageSeconds = value.roundToInt().coerceIn(5, 300)
                                            },
                                            valueRange = 5f..300f,
                                            steps = 294
                                        )
                                    }
                                }
                                HorizontalDivider()
                                CompactMenuTextButton(
                                    text = "Widget entfernen",
                                    style = widgetMenuTextStyle,
                                    fillWidth = true,
                                    onClick = {
                                        onRemove(menuWidgetId)
                                        widgetMenuWidgetId = null
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Speichern",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = {
                            if (menuWidget.kind == WidgetKind.BATTERY) {
                                val updated = BatteryWidgetSettings(
                                    chemistry = selectedBatteryChemistry
                                )
                                batterySettingsByWidget[menuWidgetId] = updated
                                onUpdateBatteryWidgetSettings(
                                    menuWidgetId,
                                    serializeBatteryWidgetSettings(updated),
                                )
                            }
                            if (menuWidget.kind == WidgetKind.AUTOPILOT) {
                                val currentAutopilotSettings = autopilotSettingsByWidget
                                    .getOrPut(menuWidgetId) { AutopilotWidgetSettings() }
                                val updatedAutopilotSettings = currentAutopilotSettings.copy(
                                    rudderAverageSeconds = selectedAutopilotRudderAverageSeconds
                                )
                                autopilotSettingsByWidget[menuWidgetId] = updatedAutopilotSettings
                                onUpdateAutopilotWidgetSettings(
                                    menuWidgetId,
                                    serializeAutopilotWidgetSettings(updatedAutopilotSettings),
                                )
                            }
                            if (menuWidget.kind == WidgetKind.WIND) {
                                val currentWindSettings = windSettingsByWidget
                                    .getOrPut(menuWidgetId) { WindWidgetSettings(tackingAngleDeg = sharedTackingAngleDeg) }
                                val previousMinMaxUsesTrueWind = currentWindSettings.minMaxUsesTrueWind
                                val updatedWindSettings = currentWindSettings.copy(
                                    showBoatDirection = selectedWindShowBoatDirection,
                                    showNorthDirection = selectedWindShowNorthDirection,
                                    showWindSpeed = selectedWindShowWindSpeed,
                                    speedUnit = selectedWindSpeedUnit,
                                    tackingAngleDeg = sharedTackingAngleDeg,
                                    historyWindowMinutes = selectedWindHistoryWindowMinutes,
                                    minMaxUsesTrueWind = selectedWindMinMaxUsesTrueWind,
                                )
                                windSettingsByWidget[menuWidgetId] = updatedWindSettings
                                if (updatedWindSettings.minMaxUsesTrueWind != previousMinMaxUsesTrueWind) {
                                    windSpeedHistoryByWidget[menuWidgetId]?.clear()
                                }
                                onUpdateWindWidgetSettings(
                                    menuWidgetId,
                                    serializeWindWidgetSettings(updatedWindSettings),
                                )
                            }
                            if (menuWidget.kind == WidgetKind.ANCHOR_WATCH) {
                                val current = anchorWatchSettingsByWidget.getOrPut(menuWidgetId) { AnchorWatchWidgetSettings() }
                                val selectedAnchorWatchTolerancePercent = selectedAnchorWatchDeviation.coerceIn(0f, 30f)
                                val normalizedCalibrationUnit = if (selectedAnchorWatchChainCalibrationUnit in AnchorWatchChainUnit.entries) {
                                    selectedAnchorWatchChainCalibrationUnit
                                } else {
                                    AnchorWatchChainUnit.METRIC
                                }
                                val normalizedStrengthLabel = anchorWatchNormalizeChainStrengthLabel(
                                    normalizedCalibrationUnit,
                                    selectedAnchorWatchChainStrengthLabel,
                                )
                                val chainSignalLengthMeters = anchorWatchChainSpacingMeters(
                                    normalizedCalibrationUnit,
                                    normalizedStrengthLabel,
                                    selectedAnchorWatchChainPocketsText,
                                )
                                val chainSignalLengthMm = (chainSignalLengthMeters * 1000f).coerceIn(0.1f, 5000f)
                                val chainPockets = anchorWatchChainPocketsFromText(selectedAnchorWatchChainPocketsText)
                                val toleranceSourceChainLengthMeters = selectedAnchorWatchCalibratedChainLengthMeters.takeIf {
                                    it > 0f
                                } ?: DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS
                                val toleranceInMeters = (
                                    toleranceSourceChainLengthMeters * selectedAnchorWatchTolerancePercent / 100f
                                ).coerceIn(0.5f, 50f)
                                val updated = current.copy(
                                    monitoringEnabled = selectedAnchorWatchEnabled,
                                    chainSensorEnabled = selectedAnchorWatchChainSensorEnabled,
                                    chainLengthCalibrationEnabled = selectedAnchorWatchChainLengthCalibrationEnabled,
                                    calibratedChainLengthMeters = selectedAnchorWatchCalibratedChainLengthMeters.coerceAtLeast(0f),
                                    maxDeviationMeters = toleranceInMeters,
                                    maxDeviationPercent = selectedAnchorWatchTolerancePercent,
                                    maxDeviationUnit = AnchorWatchDistanceUnit.METERS,
                                    chainCalibrationUnit = normalizedCalibrationUnit,
                                    chainStrengthLabel = normalizedStrengthLabel,
                                    chainPockets = chainPockets,
                                    chainSignalLength = chainSignalLengthMm,
                                    alarmToneIndex = selectedAnchorWatchAlarmTone.coerceIn(
                                        0,
                                        ECHO_SOUNDER_TONES.lastIndex,
                                    ),
                                )
                                anchorWatchSettingsByWidget[menuWidgetId] = updated
                                onUpdateAnchorWatchWidgetSettings(
                                    menuWidgetId,
                                    serializeAnchorWatchWidgetSettings(updated),
                                )
                            }
                            if (menuWidget.kind == WidgetKind.AIS) {
                                val updated = AisWidgetSettings(
                                    cpaAlarmDistanceNm = selectedAisAlarmDistanceNm.coerceIn(0.1f, 20f),
                                    cpaAlarmMinutes = selectedAisAlarmMinutes.coerceIn(0.5f, 60f),
                                    northUp = selectedAisOrientationNorthUp,
                                    fontSizeOffsetSp = selectedAisFontSizeOffsetSp.coerceIn(-4, 2),
                                    targetVisibilityMinutes = selectedAisTargetVisibilityMinutes.coerceIn(1, 5),
                                    targetVisibilityMinutesByMmsi = selectedAisTargetVisibilityByMmsi
                                        .filterValues { value -> value in 1..5 }
                                        .filterKeys { key -> key.isNotBlank() },
                                )
                                aisSettingsByWidget[menuWidgetId] = updated
                                onUpdateAisWidgetSettings(
                                    menuWidgetId,
                                    serializeAisWidgetSettings(updated),
                                )
                            }
                            if (isSeaChartWidgetKind(menuWidget.kind)) {
                                val normalizedMapProvider = normalizedSelectableSeaChartProvider(selectedSeaChartMapProvider)
                                val updated = SeaChartWidgetSettings(
                                    mapProvider = normalizedMapProvider,
                                    speedSource = selectedSeaChartSpeedSource,
                                    showAisOverlay = selectedSeaChartShowAisOverlay,
                                    showGribOverlay = selectedSeaChartShowGribOverlay,
                                    showOpenSeaMapOverlay = selectedSeaChartShowOpenSeaMapOverlay,
                                    showHeadingLine = selectedSeaChartShowHeadingLine,
                                    headingLineLengthNm = selectedSeaChartHeadingLineLengthNm.coerceIn(0.1f, 20f),
                                    showCogVector = selectedSeaChartShowCogVector,
                                    cogVectorMinutes = selectedSeaChartCogVectorMinutes.coerceIn(1, 60),
                                    showPredictor = selectedSeaChartShowPredictor,
                                    predictorMinutes = selectedSeaChartPredictorMinutes.coerceIn(1, 60),
                                    predictorIntervalMinutes = selectedSeaChartPredictorIntervalMinutes.coerceIn(
                                        1,
                                        selectedSeaChartPredictorMinutes.coerceIn(1, 60),
                                    ),
                                    showPredictorLabels = selectedSeaChartShowPredictorLabels,
                                    showBoatIcon = selectedSeaChartShowBoatIcon,
                                    boatIconSizeDp = selectedSeaChartBoatIconSizeDp.coerceIn(12, 40),
                                    showCourseLine = selectedSeaChartShowCourseLine,
                                    showRoadLayers = selectedSeaChartShowRoadLayers,
                                    showDepthLines = selectedSeaChartShowDepthLines,
                                    showContourLines = selectedSeaChartShowContourLines,
                                    showTrack = selectedSeaChartShowTrack,
                                    trackDurationMinutes = selectedSeaChartTrackDurationMinutes.coerceIn(1, 24 * 60),
                                    trackRecordIntervalSeconds = selectedSeaChartTrackRecordIntervalSeconds.coerceIn(1, 60),
                                    guardZoneEnabled = selectedSeaChartGuardZoneEnabled,
                                    guardZoneInnerNm = selectedSeaChartGuardZoneInnerNm.coerceIn(0f, 10f),
                                    guardZoneOuterNm = selectedSeaChartGuardZoneOuterNm.coerceIn(
                                        selectedSeaChartGuardZoneInnerNm.coerceIn(0f, 10f),
                                        20f,
                                    ),
                                    guardZoneSectorStartDeg = wrap360Ui(selectedSeaChartGuardZoneSectorStartDeg),
                                    guardZoneSectorEndDeg = selectedSeaChartGuardZoneSectorEndDeg.coerceIn(0f, 360f),
                                    showLaylines = selectedSeaChartShowLaylines,
                                    tackingAngleDeg = selectedSeaChartTackingAngleDeg.coerceIn(20, 80),
                                    laylineLengthNm = selectedSeaChartLaylineLengthNm.coerceIn(0.1f, 20f),
                                    showSafetyContour = selectedSeaChartShowSafetyContour,
                                    safetyDepthMeters = selectedSeaChartSafetyDepthMeters.coerceIn(0.5f, 50f),
                                    courseLineBearingDeg = wrap360Ui(selectedSeaChartCourseLineBearingDeg),
                                    courseLineDistanceNm = selectedSeaChartCourseLineDistanceNm.coerceIn(0.1f, 40f),
                                )
                                seaChartSettingsByWidget[menuWidgetId] = updated
                                onUpdateSeaChartWidgetSettings(
                                    menuWidgetId,
                                    serializeSeaChartWidgetSettings(updated),
                                )
                                seaChartDownloadScope.launch {
                                    val resolvedSource = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        activeSeaChartSourceLabel(
                                            context,
                                            normalizedMapProvider,
                                        )
                                    }
                                    val resolvedTemplate = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        activeSeaChartTileTemplate(
                                            context,
                                            normalizedMapProvider,
                                        )
                                    }
                                    seaChartSourceByWidget[menuWidgetId] = resolvedSource
                                    seaChartTileTemplateByProvider[normalizedMapProvider] = resolvedTemplate
                                }
                            }
                            if (menuWidget.kind == WidgetKind.ECHOSOUNDER) {
                                val current = echosounderSettingsByWidget
                                    .getOrPut(menuWidgetId) { EchosounderWidgetSettings() }
                                val updated = current.copy(
                                    minDepthMeters = selectedEchoMinDepth.coerceIn(0.1f, 40f),
                                    dynamicChangeRateMps = selectedEchoDynamicRate.coerceIn(0f, 5f),
                                    depthUnit = selectedEchoDepthUnit,
                                    alarmToneIndex = selectedEchoAlarmTone.coerceIn(
                                        0,
                                        ECHO_SOUNDER_TONES.lastIndex,
                                    ),
                                )
                                echosounderSettingsByWidget[menuWidgetId] = updated
                                onUpdateEchosounderWidgetSettings(
                                    menuWidgetId,
                                    serializeEchosounderWidgetSettings(updated),
                                )
                            }
                            if (menuWidget.kind == WidgetKind.LOG) {
                                val updated = LogWidgetSettings(
                                    periodMinutes = selectedLogWindowMinutes.coerceIn(1, 120),
                                    speedSource = selectedLogSpeedSource,
                                )
                                logSettingsByWidget[menuWidgetId] = updated
                                onUpdateLogWidgetSettings(
                                    menuWidgetId,
                                    serializeLogWidgetSettings(updated),
                                )
                            }
                            if (menuWidget.kind == WidgetKind.TEMPERATURE) {
                                val normalizedSensorNames = normalizeTemperatureSensorNames(selectedTemperatureSensorNames)
                                val updated = TemperatureWidgetSettings(
                                    unit = selectedTemperatureUnit,
                                    sensors = normalizedSensorNames,
                                )
                                temperatureSettingsByWidget[menuWidgetId] = updated
                                onUpdateTemperatureWidgetSettings(
                                    menuWidgetId,
                                    serializeTemperatureWidgetSettings(updated),
                                )
                            }
                            onRename(menuWidgetId, widgetRenameText.ifBlank { menuWidget.title })
                            showAnchorWatchChainCalibrationDialog = null
                            showWindHistoryWindowMenu = false
                            widgetMenuWidgetId = null
                            }
                        )
            },
                dismissButton = {
                    CompactMenuTextButton(
                        text = "Abbrechen",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = {
                            showAnchorWatchChainCalibrationDialog = null
                            showWindHistoryWindowMenu = false
                            widgetMenuWidgetId = null
                        }
                    )
            }
        )
            } else {
                widgetMenuWidgetId = null
            }
        }

        val startSeaChartDownload: (String, String, SeaChartMapProvider) -> Unit = startDownload@{ regionLabel, regionUrl, mapProvider ->
            val targetFileName = seaChartDownloadFileName(label = regionLabel, url = regionUrl)
            if (!seaChartHasUsableInternetConnection(context)) {
                val message = "Keine Internetverbindung erkannt. Download wurde nicht gestartet."
                logSeaChartInfo(
                    "seaCHART Download abgebrochen: offline, provider=${mapProvider.name}, region=$regionLabel",
                )
                seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                    message = message,
                    canRetry = true,
                    provider = mapProvider,
                    regionLabel = regionLabel,
                    regionUrl = regionUrl,
                    fileName = targetFileName,
                    filePath = seaChartDownloadTargetFilePath(
                        context = context,
                        mapProvider = mapProvider,
                        regionFolderName = seaChartDownloadTargetFolderName(regionLabel, regionUrl),
                        fileName = targetFileName,
                    ),
                )
                seaChartDownloadUiState = SeaChartDownloadUiState(
                    isRunning = false,
                    statusText = message,
                    downloadedFileName = targetFileName,
                    errorText = message,
                    completed = false,
                )
                onShowSeaChartNotice("Offlinekarten", message)
                return@startDownload
            }
            seaChartDownloadUiState = SeaChartDownloadUiState(
                isRunning = true,
                downloadedFileName = targetFileName,
                statusText = "Download gestartet",
                isUnpacking = false,
                completed = false,
                unpackedBytes = 0L,
                unpackTotalBytes = -1L,
                errorText = null,
            )
            seaChartDownloadErrorDialog = null
            val currentDownloadJob = seaChartDownloadScope.launch {
                val selectedProvider = mapProvider
                try {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        seaChartDeleteStaleDownloadFile(
                            context = context,
                            mapProvider = selectedProvider,
                            regionLabel = regionLabel,
                            regionUrl = regionUrl,
                            fileName = targetFileName,
                        )
                        cleanupSeaChartDownloadArtifacts(
                            context = context,
                            mapProvider = selectedProvider,
                            regionLabel = regionLabel,
                            regionUrl = regionUrl,
                        )
                        seaChartInvalidateSeaChartCaches(selectedProvider)
                    }
                    val downloadedFile = downloadSeaChartFileWithDownloadManager(
                        context = context,
                        url = regionUrl,
                        fileName = targetFileName,
                        mapProvider = selectedProvider,
                        regionLabel = regionLabel,
                        onDownloadId = { downloadId ->
                            seaChartDownloadManagerId = downloadId
                        },
                        onPersistentNotice = { statusText ->
                            onShowSeaChartNotice("seaCHART Status", statusText)
                        },
                        onProgress = { downloadedBytes, totalBytes ->
                            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                isRunning = true,
                                isUnpacking = false,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                unpackedBytes = 0L,
                                unpackTotalBytes = -1L,
                                progress = if (totalBytes > 0L) {
                                    (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    seaChartDownloadUiState.progress
                                },
                                statusText = if (totalBytes > 0L) {
                                    "Lädt: ${formatSeaChartMegabytes(downloadedBytes)} von ${formatSeaChartMegabytes(totalBytes)}"
                                } else {
                                    "Lädt: ${formatSeaChartMegabytes(downloadedBytes)}"
                                },
                                downloadedFileName = targetFileName,
                                errorText = null,
                            )
                        },
                        onUnpackProgress = { unpackedBytes, totalUnpackBytes ->
                            val unpackProgress = if (totalUnpackBytes > 0L) {
                                (unpackedBytes.toFloat() / totalUnpackBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                seaChartDownloadUiState.progress
                            }
                            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                isRunning = true,
                                isUnpacking = true,
                                unpackedBytes = unpackedBytes,
                                unpackTotalBytes = totalUnpackBytes,
                                progress = unpackProgress,
                                downloadedFileName = targetFileName,
                                errorText = null,
                            )
                        },
                        onStatus = { statusText ->
                            val unpackProgress = parseSeaChartUnpackPercent(statusText)
                            val isUnpackingPhase = statusText.startsWith("Entpacke:") ||
                                statusText.startsWith("Entpacken gestartet") ||
                                statusText.startsWith("Entpacken fehlgeschlagen") ||
                                statusText.startsWith("Prüfe ZIP:") ||
                                statusText.startsWith("ZIP-Prüfung gestartet") ||
                                statusText.startsWith("ZIP-Prüfung fehlgeschlagen")
                            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                isRunning = true,
                                statusText = statusText,
                                downloadedFileName = targetFileName,
                                isUnpacking = isUnpackingPhase,
                                progress = if (isUnpackingPhase && unpackProgress != null) {
                                    unpackProgress
                                } else {
                                    seaChartDownloadUiState.progress
                                },
                            )
                            announceSeaChartUnpackStatus(
                                context = context,
                                statusText = statusText,
                                onPersistentNotice = { message ->
                                    onShowSeaChartNotice("seaCHART Status", message)
                                },
                            )
                        },
                    )
                    page.widgets
                        .filter { isSeaChartWidgetKind(it.kind) }
                        .forEach { widget ->
                            val resolvedSource = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                activeSeaChartSourceLabel(
                                    context,
                                    selectedProvider,
                                )
                            }
                            val resolvedTemplate = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                activeSeaChartTileTemplate(
                                    context,
                                    selectedProvider,
                                )
                            }
                            seaChartSourceByWidget[widget.id] = resolvedSource
                            seaChartTileTemplateByProvider[selectedProvider] = resolvedTemplate
                        }
                    seaChartInvalidateSeaChartCaches(selectedProvider)
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val remoteInfo = seaChartCheckRemoteArchiveInfo(context, regionUrl).info
                        seaChartStoreDownloadMetadata(
                            context = context,
                            mapProvider = selectedProvider,
                            regionLabel = regionLabel,
                            regionUrl = regionUrl,
                            metadata = SeaChartDownloadMetadata(
                                fileName = targetFileName,
                                downloadedAtMillis = System.currentTimeMillis(),
                                etag = remoteInfo?.etag,
                                lastModified = remoteInfo?.lastModified,
                                contentLength = remoteInfo?.contentLength ?: downloadedFile.length(),
                            ),
                        )
                    }
                    val extractedFolder = downloadedFile.parentFile?.absolutePath.orEmpty()
                    val targetSummary = if (extractedFolder.isBlank()) {
                        "Download abgeschlossen"
                    } else {
                        "Download abgeschlossen\nKarte entpackt nach:\n$extractedFolder"
                    }
                    val resolvedStatusText = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        seaChartDisplayStatusMessage(
                            context,
                            selectedProvider,
                            targetSummary,
                        )
                    }
                    seaChartDownloadUiState = SeaChartDownloadUiState(
                        isRunning = false,
                        progress = 1f,
                        downloadedBytes = downloadedFile.length(),
                        totalBytes = downloadedFile.length(),
                        statusText = resolvedStatusText,
                        downloadedFileName = downloadedFile.name,
                        completed = true,
                        isUnpacking = false,
                        unpackedBytes = downloadedFile.length(),
                        unpackTotalBytes = downloadedFile.length(),
                        errorText = null,
                    )
                    seaChartLayerFilterCapabilitiesRefresh++
                    seaChartDownloadErrorDialog = null
                } catch (ex: kotlinx.coroutines.CancellationException) {
                    logSeaChartError(
                        "seaCHART Download abgebrochen: provider=${selectedProvider.name}, file=$targetFileName",
                        ex,
                    )
                    seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                        message = "Der Download/Entpack-Vorgang wurde abgebrochen.",
                        canRetry = false,
                        provider = selectedProvider,
                        regionLabel = regionLabel,
                        regionUrl = regionUrl,
                        fileName = targetFileName,
                        filePath = seaChartDownloadTargetFilePath(
                            context = context,
                            mapProvider = selectedProvider,
                            regionFolderName = seaChartDownloadTargetFolderName(regionLabel, regionUrl),
                            fileName = targetFileName,
                        ),
                    )
                    seaChartDownloadUiState = seaChartDownloadUiState.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "Download/Entpack abgebrochen",
                        isUnpacking = false,
                        unpackedBytes = 0L,
                        unpackTotalBytes = -1L,
                        errorText = "Download/Entpack wurde vom Nutzer abgebrochen.",
                        downloadedFileName = targetFileName,
                        completed = false,
                    )
                } catch (ex: Exception) {
                    val errorText = seaChartDownloadRetryHintMessage(ex.message ?: "Unbekannter Fehler")
                    seaChartClearDownloadSession(context)
                    logSeaChartError(
                        "seaCHART Download fehlgeschlagen: provider=${selectedProvider.name}, file=$targetFileName, message=$errorText",
                        ex,
                    )
                    seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                        message = errorText,
                        canRetry = true,
                        provider = selectedProvider,
                        regionLabel = regionLabel,
                        regionUrl = regionUrl,
                        fileName = targetFileName,
                        filePath = seaChartDownloadTargetFilePath(
                            context = context,
                            mapProvider = selectedProvider,
                            regionFolderName = seaChartDownloadTargetFolderName(regionLabel, regionUrl),
                            fileName = targetFileName,
                        ),
                    )
                    seaChartDownloadUiState = seaChartDownloadUiState.copy(
                        isRunning = false,
                        progress = 0f,
                        statusText = "Download fehlgeschlagen",
                        isUnpacking = false,
                        unpackedBytes = 0L,
                        unpackTotalBytes = -1L,
                        errorText = errorText,
                        downloadedFileName = targetFileName,
                        completed = false,
                    )
                } finally {
                    seaChartDownloadJob = null
                    seaChartDownloadManagerId = SEA_CHART_NO_ACTIVE_DOWNLOAD_ID
                }
            }
            seaChartDownloadJob = currentDownloadJob
        }

        val requestSeaChartDownload: (String, String, SeaChartMapProvider) -> Unit = { regionLabel, regionUrl, mapProvider ->
            val hasExistingFiles = seaChartRegionHasExistingFiles(
                context = context,
                mapProvider = mapProvider,
                regionLabel = regionLabel,
                regionUrl = regionUrl,
            )
            if (hasExistingFiles) {
                val targetFileName = seaChartDownloadFileName(label = regionLabel, url = regionUrl)
                seaChartDownloadUiState = SeaChartDownloadUiState(
                    isRunning = true,
                    downloadedFileName = targetFileName,
                    statusText = "Prüfe auf Aktualisierung...",
                    isUnpacking = false,
                    completed = false,
                    unpackedBytes = 0L,
                    unpackTotalBytes = -1L,
                    errorText = null,
                )
                seaChartDownloadErrorDialog = null
                val currentCheckJob = seaChartDownloadScope.launch {
                    try {
                        val localMetadata = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            seaChartLoadDownloadMetadata(
                                context = context,
                                mapProvider = mapProvider,
                                regionLabel = regionLabel,
                                regionUrl = regionUrl,
                            )
                        }
                        val remoteCheck = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            seaChartCheckRemoteArchiveInfo(context, regionUrl)
                        }
                        when (remoteCheck.status) {
                            SeaChartRemoteCheckStatus.OK -> when (
                                seaChartDetermineRemoteUpdateStatus(
                                    localMetadata = localMetadata,
                                    remoteInfo = remoteCheck.info,
                                )
                            ) {
                                SeaChartRemoteUpdateStatus.UNCHANGED -> {
                                    val message = "Keine neuere Karte für $regionLabel gefunden."
                                    logSeaChartInfo(
                                        "seaCHART Update-Prüfung: unverändert, provider=${mapProvider.name}, region=$regionLabel",
                                    )
                                    seaChartDownloadUiState = SeaChartDownloadUiState(
                                        isRunning = false,
                                        statusText = message,
                                        downloadedFileName = targetFileName,
                                        completed = false,
                                    )
                                    onShowSeaChartNotice("Offlinekarten", message)
                                }
                                SeaChartRemoteUpdateStatus.UPDATED -> {
                                    val message = "Für $regionLabel sind neuere Kartendaten verfügbar."
                                    logSeaChartInfo(
                                        "seaCHART Update-Prüfung: aktualisiert, provider=${mapProvider.name}, region=$regionLabel",
                                    )
                                    seaChartExistingDataPrompt = SeaChartExistingDataPromptState(
                                        request = SeaChartDownloadRequest(
                                            regionLabel = regionLabel,
                                            regionUrl = regionUrl,
                                            mapProvider = mapProvider,
                                        ),
                                        title = "Aktualisierte Kartendaten gefunden",
                                        message = "Für diese Region sind auf dem Server neuere Kartendaten verfügbar. Empfehlung: aktualisieren.",
                                        confirmText = "Aktualisieren",
                                    )
                                    seaChartDownloadUiState = SeaChartDownloadUiState(
                                        isRunning = false,
                                        statusText = message,
                                        downloadedFileName = targetFileName,
                                        completed = false,
                                    )
                                    onShowSeaChartNotice("Offlinekarten", message)
                                }
                                SeaChartRemoteUpdateStatus.UNKNOWN -> {
                                    val message = "Serverstand für $regionLabel konnte nicht sicher verglichen werden."
                                    logSeaChartInfo(
                                        "seaCHART Update-Prüfung: nicht eindeutig, provider=${mapProvider.name}, region=$regionLabel",
                                    )
                                    seaChartExistingDataPrompt = SeaChartExistingDataPromptState(
                                        request = SeaChartDownloadRequest(
                                            regionLabel = regionLabel,
                                            regionUrl = regionUrl,
                                            mapProvider = mapProvider,
                                        ),
                                        title = "Kartendaten vorhanden",
                                        message = "Vorhandene Kartendaten wurden gefunden, aber der Serverstand konnte nicht sicher verglichen werden. Du kannst neu laden oder abbrechen.",
                                        confirmText = "Neu laden",
                                    )
                                    seaChartDownloadUiState = SeaChartDownloadUiState(
                                        isRunning = false,
                                        statusText = message,
                                        downloadedFileName = targetFileName,
                                        completed = false,
                                    )
                                    onShowSeaChartNotice("Offlinekarten", message)
                                }
                            }
                            SeaChartRemoteCheckStatus.OFFLINE -> {
                                seaChartExistingDataPrompt = SeaChartExistingDataPromptState(
                                    request = SeaChartDownloadRequest(
                                        regionLabel = regionLabel,
                                        regionUrl = regionUrl,
                                        mapProvider = mapProvider,
                                    ),
                                    title = "Offline erkannt",
                                    message = "Keine Internetverbindung erkannt. Vorhandene Kartendaten wurden nicht verändert. Du kannst später erneut prüfen oder jetzt trotzdem neu laden.",
                                    confirmText = "Neu laden",
                                )
                                val notice = remoteCheck.message ?: "Keine Internetverbindung erkannt."
                                logSeaChartInfo(
                                    "seaCHART Update-Prüfung: offline, provider=${mapProvider.name}, region=$regionLabel",
                                )
                                seaChartDownloadUiState = SeaChartDownloadUiState(
                                    isRunning = false,
                                    statusText = notice,
                                    downloadedFileName = targetFileName,
                                    completed = false,
                                )
                                onShowSeaChartNotice("Offlinekarten", notice)
                            }
                            SeaChartRemoteCheckStatus.UNREACHABLE -> {
                                val message = remoteCheck.message ?: "Server nicht erreichbar oder Verbindung zu schwach."
                                logSeaChartInfo(
                                    "seaCHART Update-Prüfung: server nicht erreichbar, provider=${mapProvider.name}, region=$regionLabel",
                                )
                                seaChartExistingDataPrompt = SeaChartExistingDataPromptState(
                                    request = SeaChartDownloadRequest(
                                        regionLabel = regionLabel,
                                        regionUrl = regionUrl,
                                        mapProvider = mapProvider,
                                    ),
                                    title = "Server nicht erreichbar",
                                    message = "Der Server antwortet nicht oder die Verbindung ist zu schwach. Vorhandene Kartendaten wurden nicht verändert. Du kannst später erneut prüfen oder jetzt trotzdem neu laden.",
                                    confirmText = "Neu laden",
                                )
                                seaChartDownloadUiState = SeaChartDownloadUiState(
                                    isRunning = false,
                                    statusText = message,
                                    downloadedFileName = targetFileName,
                                    completed = false,
                                )
                                onShowSeaChartNotice("Offlinekarten", message)
                            }
                            SeaChartRemoteCheckStatus.UNKNOWN -> {
                                val message = remoteCheck.message ?: "Serverstand für $regionLabel konnte nicht sicher verglichen werden."
                                logSeaChartInfo(
                                    "seaCHART Update-Prüfung: nicht eindeutig, provider=${mapProvider.name}, region=$regionLabel",
                                )
                                seaChartExistingDataPrompt = SeaChartExistingDataPromptState(
                                    request = SeaChartDownloadRequest(
                                        regionLabel = regionLabel,
                                        regionUrl = regionUrl,
                                        mapProvider = mapProvider,
                                    ),
                                    title = "Kartendaten vorhanden",
                                    message = "Vorhandene Kartendaten wurden gefunden, aber der Serverstand konnte nicht sicher verglichen werden. Du kannst neu laden oder abbrechen.",
                                    confirmText = "Neu laden",
                                )
                                seaChartDownloadUiState = SeaChartDownloadUiState(
                                    isRunning = false,
                                    statusText = message,
                                    downloadedFileName = targetFileName,
                                    completed = false,
                                )
                                onShowSeaChartNotice("Offlinekarten", message)
                            }
                        }
                    } catch (error: Exception) {
                        logSeaChartError(
                            "seaCHART Update-Prüfung fehlgeschlagen: provider=${mapProvider.name}, region=$regionLabel",
                            error,
                        )
                        seaChartExistingDataPrompt = SeaChartExistingDataPromptState(
                            request = SeaChartDownloadRequest(
                                regionLabel = regionLabel,
                                regionUrl = regionUrl,
                                mapProvider = mapProvider,
                            ),
                            title = "Update-Prüfung fehlgeschlagen",
                            message = "Der Server konnte nicht sicher geprüft werden. Du kannst neu laden oder abbrechen.",
                            confirmText = "Neu laden",
                        )
                        seaChartDownloadUiState = SeaChartDownloadUiState(
                            isRunning = false,
                            statusText = "Update-Prüfung fehlgeschlagen.",
                            downloadedFileName = targetFileName,
                            errorText = error.message,
                            completed = false,
                        )
                        onShowSeaChartNotice(
                            "Offlinekarten",
                            "Update-Prüfung für $regionLabel fehlgeschlagen.",
                        )
                    } finally {
                        seaChartDownloadJob = null
                    }
                }
                seaChartDownloadJob = currentCheckJob
            } else {
                startSeaChartDownload(regionLabel, regionUrl, mapProvider)
            }
        }

        val seaChartDownloadWidgetId = showSeaChartDownloadDialog
        if (
            seaChartDownloadWidgetId != null &&
            page.widgets.any { it.id == seaChartDownloadWidgetId && isSeaChartWidgetKind(it.kind) }
        ) {
                val uriHandler = LocalUriHandler.current
                val selectedSeaChartDownloadCatalog = seaChartDownloadCatalog
                    ?: SeaChartDownloadCatalog.OPEN_SEA_CHARTS
                val menuTitle = seaChartDownloadCatalogDialogTitle(selectedSeaChartDownloadCatalog)
                val menuDescription = seaChartDownloadCatalogDescription(selectedSeaChartDownloadCatalog)
                val seaChartDownloadRegions = seaChartDownloadCatalogRegions(selectedSeaChartDownloadCatalog)
                val seaChartDownloadResources = seaChartDownloadCatalogResources(selectedSeaChartDownloadCatalog)
                val selectedSeaChartDownloadProvider = seaChartDownloadCatalogProvider(selectedSeaChartDownloadCatalog)
                val hasRegionDownloads = seaChartDownloadRegions.isNotEmpty()
                var selectedSeaChartDownloadRegionIndex by rememberSaveable(page.id, selectedSeaChartDownloadCatalog) {
                    mutableIntStateOf(0)
                }
                val selectedSeaChartDownloadRegion = seaChartDownloadRegions.getOrNull(selectedSeaChartDownloadRegionIndex)
                val isDownloading = seaChartDownloadUiState.isRunning
                val downloadedRegionsForProvider = seaChartDownloadedRegionsByProvider[selectedSeaChartDownloadProvider]
                    ?: emptyList()
                LaunchedEffect(selectedSeaChartDownloadCatalog, seaChartDownloadedRegionsRefresh) {
                    val provider = selectedSeaChartDownloadProvider
                    val providerRegions = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        seaChartDownloadedRegions(context, provider)
                    }
                    seaChartDownloadedRegionsByProvider = seaChartDownloadedRegionsByProvider
                        .toMutableMap()
                        .apply { put(provider, providerRegions) }
                }
                /* val startSeaChartDownload: (String, String, SeaChartMapProvider) -> Unit =
                    { regionLabel, regionUrl, mapProvider ->
                        val targetFileName = seaChartDownloadFileName(label = regionLabel, url = regionUrl)
                        seaChartDeleteStaleDownloadFile(
                            context = context,
                            mapProvider = mapProvider,
                            regionLabel = regionLabel,
                            regionUrl = regionUrl,
                            fileName = targetFileName,
                        )
                        cleanupSeaChartDownloadArtifacts(
                            context = context,
                            mapProvider = mapProvider,
                            regionLabel = regionLabel,
                            regionUrl = regionUrl,
                        )
                        seaChartDownloadUiState = SeaChartDownloadUiState(
                            isRunning = true,
                            downloadedFileName = targetFileName,
                            statusText = "Download gestartet",
                            isUnpacking = false,
                            completed = false,
                            unpackedBytes = 0L,
                            unpackTotalBytes = -1L,
                            errorText = null,
                        )
                        seaChartDownloadErrorDialog = null
                        val currentDownloadJob = seaChartDownloadScope.launch {
                            val selectedProvider = mapProvider
                            try {
                                val downloadedFile = downloadSeaChartFileWithDownloadManager(
                                    context = context,
                                    url = regionUrl,
                                    fileName = targetFileName,
                                    mapProvider = selectedProvider,
                                    regionLabel = regionLabel,
                                    onDownloadId = { downloadId ->
                                        seaChartDownloadManagerId = downloadId
                                    },
                                    onPersistentNotice = { statusText ->
                                        onShowSeaChartNotice("seaCHART Status", statusText)
                                    },
                                    onProgress = { downloadedBytes, totalBytes ->
                                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                            isRunning = true,
                                            isUnpacking = false,
                                            downloadedBytes = downloadedBytes,
                                            totalBytes = totalBytes,
                                            unpackedBytes = 0L,
                                            unpackTotalBytes = -1L,
                                            progress = if (totalBytes > 0L) {
                                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                            } else {
                                                seaChartDownloadUiState.progress
                                            },
                                            statusText = if (totalBytes > 0L) {
                                                "Lädt: ${formatSeaChartMegabytes(downloadedBytes)} von ${formatSeaChartMegabytes(totalBytes)}"
                                            } else {
                                                "Lädt: ${formatSeaChartMegabytes(downloadedBytes)}"
                                            },
                                            downloadedFileName = targetFileName,
                                            errorText = null,
                                        )
                                    },
                                    onUnpackProgress = { unpackedBytes, totalUnpackBytes ->
                                        val unpackProgress = if (totalUnpackBytes > 0L) {
                                            (unpackedBytes.toFloat() / totalUnpackBytes.toFloat()).coerceIn(0f, 1f)
                                        } else {
                                            seaChartDownloadUiState.progress
                                        }
                                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                            isRunning = true,
                                            isUnpacking = true,
                                            unpackedBytes = unpackedBytes,
                                            unpackTotalBytes = totalUnpackBytes,
                                            progress = unpackProgress,
                                            downloadedFileName = targetFileName,
                                            errorText = null,
                                        )
                                    },
                                    onStatus = { statusText ->
                                        val unpackProgress = parseSeaChartUnpackPercent(statusText)
                                        val isUnpackingPhase = statusText.startsWith("Entpacke:") ||
                                            statusText.startsWith("Entpacken gestartet") ||
                                            statusText.startsWith("Entpacken fehlgeschlagen")
                                        seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                            isRunning = true,
                                            statusText = statusText,
                                            downloadedFileName = targetFileName,
                                            isUnpacking = isUnpackPhase,
                                            progress = if (isUnpackingPhase && unpackProgress != null) {
                                                unpackProgress
                                            } else {
                                                seaChartDownloadUiState.progress
                                            },
                                        )
                                        announceSeaChartUnpackStatus(
                                            context = context,
                                            statusText = statusText,
                                            onPersistentNotice = { message ->
                                                onShowSeaChartNotice("seaCHART Status", message)
                                            },
                                        )
                                    },
                                )
                                page.widgets
                                    .filter { isSeaChartWidgetKind(it.kind) }
                                    .forEach { widget ->
                                        val resolvedSource = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            activeSeaChartSourceLabel(
                                                context,
                                                selectedProvider,
                                            )
                                        }
                                        val resolvedTemplate = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            activeSeaChartTileTemplate(
                                                context,
                                                selectedProvider,
                                            )
                                        }
                                        seaChartSourceByWidget[widget.id] = resolvedSource
                                        seaChartTileTemplateByProvider[selectedProvider] = resolvedTemplate
                                    }
                                val extractedFolder = downloadedFile.parentFile?.absolutePath.orEmpty()
                                val targetSummary = if (extractedFolder.isBlank()) {
                                    "Download abgeschlossen"
                                } else {
                                    "Download abgeschlossen\nKarte entpackt nach:\n$extractedFolder"
                                }
                                val resolvedStatusText = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    seaChartDisplayStatusMessage(
                                        context,
                                        selectedProvider,
                                        targetSummary,
                                    )
                                }
                                seaChartDownloadUiState = SeaChartDownloadUiState(
                                    isRunning = false,
                                    progress = 1f,
                                    downloadedBytes = downloadedFile.length(),
                                    totalBytes = downloadedFile.length(),
                                    statusText = resolvedStatusText,
                                    downloadedFileName = downloadedFile.name,
                                    completed = true,
                                    isUnpacking = false,
                                    unpackedBytes = downloadedFile.length(),
                                    unpackTotalBytes = downloadedFile.length(),
                                    errorText = null,
                                )
                                seaChartLayerFilterCapabilitiesRefresh++
                                seaChartDownloadErrorDialog = null
                            } catch (ex: kotlinx.coroutines.CancellationException) {
                                logSeaChartError(
                                    "seaCHART Dialog-Download abgebrochen: provider=${selectedProvider.name}, file=$targetFileName",
                                    ex,
                                )
                                seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                                    message = "Der Download/Entpack-Vorgang wurde abgebrochen.",
                                    canRetry = false,
                                    provider = selectedProvider,
                                    regionLabel = regionLabel,
                                    regionUrl = regionUrl,
                                    fileName = targetFileName,
                                    filePath = seaChartDownloadTargetFilePath(
                                        context = context,
                                        mapProvider = selectedProvider,
                                        regionFolderName = seaChartDownloadTargetFolderName(regionLabel, regionUrl),
                                        fileName = targetFileName,
                                    ),
                                )
                                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                    isRunning = false,
                                    progress = 0f,
                                    statusText = "Download/Entpack abgebrochen",
                                    isUnpacking = false,
                                    unpackedBytes = 0L,
                                    unpackTotalBytes = -1L,
                                    errorText = "Download/Entpack wurde vom Nutzer abgebrochen.",
                                    downloadedFileName = targetFileName,
                                    completed = false,
                                )
                            } catch (ex: Exception) {
                                val errorText = seaChartDownloadRetryHintMessage(ex.message ?: "Unbekannter Fehler")
                                seaChartClearDownloadSession(context)
                                logSeaChartError(
                                    "seaCHART Dialog-Download fehlgeschlagen: provider=${selectedProvider.name}, file=$targetFileName, message=$errorText",
                                    ex,
                                )
                                seaChartDownloadErrorDialog = SeaChartDownloadErrorState(
                                    message = errorText,
                                    canRetry = true,
                                    provider = selectedProvider,
                                    regionLabel = regionLabel,
                                    regionUrl = regionUrl,
                                    fileName = targetFileName,
                                    filePath = seaChartDownloadTargetFilePath(
                                        context = context,
                                        mapProvider = selectedProvider,
                                        regionFolderName = seaChartDownloadTargetFolderName(regionLabel, regionUrl),
                                        fileName = targetFileName,
                                    ),
                                )
                                seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                    isRunning = false,
                                    progress = 0f,
                                    statusText = "Download fehlgeschlagen",
                                    isUnpacking = false,
                                    unpackedBytes = 0L,
                                    unpackTotalBytes = -1L,
                                    errorText = errorText,
                                    downloadedFileName = targetFileName,
                                    completed = false,
                                )
                            } finally {
                                seaChartDownloadJob = null
                                seaChartDownloadManagerId = SEA_CHART_NO_ACTIVE_DOWNLOAD_ID
                            }
                        }
                        seaChartDownloadJob = currentDownloadJob */
                CompactMenuDialog(
                    onDismissRequest = {
                        showSeaChartDownloadDialog = null
                        seaChartDownloadCatalog = null
                    },
                    isDarkMenu = darkBackground,
                    title = { Text(menuTitle, style = widgetMenuTextStyle) },
                    text = {
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            ProvideTextStyle(value = widgetMenuTextStyle) {
                                Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                    Text(menuDescription, style = widgetMenuTextStyle)
                                    HorizontalDivider()
                                    if (hasRegionDownloads) {
                                        Text("AT5 Regionen", style = widgetMenuTextStyle)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            OptionSelectorRow(
                                                selectedOption = selectedSeaChartDownloadRegionIndex,
                                                options = seaChartDownloadRegions.indices.toList(),
                                                optionLabel = { index ->
                                                    seaChartDownloadRegions[index].first
                                                },
                                                onOptionSelected = {
                                                    selectedSeaChartDownloadRegionIndex = it
                                                    seaChartDownloadUiState = SeaChartDownloadUiState(
                                                        statusText = "Neue Region gewählt: ${seaChartDownloadRegions[it].first}",
                                                    )
                                                },
                                                style = widgetMenuTextStyle,
                                                darkBackground = darkBackground,
                                            )
                                            CompactMenuTextButton(
                                                text = if (isDownloading) "Download läuft..." else "Karte laden",
                                                style = widgetLinkTextStyle,
                                                fillWidth = false,
                                                enabled = !isDownloading,
                                                onClick = {
                                                    val region = seaChartDownloadRegions.getOrNull(selectedSeaChartDownloadRegionIndex)
                                                        ?: return@CompactMenuTextButton
                                                    requestSeaChartDownload(
                                                        region.first,
                                                        region.second,
                                                        selectedSeaChartDownloadProvider,
                                                    )
                                                },
                                            )
                                            CompactMenuTextButton(
                                                text = "Im Browser öffnen",
                                                style = widgetLinkTextStyle,
                                                fillWidth = false,
                                                onClick = {
                                                    val region = seaChartDownloadRegions.getOrNull(selectedSeaChartDownloadRegionIndex)
                                                    if (region != null) {
                                                        uriHandler.openUri(region.second)
                                                    }
                                                },
                                                enabled = hasRegionDownloads,
                                            )
                                        }
                                        if (selectedSeaChartDownloadRegion != null) {
                                            Text(
                                                selectedSeaChartDownloadRegion.third,
                                                style = widgetMenuTextStyle.copy(
                                                    color = widgetMenuTextStyle.color.copy(alpha = 0.75f),
                                                ),
                                            )
                                        }
                                    } else {
                                        Text(
                                            "Für diese Quelle sind aktuell keine vordefinierten Regionen hinterlegt.",
                                            style = widgetMenuTextStyle,
                                        )
                                    }
                                    if (seaChartDownloadUiState.statusText.isNotBlank()) {
                                        HorizontalDivider()
                                        val showUnpackProgress = seaChartDownloadUiState.isUnpacking
                                        val operationBytes = if (showUnpackProgress) {
                                            seaChartDownloadUiState.unpackedBytes
                                        } else {
                                            seaChartDownloadUiState.downloadedBytes
                                        }
                                        val operationTotalBytes = if (showUnpackProgress) {
                                            seaChartDownloadUiState.unpackTotalBytes
                                        } else {
                                            seaChartDownloadUiState.totalBytes
                                        }
                                        val operationProgress = if (operationTotalBytes > 0L) {
                                            (operationBytes.toFloat() / operationTotalBytes.toFloat()).coerceIn(0f, 1f)
                                        } else {
                                            seaChartDownloadUiState.progress
                                        }
                                        if (isDownloading || showUnpackProgress) {
                                            if (operationTotalBytes > 0L) {
                                                LinearProgressIndicator(
                                                    progress = operationProgress,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                        val statusDisplayText = when {
                                            seaChartDownloadUiState.errorText != null -> "Fehler: ${seaChartDownloadUiState.errorText}"
                                            showUnpackProgress && seaChartDownloadUiState.unpackTotalBytes > 0L ->
                                                "Entpacke: ${formatSeaChartMegabytes(seaChartDownloadUiState.unpackedBytes)} " +
                                                    "von ${formatSeaChartMegabytes(seaChartDownloadUiState.unpackTotalBytes)} " +
                                                    "(${(operationProgress * 100f).roundToInt()} %)"
                                            showUnpackProgress ->
                                                "Entpacke: ${formatSeaChartMegabytes(seaChartDownloadUiState.unpackedBytes)}"
                                            isDownloading && seaChartDownloadUiState.totalBytes > 0L ->
                                                "Lädt: ${formatSeaChartMegabytes(seaChartDownloadUiState.downloadedBytes)} " +
                                                    "von ${formatSeaChartMegabytes(seaChartDownloadUiState.totalBytes)} " +
                                                    "(${(operationProgress * 100f).roundToInt()} %)"
                                            isDownloading ->
                                                seaChartDownloadUiState.statusText
                                            seaChartDownloadUiState.completed ->
                                                "${seaChartDownloadUiState.statusText} (${seaChartDownloadUiState.downloadedFileName})"
                                            else -> seaChartDownloadUiState.statusText
                                        }
                                        Text(
                                            statusDisplayText,
                                            style = widgetMenuTextStyle.copy(
                                                color = if (seaChartDownloadUiState.errorText != null) {
                                                    Color(0xFFFF6E73)
                                                } else {
                                                    widgetMenuTextStyle.color
                                                },
                                            ),
                                        )
                                        if (seaChartDownloadUiState.downloadedFileName.isNotBlank()) {
                                            Text(
                                                "Datei: ${seaChartDownloadUiState.downloadedFileName}",
                                                style = widgetMenuTextStyle.copy(
                                                    color = widgetMenuTextStyle.color.copy(alpha = 0.65f),
                                                ),
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Bereits heruntergeladene Regionen", style = widgetMenuTextStyle)
                                    if (downloadedRegionsForProvider.isEmpty()) {
                                        Text(
                                            "Noch keine Karten für diesen Provider gefunden.",
                                            style = widgetMenuTextStyle.copy(
                                                color = widgetMenuTextStyle.color.copy(alpha = 0.7f),
                                            ),
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            downloadedRegionsForProvider.forEach { region ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top,
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                                    ) {
                                                        Text(
                                                            "• ${formatSeaChartRegionLabel(region.regionName)}",
                                                            style = widgetMenuTextStyle,
                                                        )
                                                        Text(
                                                            "  geladen am: ${formatSeaChartDownloadTimestamp(region.lastLoadedAtMillis)} (${region.fileName})",
                                                            style = widgetMenuTextStyle.copy(
                                                                color = widgetMenuTextStyle.color.copy(alpha = 0.7f),
                                                            ),
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        CompactMenuTextButton(
                                                            text = "Info",
                                                            style = widgetLinkTextStyle,
                                                            fillWidth = false,
                                                            onClick = {
                                                                seaChartDownloadedRegionInfoDialog = region
                                                            },
                                                        )
                                                        CompactMenuTextButton(
                                                            text = "Karte löschen",
                                                            style = widgetLinkTextStyle,
                                                            fillWidth = false,
                                                            onClick = {
                                                                seaChartDeleteRegionPrompt = region
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider()
                                    seaChartDownloadResources.forEach { (label, url, description) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(label, style = widgetMenuTextStyle)
                                            Text(description, style = widgetMenuTextStyle.copy(color = widgetMenuTextStyle.color.copy(alpha = 0.8f)))
                                            CompactMenuTextButton(
                                                text = "Link öffnen",
                                                style = widgetLinkTextStyle,
                                                fillWidth = false,
                                                    onClick = { uriHandler.openUri(url) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isDownloading) {
                                CompactMenuTextButton(
                                    text = "Abbrechen",
                                    style = widgetMenuTextStyle,
                                    fillWidth = false,
                                    onClick = {
                                        val currentJob = seaChartDownloadJob
                                        if (currentJob?.isActive == true) {
                                            seaChartCancelDownloadManager(context, seaChartDownloadManagerId)
                                            seaChartClearDownloadSession(context)
                                            seaChartDownloadManagerId = SEA_CHART_NO_ACTIVE_DOWNLOAD_ID
                                            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                                isRunning = true,
                                                isUnpacking = false,
                                                statusText = "Abbruch wird durchgeführt...",
                                                errorText = null,
                                            )
                                            currentJob.cancel(kotlinx.coroutines.CancellationException("Abbruch durch Nutzer"))
                                        }
                                        seaChartDownloadErrorDialog = null
                                    },
                                )
                            }
                            CompactMenuTextButton(
                                text = "Schließen",
                                style = widgetMenuTextStyle,
                                fillWidth = false,
                                onClick = {
                                    showSeaChartDownloadDialog = null
                                    seaChartDownloadCatalog = null
                                },
                            )
                        }
                    },
                )
                val existingDataPrompt = seaChartExistingDataPrompt
                if (existingDataPrompt != null) {
                    CompactMenuDialog(
                        onDismissRequest = { seaChartExistingDataPrompt = null },
                        isDarkMenu = darkBackground,
                        title = { Text(existingDataPrompt.title, style = widgetMenuTextStyle) },
                        text = {
                            CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                                ProvideTextStyle(value = widgetMenuTextStyle) {
                                    Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                        Text(
                                            existingDataPrompt.message,
                                            style = widgetMenuTextStyle,
                                        )
                                        Text(
                                            "Region: ${existingDataPrompt.request.regionLabel}",
                                            style = widgetMenuTextStyle.copy(
                                                color = widgetMenuTextStyle.color.copy(alpha = 0.74f),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            CompactMenuTextButton(
                                text = existingDataPrompt.confirmText,
                                style = widgetLinkTextStyle,
                                fillWidth = false,
                                onClick = {
                                    seaChartExistingDataPrompt = null
                                    startSeaChartDownload(
                                        existingDataPrompt.request.regionLabel,
                                        existingDataPrompt.request.regionUrl,
                                        existingDataPrompt.request.mapProvider,
                                    )
                                },
                            )
                        },
                        dismissButton = {
                            CompactMenuTextButton(
                                text = "Abbrechen",
                                style = widgetLinkTextStyle,
                                fillWidth = false,
                                onClick = {
                                    seaChartExistingDataPrompt = null
                                    seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                        statusText = "Download abgebrochen. Vorhandene Kartendaten wurden nicht verändert.",
                                        isRunning = false,
                                        isUnpacking = false,
                                        completed = false,
                                        progress = 0f,
                                    )
                                },
                            )
                        },
                    )
                }
        } else if (seaChartDownloadWidgetId != null) {
            showSeaChartDownloadDialog = null
            seaChartDownloadCatalog = null
        }

        val downloadedRegionInfoDialog = seaChartDownloadedRegionInfoDialog
        if (downloadedRegionInfoDialog != null) {
            CompactMenuDialog(
                onDismissRequest = { seaChartDownloadedRegionInfoDialog = null },
                isDarkMenu = darkBackground,
                title = { Text(formatSeaChartRegionLabel(downloadedRegionInfoDialog.regionName), style = widgetMenuTextStyle) },
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        ProvideTextStyle(value = widgetMenuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Text(
                                    "Datei: ${downloadedRegionInfoDialog.fileName}",
                                    style = widgetMenuTextStyle,
                                )
                                Text(
                                    "Geladen am: ${formatSeaChartDownloadTimestamp(downloadedRegionInfoDialog.lastLoadedAtMillis)}",
                                    style = widgetMenuTextStyle,
                                )
                                Text(
                                    "Pfad: ${downloadedRegionInfoDialog.filePath}",
                                    style = widgetMenuTextStyle.copy(
                                        color = widgetMenuTextStyle.color.copy(alpha = 0.74f),
                                    ),
                                )
                                if (!downloadedRegionInfoDialog.isRenderable && !downloadedRegionInfoDialog.formatNote.isNullOrBlank()) {
                                    Text(
                                        "Hinweis: ${downloadedRegionInfoDialog.formatNote}",
                                        style = widgetMenuTextStyle.copy(
                                            color = widgetMenuTextStyle.color.copy(alpha = 0.74f),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Schließen",
                        style = widgetLinkTextStyle,
                        fillWidth = false,
                        onClick = { seaChartDownloadedRegionInfoDialog = null },
                    )
                },
            )
        }

        val deleteRegionPrompt = seaChartDeleteRegionPrompt
        if (deleteRegionPrompt != null) {
            CompactMenuDialog(
                onDismissRequest = { seaChartDeleteRegionPrompt = null },
                isDarkMenu = darkBackground,
                title = { Text("Karte löschen", style = widgetMenuTextStyle) },
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        ProvideTextStyle(value = widgetMenuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Text(
                                    "Soll die heruntergeladene Karte wirklich gelöscht werden?",
                                    style = widgetMenuTextStyle,
                                )
                                Text(
                                    formatSeaChartRegionLabel(deleteRegionPrompt.regionName),
                                    style = widgetMenuTextStyle.copy(
                                        color = widgetMenuTextStyle.color.copy(alpha = 0.82f),
                                    ),
                                )
                                Text(
                                    deleteRegionPrompt.filePath,
                                    style = widgetMenuTextStyle.copy(
                                        color = widgetMenuTextStyle.color.copy(alpha = 0.68f),
                                    ),
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Karte löschen",
                        style = widgetLinkTextStyle,
                        fillWidth = false,
                        onClick = {
                            val deleted = seaChartDeletePathRecursively(deleteRegionPrompt.filePath)
                            seaChartDeleteRegionPrompt = null
                            seaChartDownloadedRegionInfoDialog = null
                            seaChartDownloadedRegionsRefresh += 1
                            seaChartLayerFilterCapabilitiesRefresh += 1
                            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                statusText = if (deleted) {
                                    "Karte gelöscht: ${formatSeaChartRegionLabel(deleteRegionPrompt.regionName)}"
                                } else {
                                    "Karte konnte nicht gelöscht werden: ${formatSeaChartRegionLabel(deleteRegionPrompt.regionName)}"
                                },
                                isRunning = false,
                                isUnpacking = false,
                                completed = false,
                                errorText = null,
                            )
                            onShowSeaChartNotice(
                                "Offlinekarten",
                                if (deleted) {
                                    "Karte gelöscht: ${formatSeaChartRegionLabel(deleteRegionPrompt.regionName)}"
                                } else {
                                    "Löschen fehlgeschlagen: ${formatSeaChartRegionLabel(deleteRegionPrompt.regionName)}"
                                },
                            )
                        },
                    )
                },
                dismissButton = {
                    CompactMenuTextButton(
                        text = "Abbrechen",
                        style = widgetLinkTextStyle,
                        fillWidth = false,
                        onClick = { seaChartDeleteRegionPrompt = null },
                    )
                },
            )
        }

        val seaChartLayerFilterDialogWidgetId = showSeaChartLayerFilterDialog
        if (
            seaChartLayerFilterDialogWidgetId != null &&
            page.widgets.any { it.id == seaChartLayerFilterDialogWidgetId && isLegacySeaChartWidgetKind(it.kind) }
        ) {
            var capabilities by remember(
                selectedSeaChartMapProvider,
                seaChartLayerFilterCapabilitiesRefresh,
            ) { mutableStateOf(SeaChartLayerFilterCapabilities()) }
            LaunchedEffect(selectedSeaChartMapProvider, seaChartLayerFilterCapabilitiesRefresh) {
                capabilities = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    seaChartLayerFilterCapabilities(
                        context = context,
                        mapProvider = selectedSeaChartMapProvider,
                    )
                }
            }
            CompactMenuDialog(
                onDismissRequest = { showSeaChartLayerFilterDialog = null },
                isDarkMenu = darkBackground,
                title = { Text("Kartendetails", style = widgetMenuTextStyle) },
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        ProvideTextStyle(value = widgetMenuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Text(
                                    "Auswahl der Kartendetails, sofern diese unterstützt werden",
                                    style = widgetMenuTextStyle.copy(
                                        color = widgetMenuTextStyle.color.copy(alpha = 0.75f),
                                    ),
                                )
                                if (!capabilities.hasAnyLayer()) {
                                    Text(
                                        "Für diese Karte wurden keine auswertbaren Kartendetails erkannt.",
                                        style = widgetMenuTextStyle.copy(
                                            color = widgetMenuTextStyle.color.copy(alpha = 0.75f),
                                        ),
                                    )
                                }
                                if (capabilities.supportsRoadLayers) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Straßen", style = widgetMenuTextStyle)
                                        Switch(
                                            checked = seaChartLayerFilterRoadLayers,
                                            onCheckedChange = { seaChartLayerFilterRoadLayers = it },
                                            modifier = Modifier.scale(0.5f),
                                        )
                                    }
                                }
                                if (capabilities.supportsDepthLines) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Tiefenlinien", style = widgetMenuTextStyle)
                                        Switch(
                                            checked = seaChartLayerFilterDepthLines,
                                            onCheckedChange = { seaChartLayerFilterDepthLines = it },
                                            modifier = Modifier.scale(0.5f),
                                        )
                                    }
                                }
                                if (capabilities.supportsContourLines) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Höhenlinien", style = widgetMenuTextStyle)
                                        Switch(
                                            checked = seaChartLayerFilterContourLines,
                                            onCheckedChange = { seaChartLayerFilterContourLines = it },
                                            modifier = Modifier.scale(0.5f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompactMenuTextButton(
                            text = "Abbrechen",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = {
                                showSeaChartLayerFilterDialog = null
                                val currentSettings = seaChartSettingsByWidget[seaChartLayerFilterDialogWidgetId]
                                if (currentSettings != null) {
                                    seaChartLayerFilterRoadLayers = currentSettings.showRoadLayers
                                    seaChartLayerFilterDepthLines = currentSettings.showDepthLines
                                    seaChartLayerFilterContourLines = currentSettings.showContourLines
                                }
                            },
                        )
                        CompactMenuTextButton(
                            text = "Speichern",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = {
                                val currentSettings = seaChartSettingsByWidget[seaChartLayerFilterDialogWidgetId]
                                if (currentSettings != null) {
                                    selectedSeaChartShowRoadLayers = seaChartLayerFilterRoadLayers
                                    selectedSeaChartShowDepthLines = seaChartLayerFilterDepthLines
                                    selectedSeaChartShowContourLines = seaChartLayerFilterContourLines
                                    seaChartSettingsByWidget[seaChartLayerFilterDialogWidgetId] = currentSettings.copy(
                                        showRoadLayers = seaChartLayerFilterRoadLayers,
                                        showDepthLines = seaChartLayerFilterDepthLines,
                                        showContourLines = seaChartLayerFilterContourLines,
                                    )
                                }
                                showSeaChartLayerFilterDialog = null
                            },
                        )
                    }
                },
            )
        } else if (seaChartLayerFilterDialogWidgetId != null) {
            showSeaChartLayerFilterDialog = null
        }

        if (seaChartPendingZipApprovalFiles.isNotEmpty()) {
            CompactMenuDialog(
                onDismissRequest = {},
                isDarkMenu = darkBackground,
                title = { Text("ZIP-Dateien bereit", style = widgetMenuTextStyle) },
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        ProvideTextStyle(value = widgetMenuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Text(
                                    "Folgende Dateien stehen zum Entpacken bereit. Soll das Entpacken jetzt gestartet werden?",
                                    style = widgetMenuTextStyle,
                                )
                                HorizontalDivider()
                                seaChartPendingZipApprovalFiles.forEachIndexed { index, zipInfo ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "${seaChartProviderDisplayName(zipInfo.provider)}: ${zipInfo.fileName}",
                                            style = widgetMenuTextStyle,
                                        )
                                        Text(
                                            "Größe: ${formatSeaChartBytes(zipInfo.sizeBytes)}",
                                            style = widgetMenuTextStyle,
                                        )
                                        Text(
                                            "Datei: ${zipInfo.filePath}",
                                            style = widgetMenuTextStyle,
                                        )
                                        Text(
                                            "Zielordner: ${zipInfo.destinationPath}",
                                            style = widgetMenuTextStyle,
                                        )
                                    }
                                    if (index < seaChartPendingZipApprovalFiles.lastIndex) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Entpacken",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = {
                            runPendingSeaChartZipImport()
                        },
                    )
                },
                dismissButton = {
                    CompactMenuTextButton(
                        text = "Ablehnen",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = {
                            val declinedCount = seaChartPendingZipApprovalFiles.size
                            seaChartPendingZipApprovalFiles = emptyList()
                            seaChartPendingZipImportDeclined = true
                            seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                isRunning = false,
                                progress = 0f,
                                statusText = "Entpacken abgelehnt. $declinedCount Zip-Datei(en) bleiben unverändert liegen.",
                                completed = false,
                                errorText = null,
                                isUnpacking = false,
                                unpackedBytes = 0L,
                                unpackTotalBytes = -1L,
                            )
                        },
                    )
                },
            )
        }

        val seaChartDownloadErrorState = seaChartDownloadErrorDialog
        if (seaChartDownloadErrorState != null) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color.Black.copy(alpha = 0.9f),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = {
                    Text("Download/Entpack Fehler", style = seaChartWarningDialogTextStyle)
                },
                text = {
                    Text(
                        seaChartDownloadErrorState.message,
                        style = seaChartWarningDialogTextStyle,
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!seaChartDownloadErrorState.filePath.isNullOrBlank()) {
                            CompactMenuTextButton(
                                text = "Datei löschen",
                                style = widgetLinkTextStyle,
                                fillWidth = false,
                                onClick = {
                                    val deleted = seaChartDeleteFileAtPath(seaChartDownloadErrorState.filePath)
                                    seaChartDownloadErrorDialog = null
                                    seaChartDownloadUiState = seaChartDownloadUiState.copy(
                                        isRunning = false,
                                        progress = 0f,
                                        isUnpacking = false,
                                        unpackedBytes = 0L,
                                        unpackTotalBytes = -1L,
                                        statusText = if (deleted) {
                                            "Defekte ZIP-Datei gelöscht."
                                        } else {
                                            "Defekte ZIP-Datei konnte nicht gelöscht werden."
                                        },
                                        completed = false,
                                    )
                                },
                            )
                        }
                        if (
                            seaChartDownloadErrorState.canRetry &&
                            seaChartDownloadErrorState.provider != null &&
                            seaChartDownloadErrorState.regionLabel != null &&
                            seaChartDownloadErrorState.regionUrl != null
                        ) {
                            CompactMenuTextButton(
                                text = "Download wiederholen",
                                style = widgetLinkTextStyle,
                                fillWidth = false,
                                onClick = {
                                    val provider = seaChartDownloadErrorState.provider
                                    val regionLabel = seaChartDownloadErrorState.regionLabel
                                    val regionUrl = seaChartDownloadErrorState.regionUrl
                                    if (provider != null && regionLabel != null && regionUrl != null) {
                                        seaChartDownloadUiState = SeaChartDownloadUiState(
                                            isRunning = true,
                                            statusText = "Wiederhole Download-Prüfung...",
                                            downloadedFileName = seaChartDownloadErrorState.fileName.orEmpty(),
                                            completed = false,
                                            isUnpacking = false,
                                            unpackedBytes = 0L,
                                            unpackTotalBytes = -1L,
                                            errorText = null,
                                        )
                                        seaChartDownloadErrorDialog = null
                                        onShowSeaChartNotice(
                                            "Offlinekarten",
                                            "Download wird erneut geprüft...",
                                        )
                                        requestSeaChartDownload(
                                            regionLabel,
                                            regionUrl,
                                            provider,
                                        )
                                    }
                                },
                            )
                        }
                        CompactMenuTextButton(
                            text = "OK",
                            style = widgetLinkTextStyle,
                            fillWidth = false,
                            onClick = {
                                seaChartDownloadErrorDialog = null
                            },
                        )
                    }
                },
            )
        }

        val minDepthWidgetId = showEchosounderMinDepthDialog
        if (minDepthWidgetId != null) {
            val targetWidget = page.widgets.firstOrNull {
                it.id == minDepthWidgetId && it.kind == WidgetKind.ECHOSOUNDER
            }
            if (targetWidget != null) {
                CompactMenuDialog(
                    onDismissRequest = { showEchosounderMinDepthDialog = null },
                    isDarkMenu = darkBackground,
                    title = {},
                    text = {
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            ProvideTextStyle(value = widgetMenuTextStyle) {
                                Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Mindesttiefe", style = widgetMenuTextStyle)
                                        Text(
                                            if (selectedEchoDepthUnit == EchosounderDepthUnit.METERS) {
                                                "${selectedEchoMinDepth.roundToInt()} m"
                                            } else {
                                                "${(selectedEchoMinDepth * 39.3701f).roundToInt()} inch"
                                            },
                                            style = widgetMenuTextStyle
                                        )
                                    }
                                    Slider(
                                        value = selectedEchoMinDepth,
                                        onValueChange = { selectedEchoMinDepth = it.coerceIn(0.1f, 40f) },
                                        valueRange = 0.1f..40f,
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        CompactMenuTextButton(
                            text = "Speichern",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = {
                                val current = echosounderSettingsByWidget.getOrPut(minDepthWidgetId) {
                                    EchosounderWidgetSettings()
                                }
                                val updated = current.copy(
                                    minDepthMeters = selectedEchoMinDepth.coerceIn(0.1f, 40f),
                                    dynamicChangeRateMps = current.dynamicChangeRateMps,
                                    depthUnit = selectedEchoDepthUnit,
                                    alarmToneIndex = current.alarmToneIndex,
                                )
                                echosounderSettingsByWidget[minDepthWidgetId] = updated
                                onUpdateEchosounderWidgetSettings(
                                    minDepthWidgetId,
                                    serializeEchosounderWidgetSettings(updated),
                                )
                                showEchosounderMinDepthDialog = null
                            }
                        )
                    },
                    dismissButton = {
                        CompactMenuTextButton(
                            text = "Abbrechen",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = { showEchosounderMinDepthDialog = null }
                        )
                    }
                )
            } else {
                showEchosounderMinDepthDialog = null
            }
        }

        val dynamicDialogWidgetId = showEchosounderDynamicDialog
        if (dynamicDialogWidgetId != null) {
            val targetWidget = page.widgets.firstOrNull {
                it.id == dynamicDialogWidgetId && it.kind == WidgetKind.ECHOSOUNDER
            }
            if (targetWidget != null) {
                CompactMenuDialog(
                    onDismissRequest = { showEchosounderDynamicDialog = null },
                    isDarkMenu = darkBackground,
                    title = {},
                    text = {
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            ProvideTextStyle(value = widgetMenuTextStyle) {
                                Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tiefendynamik", style = widgetMenuTextStyle)
                                        Text(
                                            "${selectedEchoDynamicRate.roundToInt()} m/s",
                                            style = widgetMenuTextStyle
                                        )
                                    }
                                    Slider(
                                        value = selectedEchoDynamicRate,
                                        onValueChange = { selectedEchoDynamicRate = it.coerceIn(0f, 5f) },
                                        valueRange = 0f..5f,
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        CompactMenuTextButton(
                            text = "Speichern",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = {
                                val current = echosounderSettingsByWidget.getOrPut(dynamicDialogWidgetId) {
                                    EchosounderWidgetSettings()
                                }
                                val updated = current.copy(
                                    minDepthMeters = current.minDepthMeters,
                                    dynamicChangeRateMps = selectedEchoDynamicRate.coerceIn(0f, 5f),
                                    depthUnit = selectedEchoDepthUnit,
                                    alarmToneIndex = current.alarmToneIndex,
                                )
                                echosounderSettingsByWidget[dynamicDialogWidgetId] = updated
                                onUpdateEchosounderWidgetSettings(
                                    dynamicDialogWidgetId,
                                    serializeEchosounderWidgetSettings(updated),
                                )
                                showEchosounderDynamicDialog = null
                            }
                        )
                    },
                    dismissButton = {
                        CompactMenuTextButton(
                            text = "Abbrechen",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = { showEchosounderDynamicDialog = null }
                        )
                    }
                )
            } else {
                showEchosounderDynamicDialog = null
            }
        }

        val autopilotSettingsWidgetId = autopilotMenuWidgetId
        if (
            autopilotSettingsWidgetId != null &&
            page.widgets.any { it.id == autopilotSettingsWidgetId && it.kind == WidgetKind.AUTOPILOT }
        ) {
            CompactMenuDialog(
                onDismissRequest = { autopilotMenuWidgetId = null },
                isDarkMenu = darkBackground,
                title = {},
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        ProvideTextStyle(value = widgetMenuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Zielgerät Garmin", style = widgetMenuTextStyle)
                                    RadioButton(
                                        selected = selectedAutopilotTargetDevice == AutopilotTargetDevice.GARMIN,
                                        onClick = { selectedAutopilotTargetDevice = AutopilotTargetDevice.GARMIN },
                                        colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                    )
                                }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Zielgerät Raymarine", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedAutopilotTargetDevice == AutopilotTargetDevice.RAYMARINE,
                                            onClick = { selectedAutopilotTargetDevice = AutopilotTargetDevice.RAYMARINE },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Zielgerät Simrad", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedAutopilotTargetDevice == AutopilotTargetDevice.SIMRAD,
                                            onClick = { selectedAutopilotTargetDevice = AutopilotTargetDevice.SIMRAD },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Text(text = autopilotProtocolHint(selectedAutopilotTargetDevice), style = widgetMenuTextStyle)
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gateway SignalK", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedAutopilotGatewayBackend == AutopilotGatewayBackend.SIGNALK_V2,
                                            onClick = {
                                                selectedAutopilotGatewayBackend = AutopilotGatewayBackend.SIGNALK_V2
                                                selectedAutopilotGatewayPort = AutopilotGatewayBackend.SIGNALK_V2.defaultPort.toString()
                                            },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gateway Yacht", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedAutopilotGatewayBackend == AutopilotGatewayBackend.YACHT_DEVICES_0183,
                                            onClick = {
                                                selectedAutopilotGatewayBackend = AutopilotGatewayBackend.YACHT_DEVICES_0183
                                                selectedAutopilotGatewayPort = AutopilotGatewayBackend.YACHT_DEVICES_0183.defaultPort.toString()
                                            },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gateway Actisense", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedAutopilotGatewayBackend == AutopilotGatewayBackend.ACTISENSE_0183,
                                            onClick = {
                                                selectedAutopilotGatewayBackend = AutopilotGatewayBackend.ACTISENSE_0183
                                                selectedAutopilotGatewayPort = AutopilotGatewayBackend.ACTISENSE_0183.defaultPort.toString()
                                            },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gateway Direkt UDP", style = widgetMenuTextStyle)
                                        RadioButton(
                                            selected = selectedAutopilotGatewayBackend == AutopilotGatewayBackend.DIRECT_UDP_JSON,
                                            onClick = {
                                                selectedAutopilotGatewayBackend = AutopilotGatewayBackend.DIRECT_UDP_JSON
                                                selectedAutopilotGatewayPort = AutopilotGatewayBackend.DIRECT_UDP_JSON.defaultPort.toString()
                                            },
                                            colors = SeaFoxDesignTokens.LinkControl.radioButtonColors(),
                                        )
                                    }
                                    Text(text = autopilotGatewayHint(selectedAutopilotGatewayBackend), style = widgetMenuTextStyle)
                                    HorizontalDivider()
                                    Text(
                                        "Safety Gate: Autopilot-Kommandos werden erst nach Opt-in und sichtbarer Bestätigung gesendet.",
                                        style = widgetMenuTextStyle,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Safety Gate armieren", style = widgetMenuTextStyle)
                                        Switch(
                                            checked = selectedAutopilotSafetyGateArmed,
                                            onCheckedChange = { selectedAutopilotSafetyGateArmed = it },
                                            modifier = Modifier.scale(0.5f),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Bestätigungs-Timeout ${selectedAutopilotCommandTimeoutSeconds}s",
                                            style = widgetMenuTextStyle,
                                        )
                                        Slider(
                                            modifier = Modifier.weight(1f),
                                            value = selectedAutopilotCommandTimeoutSeconds.toFloat(),
                                            onValueChange = { value ->
                                                selectedAutopilotCommandTimeoutSeconds = value.roundToInt().coerceIn(1, 30)
                                            },
                                            valueRange = 1f..30f,
                                            steps = 28,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gateway IP / Host", style = widgetMenuTextStyle)
                                        MenuCompactTextField(
                                            value = selectedAutopilotGatewayHost,
                                            onValueChange = { selectedAutopilotGatewayHost = it },
                                            singleLine = true,
                                            textStyle = widgetInputFieldTextStyle,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(0.dp)
                                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                            shape = MENU_INPUT_FIELD_SHAPE
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gateway Port", style = widgetMenuTextStyle)
                                        MenuCompactTextField(
                                            value = selectedAutopilotGatewayPort,
                                            onValueChange = { selectedAutopilotGatewayPort = it.filter { c -> c.isDigit() } },
                                            singleLine = true,
                                            textStyle = widgetInputFieldTextStyle,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(0.dp)
                                                .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                            shape = MENU_INPUT_FIELD_SHAPE
                                        )
                                    }
                                    if (selectedAutopilotGatewayBackend == AutopilotGatewayBackend.SIGNALK_V2) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                            Text("SignalK Autopilot ID", style = widgetMenuTextStyle)
                                            MenuCompactTextField(
                                                value = selectedAutopilotSignalKId,
                                                onValueChange = { selectedAutopilotSignalKId = it },
                                                singleLine = true,
                                                textStyle = widgetInputFieldTextStyle,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(0.dp)
                                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                                shape = MENU_INPUT_FIELD_SHAPE
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("SignalK Token", style = widgetMenuTextStyle)
                                            MenuCompactTextField(
                                                value = selectedAutopilotAuthToken,
                                                onValueChange = { selectedAutopilotAuthToken = it },
                                                singleLine = true,
                                                textStyle = widgetInputFieldTextStyle,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(0.dp)
                                                    .heightIn(min = MENU_INPUT_FIELD_HEIGHT),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

                                            colors = menuOutlinedTextFieldColors(darkBackground),
                                                shape = MENU_INPUT_FIELD_SHAPE
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        CompactMenuTextButton(
                            text = "Speichern",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = {
                                val parsedPort = selectedAutopilotGatewayPort.toIntOrNull()
                                    ?: selectedAutopilotGatewayBackend.defaultPort
                                val updatedAutopilotSettings = AutopilotWidgetSettings(
                                    targetDevice = selectedAutopilotTargetDevice,
                                    gatewayBackend = selectedAutopilotGatewayBackend,
                                    gatewayHost = selectedAutopilotGatewayHost.trim().ifBlank { "255.255.255.255" },
                                    gatewayPort = parsedPort.coerceIn(1, 65535),
                                    signalKAutopilotId = selectedAutopilotSignalKId.trim().ifBlank { "_default" },
                                    rudderAverageSeconds = selectedAutopilotRudderAverageSeconds,
                                    safetyGateArmed = selectedAutopilotSafetyGateArmed,
                                    commandTimeoutMs = selectedAutopilotCommandTimeoutSeconds.coerceIn(1, 30) * 1000L,
                                    authToken = selectedAutopilotAuthToken.trim(),
                                )
                                autopilotSettingsByWidget[autopilotSettingsWidgetId] = updatedAutopilotSettings
                                onUpdateAutopilotWidgetSettings(
                                    autopilotSettingsWidgetId,
                                    serializeAutopilotWidgetSettings(updatedAutopilotSettings),
                                )
                                autopilotMenuWidgetId = null
                            }
                        )
                    },
                    dismissButton = {
                        CompactMenuTextButton(
                            text = "Abbrechen",
                            style = widgetMenuTextStyle,
                            fillWidth = false,
                            onClick = { autopilotMenuWidgetId = null }
                        )
                    }
                )
            }

        val autopilotDispatch = pendingAutopilotDispatch
        if (autopilotDispatch != null) {
            CompactMenuDialog(
                onDismissRequest = { pendingAutopilotDispatch = null },
                isDarkMenu = darkBackground,
                title = { Text("Autopilot-Befehl bestätigen", style = widgetMenuTextStyle) },
                text = {
                    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                        ProvideTextStyle(value = widgetMenuTextStyle) {
                            Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                Text(
                                    "Modus: ${autopilotDispatch.command.mode.label} · Ziel: ${autopilotDispatch.command.targetDevice.label}",
                                    style = widgetMenuTextStyle,
                                )
                                Text(
                                    "Gateway: ${autopilotDispatch.backend.label} auf ${autopilotDispatch.host}:${autopilotDispatch.port}",
                                    style = widgetMenuTextStyle,
                                )
                                Text(
                                    "Safety Gate ist armiert. Sende nur, wenn Crew, Umgebung und Autopilot-Gateway bewusst geprüft sind.",
                                    style = widgetMenuTextStyle.copy(color = Color(0xFFFFC857)),
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Senden",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = {
                            onSendAutopilotCommand(
                                autopilotDispatch.copy(confirmedAtMs = System.currentTimeMillis()),
                            )
                            pendingAutopilotDispatch = null
                        }
                    )
                },
                dismissButton = {
                    CompactMenuTextButton(
                        text = "Abbrechen",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = { pendingAutopilotDispatch = null }
                    )
                }
            )
        }

        val helpWidgetId = widgetHelpWidgetId
        if (helpWidgetId != null) {
            val helpWidget = page.widgets.firstOrNull { it.id == helpWidgetId }
            if (helpWidget != null) {
                CompactMenuDialog(
                    onDismissRequest = { widgetHelpWidgetId = null },
                    isDarkMenu = darkBackground,
                    // Keinen Titel-Block in Widget-Hilfe anzeigen.
                    title = {},
                    text = {
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            ProvideTextStyle(value = widgetMenuTextStyle) {
                                Column(verticalArrangement = Arrangement.spacedBy(MENU_SPACING)) {
                                    widgetHelpLines(helpWidget.kind).forEach { line ->
                                        Text("• $line", style = widgetMenuTextStyle)
                                    }
                                }
                            }
                        }
                    },
                confirmButton = {
                    CompactMenuTextButton(
                        text = "Schließen",
                        style = widgetMenuTextStyle,
                        fillWidth = false,
                        onClick = { widgetHelpWidgetId = null }
                    )
                    }
                )
            } else {
                widgetHelpWidgetId = null
            }
        }
    }
}

private data class LogSpeedSample(
    val timestampMs: Long,
    val speedKn: Float,
)

private data class AnchorWatchRuntimeState(
    val isSignalDetected: Boolean = false,
    val isAnchorSet: Boolean = false,
    val wasAlarm: Boolean = false,
    val anchorLatitude: Float? = null,
    val anchorLongitude: Float? = null,
    val lastKnownChainLengthMeters: Float? = null,
    val lastAnchorSignalValue: Float? = null,
    val chainLengthResetOffsetMeters: Float = 0f,
    val lastAnchorChainSource: String? = null,
    val lastChainLengthWhenSet: Float? = null,
)

private data class WindSpeedSample(
    val timestampMs: Long,
    val speedKn: Float,
)

private fun computeDistanceFromSamples(samples: List<LogSpeedSample>): Float {
    if (samples.size < 2) return 0f
    var distance = 0f
    for (i in 1 until samples.size) {
        val previous = samples[i - 1]
        val current = samples[i]
        val dtHours = (current.timestampMs - previous.timestampMs).toFloat() / 3_600_000f
        if (dtHours > 0f) {
            distance += ((previous.speedKn + current.speedKn) / 2f) * dtHours
        }
    }
    return distance
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DraggableWidgetCard(
    title: String,
    titleScale: Float,
    gridStepPx: Float,
    widgetFrameStyle: WidgetFrameStyle,
    widgetFrameStyleGrayOffset: Int,
    darkBackground: Boolean,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    onMove: (Float, Float, Boolean) -> Unit,
    onResize: (Float, Float, Boolean) -> Unit,
    canInteract: Boolean,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    onSnap: () -> Unit,
    onClose: () -> Unit,
    isAlarmActive: Boolean = false,
    isAlarmMuted: Boolean = false,
    onMuteAlarm: (() -> Unit)? = null,
    hasSubmenu: Boolean = false,
    onOpenSubmenu: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { width.toDp() }
    val heightDp = with(density) { height.toDp() }
    val frameOuterPadding = 1.dp
    val opacityOffset = widgetFrameStyleGrayOffset * 0.012f
    val baseSurface = if (darkBackground) {
        SeaFoxDesignTokens.Color.surfaceDark
    } else {
        SeaFoxDesignTokens.Color.surfaceLight
    }
    val borderPanelAlpha = ((if (darkBackground) 0.52f else 0.78f) + opacityOffset).coerceIn(0.30f, 1f)
    val filledPanelAlpha = ((if (darkBackground) 0.88f else 0.96f) + opacityOffset).coerceIn(0.45f, 1f)
    val frameStyleBackground = when (widgetFrameStyle) {
        WidgetFrameStyle.BORDER -> baseSurface.copy(alpha = borderPanelAlpha)
        WidgetFrameStyle.BACKGROUND -> baseSurface.copy(alpha = filledPanelAlpha)
    }
    val widgetContentColor = if (darkBackground) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val frameToTitleGap = 1.5.dp
    val headerHeightPx = with(density) { 40f.toDp() }
    val headerHamburgerSize = headerHeightPx * 0.7f
    val headerHamburgerWidth = headerHamburgerSize
    val headerHamburgerBottomOffset = with(density) { 0f.toDp() }
    val headerHamburgerLeftOffset = with(density) { 10.sp.toDp() }
    val titleToGraphicGap = frameToTitleGap
    val defaultMoveHandleRatio = 0.5f
    val resizeHandleSize = 84.dp
    val scope = rememberCoroutineScope()
    val longPressActivateMs = 2000L
    val dragSpeedMultiplier = 1f
    val resizeHandlesTimeoutMs = 4000L
    val liveWidgetX = rememberUpdatedState(x)
    val liveWidgetY = rememberUpdatedState(y)
    var editMode by rememberSaveable { mutableStateOf(false) }
    var transformMode by rememberSaveable { mutableStateOf(false) }
    var inactivityResetJob by remember { mutableStateOf<Job?>(null) }
    var resetDeadlineMs by remember { mutableLongStateOf(0L) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isResizing by remember { mutableStateOf(false) }
    var topLeftCarryX by remember { mutableFloatStateOf(0f) }
    var topLeftCarryY by remember { mutableFloatStateOf(0f) }
    var topRightCarryX by remember { mutableFloatStateOf(0f) }
    var topRightCarryY by remember { mutableFloatStateOf(0f) }
    var bottomRightCarryX by remember { mutableFloatStateOf(0f) }
    var bottomRightCarryY by remember { mutableFloatStateOf(0f) }
    var bottomLeftCarryX by remember { mutableFloatStateOf(0f) }
    var bottomLeftCarryY by remember { mutableFloatStateOf(0f) }

    fun consumeGridSteps(
        carryX: Float,
        carryY: Float,
        step: Float,
    ): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        var localCarryX = carryX
        var localCarryY = carryY
        var outX = 0f
        var outY = 0f

        while (abs(localCarryX) >= step) {
            val oneStep = if (localCarryX > 0f) step else -step
            outX += oneStep
            localCarryX -= oneStep
        }
        while (abs(localCarryY) >= step) {
            val oneStep = if (localCarryY > 0f) step else -step
            outY += oneStep
            localCarryY -= oneStep
        }
        return (outX to outY) to (localCarryX to localCarryY)
    }

    fun scheduleResetToStandard() {
        Log.d(TAG_WIDGET_INTERACTION, "scheduleResetToStandard: title=$title, timeoutMs=$resizeHandlesTimeoutMs")
        inactivityResetJob?.cancel()
        resetDeadlineMs = System.currentTimeMillis() + resizeHandlesTimeoutMs
        inactivityResetJob = scope.launch {
            delay(resizeHandlesTimeoutMs)
            Log.d(TAG_WIDGET_INTERACTION, "widgetAutoReset: title=$title")
            editMode = false
            transformMode = false
            countdownSeconds = 0
            onSnap()
        }
    }

    fun activateEditMode() {
        Log.d(TAG_WIDGET_INTERACTION, "activateEditMode: title=$title")
        if (!editMode) editMode = true
        transformMode = true
        scheduleResetToStandard()
    }

    LaunchedEffect(editMode, resetDeadlineMs) {
        if (!editMode) {
            transformMode = false
            countdownSeconds = 0
            return@LaunchedEffect
        }

        while (editMode) {
            val remainingMs = (resetDeadlineMs - System.currentTimeMillis()).coerceAtLeast(0L)
            countdownSeconds = ((remainingMs + 999L) / 1000L).toInt()
            if (remainingMs <= 0L) break
            delay(100L)
        }
    }

    val isAlarmBorderActive = isAlarmActive
    val frameBorderColor = when {
        isAlarmBorderActive -> SeaFoxDesignTokens.Color.coral
        transformMode -> SeaFoxDesignTokens.Color.brass
        darkBackground -> SeaFoxDesignTokens.Color.hairlineDark
        else -> SeaFoxDesignTokens.Color.hairlineLight
    }
    val frameBorderWidth = if (transformMode || isAlarmBorderActive) 1.5.dp else 1.dp
    Box(
        modifier = Modifier
            .offset { IntOffset(x.toInt(), y.toInt()) }
            .size(widthDp, heightDp)
            .padding(frameOuterPadding)
            .border(
                width = frameBorderWidth,
                color = frameBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        val widgetContentWidth = (widthDp - frameOuterPadding * 2).coerceAtLeast(0.dp)
        val widgetContentHeight = (heightDp - frameOuterPadding * 2).coerceAtLeast(0.dp)
        val widgetWidthPx = with(density) { widgetContentWidth.toPx() }
        val widgetHeightPx = with(density) { widgetContentHeight.toPx() }
        val moveHandleSizePx = kotlin.math.min(widgetWidthPx, widgetHeightPx) * defaultMoveHandleRatio
        val moveHandleRadiusPx = moveHandleSizePx / 2f
        val moveHandleSizeDp = with(density) { moveHandleSizePx.toDp() }
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(frameToTitleGap)
                .pointerInput(gridStepPx, canInteract) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!canInteract) {
                            return@awaitEachGesture
                        }

                        val startX = down.position.x
                        val startY = down.position.y
                        val halfWidgetWidth = widgetWidthPx / 2f
                        val halfWidgetHeight = widgetHeightPx / 2f
                        val centerHandleActive = kotlin.math.abs(startX - halfWidgetWidth) <= moveHandleRadiusPx &&
                            kotlin.math.abs(startY - halfWidgetHeight) <= moveHandleRadiusPx
                        if (!centerHandleActive) {
                            return@awaitEachGesture
                        }

                        val startAbsX = liveWidgetX.value + startX
                        val startAbsY = liveWidgetY.value + startY
                        val startAbs = androidx.compose.ui.geometry.Offset(startAbsX, startAbsY)
                        var hadMovementBeyondTolerance = false
                        var moveActive = false
                        var lastAbs = startAbs
                        val nowMs = System.currentTimeMillis()
                        val quickReengage = editMode && nowMs <= resetDeadlineMs
                        if (quickReengage) {
                            moveActive = true
                            down.consume()
                            onInteractionStart()
                            activateEditMode()
                        }
                        val longPressActivationJob = scope.launch {
                            delay(longPressActivateMs)
                            if (!hadMovementBeyondTolerance && !moveActive) {
                                Log.d(
                                    TAG_WIDGET_INTERACTION,
                                    "longPressActivatedMove: title=$title, elapsedMs=$longPressActivateMs"
                                )
                                moveActive = true
                                down.consume()
                                activateEditMode()
                                onInteractionStart()
                            }
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: break
                                if (!change.pressed) break
                                if (isResizing) continue

                                if (!moveActive) {
                                    val currentAbsX = liveWidgetX.value + change.position.x
                                    val currentAbsY = liveWidgetY.value + change.position.y
                                    val currentAbs = androidx.compose.ui.geometry.Offset(currentAbsX, currentAbsY)
                                    val dx = currentAbs.x - startAbs.x
                                    val dy = currentAbs.y - startAbs.y
                                    val moved = (dx * dx + dy * dy) > (14f * 14f)
                                    if (moved) {
                                        hadMovementBeyondTolerance = true
                                        longPressActivationJob.cancel()
                                        break
                                    }
                                    continue
                                }

                                val currentAbsX = liveWidgetX.value + change.position.x
                                val currentAbsY = liveWidgetY.value + change.position.y
                                val currentAbs = androidx.compose.ui.geometry.Offset(currentAbsX, currentAbsY)
                                val dragX = (currentAbs.x - lastAbs.x) * dragSpeedMultiplier
                                val dragY = (currentAbs.y - lastAbs.y) * dragSpeedMultiplier
                                lastAbs = currentAbs

                                if (dragX != 0f || dragY != 0f) {
                                    onMove(dragX, dragY, false)
                                    Log.d(
                                        TAG_WIDGET_INTERACTION,
                                        "moveGesture: title=$title, dx=$dragX, dy=$dragY"
                                    )
                                }
                                scheduleResetToStandard()
                                if (change.pressed) {
                                    change.consume()
                                }
                            }
                        } finally {
                            longPressActivationJob.cancel()
                        }
                        if (moveActive) {
                            onSnap()
                            activateEditMode()
                            onInteractionEnd()
                        } else {
                            onInteractionEnd()
                        }
                    }
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = frameStyleBackground,
                contentColor = widgetContentColor,
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                val showMuteControl = onMuteAlarm != null && isAlarmActive
                val headerBrush = when {
                    isAlarmActive -> Brush.horizontalGradient(
                        listOf(
                            SeaFoxDesignTokens.Color.coral.copy(alpha = 0.78f),
                            SeaFoxDesignTokens.Color.coral.copy(alpha = 0.38f),
                        )
                    )
                    darkBackground -> Brush.horizontalGradient(
                        listOf(
                            SeaFoxDesignTokens.Color.panelHeaderDark,
                            SeaFoxDesignTokens.Color.graphiteRaised.copy(alpha = 0.34f),
                        )
                    )
                    else -> Brush.horizontalGradient(
                        listOf(
                            SeaFoxDesignTokens.Color.panelHeaderLight,
                            Color.White.copy(alpha = 0.68f),
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .height(headerHeightPx)
                        .padding(start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * titleScale - 4f).sp,
                            lineHeight = (MaterialTheme.typography.titleSmall.lineHeight.value * titleScale - 4f).sp,
                        ),
                        color = widgetContentColor,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                        if (showMuteControl) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                val muteButtonText = if (isAlarmMuted) "UNMUTE" else "MUTE"
                                TextButton(
                                    onClick = {
                                        onMuteAlarm?.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = nonMenuTextButtonColors(),
                                    modifier = Modifier.height(24.dp),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = muteButtonText,
                                        color = NON_MENU_BUTTON_COLOR,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = (MaterialTheme.typography.labelSmall.fontSize.value + 2f).sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasSubmenu && onOpenSubmenu != null) {
                                Box(
                                    modifier = Modifier
                                        .size(headerHamburgerWidth, headerHamburgerSize)
                                        .align(Alignment.Bottom)
                                        .offset(
                                            x = -headerHamburgerLeftOffset,
                                            y = headerHamburgerBottomOffset,
                                        )
                                        .clickable {
                                            onOpenSubmenu()
                                            scheduleResetToStandard()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    HamburgerMenuIcon(
                                        color = widgetContentColor.copy(alpha = 0.92f),
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 2.5.dp,
                                    )
                                }
                            }
                        }
                    }
                Spacer(modifier = Modifier.height(titleToGraphicGap))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                ) {
                    val dataTextScale = titleScale.coerceAtLeast(0.5f)
                    val dataTextStyle = MaterialTheme.typography.titleSmall.copy(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize * dataTextScale,
                        lineHeight = MaterialTheme.typography.titleSmall.lineHeight * dataTextScale,
                        fontWeight = FontWeight.Medium,
                        color = widgetContentColor,
                    )
                    ProvideTextStyle(value = dataTextStyle) {
                        content()
                    }
                }
            }
        }

        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(30.dp)
                    .background(SeaFoxDesignTokens.Color.coral, CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("X", color = Color.White)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(moveHandleSizeDp)
                    .background(SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${countdownSeconds}s",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 0.dp, top = 0.dp)
                    .size(resizeHandleSize)
                .pointerInput(canInteract) {
                    if (!canInteract) return@pointerInput
                    detectDragGestures(
                            onDragStart = {
                                topLeftCarryX = 0f
                                topLeftCarryY = 0f
                                isResizing = true
                                onInteractionStart()
                                scheduleResetToStandard()
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val step = gridStepPx.coerceAtLeast(1f)
                                val (outDelta, carries) = consumeGridSteps(
                                    carryX = topLeftCarryX + drag.x,
                                    carryY = topLeftCarryY + drag.y,
                                    step = step
                                )
                                topLeftCarryX = carries.first
                                topLeftCarryY = carries.second
                                if (outDelta.first != 0f || outDelta.second != 0f) {
                                    onMove(outDelta.first, outDelta.second, false)
                                    onResize(-outDelta.first, -outDelta.second, false)
                                    Log.d(
                                        TAG_WIDGET_INTERACTION,
                                        "resizeTopLeft: title=$title, dx=${outDelta.first}, dy=${outDelta.second}"
                                    )
                                }
                                scheduleResetToStandard()
                            },
                            onDragEnd = {
                                topLeftCarryX = 0f
                                topLeftCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            },
                            onDragCancel = {
                                topLeftCarryX = 0f
                                topLeftCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = size.width * 0.45f
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(sizePx, 0f)
                            lineTo(0f, sizePx)
                            close()
                        },
                        color = SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.28f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 0.dp, top = 0.dp)
                    .size(resizeHandleSize)
                .pointerInput(canInteract) {
                    if (!canInteract) return@pointerInput
                    detectDragGestures(
                            onDragStart = {
                                topRightCarryX = 0f
                                topRightCarryY = 0f
                                isResizing = true
                                onInteractionStart()
                                scheduleResetToStandard()
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val step = gridStepPx.coerceAtLeast(1f)
                                val (outDelta, carries) = consumeGridSteps(
                                    carryX = topRightCarryX + drag.x,
                                    carryY = topRightCarryY + drag.y,
                                    step = step
                                )
                                topRightCarryX = carries.first
                                topRightCarryY = carries.second
                                if (outDelta.first != 0f || outDelta.second != 0f) {
                                    onMove(0f, outDelta.second, false)
                                    onResize(outDelta.first, -outDelta.second, false)
                                    Log.d(
                                        TAG_WIDGET_INTERACTION,
                                        "resizeTopRight: title=$title, dx=${outDelta.first}, dy=${outDelta.second}"
                                    )
                                }
                                scheduleResetToStandard()
                            },
                            onDragEnd = {
                                topRightCarryX = 0f
                                topRightCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            },
                            onDragCancel = {
                                topRightCarryX = 0f
                                topRightCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = size.width * 0.45f
                    drawPath(
                        path = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width - sizePx, 0f)
                            lineTo(size.width, sizePx)
                            close()
                        },
                        color = SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.28f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 0.dp, bottom = 0.dp)
                    .size(resizeHandleSize)
                .pointerInput(canInteract) {
                    if (!canInteract) return@pointerInput
                    detectDragGestures(
                            onDragStart = {
                                bottomLeftCarryX = 0f
                                bottomLeftCarryY = 0f
                                isResizing = true
                                onInteractionStart()
                                scheduleResetToStandard()
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val step = gridStepPx.coerceAtLeast(1f)
                                val (outDelta, carries) = consumeGridSteps(
                                    carryX = bottomLeftCarryX + drag.x,
                                    carryY = bottomLeftCarryY + drag.y,
                                    step = step
                                )
                                bottomLeftCarryX = carries.first
                                bottomLeftCarryY = carries.second
                                if (outDelta.first != 0f || outDelta.second != 0f) {
                                    onMove(outDelta.first, 0f, false)
                                    onResize(-outDelta.first, outDelta.second, false)
                                    Log.d(
                                        TAG_WIDGET_INTERACTION,
                                        "resizeBottomLeft: title=$title, dx=${outDelta.first}, dy=${outDelta.second}"
                                    )
                                }
                                scheduleResetToStandard()
                            },
                            onDragEnd = {
                                bottomLeftCarryX = 0f
                                bottomLeftCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            },
                            onDragCancel = {
                                bottomLeftCarryX = 0f
                                bottomLeftCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = size.width * 0.45f
                    val triangle = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(sizePx, size.height)
                        lineTo(0f, size.height - sizePx)
                        close()
                    }
                    drawPath(path = triangle, color = SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.28f))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 0.dp)
                    .size(resizeHandleSize)
                .pointerInput(canInteract) {
                    if (!canInteract) return@pointerInput
                    detectDragGestures(
                            onDragStart = {
                                bottomRightCarryX = 0f
                                bottomRightCarryY = 0f
                                isResizing = true
                                onInteractionStart()
                                scheduleResetToStandard()
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val step = gridStepPx.coerceAtLeast(1f)
                                val (outDelta, carries) = consumeGridSteps(
                                    carryX = bottomRightCarryX + drag.x,
                                    carryY = bottomRightCarryY + drag.y,
                                    step = step
                                )
                                bottomRightCarryX = carries.first
                                bottomRightCarryY = carries.second
                                if (outDelta.first != 0f || outDelta.second != 0f) {
                                    onResize(outDelta.first, outDelta.second, false)
                                    Log.d(
                                        TAG_WIDGET_INTERACTION,
                                        "resizeBottomRight: title=$title, dx=${outDelta.first}, dy=${outDelta.second}"
                                    )
                                }
                                scheduleResetToStandard()
                            },
                            onDragEnd = {
                                bottomRightCarryX = 0f
                                bottomRightCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            },
                            onDragCancel = {
                                bottomRightCarryX = 0f
                                bottomRightCarryY = 0f
                                isResizing = false
                                onInteractionEnd()
                                onSnap()
                                scheduleResetToStandard()
                            }
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = size.width * 0.45f
                    val triangle = Path().apply {
                        moveTo(size.width, size.height)
                        lineTo(size.width - sizePx, size.height)
                        lineTo(size.width, size.height - sizePx)
                        close()
                    }
                    drawPath(path = triangle, color = SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.28f))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onCreatePage: () -> Unit,
    darkBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = if (darkBackground) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val mutedColor = if (darkBackground) SeaFoxDesignTokens.Color.mutedDark else SeaFoxDesignTokens.Color.mutedLight
    Box(modifier = modifier) {
        PremiumDashboardBackdrop(
            darkBackground = darkBackground,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(132.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.38f
                drawCircle(
                    color = SeaFoxDesignTokens.Color.cyan.copy(alpha = if (darkBackground) 0.16f else 0.10f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.4.dp.toPx()),
                )
                drawLine(
                    color = SeaFoxDesignTokens.Color.brass.copy(alpha = 0.80f),
                    start = Offset(center.x, center.y - radius * 0.72f),
                    end = Offset(center.x, center.y + radius * 0.72f),
                    strokeWidth = 2.dp.toPx(),
                )
                drawLine(
                    color = SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.75f),
                    start = Offset(center.x - radius * 0.72f, center.y),
                    end = Offset(center.x + radius * 0.72f, center.y),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Noch keine Seite vorhanden",
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Lege eine Navigationsseite an und platziere deine Instrumente.",
                color = mutedColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(18.dp))
            ElevatedButton(
                onClick = onCreatePage,
                shape = RoundedCornerShape(8.dp),
                colors = nonMenuElevatedButtonColors(),
            ) {
                Text("Erste Seite erstellen", color = NON_MENU_BUTTON_COLOR)
            }
        }
    }
}

private val OrbitronFamily = FontFamily(Font(R.font.orbitron_variable))
private val PtMonoFamily = FontFamily(Font(R.font.dot_gothic16_regular))
private val ElectrolizeFamily = FontFamily(Font(R.font.electrolize_regular))
private val DotGothicFamily = FontFamily(Font(R.font.dot_gothic16_regular))

@Composable
private fun HamburgerMenuIcon(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier) {
        val pxStroke = strokeWidth.toPx()
        val halfStroke = pxStroke / 2f
        val width = size.width
        val height = size.height
        val insetX = maxOf(2.dp.toPx(), width * 0.1f)
        val insetY = maxOf(2.dp.toPx(), height * 0.12f)
        val availableHeight = (height - (insetY * 2f) - pxStroke * 3f)
        val spacing = if (availableHeight <= 0f) 0f else availableHeight / 2f
        val yTop = insetY + halfStroke
        val yMiddle = yTop + pxStroke + spacing
        val yBottom = yMiddle + pxStroke + spacing

        drawLine(color, Offset(insetX, yTop), Offset(width - insetX, yTop), pxStroke)
        drawLine(color, Offset(insetX, yMiddle), Offset(width - insetX, yMiddle), pxStroke)
        drawLine(color, Offset(insetX, yBottom), Offset(width - insetX, yBottom), pxStroke)
    }
}

private fun createModernTypography(uiFont: UiFont): Typography {
    val base = Typography()
    val scale = 1.0f
    val letterSpacingScale = when (uiFont) {
        UiFont.ORBITRON -> 2f
        UiFont.DOT_GOTHIC -> 1.4f
        else -> 1.2f
    }
    val family = when (uiFont) {
        UiFont.FUTURA -> FontFamily.SansSerif
        UiFont.ORBITRON -> OrbitronFamily
        UiFont.PT_MONO -> PtMonoFamily
        UiFont.ELECTROLIZE -> ElectrolizeFamily
        UiFont.DOT_GOTHIC -> DotGothicFamily
    }

    return base.copy(
        displayLarge = base.displayLarge.withFontSettings(
            family = family,
            scale = scale,
            weight = base.displayLarge.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        displayMedium = base.displayMedium.withFontSettings(
            family = family,
            scale = scale,
            weight = base.displayMedium.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        displaySmall = base.displaySmall.withFontSettings(
            family = family,
            scale = scale,
            weight = base.displaySmall.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        headlineLarge = base.headlineLarge.withFontSettings(
            family = family,
            scale = scale,
            weight = base.headlineLarge.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        headlineSmall = base.headlineSmall.withFontSettings(
            family = family,
            scale = scale,
            weight = FontWeight.SemiBold,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        titleLarge = base.titleLarge.withFontSettings(
            family = family,
            scale = scale,
            weight = base.titleLarge.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        titleMedium = base.titleMedium.withFontSettings(
            family = family,
            scale = scale,
            weight = FontWeight.SemiBold,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        titleSmall = base.titleSmall.withFontSettings(
            family = family,
            scale = scale,
            weight = FontWeight.Medium,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        bodyMedium = base.bodyMedium.withFontSettings(
            family = family,
            scale = scale,
            weight = base.bodyMedium.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        bodyLarge = base.bodyLarge.withFontSettings(
            family = family,
            scale = scale,
            weight = base.bodyLarge.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        bodySmall = base.bodySmall.withFontSettings(
            family = family,
            scale = scale,
            weight = base.bodySmall.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        labelLarge = base.labelLarge.withFontSettings(
            family = family,
            scale = scale,
            weight = FontWeight.Medium,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        labelMedium = base.labelMedium.withFontSettings(
            family = family,
            scale = scale,
            weight = base.labelMedium.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
        labelSmall = base.labelSmall.withFontSettings(
            family = family,
            scale = scale,
            weight = base.labelSmall.fontWeight,
            letterSpacingScale = letterSpacingScale,
            forceSpecifiedLetterSpacing = uiFont == UiFont.ORBITRON,
        ),
    )
}

private fun androidx.compose.ui.text.TextStyle.withFontSettings(
    family: FontFamily,
    scale: Float,
    weight: FontWeight?,
    letterSpacingScale: Float = 1.2f,
    forceSpecifiedLetterSpacing: Boolean = false,
): androidx.compose.ui.text.TextStyle {
    val scaledFontSize = fontSize * scale
    val scaledLineHeight = lineHeight * scale
    val shouldForceSpacing = forceSpecifiedLetterSpacing && (letterSpacing == TextUnit.Unspecified || letterSpacing.value == 0f)
    val resolvedLetterSpacing = if (!shouldForceSpacing) {
        letterSpacing * letterSpacingScale
    } else {
        // Orbitron wird über einen relativen Faktor skaliert, falls die Vorgabe-Typografie
        // keine explizite Buchstabenabstandsangabe enthält.
        (scaledFontSize * 0.03f) * letterSpacingScale
    }

    return copy(
        fontFamily = family,
        fontWeight = weight ?: fontWeight,
        fontSize = scaledFontSize,
        lineHeight = scaledLineHeight,
        letterSpacing = resolvedLetterSpacing,
    )
}

private fun wrap360Ui(value: Float): Float {
    var wrapped = value % 360f
    if (wrapped < 0f) wrapped += 360f
    return wrapped
}

private fun applySystemAlarmPresetVolume(context: Context, preset: Float) {
    val clampedPreset = preset.coerceIn(0f, 1f)
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val streams = intArrayOf(
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_MUSIC,
    )

    for (streamType in streams) {
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        if (maxVolume <= 0) continue
        val targetIndex = (clampedPreset * maxVolume).roundToInt().coerceIn(0, maxVolume)
        try {
            audioManager.setStreamVolume(streamType, targetIndex, 0)
            return
        } catch (_: Exception) {
            continue
        }
    }
}

private fun calculateTrueWindAngleDeg(
    apparentSpeed: Float,
    apparentAngleDeg: Float,
    boatSpeedKn: Float,
): Float {
    if (!apparentSpeed.isFinite() || !apparentAngleDeg.isFinite() || !boatSpeedKn.isFinite()) {
        return Float.NaN
    }

    val relativeAngle = normalizeSignedAngleUi(apparentAngleDeg) * PI.toDouble() / 180.0
    val apparentX = apparentSpeed.toDouble() * sin(relativeAngle)
    val apparentY = apparentSpeed.toDouble() * cos(relativeAngle)
    val trueY = apparentY + boatSpeedKn.toDouble()
    val trueYClamped = trueY
    if (!trueYClamped.isFinite()) return Float.NaN
    val trueAngle = atan2(apparentX, trueYClamped) * 180.0 / PI
    return normalizeSignedAngleUi(trueAngle.toFloat())
}

private fun calculateTrueWindSpeedKn(
    apparentSpeed: Float,
    apparentAngleDeg: Float,
    boatSpeedKn: Float,
): Float {
    if (!apparentSpeed.isFinite() || !apparentAngleDeg.isFinite() || !boatSpeedKn.isFinite()) {
        return Float.NaN
    }

    val relativeAngle = normalizeSignedAngleUi(apparentAngleDeg) * PI.toDouble() / 180.0
    val apparentX = apparentSpeed.toDouble() * sin(relativeAngle)
    val apparentY = apparentSpeed.toDouble() * cos(relativeAngle)
    val trueY = apparentY + boatSpeedKn.toDouble()
    return hypot(apparentX, trueY).toFloat()
}

private fun calculateApparentWindFromTrue(
    trueSpeed: Float,
    trueAngleRelativeDeg: Float,
    boatSpeedKn: Float,
): Pair<Float, Float>? {
    if (!trueSpeed.isFinite() || !trueAngleRelativeDeg.isFinite() || !boatSpeedKn.isFinite()) {
        return null
    }

    val relativeAngle = normalizeSignedAngleUi(trueAngleRelativeDeg) * PI.toDouble() / 180.0
    val trueX = trueSpeed.toDouble() * sin(relativeAngle)
    val trueY = trueSpeed.toDouble() * cos(relativeAngle)
    val apparentY = trueY - boatSpeedKn.toDouble()
    val apparentSpeed = hypot(trueX, apparentY).coerceAtLeast(0.0)
    if (!apparentSpeed.isFinite()) return null
    val apparentAngle = normalizeSignedAngleUi(((atan2(trueX, apparentY) * 180.0) / PI.toDouble()).toFloat())
    return apparentSpeed.toFloat() to apparentAngle
}

private fun normalizeSignedAngleUi(value: Float): Float {
    val wrapped = wrap360Ui(value)
    return if (wrapped > 180f) wrapped - 360f else wrapped
}

private fun looksLikeAnchorWatchSignalCountSource(key: String): Boolean {
    return key.contains("signal")
            || key.contains("count")
            || key.contains("puls")
            || key.contains("cnt")
            || key.contains("winch")
}

private fun anchorWatchChainSignalLengthToMeters(
    valuePerSignalMm: Float,
): Float {
    return valuePerSignalMm.coerceAtLeast(0.1f) / 1000f
}

private fun pickAnchorChainLengthFromTelemetry(telemetry: Map<String, Float>): AnchorWatchTelemetry? {
    val candidates = linkedMapOf<String, Pair<Float, Boolean>>()

    for ((key, value) in telemetry) {
        val lower = key.lowercase()
        if (!value.isFinite() || value < 0f) continue

        val isAnchorChainKey = lower == "anchor_chain_length" ||
                lower == "anchor_chain" ||
                lower == "chain_length" ||
                lower == "anchor_line_length" ||
                lower.contains("anchor") && lower.contains("chain")

        if (isAnchorChainKey) {
            val looksLikeSignal = looksLikeAnchorWatchSignalCountSource(lower)
            val isFeet = lower.endsWith("_ft") || lower.contains("feet") || lower.contains("_ft_")
            val normalizedMeters = if (looksLikeSignal) {
                value
            } else if (isFeet) {
                value * 0.3048f
            } else {
                value
            }
            candidates[lower] = normalizedMeters to looksLikeSignal
        }
    }

    if (candidates.isEmpty()) return null

    for ((source, candidate) in candidates) {
        val value = candidate.first
        val isSignalCount = candidate.second
        if (value >= 0f) {
                return AnchorWatchTelemetry(
                lengthMeters = value,
                rawValue = value,
                sourceKey = source,
                isSignalCount = isSignalCount,
            )
        }
    }

    val fallback = candidates.entries.firstOrNull { it.value.first >= 0f } ?: return null
    return AnchorWatchTelemetry(
        lengthMeters = fallback.value.first,
        rawValue = fallback.value.first,
        sourceKey = fallback.key,
        isSignalCount = fallback.value.second,
    )
}

private fun computeDistanceMeters(lat1: Float, lon1: Float, lat2: Float, lon2: Float): Float {
    val earthRadiusMeters = 6_371_000.0
    val lat1Rad = Math.toRadians(lat1.toDouble())
    val lat2Rad = Math.toRadians(lat2.toDouble())
    val dLat = Math.toRadians((lat2 - lat1).toDouble())
    val dLon = Math.toRadians((lon2 - lon1).toDouble())

    val a = kotlin.math.sin(dLat / 2.0) * kotlin.math.sin(dLat / 2.0) +
        kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
        kotlin.math.sin(dLon / 2.0) * kotlin.math.sin(dLon / 2.0)

    val clampedA = a.coerceIn(0.0, 1.0)
    val c = 2.0 * kotlin.math.asin(kotlin.math.sqrt(clampedA))
    return (earthRadiusMeters * c).toFloat().coerceAtLeast(0f)
}

private val aisSingleFieldKeys = setOf(
    "mmsi",
    "ship_mmsi",
    "target_mmsi",
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
    "cpa_nm",
    "cpa",
    "time_to_cpa",
    "time_to_cpa_min",
    "time_to_cpa_minutes",
    "cpa_time",
    "course",
    "course_over_ground",
    "cog",
    "sog",
    "speed",
    "speed_kn",
    "speed_knots",
    "speed_over_ground",
    "sog_kn",
    "target_speed",
    "target_speed_kn",
    "ais_speed",
    "ais_target_speed",
    "heading",
    "relative_bearing",
    "relative_bearing_deg",
    "bearing",
    "bearing_deg",
    "target_bearing",
    "target_bearing_deg",
    "nav_status",
    "target_nav_status",
    "maneuver_restriction",
    "target_latitude",
    "target_lat",
    "target_lon",
    "target_longitude",
    "lat",
    "latitude",
    "lon",
    "longitude",
    "position.latitude",
    "position.longitude",
    "position_latitude",
    "position_longitude",
    "position.lat",
    "position.lon",
    "target_position_latitude",
    "target_position_lat",
    "target_position_longitude",
    "target_position_lon",
    "target_position.latitude",
    "target_position.longitude",
    "target_position.lat",
    "target_position.lon",
)

private val aisSingleTextFieldKeys = setOf(
    "mmsi",
    "ship_mmsi",
    "target_mmsi",
    "ais_target_mmsi",
    "ais_mmsi",
    "name",
    "ship_name",
    "vessel_name",
    "ais_name",
    "target_name",
)

private val aisMmsiTextFieldKeys = setOf(
    "ais_mmsi",
    "mmsi",
    "ship_mmsi",
    "target_mmsi",
    "ais_target_mmsi",
)

private val AIS_DIRECT_COORDINATE_KEYS = setOf(
    "mmsi",
    "ship_mmsi",
    "target_mmsi",
    "ais_target_mmsi",
    "ais_mmsi",
    "target_latitude",
    "ais_target_latitude",
    "target_lat",
    "target_position_lat",
    "target_position_latitude",
    "latitude",
    "ais_lat",
    "ais_latitude",
    "lat",
    "position.latitude",
    "position_latitude",
    "position.lat",
    "target_longitude",
    "ais_target_longitude",
    "target_lon",
    "longitude",
    "ais_lon",
    "ais_longitude",
    "lon",
    "position.longitude",
    "position_longitude",
    "position.lon",
    "target_position.latitude",
    "target_position.longitude",
    "target_position.lat",
    "target_position.lon",
    "target.position.latitude",
    "target.position.lon",
    "target.position.latitude",
    "target.position.lat",
    "target.position.longitude",
)

private val AIS_INDEXED_FIELD_PREFIXES = listOf(
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

private val AIS_INDEXED_FIELD_PATTERNS = AIS_INDEXED_FIELD_PREFIXES.flatMap { prefix ->
    listOf(
        Regex("^(?:${Regex.escape(prefix)}(?:[._]targets)?)(?:\\[(\\d+)\\]|[._](\\d+))[._](.+)$"),
        Regex("(?:^|[._])(?:${Regex.escape(prefix)}(?:[._]targets)?)(?:\\[(\\d+)\\]|[._](\\d+))[._](.+)$"),
    )
}

private val AIS_INDEXED_FIELD_CATCH_ALL_PATTERNS = arrayOf(
    Regex("^ais_(\\d+)_(.+)$"),
    Regex("^ais\\[(\\d+)\\](.+)$"),
    Regex("^ais\\.(\\d+)\\.(.+)$"),
)

private val AIS_INDEXED_REST_PATTERNS = arrayOf(
    Regex("^_(\\d+)[._](.+)$"),
    Regex("^\\[(\\d+)\\][._](.+)$"),
    Regex("^\\.(\\d+)\\.(.+)$"),
)

private val AIS_INDEXED_DIRECT_PATTERNS = arrayOf(
    Regex("^(\\d+)[._](.+)$"),
    Regex("^\\[(\\d+)\\](.+)$"),
    Regex("^\\.(\\d+)\\.(.+)$"),
)

private val AIS_CANONICALIZE_FIELD_PATTERN = Regex("[^a-z0-9_]+")
private val AIS_CANONICALIZE_UNDERSCORE_PATTERN = Regex("_+")

private val AIS_PGN_MMSI_KEYS = listOf(
    "mmsi",
    "ship_mmsi",
    "target_mmsi",
    "ais_target_mmsi",
    "ais_mmsi",
)

private val AIS_TARGET_DISTANCE_KEYS = listOf(
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
)

private val AIS_TARGET_ABSOLUTE_BEARING_KEYS = listOf(
    "bearing_deg",
    "bearing",
    "target_bearing_deg",
    "target_bearing",
    "course_to_target",
    "ais_bearing",
)

private val AIS_TARGET_RELATIVE_BEARING_KEYS = listOf(
    "relative_bearing_deg",
    "relative_bearing",
    "bearing_relative",
    "relative_target_bearing",
    "ais_relative_bearing",
)

private val AIS_TARGET_COURSE_KEYS = listOf(
    "cog",
    "course",
    "course_over_ground",
    "heading",
    "target_course",
    "ais_course",
    "ais_target_course",
    "ais_heading",
    "ais_target_heading",
)

private val AIS_TARGET_SPEED_KEYS = listOf(
    "sog",
    "speed",
    "speed_kn",
    "speed_knots",
    "sog_kn",
    "target_speed",
    "target_speed_kn",
    "ais_speed",
    "ais_target_speed",
)

private val AIS_TARGET_CPA_KEYS = listOf("cpa_nm", "cpa", "closest_point_of_approach")
private val AIS_TARGET_CPA_TIME_KEYS = listOf(
    "time_to_cpa",
    "time_to_cpa_min",
    "time_to_cpa_minutes",
    "cpa_time",
    "ttcpa",
    "ttcpa_min",
)
private val AIS_TARGET_NAV_STATUS_KEYS = listOf("nav_status", "navigation_status", "target_nav_status")
private val AIS_TARGET_LAT_KEYS = listOf(
    "target_latitude",
    "ais_target_latitude",
    "target_lat",
    "target_position_lat",
    "target_position_latitude",
    "position.lat",
    "position_latitude",
    "latitude",
    "ais_lat",
    "ais_latitude",
    "lat",
    "position.latitude",
    "target.position.latitude",
    "target.position.lat",
    "target_position.latitude",
    "target_position.lat",
)
private val AIS_TARGET_LON_KEYS = listOf(
    "target_longitude",
    "ais_target_longitude",
    "target_lon",
    "target_position_lon",
    "target_position_longitude",
    "position.lon",
    "position_longitude",
    "longitude",
    "ais_lon",
    "ais_longitude",
    "lon",
    "position.longitude",
    "target.position.longitude",
    "target.position.lon",
    "target_position.longitude",
    "target_position.lon",
)
private val AIS_TARGET_NAME_KEYS = listOf("name", "ship_name", "vessel_name", "ais_name", "target_name")

private fun isLikelyAisIdentityField(key: String): Boolean {
    val normalized = canonicalizeAisFieldName(key)
    return normalized == "mmsi" ||
        normalized == "ship_mmsi" ||
        normalized == "target_mmsi" ||
        normalized == "ais_mmsi" ||
        normalized == "ais_target_mmsi" ||
        normalized.endsWith("_mmsi")
}

private fun isAisDirectKeyPrefix(key: String): Boolean {
    return key.startsWith("ais") ||
        key.startsWith("target") ||
        key.startsWith("targets")
}

private val AIS_FIELD_CONTAINER_PREFIXES = listOf(
    "ais",
    "ais_",
    "target",
    "target.",
    "target_",
    "target[",
    "targets",
    "targets.",
    "targets_",
    "targets[",
    "fields",
    "field",
    "data_fields",
    "n2kfields",
    "n2k_fields",
    "fields.",
    "field.",
    "data_fields.",
    "n2kfields.",
    "n2k_fields.",
)

private val aisAliasFields = mapOf(
    "navigation_status" to "nav_status",
    "navigationstatus" to "nav_status",
    "navigation-status" to "nav_status",
    "course_overground" to "course",
    "courseoverground" to "course",
    "course_over_ground" to "course",
    "true_course" to "course",
    "speed_overground" to "sog",
    "speedoverground" to "sog",
    "sog" to "sog",
    "sogkn" to "sog_kn",
    "speed_over_ground" to "speed_over_ground",
    "target_latitude" to "target_latitude",
    "target_lat" to "target_lat",
    "target_longitude" to "target_longitude",
    "target_lon" to "target_lon",
)

private fun canonicalizeAisFieldName(raw: String): String {
    return raw
        .lowercase()
        .replace('-', '_')
        .replace(' ', '_')
        .replace(AIS_CANONICALIZE_FIELD_PATTERN, "_")
        .replace(AIS_CANONICALIZE_UNDERSCORE_PATTERN, "_")
        .trim('_')
}

private fun resolveAisContainerField(key: String, isTextField: Boolean): String? {
    val normalizedKey = key.lowercase()
    if (AIS_FIELD_CONTAINER_PREFIXES.none { normalizedKey.startsWith(it) }) {
        return null
    }

    val suffix = when {
        normalizedKey.startsWith("ais_") -> normalizedKey.removePrefix("ais_")
        normalizedKey.startsWith("ais.") -> normalizedKey.removePrefix("ais.")
        normalizedKey.startsWith("ais[") -> normalizedKey
            .removePrefix("ais[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("target.") -> normalizedKey
            .removePrefix("target.")
            .trimStart('.', '_')
        normalizedKey.startsWith("targets.") -> normalizedKey
            .removePrefix("targets.")
            .trimStart('.', '_')
        normalizedKey.startsWith("target[") -> normalizedKey
            .removePrefix("target[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("targets[") -> normalizedKey
            .removePrefix("targets[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("target_") -> normalizedKey.removePrefix("target_")
        normalizedKey.startsWith("targets_") -> normalizedKey.removePrefix("targets_")
        normalizedKey.startsWith("fields.") -> normalizedKey.removePrefix("fields.")
        normalizedKey.startsWith("field.") -> normalizedKey.removePrefix("field.")
        normalizedKey.startsWith("fields[") -> normalizedKey
            .removePrefix("fields[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("field[") -> normalizedKey
            .removePrefix("field[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("data_fields.") -> normalizedKey.removePrefix("data_fields.")
        normalizedKey.startsWith("data_fields[") -> normalizedKey
            .removePrefix("data_fields[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("n2kfields.") -> normalizedKey.removePrefix("n2kfields.")
        normalizedKey.startsWith("n2kfields[") -> normalizedKey
            .removePrefix("n2kfields[")
            .substringAfter(']')
            .trimStart('.', '_')
        normalizedKey.startsWith("n2k_fields.") -> normalizedKey.removePrefix("n2k_fields.")
        normalizedKey.startsWith("n2k_fields[") -> normalizedKey
            .removePrefix("n2k_fields[")
            .substringAfter(']')
            .trimStart('.', '_')
        else -> return null
    }.trim()

    if (suffix.isBlank()) return null

    val candidates = mutableListOf(canonicalizeAisFieldName(suffix))
    val leaf = suffix.substringAfterLast('.', suffix).trim()
    if (leaf != suffix) {
        candidates.add(canonicalizeAisFieldName(leaf))
    }

    candidates.forEach { rawCandidate ->
        val candidate = aisAliasFields[rawCandidate] ?: rawCandidate
        if (isTextField) {
            if (aisSingleTextFieldKeys.contains(candidate)) return candidate
            return@forEach
        }
        if (aisSingleFieldKeys.contains(candidate) || candidate.contains("distance") || candidate.contains("bearing")) {
            return candidate
        }
    }

    return null
}

private fun isAisUpdateFromTelemetry(text: Map<String, String>): Boolean {
    val nmea0183Category = text["nmea0183_category"]?.uppercase(Locale.ROOT)?.trim()
    if (nmea0183Category == "AIS") {
        return true
    }

    val nmeaSentence = text["nmea_sentence"]?.uppercase(Locale.ROOT)?.trim()
    val nmea0183Sentence = text["nmea0183_sentence"]?.uppercase(Locale.ROOT)?.trim()
    val nmea0183SentenceFull = text["nmea0183_sentence_full"]?.uppercase(Locale.ROOT)?.trim()
    if (
        nmeaSentence == "VDM" || nmeaSentence == "VDO" ||
        nmea0183Sentence == "VDM" || nmea0183Sentence == "VDO" ||
        nmea0183SentenceFull == "AIVDM" || nmea0183SentenceFull == "AIVDO"
    ) {
        return true
    }

    val detectedPgn = text["n2k_detected_pgn"] ?: text["n2k_pgn"]
    val pgn = detectedPgn?.trim()?.toIntOrNull() ?: return false
    return pgn == 129038 || pgn == 129039
}

private fun hasDirectAisDataSignature(
    telemetryValues: Map<String, Float>,
    telemetryText: Map<String, String>,
): Boolean {
    val hasMmsiNumeric = telemetryValues.keys.any(::isLikelyAisIdentityField)
    val hasMmsiText = telemetryText.keys.any(::isLikelyAisIdentityField)
    if (hasMmsiNumeric || hasMmsiText) return true

    val hasAisTextTag = telemetryText.keys.any { it.lowercase(Locale.ROOT).startsWith("ais") }
    return hasAisTextTag
}

private fun parseAisTargetsFromTelemetry(
    telemetry: Map<String, Float>,
    telemetryText: Map<String, String>,
    ownHeadingDeg: Float?,
    ownLat: Float?,
    ownLon: Float?,
    receivedAtMs: Long,
): List<AisTargetData> {
    val normalizedOwnHeadingDeg = ownHeadingDeg
        ?.let { heading -> heading.takeIf { it.isFinite() }?.let(::wrap360Ui) }
    val directValues = mutableMapOf<String, Float>()
    val directTextValues = mutableMapOf<String, String>()
    val groupedValues = linkedMapOf<String, MutableMap<String, Float>>()
    val groupedTextValues = linkedMapOf<String, MutableMap<String, String>>()
    var hasDirectAisContext = false

    telemetry.forEach { (rawKey, value) ->
        if (!value.isFinite()) return@forEach
        val key = rawKey.lowercase()
        val indexedField = parseAisIndexedFieldName(key)
        if (indexedField != null) {
            hasDirectAisContext = true
            val (index, suffix) = indexedField
            val valuesByIndex = groupedValues.getOrPut(index) { mutableMapOf() }
            valuesByIndex[suffix] = value
            val leafSuffix = suffix.substringAfterLast('.')
            if (leafSuffix.isNotBlank() && leafSuffix != suffix) {
                valuesByIndex[leafSuffix] = value
            }
            val resolvedSuffix = resolveAisContainerField(key, isTextField = false)
            if (resolvedSuffix != null) {
                valuesByIndex[resolvedSuffix] = value
            }
            return@forEach
        }

        if (key.startsWith("ais_")) {
            hasDirectAisContext = true
            val suffix = key.removePrefix("ais_").takeIf { it.isNotBlank() } ?: return@forEach
            if (aisSingleFieldKeys.contains(suffix) || suffix.contains("distance") || suffix.contains("bearing")) {
                directValues[suffix] = value
            }
            return@forEach
        }
        if (key.startsWith("ais.")) {
            val suffix = key.removePrefix("ais.").takeIf { it.isNotBlank() } ?: return@forEach
            hasDirectAisContext = true
            if (aisSingleFieldKeys.contains(suffix) || suffix.contains("distance") || suffix.contains("bearing")) {
                directValues[suffix] = value
            }
            return@forEach
        }
        val containerField = resolveAisContainerField(key, isTextField = false)
        if (containerField != null) {
            hasDirectAisContext = hasDirectAisContext || isAisDirectKeyPrefix(key)
            directValues[containerField] = value
            return@forEach
        }

        if (aisSingleFieldKeys.contains(key)) {
            directValues[key] = value
        }
    }

    telemetryText.forEach { (rawKey, value) ->
        val key = rawKey.lowercase()
        if (value.isBlank()) return@forEach
        val indexedField = parseAisIndexedFieldName(key)
        if (indexedField != null) {
            hasDirectAisContext = true
            val (index, suffix) = indexedField
            val valuesByIndex = groupedTextValues.getOrPut(index) { mutableMapOf() }
            valuesByIndex[suffix] = value.trim()
            val leafSuffix = suffix.substringAfterLast('.')
            if (leafSuffix.isNotBlank() && leafSuffix != suffix) {
                valuesByIndex[leafSuffix] = value.trim()
            }
            val resolvedSuffix = resolveAisContainerField(key, isTextField = true)
            if (resolvedSuffix != null) {
                valuesByIndex[resolvedSuffix] = value.trim()
            }
            return@forEach
        }

        if (key.startsWith("ais_")) {
            hasDirectAisContext = true
            val suffix = key.removePrefix("ais_").takeIf { it.isNotBlank() } ?: return@forEach
            if (aisSingleTextFieldKeys.contains(suffix)) {
                directTextValues[suffix] = value.trim()
            }
            return@forEach
        }
        if (key.startsWith("ais.")) {
            hasDirectAisContext = true
            val suffix = key.removePrefix("ais.").takeIf { it.isNotBlank() } ?: return@forEach
            if (aisSingleTextFieldKeys.contains(suffix)) {
                directTextValues[suffix] = value.trim()
            }
            return@forEach
        }
        val containerTextField = resolveAisContainerField(key, isTextField = true)
        if (containerTextField != null) {
            hasDirectAisContext = hasDirectAisContext || isAisDirectKeyPrefix(key)
            directTextValues[containerTextField] = value.trim()
            return@forEach
        }

        if (aisSingleTextFieldKeys.contains(key)) {
            directTextValues[key] = value.trim()
        }
    }

    val hasDirectMmsiContext = directTextValues.keys.any { it in aisMmsiTextFieldKeys } ||
        directValues.keys.any { it in aisMmsiTextFieldKeys }
    val hasUnscopedAisIdentityContext = directValues.keys.any {
        it in aisSingleFieldKeys && it !in AIS_DIRECT_COORDINATE_KEYS
    } || directTextValues.keys.any { it in aisMmsiTextFieldKeys }
    hasDirectAisContext = hasDirectAisContext || hasDirectMmsiContext
    val hasAisSourceMetadata = isAisUpdateFromTelemetry(telemetryText) ||
        hasDirectAisDataSignature(telemetry, telemetryText)
    val hasDirectIdentityContext = hasDirectMmsiContext ||
        directValues.keys.any { it in aisMmsiTextFieldKeys }
    val hasAisSignalForCoordinates = hasDirectAisContext ||
        hasUnscopedAisIdentityContext ||
        hasAisSourceMetadata ||
        hasDirectIdentityContext
    if (!hasAisSignalForCoordinates) {
        directValues.keys.removeAll(AIS_DIRECT_COORDINATE_KEYS)
    }

    val parsedTargets = LinkedHashMap<String, AisTargetData>()
    groupedValues.forEach { (index, values) ->
        parseAisTargetFromValues(
            values = values,
            textValues = groupedTextValues[index] ?: emptyMap(),
            ownHeadingDeg = normalizedOwnHeadingDeg,
            ownLat = ownLat,
            ownLon = ownLon,
            fallbackId = "ais_$index",
            receivedAtMs = receivedAtMs,
        )?.let { target ->
            parsedTargets[aisTargetIdentityKey(target)] = target
        }
    }

    val directTarget = parseAisTargetFromValues(
        values = directValues,
        textValues = directTextValues,
        ownHeadingDeg = normalizedOwnHeadingDeg,
        ownLat = ownLat,
        ownLon = ownLon,
        fallbackId = "ais",
        receivedAtMs = receivedAtMs,
    )

    if (directTarget != null && hasDirectIdentityContext) {
        parsedTargets[aisTargetIdentityKey(directTarget)] = directTarget
    }

    return parsedTargets.values.toList()
}

private fun parseAisIndexedFieldName(key: String): Pair<String, String>? {
    val normalized = key.lowercase()
    AIS_INDEXED_FIELD_CATCH_ALL_PATTERNS.forEach { pattern ->
        val match = pattern.find(normalized)
        if (match != null) {
            val index = match.groupValues[1]
            val field = match.groupValues[2].trimStart('_', '.', '[' , ']').trim()
            if (isLikelyAisIndexedAisField(field)) {
                return index to field
            }
        }
    }

    var rest = ""

    val directMatch = AIS_INDEXED_FIELD_PREFIXES.firstNotNullOfOrNull { prefix ->
        AIS_INDEXED_FIELD_PATTERNS.firstNotNullOfOrNull { pattern ->
            val match = pattern.find(normalized) ?: return@firstNotNullOfOrNull null
            val index = match.groupValues[1].ifEmpty { match.groupValues[2] }
            val field = match.groupValues[3].trim().trimStart('_', '.', '[' , ']')
            if (index.isNotEmpty() && isLikelyAisIndexedAisField(field)) {
                index to field
            } else {
                null
            }
        }
    }
    if (directMatch != null) return directMatch

    val matchedPrefix = AIS_INDEXED_FIELD_PREFIXES.firstOrNull {
        normalized.startsWith("${it}.") || normalized.startsWith("${it}[") || normalized.startsWith("${it}_") || normalized == it
    }
    if (matchedPrefix == null) return null

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
    AIS_INDEXED_REST_PATTERNS.forEach { pattern ->
        val match = pattern.find(rest) ?: return@forEach
        val index = match.groupValues[1]
        val field = match.groupValues[2]
            .trimStart('_', '.')
            .trim()
        if (field.isBlank() || !isLikelyAisIndexedAisField(field)) return@forEach
        return index to field
    }
    AIS_INDEXED_DIRECT_PATTERNS.forEach { pattern ->
        val match = pattern.find(normalized) ?: return@forEach
        val index = match.groupValues[1]
        val field = match.groupValues[2]
            .trimStart('_', '.', '[' , ']')
            .trim()
        if (field.isBlank() || !isLikelyAisIndexedAisField(field)) return@forEach
        return index to field
    }
    return null
}

private fun isLikelyAisIndexedAisField(field: String): Boolean {
    val normalized = canonicalizeAisFieldName(field)
    if (normalized.isBlank()) return false
    val candidate = aisAliasFields[normalized.substringAfterLast('.')] ?: normalized.substringAfterLast('.')
    return aisSingleFieldKeys.contains(candidate)
        || aisSingleTextFieldKeys.contains(candidate)
        || candidate.contains("distance")
        || candidate.contains("bearing")
        || candidate.contains("latitude")
        || candidate.contains("longitude")
        || candidate.contains("position")
        || candidate.contains("mmsi")
}

private fun parseAisTargetFromValues(
    values: Map<String, Float>,
    textValues: Map<String, String>,
    ownHeadingDeg: Float?,
    ownLat: Float?,
    ownLon: Float?,
    fallbackId: String,
    receivedAtMs: Long = System.currentTimeMillis(),
): AisTargetData? {
    val textMmsi = parseAisText(values = textValues, keys = AIS_PGN_MMSI_KEYS)
    val parsedTextMmsi = parseAisMmsiText(textMmsi)
    val parsedNumericMmsi = pickAisFloat(
        values,
        textValues,
        AIS_PGN_MMSI_KEYS
    )
    val parsedLastSeenMs = parseAisLastSeenMs(
        textValues = textValues,
        fallbackNumericValues = values,
    )
    val normalizedLastSeenMs = parsedLastSeenMs?.takeIf { it > 0L } ?: receivedAtMs
    val mmsi = parsedTextMmsi?.toFloatOrNull() ?: parsedNumericMmsi
    val mmsiText = parsedTextMmsi ?: parsedNumericMmsi?.let { normalizeAisMmsi(it) }

    val distanceCandidate = pickAisFloatWithField(
        values,
        textValues,
        AIS_TARGET_DISTANCE_KEYS
    )
    var distanceNm = distanceCandidate?.let { (sourceField, rawDistanceNm) ->
        parseAisDistanceNm(sourceField, rawDistanceNm)
    }

    var absoluteBearingDeg = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_ABSOLUTE_BEARING_KEYS
    )
    var relativeBearingDeg = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_RELATIVE_BEARING_KEYS
    )

    val courseDeg = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_COURSE_KEYS
    )
    val speedKn = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_SPEED_KEYS
    )
    val cpaNm = pickAisFloat(values, textValues, AIS_TARGET_CPA_KEYS)
    val cpaTimeMinutes = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_CPA_TIME_KEYS
    )
    val navStatus = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_NAV_STATUS_KEYS
    )

    val targetLat = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_LAT_KEYS
    )
    val targetLon = pickAisFloat(
        values,
        textValues,
        AIS_TARGET_LON_KEYS
    )
    val vesselName = parseAisText(textValues, AIS_TARGET_NAME_KEYS)

    val normalizedOwnHeadingDeg = ownHeadingDeg
        ?.let { heading -> heading.takeIf { it.isFinite() }?.let(::wrap360Ui) }

    val computedDistanceNm = computeAisDistanceNm(
        ownLat = ownLat,
        ownLon = ownLon,
        targetLat = targetLat,
        targetLon = targetLon,
    )
    if (computedDistanceNm != null) {
        distanceNm = computedDistanceNm.coerceAtLeast(0f)
    } else {
        distanceNm = distanceNm?.coerceAtLeast(0f)
    }
    if (distanceNm == null || absoluteBearingDeg == null || relativeBearingDeg == null) {
        val computedBearingDeg = computeGpsBearingNm(ownLat, ownLon, targetLat, targetLon)
        if (absoluteBearingDeg == null && computedBearingDeg != null) {
            absoluteBearingDeg = computedBearingDeg
        }
        if (relativeBearingDeg == null && computedBearingDeg != null) {
            relativeBearingDeg = normalizedOwnHeadingDeg?.let {
                normalizeSignedAngleUi(computedBearingDeg - it)
            } ?: computedBearingDeg
        }
    }

    if (relativeBearingDeg != null) {
        relativeBearingDeg = normalizeSignedAngleUi(relativeBearingDeg)
    }

    if (absoluteBearingDeg == null && relativeBearingDeg != null && normalizedOwnHeadingDeg != null) {
        absoluteBearingDeg = wrap360Ui(relativeBearingDeg + normalizedOwnHeadingDeg)
    }

    val fallback = mmsiText?.let { "ais_$it" } ?: fallbackId

    return AisTargetData(
        id = fallback,
        name = vesselName,
        mmsi = mmsi,
        mmsiText = mmsiText,
        lastSeenMs = normalizedLastSeenMs,
        latitude = targetLat,
        longitude = targetLon,
        distanceNm = distanceNm,
        cpaNm = cpaNm,
        cpaTimeMinutes = cpaTimeMinutes,
        courseDeg = courseDeg,
        speedKn = speedKn,
        maneuverRestriction = formatAisManeuverRestriction(navStatus),
        relativeBearingDeg = relativeBearingDeg,
        absoluteBearingDeg = absoluteBearingDeg,
    )
}

private fun pickAisFloat(
    values: Map<String, Float>,
    textValues: Map<String, String>,
    keys: List<String>,
): Float? {
    keys.forEach { key ->
        val value = values[key]
        if (value != null && value.isFinite()) return value
        val textValue = textValues[key]?.trim()?.replace(",", ".") ?: return@forEach
        val parsed = textValue.toFloatOrNull()
        if (parsed != null && parsed.isFinite()) {
            return parsed
        }
    }
    return null
}

private fun pickAisFloat(values: Map<String, Float>, keys: List<String>): Float? {
    keys.forEach { key ->
        val value = values[key]
        if (value != null && value.isFinite()) return value
    }
    return null
}

private fun pickAisFloatWithField(
    values: Map<String, Float>,
    textValues: Map<String, String>,
    keys: List<String>,
): Pair<String, Float>? {
    keys.forEach { key ->
        val value = values[key]
        if (value != null && value.isFinite()) return key to value
        val textValue = textValues[key]?.trim()?.replace(",", ".") ?: return@forEach
        val parsed = textValue.toFloatOrNull()
        if (parsed != null && parsed.isFinite()) return key to parsed
    }
    return null
}

private fun pickAisFloatWithField(
    values: Map<String, Float>,
    keys: List<String>,
): Pair<String, Float>? = pickAisFloatWithField(values = values, textValues = emptyMap(), keys = keys)

private fun parseAisDistanceNm(sourceField: String, rawDistanceNm: Float?): Float? {
    if (rawDistanceNm == null || !rawDistanceNm.isFinite()) return null
    if (rawDistanceNm < 0f) return null

    return when (sourceField.lowercase()) {
        "distance_m",
        "distance_meters",
        "distance_meter",
        "range_m",
        "range_meters",
        "range_meter" -> rawDistanceNm / 1852f
        "distance_km",
        "distance_kilometers",
        "range_km",
        "range_kilometers" -> rawDistanceNm / 1.852f
        else -> {
            if (sourceField == "range" || sourceField == "distance") {
                if (rawDistanceNm > 2000f) rawDistanceNm / 1852f else rawDistanceNm
            } else {
                rawDistanceNm
            }
        }
    }
}

private fun parseAisText(values: Map<String, String>, keys: List<String>): String? {
    keys.forEach { key ->
        val value = values[key]
        if (!value.isNullOrBlank()) return value.trim()
    }
    return null
}

private fun parseAisLastSeenMs(
    textValues: Map<String, String>,
    fallbackNumericValues: Map<String, Float>,
): Long? {
    val candidates = listOf(
        "last_seen_ms",
        "ais_last_seen_ms",
        "target_last_seen_ms",
    )

    candidates.forEach { key ->
        val rawValue = textValues[key]?.trim()
        if (!rawValue.isNullOrBlank()) {
            val parsed = rawValue.toLongOrNull() ?: rawValue.toDoubleOrNull()?.toLong()
            if (parsed != null) {
                return parsed
            }
        }
    }
    candidates.forEach { key ->
        val rawValue = fallbackNumericValues[key]
            ?.takeIf { it.isFinite() }
            ?.toString()
            ?.trim()
            ?: return@forEach
        val parsed = rawValue.toLongOrNull()
        if (parsed != null) {
            return parsed
        }
    }
    return null
}

private fun parseAisMmsiTextToFloat(value: String?): Float? {
    return parseAisMmsiText(value)?.toFloatOrNull()
}

private fun parseAisMmsiText(value: String?): String? {
    val cleaned = value
        ?.trim()
        ?.filter { ch -> ch.isDigit() }
        ?: return null
    return cleaned.ifBlank { null }
}

private fun formatAisManeuverRestriction(value: Float?): String? {
    if (value == null || value.isNaN() || value.isInfinite()) return null
    return when (value.roundToInt()) {
        0 -> "unter Fahrt"
        1 -> "vor Anker"
        2 -> "nicht steuerbar"
        3 -> "eingeschränkt durch Seegang"
        4 -> "festgemacht / Treibanker"
        5 -> "nicht fahrfähig"
        6 -> "fischend"
        7 -> "unter Segel"
        8 -> "manövriert"
        9 -> "nicht klar"
        10 -> "nicht besetzt"
        11 -> "manöverfähig"
        else -> "Status ${value.roundToInt()}"
    }
}

private fun computeAisDistanceNm(
    ownLat: Float?,
    ownLon: Float?,
    targetLat: Float?,
    targetLon: Float?,
): Float? {
    if (
        ownLat == null || ownLon == null ||
        targetLat == null || targetLon == null
    ) return null
    if (
        ownLat !in -90f..90f ||
        ownLon !in -180f..180f ||
        targetLat !in -90f..90f ||
        targetLon !in -180f..180f
    ) return null

    val earthRadiusNm = 3440.065f
    val lat1 = Math.toRadians(ownLat.toDouble())
    val lat2 = Math.toRadians(targetLat.toDouble())
    val dLat = Math.toRadians((targetLat - ownLat).toDouble())
    val dLon = Math.toRadians((targetLon - ownLon).toDouble())
    val sinHalfLat = kotlin.math.sin(dLat / 2.0)
    val sinHalfLon = kotlin.math.sin(dLon / 2.0)
    val a = sinHalfLat * sinHalfLat +
        kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinHalfLon * sinHalfLon
    if (a <= 0.0) return 0f
    val c = 2.0 * kotlin.math.atan2(
        kotlin.math.sqrt(a),
        kotlin.math.sqrt(1.0 - a)
    )
    return (earthRadiusNm * c).toFloat()
}

private fun computeGpsBearingNm(
    ownLat: Float?,
    ownLon: Float?,
    targetLat: Float?,
    targetLon: Float?,
): Float? {
    if (
        ownLat == null || ownLon == null ||
        targetLat == null || targetLon == null
    ) return null
    if (
        ownLat !in -90f..90f || ownLon !in -180f..180f ||
        targetLat !in -90f..90f || targetLon !in -180f..180f
    ) return null

    val toRad = (PI / 180f).toFloat()
    val lat1 = ownLat * toRad
    val lat2 = targetLat * toRad
    val dLon = (targetLon - ownLon) * toRad
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val bearing = atan2(y, x) * (180f / Math.PI.toFloat())
    return wrap360Ui(bearing)
}

@Preview(showBackground = true, name = "Dashboard Seite", widthDp = 430, heightDp = 860)
@Composable
fun DashboardPagePreview() {
    val telemetry = mapOf(
        "battery_soc" to 76.4f,
        "water_tank" to 42.0f,
        "wind_speed" to 12.7f,
        "wind_angle" to 37.0f,
        "autopilot_heading" to 118.0f,
        "autopilot_mode" to 1f,
        "engine_rpm" to 2800f,
    )

    val page = DashboardPage(
        name = "Segeln",
        widgets = listOf(
            DashboardWidget(
                kind = WidgetKind.BATTERY,
                title = "Batterie",
                xPx = 30f,
                yPx = 30f,
                widthPx = 260f,
                heightPx = 190f,
                dataKeys = listOf("battery_soc", "battery_level", "soc")
            ),
            DashboardWidget(
                kind = WidgetKind.WATER_TANK,
                title = "Wassertank",
                xPx = 320f,
                yPx = 30f,
                widthPx = 230f,
                heightPx = 185f,
                dataKeys = listOf("water_tank", "tank_level")
            ),
            DashboardWidget(
                kind = WidgetKind.WIND,
                title = "Wind",
                xPx = 30f,
                yPx = 250f,
                widthPx = 320f,
                heightPx = 220f,
                dataKeys = listOf("wind_angle", "wind_direction")
            )
        )
    )

    MaterialTheme(typography = createModernTypography(UiFont.ORBITRON)) {
        Box(modifier = Modifier.fillMaxSize()) {
            DashboardPageLayout(
                page = page,
                telemetry = telemetry,
                telemetryText = emptyMap(),
                aisTelemetry = emptyMap(),
                aisTelemetryText = emptyMap(),
                recentNmeaPgnHistory = emptyList(),
                recentNmea0183History = emptyList(),
                dalyDebugEvents = emptyList(),
                detectedNmeaSources = emptyList(),
                titleScale = 1.0f,
            uiFont = UiFont.ORBITRON,
            darkBackground = true,
            gridStepPercent = 2.5f,
            widgetFrameStyle = WidgetFrameStyle.BORDER,
            widgetFrameStyleGrayOffset = 0,
            onMove = { _, _, _, _, _, _, _ -> },
                onResize = { _, _, _, _, _, _, _ -> },
                onSnap = { _, _, _, _ -> },
                onRemove = { },
                onRename = { _, _ -> },
                onAddWidget = { _, _, _, _ -> false },
                onAddWidgetError = {},
                onPageMetrics = { _, _, _ -> },
                onApplyMinimums = { _, _, _ -> },
                onSendAutopilotCommand = {},
                onUpdateWindWidgetSettings = { _, _ -> },
                onUpdateBatteryWidgetSettings = { _, _ -> },
                onUpdateAisWidgetSettings = { _, _ -> },
                onUpdateEchosounderWidgetSettings = { _, _ -> },
                                onUpdateAutopilotWidgetSettings = { _, _ -> },
                                onUpdateLogWidgetSettings = { _, _ -> },
                                onUpdateAnchorWatchWidgetSettings = { _, _ -> },
                                onUpdateTemperatureWidgetSettings = { _, _ -> },
                                onUpdateSeaChartWidgetSettings = { _, _ -> },
                                alarmToneVolume = 0.7f,
                                alarmRepeatIntervalSeconds = 5,
                                onAnchorWatchAlarm = { },
                                onCallAisMmsi = { },
                                storedWindWidgetSettings = emptyMap(),
                storedBatteryWidgetSettings = emptyMap(),
                storedAisWidgetSettings = emptyMap(),
                storedEchosounderWidgetSettings = emptyMap(),
                storedAutopilotWidgetSettings = emptyMap(),
                storedLogWidgetSettings = emptyMap(),
                storedAnchorWatchWidgetSettings = emptyMap(),
                storedTemperatureWidgetSettings = emptyMap(),
                storedSeaChartWidgetSettings = emptyMap(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Top Bar", widthDp = 430, heightDp = 240)
@Composable
fun TopBarPreview() {
    val state = DashboardState(
        pages = listOf(
            DashboardPage(name = "Navigation", widgets = emptyList()),
            DashboardPage(name = "Motor", widgets = emptyList()),
            DashboardPage(name = "Wetter", widgets = emptyList()),
        ),
        selectedPage = 0,
    )

    MaterialTheme(typography = createModernTypography(UiFont.ORBITRON)) {
        DashboardTopBar(
            state = state,
            darkBackground = true,
                onSelectPage = {},
                onAddPage = {},
                onRenamePage = { _, _ -> },
                onAddWidget = { _, _, _, _ -> false },
                onAddWidgetError = {},
                onToggleRouterSimulation = {},
                onToggleBackground = {},
                onUpdateUiFont = {},
                onUpdateFontScale = {},
            onUpdateGridStepPercent = {},
            onUpdateLayoutOrientation = {},
            onUpdateAlarmToneVolume = {},
            onUpdateAlarmRepeatIntervalSeconds = {},
            onUpdateAlarmVoiceAnnouncementsEnabled = {},
            onUpdateAlarmVoiceProfileIndex = {},
            onUpdateWidgetFrameStyle = {},
            onUpdateWidgetFrameStyleGrayOffset = {},
            onUpdateNmeaRouter = { _, _, _ -> },
            onUpdateNmeaSourceDisplayName = { _, _ -> },
            onClearNmeaSourceDisplayName = {},
            onRemoveNmeaSourceProfile = {},
            onUpdateBoatProfile = {},
            onUpdateBackupPrivacyMode = {},
            onUpdateBootAutostartEnabled = {},
            onUpdateRouterSimulationOrigin = { _, _ -> },
            onClearStoredData = {},
            onRestoreStoredData = { false },
            onLoadReleaseNotes = { "Keine Release-Notizen." },
            onLoadReleaseNoteCount = { 0 },
            onExitApp = {},
            onTriggerAnchorWatchAlarm = {},
        )
    }
}
