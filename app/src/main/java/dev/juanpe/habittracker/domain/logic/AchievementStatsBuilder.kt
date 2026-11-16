package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.AchievementStats
import dev.juanpe.habittracker.domain.model.StreakState

/**
 * Reúne en un [AchievementStats] los números que usan los logros.
 * Lo comparten el repositorio (para desbloquear) y la pantalla de logros
 * (para pintar el progreso), así ambos cuentan exactamente igual.
 *
 * @param streak estado de la racha global incluyendo el día de hoy si ya está completo.
 * @param completeDays días completos en total, incluido hoy si lo está.
 */
fun buildAchievementStats(
    xp: XpSummary,
    streak: StreakState,
    completeDays: Int,
    bestHabitStreakDays: Int,
): AchievementStats = AchievementStats(
    totalCompletions = xp.totalCompletions,
    completionsByArea = xp.completionsByArea,
    completeDays = completeDays,
    bestGlobalStreak = streak.best,
    bestHabitStreakDays = bestHabitStreakDays,
    generalLevel = xp.general.level,
    minAreaLevel = xp.minAreaLevel,
    freezesUsed = streak.freezesUsed,
    comebacks = streak.comebacks,
)
