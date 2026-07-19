package com.appdoctor.compose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A labelled value used by [MiniBarChart]. */
internal data class LabeledValue(val label: String, val value: Float, val valueText: String)

/**
 * A minimal, allocation-light line chart (no third-party charting library). Renders [values]
 * as a single polyline scaled to the canvas; safe for frequent (≈1 Hz) updates.
 */
@Composable
internal fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        if (values.size < 2) return@Canvas
        val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val strokePx = 2.dp.toPx()
        val usableHeight = size.height - strokePx
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = usableHeight - (value / maxValue) * usableHeight + strokePx / 2f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = strokePx))
    }
}

/**
 * A compact horizontal bar chart for "top N" style rankings. Each bar's width is proportional
 * to its value relative to the largest. Built from plain layout — no Canvas needed.
 */
@Composable
internal fun MiniBarChart(
    bars: List<LabeledValue>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (bars.isEmpty()) return
    val max = bars.maxOf { it.value }.coerceAtLeast(1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bars.forEach { bar ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = bar.valueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp),
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((bar.value / max).coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(barColor, RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

/** Draws a faint baseline used by the frame timeline; kept trivial and lightweight. */
@Composable
internal fun FrameTimeline(
    dropRates: List<Float>,
    modifier: Modifier = Modifier,
    goodColor: Color = MaterialTheme.colorScheme.primary,
    badColor: Color = Color(0xFFF85149),
) {
    Canvas(modifier = modifier.fillMaxWidth().height(36.dp)) {
        if (dropRates.isEmpty()) return@Canvas
        val barWidth = size.width / dropRates.size
        dropRates.forEachIndexed { index, rate ->
            val clamped = rate.coerceIn(0f, 1f)
            val barHeight = (clamped * size.height).coerceAtLeast(1f)
            val color = lerpColor(goodColor, badColor, clamped)
            drawRect(
                color = color,
                topLeft = Offset(index * barWidth, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight),
            )
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = from.alpha + (to.alpha - from.alpha) * f,
    )
}
