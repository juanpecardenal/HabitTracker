package dev.juanpe.habittracker.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.logic.GlobalStreakEngine
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.domain.logic.WeekCompliance
import dev.juanpe.habittracker.ui.components.AnimatedBar
import dev.juanpe.habittracker.ui.components.AppCard
import dev.juanpe.habittracker.ui.components.Flame
import dev.juanpe.habittracker.ui.components.FreezeTokens
import dev.juanpe.habittracker.ui.components.LevelBadge
import dev.juanpe.habittracker.ui.components.SectionTitle
import dev.juanpe.habittracker.ui.components.streakText
import dev.juanpe.habittracker.ui.theme.AppTheme
import dev.juanpe.habittracker.ui.theme.color
import dev.juanpe.habittracker.ui.theme.icon
import dev.juanpe.habittracker.ui.theme.labelRes
import dev.juanpe.habittracker.ui.theme.soft

/** Pantalla de progreso: nivel general, racha y comodines, niveles por área y resumen semanal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: ProgressViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LevelCard(state)
            StreakCard(state)

            SectionTitle(stringResource(R.string.progress_areas_title), Modifier.padding(start = 4.dp, top = 12.dp))
            AreaLevelsCard(state)

            SectionTitle(stringResource(R.string.progress_week_title), Modifier.padding(start = 4.dp, top = 12.dp))
            WeeklySummaryCard(state)
        }
    }
}

@Composable
private fun LevelCard(state: ProgressUiState) {
    val gold = AppTheme.colors.gold
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(level = state.level.level, color = gold, size = 72.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.level_number, state.level.level),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.progress_xp_total, state.level.totalXp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                AnimatedBar(fraction = state.level.fraction, color = gold, height = 12.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.progress_xp_to_next, state.level.xpRemaining, state.level.level + 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Racha global con la llama grande y, debajo, los comodines. */
@Composable
private fun StreakCard(state: ProgressUiState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Flame(streak = state.streak, modifier = Modifier.size(84.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = streakText(state.streak, StreakUnit.DAYS),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.progress_streak_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.progress_streak_best, streakText(state.bestStreak, StreakUnit.DAYS)),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.flameOuter,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (state.streak == 0) {
                Text(
                    text = stringResource(R.string.progress_streak_zero),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.progress_freezes_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                FreezeTokens(available = state.freezes, max = GlobalStreakEngine.MAX_FREEZES, iconSize = 26.dp)
            }
            Text(
                text = stringResource(R.string.progress_freezes_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            if (state.hasMaxFreezes) {
                Text(
                    text = stringResource(R.string.progress_freezes_max),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppTheme.colors.freeze,
                )
            } else {
                AnimatedBar(
                    fraction = state.freezeProgress.toFloat() / GlobalStreakEngine.DAYS_PER_FREEZE,
                    color = AppTheme.colors.freeze,
                    height = 8.dp,
                )
                Text(
                    text = pluralStringResource(R.plurals.progress_freezes_next, state.daysToNextFreeze, state.daysToNextFreeze),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AreaLevelsCard(state: ProgressUiState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            state.areas.forEach { item ->
                val color = item.area.color
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(color.soft(0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.area.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(item.area.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(R.string.level_number, item.level.level),
                                style = MaterialTheme.typography.labelLarge,
                                color = color,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        AnimatedBar(fraction = item.level.fraction, color = color, height = 8.dp)
                        if (item.area == state.weakestArea) {
                            Text(
                                text = stringResource(R.string.progress_area_weakest),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(state: ProgressUiState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Cifra global de la semana y su comparación con la anterior.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = percentText(state.thisWeek),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        text = stringResource(R.string.progress_week_overall),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DeltaChip(state.weekDeltaPoints)
            }
            Text(
                text = stringResource(weekMessage(state)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.areas.forEach { item ->
                    val color = item.area.color
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(item.area.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(percentText(item.thisWeek), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            DeltaChip(item.deltaPoints, compact = true)
                        }
                        Spacer(Modifier.height(6.dp))
                        // Barra de arriba: esta semana. Barra fina de abajo: la semana pasada.
                        AnimatedBar(fraction = item.thisWeek.ratio ?: 0f, color = color, height = 10.dp)
                        Spacer(Modifier.height(4.dp))
                        AnimatedBar(fraction = item.lastWeek.ratio ?: 0f, color = color.copy(alpha = 0.35f), height = 5.dp)
                    }
                }
            }
            Text(
                text = stringResource(R.string.progress_week_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun percentText(week: WeekCompliance): String =
    week.ratio?.let { stringResource(R.string.percent, (it * 100).toInt()) } ?: "–"

/** Mensaje del resumen: siempre en positivo, también cuando la semana va peor. */
private fun weekMessage(state: ProgressUiState): Int {
    val delta = state.weekDeltaPoints
    return when {
        state.thisWeek.ratio == null -> R.string.progress_week_message_empty
        delta == null -> R.string.progress_week_message_first
        delta > 0 -> R.string.progress_week_message_up
        delta < 0 -> R.string.progress_week_message_down
        else -> R.string.progress_week_message_same
    }
}

/** Etiqueta "+12" / "-5" con flecha, comparando con la semana anterior. */
@Composable
private fun DeltaChip(deltaPoints: Int?, compact: Boolean = false) {
    if (deltaPoints == null) return
    val (icon, tint) = when {
        deltaPoints > 0 -> Icons.AutoMirrored.Rounded.TrendingUp to AppTheme.colors.diet
        // Bajar no se pinta en rojo: es información, no una regañina.
        deltaPoints < 0 -> Icons.AutoMirrored.Rounded.TrendingDown to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Icons.AutoMirrored.Rounded.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.soft(0.14f))
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 3.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.delta_points, deltaPoints),
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            color = tint,
        )
    }
}
