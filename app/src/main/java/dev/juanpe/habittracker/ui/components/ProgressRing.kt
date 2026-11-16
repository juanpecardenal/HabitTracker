package dev.juanpe.habittracker.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val NoPulse: State<Float> = mutableFloatStateOf(0f)

/**
 * Anillo de progreso dibujado con Canvas. El arco se rellena con un muelle
 * (spring), así que al avanzar "empuja" con un pequeño rebote en vez de saltar.
 * Cuando llega al 100 % late suavemente.
 *
 * @param progress fracción 0..1.
 * @param colors colores del degradado del arco.
 * @param content lo que se pinta en el centro del anillo.
 */
@Composable
fun ProgressRing(
    progress: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessVeryLow),
        label = "ringProgress",
    )
    val isFull = progress >= 1f
    // El latido solo existe (y solo consume) cuando el anillo está completo.
    val pulseState: State<Float> = if (isFull) {
        val transition = rememberInfiniteTransition(label = "ringPulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
            label = "ringPulseValue",
        )
    } else {
        NoPulse
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val pulse = pulseState.value // leído al dibujar: repinta sin recomponer
            val stroke = strokeWidth.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke),
            )

            val sweep = 360f * animatedProgress.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                // Se repite el primer color al final para que el degradado no tenga costura.
                val brush = Brush.sweepGradient(colors + colors.first(), center)
                // Girado -90° para que el arco empiece arriba.
                rotate(-90f) {
                    if (isFull) {
                        drawArc(
                            brush = brush,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            alpha = 0.10f + 0.18f * pulse,
                            style = Stroke(stroke * (1.5f + 0.5f * pulse)),
                        )
                    }
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}
