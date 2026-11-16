package dev.juanpe.habittracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Cada cuánto hay que cumplir un hábito. */
sealed interface Frequency {
    /** Todos los días. */
    data object Daily : Frequency

    /** Solo los días de la semana indicados (por ejemplo lunes, miércoles y viernes). */
    data class SpecificDays(val days: Set<DayOfWeek>) : Frequency

    /** Un número de veces por semana, los días que el usuario prefiera. */
    data class TimesPerWeek(val times: Int) : Frequency
}

/** Unidad de la cantidad objetivo opcional de un hábito. */
enum class TargetUnit { MINUTES, REPS, GLASSES, PAGES, TIMES }

/** Cantidad objetivo opcional: "45 minutos", "8 vasos"... */
data class Target(val amount: Int, val unit: TargetUnit)

data class Habit(
    val id: Long = 0,
    val name: String,
    val area: Area,
    val frequency: Frequency = Frequency.Daily,
    val target: Target? = null,
    /** Hora del recordatorio, o null si no tiene. */
    val reminder: LocalTime? = null,
    val createdOn: LocalDate,
    /** Día en que se archivó. Desde ese día (incluido) deja de contar. */
    val archivedOn: LocalDate? = null,
) {
    /** Cantidad necesaria para dar el hábito por cumplido (1 si no tiene objetivo). */
    val goal: Int get() = target?.amount ?: 1

    val isArchived: Boolean get() = archivedOn != null

    /**
     * Los objetivos pequeños (8 vasos de agua) se registran de uno en uno con cada toque;
     * los grandes (45 minutos) se completan de golpe.
     */
    val countsStepByStep: Boolean get() = target != null && target.amount <= STEP_BY_STEP_LIMIT

    /** ¿El hábito existía y no estaba archivado ese día? */
    fun isActiveOn(date: LocalDate): Boolean =
        !date.isBefore(createdOn) && (archivedOn == null || date.isBefore(archivedOn))

    /** Cantidad registrada que queda tras un toque: avanzar, completar o desmarcar. */
    fun amountAfterTap(current: Int): Int = when {
        current >= goal -> 0
        countsStepByStep -> current + 1
        else -> goal
    }

    companion object {
        const val STEP_BY_STEP_LIMIT = 10
    }
}

/** Lo registrado para un hábito en un día concreto. */
data class Completion(
    val habitId: Long,
    val date: LocalDate,
    val amount: Int,
    val done: Boolean,
)
