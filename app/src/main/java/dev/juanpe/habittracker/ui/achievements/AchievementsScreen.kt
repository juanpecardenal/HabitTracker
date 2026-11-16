package dev.juanpe.habittracker.ui.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.ui.components.AnimatedBar
import dev.juanpe.habittracker.ui.components.AppCard
import dev.juanpe.habittracker.ui.components.mediumLabel
import dev.juanpe.habittracker.ui.theme.AppTheme

/** Colección de logros: los conseguidos brillan, los pendientes enseñan cuánto falta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.achievements_title)) }) },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // La cabecera ocupa las dos columnas.
            item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                SummaryCard(unlocked = state.unlockedCount, total = state.items.size)
            }
            items(state.items, key = { it.achievement.name }) { item ->
                AchievementCard(
                    item = item,
                    onClick = if (item.isUnlocked) {
                        { viewModel.replay(item.achievement) }
                    } else {
                        null
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(unlocked: Int, total: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.achievements_count, unlocked, total),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                // Estado vacío: aún sin logros, se anima a por el primero.
                text = stringResource(
                    if (unlocked == 0) R.string.achievements_empty_hint else R.string.achievements_replay_hint,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            AnimatedBar(
                fraction = if (total == 0) 0f else unlocked.toFloat() / total,
                color = AppTheme.colors.gold,
                height = 10.dp,
            )
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementItem, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val achievement = item.achievement
    AppCard(
        modifier = modifier.fillMaxWidth().heightIn(min = 196.dp),
        color = if (item.isUnlocked) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AchievementBadge(achievement = achievement, unlocked = item.isUnlocked, size = 68.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(achievement.titleRes),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(achievement.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            val unlockedOn = item.unlockedOn
            if (unlockedOn != null) {
                Text(
                    text = unlockedOn.mediumLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = achievement.tier.color,
                )
            } else {
                AnimatedBar(fraction = item.fraction, color = achievement.tier.color, height = 6.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.achievements_progress, item.progress, achievement.goal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
