package dev.juanpe.habittracker.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.juanpe.habittracker.data.local.AppDatabase
import dev.juanpe.habittracker.domain.logic.MONDAY
import dev.juanpe.habittracker.domain.logic.TUESDAY
import dev.juanpe.habittracker.domain.logic.WEDNESDAY
import dev.juanpe.habittracker.domain.logic.habit
import dev.juanpe.habittracker.domain.model.Achievement
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Celebration
import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Target
import dev.juanpe.habittracker.domain.model.TargetUnit
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de integración del repositorio de progreso contra una base de datos
 * Room real (en memoria). Comprueban que las piezas de `domain/logic` quedan
 * bien conectadas: marcar, desmarcar, bonus de día, cierre de días y logros.
 *
 * Usan Robolectric para tener un `Context` de Android sin emulador. Se indica
 * una `Application` vacía para que no arranque la de verdad (HabitApp).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProgressRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var habits: HabitRepository
    private lateinit var progress: ProgressRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habits = HabitRepository(db)
        progress = ProgressRepository(db)
    }

    @After
    fun tearDown() = db.close()

    /** Crea el perfil el lunes con los hábitos indicados (sus ids serán 1, 2, 3...). */
    private suspend fun start(vararg seed: Habit) {
        progress.ensureInitialized(MONDAY) { seed.map { it.copy(id = 0, createdOn = MONDAY) } }
    }

    private suspend fun totalXp(): Int = progress.observeXp().first().total

    private suspend fun streak() = progress.getProfile()!!.streak

    private suspend fun statusOf(date: LocalDate): DayStatus? =
        progress.observeDayRecords().first().firstOrNull { it.date == date }?.status

    // --- Arranque ---------------------------------------------------------------

    @Test
    fun `la primera vez crea el perfil y los habitos de ejemplo y despues no los duplica`() = runBlocking {
        start(habit(), habit())
        start(habit(), habit(), habit()) // segunda llamada: no debe hacer nada

        assertEquals(2, habits.getHabits().size)
        assertEquals(1, streak().freezes)
        assertEquals(0, streak().current)
    }

    // --- Marcar y desmarcar --------------------------------------------------------

    @Test
    fun `cumplir un habito da xp a su area y desmarcarlo la retira`() = runBlocking {
        start(habit(area = Area.STUDY), habit())

        val result = progress.setProgress(habitId = 1, date = MONDAY, newAmount = 1)
        assertTrue(result.completedNow)
        assertEquals(10, result.xpGained)
        assertEquals(10, totalXp())
        assertEquals(10, progress.observeXp().first().byArea[Area.STUDY])

        val undone = progress.setProgress(habitId = 1, date = MONDAY, newAmount = 0)
        assertFalse(undone.completedNow)
        assertEquals(0, totalXp())
    }

    @Test
    fun `marcar dos veces el mismo habito no da xp dos veces`() = runBlocking {
        start(habit(), habit())

        progress.setProgress(1, MONDAY, 1)
        val again = progress.setProgress(1, MONDAY, 1)

        assertFalse(again.completedNow)
        assertEquals(0, again.xpGained)
        assertEquals(10, totalXp())
    }

    @Test
    fun `un habito con cantidad solo se cumple al llegar al objetivo`() = runBlocking {
        start(habit(target = Target(8, TargetUnit.GLASSES)), habit())

        val partial = progress.setProgress(1, MONDAY, 5)
        assertFalse(partial.completedNow)
        assertEquals(0, totalXp())

        val full = progress.setProgress(1, MONDAY, 8)
        assertTrue(full.completedNow)
        assertEquals(10, totalXp())

        // Bajar la cantidad por debajo del objetivo lo deja a medias y retira la XP.
        progress.setProgress(1, MONDAY, 3)
        assertEquals(0, totalXp())
    }

    @Test
    fun `la racha del habito da xp extra al dia siguiente`() = runBlocking {
        start(habit(), habit())

        progress.setProgress(1, MONDAY, 1)
        progress.closePendingDays(TUESDAY)
        val secondDay = progress.setProgress(1, TUESDAY, 1)

        assertEquals(11, secondDay.xpGained)
    }

    // --- Día completo ----------------------------------------------------------------

    @Test
    fun `completar el dia da el bonus y lo celebra una sola vez`() = runBlocking {
        start(habit(), habit())

        val first = progress.setProgress(1, MONDAY, 1)
        assertTrue(first.celebrations.none { it is Celebration.DayComplete })

        val second = progress.setProgress(2, MONDAY, 1)
        val celebration = second.celebrations.filterIsInstance<Celebration.DayComplete>().single()
        assertEquals(1, celebration.streak)
        assertEquals(20, celebration.bonusXp)
        assertEquals(10 + 10 + 20, totalXp())

        // Desmarcar retira el bonus; volver a marcar lo devuelve, pero sin repetir la fiesta.
        progress.setProgress(2, MONDAY, 0)
        assertEquals(10, totalXp())
        val again = progress.setProgress(2, MONDAY, 1)
        assertTrue(again.celebrations.none { it is Celebration.DayComplete })
        assertEquals(40, totalXp())
    }

    @Test
    fun `los primeros logros se desbloquean una sola vez`() = runBlocking {
        start(habit(), habit())

        val first = progress.setProgress(1, MONDAY, 1)
        assertEquals(
            listOf(Achievement.FIRST_STEP),
            first.celebrations.filterIsInstance<Celebration.AchievementUnlocked>().map { it.achievement },
        )

        val second = progress.setProgress(2, MONDAY, 1)
        assertEquals(
            listOf(Achievement.FIRST_FULL_DAY),
            second.celebrations.filterIsInstance<Celebration.AchievementUnlocked>().map { it.achievement },
        )

        // Desmarcar y volver a marcar no vuelve a dar los mismos logros.
        progress.setProgress(1, MONDAY, 0)
        val repeated = progress.setProgress(1, MONDAY, 1)
        assertTrue(repeated.celebrations.none { it is Celebration.AchievementUnlocked })
        assertEquals(
            setOf(Achievement.FIRST_STEP, Achievement.FIRST_FULL_DAY),
            progress.observeUnlockedAchievements().first().keys,
        )
    }

    @Test
    fun `al llegar a 100 xp se celebra la subida al nivel 2`() = runBlocking {
        // Ocho hábitos: 8 x 10 XP + 20 de bonus de día = 100 XP.
        start(*Array(8) { habit() })

        val results = (1L..8L).map { progress.setProgress(it, MONDAY, 1) }

        assertEquals(100, totalXp())
        assertTrue(results.dropLast(1).none { r -> r.celebrations.any { it is Celebration.LevelUp && it.area == null } })
        assertTrue(Celebration.LevelUp(level = 2, area = null) in results.last().celebrations)
        assertEquals(2, progress.getProfile()!!.maxLevelCelebrated)
    }

    // --- Cierre de días: racha global y comodines -----------------------------------------

    @Test
    fun `cerrar un dia completo suma a la racha global`() = runBlocking {
        start(habit())
        progress.setProgress(1, MONDAY, 1)

        progress.closePendingDays(TUESDAY)

        assertEquals(DayStatus.COMPLETE, statusOf(MONDAY))
        assertEquals(1, streak().current)
        assertEquals(MONDAY, progress.getProfile()!!.lastClosedDate)
    }

    @Test
    fun `cerrar dos veces el mismo dia no cuenta doble`() = runBlocking {
        start(habit())
        progress.setProgress(1, MONDAY, 1)

        progress.closePendingDays(TUESDAY)
        progress.closePendingDays(TUESDAY)

        assertEquals(1, streak().current)
    }

    @Test
    fun `un dia fallado con racha gasta el comodin y la racha sigue`() = runBlocking {
        start(habit())
        progress.setProgress(1, MONDAY, 1)

        // El martes no se hace nada; el miércoles se abre la app.
        val celebrations = progress.closePendingDays(WEDNESDAY)

        assertEquals(DayStatus.FROZEN, statusOf(TUESDAY))
        assertEquals(1, streak().current)
        assertEquals(0, streak().freezes)
        // Usar un comodín por primera vez desbloquea su logro.
        assertTrue(Celebration.AchievementUnlocked(Achievement.SAFETY_NET) in celebrations)
    }

    @Test
    fun `tras varios dias sin abrir la app se gasta el comodin y despues se reinicia la racha`() = runBlocking {
        start(habit())
        progress.setProgress(1, MONDAY, 1)

        // Se vuelve a abrir el jueves: martes y miércoles sin hacer.
        val thursday = WEDNESDAY.plusDays(1)
        progress.closePendingDays(thursday)

        assertEquals(DayStatus.FROZEN, statusOf(TUESDAY))
        assertEquals(DayStatus.MISSED, statusOf(WEDNESDAY))
        assertEquals(0, streak().current)
        assertEquals(1, streak().best)

        // Y completar el día de la vuelta cuenta como "regreso".
        val comeback = progress.setProgress(1, thursday, 1)
        assertTrue(Celebration.AchievementUnlocked(Achievement.COMEBACK) in comeback.celebrations)
    }

    @Test
    fun `el dia protegido por comodin no rompe la racha del habito`() = runBlocking {
        start(habit())
        progress.setProgress(1, MONDAY, 1)
        progress.closePendingDays(WEDNESDAY) // el martes queda protegido

        val result = progress.setProgress(1, WEDNESDAY, 1)

        // Racha del hábito = 2 (lunes y miércoles; el martes no rompe): 10 + 1 de bonus.
        assertEquals(11, result.xpGained)
    }

    @Test
    fun `borrar un habito conserva la experiencia ganada con el`() = runBlocking {
        start(habit(), habit())
        progress.setProgress(1, MONDAY, 1)

        habits.delete(1)

        assertEquals(1, habits.getHabits().size)
        assertEquals(10, totalXp())
    }
}
