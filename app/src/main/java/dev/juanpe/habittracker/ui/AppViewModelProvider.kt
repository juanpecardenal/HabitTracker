package dev.juanpe.habittracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.juanpe.habittracker.HabitApp
import dev.juanpe.habittracker.di.AppContainer
import dev.juanpe.habittracker.ui.achievements.AchievementsViewModel
import dev.juanpe.habittracker.ui.detail.HabitDetailViewModel
import dev.juanpe.habittracker.ui.habits.EditHabitViewModel
import dev.juanpe.habittracker.ui.habits.HabitsViewModel
import dev.juanpe.habittracker.ui.progress.ProgressViewModel
import dev.juanpe.habittracker.ui.settings.SettingsViewModel
import dev.juanpe.habittracker.ui.today.TodayViewModel

/**
 * Fábrica de todos los ViewModels. Como reciben dependencias por el constructor,
 * Android no sabe crearlos solo: aquí se le explica cómo, sacando cada pieza
 * del [AppContainer].
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            TodayViewModel(container().snapshots, container().progressRepository, container().celebrations)
        }
        initializer {
            HabitsViewModel(container().snapshots, container().habitRepository, container().reminderScheduler)
        }
        initializer {
            EditHabitViewModel(
                // Da acceso a los argumentos de navegación (id del hábito, área).
                savedStateHandle = createSavedStateHandle(),
                habitRepository = container().habitRepository,
                reminderScheduler = container().reminderScheduler,
                clock = container().clock,
            )
        }
        initializer {
            HabitDetailViewModel(createSavedStateHandle(), container().snapshots)
        }
        initializer {
            ProgressViewModel(container().snapshots)
        }
        initializer {
            AchievementsViewModel(container().snapshots, container().progressRepository, container().celebrations)
        }
        initializer {
            SettingsViewModel(container().progressRepository, container().reminderScheduler)
        }
    }

    private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as HabitApp).container
}
