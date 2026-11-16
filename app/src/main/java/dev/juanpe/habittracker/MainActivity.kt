package dev.juanpe.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.juanpe.habittracker.ui.navigation.HabitTrackerApp
import dev.juanpe.habittracker.ui.theme.HabitTrackerTheme
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Única Activity de la app: toda la interfaz es Compose y la navegación va por dentro. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitTrackerTheme {
                HabitTrackerApp()
            }
        }
        watchForDayChange()
    }

    /**
     * Cada vez que la app vuelve a primer plano, y una vez por minuto mientras
     * esté abierta, comprueba si ha cambiado el día. Si es así, cierra el día
     * anterior y las pantallas pasan solas al nuevo.
     */
    private fun watchForDayChange() {
        val container = (application as HabitApp).container
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    if (LocalDate.now() != container.clock.today.value) container.prepareDay()
                    delay(60_000)
                }
            }
        }
    }
}
