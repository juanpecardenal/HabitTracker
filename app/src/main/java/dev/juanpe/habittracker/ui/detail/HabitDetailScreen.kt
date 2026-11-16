package dev.juanpe.habittracker.ui.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.ui.components.AppCard
import dev.juanpe.habittracker.ui.components.Flame
import dev.juanpe.habittracker.ui.components.formatted
import dev.juanpe.habittracker.ui.components.streakText
import dev.juanpe.habittracker.ui.habits.habitSummary
import dev.juanpe.habittracker.ui.theme.color
import dev.juanpe.habittracker.ui.theme.icon
import dev.juanpe.habittracker.ui.theme.labelRes
import dev.juanpe.habittracker.ui.theme.soft

/** Estadísticas de un hábito: rachas, mapa de calor y cumplimiento semanal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habit = state.habit

    // Si el hábito se borra (desde el formulario de edición), esta pantalla ya no tiene sentido.
    LaunchedEffect(state.isLoading, habit == null) {
        if (!state.isLoading && habit == null) onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (habit != null) {
                        IconButton(onClick = { onEdit(habit.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (habit == null) return@Scaffold
        val color = habit.area.color

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailHeader(habit)

            if (state.totalDone == 0) {
                AppCard(color = color.soft(0.10f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.detail_no_records),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Cuatro cifras clave en una cuadrícula de 2x2.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = streakText(state.streak),
                    label = stringResource(R.string.detail_current_streak),
                    modifier = Modifier.weight(1f),
                ) {
                    val flameDays = if (state.streak.unit == StreakUnit.WEEKS) state.streak.current * 7 else state.streak.current
                    Flame(streak = flameDays, modifier = Modifier.size(34.dp))
                }
                StatTile(
                    value = streakText(state.streak.best, state.streak.unit),
                    label = stringResource(R.string.detail_best_streak),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = state.totalDone.toString(),
                    label = stringResource(R.string.detail_total),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = state.compliancePercent?.let { stringResource(R.string.percent, it) } ?: "–",
                    label = stringResource(R.string.detail_compliance, HabitDetailViewModel.CHART_WEEKS),
                    modifier = Modifier.weight(1f),
                )
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_heatmap_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.detail_heatmap_subtitle, HabitDetailViewModel.HEATMAP_WEEKS),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Heatmap(cells = state.heatmap, today = state.today, color = color)
                    Spacer(Modifier.height(12.dp))
                    HeatmapLegend(color)
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_weekly_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.detail_weekly_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    WeeklyChart(weeks = state.weeks, color = color)
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(habit: Habit) {
    val color = habit.area.color
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        // Etiqueta del área.
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.soft(0.16f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(habit.area.icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(habit.area.labelRes), style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(10.dp))
        Text(habit.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = habitSummary(habit),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        habit.reminder?.let { time ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.detail_reminder_at, time.formatted()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (habit.isArchived) {
            Text(
                text = stringResource(R.string.detail_archived),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Tarjeta pequeña con una cifra destacada y su etiqueta. */
@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    AppCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegend(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendSwatch(MaterialTheme.colorScheme.surfaceContainerHighest, stringResource(R.string.detail_legend_pending))
        Spacer(Modifier.width(6.dp))
        LegendSwatch(color.copy(alpha = 0.45f), stringResource(R.string.detail_legend_partial))
        Spacer(Modifier.width(6.dp))
        LegendSwatch(color, stringResource(R.string.detail_legend_done))
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
