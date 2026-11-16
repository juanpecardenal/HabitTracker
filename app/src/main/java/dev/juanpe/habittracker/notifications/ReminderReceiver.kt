package dev.juanpe.habittracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.juanpe.habittracker.HabitApp
import dev.juanpe.habittracker.di.AppContainer
import dev.juanpe.habittracker.domain.logic.DayDemand
import dev.juanpe.habittracker.domain.logic.Schedule
import dev.juanpe.habittracker.domain.logic.startOfWeek
import kotlinx.coroutines.launch

/**
 * Recibe las alarmas de [ReminderScheduler]. Antes de avisar consulta la base
 * de datos: si el hábito ya está hecho (o hoy no toca), no molesta.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as HabitApp).container
        // goAsync() mantiene vivo el receiver mientras la corrutina consulta la base de datos.
        val pendingResult = goAsync()
        container.appScope.launch {
            try {
                container.prepareDay()
                when (intent.action) {
                    ACTION_HABIT -> remindHabit(container, intent.getLongExtra(EXTRA_HABIT_ID, -1))
                    ACTION_EVENING -> remindEvening(container)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun remindHabit(container: AppContainer, habitId: Long) {
        val habit = container.habitRepository.getHabit(habitId) ?: return
        container.reminderScheduler.scheduleHabit(habit) // la de mañana

        val today = container.clock.today.value
        val doneDates = container.habitRepository.getCompletionsFor(habitId)
            .filter { it.done }
            .map { it.date }
            .toSet()
        if (today in doneDates) return

        when (Schedule.demandOn(habit, today, doneDates)) {
            DayDemand.NOT_SCHEDULED -> return
            DayDemand.OPTIONAL -> {
                // Semanal con el objetivo de la semana ya cumplido: no hace falta avisar.
                val weekStart = today.startOfWeek()
                val doneThisWeek = doneDates.count { !it.isBefore(weekStart) }
                if (doneThisWeek >= Schedule.weeklyTarget(habit, weekStart)) return
            }
            DayDemand.REQUIRED -> Unit
        }
        container.notifier.showHabitReminder(habit)
    }

    private suspend fun remindEvening(container: AppContainer) {
        val profile = container.progressRepository.getProfile() ?: return
        container.reminderScheduler.scheduleEvening(profile.eveningReminderEnabled, profile.eveningReminderTime)

        val outcome = container.progressRepository.dayOutcome(container.clock.today.value)
        if (outcome.pending > 0) {
            container.notifier.showStreakAtRisk(
                streak = profile.streak.current,
                pending = outcome.pending,
                freezes = profile.streak.freezes,
            )
        }
    }

    companion object {
        const val ACTION_HABIT = "dev.juanpe.habittracker.REMIND_HABIT"
        const val ACTION_EVENING = "dev.juanpe.habittracker.REMIND_EVENING"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}

/** Al reiniciar el móvil (o cambiar la hora) las alarmas se pierden: se reprograman. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Solo se atienden los avisos del sistema declarados en el manifest.
        if (intent.action !in HANDLED_ACTIONS) return

        val container = (context.applicationContext as HabitApp).container
        val pendingResult = goAsync()
        container.appScope.launch {
            try {
                container.prepareDay()
                container.rescheduleReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
