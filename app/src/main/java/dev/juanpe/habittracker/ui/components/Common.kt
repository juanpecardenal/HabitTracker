package dev.juanpe.habittracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.juanpe.habittracker.ui.theme.AppTheme

/** Tarjeta base de la app: superficie clara, esquinas grandes y un borde muy fino. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * Barra de progreso horizontal que se rellena con un muelle.
 * Se usa para la experiencia, los niveles por área y el progreso de los logros.
 */
@Composable
fun AnimatedBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow),
        label = "bar",
    )
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * animated.coerceIn(0f, 1f)
        if (width > 0f) {
            // Con muy poco progreso se dibuja al menos un punto redondo para que se vea algo.
            drawRoundRect(color, size = Size(width.coerceAtLeast(size.height), size.height), cornerRadius = radius)
        }
    }
}

/** Insignia redonda con el número de nivel. */
@Composable
fun LevelBadge(level: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.72f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.toString(),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.42f).sp),
        )
    }
}

/** Fila de escudos: comodines disponibles (rellenos) sobre el máximo (apagados). */
@Composable
fun FreezeTokens(available: Int, max: Int, modifier: Modifier = Modifier, iconSize: Dp = 22.dp) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { index ->
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = if (index < available) AppTheme.colors.freeze else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

/** Estado vacío con ilustración: título, explicación y una acción opcional. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyIllustration(modifier = Modifier.size(148.dp))
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Ilustración de los estados vacíos: un anillo punteado esperando su primer check. */
@Composable
private fun EmptyIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val gold = AppTheme.colors.gold
    val pink = AppTheme.colors.relations
    val green = AppTheme.colors.diet
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        drawCircle(soft.copy(alpha = 0.55f), radius * 0.92f)
        drawCircle(
            color = primary.copy(alpha = 0.55f),
            radius = radius * 0.58f,
            style = Stroke(
                width = radius * 0.09f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(radius * 0.02f, radius * 0.24f)),
            ),
        )
        val check = Path().apply {
            moveTo(center.x - radius * 0.22f, center.y + radius * 0.02f)
            lineTo(center.x - radius * 0.05f, center.y + radius * 0.18f)
            lineTo(center.x + radius * 0.25f, center.y - radius * 0.16f)
        }
        drawPath(check, primary, style = Stroke(radius * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawSparkle(Offset(center.x + radius * 0.66f, center.y - radius * 0.62f), radius * 0.16f, gold)
        drawSparkle(Offset(center.x - radius * 0.72f, center.y - radius * 0.38f), radius * 0.10f, pink)
        drawSparkle(Offset(center.x + radius * 0.60f, center.y + radius * 0.62f), radius * 0.09f, green)
    }
}

/** Destello de cuatro puntas, usado como adorno en ilustraciones y celebraciones. */
fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color, alpha: Float = 1f) {
    if (radius <= 0f || alpha <= 0f) return
    val waist = radius * 0.22f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + waist, center.y - waist, center.x + radius, center.y)
        quadraticTo(center.x + waist, center.y + waist, center.x, center.y + radius)
        quadraticTo(center.x - waist, center.y + waist, center.x - radius, center.y)
        quadraticTo(center.x - waist, center.y - waist, center.x, center.y - radius)
        close()
    }
    drawPath(path, color, alpha = alpha.coerceIn(0f, 1f))
}
