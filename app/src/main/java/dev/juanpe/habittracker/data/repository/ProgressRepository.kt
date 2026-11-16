package dev.juanpe.habittracker.data.repository

import androidx.room.withTransaction
import dev.juanpe.habittracker.data.local.AppDatabase
import dev.juanpe.habittracker.data.local.AreaXpRow
import dev.juanpe.habittracker.data.local.CompletionEntity
import dev.juanpe.habittracker.data.local.DayRecordEntity
import dev.juanpe.habittracker.data.local.UnlockedAchievementEntity
import dev.juanpe.habittracker.data.local.XpEventEntity
import dev.juanpe.habittracker.data.local.XpEventEntity.Companion.DAY_BONUS_HABIT_ID
import dev.juanpe.habittracker.data.local.XpEventType
import dev.juanpe.habittracker.data.local.toDomain
import dev.juanpe.habittracker.data.local.toEntity
import dev.juanpe.habittracker.domain.logic.CompletionIndex
import dev.juanpe.habittracker.domain.logic.DayEvaluator
import dev.juanpe.habittracker.domain.logic.DayOutcome
import dev.juanpe.habittracker.domain.logic.GlobalStreakEngine
import dev.juanpe.habittracker.domain.logic.HabitStreak
import dev.juanpe.habittracker.domain.logic.HabitStreakCalculator
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.domain.logic.XpRules
import dev.juanpe.habittracker.domain.logic.XpSummary
import dev.juanpe.habittracker.domain.logic.buildAchievementStats
import dev.juanpe.habittracker.domain.logic.daysBetween
import dev.juanpe.habittracker.domain.logic.startOfWeek
import dev.juanpe.habittracker.domain.model.Achievement
import dev.juanpe.habittracker.domain.model.AchievementStats
import dev.juanpe.habittracker.domain.model.Celebration
import dev.juanpe.habittracker.domain.model.DayRecord
import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Profile
import dev.juanpe.habittracker.domain.model.ProgressResult
import dev.juanpe.habittracker.domain.model.StreakState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Todo lo que tiene que ver con el "juego": registrar progreso, experiencia,
 * racha global, comodines y logros.
 *
 * Este repositorio solo coordina lecturas y escrituras; las reglas de verdad
 * están en `domain/logic` como funciones puras (y con tests).
 */
class ProgressRepository(private val db: AppDatabase) {

    private val habitDao = db.habitDao()
    private val completionDao = db.completionDao()
    private val xpDao = db.xpDao()
    private val dayRecordDao = db.dayRecordDao()
    private val profileDao = db.profileDao()
    private val achievementDao = db.achievementDao()

    // --- Lectura reactiva ------------------------------------------------------

    fun observeProfile(): Flow<Profile> = profileDao.observe().filterNotNull().map { it.toDomain() }

    fun observeXp(): Flow<XpSummary> =
        combine(xpDao.observeTotal(), xpDao.observeByArea()) { total, rows -> xpSummary(total, rows) }

    fun observeDayRecords(): Flow<List<DayRecord>> =
        dayRecordDao.observeAll().map { list -> list.map { DayRecord(it.date, it.status) } }

    /** Logros ya conseguidos, con el día en que se desbloquearon. */
    fun observeUnlockedAchievements(): Flow<Map<Achievement, LocalDate>> =
        achievementDao.observeAll().map { list ->
            list.mapNotNull { row -> row.toAchievement()?.let { it to row.unlockedOn } }.toMap()
        }

    suspend fun getProfile(): Profile? = profileDao.get()?.toDomain()

    /** Balance del día [date] con lo registrado hasta ahora. */
    suspend fun dayOutcome(date: LocalDate): DayOutcome {
        val habits = habitDao.getAll().map { it.toDomain() }
        val completions = completionDao.getBetween(date.startOfWeek(), date).map { it.toDomain() }
        return DayEvaluator.evaluate(date, habits, CompletionIndex(completions))
    }

    // --- Arranque ---------------------------------------------------------------

