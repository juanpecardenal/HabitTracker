package dev.juanpe.habittracker.ui.celebration

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Celebration
import dev.juanpe.habittracker.ui.achievements.AchievementBadge
import dev.juanpe.habittracker.ui.achievements.color
import dev.juanpe.habittracker.ui.achievements.descriptionRes
import dev.juanpe.habittracker.ui.achievements.titleRes
import dev.juanpe.habittracker.ui.components.Confetti
import dev.juanpe.habittracker.ui.components.Flame
import dev.juanpe.habittracker.ui.components.ProgressRing
import dev.juanpe.habittracker.ui.components.drawSparkle
import dev.juanpe.habittracker.ui.components.rememberAppHaptics
import dev.juanpe.habittracker.ui.theme.AppTheme
import dev.juanpe.habittracker.ui.theme.color
import dev.juanpe.habittracker.ui.theme.labelRes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Fondo oscuro de las celebraciones (igual en tema claro y oscuro). */
private val Backdrop = Color(0xF70E0C18)

/**
 * Muestra a pantalla completa la celebración pendiente, si la hay.
 * Estas animaciones son largas y vistosas a propósito: solo aparecen en
 * momentos especiales (día completo, subir de nivel, logro nuevo).
 */
@Composable
fun CelebrationHost(celebration: Celebration?, onDismiss: (Celebration) -> Unit) {
    Crossfade(targetState = celebration, animationSpec = tween(240), label = "celebration") { current ->
        when (current) {
            null -> Unit
            is Celebration.DayComplete -> DayCompleteCelebration(current) { onDismiss(current) }
            is Celebration.LevelUp -> LevelUpCelebration(current) { onDismiss(current) }
            is Celebration.AchievementUnlocked -> AchievementCelebration(current) { onDismiss(current) }
        }
    }
}

/** Estructura común: fondo que bloquea los toques, contenido centrado y botón para seguir. */
@Composable
private fun CelebrationScaffold(
    buttonText: String,
    buttonColor: Color,
    onDismiss: () -> Unit,
    background: @Composable BoxScope.() -> Unit = {},
    foreground: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Backdrop)
            // Consume los toques para que no lleguen a la pantalla de debajo.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        background()
        Column(
            modifier = Modifier.systemBarsPadding().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp),
            ) {
                Text(buttonText, style = MaterialTheme.typography.titleMedium)
            }
        }
        foreground()
    }
}

/** Animación de entrada compartida: de 0 a 1 con un muelle que rebota. */
@Composable
private fun rememberPopIn(delayMillis: Long = 0): Animatable<Float, *> {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        pop.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow))
    }
    return pop
}

