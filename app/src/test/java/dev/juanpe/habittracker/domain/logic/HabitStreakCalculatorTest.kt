package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Frequency
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

/** Rachas de cada hábito. */
class HabitStreakCalculatorTest {

    private val daily = habit()

    @Test
    fun `sin registros la racha es cero`() {
        val streak = HabitStreakCalculator.calculate(daily, emptySet(), THURSDAY)
        assertEquals(0, streak.current)
        assertEquals(0, streak.best)
        assertEquals(StreakUnit.DAYS, streak.unit)
    }

    @Test
    fun `dias seguidos hasta hoy suman racha`() {
        val streak = HabitStreakCalculator.calculate(daily, setOf(TUESDAY, WEDNESDAY, THURSDAY), THURSDAY)
        assertEquals(3, streak.current)
        assertEquals(3, streak.best)
    }

    @Test
    fun `hoy sin hacer todavia no rompe la racha`() {
        val streak = HabitStreakCalculator.calculate(daily, setOf(TUESDAY, WEDNESDAY), THURSDAY)
        assertEquals(2, streak.current)
    }

    @Test
    fun `un dia fallado reinicia la racha actual pero conserva la mejor`() {
        // Lunes a miércoles hechos, jueves fallado, viernes y sábado hechos.
        val doneDates = setOf(MONDAY, TUESDAY, WEDNESDAY, FRIDAY, SATURDAY)
        val streak = HabitStreakCalculator.calculate(daily, doneDates, SATURDAY)
        assertEquals(2, streak.current)
        assertEquals(3, streak.best)
    }

    @Test
    fun `ayer fallado deja la racha a cero`() {
        val streak = HabitStreakCalculator.calculate(daily, setOf(MONDAY, TUESDAY), THURSDAY)
        assertEquals(0, streak.current)
        assertEquals(2, streak.best)
    }

    @Test
    fun `un dia protegido por comodin no rompe la racha ni suma`() {
        val doneDates = setOf(MONDAY, TUESDAY, THURSDAY)
        val streak = HabitStreakCalculator.calculate(daily, doneDates, THURSDAY, protectedDates = setOf(WEDNESDAY))
        assertEquals(3, streak.current)
    }

    @Test
    fun `en dias concretos los dias que no toca se saltan`() {
        val habit = habit(frequency = Frequency.SpecificDays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        // Lunes, miércoles y viernes hechos; martes y jueves no tocaban.
        val streak = HabitStreakCalculator.calculate(habit, setOf(MONDAY, WEDNESDAY, FRIDAY), SATURDAY)
        assertEquals(3, streak.current)
    }

    @Test
    fun `en dias concretos fallar un dia programado rompe la racha`() {
        val habit = habit(frequency = Frequency.SpecificDays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        val streak = HabitStreakCalculator.calculate(habit, setOf(MONDAY, FRIDAY), SATURDAY)
        assertEquals(1, streak.current)
        assertEquals(1, streak.best)
    }

    @Test
    fun `la racha no cuenta dias anteriores a la creacion`() {
        val habit = habit(createdOn = WEDNESDAY)
        val streak = HabitStreakCalculator.calculate(habit, setOf(WEDNESDAY, THURSDAY), THURSDAY)
        assertEquals(2, streak.current)
    }

    // --- Hábitos semanales: la racha se cuenta en semanas ---------------------------

    private val twiceAWeek = habit(frequency = Frequency.TimesPerWeek(2))

    @Test
    fun `la racha semanal cuenta semanas con el objetivo cumplido`() {
        val doneDates = setOf(
            MONDAY.minusWeeks(2), TUESDAY.minusWeeks(2), // hace dos semanas: 2 de 2
            MONDAY.minusWeeks(1), FRIDAY.minusWeeks(1), // semana pasada: 2 de 2
            MONDAY, // esta semana: 1 de 2 (en curso)
        )
        val streak = HabitStreakCalculator.calculate(twiceAWeek, doneDates, WEDNESDAY)
        assertEquals(StreakUnit.WEEKS, streak.unit)
        // La semana en curso aún no suma, pero tampoco rompe.
        assertEquals(2, streak.current)
    }

    @Test
    fun `la semana en curso suma en cuanto se alcanza el objetivo`() {
        val doneDates = setOf(MONDAY.minusWeeks(1), FRIDAY.minusWeeks(1), MONDAY, TUESDAY)
        val streak = HabitStreakCalculator.calculate(twiceAWeek, doneDates, WEDNESDAY)
        assertEquals(2, streak.current)
        assertEquals(2, streak.best)
    }

    @Test
    fun `una semana pasada sin cumplir rompe la racha semanal`() {
        val doneDates = setOf(
            MONDAY.minusWeeks(3), TUESDAY.minusWeeks(3), // cumplida
            MONDAY.minusWeeks(2), TUESDAY.minusWeeks(2), // cumplida
            MONDAY.minusWeeks(1), // solo 1 de 2: rota
            MONDAY, TUESDAY, // cumplida
        )
        // El hábito existe desde hace 3 semanas para que las anteriores no cuenten como fallos.
        val habit = twiceAWeek.copy(createdOn = MONDAY.minusWeeks(3))
        val streak = HabitStreakCalculator.calculate(habit, doneDates, WEDNESDAY)
        assertEquals(1, streak.current)
        assertEquals(2, streak.best)
    }
}
