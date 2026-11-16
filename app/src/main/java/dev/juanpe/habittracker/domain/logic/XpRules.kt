package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Area

/** Cuánta experiencia da cada cosa. */
object XpRules {

    const val HABIT_BASE = 10
    const val HABIT_MAX_STREAK_BONUS = 5

    const val DAY_BASE = 20
    const val DAY_BONUS_PER_STREAK_DAY = 2
    const val DAY_MAX_STREAK_BONUS = 30

    /**
     * XP por cumplir un hábito: base + 1 punto extra por cada día (o semana)
     * de racha a partir del segundo, hasta un máximo.
     *
     * @param streakAfter racha del hábito contando ya esta vez.
     */
    fun habitXp(streakAfter: Int): Int =
        HABIT_BASE + (streakAfter - 1).coerceIn(0, HABIT_MAX_STREAK_BONUS)

    /**
     * Bonus por completar el día: base + extra creciente con la racha global.
     *
     * @param globalStreakAfter racha de días completos contando ya el de hoy.
     */
    fun dayBonusXp(globalStreakAfter: Int): Int {
        val streakBonus = (globalStreakAfter - 1).coerceAtLeast(0) * DAY_BONUS_PER_STREAK_DAY
        return DAY_BASE + streakBonus.coerceAtMost(DAY_MAX_STREAK_BONUS)
    }
}

/** Posición dentro de la curva de niveles para una cantidad de XP. */
data class LevelProgress(
    val level: Int,
    /** XP acumulada dentro del nivel actual. */
    val xpIntoLevel: Int,
    /** XP total que pide el nivel actual para pasar al siguiente. */
    val xpForNextLevel: Int,
    val totalXp: Int,
) {
    val fraction: Float get() = xpIntoLevel.toFloat() / xpForNextLevel

    val xpRemaining: Int get() = xpForNextLevel - xpIntoLevel
}

/**
 * Curva de niveles: pasar del nivel n al n+1 cuesta `base + step * (n - 1)` XP,
 * así cada nivel pide un poco más que el anterior.
 */
class LevelCurve(private val base: Int, private val step: Int) {

    /** XP necesaria para pasar de [level] a [level] + 1. */
    fun xpToAdvanceFrom(level: Int): Int = base + step * (level - 1)

    /** XP total acumulada necesaria para alcanzar [level] (0 para el nivel 1). */
    fun totalXpForLevel(level: Int): Int = (1 until level).sumOf { xpToAdvanceFrom(it) }

    fun progressFor(totalXp: Int): LevelProgress {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        while (remaining >= xpToAdvanceFrom(level)) {
            remaining -= xpToAdvanceFrom(level)
            level++
        }
        return LevelProgress(level, remaining, xpToAdvanceFrom(level), totalXp.coerceAtLeast(0))
    }

    companion object {
        /** Nivel general: suma toda la XP (hábitos + bonus de día). */
        val GENERAL = LevelCurve(base = 100, step = 50)

        /** Nivel de área: solo suma la XP de sus hábitos, así que la curva es más suave. */
        val AREA = LevelCurve(base = 60, step = 30)
    }
}

/** XP total y por área, con acceso directo a los niveles. */
data class XpSummary(
    val total: Int = 0,
    val byArea: Map<Area, Int> = emptyMap(),
    /** Número de hábitos cumplidos en cada área (para estadísticas y logros). */
    val completionsByArea: Map<Area, Int> = emptyMap(),
) {
    val totalCompletions: Int get() = completionsByArea.values.sum()

    val general: LevelProgress get() = LevelCurve.GENERAL.progressFor(total)

    fun area(area: Area): LevelProgress = LevelCurve.AREA.progressFor(byArea[area] ?: 0)

    /** Nivel del área más floja. */
    val minAreaLevel: Int get() = Area.entries.minOf { area(it).level }
}
