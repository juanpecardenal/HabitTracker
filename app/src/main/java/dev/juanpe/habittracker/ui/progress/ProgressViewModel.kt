package dev.juanpe.habittracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.data.repository.SnapshotSource
import dev.juanpe.habittracker.domain.logic.AppSnapshot
import dev.juanpe.habittracker.domain.logic.GlobalStreakEngine
import dev.juanpe.habittracker.domain.logic.LevelCurve
import dev.juanpe.habittracker.domain.logic.LevelProgress
import dev.juanpe.habittracker.domain.logic.WeekCompliance
import dev.juanpe.habittracker.domain.logic.WeeklySummaryCalculator
import dev.juanpe.habittracker.domain.logic.startOfWeek
import dev.juanpe.habittracker.domain.model.Area
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Nivel y resumen semanal de un área. */
data class AreaProgress(
    val area: Area,
    val level: LevelProgress,
    val thisWeek: WeekCompliance,
    val lastWeek: WeekCompliance,
) {
    /** Diferencia en puntos porcentuales con la semana pasada, o null si no se puede comparar. */
    val deltaPoints: Int? get() = deltaPoints(thisWeek, lastWeek)
}

/** Diferencia en puntos porcentuales entre dos semanas (null si alguna no tiene datos). */
fun deltaPoints(thisWeek: WeekCompliance, lastWeek: WeekCompliance): Int? {
    val current = thisWeek.ratio ?: return null
    val previous = lastWeek.ratio ?: return null
    return ((current - previous) * 100).toInt()
}

data class ProgressUiState(
    val isLoading: Boolean = true,
    val level: LevelProgress = LevelCurve.GENERAL.progressFor(0),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val freezes: Int = 0,
    /** Días completos seguidos que faltan para ganar el próximo comodín. */
    val daysToNextFreeze: Int = GlobalStreakEngine.DAYS_PER_FREEZE,
    val freezeProgress: Int = 0,
    val areas: List<AreaProgress> = emptyList(),
    /** Área con menos experiencia, para sugerir dónde poner el foco (null si van todas igual). */
    val weakestArea: Area? = null,
    val thisWeek: WeekCompliance = WeekCompliance.EMPTY,
    val lastWeek: WeekCompliance = WeekCompliance.EMPTY,
) {
    val hasMaxFreezes: Boolean get() = freezes >= GlobalStreakEngine.MAX_FREEZES
    val weekDeltaPoints: Int? get() = deltaPoints(thisWeek, lastWeek)
}

class ProgressViewModel(snapshots: SnapshotSource) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = snapshots.observe()
        .map(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    private fun buildState(snapshot: AppSnapshot): ProgressUiState {
        val today = snapshot.today
        val weekStart = today.startOfWeek()
        // En el resumen cuentan también los hábitos archivados: formaron parte de esas semanas.
        val thisWeek = WeeklySummaryCalculator.byArea(weekStart, today, snapshot.habits, snapshot.completions)
        val lastWeek = WeeklySummaryCalculator.byArea(weekStart.minusWeeks(1), today, snapshot.habits, snapshot.completions)

        val areas = Area.entries.map { area ->
            AreaProgress(
                area = area,
                level = snapshot.xp.area(area),
                thisWeek = thisWeek.getValue(area),
                lastWeek = lastWeek.getValue(area),
            )
        }
        val xpByArea = areas.map { it.level.totalXp }
        val streak = snapshot.liveStreak

        return ProgressUiState(
            isLoading = false,
            level = snapshot.xp.general,
            streak = streak.current,
            bestStreak = streak.best,
            freezes = streak.freezes,
            daysToNextFreeze = GlobalStreakEngine.DAYS_PER_FREEZE - streak.freezeProgress,
            freezeProgress = streak.freezeProgress,
            areas = areas,
            weakestArea = if (xpByArea.distinct().size > 1) areas.minBy { it.level.totalXp }.area else null,
            thisWeek = thisWeek.values.fold(WeekCompliance.EMPTY, WeekCompliance::plus),
            lastWeek = lastWeek.values.fold(WeekCompliance.EMPTY, WeekCompliance::plus),
        )
    }
}
