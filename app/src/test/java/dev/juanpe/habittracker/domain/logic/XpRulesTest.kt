package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Achievement
import dev.juanpe.habittracker.domain.model.AchievementStats
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Experiencia, niveles y logros. */
class XpRulesTest {

    // --- XP por hábito y por día --------------------------------------------------

    @Test
    fun `un habito sin racha da la xp base`() {
        assertEquals(10, XpRules.habitXp(streakAfter = 0))
        assertEquals(10, XpRules.habitXp(streakAfter = 1))
    }

    @Test
    fun `la racha del habito da un punto extra por dia hasta el tope`() {
        assertEquals(11, XpRules.habitXp(streakAfter = 2))
        assertEquals(14, XpRules.habitXp(streakAfter = 5))
        assertEquals(15, XpRules.habitXp(streakAfter = 6))
        assertEquals(15, XpRules.habitXp(streakAfter = 100))
    }

    @Test
    fun `el bonus de dia completo crece con la racha global hasta el tope`() {
        assertEquals(20, XpRules.dayBonusXp(globalStreakAfter = 1))
        assertEquals(22, XpRules.dayBonusXp(globalStreakAfter = 2))
        assertEquals(40, XpRules.dayBonusXp(globalStreakAfter = 11))
        assertEquals(50, XpRules.dayBonusXp(globalStreakAfter = 16))
        assertEquals(50, XpRules.dayBonusXp(globalStreakAfter = 365))
    }

    // --- Niveles -------------------------------------------------------------------

    private val curve = LevelCurve.GENERAL // 100 XP el primer nivel y 50 más cada uno

    @Test
    fun `con cero xp se esta en el nivel 1`() {
        val progress = curve.progressFor(0)
        assertEquals(1, progress.level)
        assertEquals(0, progress.xpIntoLevel)
        assertEquals(100, progress.xpForNextLevel)
        assertEquals(0f, progress.fraction, 0.001f)
    }

    @Test
    fun `cada nivel pide mas xp que el anterior`() {
        assertEquals(100, curve.xpToAdvanceFrom(1))
        assertEquals(150, curve.xpToAdvanceFrom(2))
        assertEquals(200, curve.xpToAdvanceFrom(3))
    }

    @Test
    fun `xp total acumulada para alcanzar cada nivel`() {
        assertEquals(0, curve.totalXpForLevel(1))
        assertEquals(100, curve.totalXpForLevel(2))
        assertEquals(250, curve.totalXpForLevel(3))
        assertEquals(450, curve.totalXpForLevel(4))
    }

    @Test
    fun `se sube de nivel justo al alcanzar el umbral`() {
        assertEquals(1, curve.progressFor(99).level)

        val exact = curve.progressFor(100)
        assertEquals(2, exact.level)
        assertEquals(0, exact.xpIntoLevel)
        assertEquals(150, exact.xpForNextLevel)
    }

    @Test
    fun `el progreso dentro del nivel se calcula sobre lo que pide ese nivel`() {
        val progress = curve.progressFor(175) // nivel 2 con 75 de 150
        assertEquals(2, progress.level)
        assertEquals(75, progress.xpIntoLevel)
        assertEquals(75, progress.xpRemaining)
        assertEquals(0.5f, progress.fraction, 0.001f)
    }

    @Test
    fun `progressFor y totalXpForLevel son coherentes entre si`() {
        for (level in 1..30) {
            val threshold = curve.totalXpForLevel(level)
            assertEquals(level, curve.progressFor(threshold).level)
            if (level > 1) assertEquals(level - 1, curve.progressFor(threshold - 1).level)
        }
    }

    @Test
    fun `una xp negativa se trata como cero`() {
        assertEquals(1, curve.progressFor(-50).level)
        assertEquals(0, curve.progressFor(-50).totalXp)
    }

    @Test
    fun `los niveles de area usan su propia curva y el area sin xp esta en nivel 1`() {
        val xp = XpSummary(total = 500, byArea = mapOf(Area.DIET to 60, Area.TRAINING to 200))
        assertEquals(2, xp.area(Area.DIET).level) // 60 XP: justo el primer umbral
        assertEquals(3, xp.area(Area.TRAINING).level) // 60 + 90 = 150 para el nivel 3
        assertEquals(1, xp.area(Area.STUDY).level)
        assertEquals(1, xp.minAreaLevel)
        assertEquals(4, xp.general.level) // 450 para el nivel 4
    }

    // --- Logros ---------------------------------------------------------------------

    @Test
    fun `sin actividad no hay ningun logro`() {
        assertEquals(emptyList<Achievement>(), Achievement.newlyEarned(AchievementStats(), emptySet()))
    }

    @Test
    fun `el primer habito desbloquea solo el primer paso`() {
        val stats = AchievementStats(totalCompletions = 1, completionsByArea = mapOf(Area.STUDY to 1))
        assertEquals(listOf(Achievement.FIRST_STEP), Achievement.newlyEarned(stats, emptySet()))
    }

    @Test
    fun `los logros ya conseguidos no se vuelven a dar`() {
        val stats = AchievementStats(totalCompletions = 1)
        assertEquals(emptyList<Achievement>(), Achievement.newlyEarned(stats, setOf(Achievement.FIRST_STEP)))
    }

    @Test
    fun `una racha de 30 dias desbloquea tambien las rachas menores`() {
        val earned = Achievement.newlyEarned(AchievementStats(bestGlobalStreak = 30), emptySet())
        assertTrue(Achievement.STREAK_7 in earned)
        assertTrue(Achievement.STREAK_14 in earned)
        assertTrue(Achievement.STREAK_30 in earned)
        assertFalse(Achievement.STREAK_100 in earned)
    }

    @Test
    fun `cien entrenamientos se cuentan solo en su area`() {
        val stats = AchievementStats(totalCompletions = 100, completionsByArea = mapOf(Area.TRAINING to 100))
        assertTrue(Achievement.TRAINING_100.isEarned(stats))
        assertFalse(Achievement.DIET_25.isEarned(stats))
    }

    @Test
    fun `el progreso de un logro nunca supera su meta`() {
        val stats = AchievementStats(bestGlobalStreak = 500)
        assertEquals(7, Achievement.STREAK_7.progress(stats))
        assertEquals(3, Achievement.STREAK_7.progress(AchievementStats(bestGlobalStreak = 3)))
    }

    @Test
    fun `el equilibrio exige que el area mas floja llegue al nivel 3`() {
        assertFalse(Achievement.BALANCE.isEarned(AchievementStats(minAreaLevel = 2)))
        assertTrue(Achievement.BALANCE.isEarned(AchievementStats(minAreaLevel = 3)))
    }

    @Test
    fun `las estadisticas de logros se montan con la racha y la xp actuales`() {
        val xp = XpSummary(
            total = 300,
            byArea = mapOf(Area.DIET to 150),
            completionsByArea = mapOf(Area.DIET to 12, Area.STUDY to 3),
        )
        val streak = StreakState(current = 2, best = 9, freezesUsed = 1, comebacks = 2)
        val stats = buildAchievementStats(xp, streak, completeDays = 20, bestHabitStreakDays = 11)

        assertEquals(15, stats.totalCompletions)
        assertEquals(12, stats.completionsIn(Area.DIET))
        assertEquals(0, stats.completionsIn(Area.TRAINING))
        assertEquals(9, stats.bestGlobalStreak)
        assertEquals(3, stats.generalLevel)
        assertEquals(1, stats.minAreaLevel)
        assertEquals(1, stats.freezesUsed)
        assertEquals(2, stats.comebacks)
    }
}
