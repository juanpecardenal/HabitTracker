package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate

/** Cumplimiento de una semana: veces esperadas frente a veces hechas. */
data class WeekCompliance(val expected: Int, val done: Int) {

    /** Fracción 0..1, o null si esa semana no tocaba nada. */
    val ratio: Float? get() = if (expected == 0) null else done.toFloat() / expected

    operator fun plus(other: WeekCompliance) = WeekCompliance(expected + other.expected, done + other.done)

    companion object {
        val EMPTY = WeekCompliance(0, 0)
    }
}

/**
 * Calcula el porcentaje de cumplimiento semanal.
 *
 * Idea clave: no se penaliza lo que todavía se puede hacer. En la semana en curso
 * solo cuenta como "esperado" lo que ya debería estar hecho; lo de hoy (y lo que
 * queda de semana) solo entra en la cuenta cuando se cumple.
 */
object WeeklySummaryCalculator {

    /** Cumplimiento de un hábito en la semana que empieza en [weekStart]. */
    fun habitWeek(habit: Habit, weekStart: LocalDate, today: LocalDate, doneDates: Set<LocalDate>): WeekCompliance {
        val weekEnd = weekStart.plusDays(6)
        return when (habit.frequency) {
            is Frequency.TimesPerWeek -> flexibleWeek(habit, weekStart, weekEnd, today, doneDates)
            else -> plannedWeek(habit, weekStart, weekEnd, today, doneDates)
        }
    }

    /** Hábitos diarios o de días concretos: se cuenta día a día. */
    private fun plannedWeek(
        habit: Habit,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        today: LocalDate,
        doneDates: Set<LocalDate>,
    ): WeekCompliance {
        var expected = 0
        var done = 0
        for (date in daysBetween(weekStart, minOf(weekEnd, today))) {
            if (!Schedule.isPlannedOn(habit, date)) continue
            val isDone = date in doneDates
            if (date == today && !isDone) continue // hoy aún no ha terminado
            expected++
            if (isDone) done++
        }
        return WeekCompliance(expected, done)
    }

    /**
     * Hábitos "X veces por semana": lo esperado es lo que ya tendría que estar hecho
     * para poder llegar al objetivo con los días que quedan. Adelantar trabajo cuenta.
     */
    private fun flexibleWeek(
        habit: Habit,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        today: LocalDate,
        doneDates: Set<LocalDate>,
    ): WeekCompliance {
        if (weekStart.isAfter(today)) return WeekCompliance.EMPTY
        val target = Schedule.weeklyTarget(habit, weekStart)
        val done = doneDates.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }.coerceAtMost(target)

        // Oportunidades que quedan: los días futuros de la semana, y hoy si aún no se ha hecho.
        val chancesLeft = daysBetween(maxOf(today, weekStart), weekEnd).count { date ->
            habit.isActiveOn(date) && !(date == today && date in doneDates)
        }
        val shouldHaveDone = (target - chancesLeft).coerceAtLeast(0)
        return WeekCompliance(expected = maxOf(shouldHaveDone, done), done = done)
    }

    /** Cumplimiento por área, sumando todos sus hábitos. */
    fun byArea(
        weekStart: LocalDate,
        today: LocalDate,
        habits: List<Habit>,
        index: CompletionIndex,
    ): Map<Area, WeekCompliance> =
        Area.entries.associateWith { area ->
            habits.filter { it.area == area }
                .map { habitWeek(it, weekStart, today, index.doneDates(it.id)) }
                .fold(WeekCompliance.EMPTY, WeekCompliance::plus)
        }
}
