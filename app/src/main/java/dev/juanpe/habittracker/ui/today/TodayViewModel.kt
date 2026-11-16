package dev.juanpe.habittracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.data.repository.ProgressRepository
import dev.juanpe.habittracker.data.repository.SnapshotSource
import dev.juanpe.habittracker.di.CelebrationQueue
import dev.juanpe.habittracker.domain.logic.AppSnapshot
import dev.juanpe.habittracker.domain.logic.DayDemand
import dev.juanpe.habittracker.domain.logic.HabitStreak
import dev.juanpe.habittracker.domain.logic.LevelCurve
import dev.juanpe.habittracker.domain.logic.LevelProgress
import dev.juanpe.habittracker.domain.logic.Schedule
import dev.juanpe.habittracker.domain.logic.startOfWeek
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un hábito tal y como se muestra en la lista de hoy. */
data class TodayHabit(
    val habit: Habit,
    val amount: Int,
    val done: Boolean,
    /** Semanal con margen: hacerlo hoy suma, pero no es necesario para completar el día. */
    val optional: Boolean,
    val streak: HabitStreak,
    /** Veces hechas esta semana y objetivo semanal (solo hábitos "X veces por semana"). */
    val weekDone: Int,
    val weekTarget: Int,
) {
    val fraction: Float get() = (amount.toFloat() / habit.goal).coerceIn(0f, 1f)
    val isWeekly: Boolean get() = habit.frequency is Frequency.TimesPerWeek
}

data class TodaySection(val area: Area, val habits: List<TodayHabit>) {
    val doneCount: Int get() = habits.count { it.done }
}

/** Qué pasó ayer, para dar el mensaje adecuado al abrir la app. */
enum class YesterdayNote { NONE, MISSED, FROZEN }

data class TodayUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val sections: List<TodaySection> = emptyList(),
    /** false si el usuario no tiene ningún hábito activo (estado vacío). */
    val hasHabits: Boolean = false,
    /** Datos del anillo: hábitos hechos sobre los que cuentan hoy. */
    val ringDone: Int = 0,
    val ringTotal: Int = 0,
    val dayComplete: Boolean = false,
    val streak: Int = 0,
    val freezes: Int = 0,
    val level: LevelProgress = LevelCurve.GENERAL.progressFor(0),
    val yesterdayNote: YesterdayNote = YesterdayNote.NONE,
    /** true si hay recordatorios configurados (para ofrecer activar las notificaciones). */
    val usesNotifications: Boolean = false,
    val notificationBannerDismissed: Boolean = false,
) {
    val ringProgress: Float get() = if (ringTotal == 0) 0f else ringDone.toFloat() / ringTotal
}

/** XP recién ganada con un hábito, para mostrar el "+12 XP" flotante en su fila. */
data class XpPop(val habitId: Long, val amount: Int, val id: Int)

class TodayViewModel(
    snapshots: SnapshotSource,
    private val progressRepository: ProgressRepository,
    private val celebrations: CelebrationQueue,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = snapshots.observe()
        .map(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private val _xpPop = MutableStateFlow<XpPop?>(null)
    val xpPop: StateFlow<XpPop?> = _xpPop.asStateFlow()
    private var popCounter = 0

    /** Toque en el check: avanza, completa o desmarca según el estado actual. */
    fun onHabitTapped(item: TodayHabit) = setAmount(item.habit.id, item.habit.amountAfterTap(item.amount))

    /** Guarda una cantidad concreta (desde el panel de cantidad). */
    fun setAmount(habitId: Long, amount: Int) {
        val today = uiState.value.date
        viewModelScope.launch {
            val result = progressRepository.setProgress(habitId, today, amount)
            if (result.xpGained > 0) {
                val pop = XpPop(habitId, result.xpGained, popCounter++)
                _xpPop.value = pop
                // Se retira al acabar su animación para que no se repita al volver a la pantalla.
                launch {
                    delay(XP_POP_MILLIS)
                    if (_xpPop.value == pop) _xpPop.value = null
                }
            }
            if (result.celebrations.isNotEmpty()) {
                // Pequeña espera para que se vea terminar la animación del check antes de la fiesta.
                delay(CELEBRATION_DELAY_MILLIS)
                celebrations.add(result.celebrations)
            }
        }
    }

    fun dismissNotificationBanner() {
        viewModelScope.launch {
            progressRepository.updateProfile { it.copy(notificationBannerDismissed = true) }
        }
    }

    companion object {
        const val XP_POP_MILLIS = 1100L
        private const val CELEBRATION_DELAY_MILLIS = 480L
    }

    private fun buildState(snapshot: AppSnapshot): TodayUiState {
        val today = snapshot.today
        val weekStart = today.startOfWeek()

        val items = snapshot.activeHabits.mapNotNull { habit ->
            val demand = snapshot.demandToday(habit)
            if (demand == DayDemand.NOT_SCHEDULED) return@mapNotNull null
            val doneDates = snapshot.completions.doneDates(habit.id)
            TodayHabit(
                habit = habit,
                amount = snapshot.completions.amount(habit.id, today),
                done = today in doneDates,
                optional = demand == DayDemand.OPTIONAL,
                streak = snapshot.streakOf(habit),
                weekDone = doneDates.count { !it.isBefore(weekStart) },
                weekTarget = Schedule.weeklyTarget(habit, weekStart),
            )
        }
        val sections = Area.entries.mapNotNull { area ->
            val habits = items.filter { it.habit.area == area }
            if (habits.isEmpty()) null else TodaySection(area, habits)
        }

        val outcome = snapshot.todayOutcome
        val yesterdayStatus = snapshot.dayRecords.lastOrNull { it.date == today.minusDays(1) }?.status
        return TodayUiState(
            isLoading = false,
            date = today,
            sections = sections,
            hasHabits = snapshot.activeHabits.isNotEmpty(),
            // Los opcionales solo entran en el anillo cuando se hacen: nunca restan.
            ringDone = outcome.requiredDone + outcome.optionalDone,
            ringTotal = outcome.required + outcome.optionalDone,
            dayComplete = outcome.isComplete,
            streak = snapshot.liveStreak.current,
            freezes = snapshot.liveStreak.freezes,
            level = snapshot.xp.general,
            yesterdayNote = when {
                outcome.isComplete -> YesterdayNote.NONE // hoy ya está hecho: no hace falta el mensaje
                yesterdayStatus == DayStatus.MISSED -> YesterdayNote.MISSED
                yesterdayStatus == DayStatus.FROZEN -> YesterdayNote.FROZEN
                else -> YesterdayNote.NONE
            },
            usesNotifications = snapshot.profile.eveningReminderEnabled ||
                snapshot.activeHabits.any { it.reminder != null },
            notificationBannerDismissed = snapshot.profile.notificationBannerDismissed,
        )
    }
}
