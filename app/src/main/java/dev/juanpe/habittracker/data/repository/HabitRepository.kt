package dev.juanpe.habittracker.data.repository

import dev.juanpe.habittracker.data.local.AppDatabase
import dev.juanpe.habittracker.data.local.toDomain
import dev.juanpe.habittracker.data.local.toEntity
import dev.juanpe.habittracker.domain.model.Completion
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Acceso a los hábitos y a su historial. Los ViewModels solo hablan con los
 * repositorios, nunca con Room directamente.
 */
class HabitRepository(db: AppDatabase) {

    private val habitDao = db.habitDao()
    private val completionDao = db.completionDao()

    /** Todos los hábitos, incluidos los archivados. */
    fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCompletions(): Flow<List<Completion>> =
        completionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getHabits(): List<Habit> = habitDao.getAll().map { it.toDomain() }

    suspend fun getHabit(id: Long): Habit? = habitDao.getById(id)?.toDomain()

    suspend fun getCompletionsFor(habitId: Long): List<Completion> =
        completionDao.getForHabit(habitId).map { it.toDomain() }

    /** Crea el hábito si es nuevo (id = 0) o lo actualiza. Devuelve su id. */
    suspend fun save(habit: Habit): Long =
        if (habit.id == 0L) {
            habitDao.insert(habit.toEntity())
        } else {
            habitDao.update(habit.toEntity())
            habit.id
        }

    /** Archiva el hábito desde [archivedOn], o lo recupera si es null. */
    suspend fun setArchived(id: Long, archivedOn: LocalDate?) {
        val habit = habitDao.getById(id) ?: return
        habitDao.update(habit.copy(archivedOn = archivedOn))
    }

    /** Borra el hábito y su historial. La experiencia ya ganada se conserva. */
    suspend fun delete(id: Long) = habitDao.deleteById(id)
}
