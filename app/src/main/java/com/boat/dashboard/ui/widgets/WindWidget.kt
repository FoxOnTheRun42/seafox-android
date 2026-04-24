package com.seafox.nmea_dashboard.ui.widgets

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.seafox.nmea_dashboard.R
import com.seafox.nmea_dashboard.data.UiFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WindWidget(
    apparentAngleDeg: Float,
    apparentSpeedKn: Float,
    absoluteSpeedKn: Float,
    headingDeg: Float,
    trueWindAngleDeg: Float,
    uiFont: UiFont,
    settings: WindWidgetSettings,
    historyMinKn: Float?,
    historyMaxKn: Float?,
    darkBackground: Boolean,
    modifier: Modifier = Modifier,
    onToggleMinMaxSource: () -> Unit = {},
) {
    val apparentAngleForRender = if (apparentAngleDeg.isFinite()) apparentAngleDeg else 0f
    val signedBoatAngle = normalizeSignedAngle(apparentAngleForRender)
    val northAngle = wrap360(headingDeg + signedBoatAngle)
    val halfTackDisplayAngle = (settings.tackingAngleDeg * 0.5f * 1.3f).coerceAtMost(179f)
    val relativeSpeedText = formatWindSpeed(apparentSpeedKn, settings.speedUnit)
    val absoluteSpeedText = formatWindSpeed(absoluteSpeedKn, settings.speedUnit)
    val trueWindText = if (trueWindAngleDeg.isFinite()) formatSignedDegrees(normalizeSignedAngle(trueWindAngleDeg)) else null
    val context = LocalContext.current
    val paintTypeface = if (uiFont == UiFont.ORBITRON) {
        ResourcesCompat.getFont(context, R.font.orbitron_variable) ?: Typeface.DEFAULT
    } else if (uiFont == UiFont.PT_MONO) {
        ResourcesCompat.getFont(context, R.font.dot_gothic16_regular) ?: Typeface.MONOSPACE
    } else if (uiFont == UiFont.ELECTROLIZE) {
        ResourcesCompat.getFont(context, R.font.electrolize_regular) ?: Typeface.SANS_SERIF
    } else if (uiFont == UiFont.DOT_GOTHIC) {
        ResourcesCompat.getFont(context, R.font.dot_gothic16_regular) ?: Typeface.SANS_SERIF
    } else {
        Typeface.create("sans-serif", Typeface.NORMAL)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val referenceWidthPx = with(density) { 340.dp.toPx() }
        val referenceHeightPx = with(density) { 340.dp.toPx() }
        val widthScale = if (referenceWidthPx > 0f) widthPx / referenceWidthPx else 1f
        val heightScale = if (referenceHeightPx > 0f) heightPx / referenceHeightPx else 1f
        val windBaseFontScale = 1.5f
        val widgetTextScale = ((widthScale + heightScale) * 0.5f).coerceIn(0.55f, 2.6f)
        val dataTextPx = with(density) { LocalTextStyle.current.fontSize.toPx() }
        val sixSpPx = with(density) { 6.sp.toPx() }
        val twoSpPx = with(density) { 2.sp.toPx() }
        val centerTextPx = (dataTextPx * 1.05f * widgetTextScale * windBaseFontScale).coerceIn(14f, 90f)
        val windValueBoostPx = sixSpPx
        val cornerTextPx = (dataTextPx * 0.9f * widgetTextScale * windBaseFontScale).coerceIn(12f, 80f)
        val minMaxTextPx = (cornerTextPx - sixSpPx).coerceAtLeast(8f)
        val minMaxValueTextPx = minMaxTextPx + sixSpPx
        var minMaxModeRect1 by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
        var minMaxModeRect2 by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cornerTextPx, minMaxModeRect1, minMaxModeRect2) {
                        detectTapGestures { tap ->
                            if (minMaxModeRect1.contains(tap) || minMaxModeRect2.contains(tap)) {
                                onToggleMinMaxSource()
                            }
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = minOf(size.width, size.height) * 0.398f
                val ringStroke = (radius * 0.07f).coerceAtLeast(2f)
                val sectorStroke = ringStroke * 1.35f
                val trueWindColor = Color(0xFF1E88E5)
                val arcTopLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius)
                val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                val labelTextColor = if (darkBackground) {
                    android.graphics.Color.WHITE
                } else {
                    android.graphics.Color.BLACK
                }
                drawArc(
                    color = Color.Red.copy(alpha = 0.9f),
                    startAngle = 270f - halfTackDisplayAngle,
                    sweepAngle = halfTackDisplayAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = sectorStroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Color(0xFF2ECC71),
                    startAngle = 270f,
                    sweepAngle = halfTackDisplayAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = sectorStroke, cap = StrokeCap.Round),
                )

                val tickColor = Color.Gray.copy(alpha = 0.5f)
                val normalTickHalfLength = radius * 0.03f
                val majorTickHalfLength = normalTickHalfLength * 2f
                val tickThickness = ((radius * 0.012f).coerceAtLeast(1f)) * 2f
                for (deg in -180..180 step 45) {
                    val isMajor = deg % 90 == 0
                    val isMajorTick = isMajor
                    val angle = (deg - 90f) * PI.toFloat() / 180f
                    val halfLength = if (isMajorTick) majorTickHalfLength else normalTickHalfLength
                    // Tick lines start and end exactly half of their length on either side of circle edge.
                    val innerRadius = (radius - halfLength).coerceAtLeast(1f)
                    val outerRadius = radius + halfLength
                    val inner = androidx.compose.ui.geometry.Offset(
                        cx + kotlin.math.cos(angle).toFloat() * innerRadius,
                        cy + kotlin.math.sin(angle).toFloat() * innerRadius,
                    )
                    val outer = androidx.compose.ui.geometry.Offset(
                        cx + kotlin.math.cos(angle).toFloat() * outerRadius,
                        cy + kotlin.math.sin(angle).toFloat() * outerRadius,
                    )
                    drawLine(
                        color = tickColor,
                        start = inner,
                        end = outer,
                        strokeWidth = if (isMajorTick) tickThickness * 1.45f else tickThickness,
                    )
                }

                fun drawWindArrow(
                    angleDeg: Float,
                    color: Color,
                    sizeScale: Float = 1f,
                ) {
                    if (!angleDeg.isFinite()) return
                    val localSignedAngle = normalizeSignedAngle(angleDeg)
                    val localCanvasAngle = 270f + localSignedAngle
                    val localAngleRad = (localCanvasAngle * PI / 180f).toFloat()
                    val outX = cos(localAngleRad)
                    val outY = sin(localAngleRad)
                    val tanX = -outY
                    val tanY = outX
                    val baseRadius = radius * 1.08f * sizeScale
                    val tipRadius = radius * 0.48f * sizeScale
                    val baseHalfWidth = radius * 0.066f * sizeScale
                    val baseCenterX = cx + outX * baseRadius
                    val baseCenterY = cy + outY * baseRadius

                    val tip = androidx.compose.ui.geometry.Offset(cx + outX * tipRadius, cy + outY * tipRadius)
                    val p1 = androidx.compose.ui.geometry.Offset(
                        baseCenterX + tanX * baseHalfWidth,
                        baseCenterY + tanY * baseHalfWidth,
                    )
                    val p2 = androidx.compose.ui.geometry.Offset(
                        baseCenterX - tanX * baseHalfWidth,
                        baseCenterY - tanY * baseHalfWidth,
                    )

                    val arrow = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        close()
                    }
                    drawPath(path = arrow, color = color)
                }

                drawWindArrow(
                    angleDeg = apparentAngleDeg,
                    color = Color.White,
                )
                if (trueWindAngleDeg.isFinite()) {
                    drawWindArrow(
                        angleDeg = trueWindAngleDeg,
                        color = trueWindColor,
                        sizeScale = 0.552f,
                    )
                }

                val centerPaint = Paint().apply {
                    color = labelTextColor
                    textSize = centerTextPx
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = paintTypeface
                }
                val apparentSpeedPaint = Paint(centerPaint).apply {
                    textSize = (centerTextPx * 1.25f + windValueBoostPx - twoSpPx).coerceAtLeast(8f)
                }
                val labelOffsetFactor = 0.75f

                fun drawOutlinedText(
                    text: String,
                    x: Float,
                    y: Float,
                    paint: Paint,
                    outlineColor: Int = android.graphics.Color.BLACK,
                ) {
                    val outlinePaint = Paint(paint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (paint.textSize * 0.17f).coerceAtLeast(1.2f)
                        color = outlineColor
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(text, x, y, outlinePaint)
                    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
                }

                fun drawLabeledValue(
                    value: String,
                    label: String,
                    x: Float,
                    y: Float,
                    basePaint: Paint,
                    baseAlign: Paint.Align = Paint.Align.CENTER,
                    outlined: Boolean = false,
                    valueAboveLabel: Boolean = false,
                ) {
                    val valuePaint = Paint(basePaint).apply {
                        textAlign = baseAlign
                    }
                    val labelPaint = Paint(basePaint).apply {
                        textAlign = baseAlign
                        textSize = (valuePaint.textSize * 0.5f)
                    }
                    if (outlined) {
                        if (valueAboveLabel) {
                            drawOutlinedText(
                                text = value,
                                x = x,
                                y = y,
                                paint = valuePaint,
                            )
                            drawOutlinedText(
                                text = label,
                                x = x,
                                y = y + valuePaint.textSize * labelOffsetFactor,
                                paint = labelPaint,
                            )
                        } else {
                            drawOutlinedText(value, x, y, valuePaint)
                            drawOutlinedText(
                                text = label,
                                x = x,
                                y = y + valuePaint.textSize * labelOffsetFactor,
                                paint = labelPaint,
                            )
                        }
                    } else {
                        drawContext.canvas.nativeCanvas.drawText(value, x, y, valuePaint)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            y + valuePaint.textSize * labelOffsetFactor,
                            labelPaint
                        )
                    }
                }

                if (settings.showWindSpeed) {
                    drawLabeledValue(
                        value = relativeSpeedText,
                        label = "AWS",
                        cx,
                        cy - radius * 0.55f,
                        basePaint = apparentSpeedPaint,
                        baseAlign = Paint.Align.CENTER,
                        outlined = true,
                        valueAboveLabel = true,
                    )
                }

                if (settings.showBoatDirection) {
                    val apparentDirectionPaint = Paint().apply {
                        color = labelTextColor
                        textSize = (cornerTextPx * 1.3f + windValueBoostPx - twoSpPx).coerceAtLeast(8f)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                        typeface = paintTypeface
                    }
                    drawLabeledValue(
                        value = formatSignedDegrees(signedBoatAngle),
                        label = "AWA",
                        cx,
                        cy + radius * 0.68f,
                        basePaint = apparentDirectionPaint,
                        baseAlign = Paint.Align.CENTER,
                        outlined = true,
                        valueAboveLabel = true,
                    )
                }

                val cornerPaint = Paint().apply {
                    color = labelTextColor
                    textSize = cornerTextPx
                    textAlign = Paint.Align.LEFT
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = paintTypeface
                }
                val trueWindInfoPaint = Paint().apply {
                    color = trueWindColor.toArgb()
                    textSize = cornerTextPx
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = paintTypeface
                }
                val northDirectionTextPx = (cornerTextPx - with(density) { 2.sp.toPx() }).coerceAtLeast(8f)
                val northDirectionPaint = Paint().apply {
                    color = Color.Gray.copy(alpha = 0.5f).toArgb()
                    textSize = northDirectionTextPx
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                    typeface = paintTypeface
                }
                val bottomValuePaint = Paint(trueWindInfoPaint).apply {
                    textSize = cornerTextPx + windValueBoostPx
                }
                val bottomTitlePaint = Paint(bottomValuePaint).apply {
                    textSize = cornerTextPx * 0.55f
                }
                val twsTwaValueTextPx = (cornerTextPx + windValueBoostPx - twoSpPx).coerceAtLeast(8f)
                val twsTwaTitleTextPx = (cornerTextPx * 0.55f - twoSpPx).coerceAtLeast(8f)
                val twsTwaTitleTextBoostPx = twsTwaTitleTextPx + with(density) { 2.sp.toPx() }
                val bottomLineGap = twsTwaValueTextPx * 0.85f
                val twsTwaBaselineOffsetPx = bottomLineGap * 0.12f
                val twsTwaVerticalLiftPx = with(density) { 6.dp.toPx() }

                if (settings.showNorthDirection) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "°/N ${northAngle.roundToInt()}°",
                        cx,
                        cy + cornerTextPx * 0.35f,
                        northDirectionPaint
                    )
                }

                val twsBottomY = size.height - radius * 0.12f - twsTwaBaselineOffsetPx - twsTwaVerticalLiftPx
                val twsBottomX = 0f
                val twaBottomY = twsBottomY
                val twaBottomX = size.width
                val windSpeedText = absoluteSpeedText
                val twaValueX = twaBottomX
                val twsValueX = twsBottomX
                val twaTextPaint = Paint(bottomValuePaint).apply {
                    textAlign = Paint.Align.RIGHT
                }
                val twsTextPaint = Paint(bottomValuePaint).apply {
                    textAlign = Paint.Align.LEFT
                }
                val twaLabelPaint = Paint(bottomTitlePaint).apply {
                    textAlign = Paint.Align.RIGHT
                }
                val twsLabelPaint = Paint(bottomTitlePaint).apply {
                    textAlign = Paint.Align.LEFT
                }
                twaTextPaint.textSize = twsTwaValueTextPx
                twsTextPaint.textSize = twsTwaValueTextPx
                twaLabelPaint.textSize = twsTwaTitleTextBoostPx
                twsLabelPaint.textSize = twsTwaTitleTextBoostPx

                drawContext.canvas.nativeCanvas.drawText(
                    windSpeedText,
                    twsValueX,
                    twsBottomY,
                    twsTextPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "TWS",
                    twsValueX,
                    twsBottomY + bottomLineGap,
                    twsLabelPaint
                )

                if (trueWindText != null) {
                    drawContext.canvas.nativeCanvas.drawText(
                        trueWindText,
                        twaValueX,
                        twaBottomY,
                        twaTextPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "TWA",
                        twaValueX,
                        twaBottomY + bottomLineGap,
                        twaLabelPaint
                    )
                }

                val minSourceLabel = if (settings.minMaxUsesTrueWind) "TWS" else "AWS"
                val maxSourceLabel = if (settings.minMaxUsesTrueWind) "TWS" else "AWS"
                val minLine1Text = "Min $minSourceLabel"
                val maxLine1Text = "Max $maxSourceLabel"
                val minLine2Text = historyMinKn?.let { formatWindSpeed(it, settings.speedUnit) }
                val maxLine2Text = historyMaxKn?.let { formatWindSpeed(it, settings.speedUnit) }
                val minMaxColorPaint = if (settings.minMaxUsesTrueWind) trueWindInfoPaint else cornerPaint
                val minMaxValuePaint = Paint(if (settings.minMaxUsesTrueWind) {
                    Paint(trueWindInfoPaint)
                } else {
                    Paint(cornerPaint)
                }).apply {
                    textSize = twsTwaValueTextPx
                }
                val minMaxLabelPaint = Paint(minMaxColorPaint).apply {
                    isFakeBoldText = true
                    isAntiAlias = true
                    textSize = twsTwaTitleTextBoostPx
                }
                val minMaxTapPadding = 12f
                val lineTop = radius * 0.22f
                val minValuePaint = Paint(minMaxValuePaint).apply {
                    textAlign = Paint.Align.LEFT
                }
                val maxValuePaint = Paint(minMaxValuePaint).apply {
                    textAlign = Paint.Align.RIGHT
                }
                val minLabelPaint = Paint(minMaxLabelPaint).apply {
                    textAlign = Paint.Align.LEFT
                }
                val maxLabelPaint = Paint(minMaxLabelPaint).apply {
                    textAlign = Paint.Align.RIGHT
                }
                val minLineTop = lineTop
                val minLabelTop = lineTop + minMaxValueTextPx * 0.95f
                val maxLineTop = lineTop
                val maxLabelTop = lineTop + minMaxValueTextPx * 0.95f

                if (minLine2Text != null) {
                    val minTouchWidth = maxOf(
                        minMaxValuePaint.measureText(minLine1Text),
                        minMaxValuePaint.measureText(minLine2Text),
                    ) + minMaxTapPadding
                    val clampedMinBaseX = 0f
                    drawContext.canvas.nativeCanvas.drawText(
                        minLine2Text,
                        clampedMinBaseX,
                        minLineTop,
                        minValuePaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        minLine1Text,
                        clampedMinBaseX,
                        minLabelTop,
                        minLabelPaint
                    )
                    minMaxModeRect1 = Rect(
                        clampedMinBaseX,
                        minLineTop - minMaxValueTextPx * 0.75f,
                        clampedMinBaseX + minTouchWidth,
                        minLabelTop + minMaxValueTextPx * 0.75f
                    )
                } else {
                    minMaxModeRect1 = Rect(0f, 0f, 0f, 0f)
                }

                if (maxLine2Text != null) {
                    val maxBaseX = size.width
                    val maxPadding = 12f
                    val maxTouchWidth = maxOf(
                        minMaxValuePaint.measureText(maxLine1Text),
                        minMaxValuePaint.measureText(maxLine2Text),
                    ) + maxPadding
                    drawContext.canvas.nativeCanvas.drawText(
                        maxLine2Text,
                        maxBaseX,
                        maxLineTop,
                        maxValuePaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        maxLine1Text,
                        maxBaseX,
                        maxLabelTop,
                        maxLabelPaint
                    )
                    minMaxModeRect2 = Rect(
                        maxBaseX - maxTouchWidth,
                        maxLineTop - minMaxValueTextPx * 0.75f,
                        size.width,
                        maxLabelTop + minMaxValueTextPx * 0.75f
                    )
                } else {
                    minMaxModeRect2 = Rect(0f, 0f, 0f, 0f)
                }

            }
        }
    }
}

private fun formatWindSpeed(speedKn: Float, unit: WindSpeedUnit): String {
    val clampedKn = speedKn.coerceAtLeast(0f)
    return when (unit) {
        WindSpeedUnit.KNOTS -> "${(clampedKn * 10f).roundToInt() / 10f} kn"
        WindSpeedUnit.MPS -> "${((clampedKn * 0.514444f) * 10f).roundToInt() / 10f} m/s"
        WindSpeedUnit.KMH -> "${((clampedKn * 1.852f) * 10f).roundToInt() / 10f} km/h"
        WindSpeedUnit.BEAUFORT -> "${speedKnToBeaufort(clampedKn)} Bft"
    }
}

private fun speedKnToBeaufort(speedKn: Float): Int {
    val mps = speedKn * 0.514444f
    val limits = floatArrayOf(0.3f, 1.6f, 3.4f, 5.5f, 8.0f, 10.8f, 13.9f, 17.2f, 20.8f, 24.5f, 28.5f, 32.7f)
    for (index in limits.indices) {
        if (mps < limits[index]) return index
    }
    return 12
}

private fun formatSignedDegrees(value: Float): String {
    val rounded = value.roundToInt()
    val sign = if (rounded >= 0) "+" else ""
    return "$sign$rounded°"
}
