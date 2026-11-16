package dev.juanpe.habittracker.domain.model

/** Categoría visual de un logro (cambia el color de la medalla). */
enum class AchievementTier { BRONZE, SILVER, GOLD }

/** Números con los que se decide qué logros están conseguidos. */
data class AchievementStats(
    val totalCompletions: Int = 0,
    val completionsByArea: Map<Area, Int> = emptyMap(),
    val completeDays: Int = 0,
    val bestGlobalStreak: Int = 0,
    /** Mejor racha en días de un único hábito. */
    val bestHabitStreakDays: Int = 0,
    val generalLevel: Int = 1,
    /** Nivel del área más floja. */
    val minAreaLevel: Int = 1,
    val freezesUsed: Int = 0,
    val comebacks: Int = 0,
) {
    fun completionsIn(area: Area): Int = completionsByArea[area] ?: 0
}

/**
 * Catálogo de logros. Cada uno se define con una meta ([goal]) y la métrica
 * que se compara con ella, así la misma definición sirve para saber si está
 * conseguido y para pintar su barra de progreso.
 */
enum class Achievement(
    val tier: AchievementTier,
    val goal: Int,
    private val metric: (AchievementStats) -> Int,
) {
    // Primeros pasos
    FIRST_STEP(AchievementTier.BRONZE, 1, { it.totalCompletions }),
    FIRST_FULL_DAY(AchievementTier.BRONZE, 1, { it.completeDays }),

    // Rachas de días completos
    STREAK_7(AchievementTier.SILVER, 7, { it.bestGlobalStreak }),
    STREAK_14(AchievementTier.SILVER, 14, { it.bestGlobalStreak }),
    STREAK_30(AchievementTier.GOLD, 30, { it.bestGlobalStreak }),
    STREAK_100(AchievementTier.GOLD, 100, { it.bestGlobalStreak }),
    HABIT_STREAK_21(AchievementTier.SILVER, 21, { it.bestHabitStreakDays }),

    // Volver a empezar también cuenta
    COMEBACK(AchievementTier.BRONZE, 1, { it.comebacks }),
    SAFETY_NET(AchievementTier.BRONZE, 1, { it.freezesUsed }),

    // Volumen total
    COMPLETIONS_50(AchievementTier.BRONZE, 50, { it.totalCompletions }),
    COMPLETIONS_250(AchievementTier.SILVER, 250, { it.totalCompletions }),
    COMPLETIONS_1000(AchievementTier.GOLD, 1000, { it.totalCompletions }),

    // Por área
    DIET_25(AchievementTier.BRONZE, 25, { it.completionsIn(Area.DIET) }),
    DIET_100(AchievementTier.GOLD, 100, { it.completionsIn(Area.DIET) }),
    TRAINING_25(AchievementTier.BRONZE, 25, { it.completionsIn(Area.TRAINING) }),
    TRAINING_100(AchievementTier.GOLD, 100, { it.completionsIn(Area.TRAINING) }),
    STUDY_25(AchievementTier.BRONZE, 25, { it.completionsIn(Area.STUDY) }),
    STUDY_100(AchievementTier.GOLD, 100, { it.completionsIn(Area.STUDY) }),
    RELATIONS_25(AchievementTier.BRONZE, 25, { it.completionsIn(Area.RELATIONS) }),
    RELATIONS_100(AchievementTier.GOLD, 100, { it.completionsIn(Area.RELATIONS) }),

    // Niveles
    LEVEL_5(AchievementTier.BRONZE, 5, { it.generalLevel }),
    LEVEL_10(AchievementTier.SILVER, 10, { it.generalLevel }),
    LEVEL_20(AchievementTier.GOLD, 20, { it.generalLevel }),
    BALANCE(AchievementTier.SILVER, 3, { it.minAreaLevel }),
    ;

    /** Progreso actual hacia la meta, sin pasarse de ella. */
    fun progress(stats: AchievementStats): Int = metric(stats).coerceIn(0, goal)

    fun isEarned(stats: AchievementStats): Boolean = metric(stats) >= goal

    companion object {
        /** Logros que se cumplen con [stats] y aún no están en [alreadyUnlocked]. */
        fun newlyEarned(stats: AchievementStats, alreadyUnlocked: Set<Achievement>): List<Achievement> =
            entries.filter { it !in alreadyUnlocked && it.isEarned(stats) }
    }
}
