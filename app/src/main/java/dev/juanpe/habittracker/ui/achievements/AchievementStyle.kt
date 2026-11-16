package dev.juanpe.habittracker.ui.achievements

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Achievement
import dev.juanpe.habittracker.domain.model.AchievementTier
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.ui.theme.AppTheme
import dev.juanpe.habittracker.ui.theme.icon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* Textos, iconos y colores de cada logro (la lógica está en domain/model/Achievement.kt). */

@get:StringRes
val Achievement.titleRes: Int
    get() = when (this) {
        Achievement.FIRST_STEP -> R.string.ach_first_step_title
        Achievement.FIRST_FULL_DAY -> R.string.ach_first_full_day_title
        Achievement.STREAK_7 -> R.string.ach_streak_7_title
        Achievement.STREAK_14 -> R.string.ach_streak_14_title
        Achievement.STREAK_30 -> R.string.ach_streak_30_title
        Achievement.STREAK_100 -> R.string.ach_streak_100_title
        Achievement.HABIT_STREAK_21 -> R.string.ach_habit_streak_21_title
        Achievement.COMEBACK -> R.string.ach_comeback_title
        Achievement.SAFETY_NET -> R.string.ach_safety_net_title
        Achievement.COMPLETIONS_50 -> R.string.ach_completions_50_title
        Achievement.COMPLETIONS_250 -> R.string.ach_completions_250_title
        Achievement.COMPLETIONS_1000 -> R.string.ach_completions_1000_title
        Achievement.DIET_25 -> R.string.ach_diet_25_title
        Achievement.DIET_100 -> R.string.ach_diet_100_title
        Achievement.TRAINING_25 -> R.string.ach_training_25_title
        Achievement.TRAINING_100 -> R.string.ach_training_100_title
        Achievement.STUDY_25 -> R.string.ach_study_25_title
        Achievement.STUDY_100 -> R.string.ach_study_100_title
        Achievement.RELATIONS_25 -> R.string.ach_relations_25_title
        Achievement.RELATIONS_100 -> R.string.ach_relations_100_title
        Achievement.LEVEL_5 -> R.string.ach_level_5_title
        Achievement.LEVEL_10 -> R.string.ach_level_10_title
        Achievement.LEVEL_20 -> R.string.ach_level_20_title
        Achievement.BALANCE -> R.string.ach_balance_title
    }

@get:StringRes
val Achievement.descriptionRes: Int
    get() = when (this) {
        Achievement.FIRST_STEP -> R.string.ach_first_step_desc
        Achievement.FIRST_FULL_DAY -> R.string.ach_first_full_day_desc
        Achievement.STREAK_7 -> R.string.ach_streak_7_desc
        Achievement.STREAK_14 -> R.string.ach_streak_14_desc
        Achievement.STREAK_30 -> R.string.ach_streak_30_desc
        Achievement.STREAK_100 -> R.string.ach_streak_100_desc
        Achievement.HABIT_STREAK_21 -> R.string.ach_habit_streak_21_desc
        Achievement.COMEBACK -> R.string.ach_comeback_desc
        Achievement.SAFETY_NET -> R.string.ach_safety_net_desc
        Achievement.COMPLETIONS_50 -> R.string.ach_completions_50_desc
        Achievement.COMPLETIONS_250 -> R.string.ach_completions_250_desc
        Achievement.COMPLETIONS_1000 -> R.string.ach_completions_1000_desc
        Achievement.DIET_25 -> R.string.ach_diet_25_desc
        Achievement.DIET_100 -> R.string.ach_diet_100_desc
        Achievement.TRAINING_25 -> R.string.ach_training_25_desc
        Achievement.TRAINING_100 -> R.string.ach_training_100_desc
        Achievement.STUDY_25 -> R.string.ach_study_25_desc
        Achievement.STUDY_100 -> R.string.ach_study_100_desc
        Achievement.RELATIONS_25 -> R.string.ach_relations_25_desc
        Achievement.RELATIONS_100 -> R.string.ach_relations_100_desc
        Achievement.LEVEL_5 -> R.string.ach_level_5_desc
        Achievement.LEVEL_10 -> R.string.ach_level_10_desc
        Achievement.LEVEL_20 -> R.string.ach_level_20_desc
        Achievement.BALANCE -> R.string.ach_balance_desc
    }

