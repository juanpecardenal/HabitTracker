package dev.juanpe.habittracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.data.repository.ProgressRepository
import dev.juanpe.habittracker.notifications.ReminderScheduler
import dev.juanpe.habittracker.ui.components.AppCard
import dev.juanpe.habittracker.ui.components.SectionTitle
import dev.juanpe.habittracker.ui.components.formatted
import dev.juanpe.habittracker.ui.components.rememberNotificationPermissionState
import dev.juanpe.habittracker.ui.habits.TimePickerDialog
import dev.juanpe.habittracker.ui.theme.AppTheme
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderTime: LocalTime = LocalTime.of(20, 0),
)

class SettingsViewModel(
    private val progressRepository: ProgressRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = progressRepository.observeProfile()
        .map { SettingsUiState(it.eveningReminderEnabled, it.eveningReminderTime) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setEveningReminderEnabled(enabled: Boolean) = updateEveningReminder(enabled, uiState.value.eveningReminderTime)

    fun setEveningReminderTime(time: LocalTime) = updateEveningReminder(uiState.value.eveningReminderEnabled, time)

    /** Guarda el ajuste y reprograma (o cancela) la alarma de la tarde. */
    private fun updateEveningReminder(enabled: Boolean, time: LocalTime) {
        viewModelScope.launch {
            progressRepository.updateProfile {
                it.copy(eveningReminderEnabled = enabled, eveningReminderTime = time)
            }
            reminderScheduler.scheduleEvening(enabled, time)
        }
    }
}

/** Ajustes: el aviso de la tarde y una explicación de cómo funciona el sistema de juego. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications = rememberNotificationPermissionState()
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_reminders_title), Modifier.padding(start = 4.dp))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_evening_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.settings_evening_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = state.eveningReminderEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setEveningReminderEnabled(enabled)
                            if (enabled && !notifications.granted) notifications.request()
                        },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.eveningReminderEnabled) { showTimePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_evening_time),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = state.eveningReminderTime.formatted(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.eveningReminderEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
                if (!notifications.granted) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                        Text(
                            text = stringResource(R.string.notifications_disabled_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = notifications.request) {
                            Text(stringResource(R.string.action_enable_notifications))
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_how_title), Modifier.padding(start = 4.dp, top = 12.dp))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HowItWorksRow(Icons.Rounded.Bolt, AppTheme.colors.gold, R.string.settings_how_xp_title, R.string.settings_how_xp_body)
                    HowItWorksRow(Icons.Rounded.TaskAlt, AppTheme.colors.diet, R.string.settings_how_day_title, R.string.settings_how_day_body)
                    HowItWorksRow(Icons.Rounded.LocalFireDepartment, AppTheme.colors.flameOuter, R.string.settings_how_streak_title, R.string.settings_how_streak_body)
                    HowItWorksRow(Icons.Rounded.Shield, AppTheme.colors.freeze, R.string.settings_how_freeze_title, R.string.settings_how_freeze_body)
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initial = state.eveningReminderTime,
            onConfirm = {
                viewModel.setEveningReminderTime(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun HowItWorksRow(icon: ImageVector, tint: Color, title: Int, body: Int) {
    Row {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
