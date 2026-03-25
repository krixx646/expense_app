package com.chelumore.xpenze.core.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

data class PieSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    chartSize: androidx.compose.ui.unit.Dp = 180.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 22.dp,
    showLegend: Boolean = true,
    centerLabel: String? = null
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val diameter = min(this.size.width, this.size.height)
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val rect = Rect(
                offset = Offset(inset, inset),
                size = Size(diameter - strokePx, diameter - strokePx)
            )

            // Background ring
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (total > 0f) {
                var start = -90f
                slices.filter { it.value > 0f }.forEach { slice ->
                    val sweep = (slice.value / total) * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    start += sweep
                }
            }
        }

        if (centerLabel != null) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (showLegend) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEach { slice ->
                    LegendRow(label = slice.label, color = slice.color)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


