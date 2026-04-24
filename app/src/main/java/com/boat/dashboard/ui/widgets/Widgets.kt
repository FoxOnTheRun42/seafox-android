package com.seafox.nmea_dashboard.ui.widgets

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import com.seafox.nmea_dashboard.SeaFoxDesignTokens
import com.seafox.nmea_dashboard.R
import com.seafox.nmea_dashboard.data.NmeaSourceProfile
import com.seafox.nmea_dashboard.data.NmeaPgnHistoryEntry
import com.seafox.nmea_dashboard.data.Nmea0183HistoryEntry
import com.seafox.nmea_dashboard.data.AutopilotControlMode
import com.seafox.nmea_dashboard.data.UiFont
import com.seafox.nmea_dashboard.ui.MENU_SPACING
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.hypot
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import kotlin.concurrent.thread
import kotlinx.coroutines.delay

private const val AIS_TARGET_STALE_WARNING_MINUTES = 3

private val menuDialogPadding = SeaFoxDesignTokens.Size.menuContentPadding

enum class WindSpeedUnit(val label: String) {
    MPS("m/s"),
    KNOTS("kn"),
    BEAUFORT("Bft"),
    KMH("km/h"),
}

enum class BatteryChemistry(val label: String) {
    LEAD_ACID("Bleisäure"),
    LITHIUM("Lithium"),
}

data class BatteryWidgetSettings(
    val chemistry: BatteryChemistry = BatteryChemistry.LEAD_ACID,
)

const val DEFAULT_TACKING_ANGLE_DEG = 90
const val DEFAULT_ANCHOR_WATCH_DEVIATION_METERS = 10f
const val DEFAULT_ANCHOR_WATCH_TOLERANCE_PERCENT = 10f
const val DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS = 30f

data class WindWidgetSettings(
    val showBoatDirection: Boolean = true,
    val showNorthDirection: Boolean = true,
    val showWindSpeed: Boolean = true,
    val speedUnit: WindSpeedUnit = WindSpeedUnit.KNOTS,
    val tackingAngleDeg: Int = DEFAULT_TACKING_ANGLE_DEG,
    val historyWindowMinutes: Int = 5,
    val minMaxUsesTrueWind: Boolean = true,
)

