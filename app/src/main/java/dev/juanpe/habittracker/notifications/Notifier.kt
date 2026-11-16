package dev.juanpe.habittracker.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.juanpe.habittracker.MainActivity
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate

/** Crea los canales y muestra las notificaciones de la app. */
class Notifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** Los canales son obligatorios desde Android 8. Crearlos varias veces no hace daño. */
    fun createChannels() {
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminders_description) }

        val streak = NotificationChannel(
            CHANNEL_STREAK,
            context.getString(R.string.channel_streak_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_streak_description) }

        manager.createNotificationChannel(reminders)
        manager.createNotificationChannel(streak)
    }

    /** false si falta el permiso (Android 13+) o el usuario las ha desactivado. */
    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    fun showHabitReminder(habit: Habit) {
        // Se va rotando el mensaje para que no sea siempre el mismo.
        val messages = context.resources.getStringArray(R.array.reminder_messages)
        val message = messages[((habit.id + LocalDate.now().dayOfYear) % messages.size).toInt()]
        show(
            id = habit.id.toInt(),
            channel = CHANNEL_REMINDERS,
            title = habit.name,
            text = message,
        )
    }

    /** Aviso de la tarde: quedan hábitos por hacer. Tono de ánimo, nunca de reproche. */
    fun showStreakAtRisk(streak: Int, pending: Int, freezes: Int) {
        val resources = context.resources
        val title = if (streak > 0) {
            resources.getQuantityString(R.plurals.notification_streak_title, streak, streak)
        } else {
            context.getString(R.string.notification_evening_title_no_streak)
        }
        var text = resources.getQuantityString(R.plurals.notification_streak_text, pending, pending)
        if (streak > 0 && freezes > 0) {
            text += " " + context.getString(R.string.notification_streak_freeze_hint)
        }
        show(id = EVENING_NOTIFICATION_ID, channel = CHANNEL_STREAK, title = title, text = text)
    }

    @SuppressLint("MissingPermission") // Se comprueba con canNotify() justo antes.
    private fun show(id: Int, channel: String, title: String, text: String) {
        if (!canNotify()) return

        // Al tocar la notificación se abre la app.
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL_REMINDERS = "habit_reminders"
        const val CHANNEL_STREAK = "streak_at_risk"
        const val EVENING_NOTIFICATION_ID = -1
    }
}
