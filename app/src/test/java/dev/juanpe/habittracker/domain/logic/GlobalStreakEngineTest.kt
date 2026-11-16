package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Completion
import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Racha global de días completos, comodines y evaluación del día. */
class GlobalStreakEngineTest {

    /** Aplica varios días seguidos y devuelve el estado final. */
    private fun StreakState.after(vararg days: DayStatus): StreakState =
        days.fold(this) { state, status -> GlobalStreakEngine.applyDay(state, status).state }

    private fun completeDays(count: Int) = Array(count) { DayStatus.COMPLETE }

    // --- Racha ------------------------------------------------------------------

    @Test
    fun `cada dia completo suma uno a la racha y actualiza la mejor`() {
        val state = StreakState().after(*completeDays(3))
        assertEquals(3, state.current)
        assertEquals(3, state.best)
    }

    @Test
    fun `un dia de descanso no suma ni rompe`() {
        val state = StreakState().after(DayStatus.COMPLETE, DayStatus.REST, DayStatus.COMPLETE)
        assertEquals(2, state.current)
    }

    @Test
    fun `la mejor racha se conserva al romperse la actual`() {
        val state = StreakState(freezes = 0).after(*completeDays(4), DayStatus.MISSED, DayStatus.COMPLETE)
        assertEquals(1, state.current)
        assertEquals(4, state.best)
    }

    // --- Comodines ----------------------------------------------------------------

    @Test
    fun `se empieza con un comodin de regalo`() {
        assertEquals(1, StreakState().freezes)
    }

    @Test
    fun `un fallo con racha y comodin gasta el comodin y conserva la racha`() {
        val before = StreakState().after(*completeDays(3))
        val result = GlobalStreakEngine.applyDay(before, DayStatus.MISSED)

        assertEquals(DayStatus.FROZEN, result.status)
        assertEquals(3, result.state.current)
        assertEquals(0, result.state.freezes)
        assertEquals(1, result.state.freezesUsed)
    }

    @Test
    fun `un fallo sin comodines rompe la racha`() {
        val before = StreakState(freezes = 0).after(*completeDays(3))
        val result = GlobalStreakEngine.applyDay(before, DayStatus.MISSED)

        assertEquals(DayStatus.MISSED, result.status)
        assertEquals(0, result.state.current)
    }

    @Test
    fun `no se gasta un comodin si no hay racha que proteger`() {
        val result = GlobalStreakEngine.applyDay(StreakState(), DayStatus.MISSED)

        assertEquals(DayStatus.MISSED, result.status)
        assertEquals(1, result.state.freezes)
    }

    @Test
    fun `dos fallos seguidos con un solo comodin protegen el primero y rompen con el segundo`() {
        val state = StreakState().after(*completeDays(3), DayStatus.MISSED, DayStatus.MISSED)
        assertEquals(0, state.current)
        assertEquals(0, state.freezes)
        assertEquals(1, state.freezesUsed)
    }

    @Test
    fun `siete dias completos seguidos ganan un comodin`() {
        val sixDays = StreakState(freezes = 0).after(*completeDays(6))
        assertEquals(0, sixDays.freezes)

        val seventh = GlobalStreakEngine.applyDay(sixDays, DayStatus.COMPLETE)
        assertTrue(seventh.freezeEarned)
        assertEquals(1, seventh.state.freezes)
        assertEquals(0, seventh.state.freezeProgress)
    }

    @Test
    fun `no se acumulan mas comodines que el maximo`() {
        val state = StreakState(freezes = GlobalStreakEngine.MAX_FREEZES).after(*completeDays(6))
        val result = GlobalStreakEngine.applyDay(state, DayStatus.COMPLETE)

        assertFalse(result.freezeEarned)
        assertEquals(GlobalStreakEngine.MAX_FREEZES, result.state.freezes)
    }

