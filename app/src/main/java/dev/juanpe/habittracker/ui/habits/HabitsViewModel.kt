package dev.juanpe.habittracker.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.data.repository.HabitRepository
import dev.juanpe.habittracker.data.repository.SnapshotSource
import dev.juanpe.habittracker.domain.logic.HabitStreak
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitListItem(val habit: Habit, val streak: HabitStreak)

data class HabitsSection(val area: Area, val items: List<HabitListItem>)

data class HabitsUiState(
    val isLoading: Boolean = true,
    /** Siempre las cuatro áreas, aunque alguna esté vacía. */
    val sections: List<HabitsSection> = emptyList(),
    val archived: List<Habit> = emptyList(),
) {
    val isEmpty: Boolean get() = archived.isEmpty() && sections.all { it.items.isEmpty() }
}

/** Pantalla "Mis hábitos": la lista completa para gestionarlos. */
class HabitsViewModel(
    snapshots: SnapshotSource,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<HabitsUiState> = snapshots.observe()
        .map { snapshot ->
            HabitsUiState(
                isLoading = false,
                sections = Area.entries.map { area ->
                    HabitsSection(
                        area = area,
                        items = snapshot.activeHabits
                            .filter { it.area == area }
                            .map { HabitListItem(it, snapshot.streakOf(it)) },
                    )
                },
                archived = snapshot.habits.filter { it.isArchived },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    /** Devuelve un hábito archivado a la lista activa. */
    fun restore(habit: Habit) {
        viewModelScope.launch {
            habitRepository.setArchived(habit.id, null)
            reminderScheduler.scheduleHabit(habit.copy(archivedOn = null))
        }
    }

    fun delete(habit: Habit) {
        viewModelScope.launch {
            habitRepository.delete(habit.id)
            reminderScheduler.cancelHabit(habit.id)
        }
    }
}