    /** La primera vez que se abre la app crea el perfil y los hábitos de ejemplo. */
    suspend fun ensureInitialized(today: LocalDate, seedHabits: () -> List<Habit>) {
        db.withTransaction {
            if (profileDao.get() != null) return@withTransaction
            val profile = Profile(streak = StreakState(), lastClosedDate = today.minusDays(1))
            profileDao.upsert(profile.toEntity())
            habitDao.insertAll(seedHabits().map { it.toEntity() })
        }
    }

    /**
     * Cierra los días pasados que aún no se habían evaluado (normalmente solo ayer):
     * guarda cómo acabó cada uno y actualiza la racha global, gastando comodines
     * si hace falta. Se llama al abrir la app y antes de los avisos.
     */
    suspend fun closePendingDays(today: LocalDate): List<Celebration> = db.withTransaction {
        val profile = profileDao.get()?.toDomain() ?: return@withTransaction emptyList()
        val firstOpenDay = profile.lastClosedDate.plusDays(1)
        val yesterday = today.minusDays(1)
        if (firstOpenDay.isAfter(yesterday)) return@withTransaction emptyList()

        val habits = habitDao.getAll().map { it.toDomain() }
        val completions = completionDao.getBetween(firstOpenDay.startOfWeek(), yesterday).map { it.toDomain() }
        val index = CompletionIndex(completions)

        var streak = profile.streak
        for (date in daysBetween(firstOpenDay, yesterday)) {
            val outcome = DayEvaluator.evaluate(date, habits, index)
            val result = GlobalStreakEngine.applyDay(streak, outcome.status)
            dayRecordDao.upsert(DayRecordEntity(date, result.status))
            streak = result.state
        }
        profileDao.upsert(profile.copy(streak = streak, lastClosedDate = yesterday).toEntity())

        val stats = buildAchievementStats(
            xp = currentXp(),
            streak = streak,
            completeDays = dayRecordDao.countWithStatus(DayStatus.COMPLETE),
            bestHabitStreakDays = 0,
        )
        unlockNewAchievements(stats, today).map { Celebration.AchievementUnlocked(it) }
    }

    // --- Registrar progreso -------------------------------------------------------

