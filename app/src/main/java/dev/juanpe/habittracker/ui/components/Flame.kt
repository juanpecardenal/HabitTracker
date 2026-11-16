package dev.juanpe.habittracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import dev.juanpe.habittracker.ui.theme.AppTheme

/** Número de tamaños de llama (además de "apagada"). */
const val MAX_FLAME_TIER = 5

/**
 * La llama crece por escalones según los días de racha:
 * 0 = apagada, 1-2, 3-6, 7-13, 14-29 y 30 o más.
 */
fun flameTier(streak: Int): Int = when {
    streak <= 0 -> 0
    streak < 3 -> 1
    streak < 7 -> 2
    streak < 14 -> 3
    streak < 30 -> 4
    else -> MAX_FLAME_TIER
}

/**
 * Llama de racha dibujada con Canvas: tres capas (exterior, media y núcleo)
 * con la misma silueta a distinto tamaño. Con más racha la llama es más grande,
 * se mueve más y, en los niveles altos, brilla y suelta chispas.
 *
 * @param animated false para dibujarla quieta (por ejemplo en listas largas).
 */
@Composable
fun Flame(streak: Int, modifier: Modifier = Modifier, animated: Boolean = true) {
    val colors = AppTheme.colors
    val unlitColor = MaterialTheme.colorScheme.outline
    val tier = flameTier(streak)

    // El cambio de tamaño entre escalones también se anima.
    val growth by animateFloatAsState(
        targetValue = if (tier == 0) 0.7f else 0.62f + 0.076f * tier,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
        label = "flameGrowth",
    )

    // Los valores animados se guardan como State y se leen dentro del Canvas:
    // así cada fotograma solo repinta, sin recomponer.
    val motion = if (animated && tier > 0) rememberFlameMotion() else StillFlame

    val path = remember { Path() }

    Canvas(modifier = modifier) {
        val flicker = motion.flicker.value
        val sway = motion.sway.value
        val rise = motion.rise.value
        val flameWidth = size.width * growth
        val flameHeight = size.height * growth
        // Cuanta más racha, más se agita.
        val energy = 0.4f + 0.12f * tier

        // Resplandor de fondo en las rachas largas.
        if (tier >= 4) {
            val glowCenter = Offset(size.width / 2, size.height - flameHeight * 0.45f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.flameMiddle.copy(alpha = 0.30f + 0.10f * flicker), Color.Transparent),
                    center = glowCenter,
                    radius = size.minDimension * 0.62f,
                ),
                radius = size.minDimension * 0.62f,
                center = glowCenter,
            )
        }

        // La llama se apoya en el centro de la base del lienzo.
        translate(left = (size.width - flameWidth) / 2, top = size.height - flameHeight) {
            val base = Offset(flameWidth / 2, flameHeight)
            if (tier == 0) {
                drawFlameLayer(path, flameWidth, flameHeight, 0f, unlitColor.copy(alpha = 0.45f))
            } else {
                scale(1f, 1f + 0.05f * energy * (flicker - 0.5f) * 2f, pivot = base) {
                    drawFlameLayer(path, flameWidth, flameHeight, sway * energy, colors.flameOuter)
                }
                scale(0.64f, 0.64f + 0.07f * energy * (0.5f - flicker) * 2f, pivot = base) {
                    drawFlameLayer(path, flameWidth, flameHeight, -sway * energy, colors.flameMiddle)
                }
                scale(0.34f, 0.36f + 0.05f * flicker, pivot = base) {
                    drawFlameLayer(path, flameWidth, flameHeight, sway * energy * 0.5f, colors.flameCore)
                }
            }

            // Chispas que suben, solo en el escalón más alto.
            if (tier == MAX_FLAME_TIER && animated) {
                for (i in 0 until 3) {
                    val phase = (rise + i / 3f) % 1f
                    val x = flameWidth * (0.25f + 0.25f * i) + sway * flameWidth * 0.04f
                    val y = flameHeight * (0.35f - 0.5f * phase)
                    drawCircle(colors.flameMiddle.copy(alpha = 1f - phase), flameWidth * 0.035f, Offset(x, y))
                }
            }
        }
    }
}

/** Los tres movimientos de la llama: parpadeo, vaivén de la punta y subida de chispas. */
private class FlameMotion(val flicker: State<Float>, val sway: State<Float>, val rise: State<Float>)

private val StillFlame = FlameMotion(mutableFloatStateOf(0.5f), mutableFloatStateOf(0f), mutableFloatStateOf(0f))

@Composable
private fun rememberFlameMotion(): FlameMotion {
    val transition = rememberInfiniteTransition(label = "flame")
    val flicker = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flicker",
    )
    val sway = transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val rise = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "rise",
    )
    return remember(transition) { FlameMotion(flicker, sway, rise) }
}

/**
 * Silueta de la llama: una gota con la punta arriba. [tipSway] (de -1 a 1)
 * desplaza la punta a los lados para que parezca que el aire la mueve.
 */
private fun DrawScope.drawFlameLayer(path: Path, w: Float, h: Float, tipSway: Float, color: Color) {
    val tipX = w * (0.5f + 0.07f * tipSway)
    path.reset()
    path.moveTo(tipX, 0f)
    path.cubicTo(w * 0.62f, h * 0.28f, w * 0.96f, h * 0.44f, w * 0.89f, h * 0.71f)
    path.cubicTo(w * 0.83f, h * 0.92f, w * 0.67f, h, w * 0.5f, h)
    path.cubicTo(w * 0.33f, h, w * 0.17f, h * 0.92f, w * 0.11f, h * 0.71f)
    path.cubicTo(w * 0.04f, h * 0.44f, w * 0.38f, h * 0.28f, tipX, 0f)
    path.close()
    drawPath(path, color)
}
