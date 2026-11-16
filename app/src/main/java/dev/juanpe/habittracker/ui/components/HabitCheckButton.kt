package dev.juanpe.habittracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * El botón de marcar un hábito: la pieza más importante de la app.
 *
 * Al completarse encadena tres animaciones cortas (en total menos de medio segundo,
 * para no estorbar en una acción que se repite muchas veces al día):
 *  1. el círculo se rellena de color con un muelle que rebota,
 *  2. el check se dibuja trazo a trazo,
 *  3. un anillo y unas chispas salen despedidos hacia fuera.
 *
 * En los hábitos con cantidad (3 de 8 vasos), un arco muestra el avance parcial.
 *
 * @param fraction avance 0..1 hacia el objetivo.
 * @param stateLabel texto para lectores de pantalla ("Hecho", "3 de 8"...).
 */
@Composable
fun HabitCheckButton(
    done: Boolean,
    fraction: Float,
    color: Color,
    stateLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 46.dp,
) {
    // 1. Relleno con rebote al marcar; al desmarcar se encoge rápido y sin florituras.
    val fill by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = if (done) {
            spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)
        } else {
            tween(140)
        },
        label = "checkFill",
    )
    // 2. Trazo del check, un pelín después de que empiece el relleno.
    val checkStroke by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = if (done) tween(240, delayMillis = 80, easing = FastOutSlowInEasing) else tween(90),
        label = "checkStroke",
    )
    // 3. Estallido: solo cuando pasa de no hecho a hecho mientras está en pantalla
    //    (no al abrir la app con hábitos ya marcados).
    val burst = remember { Animatable(1f) }
    var wasDone by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !wasDone) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
        }
        wasDone = done
    }

    val partial by animateFloatAsState(
        targetValue = if (done) 0f else fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "checkPartial",
    )

    // El botón se hunde un poco mientras se pulsa.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "checkPress",
    )

    val checkPath = remember { Path() }
    val checkSegment = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    Box(
        modifier = modifier
            .size(size)
            .semantics { stateDescription = stateLabel }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // la respuesta visual es la propia animación
                role = Role.Checkbox,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
        ) {
            val radius = this.size.minDimension / 2 * 0.82f
            val ringStroke = 2.5.dp.toPx()

            drawBurst(burst.value, radius, color)

            // Aro vacío con un fondo muy suave del color del área.
            drawCircle(color.copy(alpha = 0.10f), radius)
            drawCircle(color.copy(alpha = 0.55f), radius - ringStroke / 2, style = Stroke(ringStroke))

            // Avance parcial de los hábitos con cantidad.
            if (partial > 0.001f) {
                val arcRadius = radius - ringStroke / 2
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * partial,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(ringStroke * 1.7f, cap = StrokeCap.Round),
                )
            }

            // Relleno (el muelle hace que `fill` pase de 1 y vuelva: ese es el rebote).
            if (fill > 0.001f) {
                drawCircle(color, radius * fill)
            }

            // Check dibujado progresivamente recortando el trazado con PathMeasure.
            if (checkStroke > 0.001f) {
                val box = radius * 2
                val left = center.x - radius
                val top = center.y - radius
                checkPath.reset()
                checkPath.moveTo(left + box * 0.29f, top + box * 0.53f)
                checkPath.lineTo(left + box * 0.44f, top + box * 0.67f)
                checkPath.lineTo(left + box * 0.72f, top + box * 0.36f)
                pathMeasure.setPath(checkPath, false)
                checkSegment.reset()
                pathMeasure.getSegment(0f, pathMeasure.length * checkStroke, checkSegment, true)
                drawPath(
                    path = checkSegment,
                    color = Color.White,
                    style = Stroke(radius * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/** Anillo que se expande y ocho chispas que salen en círculo, desvaneciéndose. */
private fun DrawScope.drawBurst(progress: Float, radius: Float, color: Color) {
    if (progress <= 0f || progress >= 1f) return
    val fade = 1f - progress

    drawCircle(
        color = color.copy(alpha = 0.55f * fade),
        radius = radius * (1f + 0.55f * progress),
        style = Stroke(3.dp.toPx() * fade),
    )

    val sparks = 8
    val distance = radius * (1.05f + 0.75f * progress)
    for (i in 0 until sparks) {
        val angle = (2 * PI * i / sparks + PI / 8).toFloat()
        val position = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance)
        // Chispas alternas más claras para dar algo de variedad.
        val sparkColor = if (i % 2 == 0) color else color.copy(alpha = 0.6f)
        drawCircle(sparkColor.copy(alpha = sparkColor.alpha * fade), 2.6.dp.toPx() * (0.4f + 0.6f * fade), position)
    }
}
