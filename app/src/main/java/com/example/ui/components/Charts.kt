package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MonthlyDataPoint
import kotlin.math.cos
import kotlin.math.sin

data class ChartSlice(
    val label: String,
    val value: Double,
    val percentage: Double,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutPieChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Total",
    centerValue: String = "",
    chartSize: Dp = 180.dp,
    strokeWidth: Float = 36f,
    showLegend: Boolean = true
) {
    if (slices.isEmpty() || slices.all { it.value <= 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartSize)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No distribution data available yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = slices.sumOf { it.value }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            val emptyColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val diameter = size.minDimension
                val radius = diameter / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background track
                drawCircle(
                    color = emptyColor,
                    radius = radius - strokeWidth / 2f,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                var startAngle = -90f
                val effectiveSlices = slices.filter { it.value > 0.0 }

                for (slice in effectiveSlices) {
                    val sweepAngle = ((slice.value / total) * 360f).toFloat() * animationProgress.value
                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius + strokeWidth / 2f, center.y - radius + strokeWidth / 2f),
                            size = Size((radius - strokeWidth / 2f) * 2f, (radius - strokeWidth / 2f) * 2f),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            // Center text inside Donut
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = centerTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (centerValue.isNotEmpty()) {
                    Text(
                        text = centerValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showLegend) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.filter { it.value > 0.0 }.take(8).forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Text(
                            text = "${slice.label} (${String.format(java.util.Locale.US, "%.1f%%", slice.percentage)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    dataPoints: List<MonthlyDataPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightMax: Boolean = true
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No monthly trend data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = dataPoints.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val animProgress = remember { Animatable(0f) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(dataPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant
    val maxBarHighlight = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        selectedPointIndex?.let { idx ->
            if (idx in dataPoints.indices) {
                val pt = dataPoints[idx]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${pt.label} (${pt.yearMonth})",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVar
                    )
                    Text(
                        text = Formatters.formatCurrency(pt.amount, currencySymbol),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val barWidthWithSpacing = size.width / dataPoints.size
                        val tappedIdx = (offset.x / barWidthWithSpacing).toInt().coerceIn(0, dataPoints.size - 1)
                        selectedPointIndex = tappedIdx
                    }
                }
        ) {
            val width = size.width
            val chartHeight = size.height - 30f // room for x-axis labels
            val count = dataPoints.size
            val barSpacing = width / count
            val barWidth = (barSpacing * 0.55f).coerceAtMost(36.dp.toPx())

            // Draw horizontal reference grid lines
            val steps = 3
            for (s in 0..steps) {
                val y = chartHeight * (s.toFloat() / steps)
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw Bars
            dataPoints.forEachIndexed { i, pt ->
                val barFraction = ((pt.amount / maxVal).toFloat() * animProgress.value).coerceIn(0f, 1f)
                val currentBarHeight = (chartHeight * barFraction).coerceAtLeast(4f)
                val xCenter = (i * barSpacing) + (barSpacing / 2f)
                val left = xCenter - (barWidth / 2f)
                val top = chartHeight - currentBarHeight

                val isSelected = selectedPointIndex == i
                val isMax = highlightMax && pt.amount == maxVal && pt.amount > 0

                val colorToUse = when {
                    isSelected -> Color(0xFF38BDF8)
                    isMax -> maxBarHighlight
                    else -> barColor
                }

                // Background track
                drawRoundRect(
                    color = surfaceVariant.copy(alpha = 0.5f),
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Filled Bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(colorToUse, colorToUse.copy(alpha = 0.75f)),
                        startY = top,
                        endY = chartHeight
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, currentBarHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dataPoints.forEachIndexed { i, pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedPointIndex == i) MaterialTheme.colorScheme.primary else onSurfaceVar,
                    fontWeight = if (selectedPointIndex == i) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SmoothAreaTrendChart(
    dataPoints: List<MonthlyDataPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    lineColor: Color = Color(0xFF10B981),
    fillGradientColors: List<Color> = listOf(
        Color(0xFF10B981).copy(alpha = 0.4f),
        Color(0xFF10B981).copy(alpha = 0.02f)
    )
) {
    if (dataPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add more transactions to view smooth trend",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val anim = remember { Animatable(0f) }
    var selectedIdx by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(dataPoints) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val minVal = dataPoints.minOf { it.amount }
    val maxVal = dataPoints.maxOf { it.amount }
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        selectedIdx?.let { idx ->
            if (idx in dataPoints.indices) {
                val pt = dataPoints[idx]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${pt.label} (${pt.yearMonth})",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVar
                    )
                    Text(
                        text = Formatters.formatCurrency(pt.amount, currencySymbol),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (dataPoints.size - 1)
                        val tappedIdx = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                        selectedIdx = tappedIdx
                    }
                }
        ) {
            val width = size.width
            val chartHeight = size.height - 30f
            val count = dataPoints.size
            val stepX = width / (count - 1)

            // Grid lines
            val steps = 3
            for (s in 0..steps) {
                val y = chartHeight * (s.toFloat() / steps)
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            val points = dataPoints.mapIndexed { index, dataPoint ->
                val x = index * stepX
                val normalizedVal = ((dataPoint.amount - minVal) / range).toFloat() * anim.value
                val y = chartHeight - (normalizedVal * chartHeight)
                Offset(x, y.coerceIn(4f, chartHeight))
            }

            // Draw Area Path
            val areaPath = Path().apply {
                moveTo(points.first().x, chartHeight)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
                lineTo(points.last().x, chartHeight)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = fillGradientColors,
                    startY = 0f,
                    endY = chartHeight
                ),
                style = Fill
            )

            // Draw Line Path
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Points
            points.forEachIndexed { i, pt ->
                val isSelected = selectedIdx == i
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (isSelected) Color(0xFF0284C7) else lineColor,
                    radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                    center = pt
                )
            }
        }

        // X Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dataPoints.forEachIndexed { i, pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedIdx == i) lineColor else onSurfaceVar,
                    fontWeight = if (selectedIdx == i) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
