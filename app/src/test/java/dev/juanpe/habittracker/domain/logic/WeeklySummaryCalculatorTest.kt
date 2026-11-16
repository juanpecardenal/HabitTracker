package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Frequency
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Porcentaje de cumplimiento semanal. */
class WeeklySummaryCalculatorTest {

    private val daily = habit()
    private val nextMonday = MONDAY.plusWeeks(1)

    @Test
    fun `una semana pasada cuenta sus siete dias`() {
        val doneDates = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        val week = WeeklySummaryCalculator.habitWeek(daily, MONDAY, today = nextMonday, doneDates)

        assertEquals(WeekCompliance(expected = 7, done = 5), week)
        assertEquals(5f / 7f, week.ratio!!, 0.001f)
    }

    @Test
    fun `en la semana en curso solo cuentan los dias ya pasados`() {
        // Es jueves: lunes y martes hechos, miércoles fallado, jueves aún sin hacer.
        val week = WeeklySummaryCalculator.habitWeek(daily, MONDAY, today = THURSDAY, setOf(MONDAY, TUESDAY))

        assertEquals(WeekCompliance(expected = 3, done = 2), week)
    }

    @Test
    fun `hoy solo entra en la cuenta cuando ya esta hecho`() {
        val week = WeeklySummaryCalculator.habitWeek(daily, MONDAY, today = WEDNESDAY, setOf(MONDAY, TUESDAY, WEDNESDAY))

        assertEquals(WeekCompliance(expected = 3, done = 3), week)
    }

    @Test
    fun `una semana futura no espera nada`() {
        val week = WeeklySummaryCalculator.habitWeek(daily, nextMonday, today = THURSDAY, emptySet())

        assertEquals(WeekCompliance.EMPTY, week)
        assertNull(week.ratio)
    }

    @Test
    fun `los dias concretos solo esperan los dias programados`() {
        val habit = habit(frequency = Frequency.SpecificDays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        val week = WeeklySummaryCalculator.habitWeek(habit, MONDAY, today = nextMonday, setOf(MONDAY, FRIDAY))

        assertEquals(WeekCompliance(expected = 3, done = 2), week)
    }

    @Test
    fun `no se esperan dias anteriores a la creacion del habito`() {
        val habit = habit(createdOn = FRIDAY)
        val week = WeeklySummaryCalculator.habitWeek(habit, MONDAY, today = nextMonday, setOf(FRIDAY, SATURDAY))

        assertEquals(WeekCompliance(expected = 3, done = 2), week)
    }

    // --- Hábitos "X veces por semana" ------------------------------------------------

    private val threeTimes = habit(frequency = Frequency.TimesPerWeek(3))

    @Test
    fun `un habito semanal en una semana pasada espera su objetivo completo`() {
        val week = WeeklySummaryCalculator.habitWeek(threeTimes, MONDAY, today = nextMonday, setOf(MONDAY, THURSDAY))

        assertEquals(WeekCompliance(expected = 3, done = 2), week)
    }

    @Test
    fun `un habito semanal no penaliza mientras aun se puede cumplir`() {
        // Martes sin haber hecho nada: quedan 6 días para 3 veces. Nada que reprochar.
        val week = WeeklySummaryCalculator.habitWeek(threeTimes, MONDAY, today = TUESDAY, emptySet())

        assertEquals(0, week.expected)
        assertNull(week.ratio)
    }

    @Test
    fun `adelantar veces de un habito semanal cuenta como cumplimiento`() {
        val week = WeeklySummaryCalculator.habitWeek(threeTimes, MONDAY, today = TUESDAY, setOf(MONDAY, TUESDAY))

        assertEquals(WeekCompliance(expected = 2, done = 2), week)
    }

    @Test
    fun `un habito semanal empieza a penalizar cuando ya no llega`() {
        // Sábado sin hacer nada: quedan 2 oportunidades para 3 veces, así que 1 ya se ha perdido.
        val week = WeeklySummaryCalculator.habitWeek(threeTimes, MONDAY, today = SATURDAY, emptySet())

        assertEquals(WeekCompliance(expected = 1, done = 0), week)
    }

    @Test
    fun `hacer de mas un habito semanal no pasa del cien por cien`() {
        val doneDates = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        val week = WeeklySummaryCalculator.habitWeek(threeTimes, MONDAY, today = nextMonday, doneDates)

        assertEquals(WeekCompliance(expected = 3, done = 3), week)
        assertEquals(1f, week.ratio!!, 0.001f)
    }

    // --- Resumen por área ----------------------------------------------------------------

    @Test
    fun `el resumen por area suma los habitos de cada area`() {
        val habits = listOf(
            habit(id = 1, area = Area.DIET),
            habit(id = 2, area = Area.DIET),
            habit(id = 3, area = Area.STUDY),
        )
        val completions = daysBetween(MONDAY, SUNDAY).map { done(1, it) }.toList() + // dieta: 7 de 7
            listOf(done(2, MONDAY)) + // dieta: 1 de 7
            listOf(done(3, MONDAY), done(3, TUESDAY)) // estudio: 2 de 7
        val summary = WeeklySummaryCalculator.byArea(MONDAY, nextMonday, habits, CompletionIndex(completions))

        assertEquals(WeekCompliance(expected = 14, done = 8), summary[Area.DIET])
        assertEquals(WeekCompliance(expected = 7, done = 2), summary[Area.STUDY])
        // Las áreas sin hábitos aparecen, pero sin nada esperado.
        assertEquals(WeekCompliance.EMPTY, summary[Area.TRAINING])
        assertNull(summary.getValue(Area.RELATIONS).ratio)
    }
}