    @Test
    fun `un dia protegido no reinicia el progreso hacia el siguiente comodin`() {
        val state = StreakState().after(*completeDays(3), DayStatus.MISSED, *completeDays(3))
        // 3 + 3 días completos = 6 de progreso; el día congelado no lo borró.
        assertEquals(6, state.freezeProgress)
        assertEquals(6, state.current)
    }

    @Test
    fun `romper la racha reinicia el progreso hacia el comodin`() {
        val state = StreakState(freezes = 0).after(*completeDays(5), DayStatus.MISSED)
        assertEquals(0, state.freezeProgress)
    }

    // --- Regresos -------------------------------------------------------------------

    @Test
    fun `completar un dia tras romper la racha cuenta como regreso`() {
        val state = StreakState(freezes = 0).after(DayStatus.COMPLETE, DayStatus.MISSED, DayStatus.COMPLETE)
        assertEquals(1, state.comebacks)
        assertFalse(state.pendingComeback)
    }

    @Test
    fun `un regreso solo se cuenta una vez`() {
        val state = StreakState(freezes = 0).after(DayStatus.MISSED, DayStatus.COMPLETE, DayStatus.COMPLETE)
        assertEquals(1, state.comebacks)
    }

    @Test
    fun `un dia salvado por comodin no cuenta como regreso`() {
        val state = StreakState().after(DayStatus.COMPLETE, DayStatus.MISSED, DayStatus.COMPLETE)
        assertEquals(0, state.comebacks)
    }

    // --- Evaluación del día (DayEvaluator) ----------------------------------------------

    @Test
    fun `el dia esta completo cuando se hacen todos los obligatorios`() {
        val habits = listOf(habit(id = 1), habit(id = 2))
        val allDone = CompletionIndex(listOf(done(1, MONDAY), done(2, MONDAY)))
        val oneDone = CompletionIndex(listOf(done(1, MONDAY)))

        assertEquals(DayStatus.COMPLETE, DayEvaluator.evaluate(MONDAY, habits, allDone).status)

        val partial = DayEvaluator.evaluate(MONDAY, habits, oneDone)
        assertEquals(DayStatus.MISSED, partial.status)
        assertEquals(1, partial.pending)
    }

    @Test
    fun `un habito semanal con margen no impide completar el dia`() {
        val habits = listOf(habit(id = 1), habit(id = 2, frequency = Frequency.TimesPerWeek(2)))
        val outcome = DayEvaluator.evaluate(MONDAY, habits, indexOf(1, MONDAY))

        assertEquals(1, outcome.required)
        assertTrue(outcome.isComplete)
    }

    @Test
    fun `un dia sin nada programado es de descanso`() {
        val onlyWeekly = listOf(habit(frequency = Frequency.TimesPerWeek(2)))
        val outcome = DayEvaluator.evaluate(MONDAY, onlyWeekly, CompletionIndex(emptyList()))

        assertEquals(0, outcome.required)
        assertEquals(DayStatus.REST, outcome.status)
    }

    @Test
    fun `hacer un opcional en un dia sin obligatorios lo convierte en dia completo`() {
        val onlyWeekly = listOf(habit(frequency = Frequency.TimesPerWeek(2)))
        val outcome = DayEvaluator.evaluate(MONDAY, onlyWeekly, indexOf(1, MONDAY))

        assertEquals(DayStatus.COMPLETE, outcome.status)
    }

    @Test
    fun `los habitos archivados no cuentan para el dia`() {
        val habits = listOf(habit(id = 1), habit(id = 2, archivedOn = MONDAY))
        val outcome = DayEvaluator.evaluate(MONDAY, habits, indexOf(1, MONDAY))

        assertEquals(1, outcome.required)
        assertTrue(outcome.isComplete)
    }

    @Test
    fun `un registro a medias no cuenta como hecho`() {
        val halfDone = CompletionIndex(
            listOf(Completion(1, MONDAY, amount = 3, done = false)),
        )
        val outcome = DayEvaluator.evaluate(MONDAY, listOf(habit()), halfDone)

        assertEquals(DayStatus.MISSED, outcome.status)
        assertEquals(3, halfDone.amount(1, MONDAY))
    }
}
