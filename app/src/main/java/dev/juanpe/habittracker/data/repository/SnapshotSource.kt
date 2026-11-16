package dev.juanpe.habittracker.data.repository

import dev.juanpe.habittracker.domain.logic.AppSnapshot
import dev.juanpe.habittracker.domain.logic.CompletionIndex
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

/**
 * Junta todos los Flows de datos en uno solo de [AppSnapshot].
 * Cada vez que cambia algo en la base de datos (o cambia el día) se emite
 * una foto nueva y las pantallas se actualizan solas.
 */
class SnapshotSource(
    private val habitRepository: HabitRepository,
    private val progressRepository: ProgressRepository,
    private val today: StateFlow<LocalDate>,
) {
    fun observe(): Flow<AppSnapshot> {
        // combine admite como mucho 5 flujos con tipos distintos; se agrupan en dos pasos.
        val habitData = combine(
            habitRepository.observeHabits(),
            habitRepository.observeCompletions(),
        ) { habits, completions -> habits to CompletionIndex(completions) }

        return combine(
            today,
            habitData,
            progressRepository.observeProfile(),
            progressRepository.observeXp(),
            progressRepository.observeDayRecords(),
        ) { today, (habits, completions), profile, xp, dayRecords ->
            AppSnapshot(today, habits, completions, profile, xp, dayRecords)
        }
            .conflate() // si llegan varios cambios seguidos, basta con el último
            .flowOn(Dispatchers.Default) // los cálculos, fuera del hilo principal
    }
}
