package com.chelumore.xpenze.core.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class LineEntry(
    val label: String,
    val value: Float
)

@Composable
fun LineChart(
    entries: List<LineEntry>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 160.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showLabels: Boolean = true
) {
    val max = entries.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
    val pointFillColor = MaterialTheme.colorScheme.background

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (entries.size < 2) return@Canvas

            val stepX = size.width / (entries.size - 1)
            val path = Path()

            entries.forEachIndexed { index, entry ->
                val x = index * stepX
                val y = size.height - (entry.value / max).coerceIn(0f, 1f) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // Points
            entries.forEachIndexed { index, entry ->
                val x = index * stepX
                val y = size.height - (entry.value / max).coerceIn(0f, 1f) * size.height
                drawCircle(color = pointFillColor, radius = 8f, center = Offset(x, y))
                drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
            }
        }

        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}


