package dev.juanpe.habittracker.di

import android.content.Context
import dev.juanpe.habittracker.data.SeedData
import dev.juanpe.habittracker.data.local.AppDatabase
import dev.juanpe.habittracker.data.repository.HabitRepository
import dev.juanpe.habittracker.data.repository.ProgressRepository
import dev.juanpe.habittracker.data.repository.SnapshotSource
import dev.juanpe.habittracker.domain.model.Celebration
import dev.juanpe.habittracker.notifications.Notifier
import dev.juanpe.habittracker.notifications.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Inyección de dependencias "a mano": un único objeto, creado en [dev.juanpe.habittracker.HabitApp],
 * que construye y comparte las piezas de la app (base de datos, repositorios...).
 * Es lo mismo que haría Hilt o Koin, pero sin magia: se ve quién crea qué.
 */
class AppContainer(private val context: Context) {

    /** Ámbito para trabajo que no depende de ninguna pantalla (arranque, avisos). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy { AppDatabase.build(context) }

    val habitRepository: HabitRepository by lazy { HabitRepository(database) }
    val progressRepository: ProgressRepository by lazy { ProgressRepository(database) }

    val clock = DayClock()
    val snapshots: SnapshotSource by lazy { SnapshotSource(habitRepository, progressRepository, clock.today) }
    val celebrations = CelebrationQueue()
    val notifier = Notifier(context)
    val reminderScheduler = ReminderScheduler(context)

    /**
     * Pone la app al día: crea los datos iniciales la primera vez y cierra
     * los días pasados pendientes. Es seguro llamarlo muchas veces.
     */
    suspend fun prepareDay() {
        val today = clock.refresh()
        progressRepository.ensureInitialized(today) { SeedData.exampleHabits(context, today) }
        celebrations.add(progressRepository.closePendingDays(today))
    }

    /** Vuelve a programar todas las alarmas (se pierden al reiniciar el móvil). */
    suspend fun rescheduleReminders() {
        habitRepository.getHabits().forEach(reminderScheduler::scheduleHabit)
        progressRepository.getProfile()?.let {
            reminderScheduler.scheduleEvening(it.eveningReminderEnabled, it.eveningReminderTime)
        }
    }
}

/**
 * Fuente única del "día de hoy". Las pantallas lo observan como Flow, así que
 * si la app sigue abierta al pasar la medianoche todo se recalcula solo.
 */
class DayClock {
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** Comprueba si ha cambiado el día y devuelve la fecha actual. */
    fun refresh(): LocalDate = LocalDate.now().also { _today.value = it }
}

/** Cola de celebraciones pendientes; la UI muestra la primera y la descarta al cerrarla. */
class CelebrationQueue {
    private val _pending = MutableStateFlow<List<Celebration>>(emptyList())
    val pending: StateFlow<List<Celebration>> = _pending.asStateFlow()

    fun add(celebrations: List<Celebration>) {
        if (celebrations.isNotEmpty()) _pending.update { it + celebrations }
    }

    /** Quita una celebración ya vista. Si se llama dos veces con la misma, la segunda no hace nada. */
    fun dismiss(celebration: Celebration) = _pending.update { it - celebration }
}
