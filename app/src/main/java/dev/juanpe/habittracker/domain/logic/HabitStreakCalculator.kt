package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate

/** Los hábitos semanales cuentan su racha en semanas; el resto, en días. */
enum class StreakUnit { DAYS, WEEKS }

data class HabitStreak(val current: Int, val best: Int, val unit: StreakUnit)

/** Calcula la racha actual y la máxima de un hábito a partir de los días cumplidos. */
object HabitStreakCalculator {

    /**
     * @param doneDates días en los que el hábito quedó cumplido.
     * @param protectedDates días cubiertos por un comodín: si el hábito falló
     *   ese día, la racha no se rompe (aunque tampoco suma).
     */
    fun calculate(
        habit: Habit,
        doneDates: Set<LocalDate>,
        today: LocalDate,
        protectedDates: Set<LocalDate> = emptySet(),
    ): HabitStreak = when (habit.frequency) {
        is Frequency.TimesPerWeek -> weeklyStreak(habit, doneDates, today)
        else -> dailyStreak(habit, doneDates, today, protectedDates)
    }

    /**
     * Racha en días programados seguidos. Los días en que no toca se saltan,
     * y el día de hoy sin hacer todavía no rompe nada: aún hay tiempo.
     */
    private fun dailyStreak(
        habit: Habit,
        doneDates: Set<LocalDate>,
        today: LocalDate,
        protectedDates: Set<LocalDate>,
    ): HabitStreak {
        var run = 0
        var best = 0
        for (date in daysBetween(habit.createdOn, today)) {
            if (!Schedule.isPlannedOn(habit, date)) continue
            when {
                date in doneDates -> {
                    run++
                    best = maxOf(best, run)
                }
                date == today || date in protectedDates -> Unit
                else -> run = 0
            }
        }
        return HabitStreak(current = run, best = best, unit = StreakUnit.DAYS)
    }

    /**
     * Racha en semanas seguidas alcanzando el objetivo semanal. La semana en
     * curso suma en cuanto se alcanza el objetivo y no rompe mientras siga abierta.
     */
    private fun weeklyStreak(habit: Habit, doneDates: Set<LocalDate>, today: LocalDate): HabitStreak {
        val doneByWeek = doneDates.groupingBy { it.startOfWeek() }.eachCount()
        val currentWeek = today.startOfWeek()
        var run = 0
        var best = 0
        var week = habit.createdOn.startOfWeek()
        while (!week.isAfter(currentWeek)) {
            val target = Schedule.weeklyTarget(habit, week)
            if (target > 0) {
                val done = doneByWeek[week] ?: 0
                when {
                    done >= target -> {
                        run++
                        best = maxOf(best, run)
                    }
                    week == currentWeek -> Unit
                    else -> run = 0
                }
            }
            week = week.plusWeeks(1)
        }
        return HabitStreak(current = run, best = best, unit = StreakUnit.WEEKS)
    }
}
