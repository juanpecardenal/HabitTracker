package dev.juanpe.habittracker.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.TargetUnit
import dev.juanpe.habittracker.ui.components.formatted
import dev.juanpe.habittracker.ui.components.rememberNotificationPermissionState
import dev.juanpe.habittracker.ui.components.unitName
import dev.juanpe.habittracker.ui.theme.color
import dev.juanpe.habittracker.ui.theme.icon
import dev.juanpe.habittracker.ui.theme.labelRes
import dev.juanpe.habittracker.ui.theme.soft
import java.time.DayOfWeek
import java.time.LocalTime

/** Formulario para crear un hábito nuevo o editar uno existente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(viewModel: EditHabitViewModel, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications = rememberNotificationPermissionState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Cuando el ViewModel termina (guardar, archivar o borrar), se cierra la pantalla.
    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEditing) R.string.edit_title_edit else R.string.edit_title_new))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // --- Nombre ---
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.edit_name_label)) },
                placeholder = { Text(stringResource(R.string.edit_name_placeholder)) },
                isError = state.showNameError,
                supportingText = if (state.showNameError) {
                    { Text(stringResource(R.string.edit_name_error)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            // --- Área ---
            FormLabel(stringResource(R.string.edit_area))
            AreaPicker(selected = state.area, onSelect = viewModel::onAreaChange)

            // --- Frecuencia ---
            FormLabel(stringResource(R.string.edit_frequency))
            FrequencyPicker(state, viewModel)

            // --- Cantidad objetivo (opcional) ---
            SwitchRow(
                title = stringResource(R.string.edit_target),
                subtitle = stringResource(R.string.edit_target_hint),
                checked = state.hasTarget,
                onCheckedChange = viewModel::onHasTargetChange,
            )
            AnimatedVisibility(visible = state.hasTarget) {
                TargetEditor(state, viewModel, accent = state.area.color)
            }

            // --- Recordatorio (opcional) ---
            SwitchRow(
                title = stringResource(R.string.edit_reminder),
                subtitle = stringResource(R.string.edit_reminder_hint),
                checked = state.hasReminder,
                onCheckedChange = { enabled ->
                    viewModel.onHasReminderChange(enabled)
                    // Al activar un recordatorio es el momento natural de pedir el permiso.
                    if (enabled && !notifications.granted) notifications.request()
                },
            )
            AnimatedVisibility(visible = state.hasReminder) {
                Column {
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.reminderTime.formatted())
                    }
                    if (!notifications.granted) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.notifications_disabled_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = notifications.request) {
                            Text(stringResource(R.string.action_enable_notifications))
                        }
                    }
                }
            }

            // --- Guardar ---
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = viewModel::save,
                enabled = state.isTargetValid && state.areDaysValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium)
            }

            // --- Archivar y borrar (solo al editar) ---
            if (state.isEditing) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = viewModel::toggleArchived, modifier = Modifier.weight(1f)) {
                        Text(stringResource(if (state.isArchived) R.string.action_restore else R.string.action_archive))
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(
                    text = stringResource(R.string.edit_archive_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initial = state.reminderTime,
            onConfirm = {
                viewModel.onReminderTimeChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
    if (showDeleteDialog) {
        DeleteHabitDialog(
            habitName = state.name,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
}

/** Las cuatro áreas en una cuadrícula de 2x2; la elegida se rellena con su color. */
@Composable
private fun AreaPicker(selected: Area, onSelect: (Area) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Area.entries.chunked(2).forEach { rowAreas ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowAreas.forEach { area ->
                    val isSelected = area == selected
                    val color = area.color
                    val container by animateColorAsState(
                        if (isSelected) color else color.soft(0.12f),
                        label = "areaChip",
                    )
                    val content by animateColorAsState(if (isSelected) Color.White else color, label = "areaChipContent")
                    Surface(
                        onClick = { onSelect(area) },
                        selected = isSelected,
                        shape = MaterialTheme.shapes.medium,
                        color = container,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(area.icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(area.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyPicker(state: EditHabitUiState, viewModel: EditHabitViewModel) {
    val kinds = listOf(
        FrequencyKind.DAILY to R.string.edit_frequency_daily,
        FrequencyKind.DAYS to R.string.edit_frequency_days,
        FrequencyKind.WEEKLY to R.string.edit_frequency_weekly,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        kinds.forEachIndexed { index, (kind, label) ->
            SegmentedButton(
                selected = state.frequencyKind == kind,
                onClick = { viewModel.onFrequencyKindChange(kind) },
                shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                icon = {},
            ) {
                Text(stringResource(label), maxLines = 1)
            }
        }
    }

    // Opciones propias de cada tipo de frecuencia.
    AnimatedVisibility(visible = state.frequencyKind == FrequencyKind.DAYS) {
        Column(modifier = Modifier.padding(top = 14.dp)) {
            val initials = stringArrayResource(R.array.weekday_initials)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayOfWeek.entries.forEach { day ->
                    DayToggle(
                        label = initials[day.value - 1],
                        selected = day in state.days,
                        onClick = { viewModel.onDayToggle(day) },
                    )
                }
            }
            if (!state.areDaysValid) {
                Text(
                    text = stringResource(R.string.edit_days_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
    AnimatedVisibility(visible = state.frequencyKind == FrequencyKind.WEEKLY) {
        Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { viewModel.onTimesPerWeekChange(state.timesPerWeek - 1) },
                enabled = state.timesPerWeek > 1,
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.action_decrease))
            }
            Text(
                text = pluralStringResource(R.plurals.frequency_times_per_week, state.timesPerWeek, state.timesPerWeek),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FilledTonalIconButton(
                onClick = { viewModel.onTimesPerWeekChange(state.timesPerWeek + 1) },
                enabled = state.timesPerWeek < 7,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_increase))
            }
        }
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "dayToggle",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetEditor(state: EditHabitUiState, viewModel: EditHabitViewModel, accent: Color) {
    Column {
        OutlinedTextField(
            value = state.targetAmount,
            onValueChange = viewModel::onTargetAmountChange,
            label = { Text(stringResource(R.string.edit_target_amount)) },
            isError = !state.isTargetValid && state.targetAmount.isNotEmpty(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(150.dp),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TargetUnit.entries.forEach { unit ->
                FilterChip(
                    selected = state.targetUnit == unit,
                    onClick = { viewModel.onTargetUnitChange(unit) },
                    label = { Text(unitName(unit)) },
                )
            }
        }
        val amount = state.parsedTarget
        if (amount != null) {
            Text(
                text = stringResource(
                    if (amount <= Habit.STEP_BY_STEP_LIMIT) R.string.edit_target_steps_hint else R.string.edit_target_full_hint,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
