package dev.juanpe.habittracker.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.data.repository.HabitRepository
import dev.juanpe.habittracker.di.DayClock
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Target
import dev.juanpe.habittracker.domain.model.TargetUnit
import dev.juanpe.habittracker.notifications.ReminderScheduler
import dev.juanpe.habittracker.ui.navigation.Routes
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Las tres formas de frecuencia que se pueden elegir en el formulario. */
enum class FrequencyKind { DAILY, DAYS, WEEKLY }

/** Estado del formulario de crear/editar hábito. */
data class EditHabitUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val area: Area = Area.DIET,
    val frequencyKind: FrequencyKind = FrequencyKind.DAILY,
    val days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val timesPerWeek: Int = 3,
    val hasTarget: Boolean = false,
    /** Texto tal cual lo escribe el usuario; se valida con [parsedTarget]. */
    val targetAmount: String = "",
    val targetUnit: TargetUnit = TargetUnit.MINUTES,
    val hasReminder: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val isArchived: Boolean = false,
    /** El error del nombre solo se enseña después de intentar guardar. */
    val showNameError: Boolean = false,
    /** true cuando ya se ha guardado/borrado y hay que cerrar la pantalla. */
    val finished: Boolean = false,
) {
    val parsedTarget: Int? get() = targetAmount.toIntOrNull()?.takeIf { it in 1..MAX_TARGET }
    val isNameValid: Boolean get() = name.isNotBlank()
    val isTargetValid: Boolean get() = !hasTarget || parsedTarget != null
    val areDaysValid: Boolean get() = frequencyKind != FrequencyKind.DAYS || days.isNotEmpty()
    val canSave: Boolean get() = isNameValid && isTargetValid && areDaysValid

    companion object {
        const val MAX_TARGET = 999
        const val MAX_NAME_LENGTH = 60
    }
}

class EditHabitViewModel(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
    private val clock: DayClock,
) : ViewModel() {

    private val habitId: Long = savedStateHandle[Routes.ARG_HABIT_ID] ?: Routes.NO_HABIT
    private val initialArea: String? = savedStateHandle[Routes.ARG_AREA]

    /** Hábito original cuando se edita; null al crear uno nuevo. */
    private var original: Habit? = null

    private val _uiState = MutableStateFlow(EditHabitUiState())
    val uiState: StateFlow<EditHabitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val habit = if (habitId == Routes.NO_HABIT) null else habitRepository.getHabit(habitId)
            original = habit
            _uiState.value = habit?.toFormState() ?: EditHabitUiState(
                isLoading = false,
                area = Area.entries.firstOrNull { it.name == initialArea } ?: Area.DIET,
            )
        }
    }

    // --- Eventos del formulario ---------------------------------------------------

    fun onNameChange(name: String) =
        _uiState.update { it.copy(name = name.take(EditHabitUiState.MAX_NAME_LENGTH), showNameError = false) }

    fun onAreaChange(area: Area) = _uiState.update { it.copy(area = area) }

    fun onFrequencyKindChange(kind: FrequencyKind) = _uiState.update { it.copy(frequencyKind = kind) }

    fun onDayToggle(day: DayOfWeek) = _uiState.update {
        it.copy(days = if (day in it.days) it.days - day else it.days + day)
    }

    fun onTimesPerWeekChange(times: Int) = _uiState.update { it.copy(timesPerWeek = times.coerceIn(1, 7)) }

    fun onHasTargetChange(hasTarget: Boolean) = _uiState.update { it.copy(hasTarget = hasTarget) }

    fun onTargetAmountChange(text: String) =
        _uiState.update { it.copy(targetAmount = text.filter(Char::isDigit).take(3)) }

    fun onTargetUnitChange(unit: TargetUnit) = _uiState.update { it.copy(targetUnit = unit) }

    fun onHasReminderChange(hasReminder: Boolean) = _uiState.update { it.copy(hasReminder = hasReminder) }

    fun onReminderTimeChange(time: LocalTime) = _uiState.update { it.copy(reminderTime = time) }

    // --- Acciones -------------------------------------------------------------------

    fun save() {
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(showNameError = !it.isNameValid) }
            return
        }
        val habit = Habit(
            id = original?.id ?: 0,
            name = state.name.trim(),
            area = state.area,
            frequency = when (state.frequencyKind) {
                FrequencyKind.DAILY -> Frequency.Daily
                FrequencyKind.DAYS -> Frequency.SpecificDays(state.days)
                FrequencyKind.WEEKLY -> Frequency.TimesPerWeek(state.timesPerWeek)
            },
            target = state.parsedTarget?.takeIf { state.hasTarget }?.let { Target(it, state.targetUnit) },
            reminder = state.reminderTime.takeIf { state.hasReminder },
            createdOn = original?.createdOn ?: clock.today.value,
            archivedOn = original?.archivedOn,
        )
        viewModelScope.launch {
            val id = habitRepository.save(habit)
            reminderScheduler.scheduleHabit(habit.copy(id = id))
            _uiState.update { it.copy(finished = true) }
        }
    }

    /** Archivar: el hábito deja de aparecer, pero conserva su historial y puede recuperarse. */
    fun toggleArchived() {
        val habit = original ?: return
        val archivedOn = if (habit.isArchived) null else clock.today.value
        viewModelScope.launch {
            habitRepository.setArchived(habit.id, archivedOn)
            reminderScheduler.scheduleHabit(habit.copy(archivedOn = archivedOn))
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val habit = original ?: return
        viewModelScope.launch {
            habitRepository.delete(habit.id)
            reminderScheduler.cancelHabit(habit.id)
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun Habit.toFormState(): EditHabitUiState {
        val defaults = EditHabitUiState()
        val frequency = frequency
        return EditHabitUiState(
            isLoading = false,
            isEditing = true,
            name = name,
            area = area,
            frequencyKind = when (frequency) {
                Frequency.Daily -> FrequencyKind.DAILY
                is Frequency.SpecificDays -> FrequencyKind.DAYS
                is Frequency.TimesPerWeek -> FrequencyKind.WEEKLY
            },
            days = (frequency as? Frequency.SpecificDays)?.days ?: defaults.days,
            timesPerWeek = (frequency as? Frequency.TimesPerWeek)?.times ?: defaults.timesPerWeek,
            hasTarget = target != null,
            targetAmount = target?.amount?.toString().orEmpty(),
            targetUnit = target?.unit ?: defaults.targetUnit,
            hasReminder = reminder != null,
            reminderTime = reminder ?: defaults.reminderTime,
            isArchived = isArchived,
        )
    }
}