    /**
     * Guarda la cantidad registrada hoy para un hábito y actualiza todo lo que
     * depende de ello: experiencia, bonus de día completo, niveles y logros.
     * Todo ocurre en una única transacción: o se guarda todo o nada.
     *
     * Desmarcar es seguro: la XP del hábito (y el bonus de día, si deja de
     * estar completo) se retira.
     */
    suspend fun setProgress(habitId: Long, date: LocalDate, newAmount: Int): ProgressResult = db.withTransaction {
        val habit = habitDao.getById(habitId)?.toDomain() ?: return@withTransaction ProgressResult()
        var profile = profileDao.get()?.toDomain() ?: return@withTransaction ProgressResult()
        val originalProfile = profile

        val amount = newAmount.coerceIn(0, MAX_AMOUNT)
        val done = amount >= habit.goal
        val wasDone = completionDao.get(habitId, date)?.done == true
        val xpBefore = currentXp()
        val celebrations = mutableListOf<Celebration>()

        // 1. El registro del día.
        if (amount == 0) {
            completionDao.delete(habitId, date)
        } else {
            completionDao.upsert(CompletionEntity(habitId, date, amount, done))
        }

        // 2. XP del hábito (con bonus por su racha).
        var xpGained = 0
        var habitStreak: HabitStreak? = null
        if (done) {
            val doneDates = completionDao.getForHabit(habitId).filter { it.done }.map { it.date }.toSet()
            val protectedDates = dayRecordDao.getDatesWithStatus(DayStatus.FROZEN).toSet()
            habitStreak = HabitStreakCalculator.calculate(habit, doneDates, date, protectedDates)
            if (!wasDone) {
                xpGained = XpRules.habitXp(habitStreak.current)
                xpDao.upsert(XpEventEntity(date, habitId, XpEventType.HABIT, habit.area, xpGained))
            }
        } else if (wasDone) {
            xpDao.delete(date, habitId, XpEventType.HABIT)
        }

        // 3. Bonus de día completo. La racha global real se actualiza al cerrar el día;
        //    aquí solo se calcula cómo quedaría para dar el bonus y la celebración.
        val outcome = dayOutcome(date)
        val streakPreview = if (outcome.isComplete) {
            GlobalStreakEngine.applyDay(profile.streak, DayStatus.COMPLETE)
        } else {
            null
        }
        if (streakPreview != null) {
            val existingBonus = xpDao.get(date, DAY_BONUS_HABIT_ID, XpEventType.DAY_BONUS)
            val bonus = existingBonus?.amount ?: XpRules.dayBonusXp(streakPreview.state.current)
            if (existingBonus == null) {
                xpDao.upsert(XpEventEntity(date, DAY_BONUS_HABIT_ID, XpEventType.DAY_BONUS, null, bonus))
            }
            if (profile.lastCelebratedDay != date) {
                celebrations += Celebration.DayComplete(streakPreview.state.current, bonus, streakPreview.freezeEarned)
                profile = profile.copy(lastCelebratedDay = date)
            }
        } else {
            xpDao.delete(date, DAY_BONUS_HABIT_ID, XpEventType.DAY_BONUS)
        }

        // 4. Subidas de nivel (general y del área del hábito).
        val xpAfter = currentXp()
        if (xpAfter.general.level > profile.maxLevelCelebrated) {
            celebrations += Celebration.LevelUp(xpAfter.general.level, area = null)
            profile = profile.copy(maxLevelCelebrated = xpAfter.general.level)
        }
        val areaLevel = xpAfter.area(habit.area).level
        if (areaLevel > xpBefore.area(habit.area).level) {
            celebrations += Celebration.LevelUp(areaLevel, habit.area)
        }

        // 5. Logros nuevos.
        val stats = buildAchievementStats(
            xp = xpAfter,
            streak = streakPreview?.state ?: profile.streak,
            completeDays = dayRecordDao.countWithStatus(DayStatus.COMPLETE) + if (streakPreview != null) 1 else 0,
            bestHabitStreakDays = habitStreak?.takeIf { it.unit == StreakUnit.DAYS }?.best ?: 0,
        )
        celebrations += unlockNewAchievements(stats, date).map { Celebration.AchievementUnlocked(it) }

        if (profile != originalProfile) profileDao.upsert(profile.toEntity())
        ProgressResult(completedNow = done && !wasDone, xpGained = xpGained, celebrations = celebrations)
    }

    // --- Ajustes --------------------------------------------------------------------

    /** Modifica el perfil de forma atómica (leer, transformar y guardar). */
    suspend fun updateProfile(transform: (Profile) -> Profile) {
        db.withTransaction {
            val profile = profileDao.get()?.toDomain() ?: return@withTransaction
            profileDao.upsert(transform(profile).toEntity())
        }
    }

    // --- Ayudas privadas ----------------------------------------------------------------

    private suspend fun currentXp(): XpSummary = xpSummary(xpDao.getTotal(), xpDao.getByArea())

    private fun xpSummary(total: Int, rows: List<AreaXpRow>) = XpSummary(
        total = total,
        byArea = rows.associate { it.area to it.total },
        completionsByArea = rows.associate { it.area to it.count },
    )

    private suspend fun unlockNewAchievements(stats: AchievementStats, today: LocalDate): List<Achievement> {
        val unlocked = achievementDao.getAll().mapNotNull { it.toAchievement() }.toSet()
        val newOnes = Achievement.newlyEarned(stats, unlocked)
        newOnes.forEach { achievementDao.insert(UnlockedAchievementEntity(it.name, today)) }
        return newOnes
    }

    /** Si en una versión futura se elimina un logro, las filas antiguas se ignoran. */
    private fun UnlockedAchievementEntity.toAchievement(): Achievement? =
        Achievement.entries.firstOrNull { it.name == id }

    companion object {
        const val MAX_AMOUNT = 9999
    }
}
