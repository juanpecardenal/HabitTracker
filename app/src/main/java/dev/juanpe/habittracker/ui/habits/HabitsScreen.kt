package dev.juanpe.habittracker.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.ui.components.AppCard
import dev.juanpe.habittracker.ui.components.EmptyState
import dev.juanpe.habittracker.ui.components.Flame
import dev.juanpe.habittracker.ui.components.formatted
import dev.juanpe.habittracker.ui.components.frequencyText
import dev.juanpe.habittracker.ui.components.targetText
import dev.juanpe.habittracker.ui.theme.color
import dev.juanpe.habittracker.ui.theme.icon
import dev.juanpe.habittracker.ui.theme.labelRes
import dev.juanpe.habittracker.ui.theme.soft

/** Lista de todos los hábitos, por área, con los archivados al final. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    onOpenHabit: (Long) -> Unit,
    onCreateHabit: (Area?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.habits_title)) }) },
        floatingActionButton = {
            if (!state.isLoading && !state.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = { onCreateHabit(null) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_new_habit)) },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize())

            state.isEmpty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.habits_empty_title),
                    body = stringResource(R.string.habits_empty_body),
                ) {
                    Button(onClick = { onCreateHabit(null) }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_create_habit))
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                // Hueco inferior para que el botón flotante no tape el último elemento.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.sections.forEach { section ->
                    item(key = "area-${section.area.name}") {
                        AreaTitle(section, modifier = Modifier.animateItem().padding(top = 10.dp))
                    }
                    if (section.items.isEmpty()) {
                        item(key = "empty-${section.area.name}") {
                            EmptyAreaRow(
                                area = section.area,
                                onAdd = { onCreateHabit(section.area) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(section.items, key = { it.habit.id }) { item ->
                        HabitListRow(
                            item = item,
                            onClick = { onOpenHabit(item.habit.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (state.archived.isNotEmpty()) {
                    item(key = "archived-header") {
                        ArchivedHeader(
                            count = state.archived.size,
                            expanded = archivedExpanded,
                            onToggle = { archivedExpanded = !archivedExpanded },
                            modifier = Modifier.animateItem().padding(top = 14.dp),
                        )
                    }
                    item(key = "archived-list") {
                        AnimatedVisibility(visible = archivedExpanded, modifier = Modifier.animateItem()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.archived.forEach { habit ->
                                    ArchivedRow(
                                        habit = habit,
                                        onRestore = { viewModel.restore(habit) },
                                        onDelete = { habitToDelete = habit },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    habitToDelete?.let { habit ->
        DeleteHabitDialog(
            habitName = habit.name,
            onConfirm = {
                viewModel.delete(habit)
                habitToDelete = null
            },
            onDismiss = { habitToDelete = null },
        )
    }
}

@Composable
private fun AreaTitle(section: HabitsSection, modifier: Modifier = Modifier) {
    val color = section.area.color
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(color.soft(0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(section.area.icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(stringResource(section.area.labelRes), style = MaterialTheme.typography.titleMedium)
    }
}

/** Invitación a crear el primer hábito de un área que está vacía. */
@Composable
private fun EmptyAreaRow(area: Area, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, onClick = onAdd) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = area.color)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.habits_empty_area, stringResource(area.labelRes)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HabitListRow(item: HabitListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val habit = item.habit
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Franja de color del área a la izquierda.
            Box(Modifier.size(width = 5.dp, height = 38.dp).clip(CircleShape).background(habit.area.color))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = habitSummary(habit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                habit.reminder?.let { time ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = stringResource(R.string.edit_reminder),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = time.formatted(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (item.streak.current > 0) {
                val flameDays = if (item.streak.unit == StreakUnit.WEEKS) item.streak.current * 7 else item.streak.current
                Flame(streak = flameDays, animated = false, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(2.dp))
                Text(item.streak.current.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** "Cada día · 8 vasos" */
@Composable
fun habitSummary(habit: Habit): String {
    val frequency = frequencyText(habit.frequency)
    val target = habit.target ?: return frequency
    return "$frequency · ${targetText(target)}"
}

@Composable
private fun ArchivedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "archivedArrow")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.habits_archived, count),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun ArchivedRow(habit: Habit, onRestore: () -> Unit, onDelete: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(habit.area.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Rounded.Unarchive, contentDescription = stringResource(R.string.action_restore))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Confirmación antes de borrar: es la única acción de la app que no se puede deshacer. */
@Composable
fun DeleteHabitDialog(habitName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_dialog_title)) },
        text = { Text(stringResource(R.string.delete_dialog_body, habitName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
