package dev.juanpe.habittracker.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Vibraciones de la app con nombre propio. Los tipos más expresivos solo
 * existen en versiones recientes de Android, así que en las antiguas se usa
 * el más parecido disponible.
 *
 * Nota: respetan el ajuste del sistema; si el usuario tiene desactivada la
 * respuesta táctil, no vibran.
 */
class AppHaptics(private val haptics: HapticFeedback) {

    /** Hábito completado. */
    fun success() = haptics.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackType.Confirm else HapticFeedbackType.LongPress,
    )

    /** Un paso más en un hábito con cantidad (un vaso de agua más). */
    fun tick() = haptics.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackType.SegmentTick
        } else {
            HapticFeedbackType.TextHandleMove
        },
    )

    /** Desmarcar: un toque discreto. */
    fun undo() = haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)

    /** Golpe fuerte para las celebraciones y las pulsaciones largas. */
    fun strong() = haptics.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { AppHaptics(haptics) }
}
