package dev.juanpe.habittracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.data.repository.SnapshotSource
import dev.juanpe.habittracker.domain.logic.AppSnapshot
import dev.juanpe.habittracker.domain.logic.HabitStreak
import dev.juanpe.habittracker.domain.logic.Schedule
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.domain.logic.WeekCompliance
import dev.juanpe.habittracker.domain.logic.WeeklySummaryCalculator
import dev.juanpe.habittracker.domain.logic.daysBetween
import dev.juanpe.habittracker.domain.logic.startOfWeek
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.ui.navigation.Routes
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Tipos de celda del mapa de calor. */
enum class HeatKind {
    /** Fuera del periodo del hábito (antes de crearlo o en el futuro). */
    OUTSIDE,

    /** Ese día no tocaba. */
    OFF,

    /** Tocaba y no se hizo (o aún no se ha hecho). */
    EMPTY,

    /** Se registró algo sin llegar al objetivo. */
    PARTIAL,
    DONE,
}

data class HeatCell(val date: LocalDate, val kind: HeatKind, val fraction: Float = 0f)

/** Una barra del gráfico semanal. [ratio] es null si esa semana no tocaba nada. */
data class WeekBar(val weekStart: LocalDate, val ratio: Float?, val isCurrent: Boolean)

data class HabitDetailUiState(
    val isLoading: Boolean = true,
    /** null tras cargar significa que el hábito ya no existe (se ha borrado). */
    val habit: Habit? = null,
    val streak: HabitStreak = HabitStreak(0, 0, StreakUnit.DAYS),
    val totalDone: Int = 0,
    /** Porcentaje de cumplimiento de las últimas semanas, o null si aún no hay datos. */
    val compliancePercent: Int? = null,
    val today: LocalDate = LocalDate.now(),
    /** Celdas del mapa de calor, semana a semana, de lunes a domingo. */
    val heatmap: List<HeatCell> = emptyList(),
    val weeks: List<WeekBar> = emptyList(),
)

class HabitDetailViewModel(
    savedStateHandle: SavedStateHandle,
    snapshots: SnapshotSource,
) : ViewModel() {

    private val habitId: Long = savedStateHandle[Routes.ARG_HABIT_ID] ?: Routes.NO_HABIT

    val uiState: StateFlow<HabitDetailUiState> = snapshots.observe()
        .map(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    private fun buildState(snapshot: AppSnapshot): HabitDetailUiState {
        val habit = snapshot.habits.firstOrNull { it.id == habitId }
            ?: return HabitDetailUiState(isLoading = false)
        val today = snapshot.today
        val doneDates = snapshot.completions.doneDates(habit.id)
        val currentWeek = today.startOfWeek()

        // Gráfico: cumplimiento de las últimas semanas.
        val compliances = (CHART_WEEKS - 1 downTo 0).map { weeksAgo ->
            val weekStart = currentWeek.minusWeeks(weeksAgo.toLong())
            weekStart to WeeklySummaryCalculator.habitWeek(habit, weekStart, today, doneDates)
        }
        val total = compliances.map { it.second }.fold(WeekCompliance.EMPTY, WeekCompliance::plus)

        return HabitDetailUiState(
            isLoading = false,
            habit = habit,
            streak = snapshot.streakOf(habit),
            totalDone = doneDates.size,
            compliancePercent = total.ratio?.let { (it * 100).toInt() },
            today = today,
            heatmap = buildHeatmap(habit, snapshot),
            weeks = compliances.map { (weekStart, compliance) ->
                WeekBar(weekStart, compliance.ratio, isCurrent = weekStart == currentWeek)
            },
        )
    }

    private fun buildHeatmap(habit: Habit, snapshot: AppSnapshot): List<HeatCell> {
        val today = snapshot.today
        val entries = snapshot.completions.entries(habit.id)
        val firstDay = today.startOfWeek().minusWeeks(HEATMAP_WEEKS - 1L)
        val lastDay = today.startOfWeek().plusDays(6)

        return daysBetween(firstDay, lastDay).map { date ->
            val entry = entries[date]
            when {
                entry?.done == true -> HeatCell(date, HeatKind.DONE, 1f)
                entry != null && entry.amount > 0 ->
                    HeatCell(date, HeatKind.PARTIAL, (entry.amount.toFloat() / habit.goal).coerceIn(0f, 1f))
                date.isAfter(today) || !habit.isActiveOn(date) -> HeatCell(date, HeatKind.OUTSIDE)
                Schedule.isPlannedOn(habit, date) -> HeatCell(date, HeatKind.EMPTY)
                else -> HeatCell(date, HeatKind.OFF)
            }
        }.toList()
    }

    companion object {
        const val HEATMAP_WEEKS = 18
        const val CHART_WEEKS = 8
    }
}
