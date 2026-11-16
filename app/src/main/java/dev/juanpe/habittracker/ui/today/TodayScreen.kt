package dev.juanpe.habittracker.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.logic.GlobalStreakEngine
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.ui.components.AnimatedBar
import dev.juanpe.habittracker.ui.components.AppCard
import dev.juanpe.habittracker.ui.components.EmptyState
import dev.juanpe.habittracker.ui.components.Flame
import dev.juanpe.habittracker.ui.components.FreezeTokens
import dev.juanpe.habittracker.ui.components.HabitCheckButton
import dev.juanpe.habittracker.ui.components.ProgressRing
import dev.juanpe.habittracker.ui.components.amountText
import dev.juanpe.habittracker.ui.components.longLabel
import dev.juanpe.habittracker.ui.components.rememberAppHaptics
import dev.juanpe.habittracker.ui.components.rememberNotificationPermissionState
import dev.juanpe.habittracker.ui.components.streakText
import dev.juanpe.habittracker.ui.theme.AppTheme
import dev.juanpe.habittracker.ui.theme.color
import dev.juanpe.habittracker.ui.theme.icon
import dev.juanpe.habittracker.ui.theme.labelRes
import dev.juanpe.habittracker.ui.theme.soft
import java.time.LocalTime

/**
 * Pantalla principal: los hábitos de hoy agrupados por área y el anillo del día.
 *
 * Sigue el flujo unidireccional: el estado baja desde el ViewModel
 * ([TodayUiState]) y los eventos suben como llamadas a sus funciones.
 */
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenHabit: (Long) -> Unit,
    onCreateHabit: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val xpPop by viewModel.xpPop.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val notifications = rememberNotificationPermissionState()

    // Id del hábito cuyo panel de cantidad está abierto (null = cerrado).
    var amountSheetHabitId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") { TodayHeader(state) }

        item(key = "hero") { DayHeroCard(state, modifier = Modifier.padding(top = 4.dp)) }

        if (state.yesterdayNote != YesterdayNote.NONE) {
            item(key = "yesterday") {
                YesterdayCard(state.yesterdayNote, modifier = Modifier.animateItem())
            }
        }

        if (state.usesNotifications && !notifications.granted && !state.notificationBannerDismissed) {
            item(key = "notifications") {
                NotificationBanner(
                    onEnable = notifications.request,
                    onDismiss = viewModel::dismissNotificationBanner,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        when {
            !state.hasHabits -> item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.today_empty_title),
                    body = stringResource(R.string.today_empty_body),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Button(onClick = onCreateHabit) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_create_habit))
                    }
                }
            }

            state.sections.isEmpty() -> item(key = "rest") {
                EmptyState(
                    title = stringResource(R.string.today_rest_title),
                    body = stringResource(R.string.today_rest_body),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            else -> state.sections.forEach { section ->
                item(key = "area-${section.area.name}") {
                    AreaHeader(section, modifier = Modifier.animateItem().padding(top = 10.dp))
                }
                items(section.habits, key = { it.habit.id }) { item ->
                    HabitRow(
                        item = item,
                        xpPop = xpPop?.takeIf { it.habitId == item.habit.id },
                        onToggle = {
                            val next = item.habit.amountAfterTap(item.amount)
                            when {
                                next >= item.habit.goal -> haptics.success()
                                next == 0 -> haptics.undo()
                                else -> haptics.tick()
                            }
                            viewModel.onHabitTapped(item)
                        },
                        // La pulsación larga solo tiene sentido si hay una cantidad que ajustar.
                        onLongPress = if (item.habit.target != null) {
                            {
                                haptics.strong()
                                amountSheetHabitId = item.habit.id
                            }
                        } else {
                            null
                        },
                        onOpen = { onOpenHabit(item.habit.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    // El panel se alimenta del estado actual, no de una copia: si cambia el hábito, se actualiza.
    val sheetHabit = state.sections.flatMap { it.habits }.firstOrNull { it.habit.id == amountSheetHabitId }
    if (sheetHabit != null) {
        AmountSheet(
            item = sheetHabit,
            onDismiss = { amountSheetHabitId = null },
            onSave = { amount ->
                if (amount >= sheetHabit.habit.goal && !sheetHabit.done) haptics.success()
                viewModel.setAmount(sheetHabit.habit.id, amount)
                amountSheetHabitId = null
            },
        )
    }
}

// --- Cabecera ------------------------------------------------------------------------

@Composable
private fun TodayHeader(state: TodayUiState) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 6..12 -> R.string.greeting_morning
        in 13..20 -> R.string.greeting_afternoon
        else -> R.string.greeting_night
    }
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = state.date.longLabel().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(greeting), style = MaterialTheme.typography.headlineLarge)
    }
}

/** Tarjeta protagonista: anillo del día, mensaje de ánimo, racha y nivel. */
@Composable
private fun DayHeroCard(state: TodayUiState, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val message = when {
        state.ringTotal == 0 -> R.string.today_message_rest
        state.dayComplete -> R.string.today_message_done
        state.ringDone == 0 -> R.string.today_message_start
        state.ringTotal - state.ringDone == 1 -> R.string.today_message_almost
        else -> R.string.today_message_progress
    }
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = state.ringProgress,
                colors = listOf(colors.diet, colors.study, colors.relations, colors.training),
                modifier = Modifier.size(128.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.today_ring_count, state.ringDone, state.ringTotal),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = stringResource(
                            if (state.dayComplete) R.string.today_ring_done else R.string.today_ring_label,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(message), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                // Racha global y comodines.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Flame(streak = state.streak, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = streakText(state.streak, StreakUnit.DAYS),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    FreezeTokens(available = state.freezes, max = GlobalStreakEngine.MAX_FREEZES, iconSize = 18.dp)
                }
                Spacer(Modifier.height(12.dp))

                // Nivel general.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.level_number, state.level.level),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.xp_progress, state.level.xpIntoLevel, state.level.xpForNextLevel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                AnimatedBar(fraction = state.level.fraction, color = colors.gold, height = 8.dp)
            }
        }
    }
}

