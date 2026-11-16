package dev.juanpe.habittracker.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Cómo terminó un día ya cerrado. */
enum class DayStatus {
    /** Se cumplió todo lo que tocaba. */
    COMPLETE,

    /** Faltó algo y la racha global se reinició. */
    MISSED,

    /** Faltó algo, pero un comodín protegió la racha. */
    FROZEN,

    /** No había nada programado: ni suma ni rompe la racha. */
    REST,
}

data class DayRecord(val date: LocalDate, val status: DayStatus)

/**
 * Estado de la racha global de "días completos" y de los comodines.
 * Es inmutable: [dev.juanpe.habittracker.domain.logic.GlobalStreakEngine] devuelve copias nuevas.
 */
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    /** Comodines disponibles. Se empieza con uno de regalo. */
    val freezes: Int = 1,
    /** Días completos seguidos acumulados para ganar el próximo comodín. */
    val freezeProgress: Int = 0,
    val freezesUsed: Int = 0,
    /** Veces que se ha completado un día justo después de romper la racha. */
    val comebacks: Int = 0,
    /** true si el último día evaluado rompió la racha (para detectar el "regreso"). */
    val pendingComeback: Boolean = false,
)

/** Datos del usuario que no pertenecen a ningún hábito concreto. */
data class Profile(
    val streak: StreakState,
    /** Último día ya evaluado y cerrado (normalmente, ayer). */
    val lastClosedDate: LocalDate,
    /** Día de la última celebración de "día completo", para no repetirla. */
    val lastCelebratedDay: LocalDate? = null,
    /** Nivel general más alto ya celebrado, para no repetir la animación. */
    val maxLevelCelebrated: Int = 1,
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderTime: LocalTime = LocalTime.of(20, 0),
    val notificationBannerDismissed: Boolean = false,
)

/** Momentos especiales que la UI celebra a pantalla completa. */
sealed interface Celebration {
    data class DayComplete(val streak: Int, val bonusXp: Int, val freezeEarned: Boolean) : Celebration

    /** [area] es null cuando sube el nivel general. */
    data class LevelUp(val level: Int, val area: Area?) : Celebration

    data class AchievementUnlocked(val achievement: Achievement) : Celebration
}

/** Resultado de registrar progreso en un hábito. */
data class ProgressResult(
    /** true si con esta acción el hábito ha pasado a estar cumplido. */
    val completedNow: Boolean = false,
    val xpGained: Int = 0,
    val celebrations: List<Celebration> = emptyList(),
)
