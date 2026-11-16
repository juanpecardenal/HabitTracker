package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate
import kotlin.math.roundToInt

/** Qué se espera de un hábito en un día concreto. */
enum class DayDemand {
    /** Hay que hacerlo hoy para que el día cuente como completo. */
    REQUIRED,

    /** Se puede hacer hoy (suma), pero aún puede dejarse para otro día de la semana. */
    OPTIONAL,

    /** Hoy no toca. */
    NOT_SCHEDULED,
}

/** Reglas de frecuencia: decide qué días toca cada hábito. */
object Schedule {

    private const val DAYS_IN_WEEK = 7

    /** ¿El hábito aparece en la lista de ese día? */
    fun isPlannedOn(habit: Habit, date: LocalDate): Boolean {
        if (!habit.isActiveOn(date)) return false
        return when (val frequency = habit.frequency) {
            Frequency.Daily -> true
            is Frequency.SpecificDays -> date.dayOfWeek in frequency.days
            is Frequency.TimesPerWeek -> true
        }
    }

    /** Número de días entre [from] y [to] en los que el hábito estaba activo. */
    fun activeDays(habit: Habit, from: LocalDate, to: LocalDate): Int =
        daysBetween(from, to).count { habit.isActiveOn(it) }

    /**
     * Veces que hay que cumplir un hábito "X veces por semana" en la semana que empieza
     * en [weekStart]. Si el hábito solo estuvo activo parte de la semana (se creó o se
     * archivó a mitad), el objetivo se reduce en proporción: 4 veces por semana con solo
     * 4 días disponibles son 2 veces. Devuelve 0 para otras frecuencias.
     */
    fun weeklyTarget(habit: Habit, weekStart: LocalDate): Int {
        val frequency = habit.frequency as? Frequency.TimesPerWeek ?: return 0
        val available = activeDays(habit, weekStart, weekStart.plusDays(6))
        return when (available) {
            0 -> 0
            DAYS_IN_WEEK -> frequency.times
            else -> (frequency.times * available.toDouble() / DAYS_IN_WEEK).roundToInt().coerceIn(1, available)
        }
    }

    /**
     * Qué se espera del hábito el día [date].
     *
     * Para los hábitos "X veces por semana" solo es obligatorio cuando ya no queda
     * margen: si faltan 2 veces y quedan 2 días de semana, hoy toca sí o sí.
     * Solo se miran los días anteriores a [date], así el resultado no cambia
     * al marcar o desmarcar el propio día.
     *
     * @param doneDates días en los que este hábito quedó cumplido.
     */
    fun demandOn(habit: Habit, date: LocalDate, doneDates: Set<LocalDate>): DayDemand {
        if (!isPlannedOn(habit, date)) return DayDemand.NOT_SCHEDULED
        if (habit.frequency !is Frequency.TimesPerWeek) return DayDemand.REQUIRED

        val weekStart = date.startOfWeek()
        val doneEarlierThisWeek = doneDates.count { !it.isBefore(weekStart) && it.isBefore(date) }
        val stillNeeded = weeklyTarget(habit, weekStart) - doneEarlierThisWeek
        if (stillNeeded <= 0) return DayDemand.OPTIONAL

        val daysLeft = activeDays(habit, date, weekStart.plusDays(6))
        return if (stillNeeded >= daysLeft) DayDemand.REQUIRED else DayDemand.OPTIONAL
    }
}