@Composable
private fun CelebrationTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CelebrationBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.82f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RewardPill(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

// --- Día completo -----------------------------------------------------------------

@Composable
private fun DayCompleteCelebration(celebration: Celebration.DayComplete, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val pop = rememberPopIn()
    var ringFull by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ringFull = true
        // Doble vibración: "ta-chán".
        haptics.strong()
        delay(160)
        haptics.success()
    }

    CelebrationScaffold(
        buttonText = stringResource(R.string.celebration_day_button),
        buttonColor = MaterialTheme.colorScheme.primary,
        onDismiss = onDismiss,
        foreground = {
            Confetti(
                colors = listOf(colors.diet, colors.training, colors.study, colors.relations, colors.gold),
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        ProgressRing(
            progress = if (ringFull) 1f else 0f,
            colors = listOf(colors.diet, colors.study, colors.relations, colors.training),
            trackColor = Color.White.copy(alpha = 0.12f),
            strokeWidth = 16.dp,
            modifier = Modifier
                .size(176.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
        ) {
            Flame(streak = celebration.streak, modifier = Modifier.size(92.dp))
        }
        Spacer(Modifier.height(28.dp))
        CelebrationTitle(stringResource(R.string.celebration_day_title))
        Spacer(Modifier.height(8.dp))
        CelebrationBody(
            pluralStringResource(R.plurals.celebration_day_streak, celebration.streak, celebration.streak),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RewardPill(Icons.Rounded.Bolt, stringResource(R.string.xp_gain, celebration.bonusXp), colors.gold)
            if (celebration.freezeEarned) {
                RewardPill(Icons.Rounded.Shield, stringResource(R.string.celebration_freeze_earned), colors.freeze)
            }
        }
    }
}

// --- Subida de nivel ----------------------------------------------------------------

@Composable
private fun LevelUpCelebration(celebration: Celebration.LevelUp, onDismiss: () -> Unit) {
    val accent = celebration.area?.color ?: AppTheme.colors.gold
    val haptics = rememberAppHaptics()
    val pop = rememberPopIn(delayMillis = 120)

    // El número empieza en el nivel anterior y "sube" al nuevo.
    var shownLevel by remember { mutableIntStateOf(celebration.level - 1) }
    LaunchedEffect(Unit) {
        haptics.strong()
        delay(650)
        shownLevel = celebration.level
        haptics.success()
    }

    val transition = rememberInfiniteTransition(label = "levelUp")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing)),
        label = "rays",
    )
    val twinkle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "twinkle",
    )

    CelebrationScaffold(
        buttonText = stringResource(R.string.celebration_level_button),
        buttonColor = accent,
        onDismiss = onDismiss,
        background = {
            // Rayos de luz girando despacio detrás de la insignia.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rayCount = 14
                val length = size.maxDimension
                val rayCenter = Offset(size.width / 2, size.height * 0.36f)
                val halfWidth = (PI / rayCount / 2).toFloat()
                rotate(rotation.value, pivot = rayCenter) {
                    for (i in 0 until rayCount) {
                        val angle = (2 * PI * i / rayCount).toFloat()
                        val ray = Path().apply {
                            moveTo(rayCenter.x, rayCenter.y)
                            lineTo(rayCenter.x + cos(angle - halfWidth) * length, rayCenter.y + sin(angle - halfWidth) * length)
                            lineTo(rayCenter.x + cos(angle + halfWidth) * length, rayCenter.y + sin(angle + halfWidth) * length)
                            close()
                        }
                        drawPath(
                            path = ray,
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.34f), Color.Transparent),
                                center = rayCenter,
                                radius = length * 0.55f,
                            ),
                            alpha = pop.value.coerceIn(0f, 1f),
                        )
                    }
                }
            }
        },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            // Destellos que parpadean alrededor.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val sparkles = 7
                for (i in 0 until sparkles) {
                    val angle = (2 * PI * i / sparkles + 0.4).toFloat()
                    val distance = radius * (0.80f + 0.14f * (i % 2))
                    val phase = if (i % 2 == 0) twinkle.value else 1f - twinkle.value
                    drawSparkle(
                        center = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance),
                        radius = radius * (0.05f + 0.07f * phase) * pop.value.coerceIn(0f, 1f),
                        color = Color.White,
                        alpha = 0.35f + 0.65f * phase,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f))))
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = shownLevel,
                    transitionSpec = {
                        (slideInVertically(spring(dampingRatio = 0.5f)) { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                    },
                    label = "levelNumber",
                ) { level ->
                    Text(
                        text = level.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        CelebrationTitle(stringResource(R.string.celebration_level_title))
        Spacer(Modifier.height(8.dp))
        val area = celebration.area
        CelebrationBody(
            if (area == null) {
                stringResource(R.string.celebration_level_general, celebration.level)
            } else {
                stringResource(R.string.celebration_level_area, stringResource(area.labelRes), celebration.level)
            },
        )
    }
}

// --- Logro desbloqueado ----------------------------------------------------------------

@Composable
private fun AchievementCelebration(celebration: Celebration.AchievementUnlocked, onDismiss: () -> Unit) {
    val achievement = celebration.achievement
    val tierColor = achievement.tier.color
    val haptics = rememberAppHaptics()

    val pop = rememberPopIn()
    val flip = remember { Animatable(0f) } // la medalla entra girando sobre sí misma
    val shine = remember { Animatable(0f) } // brillo que la recorre al asentarse
    val burst = remember { Animatable(0f) } // destellos que salen despedidos

    LaunchedEffect(Unit) {
        haptics.strong()
        flip.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        haptics.success()
        burst.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(850)
        shine.animateTo(1f, tween(750, easing = LinearEasing))
    }

    CelebrationScaffold(
        buttonText = stringResource(R.string.celebration_achievement_button),
        buttonColor = MaterialTheme.colorScheme.primary,
        onDismiss = onDismiss,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = burst.value
                if (progress > 0f && progress < 1f) {
                    val radius = size.minDimension / 2
                    val sparkles = 10
                    for (i in 0 until sparkles) {
                        val angle = (2 * PI * i / sparkles).toFloat()
                        val distance = radius * (0.55f + 0.42f * progress)
                        drawSparkle(
                            center = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance),
                            radius = radius * 0.09f * (1f - progress * 0.5f),
                            color = if (i % 2 == 0) tierColor else Color.White,
                            alpha = 1f - progress,
                        )
                    }
                }
                // Halo suave detrás de la medalla.
                drawCircle(
                    brush = Brush.radialGradient(listOf(tierColor.copy(alpha = 0.35f), Color.Transparent)),
                    radius = size.minDimension / 2,
                    alpha = pop.value.coerceIn(0f, 1f),
                )
            }
            AchievementBadge(
                achievement = achievement,
                unlocked = true,
                size = 148.dp,
                modifier = Modifier
                    .graphicsLayer {
                        val scale = 0.3f + 0.7f * pop.value
                        scaleX = scale
                        scaleY = scale
                        rotationY = (1f - flip.value) * 540f
                        cameraDistance = 14f * density
                    }
                    .clip(CircleShape)
                    // Franja de luz diagonal que cruza la medalla una vez.
                    .drawWithContent {
                        drawContent()
                        val progress = shine.value
                        if (progress > 0f && progress < 1f) {
                            val bandWidth = size.width * 0.35f
                            val x = -bandWidth + (size.width + bandWidth * 2) * progress
                            rotate(20f) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                        startX = x - bandWidth / 2,
                                        endX = x + bandWidth / 2,
                                    ),
                                    topLeft = Offset(x - bandWidth / 2, -size.height),
                                    size = Size(bandWidth, size.height * 3),
                                )
                            }
                        }
                    },
            )
        }
        Text(
            text = stringResource(R.string.celebration_achievement_label).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = tierColor,
        )
        Spacer(Modifier.height(8.dp))
        CelebrationTitle(stringResource(achievement.titleRes))
        Spacer(Modifier.height(8.dp))
        CelebrationBody(stringResource(achievement.descriptionRes))
    }
}