// --- Avisos --------------------------------------------------------------------------

/** Mensaje amable tras un día fallado: anima a retomar, no reprocha. */
@Composable
private fun YesterdayCard(note: YesterdayNote, modifier: Modifier = Modifier) {
    val (icon, tint, text) = when (note) {
        YesterdayNote.FROZEN -> Triple(Icons.Rounded.Shield, AppTheme.colors.freeze, R.string.today_yesterday_frozen)
        else -> Triple(Icons.Rounded.Replay, MaterialTheme.colorScheme.primary, R.string.today_yesterday_missed)
    }
    InfoCard(icon = icon, tint = tint, modifier = modifier) {
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NotificationBanner(onEnable: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    InfoCard(icon = Icons.Rounded.NotificationsActive, tint = MaterialTheme.colorScheme.primary, modifier = modifier) {
        Text(stringResource(R.string.notifications_banner_title), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.notifications_banner_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) }
            TextButton(onClick = onEnable) { Text(stringResource(R.string.action_enable)) }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppCard(modifier = modifier.fillMaxWidth(), color = tint.soft(0.10f)) {
        Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) { content() }
        }
    }
}

// --- Lista de hábitos --------------------------------------------------------------------

@Composable
private fun AreaHeader(section: TodaySection, modifier: Modifier = Modifier) {
    val color = section.area.color
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(color.soft(0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(section.area.icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(section.area.labelRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.today_ring_count, section.doneCount, section.habits.size),
            style = MaterialTheme.typography.labelLarge,
            color = if (section.doneCount == section.habits.size) color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Fila de un hábito. Tocar el check lo marca; tocar el resto abre su detalle.
 * Al cumplirse, la tarjeta se tiñe del color del área.
 */
@Composable
private fun HabitRow(
    item: TodayHabit,
    xpPop: XpPop?,
    onToggle: () -> Unit,
    onLongPress: (() -> Unit)?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = item.habit.area.color
    val containerColor by animateColorAsState(
        targetValue = if (item.done) color.soft(0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(320),
        label = "habitRowColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (item.done) color.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(320),
        label = "habitRowBorder",
    )

    Box(modifier = modifier) {
        Surface(
            onClick = onOpen,
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HabitCheckButton(
                    done = item.done,
                    fraction = item.fraction,
                    color = color,
                    stateLabel = habitStateLabel(item),
                    onClick = onToggle,
                    onLongClick = onLongPress,
                    size = 52.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = habitSubtitle(item)
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (item.streak.current > 0) {
                    Spacer(Modifier.width(8.dp))
                    // Llama quieta: en una lista con muchas filas no compensa animarlas todas.
                    Flame(streak = flameDays(item), animated = false, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(item.streak.current.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        XpPopLabel(pop = xpPop, modifier = Modifier.align(Alignment.TopEnd).padding(end = 18.dp))
    }
}

/** Para el tamaño de la llama, una semana de racha semanal equivale a siete días. */
private fun flameDays(item: TodayHabit): Int =
    if (item.streak.unit == StreakUnit.WEEKS) item.streak.current * 7 else item.streak.current

/** Línea secundaria: "3 / 8 vasos · 2 de 4 esta semana · Opcional hoy". */
@Composable
private fun habitSubtitle(item: TodayHabit): String {
    val parts = mutableListOf<String>()
    item.habit.target?.let { target ->
        parts += if (item.done) {
            amountText(maxOf(item.amount, target.amount), target.unit)
        } else {
            stringResource(R.string.amount_of_target, item.amount, amountText(target.amount, target.unit))
        }
    }
    if (item.isWeekly) {
        parts += stringResource(R.string.today_week_progress, item.weekDone, item.weekTarget)
        if (item.optional && !item.done) parts += stringResource(R.string.today_optional)
    }
    return parts.joinToString(" · ")
}

@Composable
private fun habitStateLabel(item: TodayHabit): String = when {
    item.done -> stringResource(R.string.state_done)
    item.habit.target != null -> stringResource(R.string.today_ring_count, item.amount, item.habit.goal)
    else -> stringResource(R.string.state_pending)
}
