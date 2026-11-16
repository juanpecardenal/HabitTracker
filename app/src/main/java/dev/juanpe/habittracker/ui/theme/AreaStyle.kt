package dev.juanpe.habittracker.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Area

/*
 * Aspecto de cada área: nombre, icono y color. Está en la capa de UI para
 * que el dominio no dependa de Compose ni de los recursos de Android.
 */

@get:StringRes
val Area.labelRes: Int
    get() = when (this) {
        Area.DIET -> R.string.area_diet
        Area.TRAINING -> R.string.area_training
        Area.STUDY -> R.string.area_study
        Area.RELATIONS -> R.string.area_relations
    }

val Area.icon: ImageVector
    get() = when (this) {
        Area.DIET -> Icons.Rounded.Restaurant
        Area.TRAINING -> Icons.Rounded.FitnessCenter
        Area.STUDY -> Icons.AutoMirrored.Rounded.MenuBook
        Area.RELATIONS -> Icons.Rounded.Favorite
    }

/** Color vivo del área (cambia ligeramente entre tema claro y oscuro). */
val Area.color: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.forArea(this)

fun AppColors.forArea(area: Area): Color = when (area) {
    Area.DIET -> diet
    Area.TRAINING -> training
    Area.STUDY -> study
    Area.RELATIONS -> relations
}

/** Versión suave de un color, para fondos: lo mezcla con el color de la superficie. */
@Composable
@ReadOnlyComposable
fun Color.soft(alpha: Float = 0.14f): Color =
    copy(alpha = alpha).compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
