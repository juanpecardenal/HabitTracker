package dev.juanpe.habittracker.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.ui.components.AppLocale
import java.time.LocalDate
import java.time.format.TextStyle

/*
 * Gráficos del detalle de un hábito, dibujados a mano con Canvas.
 * Ambos reciben los datos ya calculados por el ViewModel: aquí solo se pinta.
 */

/**
 * Mapa de calor tipo calendario: cada columna es una semana (la actual a la
 * derecha) y cada fila un día, de lunes a domingo. Cuanto más intenso el color,
 * más cerca se estuvo del objetivo ese día.
 *
 * Al aparecer, las columnas se revelan en una ola de izquierda a derecha.
 */
@Composable
fun Heatmap(
    cells: List<HeatCell>,
    today: LocalDate,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val weeks = cells.size / 7
    if (weeks == 0) return

    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val todayColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val dayInitials = stringArrayResource(R.array.weekday_initials)

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = LinearEasing)) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val labelColumn = 18.dp
        val labelRow = 18.dp
        val gap = 3.dp
        // El tamaño de celda sale del ancho disponible; el alto se deduce de él.
        val cell = (maxWidth - labelColumn - gap * (weeks - 1)) / weeks
        val totalHeight = labelRow + cell * 7 + gap * 6

        Canvas(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
            val cellPx = cell.toPx()
            val gapPx = gap.toPx()
            val left = labelColumn.toPx()
            val top = labelRow.toPx()
            val corner = CornerRadius(cellPx * 0.28f)

            // Iniciales de lunes, miércoles y viernes a la izquierda.
            for (row in listOf(0, 2, 4)) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = dayInitials[row],
                    style = labelStyle,
                    topLeft = Offset(0f, top + row * (cellPx + gapPx) + cellPx * 0.1f),
                )
            }

            var lastLabelColumn = -4
            for (column in 0 until weeks) {
                val x = left + column * (cellPx + gapPx)

                // Nombre del mes sobre la primera semana que lo contiene (sin amontonarlos).
                val monday = cells[column * 7].date
                val startsMonth = (0..6).any { monday.plusDays(it.toLong()).dayOfMonth == 1 }
                if ((startsMonth || column == 0) && column - lastLabelColumn >= 4 && column < weeks - 1) {
                    val month = monday.plusDays(6).month.getDisplayName(TextStyle.SHORT, AppLocale).removeSuffix(".")
                    drawText(textMeasurer, month, Offset(x, 0f), labelStyle)
                    lastLabelColumn = column
                }

                // Ola de aparición: cada columna entra un poco después que la anterior.
                val columnAlpha = ((reveal.value * (weeks + 5) - column) / 5f).coerceIn(0f, 1f)
                if (columnAlpha == 0f) continue

                for (row in 0 until 7) {
                    val heatCell = cells[column * 7 + row]
                    val topLeft = Offset(x, top + row * (cellPx + gapPx))
                    val cellSize = Size(cellPx, cellPx)
                    val cellColor = when (heatCell.kind) {
                        // Los días fuera del periodo se dibujan muy tenues para que se vea la cuadrícula.
                        HeatKind.OUTSIDE -> emptyColor.copy(alpha = 0.32f)
                        HeatKind.OFF -> emptyColor.copy(alpha = 0.6f)
                        HeatKind.EMPTY -> emptyColor
                        HeatKind.PARTIAL -> color.copy(alpha = 0.25f + 0.35f * heatCell.fraction)
                        HeatKind.DONE -> color
                    }
                    drawRoundRect(cellColor, topLeft, cellSize, corner, alpha = cellColor.alpha * columnAlpha)
                    if (heatCell.date == today) {
                        drawRoundRect(todayColor, topLeft, cellSize, corner, style = Stroke(1.5.dp.toPx()), alpha = columnAlpha)
                    }
                }
            }
        }
    }
}

/**
 * Gráfico de barras con el porcentaje de cumplimiento de cada semana.
 * Las barras crecen desde abajo al aparecer; la semana actual va resaltada.
 */
@Composable
fun WeeklyChart(weeks: List<WeekBar>, color: Color, modifier: Modifier = Modifier) {
    if (weeks.isEmpty()) return

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
    val textMeasurer = rememberTextMeasurer()

    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }

    Canvas(modifier = modifier.fillMaxWidth().height(170.dp)) {
        val labelHeight = 20.dp.toPx()
        val valueHeight = 18.dp.toPx()
        val chartTop = valueHeight
        val chartHeight = size.height - labelHeight - valueHeight
        val slot = size.width / weeks.size
        val barWidth = slot * 0.52f
        val corner = CornerRadius(barWidth * 0.35f)

        weeks.forEachIndexed { index, week ->
            val barLeft = slot * index + (slot - barWidth) / 2

            // Carril de fondo: marca hasta dónde llegaría el 100 %.
            drawRoundRect(trackColor, Offset(barLeft, chartTop), Size(barWidth, chartHeight), corner)

            val ratio = week.ratio
            if (ratio != null && ratio > 0f) {
                // Altura mínima para que un porcentaje pequeño siga viéndose como barra.
                val barHeight = (chartHeight * ratio * grow.value).coerceAtLeast(barWidth * 0.5f * grow.value)
                drawRoundRect(
                    color = if (week.isCurrent) color else color.copy(alpha = 0.55f),
                    topLeft = Offset(barLeft, chartTop + chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner,
                )
            }

            // Porcentaje encima y fecha del lunes debajo, centrados en la barra.
            val valueText = if (ratio == null) "–" else "${(ratio * 100).toInt()}%"
            val measuredValue = textMeasurer.measure(valueText, valueStyle)
            drawText(measuredValue, topLeft = Offset(barLeft + (barWidth - measuredValue.size.width) / 2, 0f))

            // Etiqueta compacta "9/11" (día/mes del lunes) para que quepan las ocho.
            val weekLabel = "${week.weekStart.dayOfMonth}/${week.weekStart.monthValue}"
            val measuredLabel = textMeasurer.measure(weekLabel, labelStyle)
            drawText(
                measuredLabel,
                topLeft = Offset(
                    x = slot * index + (slot - measuredLabel.size.width) / 2,
                    y = size.height - measuredLabel.size.height,
                ),
            )
        }
    }
}
