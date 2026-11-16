package dev.juanpe.habittracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.DayStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/*
 * DAOs: las consultas a la base de datos. Las funciones que devuelven Flow
 * vuelven a emitir solas cada vez que cambia la tabla; las `suspend` se
 * ejecutan fuera del hilo principal.
 */

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY id")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Insert
    suspend fun insertAll(habits: List<HabitEntity>)

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE date BETWEEN :from AND :to")
    suspend fun getBetween(from: LocalDate, to: LocalDate): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId")
    suspend fun getForHabit(habitId: Long): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun get(habitId: Long, date: LocalDate): CompletionEntity?

    @Upsert
    suspend fun upsert(completion: CompletionEntity)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: LocalDate)
}

/** Resultado de agrupar la XP de hábitos por área. */
data class AreaXpRow(val area: Area, val total: Int, val count: Int)

@Dao
interface XpDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    fun observeTotal(): Flow<Int>

    @Query("SELECT area, SUM(amount) AS total, COUNT(*) AS count FROM xp_events WHERE type = 'HABIT' AND area IS NOT NULL GROUP BY area")
    fun observeByArea(): Flow<List<AreaXpRow>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    suspend fun getTotal(): Int

    @Query("SELECT area, SUM(amount) AS total, COUNT(*) AS count FROM xp_events WHERE type = 'HABIT' AND area IS NOT NULL GROUP BY area")
    suspend fun getByArea(): List<AreaXpRow>

    @Query("SELECT * FROM xp_events WHERE date = :date AND habitId = :habitId AND type = :type")
    suspend fun get(date: LocalDate, habitId: Long, type: XpEventType): XpEventEntity?

    @Upsert
    suspend fun upsert(event: XpEventEntity)

    @Query("DELETE FROM xp_events WHERE date = :date AND habitId = :habitId AND type = :type")
    suspend fun delete(date: LocalDate, habitId: Long, type: XpEventType)
}

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records ORDER BY date")
    fun observeAll(): Flow<List<DayRecordEntity>>

    @Query("SELECT date FROM day_records WHERE status = :status")
    suspend fun getDatesWithStatus(status: DayStatus): List<LocalDate>

    @Query("SELECT COUNT(*) FROM day_records WHERE status = :status")
    suspend fun countWithStatus(status: DayStatus): Int

    @Upsert
    suspend fun upsert(record: DayRecordEntity)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM unlocked_achievements")
    fun observeAll(): Flow<List<UnlockedAchievementEntity>>

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun getAll(): List<UnlockedAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: UnlockedAchievementEntity)
}
