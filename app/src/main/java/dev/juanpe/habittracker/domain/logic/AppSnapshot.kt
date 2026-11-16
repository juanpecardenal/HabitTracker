package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.AchievementStats
import dev.juanpe.habittracker.domain.model.DayRecord
import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Profile
import dev.juanpe.habittracker.domain.model.StreakState
import java.time.LocalDate

/**
 * Foto completa de los datos de la app en un instante. Cada ViewModel la
 * recibe y la transforma en el estado de su pantalla, así todas las pantallas
 * calculan rachas, niveles y demás exactamente con las mismas reglas.
 *
 * Los valores derivados son `lazy`: solo se calculan si alguien los pide.
 */
class AppSnapshot(
    val today: LocalDate,
    /** Todos los hábitos, incluidos los archivados. */
    val habits: List<Habit>,
    val completions: CompletionIndex,
    val profile: Profile,
    val xp: XpSummary,
    val dayRecords: List<DayRecord>,
) {
    val activeHabits: List<Habit> by lazy { habits.filter { !it.isArchived } }

    /** Días protegidos por un comodín: no rompen las rachas de los hábitos. */
    val protectedDates: Set<LocalDate> by lazy {
        dayRecords.filter { it.status == DayStatus.FROZEN }.map { it.date }.toSet()
    }

    val todayOutcome: DayOutcome by lazy { DayEvaluator.evaluate(today, habits, completions) }

    /**
     * Racha global "en vivo": la guardada (hasta ayer) más el día de hoy si ya
     * está completo. La guardada solo cambia cuando se cierra el día.
     */
    val liveStreak: StreakState by lazy {
        if (todayOutcome.isComplete) {
            GlobalStreakEngine.applyDay(profile.streak, DayStatus.COMPLETE).state
        } else {
            profile.streak
        }
    }

    /** Días completos en total, contando hoy si lo está. */
    val completeDays: Int by lazy {
        dayRecords.count { it.status == DayStatus.COMPLETE } + if (todayOutcome.isComplete) 1 else 0
    }

    val achievementStats: AchievementStats by lazy {
        val bestHabitStreak = habits.map(::streakOf)
            .filter { it.unit == StreakUnit.DAYS }
            .maxOfOrNull { it.best } ?: 0
        buildAchievementStats(xp, liveStreak, completeDays, bestHabitStreak)
    }

    fun streakOf(habit: Habit): HabitStreak =
        HabitStreakCalculator.calculate(habit, completions.doneDates(habit.id), today, protectedDates)

    fun demandToday(habit: Habit): DayDemand =
        Schedule.demandOn(habit, today, completions.doneDates(habit.id))
}
