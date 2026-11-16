package dev.juanpe.habittracker

import android.app.Application
import dev.juanpe.habittracker.di.AppContainer
import kotlinx.coroutines.launch

/**
 * Punto de entrada del proceso. Crea el contenedor de dependencias y deja
 * la app al día en segundo plano (datos iniciales, cierre de días, alarmas).
 */
class HabitApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannels()
        container.appScope.launch {
            container.prepareDay()
            container.rescheduleReminders()
        }
    }
}
