package dev.juanpe.habittracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Un trozo de confeti. Posiciones y velocidades van en "alturas de pantalla"
 * (1.0 = el alto del lienzo), así la animación se ve igual en cualquier móvil.
 */
private class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val delay: Float,
    val size: Float,
    val color: Color,
    val isCircle: Boolean,
    val spin: Float,
    val spinSpeed: Float,
    val flipSpeed: Float,
    val wobble: Float,
)

private const val GRAVITY = 1.15f // alturas de pantalla por segundo²
private const val DURATION_SECONDS = 4.2f

private fun createPieces(count: Int, colors: List<Color>, random: Random): List<ConfettiPiece> {
    fun piece(startX: Float, startY: Float, angleDegrees: Float, speed: Float, delay: Float): ConfettiPiece {
        val angle = angleDegrees * PI.toFloat() / 180f
        return ConfettiPiece(
            startX = startX,
            startY = startY,
            velocityX = cos(angle) * speed,
            velocityY = -sin(angle) * speed,
            delay = delay,
            size = 0.008f + random.nextFloat() * 0.010f,
            color = colors[random.nextInt(colors.size)],
            isCircle = random.nextInt(4) == 0,
            spin = random.nextFloat() * 360f,
            spinSpeed = (random.nextFloat() - 0.5f) * 720f,
            flipSpeed = 4f + random.nextFloat() * 8f,
            wobble = random.nextFloat() * 2f * PI.toFloat(),
        )
    }

    val cannon = count * 2 / 5
    return buildList {
        // Dos "cañones" en las esquinas inferiores que disparan hacia arriba y al centro.
        repeat(cannon) { add(piece(-0.02f, 0.80f, 62f + random.nextFloat() * 26f, 0.9f + random.nextFloat() * 0.9f, random.nextFloat() * 0.15f)) }
        repeat(cannon) { add(piece(Float.NaN, 0.80f, 92f + random.nextFloat() * 26f, 0.9f + random.nextFloat() * 0.9f, random.nextFloat() * 0.15f)) }
        // Y una lluvia que cae desde arriba un momento después.
        repeat(count - cannon * 2) {
            add(piece(random.nextFloat(), -0.05f, 250f + random.nextFloat() * 40f, 0.05f + random.nextFloat() * 0.25f, 0.3f + random.nextFloat() * 1.2f))
        }
    }
}

/**
 * Confeti a pantalla completa con un sistema de partículas propio.
 *
 * Rendimiento: las partículas se crean una sola vez y el tiempo se guarda en un
 * estado que solo se lee dentro del bloque de dibujo. Así cada fotograma
 * repinta el Canvas pero no recompone nada.
 */
@Composable
fun Confetti(colors: List<Color>, modifier: Modifier = Modifier, pieceCount: Int = 170) {
    val pieces = remember { createPieces(pieceCount, colors, Random(System.nanoTime())) }
    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (time.floatValue < DURATION_SECONDS) {
            withFrameNanos { now -> time.floatValue = (now - start) / 1_000_000_000f }
        }
    }

    Canvas(modifier = modifier) {
        val now = time.floatValue
        if (now >= DURATION_SECONDS) return@Canvas
        val unit = size.height
        // Todo el confeti se desvanece en el último tramo.
        val globalAlpha = ((DURATION_SECONDS - now) / 0.8f).coerceIn(0f, 1f)

        for (piece in pieces) {
            val t = now - piece.delay
            if (t < 0f) continue

            // Los cañones de la derecha se marcan con NaN porque dependen del ancho real.
            val originX = if (piece.startX.isNaN()) size.width + 0.02f * unit else piece.startX * size.width
            // El rozamiento del aire frena la velocidad horizontal con el tiempo.
            val drag = 1f / (1f + 0.9f * t)
            val x = originX + piece.velocityX * unit * t * drag + sin(piece.wobble + t * 5f) * 0.008f * unit
            val y = (piece.startY + piece.velocityY * t * (0.55f + 0.45f * drag) + 0.5f * GRAVITY * t * t * 0.55f) * unit
            if (y > size.height + 40f) continue

            val pieceSize = piece.size * unit
            // Al "voltearse" en 3D el papelito se ve más o menos alto.
            val flip = abs(cos(piece.wobble + t * piece.flipSpeed))
            rotate(degrees = piece.spin + piece.spinSpeed * t, pivot = Offset(x, y)) {
                if (piece.isCircle) {
                    drawCircle(piece.color, pieceSize * 0.5f, Offset(x, y), alpha = globalAlpha)
                } else {
                    drawRoundRect(
                        color = piece.color,
                        topLeft = Offset(x - pieceSize / 2, y - pieceSize * 0.7f * flip / 2),
                        size = Size(pieceSize, pieceSize * 0.7f * flip + 1f),
                        cornerRadius = CornerRadius(pieceSize * 0.15f),
                        alpha = globalAlpha,
                    )
                }
            }
        }
    }
}
