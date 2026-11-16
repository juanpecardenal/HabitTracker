package dev.juanpe.habittracker.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.data.repository.ProgressRepository
import dev.juanpe.habittracker.data.repository.SnapshotSource
import dev.juanpe.habittracker.di.CelebrationQueue
import dev.juanpe.habittracker.domain.model.Achievement
import dev.juanpe.habittracker.domain.model.Celebration
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AchievementItem(
    val achievement: Achievement,
    /** Día en que se consiguió, o null si sigue bloqueado. */
    val unlockedOn: LocalDate?,
    val progress: Int,
) {
    val isUnlocked: Boolean get() = unlockedOn != null
    val fraction: Float get() = progress.toFloat() / achievement.goal
}

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val items: List<AchievementItem> = emptyList(),
) {
    val unlockedCount: Int get() = items.count { it.isUnlocked }
}

class AchievementsViewModel(
    snapshots: SnapshotSource,
    progressRepository: ProgressRepository,
    private val celebrations: CelebrationQueue,
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> =
        combine(snapshots.observe(), progressRepository.observeUnlockedAchievements()) { snapshot, unlocked ->
            val stats = snapshot.achievementStats
            val items = Achievement.entries.map { achievement ->
                AchievementItem(achievement, unlocked[achievement], achievement.progress(stats))
            }
            AchievementsUiState(
                isLoading = false,
                // Primero los conseguidos (los más recientes arriba); después, los más cercanos.
                items = items.filter { it.isUnlocked }.sortedByDescending { it.unlockedOn } +
                    items.filter { !it.isUnlocked }.sortedByDescending { it.fraction },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())

    /** Tocar una medalla ya conseguida vuelve a mostrar su celebración. */
    fun replay(achievement: Achievement) {
        celebrations.add(listOf(Celebration.AchievementUnlocked(achievement)))
    }
}