val Achievement.icon: ImageVector
    get() = when (this) {
        Achievement.FIRST_STEP -> Icons.Rounded.Flag
        Achievement.FIRST_FULL_DAY -> Icons.Rounded.TaskAlt
        Achievement.STREAK_7, Achievement.STREAK_14 -> Icons.Rounded.LocalFireDepartment
        Achievement.STREAK_30 -> Icons.Rounded.Whatshot
        Achievement.STREAK_100 -> Icons.Rounded.AutoAwesome
        Achievement.HABIT_STREAK_21 -> Icons.Rounded.Repeat
        Achievement.COMEBACK -> Icons.Rounded.Replay
        Achievement.SAFETY_NET -> Icons.Rounded.Shield
        Achievement.COMPLETIONS_50 -> Icons.Rounded.Bolt
        Achievement.COMPLETIONS_250 -> Icons.Rounded.Star
        Achievement.COMPLETIONS_1000 -> Icons.Rounded.WorkspacePremium
        Achievement.DIET_25, Achievement.DIET_100 -> Area.DIET.icon
        Achievement.TRAINING_25, Achievement.TRAINING_100 -> Area.TRAINING.icon
        Achievement.STUDY_25, Achievement.STUDY_100 -> Area.STUDY.icon
        Achievement.RELATIONS_25, Achievement.RELATIONS_100 -> Area.RELATIONS.icon
        Achievement.LEVEL_5 -> Icons.AutoMirrored.Rounded.TrendingUp
        Achievement.LEVEL_10 -> Icons.Rounded.MilitaryTech
        Achievement.LEVEL_20 -> Icons.Rounded.EmojiEvents
        Achievement.BALANCE -> Icons.Rounded.Balance
    }

val AchievementTier.color: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        AchievementTier.BRONZE -> AppTheme.colors.bronze
        AchievementTier.SILVER -> AppTheme.colors.silver
        AchievementTier.GOLD -> AppTheme.colors.gold
    }

/**
 * Medalla de un logro: un disco de borde ondulado (dibujado con Canvas) con el
 * icono en el centro. Bloqueada se ve gris y con un candado.
 */
@Composable
fun AchievementBadge(
    achievement: Achievement,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val tierColor = achievement.tier.color
    val lockedColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val baseColor = if (unlocked) tierColor else lockedColor
    val iconTint = if (unlocked) Color.White else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(size)
            // drawWithCache: el trazado ondulado solo se recalcula si cambia el tamaño.
            .drawWithCache {
                val radius = this.size.minDimension / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)
                val scallops = 12
                val steps = 144
                val edge = Path().apply {
                    for (i in 0..steps) {
                        val angle = 2 * PI * i / steps
                        val r = radius * (0.94f + 0.06f * cos(scallops * angle).toFloat())
                        val x = center.x + r * cos(angle).toFloat()
                        val y = center.y + r * sin(angle).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                val fill = Brush.linearGradient(
                    colors = listOf(lerp(baseColor, Color.White, 0.28f), baseColor, lerp(baseColor, Color.Black, 0.12f)),
                    start = Offset.Zero,
                    end = Offset(this.size.width, this.size.height),
                )
                onDrawBehind {
                    drawPath(edge, fill)
                    drawCircle(Color.White.copy(alpha = if (unlocked) 0.35f else 0.25f), radius * 0.70f, style = Stroke(radius * 0.05f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (unlocked) achievement.icon else Icons.Rounded.Lock,
            contentDescription = null,
            modifier = Modifier.size(size * 0.44f),
            tint = iconTint,
        )
    }
}
