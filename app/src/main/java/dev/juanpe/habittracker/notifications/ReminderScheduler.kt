package dev.juanpe.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Programa los recordatorios con AlarmManager.
 *
 * Cada alarma suena una vez; al saltar, [ReminderReceiver] la vuelve a programar
 * para el día siguiente.
 *
 * Se usan alarmas exactas para que el aviso llegue a la hora elegida (las
 * "inexactas" pueden retrasarse bastante desde Android 12). Si el sistema no
 * concede ese permiso, se recurre a una inexacta: el aviso llega igual, algo más tarde.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Programa (o cancela, si no tiene hora o está archivado) el recordatorio de un hábito. */
    fun scheduleHabit(habit: Habit) {
        val time = habit.reminder
        if (time == null || habit.isArchived) {
            cancelHabit(habit.id)
        } else {
            scheduleNext(habitIntent(habit.id), time)
        }
    }

    fun cancelHabit(habitId: Long) = alarmManager.cancel(habitIntent(habitId))

    /** Aviso de la tarde de "racha en riesgo". */
    fun scheduleEvening(enabled: Boolean, time: LocalTime) {
        if (enabled) scheduleNext(eveningIntent(), time) else alarmManager.cancel(eveningIntent())
    }

    /** Programa la alarma para la próxima vez que el reloj marque [time] (hoy o mañana). */
    private fun scheduleNext(intent: PendingIntent, time: LocalTime) {
        val now = LocalDateTime.now()
        var trigger = now.toLocalDate().atTime(time)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
        }
    }

    /** Antes de Android 12 no hace falta permiso; después hay que preguntarle al sistema. */
    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun habitIntent(habitId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_HABIT)
            .putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
        // El requestCode distingue las alarmas de cada hábito entre sí.
        return PendingIntent.getBroadcast(context, habitId.toInt(), intent, FLAGS)
    }

    private fun eveningIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_EVENING)
        return PendingIntent.getBroadcast(context, EVENING_REQUEST_CODE, intent, FLAGS)
    }

    private companion object {
        const val EVENING_REQUEST_CODE = -1
        const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