enum class TemperatureUnit(val label: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

data class TemperatureSensor(
    val name: String = "",
    val valueCelsius: Float? = null,
)

data class TemperatureWidgetSettings(
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val sensors: List<String> = List(10) { index -> "Temperatursensor ${index + 1}" },
)

data class AisTargetData(
    val id: String = "",
    val name: String? = null,
    val mmsi: Float? = null,
    val mmsiText: String? = null,
    val latitude: Float? = null,
    val longitude: Float? = null,
    val distanceNm: Float? = null,
    val cpaNm: Float? = null,
    val cpaTimeMinutes: Float? = null,
    val courseDeg: Float? = null,
    val speedKn: Float? = null,
    val maneuverRestriction: String? = null,
    val relativeBearingDeg: Float? = null,
    val absoluteBearingDeg: Float? = null,
    val lastSeenMs: Long = 0L,
)

data class AisWidgetSettings(
    val cpaAlarmDistanceNm: Float = 1f,
    val cpaAlarmMinutes: Float = 5f,
    val targetVisibilityMinutes: Int = 5,
    val targetVisibilityMinutesByMmsi: Map<String, Int> = emptyMap(),
    val displayRangeNm: Float = 10f,
    val collisionAlarmsMuted: Boolean = false,
    val northUp: Boolean = true,
    val fontSizeOffsetSp: Int = 0,
)

data class AisTargetWithAge(
    val target: AisTargetData,
    val receivedAtMs: Long,
    val visibilityTimeoutMinutes: Int = 5,
)

data class GpsWidgetData(
    val latitude: Float? = null,
    val longitude: Float? = null,
    val courseOverGround: Float? = null,
    val speedOverGround: Float? = null,
    val heading: Float? = null,
    val altitudeM: Float? = null,
    val satellites: Int? = null,
    val hdop: Float? = null,
    val fixQuality: Float? = null,
    val utcTime: String? = null,
)

data class LogWidgetSettings(
    val periodMinutes: Int = 10,
    val speedSource: LogWidgetSpeedSource = LogWidgetSpeedSource.GPS_SOG,
)

enum class LogWidgetSpeedSource(val label: String) {
    GPS_SOG("SOG"),
    LOG_STW("STW"),
}

data class SystemWidgetLoadEntry(
    val widgetId: String,
    val widgetTitle: String,
    val widgetKindLabel: String,
    val estimatedLoadPercent: Float,
)

data class SystemWidgetLoadSnapshot(
    val cpuPercent: Float,
    val memoryMb: Float,
    val javaHeapMb: Float,
    val maxHeapMb: Float,
    val loadEntries: List<SystemWidgetLoadEntry> = emptyList(),
)

@Composable
fun SystemWidget(
    snapshot: SystemWidgetLoadSnapshot,
    modifier: Modifier = Modifier,
) {
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val cpuText = String.format(Locale.GERMANY, "%.1f %%", snapshot.cpuPercent.coerceIn(0f, 999f))
    val memoryText = String.format(Locale.GERMANY, "%.1f MB", snapshot.memoryMb.coerceAtLeast(0f))
    val heapText = String.format(
        Locale.GERMANY,
        "Heap %.1f / %.1f MB",
        snapshot.javaHeapMb.coerceAtLeast(0f),
        snapshot.maxHeapMb.coerceAtLeast(0f),
    )
    val entries = snapshot.loadEntries.take(20)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("SYSTEM", fontWeight = FontWeight.Bold, color = premiumSignalColor)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("App CPU", color = mutedColor)
            Text(cpuText, color = textColor, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RAM", color = mutedColor)
            Text(memoryText, color = textColor, fontWeight = FontWeight.SemiBold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Java Heap", fontSize = 10.sp, color = mutedColor)
            Text(heapText, fontSize = 10.sp, color = textColor)
        }

        LinearProgressIndicator(
            progress = { (snapshot.cpuPercent.coerceAtLeast(0f) / 100f).coerceIn(0f, 1f) },
            color = premiumSignalColor,
            trackColor = premiumSignalColor.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Aktuelle Widget-Last (geschätzt):",
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = mutedColor,
        )

        if (entries.isEmpty()) {
            Text("Keine weiteren Widgets auf dieser Seite", fontSize = 10.sp, color = mutedColor)
            return@Column
        }

        entries.forEach { entry ->
            val safePercent = entry.estimatedLoadPercent.coerceIn(0f, 100f)
            val loadColor = when {
                safePercent >= 30f -> premiumDangerColor
                safePercent >= 15f -> premiumWarningColor
                safePercent >= 8f -> premiumPositiveColor
                else -> premiumSignalColor
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${entry.widgetKindLabel} • ${entry.widgetTitle}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = textColor,
                )
                Text(
                    text = "${safePercent.roundToInt()} %",
                    modifier = Modifier.width(48.dp),
                    maxLines = 1,
                    fontSize = 10.sp,
                    color = loadColor,
                )
            }
            LinearProgressIndicator(
                progress = { (safePercent / 100f).coerceIn(0f, 1f) },
                color = loadColor,
                trackColor = loadColor.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

enum class EchosounderDepthUnit(val label: String) {
    METERS("m"),
    INCH("inch"),
}

data class EchosounderAlarmTone(
    val name: String,
    val frequencyHz: Int,
)

data class EchosounderWidgetSettings(
    val minDepthMeters: Float = 2f,
    val dynamicChangeRateMps: Float = 0.8f,
    val depthUnit: EchosounderDepthUnit = EchosounderDepthUnit.METERS,
    val alarmToneIndex: Int = 0,
)

enum class AnchorWatchDistanceUnit(val label: String, val toFeetFactor: Float) {
    METERS("m", 3.28084f),
    FEET("ft", 1f),
}

enum class AnchorWatchChainUnit(val label: String) {
    METRIC("Metrisch"),
    IMPERIAL("Imperial"),
}

data class AnchorWatchChainStrength(
    val label: String,
    val innerSpacingMm: Float,
)

data class AnchorWatchWidgetSettings(
    val monitoringEnabled: Boolean = false,
    val maxDeviationMeters: Float = DEFAULT_ANCHOR_WATCH_DEVIATION_METERS,
    val maxDeviationPercent: Float = DEFAULT_ANCHOR_WATCH_TOLERANCE_PERCENT,
    val maxDeviationUnit: AnchorWatchDistanceUnit = AnchorWatchDistanceUnit.METERS,
    val chainSensorEnabled: Boolean = true,
    val chainLengthCalibrationEnabled: Boolean = false,
    val calibratedChainLengthMeters: Float = DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS,
    val chainSignalLength: Float = 30f,
    val chainCalibrationUnit: AnchorWatchChainUnit = AnchorWatchChainUnit.METRIC,
    val chainStrengthLabel: String = "10",
    val chainPockets: Int = 6,
    val ringCutoutStartAngleDeg: Float = 0f,
    val ringCutoutSweepAngleDeg: Float = 360f,
    val alarmToneIndex: Int = 0,
)

data class AnchorWatchTelemetry(
    val lengthMeters: Float,
    val rawValue: Float,
    val sourceKey: String,
    val isSignalCount: Boolean = false,
)

val ECHO_SOUNDER_TONES = listOf(
    EchosounderAlarmTone("Nebenhorn", 392),
    EchosounderAlarmTone("Nebelhorn Stoss", 523),
    EchosounderAlarmTone("Sirenen Ton", 784),
    EchosounderAlarmTone("Sirenen-Aufgang", 987),
    EchosounderAlarmTone("Warndreiklang", 1175),
    EchosounderAlarmTone("Wecker kurz", 1479),
    EchosounderAlarmTone("Wecker lang", 1318),
    EchosounderAlarmTone("Marinealarm", 1650),
    EchosounderAlarmTone("Kreischall", 2093),
    EchosounderAlarmTone("Generalalarm", 2637),
)

private val NMEA_PGN_LABELS = mapOf(
    130306 to "Windrichtung",
    127250 to "Kurs/Heading",
    127245 to "Ruder/Autopilot",
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
    60928 to "Gerätekennung",
)

@Composable
private fun premiumWidgetTextColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
private fun premiumWidgetMutedColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun premiumWidgetChipColor(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)

@Composable
private fun premiumWidgetHairlineColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)

private val premiumPositiveColor = SeaFoxDesignTokens.Color.emerald
private val premiumWarningColor = SeaFoxDesignTokens.Color.brass
private val premiumDangerColor = SeaFoxDesignTokens.Color.coral
private val premiumSignalColor = SeaFoxDesignTokens.Color.cyan

@Composable
private fun ScrollableMenuTextContent(
    state: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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
                .padding(end = if (canScroll) 8.dp else 0.dp)
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
                    .padding(vertical = 4.dp)
                    .width(6.dp)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(trackColor, shape = RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(thumbHeight)
                        .align(Alignment.TopEnd)
                        .offset(y = thumbOffset)
                        .background(thumbColor, shape = RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun TemperatureWidget(
    sensors: List<TemperatureSensor>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val rowBackground = premiumWidgetChipColor()
    val normalizedUnit = unit
    val values = sensors
        .take(10)
        .toMutableList()
        .apply {
            while (size < 10) {
                add(TemperatureSensor(name = "Temperatursensor ${size + 1}"))
            }
        }

    val unitLabel = normalizedUnit.label

    Box(modifier = modifier.fillMaxSize()) {
        val headerText = (LocalTextStyle.current.fontSize.value * 1.1f).coerceAtLeast(12f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Temperatur",
                fontWeight = FontWeight.Bold,
                fontSize = headerText.sp,
                color = premiumSignalColor,
            )
            values.forEachIndexed { index, sensor ->
                val name = sensor.name.ifBlank { "Temperatursensor ${index + 1}" }
                val valueText = if (sensor.valueCelsius == null || !sensor.valueCelsius.isFinite()) {
                    "—"
                } else {
                    val displayValue = when (normalizedUnit) {
                        TemperatureUnit.CELSIUS -> sensor.valueCelsius
                        TemperatureUnit.FAHRENHEIT -> (sensor.valueCelsius * 9f / 5f) + 32f
                    }
                    "${(displayValue * 10f).roundToInt() / 10f}$unitLabel"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBackground, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = mutedColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(valueText, color = textColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun BatteryWidget(
    level: Float,
    powerWatts: Float,
    chemistry: BatteryChemistry,
    cellDeltaMillivolts: Float?,
    modifier: Modifier = Modifier,
    symbolBackgroundColor: Color = Color.Transparent,
) {
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val clamped = level.coerceIn(0f, 1f)
    val powerRounded = powerWatts.roundToInt()
    val charging = powerRounded > 0
    val discharging = powerRounded < 0
    val powerColor = when {
        charging -> premiumPositiveColor
        discharging -> premiumDangerColor
        else -> mutedColor
    }
    val powerText = if (charging) "+$powerRounded W" else "$powerRounded W"
    val batteryGreen = premiumPositiveColor

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val iconWidth = minDp(maxWidth * 0.42f, maxHeight * 0.32f) * 1.2f
        val iconHeight = iconWidth * 2f
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(1.dp))
            Box(
                modifier = Modifier
                    .size(iconWidth, iconHeight)
                    .background(symbolBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bodyHeight = h * 0.84f
                    val bodyWidth = w * 0.74f
                    val left = (w - bodyWidth) / 2f
                    val top = h - bodyHeight
                    val stroke = (w * 0.05f).coerceAtLeast(2f)
                    drawRoundRect(
                        batteryGreen,
                        androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke),
                        style = Stroke(width = stroke)
                    )
                    drawRoundRect(
                        batteryGreen,
                        androidx.compose.ui.geometry.Offset(left + stroke, top + stroke + (bodyHeight - stroke * 2f) * (1f - clamped)),
                        size = androidx.compose.ui.geometry.Size(
                            bodyWidth - stroke * 2f,
                            (bodyHeight - stroke * 2f) * clamped
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 0.8f, stroke * 0.8f)
                    )
                    val terminalWidth = bodyWidth * 0.42f
                    val terminalHeight = h * 0.08f
                    drawRoundRect(
                        color = mutedColor.copy(alpha = 0.70f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            (w - terminalWidth) / 2f,
                            (top - terminalHeight * 0.7f).coerceAtLeast(0f)
                        ),
                        size = androidx.compose.ui.geometry.Size(terminalWidth, terminalHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 0.7f, stroke * 0.7f)
                    )
                }
                Text(
                    text = "${(clamped * 100).roundToInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = powerText,
                    color = powerColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (chemistry == BatteryChemistry.LITHIUM && cellDeltaMillivolts != null) {
                    Text(
                        text = "ΔZelle ${cellDeltaMillivolts.roundToInt()} mV",
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun WaterTankWidget(
    level: Float,
    modifier: Modifier = Modifier,
    tankBackgroundColor: Color = Color.Black.copy(alpha = 0.5f),
) {
    TankWidget(
        level = level,
        fillColor = Color(0xFF4A86E8),
        outlineColor = Color(0xFF4A86E8),
        tankBackgroundColor = tankBackgroundColor,
        modifier = modifier,
    )
}

@Composable
fun BlackWaterWidget(
    level: Float,
    modifier: Modifier = Modifier,
    tankBackgroundColor: Color = Color.Black.copy(alpha = 0.5f),
) {
    val gray60 = Color(0xFF999999)
    TankWidget(
        level = level,
        fillColor = gray60,
        outlineColor = gray60,
        tankBackgroundColor = tankBackgroundColor,
        modifier = modifier,
    )
}

@Composable
fun GreyWaterWidget(
    level: Float,
    modifier: Modifier = Modifier,
    tankBackgroundColor: Color = Color.Black.copy(alpha = 0.5f),
) {
    val gray30 = Color(0xFF4D4D4D)
    TankWidget(
        level = level,
        fillColor = gray30,
        outlineColor = gray30,
        tankBackgroundColor = tankBackgroundColor,
        modifier = modifier,
    )
}

@Composable
private fun TankWidget(
    level: Float,
    fillColor: Color,
    outlineColor: Color,
    tankBackgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    modifier: Modifier = Modifier,
) {
    val clamped = level.coerceIn(0f, 1f)
    val tankFillText = "${(clamped * 100).roundToInt()}%"
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tankWidth = maxWidth * 0.65f
        val tankHeight = maxHeight * 0.82f
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(tankWidth, tankHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tankBackgroundColor)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = (size.width * 0.06f).coerceAtLeast(2f)
                    val fillHeight = size.height * clamped
                    val fillTop = (size.height - fillHeight).coerceAtLeast(stroke)
                    drawRoundRect(
                        color = fillColor,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke, fillTop),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke * 2f, fillHeight - stroke)
                    )
                    drawRoundRect(
                        color = outlineColor,
                        size = size,
                        style = Stroke(width = stroke)
                    )
                }
                Text(
                    text = tankFillText,
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GpsWidget(
    data: GpsWidgetData,
    modifier: Modifier = Modifier,
) {
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val chipColor = premiumWidgetChipColor()
    var showGpsSignalDialog by remember { mutableStateOf(false) }
    val latText = formatLatitude(data.latitude)
    val lonText = formatLongitude(data.longitude)
    val speedText = data.speedOverGround?.let { "${(it * 10f).roundToInt() / 10f} kn" } ?: "—"
    val courseText = data.courseOverGround?.let { "${it.roundToInt()}°" } ?: "—"
    val headingText = data.heading?.let { "${it.roundToInt()}°" } ?: "—"
    val altitudeText = data.altitudeM?.let {
        if (it.isFinite()) "${(it * 10f).roundToInt() / 10f} m" else "—"
    } ?: "—"
    val satText = data.satellites?.toString() ?: "—"
    val hdopText = data.hdop?.let {
        if (it.isFinite()) it.roundToInt().toString() else "—"
    } ?: "—"
    val fixText = gpsFixText(data.fixQuality)
    val utcText = data.utcTime?.ifBlank { "—" } ?: "—"

    if (showGpsSignalDialog) {
        AlertDialog(
            onDismissRequest = { showGpsSignalDialog = false },
            title = { Text("GPS Signal") },
            text = {
                Column(
                    modifier = Modifier.padding(menuDialogPadding),
                    verticalArrangement = Arrangement.spacedBy(MENU_SPACING),
                ) {
                    Text("Latitude: $latText")
                    Text("Longitude: $lonText")
                    Text("Kurs über Grund: $courseText")
                    Text("SOG: $speedText")
                    Text("Kurs kompass: $headingText")
                    Text("Höhe: $altitudeText")
                    Text("Satelliten: $satText")
                    Text("HDOP: $hdopText")
                    Text("Fix: $fixText")
                    Text("UTC: $utcText")
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.padding(
                        end = SeaFoxDesignTokens.Size.menuDropdownItemLeadingPaddingEnd,
                        bottom = SeaFoxDesignTokens.Size.menuDropdownItemLeadingPaddingEnd,
                    ),
                    onClick = { showGpsSignalDialog = false },
                    shape = RoundedCornerShape(SeaFoxDesignTokens.Size.menuSmallCornerRadius),
                    colors = SeaFoxDesignTokens.NonMenuButton.textButtonColors(),
                ) {
                    Text("Schließen", color = SeaFoxDesignTokens.NonMenuButton.color)
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LAT", color = mutedColor, fontWeight = FontWeight.SemiBold)
                Text(latText, color = textColor)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LON", color = mutedColor, fontWeight = FontWeight.SemiBold)
                Text(lonText, color = textColor)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("KURS", color = mutedColor, fontWeight = FontWeight.SemiBold)
                Text(courseText, color = textColor)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SOG", color = mutedColor, fontWeight = FontWeight.SemiBold)
                Text(speedText, color = textColor)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("UTC", color = mutedColor, fontWeight = FontWeight.SemiBold)
                Text(utcText, color = textColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                onClick = { showGpsSignalDialog = true },
                colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
            ) {
                Text("GPS Signal")
            }
        }
    }
}

@Composable
fun DalyBmsWidget(
    telemetry: Map<String, Float>,
    telemetryText: Map<String, String>,
    debugMessages: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    darkBackground: Boolean = true,
) {
    val textColor = if (darkBackground) Color.White else Color.Black
    val mutedColor = textColor.copy(alpha = 0.68f)
    val statusText = telemetryText["daly_connection_status"]?.trim()?.ifBlank { "Nicht verbunden" } ?: "Nicht verbunden"
    val batteryState = telemetryText["daly_battery_state"]?.trim()?.ifBlank { "unbekannt" } ?: "unbekannt"
    val lastUpdate = telemetryText["daly_last_update"]?.trim()?.ifBlank { "—" } ?: "—"
    val rawFrame = telemetryText["daly_raw_frame"]?.trim()?.ifBlank { "—" } ?: "—"
    val isConnected = statusText.contains("aktiv", ignoreCase = true) ||
        statusText.contains("prüfung", ignoreCase = true) ||
        statusText.contains("bereit", ignoreCase = true)
    val lastDebugError = debugMessages.firstOrNull { message ->
        message.contains("Fehler", ignoreCase = true) ||
            message.contains("Error", ignoreCase = true) ||
            message.contains("Code", ignoreCase = true) ||
            message.contains("0x", ignoreCase = true) ||
            message.contains("fehl", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("Auth", ignoreCase = true) ||
            message.contains("att", ignoreCase = true)
    }
    val statusColor = if (isConnected) {
        Color(0xFF2ECC71)
    } else if (statusText.contains("fehl", ignoreCase = true)) {
        Color(0xFFFF5A5A)
    } else {
        Color(0xFF8CCEFF)
    }

    val totalVoltage = formatDalyNumeric(telemetry["daly_total_voltage"], "V")
    val current = formatDalyNumeric(telemetry["daly_current"], "A")
    val power = formatDalyNumeric(telemetry["daly_power"], "W")
    val stateOfCharge = formatDalyPercent(telemetry["daly_state_of_charge"])
    val cellCount = formatDalyNumeric(telemetry["daly_cell_count"], "z")
    val remainingCapacity = formatDalyNumeric(telemetry["daly_capacity_remaining"], "Ah")
    val temp1 = formatDalyNumeric(telemetry["daly_temperature_1"], "°C")

    @Composable
    fun RowLine(label: String, value: String, valueColor: Color = textColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = mutedColor, fontWeight = FontWeight.SemiBold)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Status",
                color = statusColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
            )
            if (lastDebugError != null) {
                Text(
                    text = "Letzte Fehlermeldung: $lastDebugError",
                    color = Color(0xFFFF5A5A),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RowLine("Batteriezustand", batteryState)
            RowLine("Spannung", totalVoltage)
            RowLine("Strom", current)
            RowLine("Leistung", power)
            RowLine("SOC", stateOfCharge)
            RowLine("Zellzahl", cellCount)
            RowLine("Restkapazität", remainingCapacity)
            RowLine("Temperatur 1", temp1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Aktualisiert", color = mutedColor, fontWeight = FontWeight.SemiBold)
                Text(lastUpdate, color = textColor)
            }
            Text(
                text = "Rohframe",
                color = mutedColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = rawFrame,
                color = mutedColor,
                fontSize = 10.sp,
                maxLines = 4,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            if (debugMessages.isNotEmpty()) {
                Text(
                    text = "Meldungen",
                    color = mutedColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    debugMessages.forEach { entry ->
                        Text(
                            text = entry,
                            color = mutedColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogWidget(
    currentSpeedKn: Float?,
    topSpeedKn: Float?,
    minSpeedKn: Float?,
    speedTrend: Int,
    dailyNm24h: Float,
    tripNm: Float,
    periodMinutes: Int,
    speedSourceLabel: String,
    modifier: Modifier = Modifier,
) {
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val chipColor = premiumWidgetChipColor()
    val speedDisplayColor = when {
        speedTrend > 0 -> premiumPositiveColor
        speedTrend < 0 -> premiumDangerColor
        else -> textColor
    }
    val speedTitleTextSize =
        (MaterialTheme.typography.bodyMedium.fontSize.value - 4f).coerceAtLeast(8f).sp
    val speedTextSize =
        (MaterialTheme.typography.bodyMedium.fontSize.value + 8f).sp
    val speedText = formatSpeedWithUnit(currentSpeedKn)
    val topText = formatSpeedWithUnit(topSpeedKn)
    val minText = formatSpeedWithUnit(minSpeedKn)
    val dailyText = "${formatNm(dailyNm24h)} NM"
    val tripText = "${formatNm(tripNm)} NM"
    val arrow = when {
        speedTrend > 0 -> "↑"
        speedTrend < 0 -> "↓"
        else -> "→"
    }
    val arrowColor = when {
        speedTrend > 0 -> premiumPositiveColor
        speedTrend < 0 -> premiumDangerColor
        else -> mutedColor
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "SPEED",
                    fontWeight = FontWeight.Bold,
                    color = premiumSignalColor,
                    fontSize = speedTitleTextSize
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    speedSourceLabel,
                    color = mutedColor,
                    fontSize = (speedTitleTextSize.value - 2f).coerceAtLeast(7f).sp,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    speedText,
                    fontWeight = FontWeight.Bold,
                    color = speedDisplayColor,
                    fontSize = speedTextSize
                )
                Spacer(Modifier.weight(1f))
                Text(
                    arrow,
                    color = arrowColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = speedTextSize
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Top ($periodMinutes min)", color = mutedColor)
                Text(topText, color = textColor, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Min", color = mutedColor)
                Text(minText, color = textColor, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tagesmeilen (24h)", color = mutedColor)
                Text(dailyText, color = textColor, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chipColor, RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Trip", color = mutedColor)
                Text(tripText, color = textColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun CompassWidget(
    headingDeg: Float?,
    courseOverGroundDeg: Float?,
    modifier: Modifier = Modifier,
) {
    val heading = headingDeg
        ?.let { wrap360(it) }
        ?.coerceIn(0f, 360f)
    val cog = courseOverGroundDeg?.let { wrap360(it) }?.coerceIn(0f, 360f)
    val headingText = formatAngle(headingDeg)
    val courseText = formatAngle(courseOverGroundDeg)
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val headingColor = premiumSignalColor
    val cogColor = premiumWarningColor

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val diameter = if (widthPx <= heightPx) widthPx else heightPx
        val circleColor = premiumSignalColor.copy(alpha = 0.18f)
        val majorTickColor = textColor.copy(alpha = 0.7f)
        val minorTickColor = textColor.copy(alpha = 0.3f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (diameter * 0.5f) * 0.45f
            val faceRadius = (radius * 0.9f).coerceAtLeast(28f)

            drawCircle(
                color = circleColor,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                radius = faceRadius,
                style = Stroke(width = 2f),
            )

            drawLine(
                color = textColor.copy(alpha = 0.25f),
                start = androidx.compose.ui.geometry.Offset(cx - faceRadius, cy),
                end = androidx.compose.ui.geometry.Offset(cx + faceRadius, cy),
                strokeWidth = 1f,
            )
            drawLine(
                color = textColor.copy(alpha = 0.25f),
                start = androidx.compose.ui.geometry.Offset(cx, cy - faceRadius),
                end = androidx.compose.ui.geometry.Offset(cx, cy + faceRadius),
                strokeWidth = 1f,
            )

            repeat(36) { index ->
                val angle = (index * 10f - 90f).toRadians()
                val outer = if (index % 3 == 0) 1f else 0.66f
                val start = androidx.compose.ui.geometry.Offset(
                    cx + kotlin.math.cos(angle) * (faceRadius - 8f),
                    cy + kotlin.math.sin(angle) * (faceRadius - 8f),
                )
                val end = androidx.compose.ui.geometry.Offset(
                    cx + kotlin.math.cos(angle) * (faceRadius - 8f * outer),
                    cy + kotlin.math.sin(angle) * (faceRadius - 8f * outer),
                )
                drawLine(
                    color = if (index % 3 == 0) majorTickColor else minorTickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (index % 3 == 0) 2f else 1f,
                )
            }

            val labels = listOf(
                0f to "N",
                90f to "E",
                180f to "S",
                270f to "W",
            )
            labels.forEach { (bearing, text) ->
                val angle = (bearing - 90f).toRadians()
                val labelRadius = faceRadius - 24f
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    cx + kotlin.math.cos(angle) * labelRadius,
                    cy + kotlin.math.sin(angle) * labelRadius + 7f,
                    Paint().apply {
                        color = textColor.toArgb()
                        textSize = 16f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
            }

            heading?.let {
                val headRad = (it - 90f).toRadians()
                drawLine(
                    color = headingColor,
                    start = androidx.compose.ui.geometry.Offset(cx, cy),
                    end = androidx.compose.ui.geometry.Offset(
                        cx + kotlin.math.cos(headRad) * faceRadius * 0.78f,
                        cy + kotlin.math.sin(headRad) * faceRadius * 0.78f,
                    ),
                    strokeWidth = 4f,
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "Heading",
                    cx + 10f,
                    cy - 4f,
                    Paint().apply {
                        color = textColor.toArgb()
                        textSize = 10f
                        isAntiAlias = true
                    }
                )
            }

            cog?.let {
                val cogRad = (it - 90f).toRadians()
                drawLine(
                    color = cogColor,
                    start = androidx.compose.ui.geometry.Offset(cx, cy),
                    end = androidx.compose.ui.geometry.Offset(
                        cx + kotlin.math.cos(cogRad) * faceRadius * 0.62f,
                        cy + kotlin.math.sin(cogRad) * faceRadius * 0.62f,
                    ),
                    strokeWidth = 2.5f,
                )
            }

            drawLine(
                color = textColor.copy(alpha = 0.72f),
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(cx, cy + faceRadius + 16f),
                strokeWidth = 1f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                "KOMPASS",
                cx,
                cy + faceRadius + 34f,
                Paint().apply {
                    color = textColor.toArgb()
                    textSize = 12f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
            )
            drawCircle(
                color = textColor,
                radius = 5f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
            drawContext.canvas.nativeCanvas.drawText(
                "H: $headingText",
                12f,
                18f,
                Paint().apply {
                    color = textColor.toArgb()
                    textSize = 14f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
            )
            drawContext.canvas.nativeCanvas.drawText(
                "COG: $courseText",
                12f,
                38f,
                Paint().apply {
                    color = if (cog != null) cogColor.toArgb() else mutedColor.toArgb()
                    textSize = 12f
                    isAntiAlias = true
                }
            )
        }
    }
}

internal fun Float.toRadians(): Float = (this * (Math.PI.toFloat() / 180f))

@Composable
fun WindSettingsDialog(
    settings: WindWidgetSettings,
    onChange: (WindWidgetSettings) -> Unit,
    onDismiss: () -> Unit,
    darkBackground: Boolean = true,
) {
    val menuTextColor = if (darkBackground) Color.White else Color.Black
    val menuScale = 0.7f
    val menuTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = MaterialTheme.typography.bodyMedium.fontSize * menuScale,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * menuScale,
        color = menuTextColor,
    )
    val menuTitleStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = MaterialTheme.typography.titleMedium.fontSize * menuScale,
        lineHeight = MaterialTheme.typography.titleMedium.lineHeight * menuScale,
        color = menuTextColor,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (darkBackground) Color.Black else Color.White,
        titleContentColor = menuTextColor,
        textContentColor = menuTextColor,
        title = { Text("Wind Menü", style = menuTitleStyle) },
        text = {
            val menuScrollState = rememberScrollState()
            ProvideTextStyle(value = menuTextStyle) {
                ScrollableMenuTextContent(
                    state = menuScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .padding(menuDialogPadding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MENU_SPACING)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("1. Windrichtung zum Boot")
                            Switch(
                                checked = settings.showBoatDirection,
                                onCheckedChange = { onChange(settings.copy(showBoatDirection = it)) }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("2. Windrichtung zu Nord")
                            Switch(
                                checked = settings.showNorthDirection,
                                onCheckedChange = { onChange(settings.copy(showNorthDirection = it)) }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("3. Windgeschwindigkeit")
                            Switch(
                                checked = settings.showWindSpeed,
                                onCheckedChange = { onChange(settings.copy(showWindSpeed = it)) }
                            )
                        }

                        Text("3.1 Einheit")
                        WindSpeedUnit.entries.forEach { unit ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = settings.speedUnit == unit,
                                    onClick = { onChange(settings.copy(speedUnit = unit)) },
                                    colors = SeaFoxDesignTokens.LinkControl.radioButtonColors()
                                )
                                Text(unit.label)
                            }
                        }

                        Text("4. Wendewinkel (${settings.tackingAngleDeg}°)")
                        Slider(
                            value = settings.tackingAngleDeg.toFloat(),
                            onValueChange = { value ->
                                onChange(settings.copy(tackingAngleDeg = value.roundToInt().coerceIn(50, 140)))
                            },
                            valueRange = 50f..140f,
                            steps = 89
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = SeaFoxDesignTokens.NonMenuButton.textButtonColors(),
            ) { Text("Schließen", style = menuTextStyle, color = SeaFoxDesignTokens.NonMenuButton.color) }
        },
    )
}

private data class EchosounderHistoryPoint(
    val timestampMs: Long,
    val distanceM: Float,
    val depthM: Float,
)

@Composable
fun EchosounderWidget(
    currentDepthM: Float,
    depthSettings: EchosounderWidgetSettings,
    sogKn: Float?,
    isMuted: Boolean = false,
    modifier: Modifier = Modifier,
    widgetTitle: String = "Echosounder",
    alarmToneVolume: Float = 0.7f,
    alarmRepeatIntervalSeconds: Int = 5,
    onEditMinDepth: () -> Unit = {},
    onEditDynamic: () -> Unit = {},
    onAlarmTonePlayed: (String) -> Unit = {},
    onAlarmStateChange: (Boolean) -> Unit = {},
) {
    val toneIndex = depthSettings.alarmToneIndex.coerceIn(0, ECHO_SOUNDER_TONES.lastIndex)
    val depthHistory = remember { mutableStateListOf<EchosounderHistoryPoint>() }
    var smoothedDepth by remember { mutableFloatStateOf(0f) }
    var previousSmoothedDepth by remember { mutableFloatStateOf(0f) }
    var lastDepthSampleMs by remember { mutableStateOf(0L) }
    var renderFrameTimestampMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val alarmDepthTopPaddingPx = with(LocalDensity.current) { 20.dp.toPx() }
    val dynamicRate = depthSettings.dynamicChangeRateMps.coerceAtLeast(0f)
    val sogMs = sogKn?.times(0.514_444_4f) ?: 0f

    var depthHistoryLength by remember { mutableIntStateOf(0) }
    var isAlarm by remember { mutableStateOf(false) }
    var alarmThresholdMeters by remember(depthSettings.minDepthMeters) {
        mutableFloatStateOf(depthSettings.minDepthMeters)
    }

    LaunchedEffect(currentDepthM, sogKn) {
        val nowMs = System.currentTimeMillis()
        val validDepth = currentDepthM.takeIf { it.isFinite() && it > 0f } ?: return@LaunchedEffect
        val depthM = validDepth.coerceIn(0.1f, 500f)
        val last = depthHistory.lastOrNull()
        val deltaTimeMs = if (lastDepthSampleMs == 0L) 1000L else (nowMs - lastDepthSampleMs).coerceAtLeast(16L)
        val filterTauMs = 1000f
        val smoothingFactor = (deltaTimeMs.toFloat() / filterTauMs).coerceIn(0f, 1f)
        previousSmoothedDepth = smoothedDepth
        if (lastDepthSampleMs == 0L || smoothedDepth <= 0f) {
            smoothedDepth = depthM
        } else {
            smoothedDepth += (depthM - smoothedDepth) * smoothingFactor
        }
        val deltaDistanceM = if (last == null) {
            0f
        } else {
            val elapsedSeconds = deltaTimeMs.toFloat() / 1000f
            val baseDelta = sogMs.coerceAtLeast(0f) * elapsedSeconds
            if (baseDelta > 0.05f) baseDelta else elapsedSeconds * 0.6f
        }

        val nextDistance = (last?.distanceM ?: 0f) + deltaDistanceM
        depthHistory.add(EchosounderHistoryPoint(nowMs, nextDistance, smoothedDepth))
        if (depthHistory.size > 240) {
            val removeCount = depthHistory.size - 240
            repeat(removeCount) { if (depthHistory.isNotEmpty()) depthHistory.removeAt(0) }
        }

        val oneMinuteAgo = nowMs - 90_000L
        depthHistory.removeAll { it.timestampMs < oneMinuteAgo }
        depthHistoryLength = depthHistory.size

        val previous = depthHistory.getOrNull(depthHistory.size - 2)
        val descentRateMps = if (previous != null && deltaTimeMs > 0L) {
            ((previousSmoothedDepth - smoothedDepth).coerceAtLeast(0f) / (deltaTimeMs / 1000f))
        } else {
            0f
        }

        val dynamicLead = ((descentRateMps - dynamicRate).coerceAtLeast(0f) * 0.6f).coerceIn(0f, 12f)
        val alarmThreshold = depthSettings.minDepthMeters + dynamicLead
        isAlarm = smoothedDepth <= alarmThreshold
        alarmThresholdMeters = alarmThreshold
        lastDepthSampleMs = nowMs
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100L)
            renderFrameTimestampMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(
        isAlarm,
        toneIndex,
        isMuted,
        alarmToneVolume,
        alarmRepeatIntervalSeconds,
        widgetTitle,
    ) {
        if (!isAlarm || isMuted) return@LaunchedEffect

        val alarmFrequency = ECHO_SOUNDER_TONES
            .getOrNull(toneIndex)
            ?.frequencyHz
            ?: ECHO_SOUNDER_TONES.first().frequencyHz
        val repeatIntervalMs = alarmRepeatIntervalSeconds.coerceIn(2, 10) * 1000L

        while (true) {
            playEchosounderAlarmTone(
                frequencyHz = alarmFrequency,
                durationMs = 400,
                volume = alarmToneVolume,
            )
            onAlarmTonePlayed(widgetTitle)
            delay(repeatIntervalMs)
        }
    }

    LaunchedEffect(isAlarm) {
        onAlarmStateChange(isAlarm)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = "Tiefe: ${formatEchosounderDepth(smoothedDepth, depthSettings.depthUnit)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value + 1f).sp,
                )
                Text(
                    text = "Alarm: ${
                        formatEchosounderDepth(alarmThresholdMeters.coerceAtLeast(0f), depthSettings.depthUnit)
                    }",
                    color = if (isAlarm) Color(0xFFFF6666) else Color.White,
                    fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value + 1f).sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxSize()) {
                val depthPathColor = if (isAlarm) Color(0xFFFF5555) else Color.White
                val depthLabelTextSizePx = with(LocalDensity.current) { 9.sp.toPx() }
                val alarmDepthLabelTextSizePx = with(LocalDensity.current) { 10.sp.toPx() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val renderTimestampMs = renderFrameTimestampMs
                    val lastPoint = depthHistory.lastOrNull()
                    if (lastPoint == null) return@Canvas

                    val interpolatedPoint = run {
                        if (depthHistory.size < 2) {
                            EchosounderHistoryPoint(
                                timestampMs = renderTimestampMs,
                                distanceM = lastPoint.distanceM,
                                depthM = lastPoint.depthM,
                            )
                        } else {
                            val previousPoint = depthHistory[depthHistory.size - 2]
                            val elapsedSinceLastMs = (renderTimestampMs - lastPoint.timestampMs).coerceAtLeast(0L)
                            val spanMs = (lastPoint.timestampMs - previousPoint.timestampMs).coerceAtLeast(1L)
                            val slope = (lastPoint.depthM - previousPoint.depthM) / spanMs.toFloat()
                            val projectedDepth = lastPoint.depthM + (slope * elapsedSinceLastMs.toFloat())
                            val projectedDistance = if (sogMs > 0f) {
                                lastPoint.distanceM + (sogMs * (elapsedSinceLastMs.toFloat() / 1000f))
                            } else {
                                lastPoint.distanceM
                            }
                            EchosounderHistoryPoint(
                                timestampMs = renderTimestampMs,
                                distanceM = projectedDistance,
                                depthM = projectedDepth.coerceIn(0.1f, 500f),
                            )
                        }
                    }
                    val renderHistory = depthHistory + interpolatedPoint

                    if (renderHistory.size < 2) return@Canvas

                    val minTimeDistance = renderHistory.firstOrNull()?.distanceM ?: 0f
                    val maxTimeDistance = renderHistory.lastOrNull()?.distanceM ?: 0f
                    val distanceSpan = (maxTimeDistance - minTimeDistance).coerceAtLeast(1f)
                    val maxDepth = renderHistory.maxOfOrNull { it.depthM } ?: 0f
                    val displayDepthMin = 5f
                    val displayAlarmDepth = alarmThresholdMeters.coerceAtLeast(0f)
                    val displayDepthMax = maxOf(displayDepthMin, maxDepth, displayAlarmDepth)
                    val displayRange = (displayDepthMax - displayDepthMin).coerceAtLeast(0.5f)
                    val alarmLineDepth = displayAlarmDepth.coerceIn(displayDepthMin, displayDepthMax)

                    val left = size.width * 0.06f
                    val right = size.width * 0.06f
                    val top = maxOf(size.height * 0.05f, alarmDepthTopPaddingPx)
                    val bottom = size.height * 0.15f
                    val width = size.width - left - right
                    val height = size.height - top - bottom

                    fun depthToY(depth: Float): Float {
                        val safeDepth = depth.coerceIn(displayDepthMin, displayDepthMax)
                        return top + ((safeDepth - displayDepthMin) / displayRange) * height
                    }

                    drawRect(
                        color = Color(0xFF1A1A1A),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.08f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.9f),
                    )

                    val points = renderHistory.map { point ->
                        val x = left + ((point.distanceM - minTimeDistance) / distanceSpan) * width
                        val y = depthToY(point.depthM)
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    for (index in 1 until points.size) {
                        val from = points[index - 1]
                        val to = points[index]
                        drawLine(
                            color = depthPathColor,
                            start = from,
                            end = to,
                            strokeWidth = 3f
                        )
                    }

                    val firstGridLine = kotlin.math.floor(displayDepthMin / 5f).toInt()
                    val lastGridLine = kotlin.math.floor(displayDepthMax / 5f).toInt()
                    var alarmLineAlreadyDrawn = false
                    for (lineIndex in firstGridLine..lastGridLine) {
                        val lineDepth = (lineIndex * 5f).coerceAtLeast(displayDepthMin)
                        val y = depthToY(lineDepth)
                        val isAlarmLine = kotlin.math.abs(lineDepth - alarmLineDepth) <= 0.2f
                        val lineColor = if (isAlarmLine) {
                            android.graphics.Color.RED
                        } else {
                            0x4DFFFFFF
                        }
                        if (isAlarmLine) alarmLineAlreadyDrawn = true
                        drawLine(
                            color = Color(lineColor),
                            start = androidx.compose.ui.geometry.Offset(left, y),
                            end = androidx.compose.ui.geometry.Offset(left + width, y),
                            strokeWidth = if (isAlarmLine) 2.2f else 1.2f
                        )
                        val label = if (isAlarmLine) {
                            formatEchosounderDepth(displayAlarmDepth, depthSettings.depthUnit)
                        } else {
                            "${lineDepth.toInt()}m"
                        }
                        val paint = Paint().apply {
                            color = if (isAlarmLine) android.graphics.Color.RED else android.graphics.Color.argb(
                                179,
                                255,
                                255,
                                255
                            )
                            textSize = if (isAlarmLine) alarmDepthLabelTextSizePx else depthLabelTextSizePx
                            textAlign = if (isAlarmLine) Paint.Align.RIGHT else Paint.Align.RIGHT
                            isAntiAlias = true
                            isFakeBoldText = isAlarmLine
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            left + width + 6f,
                            y + 6f,
                            paint,
                        )
                    }

                    if (!alarmLineAlreadyDrawn) {
                        val alarmLineY = depthToY(alarmLineDepth)
                        drawLine(
                            color = Color(0xFFFF5555),
                            start = androidx.compose.ui.geometry.Offset(left, alarmLineY),
                            end = androidx.compose.ui.geometry.Offset(left + width, alarmLineY),
                            strokeWidth = 2.5f
                        )
                        val paint = Paint().apply {
                            color = android.graphics.Color.RED
                            textSize = alarmDepthLabelTextSizePx
                            textAlign = Paint.Align.RIGHT
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            formatEchosounderDepth(displayAlarmDepth, depthSettings.depthUnit),
                            left + width + 6f,
                            alarmLineY + 6f,
                            paint,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(MENU_SPACING)
            ) {
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = onEditMinDepth,
                    modifier = Modifier.weight(1f),
                    colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
                ) {
                    Text("Tiefenalarm")
                }
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = onEditDynamic,
                    modifier = Modifier.weight(1f),
                    colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
                ) {
                    Text("Tiefendynamik")
                }
            }
        }
    }
}

@Composable
fun AnchorWatchWidget(
    monitoringEnabled: Boolean,
    anchorSet: Boolean,
    chainLengthMeters: Float?,
    currentDistanceMeters: Float?,
    boatBearingDeg: Float?,
    alertRadiusMeters: Float?,
    alarmActive: Boolean,
    displayUnit: AnchorWatchDistanceUnit,
    unitLabel: String,
    darkBackground: Boolean,
    ringCutoutStartAngleDeg: Float = 0f,
    ringCutoutSweepAngleDeg: Float = 360f,
    onRingCutoutDrag: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val bg = Color.Transparent
    val alarmColor = if (alarmActive) Color(0xFFFF6B6B) else Color(0xFF6BE58F)
    val textColor = if (darkBackground) Color.White else Color.Black
    val widgetBackgroundColor = if (darkBackground) Color.Black else Color.White

    fun formatDistance(value: Float?, fallback: String = "—"): String {
        if (value == null || !value.isFinite()) return fallback
        val converted = if (displayUnit == AnchorWatchDistanceUnit.METERS) value else value * AnchorWatchDistanceUnit.METERS.toFeetFactor
        val decimals = if (converted < 100f) 1 else 0
        val factor = if (decimals == 0) 1f else 10f
        val shown = (converted * factor).roundToInt().toFloat() / factor
        return "$shown $unitLabel"
    }

    val chainText = formatDistance(chainLengthMeters, "—")
    val alarmText = formatDistance(alertRadiusMeters, "—")
    val currentText = formatDistance(currentDistanceMeters, "—")
    val monitoringText = if (monitoringEnabled) "AN" else "AUS"
    val statusText = if (!anchorSet || alarmActive) {
        "Warnung"
    } else {
        "OK"
    }
    val monitoringColor = if (monitoringEnabled) Color(0xFF2ECC71) else Color.Gray
    val statusColor = if (!anchorSet) Color.Gray else if (alarmActive) Color(0xFFFF6B6B) else Color(0xFF2ECC71)

    val innerRadiusRatio = 0.20f

    val chainFillColor = Color(0xFF57D163).copy(alpha = 0.5f)
    val toleranceFillColor = Color(0xFF082A74)
    val defaultLineColor = Color(0xFF2A6BFF)
    val density = LocalDensity.current
    val centerDotRadiusPx = with(density) { 2.5f.dp.toPx() }
    val fullCircleDegrees = 360f
    val fullCircleDragResetThresholdDeg = 2.5f
    val fullCircleReopenRadiusPx = with(density) { 30.dp.toPx() }
    val fullCircleReopenStartAngle = 160f
    val fullCircleReopenEndAngle = 200f
    val fullCircleReopenStartAngleCanvas = ((fullCircleReopenStartAngle + 270f) % 360f + 360f) % 360f
    val fullCircleReopenEndAngleCanvas = ((fullCircleReopenEndAngle + 270f) % 360f + 360f) % 360f
    val fullCircleReopenTapTolerancePx = with(density) { 10.dp.toPx() }
    val cardinalLabelTextSizePx = with(density) { 12.sp.toPx() }
    val cardinalLabelOffsetPx = with(density) { 10.dp.toPx() }
    val dragActivationPx = with(density) { 8.dp.toPx() }

    fun normalizeAngle(angle: Float): Float {
        val normalized = angle % 360f
        return if (normalized < 0f) normalized + 360f else normalized
    }

    fun normalizeCutoutSweep(sweep: Float): Float {
        if (!sweep.isFinite()) return 360f
        val absSweep = sweep.absoluteValue
        return if (
            absSweep <= fullCircleDragResetThresholdDeg ||
            absSweep >= (fullCircleDegrees - fullCircleDragResetThresholdDeg)
        ) {
            fullCircleDegrees
        } else {
            sweep
        }
    }

    fun signedAngleDelta(fromAngle: Float, toAngle: Float): Float {
        val start = normalizeAngle(fromAngle)
        val end = normalizeAngle(toAngle)
        val rawDelta = ((end - start + 540f) % 360f) - 180f
        return if (rawDelta == -180f) 180f else rawDelta
    }

    fun isAngleWithinArc(angle: Float, arcStart: Float, arcEnd: Float): Boolean {
        val normalized = normalizeAngle(angle)
        val normalizedStart = normalizeAngle(arcStart)
        val normalizedEnd = normalizeAngle(arcEnd)
        return if (normalizedStart <= normalizedEnd) {
            normalized in normalizedStart..normalizedEnd
        } else {
            normalized >= normalizedStart || normalized <= normalizedEnd
        }
    }

    var ringCenter by remember { mutableStateOf(Offset.Zero) }
    var ringMaxRadius by remember { mutableFloatStateOf(0f) }
    var ringInnerRadius by remember { mutableFloatStateOf(0f) }
    var cutoutStartAngle by remember(ringCutoutStartAngleDeg) {
        mutableFloatStateOf(((ringCutoutStartAngleDeg % 360f) + 360f) % 360f)
    }
    var cutoutSweepAngle by remember(ringCutoutSweepAngleDeg) {
        mutableFloatStateOf(
            run {
                val normalized = ((ringCutoutSweepAngleDeg % 360f) + 360f) % 360f
                if (!ringCutoutSweepAngleDeg.isFinite() || normalized <= 0.001f || normalized >= 359.999f) {
                    360f
                } else {
                    normalized
                }
            }
        )
    }

    fun angleOfOffset(position: Offset): Float {
        val dx = position.x - ringCenter.x
        val dy = position.y - ringCenter.y
        val raw = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI.toDouble()).toFloat()
        return normalizeAngle(raw)
    }
    val normalizedCutoutStartAngle = ((cutoutStartAngle % 360f) + 360f) % 360f
    val normalizedCutoutSweepAngle = normalizeCutoutSweep(cutoutSweepAngle)
    val isFullRingCircle = normalizedCutoutSweepAngle >= 359.999f
    val isChainLengthBelowCutoutLimit = chainLengthMeters != null && chainLengthMeters.isFinite() && chainLengthMeters < 3f

    fun setRingCutout(startAngle: Float, sweepAngle: Float) {
        cutoutStartAngle = normalizeAngle(startAngle)
        cutoutSweepAngle = normalizeCutoutSweep(sweepAngle)
        onRingCutoutDrag(
            cutoutStartAngle,
            cutoutSweepAngle.absoluteValue,
        )
    }

    fun resetRingCutoutToFull() {
        setRingCutout(0f, fullCircleDegrees)
    }

    fun isInReopenHandle(position: Offset): Boolean {
        val dx = position.x - ringCenter.x
        val dy = position.y - ringCenter.y
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (distance <= 0f) return false
        val angle = angleOfOffset(position)
        val radiusDelta = abs(distance - fullCircleReopenRadiusPx)
        return radiusDelta <= fullCircleReopenTapTolerancePx && isAngleWithinArc(
            angle,
            fullCircleReopenStartAngleCanvas,
            fullCircleReopenEndAngleCanvas
        )
    }

    fun isPointInsideRing(position: Offset): Boolean {
        if (ringMaxRadius <= 0f) return false
        val dx = position.x - ringCenter.x
        val dy = position.y - ringCenter.y
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        return distance <= ringMaxRadius
    }

    fun DrawScope.drawCardinalLabels(outerRadiusPx: Float) {
        val labelPaint = Paint().apply {
            color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = cardinalLabelTextSizePx
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val labels = listOf(
            "N" to 270f,
            "E" to 0f,
            "S" to 90f,
            "W" to 180f,
        )
        for ((label, angleDeg) in labels) {
            val angle = angleDeg * PI.toFloat() / 180f
            val x = ringCenter.x + cos(angle) * (outerRadiusPx + cardinalLabelOffsetPx)
            val y = ringCenter.y + sin(angle) * (outerRadiusPx + cardinalLabelOffsetPx) + (labelPaint.textSize / 3f)
            drawContext.canvas.nativeCanvas.drawText(label, x, y, labelPaint)
        }
    }

    LaunchedEffect(isChainLengthBelowCutoutLimit) {
        if (isChainLengthBelowCutoutLimit) {
            resetRingCutoutToFull()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .border(
                width = if (alarmActive) 2.dp else 0.dp,
                color = if (alarmActive) Color(0xFFFFA726) else Color.Transparent,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kette", color = textColor)
                Text(chainText, color = textColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Distanz", color = textColor)
                Text(currentText, color = if (alarmActive) Color(0xFFFF6B6B) else textColor)
            }
            Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                Canvas(modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(1f)
                    .zIndex(100f)
                    .onGloballyPositioned { coordinates ->
                        val measuredSize = coordinates.size
                        ringCenter = Offset(measuredSize.width / 2f, measuredSize.height / 2f)
                        ringMaxRadius = (min(measuredSize.width.toFloat(), measuredSize.height.toFloat()) / 2f) * 0.9f
                        ringInnerRadius = ringMaxRadius * innerRadiusRatio
                    }
                    .pointerInput(
                        isChainLengthBelowCutoutLimit,
                        ringCenter,
                        ringMaxRadius,
                        ringInnerRadius,
                    ) {
                        var startPosition = Offset.Zero
                        var startAngle = 0f
                        var currentAngle = 0f
                        var hasInitialAngle = false
                        var moved = false
                        var reopenTap = false

                        detectDragGestures(
                            onDragStart = { down ->
                                if (isChainLengthBelowCutoutLimit) {
                                    resetRingCutoutToFull()
                                    return@detectDragGestures
                                }

                                if (ringMaxRadius <= 0f || ringInnerRadius <= 0f) {
                                    return@detectDragGestures
                                }

                                if (!isPointInsideRing(down) ) {
                                    return@detectDragGestures
                                }

                                startPosition = down
                                startAngle = angleOfOffset(down)
                                currentAngle = startAngle
                                hasInitialAngle = true
                                reopenTap = isInReopenHandle(down) && !isFullRingCircle
                                moved = false
                            },
                            onDrag = { change, _ ->
                                val dx = change.position.x - startPosition.x
                                val dy = change.position.y - startPosition.y
                                moved = moved || (dx * dx + dy * dy >= dragActivationPx * dragActivationPx)
                                if (isPointInsideRing(change.position)) {
                                    val insideAngle = angleOfOffset(change.position)
                                    if (!hasInitialAngle) {
                                        startAngle = insideAngle
                                        hasInitialAngle = true
                                    }
                                    currentAngle = insideAngle
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                if (moved) {
                                    val sweepAngle = if (hasInitialAngle) {
                                        signedAngleDelta(startAngle, currentAngle)
                                    } else {
                                        0f
                                    }
                                    if (sweepAngle > 0f) {
                                        setRingCutout(startAngle, sweepAngle)
                                    } else {
                                        resetRingCutoutToFull()
                                    }
                                } else if (reopenTap) {
                                    resetRingCutoutToFull()
                                }
                            },
                            onDragCancel = {
                                if (moved) {
                                    val sweepAngle = if (hasInitialAngle) {
                                        signedAngleDelta(startAngle, currentAngle)
                                    } else {
                                        0f
                                    }
                                    if (sweepAngle > 0f) {
                                        setRingCutout(startAngle, sweepAngle)
                                    } else {
                                        resetRingCutoutToFull()
                                    }
                                } else if (reopenTap) {
                                    resetRingCutoutToFull()
                                }
                            },
                        )
                    }) {
                    val maxRadius = (size.minDimension / 2f) * 0.9f
                    val innerRadius = maxRadius * innerRadiusRatio

                    val toleranceOuterRadius = maxRadius
                    val chainLengthRatio = if (chainLengthMeters != null && chainLengthMeters > 0f && alertRadiusMeters != null && alertRadiusMeters > 0f) {
                        (chainLengthMeters / alertRadiusMeters).coerceIn(0f, 1f)
                    } else {
                        1f
                    }

                    val chainRadius = (innerRadius + (toleranceOuterRadius - innerRadius) * chainLengthRatio).coerceIn(
                        innerRadius + 2f,
                        toleranceOuterRadius,
                    )
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    val outerTopLeft = Offset(
                        x = size.width / 2f - toleranceOuterRadius,
                        y = size.height / 2f - toleranceOuterRadius,
                    )
                    val outerSize = Size(toleranceOuterRadius * 2f, toleranceOuterRadius * 2f)
                    val chainTopLeft = Offset(
                        x = size.width / 2f - chainRadius,
                        y = size.height / 2f - chainRadius,
                    )
                    val chainSize = Size(chainRadius * 2f, chainRadius * 2f)
                    val visibleSweep = if (isFullRingCircle) {
                        fullCircleDegrees
                    } else {
                        (fullCircleDegrees - normalizedCutoutSweepAngle).coerceAtLeast(0f)
                    }
                    val visibleStart = if (isFullRingCircle) {
                        0f
                    } else {
                        normalizeAngle(normalizedCutoutStartAngle + normalizedCutoutSweepAngle)
                    }

                    drawCircle(color = widgetBackgroundColor, radius = innerRadius)
                    if (visibleSweep >= fullCircleDegrees - 0.001f) {
                        drawCircle(color = toleranceFillColor, radius = toleranceOuterRadius)
                        drawCircle(color = chainFillColor, radius = chainRadius)
                    } else {
                        drawArc(
                            color = toleranceFillColor,
                            startAngle = visibleStart,
                            sweepAngle = visibleSweep,
                            useCenter = true,
                            topLeft = outerTopLeft,
                            size = outerSize,
                        )
                        if (chainRadius > innerRadius) {
                            drawArc(
                                color = chainFillColor,
                                startAngle = visibleStart,
                                sweepAngle = visibleSweep,
                                useCenter = true,
                                topLeft = chainTopLeft,
                                size = chainSize,
                            )
                        }
                    }
                    drawCardinalLabels(toleranceOuterRadius)
                    if (anchorSet) {
                        val anchorCenter = Offset(centerX, centerY)
                        drawCircle(
                            color = defaultLineColor,
                            radius = centerDotRadiusPx,
                            center = anchorCenter,
                        )
                        val anchorLabelPaint = Paint().apply {
                            color = Color.White.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = centerDotRadiusPx * 4f
                            isFakeBoldText = true
                            isAntiAlias = true
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "A",
                            centerX,
                            centerY + anchorLabelPaint.textSize / 3f,
                            anchorLabelPaint,
                        )
                        if (currentDistanceMeters != null && currentDistanceMeters.isFinite() && boatBearingDeg != null && boatBearingDeg.isFinite()) {
                            val maxVisualRadius = if (alertRadiusMeters != null && alertRadiusMeters > 0f) {
                                alertRadiusMeters
                            } else if (chainLengthMeters != null && chainLengthMeters > 0f) {
                                chainLengthMeters
                            } else {
                                1f
                            }
                            val boatDistanceRatio = (currentDistanceMeters / maxVisualRadius).coerceAtLeast(0f)
                            val boatRangeRadiusPx = (boatDistanceRatio * toleranceOuterRadius).coerceIn(0f, toleranceOuterRadius)
                            val boatAngle = Math.toRadians((boatBearingDeg - 90f).toDouble()).toFloat()
                            val boatPosition = Offset(
                                centerX + cos(boatAngle) * boatRangeRadiusPx,
                                centerY + sin(boatAngle) * boatRangeRadiusPx,
                            )
                            drawLine(
                                color = defaultLineColor,
                                start = anchorCenter,
                                end = boatPosition,
                                strokeWidth = 2.5f,
                            )
                            val boatSymbolHalf = with(density) { 10.5f.dp.toPx() }
                            val dxToAnchor = centerX - boatPosition.x
                            val dyToAnchor = centerY - boatPosition.y
                            val toAnchorLen = hypot(dxToAnchor, dyToAnchor).coerceAtLeast(1f)
                            val dirToAnchorX = dxToAnchor / toAnchorLen
                            val dirToAnchorY = dyToAnchor / toAnchorLen
                            val sideNormalX = -dirToAnchorY
                            val sideNormalY = dirToAnchorX
                            val boatApexOffset = boatSymbolHalf * 1.15f
                            val boatBaseOffset = boatSymbolHalf * 0.35f
                            val boatBaseHalfWidth = boatSymbolHalf * 0.33f
                            val boatTip = Offset(
                                boatPosition.x + dirToAnchorX * boatApexOffset,
                                boatPosition.y + dirToAnchorY * boatApexOffset,
                            )
                            val baseCenter = Offset(
                                boatPosition.x - dirToAnchorX * boatBaseOffset,
                                boatPosition.y - dirToAnchorY * boatBaseOffset,
                            )
                            val boatLeft = Offset(
                                baseCenter.x + sideNormalX * boatBaseHalfWidth,
                                baseCenter.y + sideNormalY * boatBaseHalfWidth,
                            )
                            val boatRight = Offset(
                                baseCenter.x - sideNormalX * boatBaseHalfWidth,
                                baseCenter.y - sideNormalY * boatBaseHalfWidth,
                            )
                            val boatPath = Path().apply {
                                moveTo(boatTip.x, boatTip.y)
                                lineTo(boatLeft.x, boatLeft.y)
                                lineTo(boatRight.x, boatRight.y)
                                close()
                            }
                            drawPath(boatPath, color = Color.Yellow)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monitoringText,
                    color = monitoringColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EngineRpmWidget(rpm: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${rpm.roundToInt()} rpm")
        Spacer(Modifier.height(8.dp))
        val normalized = (rpm / 7000f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { normalized },
            modifier = Modifier.fillMaxWidth(0.92f)
        )
    }
}

@Composable
fun AisWidget(
    targets: List<AisTargetWithAge>,
    ownHeadingDeg: Float?,
    ownSpeedKn: Float?,
    settings: AisWidgetSettings,
    renderNowMs: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier,
    widgetTitle: String = "AIS",
    ownLatitudeDeg: Float? = null,
    ownLongitudeDeg: Float? = null,
    alarmToneVolume: Float = 0.7f,
    alarmRepeatIntervalSeconds: Int = 5,
    isMuted: Boolean = false,
    darkBackground: Boolean = true,
    onIncreaseRange: () -> Unit,
    onDecreaseRange: () -> Unit,
    onCallMmsi: (String) -> Unit = {},
    onAlarmTonePlayed: (String) -> Unit = {},
    onAlarmStateChange: (Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    var selectedTarget by remember { mutableStateOf<RenderedAisTarget?>(null) }
    var selectedTargetId by remember { mutableStateOf<String?>(null) }
    var overlapMagnifier by remember { mutableStateOf<AisOverlapMagnifier?>(null) }
    val context = LocalContext.current
    val visibleRangeNm = settings.displayRangeNm.takeIf { it.isFinite() }?.coerceAtLeast(5f) ?: 5f
    val canIncreaseRange = visibleRangeNm.isFinite()
    val canDecreaseRange = visibleRangeNm > 5f

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val mapRadiusPx = minOf(widthPx, heightPx) * 0.46f
            val magnifierRadiusPx = minOf(widthPx, heightPx) * 0.30f
            val overlapDetectionRadiusPx = with(density) { 28.dp.toPx() }
            val tapFallbackRadiusPx = with(density) { 40.dp.toPx() }
            val magnifierScale = 2.2f
            val overlapCloseButtonRadiusPx = with(density) { 10.dp.toPx() }
            val baseFont = LocalTextStyle.current
            val textSizePx = with(density) { baseFont.fontSize.toPx() }
            val fontOffsetPx = with(density) { settings.fontSizeOffsetSp.sp.toPx() }
            val titleTextPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = textSizePx + fontOffsetPx
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
                typeface = ResourcesCompat.getFont(context, R.font.orbitron_variable)
                    ?: Typeface.DEFAULT
            }

            val rangeNm = visibleRangeNm
            val rangeForRender = rangeNm
            val renderTargets = remember(
                targets,
                widthPx,
                heightPx,
                rangeForRender,
                ownHeadingDeg,
                ownLatitudeDeg,
                ownLongitudeDeg,
                settings.northUp,
                settings.fontSizeOffsetSp,
                settings.cpaAlarmDistanceNm,
                settings.cpaAlarmMinutes,
                renderNowMs,
            ) {
            mapAisTargetsToScreen(
                widthPx = widthPx,
                heightPx = heightPx,
                ownHeadingDeg = ownHeadingDeg,
                ownLatitudeDeg = ownLatitudeDeg,
                ownLongitudeDeg = ownLongitudeDeg,
                maxRangeNm = rangeForRender,
                mapRadiusPx = mapRadiusPx,
                targets = targets,
                alarmDistanceNm = settings.cpaAlarmDistanceNm,
                alarmMinutes = settings.cpaAlarmMinutes,
                northUp = settings.northUp,
                currentTimeMs = renderNowMs,
                targetStaleMs = { state ->
                    minOf(state.visibilityTimeoutMinutes, AIS_TARGET_STALE_WARNING_MINUTES)
                        .coerceAtLeast(1)
                        .toLong() * 60_000L
                },
            )
        }

            fun resolveMagnifierPoint(
                point: androidx.compose.ui.geometry.Offset,
                lens: AisOverlapMagnifier?,
                targetIndex: Int,
                focusTargetIndex: Int?,
                focusTargetPosition: androidx.compose.ui.geometry.Offset?,
            ): Pair<androidx.compose.ui.geometry.Offset, Boolean> {
                if (!lens?.targetIndexes.orEmpty().contains(targetIndex)) return point to false
                if (lens == null) return point to false
                if (focusTargetIndex == targetIndex) {
                    return (focusTargetPosition ?: point) to false
                }
                if (focusTargetPosition == null) {
                    val dx = point.x - lens.center.x
                    val dy = point.y - lens.center.y
                    val radius = lens.radiusPx
                    if (dx * dx + dy * dy > radius * radius) return point to false
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance <= 1f) return lens.center to false
                    val normalizedDistance = (distance / radius).coerceIn(0f, 1f)
                    val easedDistance = kotlin.math.sqrt(normalizedDistance)
                    val magnifierSpread = (1f + (lens.scale - 1f) * 0.95f) * 3.8f
                    val mappedDistanceRatio = (easedDistance * magnifierSpread).coerceAtMost(1f)
                    val ratio = (mappedDistanceRatio * radius) / distance
                    return androidx.compose.ui.geometry.Offset(
                        lens.center.x + dx * ratio,
                        lens.center.y + dy * ratio,
                    ) to false
                }

                val focusDx = point.x - focusTargetPosition.x
                val focusDy = point.y - focusTargetPosition.y
                val focusDistance = kotlin.math.sqrt(focusDx * focusDx + focusDy * focusDy)
                if (focusDistance <= 1f) return (focusTargetPosition) to false
                val angle = kotlin.math.atan2(focusDy, focusDx)
                val spreadMultiplier = (1f + (lens.scale - 1f) * 1.35f) * 4.0f
                val minSpreadDistancePx = with(density) { 44.dp.toPx() }
                val maxSpreadDistancePx = lens.radiusPx * 0.42f * 2.8f
                val spreadDistance = (focusDistance * spreadMultiplier).coerceIn(
                    minSpreadDistancePx,
                    maxSpreadDistancePx,
                )
                val transformed = androidx.compose.ui.geometry.Offset(
                    focusTargetPosition.x + kotlin.math.cos(angle) * spreadDistance,
                    focusTargetPosition.y + kotlin.math.sin(angle) * spreadDistance,
                )
                val transformedDx = transformed.x - lens.center.x
                val transformedDy = transformed.y - lens.center.y
                val transformedDistanceSq = transformedDx * transformedDx + transformedDy * transformedDy
                if (transformedDistanceSq > lens.radiusPx * lens.radiusPx) {
                    return point to true
                }
                return transformed to false
            }

            fun pickTapTarget(
                position: androidx.compose.ui.geometry.Offset,
                source: List<RenderedAisTargetOnMap>,
            ): RenderedAisTargetOnMap? {
                return source.minByOrNull { candidate ->
                    val visualPosition = candidate.visualPosition
                    val dx = visualPosition.x - position.x
                    val dy = visualPosition.y - position.y
                    dx * dx + dy * dy
                }
            }

            fun pickTargetsInRadius(
                position: androidx.compose.ui.geometry.Offset,
                source: List<RenderedAisTargetOnMap>,
                radiusPx: Float,
            ): List<RenderedAisTargetOnMap> {
                val radiusSq = radiusPx * radiusPx
                return source.filter { candidate ->
                    val visualPosition = candidate.visualPosition
                    val dx = visualPosition.x - position.x
                    val dy = visualPosition.y - position.y
                    dx * dx + dy * dy <= radiusSq
                }
            }

            val activeMagnifier = overlapMagnifier?.takeIf {
                it.expiresAtMs > System.currentTimeMillis()
            }
            val baseMagnifierTargets: List<RenderedAisTargetOnMap> = remember(renderTargets, activeMagnifier) {
                activeMagnifier?.baseTargets ?: renderTargets.mapIndexed { index, target ->
                    RenderedAisTargetOnMap(index, target, target.position, wasClampedToLensBase = false)
                }
            }
            val magnifierFocusTargetIndex = activeMagnifier?.focusTargetIndex
            val magnifierFocusPosition: androidx.compose.ui.geometry.Offset? = activeMagnifier?.focusTargetPosition
            val magnifiedTargets: List<RenderedAisTargetOnMap> = remember(baseMagnifierTargets, activeMagnifier) {
                baseMagnifierTargets.map { mapped ->
                    val (transformed, wasClampedToLensBase) = resolveMagnifierPoint(
                        mapped.visualPosition,
                        activeMagnifier,
                        mapped.index,
                        magnifierFocusTargetIndex,
                        magnifierFocusPosition,
                    )
                    mapped.copy(
                        visualPosition = transformed,
                        wasClampedToLensBase = wasClampedToLensBase,
                    )
                }
            }
            val isInActiveMagnifier: (RenderedAisTargetOnMap) -> Boolean = { item ->
                activeMagnifier?.let { lens ->
                    val dx = item.visualPosition.x - lens.center.x
                    val dy = item.visualPosition.y - lens.center.y
                    dx * dx + dy * dy <= lens.radiusPx * lens.radiusPx
                } ?: false
            }

            LaunchedEffect(overlapMagnifier) {
                val current = overlapMagnifier ?: return@LaunchedEffect
                val remainingMs = (current.expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
                if (remainingMs > 0L) {
                    delay(remainingMs)
                }
                if (overlapMagnifier?.startedAtMs == current.startedAtMs) {
                    overlapMagnifier = null
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(renderTargets, activeMagnifier, onIncreaseRange, onDecreaseRange) {
                        var zoomAccumulator = 1f
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom <= 0f || !zoom.isFinite()) return@detectTransformGestures
                            zoomAccumulator *= zoom
                            val zoomInThreshold = 1.18f
                            val zoomOutThreshold = 0.85f

                            while (zoomAccumulator >= zoomInThreshold) {
                                onIncreaseRange()
                                zoomAccumulator /= zoomInThreshold
                            }
                            while (zoomAccumulator <= zoomOutThreshold) {
                                onDecreaseRange()
                                zoomAccumulator /= zoomOutThreshold
                            }

                            zoomAccumulator = zoomAccumulator.coerceIn(0.75f, 1.35f)
                        }
                    }
                    .pointerInput(renderTargets, magnifiedTargets, overlapMagnifier, widthPx, heightPx) {
                        detectTapGestures { position ->
                            val tapTolerancePx = with(density) { 16.dp.toPx() }
                            val currentMagnifier = activeMagnifier
                            val nearest = pickTapTarget(position, magnifiedTargets)
                            val tappedTarget = nearest?.let { candidate ->
                                val visualPosition = candidate.visualPosition
                                val dx = visualPosition.x - position.x
                                val dy = visualPosition.y - position.y
                                val hitRadiusPx = maxOf(
                                    candidate.target.hitRadius * 1.7f,
                                    tapFallbackRadiusPx + tapTolerancePx,
                                )
                                if (dx * dx + dy * dy <= hitRadiusPx * hitRadiusPx) candidate else null
                            }

                            if (currentMagnifier != null) {
                                val closeButtonOffset = androidx.compose.ui.geometry.Offset(
                                    currentMagnifier.center.x,
                                    currentMagnifier.center.y - currentMagnifier.radiusPx,
                                )
                                val closeDx = position.x - closeButtonOffset.x
                                val closeDy = position.y - closeButtonOffset.y
                                val closeRadius = overlapCloseButtonRadiusPx + tapTolerancePx
                                if (closeDx * closeDx + closeDy * closeDy <= closeRadius * closeRadius) {
                                    overlapMagnifier = null
                                    return@detectTapGestures
                                }
                            }

                            tappedTarget?.let { candidate ->
                                val clusterRadiusPx = overlapDetectionRadiusPx
                                val overlappedTargets = pickTargetsInRadius(
                                    position = candidate.visualPosition,
                                    source = magnifiedTargets,
                                    radiusPx = clusterRadiusPx,
                                )

                                if (currentMagnifier == null && overlappedTargets.size >= 2) {
                                    val clusterCenter = position
                                    val focusTargetPosition = candidate.visualPosition
                                    overlapMagnifier = AisOverlapMagnifier(
                                        center = clusterCenter,
                                        radiusPx = magnifierRadiusPx,
                                        scale = magnifierScale,
                                        focusTargetIndex = candidate.index,
                                        focusTargetPosition = focusTargetPosition,
                                        targetIndexes = overlappedTargets.map { it.index }.toSet(),
                                        baseTargets = baseMagnifierTargets,
                                        startedAtMs = System.currentTimeMillis(),
                                        expiresAtMs = System.currentTimeMillis() + 15_000L,
                                    )
                                    return@detectTapGestures
                                }

                                val target = candidate.target
                                val visualPosition = candidate.visualPosition
                                val dx = visualPosition.x - position.x
                                val dy = visualPosition.y - position.y
                                val hitRadiusPx = maxOf(
                                    target.hitRadius * 1.7f,
                                    tapFallbackRadiusPx + tapTolerancePx,
                                )
                                if (dx * dx + dy * dy <= hitRadiusPx * hitRadiusPx) {
                                    selectedTarget = target
                                    selectedTargetId = target.stableId
                                    return@detectTapGestures
                                }
                            }
                        }
                    },
        ) {
            drawSimpleAisMiniMap(
                ownHeadingDeg = ownHeadingDeg,
                ownSpeedKn = ownSpeedKn,
                rangeNm = visibleRangeNm,
                northUp = settings.northUp,
                mapRadiusPx = mapRadiusPx,
                baseTextSizePx = textSizePx,
                fontOffsetPx = fontOffsetPx,
                distanceMarkerTextSizePx = (textSizePx + fontOffsetPx - with(density) { 4.sp.toPx() }).coerceAtLeast(8f),
                infoTextSizePx = (textSizePx + fontOffsetPx).coerceAtLeast(10f),
            )
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height),
                style = androidx.compose.ui.graphics.drawscope.Fill,
            )

            val ownBoatPosition = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val overlapHiddenTargetIds = run {
                val overlapRadiusPx = overlapDetectionRadiusPx
                val overlapRadiusSq = overlapRadiusPx * overlapRadiusPx
                val overlapHiddenIds = mutableSetOf<String>()
                val processed = mutableSetOf<String>()
                magnifiedTargets.forEach { seed ->
                    val seedTarget = seed.target
                    val seedId = seedTarget.stableId
                    val seedPosition = seed.visualPosition
                    if (processed.contains(seedId)) {
                        return@forEach
                    }
                    val cluster = magnifiedTargets.filter { candidate ->
                        val dx = candidate.visualPosition.x - seedPosition.x
                        val dy = candidate.visualPosition.y - seedPosition.y
                        dx * dx + dy * dy <= overlapRadiusSq
                    }
                    if (cluster.size > 1) {
                        cluster.forEach { overlapHiddenIds.add(it.target.stableId) }
                    }
                    cluster.forEach { processed.add(it.target.stableId) }
                }
                overlapHiddenIds
            }
            val magnifierTargetIndexes = activeMagnifier?.targetIndexes.orEmpty()
            val isMagnifierActive = activeMagnifier != null
            val backgroundMagnifierTargets = if (isMagnifierActive) {
                magnifiedTargets.filter { magnifierTarget ->
                    !magnifierTargetIndexes.contains(magnifierTarget.index) ||
                        magnifierTarget.wasClampedToLensBase ||
                        !isInActiveMagnifier(magnifierTarget)
                }
            } else {
                magnifiedTargets
            }
            val foregroundMagnifierTargets = if (isMagnifierActive) {
                magnifiedTargets.filter { magnifierTarget ->
                    magnifierTargetIndexes.contains(magnifierTarget.index) &&
                        !magnifierTarget.wasClampedToLensBase &&
                        isInActiveMagnifier(magnifierTarget)
                }
            } else {
                emptyList()
            }

            val renderAisTarget = { item: RenderedAisTargetOnMap ->
                val target = item.target
                val markerPosition = item.visualPosition
                val isOlderThanTwoMinutes = target.signalAgeMs >= 2 * 60_000L
                val isSelected = selectedTargetId != null && selectedTargetId == target.stableId
                val baseColor = when {
                    isSelected -> Color.Yellow
                    isOlderThanTwoMinutes -> Color(0xFF9E9E9E)
                    target.isStale -> Color(0xFF9E9E9E)
                    target.isAlarm -> Color(0xFFFF4444)
                    else -> Color.White
                }
                val markerColor = baseColor
                val markerStrokeColor = Color.Black
                val markerRadius = target.markerRadius
                val courseAngleRad = target.courseRenderAngleRad
                val speedForVisual = target.signal.speedKn?.takeIf { it.isFinite() && it > 0f } ?: 0f
                val dirX = kotlin.math.cos(courseAngleRad)
                val dirY = kotlin.math.sin(courseAngleRad)
                val half = markerRadius * 0.7f
                val sideHalf = half * 0.88f
                val baseCenter = androidx.compose.ui.geometry.Offset(
                    markerPosition.x - dirX * half * 0.35f,
                    markerPosition.y - dirY * half * 0.35f,
                )
                val left = androidx.compose.ui.geometry.Offset(
                    baseCenter.x + dirY * sideHalf,
                    baseCenter.y - dirX * sideHalf,
                )
                val right = androidx.compose.ui.geometry.Offset(
                    baseCenter.x - dirY * sideHalf,
                    baseCenter.y + dirX * sideHalf,
                )
                val tip = androidx.compose.ui.geometry.Offset(
                    markerPosition.x + dirX * half * 1.85f,
                    markerPosition.y + dirY * half * 1.85f,
                )
                val headingLineLength = half + (speedForVisual * 1.05f).coerceIn(0f, half * 3.2f)
                val headingLineEnd = androidx.compose.ui.geometry.Offset(
                    markerPosition.x + dirX * headingLineLength,
                    markerPosition.y + dirY * headingLineLength,
                )

                drawLine(
                    color = markerColor.copy(alpha = 0.45f),
                    start = androidx.compose.ui.geometry.Offset(cx, cy),
                    end = markerPosition,
                    strokeWidth = 1f
                )

                val tx = markerPosition.x
                val ty = markerPosition.y
                drawLine(
                    color = markerColor,
                    start = androidx.compose.ui.geometry.Offset(tx, ty),
                    end = headingLineEnd,
                    strokeWidth = 2.2f,
                )
                val triangle = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                }
                drawPath(path = triangle, color = markerColor)
                drawPath(
                    path = triangle,
                    color = markerStrokeColor,
                    style = Stroke(width = with(density) { 1.dp.toPx() }),
                )

                val label = aisTargetDisplayLabel(target.signal)
                val isHiddenInOverlap = !isMagnifierActive && overlapHiddenTargetIds.contains(target.stableId)
                if (!isHiddenInOverlap && label.isNotEmpty()) {
                    val labelPaint = titleTextPaint.apply {
                        textAlign = Paint.Align.CENTER
                        textSize = (textSizePx * 0.7f + fontOffsetPx).coerceAtLeast(8f)
                        setColor(markerColor.toArgb())
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        tx,
                        ty + markerRadius + 14f,
                        labelPaint,
                    )
                }
            }

            backgroundMagnifierTargets.forEach(renderAisTarget)
            activeMagnifier?.let { lens ->
                drawCircle(
                    color = Color.Black.copy(alpha = 0.30f),
                    center = lens.center,
                    radius = lens.radiusPx,
                    style = androidx.compose.ui.graphics.drawscope.Fill,
                )
            }
            foregroundMagnifierTargets.forEach(renderAisTarget)
            activeMagnifier?.let { lens ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    center = lens.center,
                    radius = lens.radiusPx,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 3.dp.toPx() }),
                )
                val closeCenter = androidx.compose.ui.geometry.Offset(
                    lens.center.x,
                    lens.center.y - lens.radiusPx,
                )
                drawCircle(
                    color = Color.White,
                    center = closeCenter,
                    radius = overlapCloseButtonRadiusPx,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                )
                val closeCrossHalf = overlapCloseButtonRadiusPx * 0.55f
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(closeCenter.x - closeCrossHalf, closeCenter.y - closeCrossHalf),
                    end = androidx.compose.ui.geometry.Offset(closeCenter.x + closeCrossHalf, closeCenter.y + closeCrossHalf),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(closeCenter.x - closeCrossHalf, closeCenter.y + closeCrossHalf),
                    end = androidx.compose.ui.geometry.Offset(closeCenter.x + closeCrossHalf, closeCenter.y - closeCrossHalf),
                    strokeWidth = 2f,
                )
            }

        if (renderTargets.isEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    "Keine AIS-Signale",
                    cx,
                    cy,
                    titleTextPaint.apply {
                        textSize = textSizePx + fontOffsetPx
                    }
                )
            }
        }

    val widgetHasAlarm = renderTargets.any { it.isAlarm }
    val activeAlarmTonePlayback = widgetHasAlarm && !settings.collisionAlarmsMuted
    LaunchedEffect(activeAlarmTonePlayback, isMuted, alarmToneVolume, alarmRepeatIntervalSeconds, widgetTitle) {
        if (!activeAlarmTonePlayback || isMuted) {
            return@LaunchedEffect
        }
        val repeatIntervalMs = alarmRepeatIntervalSeconds.coerceIn(2, 10) * 1000L
        while (true) {
            playAisCollisionAlarmTone(context = context, volume = alarmToneVolume)
                onAlarmTonePlayed(widgetTitle)
                delay(repeatIntervalMs)
            }
        }

    LaunchedEffect(widgetHasAlarm) {
        onAlarmStateChange(widgetHasAlarm)
    }

        val selectedTargetState = selectedTarget
        if (selectedTargetState != null) {
            val target = selectedTargetState.signal
            if (target != null) {
                val courseText = formatAngle(target.courseDeg)
                val distanceText = formatDistance(target.distanceNm)
                val cpaText = formatDistance(target.cpaNm)
                val cpaTimeText = formatAisCpaTime(target.cpaTimeMinutes)
                val speedText = formatSpeed(target.speedKn)
                val signalAgeText = formatSourceAgeMillis(selectedTargetState.signalAgeMs)
                val isOlderThanTwoMinutes = selectedTargetState.signalAgeMs >= 2 * 60_000L
                val menuTextColor = if (darkBackground) Color.White else Color.Black
                val ageColor = if (isOlderThanTwoMinutes) Color.Gray else menuTextColor
                val mmsiText = aisTargetMmsiLabel(target)
                    ?: "—"
                val shipName = target.name?.takeIf { it.isNotBlank() } ?: "Unbekannt"
                AlertDialog(
                    onDismissRequest = {
                        selectedTarget = null
                        selectedTargetId = null
                    },
                    containerColor = if (darkBackground) Color.Black else Color.White,
                    titleContentColor = menuTextColor,
                    textContentColor = menuTextColor,
                    title = { Text(target.name?.takeIf { it.isNotBlank() } ?: "AIS-Ziel") },
                    text = {
                Column(
                    modifier = Modifier.padding(menuDialogPadding),
                    verticalArrangement = Arrangement.spacedBy(MENU_SPACING)
                ) {
                    Text("Schiffname: $shipName")
                    Text("Signal: $signalAgeText", color = ageColor)
                    val displayLabel = if (target.name?.isNotBlank() == true) {
                        shipName
                    } else {
                        mmsiText
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kennung: $displayLabel")
                        Button(
                            onClick = {
                                if (mmsiText != "—") {
                                    onCallMmsi(mmsiText)
                                }
                            },
                            enabled = mmsiText != "—",
                            colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
                        ) {
                            Text("Anrufen")
                        }
                    }
                    Text("Distanz: $distanceText")
                    Text("CPA: $cpaText")
                    Text("Kurs: $courseText")
                            Text("Geschwindigkeit: $speedText")
                            Text("Manövrierbeschränkungen: ${target.maneuverRestriction ?: "—"}")
                            Text("CPA-Zeit: $cpaTimeText")
                        }
                    },
                    confirmButton = {
                TextButton(
                            modifier = Modifier.padding(
                                end = SeaFoxDesignTokens.Size.menuDropdownItemLeadingPaddingEnd,
                                bottom = SeaFoxDesignTokens.Size.menuDropdownItemLeadingPaddingEnd,
                            ),
                            onClick = {
                                selectedTarget = null
                                selectedTargetId = null
                            },
                            shape = RoundedCornerShape(SeaFoxDesignTokens.Size.menuSmallCornerRadius),
                            colors = SeaFoxDesignTokens.NonMenuButton.textButtonColors(),
                    ) {
                        Text("Schließen", color = SeaFoxDesignTokens.NonMenuButton.color)
                    }
                },
            )
            }
        }
    }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(MENU_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                onClick = onIncreaseRange,
                enabled = canIncreaseRange,
                colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
            ) {
                Text("+5sm")
            }
            Button(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                onClick = onDecreaseRange,
                enabled = canDecreaseRange,
                colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
            ) {
                Text("-5sm")
            }
        }
    }
}

private data class RenderedAisTarget(
    val stableId: String,
    val signal: AisTargetData,
    val position: androidx.compose.ui.geometry.Offset,
    val markerRadius: Float,
    val hitRadius: Float,
    val isAlarm: Boolean,
    val isStale: Boolean,
    val signalAgeMs: Long,
    val courseRenderAngleRad: Float,
)

private data class RenderedAisTargetOnMap(
    val index: Int,
    val target: RenderedAisTarget,
    val visualPosition: Offset,
    val wasClampedToLensBase: Boolean,
)

private data class AisOverlapMagnifier(
    val center: androidx.compose.ui.geometry.Offset,
    val radiusPx: Float,
    val scale: Float,
    val focusTargetIndex: Int?,
    val focusTargetPosition: androidx.compose.ui.geometry.Offset?,
    val targetIndexes: Set<Int>,
    val baseTargets: List<RenderedAisTargetOnMap>,
    val startedAtMs: Long,
    val expiresAtMs: Long,
)

private fun aisTargetStableId(target: AisTargetData): String {
    target.id.trim().takeIf { it.isNotBlank() }?.let { return it }
    aisTargetMmsiLabel(target)?.let { return "mmsi_$it" }
    val latText = target.latitude?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.6f", it) }
    val lonText = target.longitude?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.6f", it) }
    if (latText != null && lonText != null) {
        return "coord_${latText}_$lonText"
    }
    return "ais_${target.hashCode()}"
}

private fun aisTargetDisplayLabel(target: AisTargetData): String {
    val name = target.name?.trim()
        ?.ifBlank { null }
    if (!name.isNullOrBlank()) return name

    return aisTargetMmsiLabel(target)
        ?: target.id.takeIf { it.startsWith("mmsi_") }
            ?.removePrefix("mmsi_")
        ?: "AIS"
}

private fun aisTargetMmsiLabel(target: AisTargetData): String? {
    val textMmsi = target.mmsiText
        ?.trim()
        ?.filter { it.isDigit() }
        ?.ifBlank { null }
    if (!textMmsi.isNullOrBlank()) return textMmsi

    val numericMmsi = target.mmsi?.takeIf { it.isFinite() && it > 0f && it < 1_000_000_000f } ?: return null
    val rounded = kotlin.math.round(numericMmsi.toDouble()).toLong()
    return if (rounded in 1L..999_999_999L) {
        rounded.toString()
    } else {
        null
    }
}

private fun mapAisTargetsToScreen(
    widthPx: Float,
    heightPx: Float,
    mapRadiusPx: Float,
    ownHeadingDeg: Float?,
    ownLatitudeDeg: Float?,
    ownLongitudeDeg: Float?,
    maxRangeNm: Float,
    targets: List<AisTargetWithAge>,
    alarmDistanceNm: Float,
    alarmMinutes: Float,
    northUp: Boolean,
    currentTimeMs: Long,
    targetStaleMs: (AisTargetWithAge) -> Long = { _ -> AIS_TARGET_STALE_WARNING_MINUTES * 60_000L.toLong() },
): List<RenderedAisTarget> {
    val cx = widthPx / 2f
    val cy = heightPx / 2f
    val normalizedRange = maxRangeNm.takeIf { it.isFinite() }?.coerceAtLeast(0.5f) ?: 0.5f
    val ownHeadingDegNormalized = ownHeadingDeg
        ?.takeIf { it.isFinite() }
        ?.let(::wrap360)
    val hasOwnHeading = ownHeadingDegNormalized != null
    val normalizedOwnHeading = ownHeadingDegNormalized ?: 0f
    val ownLat = ownLatitudeDeg?.takeIf { it.isFinite() && it in -90f..90f }
    val ownLon = ownLongitudeDeg?.takeIf { it.isFinite() && it in -180f..180f }

    val effectiveNorthUp = northUp

    return targets.mapNotNull { state ->
        val target = state.target
        var bearingRaw = if (northUp) {
            when {
                target.absoluteBearingDeg != null -> target.absoluteBearingDeg
                target.relativeBearingDeg != null && hasOwnHeading ->
                    target.relativeBearingDeg + normalizedOwnHeading
                target.relativeBearingDeg != null -> target.relativeBearingDeg
                else -> null
            }
        } else {
            when {
                target.relativeBearingDeg != null -> target.relativeBearingDeg
                target.absoluteBearingDeg != null -> target.absoluteBearingDeg - if (hasOwnHeading) {
                    normalizedOwnHeading
                } else {
                    0f
                }
                else -> null
            }
        }

        if (bearingRaw == null && target.latitude != null && target.longitude != null && ownLat != null && ownLon != null) {
            bearingRaw = computeAisBearingNm(
                ownLatitudeDeg = ownLat,
                ownLongitudeDeg = ownLon,
                targetLatitudeDeg = target.latitude,
                targetLongitudeDeg = target.longitude,
            )
        }

        if (bearingRaw == null) {
            return@mapNotNull null
        }

        val computedDistanceNm = when {
            target.distanceNm != null -> target.distanceNm
            target.latitude != null && target.longitude != null && ownLat != null && ownLon != null -> computeAisDistanceNm(
                ownLatitudeDeg = ownLat,
                ownLongitudeDeg = ownLon,
                targetLatitudeDeg = target.latitude,
                targetLongitudeDeg = target.longitude,
            )
            else -> null
        }
        val normalizedDistanceNm = computedDistanceNm?.takeIf { it.isFinite() && it >= 0f } ?: run {
            return@mapNotNull null
        }
        if (normalizedDistanceNm > normalizedRange) {
            return@mapNotNull null
        }

        val bearing = normalizeSignedTo0_360(wrap360(bearingRaw))
        val normalizedDistance = (normalizedDistanceNm / normalizedRange).coerceIn(0f, 1f)
        val angleRad = Math.toRadians((bearing - 90f).toDouble()).toFloat()
        val courseAngleDeg = when {
            target.courseDeg != null && target.courseDeg.isFinite() -> {
                if (effectiveNorthUp || !hasOwnHeading) {
                    normalizeSignedTo0_360(wrap360(target.courseDeg))
                } else {
                    normalizeSignedTo0_360(wrap360(target.courseDeg - normalizedOwnHeading))
                }
            }
            else -> bearing
        }
        val courseAngleRad = Math.toRadians((courseAngleDeg - 90f).toDouble()).toFloat()
        val effectiveRadius = mapRadiusPx.coerceAtLeast(1f)
        val position = androidx.compose.ui.geometry.Offset(
            cx + kotlin.math.cos(angleRad) * normalizedDistance * effectiveRadius,
            cy + kotlin.math.sin(angleRad) * normalizedDistance * effectiveRadius,
        )

        val isAlarm = isAisAlarm(target, alarmDistanceNm, alarmMinutes)
        val baseRadius = (minOf(widthPx, heightPx) * 0.03f).coerceAtLeast(8f)
        val markerRadius = if (isAlarm) baseRadius * 1.5f else baseRadius
        val staleAfterMs = targetStaleMs(state).coerceAtLeast(1_000L)
        val ageMs = (currentTimeMs - state.receivedAtMs).coerceAtLeast(0L)
        val isStale = ageMs >= staleAfterMs

        RenderedAisTarget(
            stableId = aisTargetStableId(target),
            signal = target,
            position = position,
            markerRadius = markerRadius,
            hitRadius = (markerRadius * 1.75f).coerceAtLeast(14f),
            isAlarm = isAlarm,
            isStale = isStale,
            signalAgeMs = ageMs,
            courseRenderAngleRad = courseAngleRad,
        )
    }
}

private fun computeAisBearingNm(
    ownLatitudeDeg: Float,
    ownLongitudeDeg: Float,
    targetLatitudeDeg: Float,
    targetLongitudeDeg: Float,
): Float? {
    if (
        !ownLatitudeDeg.isFinite() || !ownLongitudeDeg.isFinite() ||
        !targetLatitudeDeg.isFinite() || !targetLongitudeDeg.isFinite()
    ) return null
    if (
        ownLatitudeDeg !in -90f..90f || ownLongitudeDeg !in -180f..180f ||
        targetLatitudeDeg !in -90f..90f || targetLongitudeDeg !in -180f..180f
    ) return null

    val toRad = (PI / 180f).toFloat()
    val lat1 = ownLatitudeDeg * toRad
    val lat2 = targetLatitudeDeg * toRad
    val dLon = (targetLongitudeDeg - ownLongitudeDeg) * toRad
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val bearing = atan2(y, x) * (180f / kotlin.math.PI.toFloat())
    return wrap360(bearing)
}

private fun computeAisDistanceNm(
    ownLatitudeDeg: Float,
    ownLongitudeDeg: Float,
    targetLatitudeDeg: Float,
    targetLongitudeDeg: Float,
): Float? {
    if (
        !ownLatitudeDeg.isFinite() || !ownLongitudeDeg.isFinite() ||
        !targetLatitudeDeg.isFinite() || !targetLongitudeDeg.isFinite()
    ) return null
    if (
        ownLatitudeDeg !in -90f..90f || ownLongitudeDeg !in -180f..180f ||
        targetLatitudeDeg !in -90f..90f || targetLongitudeDeg !in -180f..180f
    ) return null

    val earthRadiusNm = 3440.065f
    val lat1 = Math.toRadians(ownLatitudeDeg.toDouble())
    val lat2 = Math.toRadians(targetLatitudeDeg.toDouble())
    val dLat = Math.toRadians((targetLatitudeDeg - ownLatitudeDeg).toDouble())
    val dLon = Math.toRadians((targetLongitudeDeg - ownLongitudeDeg).toDouble())
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

private fun DrawScope.drawSimpleAisMiniMap(
    ownHeadingDeg: Float?,
    ownSpeedKn: Float?,
    rangeNm: Float,
    northUp: Boolean,
    mapRadiusPx: Float,
    baseTextSizePx: Float,
    fontOffsetPx: Float,
    distanceMarkerTextSizePx: Float,
    infoTextSizePx: Float,
) {
    val width = size.width
    val height = size.height
    val cx = width * 0.5f
    val cy = height * 0.5f
    val mapRadius = mapRadiusPx.coerceAtLeast(1f)
    val textScale = if (baseTextSizePx.isFinite() && baseTextSizePx > 0f) {
        baseTextSizePx
    } else {
        14f
    }
    val innerColor = Color.White.copy(alpha = 0.18f)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = infoTextSizePx
    }
    val labelPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = distanceMarkerTextSizePx
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    drawCircle(
        color = Color(0xFF061A2C),
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        radius = mapRadius,
        style = androidx.compose.ui.graphics.drawscope.Fill,
    )

    repeat(3) { step ->
        val y = cy + (height * 0.5f * (step - 1) / 2f)
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = androidx.compose.ui.geometry.Offset(cx - mapRadius, y),
            end = androidx.compose.ui.geometry.Offset(cx + mapRadius, y),
            strokeWidth = 1f,
        )

        val x = cx + (width * 0.5f * (step - 1) / 2f)
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = androidx.compose.ui.geometry.Offset(x, cy - mapRadius),
            end = androidx.compose.ui.geometry.Offset(x, cy + mapRadius),
            strokeWidth = 1f,
        )
    }

    repeat(4) { step ->
        val radius = mapRadius * ((step + 1) / 4f)
        drawCircle(
            color = innerColor,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            radius = radius,
            style = Stroke(width = 1.2f),
        )

        val labelNm = (rangeNm * (step + 1) / 4f).let { it.roundToInt() }
        drawContext.canvas.nativeCanvas.drawText(
            "${labelNm}NM",
            cx + radius * 0.6f,
            cy - radius - 4f,
            Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = labelPaint.textSize
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
        )
    }

    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = androidx.compose.ui.geometry.Offset(cx - mapRadius, cy),
        end = androidx.compose.ui.geometry.Offset(cx + mapRadius, cy),
        strokeWidth = 1f,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = androidx.compose.ui.geometry.Offset(cx, cy - mapRadius),
        end = androidx.compose.ui.geometry.Offset(cx, cy + mapRadius),
        strokeWidth = 1f,
    )

    val ownHeading = ownHeadingDeg
        ?.takeIf { it.isFinite() }
        ?.let(::wrap360)
        ?: 0f
    val headingDisplay = if (northUp) ownHeading else 0f
    val speedKn = ownSpeedKn?.coerceAtLeast(0f) ?: 0f
    val speedFactor = (speedKn / 20f).coerceIn(0f, 1f)
    val markerLength = mapRadius * (0.22f + (0.58f * speedFactor))
    val headingRad = Math.toRadians((headingDisplay - 90.0).toDouble()).toFloat()
    val headingEnd = androidx.compose.ui.geometry.Offset(
        cx + kotlin.math.cos(headingRad) * markerLength,
        cy + kotlin.math.sin(headingRad) * markerLength,
    )
    val arrowHeadOffset = 12f
    val leftWing = headingRad + PI.toFloat() / 2f
    val rightWing = headingRad - PI.toFloat() / 2f
    val leftPoint = androidx.compose.ui.geometry.Offset(
        headingEnd.x + kotlin.math.cos(leftWing) * 4f,
        headingEnd.y + kotlin.math.sin(leftWing) * 4f,
    )
    val rightPoint = androidx.compose.ui.geometry.Offset(
        headingEnd.x + kotlin.math.cos(rightWing) * 4f,
        headingEnd.y + kotlin.math.sin(rightWing) * 4f,
    )
    val arrowTip = androidx.compose.ui.geometry.Offset(
        headingEnd.x + kotlin.math.cos(headingRad) * arrowHeadOffset,
        headingEnd.y + kotlin.math.sin(headingRad) * arrowHeadOffset,
    )
    drawLine(
        color = Color(0xFF4CD9FF),
        start = androidx.compose.ui.geometry.Offset(cx, cy),
        end = headingEnd,
        strokeWidth = 2.5f,
    )
    drawLine(
        color = Color(0xFF4CD9FF),
        start = arrowTip,
        end = leftPoint,
        strokeWidth = 2f,
    )
    drawLine(
        color = Color(0xFF4CD9FF),
        start = arrowTip,
        end = rightPoint,
        strokeWidth = 2f,
    )

    val labelNorth = if (northUp) "N" else "0°"
    val labelEast = if (northUp) "E" else "90°"
    val labelSouth = if (northUp) "S" else "180°"
    val labelWest = if (northUp) "W" else "-90°"
    labelPaint.textSize = (textScale * 0.86f + fontOffsetPx).coerceAtLeast(10f)
    drawContext.canvas.nativeCanvas.drawText(labelNorth, cx, cy - mapRadius - 6f, labelPaint)
    drawContext.canvas.nativeCanvas.drawText(labelSouth, cx, cy + mapRadius + 16f, labelPaint)
    drawContext.canvas.nativeCanvas.drawText(labelWest, cx - mapRadius - 14f, cy + 4f, labelPaint)
    drawContext.canvas.nativeCanvas.drawText(labelEast, cx + mapRadius + 6f, cy + 4f, labelPaint)

    drawCircle(
        color = Color(0xFF2ECC71),
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        radius = 6f,
    )

    val infoY = height - (infoTextSizePx * 0.9f)
    drawContext.canvas.nativeCanvas.drawText(
        "Kurs ${ownHeading.roundToInt()}°",
        8f,
        infoY,
        textPaint.apply {
            textAlign = Paint.Align.LEFT
            isFakeBoldText = true
        },
    )
    drawContext.canvas.nativeCanvas.drawText(
        "SOG ${ownSpeedKn?.let { "${it.roundToInt()} kn" } ?: "—"}",
        width - 8f,
        infoY,
        textPaint.apply {
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = false
        }
    )
}

private fun isAisAlarm(
    target: AisTargetData,
    alarmDistanceNm: Float,
    alarmMinutes: Float,
): Boolean {
    val cpaThresholdActive = alarmDistanceNm > 0.01f
    val minuteThresholdActive = alarmMinutes > 0.01f
    val distanceAlarm = if (cpaThresholdActive) {
        target.cpaNm != null && target.cpaNm <= alarmDistanceNm
    } else {
        false
    }
    val timeAlarm = if (minuteThresholdActive) {
        target.cpaTimeMinutes != null && target.cpaTimeMinutes <= alarmMinutes
    } else {
        false
    }

    return when {
        cpaThresholdActive && minuteThresholdActive -> distanceAlarm || timeAlarm
        cpaThresholdActive -> distanceAlarm
        minuteThresholdActive -> timeAlarm
        else -> false
    }
}

@Composable
fun NmeaPgnWidget(
    detectedSources: List<NmeaSourceProfile>,
    receivedPgnHistory: List<NmeaPgnHistoryEntry>,
    telemetryText: Map<String, String>,
    modifier: Modifier = Modifier,
    darkBackground: Boolean = true,
) {
    val textColor = if (darkBackground) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val headerColor = premiumSignalColor
    val mutedColor = textColor.copy(alpha = 0.68f)
    val cardColor = if (darkBackground) {
        SeaFoxDesignTokens.Color.surfaceRaisedDark.copy(alpha = 0.62f)
    } else {
        SeaFoxDesignTokens.Color.surfaceRaisedLight.copy(alpha = 0.82f)
    }
    val currentSource = telemetryText["n2k_source"]?.trim()
        ?: telemetryText["source"]?.trim()
        ?: telemetryText["source_address"]?.trim()
        ?: telemetryText["src"]?.trim()
        ?: telemetryText["header.source"]?.trim()
    val currentPgnText = telemetryText["n2k_pgn"]?.trim()
        ?: telemetryText["pgn"]?.trim()
        ?: telemetryText["header.pgn"]?.trim()
    val currentPgn = currentPgnText?.toIntOrNull()
    val debugRawLine = telemetryText["n2k_raw_line"]?.trim()
    val debugPacketPreview = telemetryText["n2k_debug_preview"]?.trim()
    val payloadLength = telemetryText["n2k_payload_len"]?.trim()
        ?: telemetryText["payload_len"]?.trim()
        ?: telemetryText["payloadlength"]?.trim()
        ?: telemetryText["payload_length"]?.trim()
    val payloadHex = telemetryText["n2k_payload_hex"]?.trim()
        ?: telemetryText["payload"]?.trim()
    val currentSourceLabel = detectedSources.firstOrNull { it.sourceKey.equals(currentSource, ignoreCase = true) }?.let { source ->
        if (source.displayName.isNotBlank()) {
            "${source.displayName} (${source.sourceKey})"
        } else {
            source.sourceKey
        }
    } ?: currentSource.orEmpty().ifBlank { "—" }
    val recentPgnHistory = remember(receivedPgnHistory) { receivedPgnHistory.take(240) }

    val nowMs = System.currentTimeMillis()
    val sortedSources = detectedSources.sortedBy { it.displayName.ifBlank { it.sourceKey } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "NMEA2000 PGN-Empfang",
                    color = headerColor,
                    fontWeight = FontWeight.Bold,
                )

            Text(
                text = "Aktuelle Nachricht",
                color = textColor,
                fontWeight = FontWeight.Bold,
            )

            if (currentSource.isNullOrBlank() && currentPgnText.isNullOrBlank()) {
                Text("Noch keine PGN-Meldung empfangen.", color = mutedColor)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Quelle: $currentSourceLabel", color = textColor)
                    Text(
                        text = if (currentPgn != null) {
                            "PGN: ${formatNmeaPgnLine(currentPgn)}"
                        } else {
                            "PGN: $currentPgnText"
                        },
                        color = textColor,
                    )
                    if (payloadLength != null) {
                        Text("Payload-Länge: ${payloadLength}B", color = mutedColor, fontSize = 12.sp)
                    }
                    if (!payloadHex.isNullOrBlank()) {
                        Text(
                            text = "Payload: ${payloadHex.take(56)}",
                            color = mutedColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                    if (!debugRawLine.isNullOrBlank()) {
                        Text(
                            text = "Raw-PGN-N2K: ${debugRawLine.take(100)}",
                            color = mutedColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        )
                    }
                    if (!debugPacketPreview.isNullOrBlank()) {
                        Text(
                            text = "Preview: ${debugPacketPreview.take(100)}",
                            color = mutedColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Text(
                text = "Alle empfangenen PGNs (${recentPgnHistory.size})",
                color = textColor,
                fontWeight = FontWeight.Bold,
            )

            if (recentPgnHistory.isEmpty()) {
                Text("Noch keine PGN-Meldung empfangen.", color = mutedColor)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recentPgnHistory.forEach { entry ->
                        val sourceName = entry.sourceKey.ifBlank { "unbekannt" }
                        val sourceAge = formatSourceAgeMillis(nowMs - entry.receivedAtMs)
                        val detectedPgnText = if (entry.detectedPgnText.isNullOrBlank()) {
                            formatNmeaPgnLine(entry.pgn)
                        } else {
                            entry.detectedPgnText
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = cardColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "PGN: ${formatNmeaPgnLine(entry.pgn)}", color = textColor, fontWeight = FontWeight.SemiBold)
                                Text(text = sourceAge, color = mutedColor, fontSize = 11.sp)
                            }
                            Text("Quelle: $sourceName", color = mutedColor, fontSize = 11.sp)
                            Text("Erkannt: $detectedPgnText", color = mutedColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            if (!entry.payloadLength.isNullOrBlank()) {
                                Text("Payload-Länge: ${entry.payloadLength}B", color = mutedColor, fontSize = 11.sp)
                            }
                            if (!entry.payloadHex.isNullOrBlank()) {
                                Text(
                                    text = "Payload: ${entry.payloadHex.take(56)}",
                                    color = mutedColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                )
                            }
                            if (!entry.rawLine.isNullOrBlank()) {
                                Text(
                                    text = "Raw: ${entry.rawLine.take(100)}",
                                    color = mutedColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Bekannte Quellen (${sortedSources.size})",
                color = textColor,
                fontWeight = FontWeight.Bold,
            )

            if (sortedSources.isEmpty()) {
                Text("Noch keine Quelle erkannt.", color = mutedColor)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sortedSources.forEach { source ->
                        val sourceName = source.displayName.ifBlank { source.sourceKey }
                        val sourceAge = formatSourceAgeMillis(nowMs - source.lastSeenMs)
                        val pgnText = source.pgns
                            .distinct()
                            .sorted()
                            .joinToString(", ") { pgn -> formatNmeaPgnLine(pgn) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = cardColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = sourceName, color = textColor, fontWeight = FontWeight.SemiBold)
                                Text(text = sourceAge, color = mutedColor, fontSize = 11.sp)
                            }
                            Text("${source.sourceKey}", color = mutedColor, fontSize = 11.sp)
                            Text(
                                text = "PGN: ${if (pgnText.isBlank()) "keine" else pgnText}",
                                color = mutedColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Nmea0183Widget(
    detectedSources: List<NmeaSourceProfile>,
    received0183History: List<Nmea0183HistoryEntry>,
    telemetryText: Map<String, String>,
    modifier: Modifier = Modifier,
    darkBackground: Boolean = true,
) {
    val textColor = if (darkBackground) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val headerColor = premiumSignalColor
    val mutedColor = textColor.copy(alpha = 0.68f)
    val cardColor = if (darkBackground) {
        SeaFoxDesignTokens.Color.surfaceRaisedDark.copy(alpha = 0.62f)
    } else {
        SeaFoxDesignTokens.Color.surfaceRaisedLight.copy(alpha = 0.82f)
    }
    val liveHistory = remember(received0183History) { received0183History.take(240) }
    var isHistoryPaused by remember { mutableStateOf(false) }
    var pausedHistory by remember { mutableStateOf(liveHistory) }

    LaunchedEffect(liveHistory, isHistoryPaused) {
        if (!isHistoryPaused) {
            pausedHistory = liveHistory
        }
    }

    val currentHistory = if (isHistoryPaused) pausedHistory else liveHistory
    val current = currentHistory.firstOrNull()
    val currentSentence = current?.sentence
        ?: telemetryText["nmea0183_sentence"]?.trim()
        ?: telemetryText["nmea_sentence"]?.trim()
    val currentFullSentence = current?.fullSentence
        ?: telemetryText["nmea0183_sentence_full"]?.trim()
    val currentSource = current?.sourceKey?.ifBlank { null }
        ?: telemetryText["nmea0183_source"]?.trim()
        ?: telemetryText["nmea_source"]?.trim()
    val currentCategory = current?.category
        ?: telemetryText["nmea0183_category"]?.trim()
        ?: telemetryText["nmea0183_user_category"]?.trim()
    val currentRawLine = current?.rawLine ?: telemetryText["nmea0183_raw_line"]?.trim()
    val currentSeenMs = current?.receivedAtMs ?: null
    val recent0183History = currentHistory
    val nowMs = System.currentTimeMillis()

    val activeSources = remember(recent0183History) {
        recent0183History
            .map { it.sourceKey.ifBlank { "unbekannt" } }
            .distinct()
            .sorted()
    }
    val enrichedSources = activeSources.mapNotNull { source ->
        val profile = detectedSources.firstOrNull { it.sourceKey.equals(source, ignoreCase = true) }
        val sourceLabel = profile?.displayName?.ifBlank { source } ?: source
        val lastSeen = detectedSources
            .firstOrNull { it.sourceKey.equals(source, ignoreCase = true) }?.lastSeenMs
        if (source.isBlank()) {
            null
        } else {
            Triple(sourceLabel, source, lastSeen)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "NMEA0183 Empfang",
                color = headerColor,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktuelle Nachricht",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = { isHistoryPaused = !isHistoryPaused },
                    modifier = Modifier.height(28.dp),
                    colors = SeaFoxDesignTokens.NonMenuButton.filledButtonColors(),
                ) {
                    Text(if (isHistoryPaused) "Weiter" else "Pause")
                }
            }

            if (currentSentence.isNullOrBlank() && currentCategory.isNullOrBlank() && currentRawLine.isNullOrBlank()) {
                Text("Noch kein NMEA0183-Satz empfangen.", color = mutedColor)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val sourceLabel = when {
                        currentSource.isNullOrBlank() -> "unbekannt"
                        else -> currentSource
                    }
                    Text("Quelle: $sourceLabel", color = textColor)
                    Text("Satz: ${currentSentence ?: "UNBEKANNT"}", color = textColor)
                    if (!currentFullSentence.isNullOrBlank() && currentFullSentence != currentSentence) {
                        Text(
                            text = "Satz (voll): ${currentFullSentence.take(80)}",
                            color = mutedColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    if (!currentCategory.isNullOrBlank()) {
                        Text("Kategorie: $currentCategory", color = textColor)
                    } else {
                        Text("Kategorie: UNBEKANNT", color = mutedColor, fontSize = 11.sp)
                    }
                    if (!currentRawLine.isNullOrBlank()) {
                        Text(
                            text = "RAW: ${currentRawLine.take(140)}",
                            color = mutedColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        )
                    }
                    currentSeenAge(currentSeenMs, nowMs)?.let { ageText ->
                        Text("Empfangen: $ageText", color = mutedColor, fontSize = 11.sp)
                    }

                    if ((current?.fields?.isNotEmpty() == true)) {
                        Text(
                            text = "Felder (${current.fields.size}):",
                            color = mutedColor,
                            fontSize = 11.sp,
                        )
                        current.fields.take(30).forEach { field ->
                            Text(field, color = mutedColor, fontSize = 10.sp, maxLines = 1)
                        }
                        if (current.fields.size > 30) {
                            Text(
                                text = "+ ${current.fields.size - 30} weitere",
                                color = mutedColor,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }

            Text(
                text = "Alle empfangenen NMEA0183-Sätze (${recent0183History.size})",
                color = textColor,
                fontWeight = FontWeight.Bold,
            )

            if (recent0183History.isEmpty()) {
                Text("Noch keine NMEA0183-Sätze empfangen.", color = mutedColor)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recent0183History.forEach { entry ->
                        val sourceName = entry.sourceKey.ifBlank { "unbekannt" }
                        val sourceProfile = detectedSources.firstOrNull { it.sourceKey == sourceName }
                        val sourceDisplay = sourceProfile?.displayName?.ifBlank { sourceName } ?: sourceName
                        val ageText = currentSeenAge(entry.receivedAtMs, nowMs)
                        val categoryLabel = entry.category?.ifBlank { "UNBEKANNT" } ?: "UNBEKANNT"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = cardColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Satz: ${entry.sentence}", color = textColor, fontWeight = FontWeight.SemiBold)
                                Text(text = ageText, color = mutedColor, fontSize = 11.sp)
                            }
                            Text("Quelle: $sourceDisplay", color = mutedColor, fontSize = 11.sp)
                            Text("Kategorie: $categoryLabel", color = mutedColor, fontSize = 11.sp)
                            if (!entry.fullSentence.isNullOrBlank() && entry.fullSentence != entry.sentence) {
                                Text("Vollsatz: ${entry.fullSentence}", color = mutedColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (!entry.rawLine.isNullOrBlank()) {
                                Text(
                                    text = "Raw: ${entry.rawLine.take(100)}",
                                    color = mutedColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                )
                            }
                            if (entry.fields.isNotEmpty()) {
                                Text("Felder: ${entry.fields.take(6).joinToString()}${if (entry.fields.size > 6) ", ..." else ""}", color = mutedColor, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            if (enrichedSources.isNotEmpty()) {
                Text(
                    text = "Bekannte Quellen (${enrichedSources.size})",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enrichedSources.forEach { (displayName, sourceKey, lastSeenMs) ->
                        val sourceAge = lastSeenMs?.let { formatSourceAgeMillis(nowMs - it) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = cardColor.copy(alpha = if (darkBackground) 0.52f else 0.74f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(displayName, color = textColor, fontWeight = FontWeight.SemiBold)
                                sourceAge?.let { age -> Text(age, color = mutedColor, fontSize = 11.sp) }
                            }
                            Text(sourceKey, color = mutedColor, fontSize = 10.sp)
                        }
                    }
                }
            } else {
                Text("Noch keine Quelle erkannt.", color = mutedColor)
            }
        }
    }
}

private fun currentSeenAge(receivedAtMs: Long?, nowMs: Long): String {
    if (receivedAtMs == null || receivedAtMs <= 0L) {
        return "unbekannt"
    }
    return formatSourceAgeMillis(nowMs - receivedAtMs)
}

private fun formatNmeaPgnLine(pgn: Int): String {
    val label = NMEA_PGN_LABELS[pgn]?.let { " ($it)" } ?: ""
    return "$pgn$label"
}

private fun formatSourceAgeMillis(ageMs: Long): String {
    if (ageMs <= 0L) return "gerade eben"
    val ageSec = ageMs / 1000L
    return when {
        ageSec < 60L -> "vor ${ageSec}s"
        ageSec < 3600L -> {
            val m = ageSec / 60L
            "vor ${m}m"
        }

        else -> {
            val h = ageSec / 3600L
            "vor ${h}h"
        }
    }
}

private fun formatAisCpaTime(minutes: Float?): String {
    if (minutes == null || minutes.isNaN() || minutes.isInfinite()) return "—"
    val totalSeconds = (minutes * 60f).roundToInt().coerceAtLeast(0)
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "${mins}min ${secs}s"
}

@Composable
fun AutopilotWidget(
    heading: Float,
    mode: AutopilotControlMode,
    targetHeading: Float?,
    rudderAngleDeg: Float?,
    averageRudderAngleDeg: Float?,
    onModeSelect: (AutopilotControlMode) -> Unit,
    onAdjustTarget: (Float) -> Unit,
    onTack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = premiumWidgetTextColor()
    val mutedColor = premiumWidgetMutedColor()
    val chipColor = premiumWidgetChipColor()
    val effectiveTarget = targetHeading ?: heading
    val targetDeviation = abs(normalizeSignedAngle(effectiveTarget - heading))
    val targetColor = if (targetDeviation <= 2f) premiumPositiveColor else premiumDangerColor
    val rudder = rudderAngleDeg ?: 0f
    val averageRudder = averageRudderAngleDeg ?: 0f
    val rudderColor = when {
        rudder < -1f -> premiumDangerColor
        rudder <= 1f -> textColor
        else -> premiumDangerColor
    }
    val avgRudderColor = when {
        averageRudder < -1f -> premiumDangerColor
        averageRudder <= 1f -> textColor
        else -> premiumPositiveColor
    }
    val autopilotInteractionSource = remember { MutableInteractionSource() }
    val baseTextStyle = LocalTextStyle.current
    val baseAutopilotFontSizeSp = if (baseTextStyle.fontSize == TextUnit.Unspecified) 12f else baseTextStyle.fontSize.value
    val autopilotTextSize = (baseAutopilotFontSizeSp - 2f).coerceAtLeast(8f).sp
    val providedAutopilotTextStyle = baseTextStyle.copy(fontSize = autopilotTextSize)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val valueTextSize = (minOf(maxWidth.value, maxHeight.value) * 0.09f).coerceIn(12f, 28f).sp
        val reducedValueTextSize = (valueTextSize.value - 2f).coerceAtLeast(10f).sp
        val modeButtonTextSize = reducedValueTextSize
        val controlButtonHeight = (minOf(maxWidth.value, maxHeight.value) * 0.23f).coerceIn(52f, 92f).dp
        val modeRowHeight = (minOf(maxWidth.value, maxHeight.value) * 0.16f).coerceIn(30f, 46f).dp

        ProvideTextStyle(value = providedAutopilotTextStyle) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(modeRowHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(AutopilotControlMode.WIND, AutopilotControlMode.STBY, AutopilotControlMode.KURS).forEachIndexed { index, label ->
                    Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = autopilotInteractionSource,
                            indication = null,
                            onClick = { onModeSelect(label) }
                        )
                        .background(
                            if (mode == label) premiumSignalColor.copy(alpha = 0.72f)
                            else Color.Transparent
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label.label,
                            color = if (mode == label) Color(0xFF03131B) else textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = modeButtonTextSize,
                        )
                    }
                    if (index < 2) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(mutedColor.copy(alpha = 0.25f))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .zIndex(2f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Kurs ${heading.roundToInt()}°",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = reducedValueTextSize
                    )
                    Text(
                        text = "Soll ${effectiveTarget.roundToInt()}°",
                        color = targetColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = reducedValueTextSize
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Ruder ${formatSignedDegrees(rudder)}", color = rudderColor, fontWeight = FontWeight.SemiBold, fontSize = reducedValueTextSize)
                    Text("AVR ${formatSignedDegrees(averageRudder)}", color = avgRudderColor, fontSize = reducedValueTextSize)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(premiumDangerColor.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .height(controlButtonHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable(
                                    interactionSource = autopilotInteractionSource,
                                    indication = null,
                                    onClick = { onAdjustTarget(-1f) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-1°", color = premiumDangerColor.copy(alpha = 0.88f))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(mutedColor.copy(alpha = 0.24f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable(
                                    interactionSource = autopilotInteractionSource,
                                    indication = null,
                                    onClick = { onAdjustTarget(-10f) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-10°", color = premiumDangerColor)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(controlButtonHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(premiumSignalColor.copy(alpha = 0.84f))
                        .clickable(
                            interactionSource = autopilotInteractionSource,
                            indication = null,
                            onClick = onTack
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 4.dp.toPx()
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val radius = minOf(size.width, size.height) * 0.26f
                        val headLength = 6.5.dp.toPx()
                        val headWidth = 5f

                        val drawArrowHead = { angleDeg: Float, clockwise: Boolean ->
                            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                            val end = androidx.compose.ui.geometry.Offset(
                                center.x + cos(angleRad) * radius,
                                center.y + sin(angleRad) * radius
                            )
                            val tangent = if (clockwise) angleRad + kotlin.math.PI.toFloat() / 2f else angleRad - kotlin.math.PI.toFloat() / 2f
                            val left = androidx.compose.ui.geometry.Offset(
                                end.x + cos(tangent + kotlin.math.PI.toFloat() / 2f) * headLength + cos(tangent) * (headWidth * 0.5f),
                                end.y + sin(tangent + kotlin.math.PI.toFloat() / 2f) * headLength + sin(tangent) * (headWidth * 0.5f),
                            )
                            val right = androidx.compose.ui.geometry.Offset(
                                end.x + cos(tangent - kotlin.math.PI.toFloat() / 2f) * headLength + cos(tangent) * (headWidth * 0.5f),
                                end.y + sin(tangent - kotlin.math.PI.toFloat() / 2f) * headLength + sin(tangent) * (headWidth * 0.5f),
                            )
                            val head = Path().apply {
                                moveTo(end.x + cos(tangent) * headWidth * 0.15f, end.y + sin(tangent) * headWidth * 0.15f)
                                lineTo(left.x, left.y)
                                lineTo(right.x, right.y)
                                close()
                            }
                            drawPath(head, Color(0xFF03131B))
                        }

                        val arcRectSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 1.2f)
                        val topArcRect = androidx.compose.ui.geometry.Rect(
                            center.x - radius,
                            center.y - radius * 0.85f,
                            center.x + radius,
                            center.y + radius * 0.35f,
                        )
                        val bottomArcRect = androidx.compose.ui.geometry.Rect(
                            center.x - radius,
                            center.y - radius * 0.35f,
                            center.x + radius,
                            center.y + radius * 0.85f,
                        )

                        val topStart = 225f
                        val topSweep = 95f
                        val topEnd = topStart + topSweep
                        val bottomStart = 315f
                        val bottomSweep = -95f
                        val bottomEnd = bottomStart + bottomSweep

                        drawArc(
                            color = Color(0xFF03131B),
                            startAngle = topStart,
                            sweepAngle = topSweep,
                            useCenter = false,
                            topLeft = topArcRect.topLeft,
                            size = arcRectSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = Color(0xFF03131B),
                            startAngle = bottomStart,
                            sweepAngle = bottomSweep,
                            useCenter = false,
                            topLeft = bottomArcRect.topLeft,
                            size = arcRectSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )

                        drawArrowHead(topEnd, true)
                        drawArrowHead(bottomEnd, false)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(premiumPositiveColor.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .height(controlButtonHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable(
                                    interactionSource = autopilotInteractionSource,
                                    indication = null,
                                    onClick = { onAdjustTarget(1f) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+1°", color = premiumPositiveColor.copy(alpha = 0.88f))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(mutedColor.copy(alpha = 0.24f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable(
                                    interactionSource = autopilotInteractionSource,
                                    indication = null,
                                    onClick = { onAdjustTarget(10f) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+10°", color = premiumPositiveColor)
                }
            }
        }
        }
    }
}
    }
        }

private fun DrawScope.drawTopBoatOutline() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val bowY = h * 0.08f
    val sternY = h * 0.92f
    val halfBeam = w * 0.2f

    val hull = Path().apply {
        moveTo(cx, bowY)
        quadraticBezierTo(cx + halfBeam * 1.25f, h * 0.24f, cx + halfBeam, h * 0.72f)
        lineTo(cx + halfBeam * 0.56f, sternY)
        lineTo(cx - halfBeam * 0.56f, sternY)
        lineTo(cx - halfBeam, h * 0.72f)
        quadraticBezierTo(cx - halfBeam * 1.25f, h * 0.24f, cx, bowY)
        close()
    }
    drawPath(
        path = hull,
        color = Color.White.copy(alpha = 0.9f),
        style = Stroke(width = (w * 0.012f).coerceAtLeast(2f))
    )
}

private fun DrawScope.drawRudderGauge(
    rudderAngleDeg: Float,
    rudderColor: Color,
    maxRudderDeg: Float = 20f,
) {
    val width = size.width
    val height = size.height
    val linePadding = width * 0.07f
    val seaY = height * 0.82f

    val centerX = width * 0.5f
    val clampedRudder = rudderAngleDeg.coerceIn(-maxRudderDeg, maxRudderDeg)
    val offsetRange = width * 0.4f - linePadding
    val rudderX = centerX + (clampedRudder / maxRudderDeg) * offsetRange
    val markerRadiusPx = 2.5f

    drawCircle(
        color = Color.White,
        radius = markerRadiusPx,
        center = androidx.compose.ui.geometry.Offset(centerX, seaY),
    )

    drawCircle(
        color = rudderColor,
        radius = markerRadiusPx,
        center = androidx.compose.ui.geometry.Offset(rudderX, seaY),
    )
}

private fun formatEchosounderDepth(depthM: Float, unit: EchosounderDepthUnit): String {
    if (!depthM.isFinite() || depthM < 0f) return "—"
    val displayValue = when (unit) {
        EchosounderDepthUnit.METERS -> depthM
        EchosounderDepthUnit.INCH -> depthM * 39.3701f
    }
    val rounded = (displayValue * 10f).roundToInt() / 10f
    val suffix = if (unit == EchosounderDepthUnit.INCH) "inch" else "m"
    return "$rounded $suffix"
}

private fun playEchosounderAlarmTone(
    frequencyHz: Int,
    durationMs: Int = 450,
    volume: Float = 0.7f,
) {
    playWidgetAlarmTone(frequencyHz, durationMs, volume = volume)
}

private fun playAisCollisionAlarmTone(context: Context, volume: Float = 0.7f) {
    applySystemAlarmPresetVolume(context = context, preset = volume)
    playWidgetAlarmTone(1175, 500, volume = volume)
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

fun playWidgetAlarmTone(
    frequencyHz: Int,
    durationMs: Int = 450,
    volume: Float = 0.7f,
) {
    if (frequencyHz <= 0) return
    val normalizedVolume = volume.coerceIn(0f, 2f)
    if (normalizedVolume <= 0f) return

    thread(start = true) {
        try {
            val sampleRate = 22050
            val durationSec = durationMs / 1000f
            val sampleCount = (sampleRate * durationSec).toInt().coerceAtLeast(200)
            val bytes = ByteArray(sampleCount * 2)
            val angularStep = 2 * PI * frequencyHz / sampleRate
            for (i in 0 until sampleCount) {
                val envelope = when {
                    i < 40 -> i / 40f
                    i > sampleCount - 40 -> (sampleCount - i) / 40f
                    else -> 1f
                }.coerceIn(0f, 1f)

                val sample = (kotlin.math.sin(i * angularStep) * Short.MAX_VALUE * 0.3f * normalizedVolume * envelope).toInt().toShort()
                val idx = i * 2
                bytes[idx] = (sample.toInt() and 0xFF).toByte()
                bytes[idx + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bytes.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.setVolume(normalizedVolume.coerceAtMost(1f))
            track.write(bytes, 0, bytes.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 80L)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // ignore tone playback errors
        }
    }
}

internal fun wrap360(value: Float): Float {
    var wrapped = value % 360f
    if (wrapped < 0f) wrapped += 360f
    return wrapped
}

internal fun normalizeSignedAngle(value: Float): Float {
    val wrapped = wrap360(value)
    return if (wrapped > 180f) wrapped - 360f else wrapped
}

private fun formatSignedDegrees(value: Float): String {
    val rounded = value.roundToInt()
    val sign = if (rounded >= 0) "+" else ""
    return "$sign$rounded°"
}

fun pickValue(data: Map<String, Float>, keys: List<String>): Float {
    return pickValueOrNull(data, keys) ?: 0f
}

fun pickValueOrNull(data: Map<String, Float>, keys: List<String>): Float? {
    val normalizedData = mutableMapOf<String, Float>()
    data.forEach { (rawKey, value) ->
        if (value.isFinite()) {
            val normalizedKey = normalizeTelemetryKey(rawKey)
            if (!normalizedData.containsKey(normalizedKey)) {
                normalizedData[normalizedKey] = value
            }
        }
    }

    for (key in keys) {
        val directValue = data[key]
        if (directValue != null) return directValue

        val loweredKey = key.lowercase()
        val lowerValue = data[loweredKey]
        if (lowerValue != null) return lowerValue

        val normalizedKey = normalizeTelemetryKey(key)
        val normalizedValue = normalizedData[normalizedKey]
        if (normalizedValue != null) return normalizedValue

        val keyAlternatives = setOf(
            normalizeTelemetryKey(loweredKey.replace("_", ".")),
            normalizeTelemetryKey(loweredKey.replace(".", "_")),
        )
        keyAlternatives.forEach { alternative ->
            normalizedData[alternative]?.let { return it }
        }
    }
    return null
}

private fun normalizeTelemetryKey(rawKey: String): String {
    return rawKey
        .lowercase()
        .replace(".", "")
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
}

private fun minDp(a: Dp, b: Dp): Dp = if (a < b) a else b

private fun formatDistance(value: Float?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${(value * 10f).roundToInt() / 10f} NM"
}

private fun formatNm(value: Float?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${(value * 10f).roundToInt() / 10f}"
}

private fun formatSpeedWithUnit(value: Float?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${(value * 10f).roundToInt() / 10f} kn"
}

private fun formatDalyPercent(value: Float?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${(value * 10f).roundToInt() / 10f} %"
}

private fun formatDalyNumeric(value: Float?, unit: String): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${(value * 10f).roundToInt() / 10f} $unit"
}

internal fun formatLatitude(value: Float?): String {
    if (value == null || value.isNaN() || value !in -90f..90f) return "—"
    val direction = if (value >= 0f) "N" else "S"
    return "${String.format(Locale.US, "%.4f", value.absoluteValue)} $direction"
}

internal fun formatLongitude(value: Float?): String {
    if (value == null || value.isNaN() || value !in -180f..180f) return "—"
    val direction = if (value >= 0f) "E" else "W"
    return "${String.format(Locale.US, "%.4f", value.absoluteValue)} $direction"
}

private fun gpsFixText(fixQuality: Float?): String {
    val quality = fixQuality
        ?.roundToInt()
        ?: return "—"
    return when (quality) {
        0 -> "kein Fix"
        1 -> "GPS"
        2 -> "DGPS"
        3 -> "PPS"
        4 -> "RTK Fix"
        5 -> "RTK Float"
        6 -> "Weitere"
        else -> quality.toString()
    }
}

private fun normalizeSignedTo0_360(value: Float): Float {
    val wrapped = value % 360f
    return if (wrapped < 0f) wrapped + 360f else wrapped
}

internal fun formatAngle(value: Float?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${value.roundToInt()}°"
}

internal fun formatSpeed(value: Float?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "${(value * 10f).roundToInt() / 10f} kn"
}
