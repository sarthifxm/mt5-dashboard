package com.jetro.mt5dashboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jetro.mt5dashboard.api.EquityPoint
import com.jetro.mt5dashboard.ui.theme.*

/**
 * Pure-Compose equity growth line chart. No external chart library needed.
 */
@Composable
fun EquityLineChart(
    points: List<EquityPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = AccentCyan,
    fillColor: Color = AccentCyan.copy(alpha = 0.12f)
) {
    if (points.size < 2) {
        Box(modifier = modifier.background(ObsidianCard.copy(alpha = 0.3f)))
        return
    }

    val values = points.map { it.balance }
    val minVal = values.min()
    val maxVal = values.max()
    val range = if (maxVal == minVal) 1.0 else maxVal - minVal

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 12f

        fun xOf(i: Int) = padding + (i.toFloat() / (values.size - 1)) * (w - padding * 2)
        fun yOf(v: Double) = h - padding - ((v - minVal) / range * (h - padding * 2)).toFloat()

        // Build line path
        val linePath = Path().apply {
            values.forEachIndexed { i, v ->
                val x = xOf(i)
                val y = yOf(v)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // Build fill path (close under the line)
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(xOf(values.lastIndex), h)
            lineTo(xOf(0), h)
            close()
        }

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // Draw glow shadow (thicker, semi-transparent)
        drawPath(
            path = linePath,
            color = lineColor.copy(alpha = 0.25f),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw main line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw last point dot
        val lastX = xOf(values.lastIndex)
        val lastY = yOf(values.last())
        drawCircle(color = lineColor, radius = 5f, center = Offset(lastX, lastY))
        drawCircle(color = ObsidianBg, radius = 2.5f, center = Offset(lastX, lastY))
    }
}

/**
 * Simple bar chart for daily P&L.
 */
@Composable
fun DailyBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        Box(modifier = modifier.background(ObsidianCard.copy(alpha = 0.3f)))
        return
    }

    val maxAbs = values.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0 } ?: 1.0

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = values.size
        val barW = (w / n) * 0.6f
        val spacing = w / n
        val midY = h / 2f

        values.forEachIndexed { i, v ->
            val cx = i * spacing + spacing / 2f
            val barH = (kotlin.math.abs(v) / maxAbs * (h / 2f - 8f)).toFloat()
            val color = if (v >= 0) AccentEmerald else LossRed
            val top = if (v >= 0) midY - barH else midY
            val bottom = if (v >= 0) midY else midY + barH

            drawRoundRect(
                color = color.copy(alpha = 0.8f),
                topLeft = Offset(cx - barW / 2, top),
                size = androidx.compose.ui.geometry.Size(barW, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
            )
        }

        // Center axis line
        drawLine(
            color = ObsidianDivider,
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1f
        )
    }
}
