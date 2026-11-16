package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Target
import dev.juanpe.habittracker.domain.model.TargetUnit
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frecuencias y fechas: qué días toca cada hábito. */
class ScheduleTest {

    // --- Fechas ---------------------------------------------------------------

    @Test
    fun `la semana empieza en lunes y acaba en domingo`() {
        assertEquals(MONDAY, MONDAY.startOfWeek())
        assertEquals(MONDAY, THURSDAY.startOfWeek())
        assertEquals(MONDAY, SUNDAY.startOfWeek())
        assertEquals(SUNDAY, MONDAY.endOfWeek())
    }

    @Test
    fun `la semana se calcula bien al cruzar de anio`() {
        // El 1 de enero de 2027 es viernes: su semana empieza el lunes 28 de diciembre de 2026.
        assertEquals(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 1).startOfWeek())
    }

    @Test
    fun `daysBetween incluye ambos extremos y es vacio si el rango esta invertido`() {
        assertEquals(listOf(MONDAY, TUESDAY, WEDNESDAY), daysBetween(MONDAY, WEDNESDAY).toList())
        assertEquals(listOf(MONDAY), daysBetween(MONDAY, MONDAY).toList())
        assertEquals(emptyList<LocalDate>(), daysBetween(WEDNESDAY, MONDAY).toList())
    }

    // --- Hábitos diarios y de días concretos -------------------------------------

    @Test
    fun `un habito diario es obligatorio todos los dias`() {
        val habit = habit()
        daysBetween(MONDAY, SUNDAY).forEach { date ->
            assertEquals(DayDemand.REQUIRED, Schedule.demandOn(habit, date, emptySet()))
        }
    }

    @Test
    fun `un habito de dias concretos solo toca esos dias`() {
        val habit = habit(frequency = Frequency.SpecificDays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)))
        assertEquals(DayDemand.REQUIRED, Schedule.demandOn(habit, MONDAY, emptySet()))
        assertEquals(DayDemand.NOT_SCHEDULED, Schedule.demandOn(habit, TUESDAY, emptySet()))
        assertEquals(DayDemand.REQUIRED, Schedule.demandOn(habit, FRIDAY, emptySet()))
        assertEquals(DayDemand.NOT_SCHEDULED, Schedule.demandOn(habit, SUNDAY, emptySet()))
    }

    @Test
    fun `un habito no toca antes de crearse ni despues de archivarse`() {
        val habit = habit(createdOn = TUESDAY, archivedOn = FRIDAY)
        assertFalse(Schedule.isPlannedOn(habit, MONDAY))
        assertTrue(Schedule.isPlannedOn(habit, TUESDAY))
        assertTrue(Schedule.isPlannedOn(habit, THURSDAY))
        // El día en que se archiva ya no cuenta.
        assertFalse(Schedule.isPlannedOn(habit, FRIDAY))
    }

    // --- Hábitos "X veces por semana" ----------------------------------------------

    private val threeTimes = habit(frequency = Frequency.TimesPerWeek(3))

    @Test
    fun `un habito semanal es opcional mientras queda margen`() {
        // Lunes: hacen falta 3 y quedan 7 días.
        assertEquals(DayDemand.OPTIONAL, Schedule.demandOn(threeTimes, MONDAY, emptySet()))
        // Jueves sin haber hecho nada: hacen falta 3 y quedan 4 días.
        assertEquals(DayDemand.OPTIONAL, Schedule.demandOn(threeTimes, THURSDAY, emptySet()))
    }

    @Test
    fun `un habito semanal pasa a obligatorio cuando ya no queda margen`() {
        // Viernes sin haber hecho nada: hacen falta 3 y quedan justo 3 días.
        assertEquals(DayDemand.REQUIRED, Schedule.demandOn(threeTimes, FRIDAY, emptySet()))
        // Domingo con 2 hechas: falta 1 y queda 1 día.
        assertEquals(DayDemand.REQUIRED, Schedule.demandOn(threeTimes, SUNDAY, setOf(MONDAY, TUESDAY)))
    }

    @Test
    fun `un habito semanal con el objetivo cumplido vuelve a ser opcional`() {
        val doneDates = setOf(MONDAY, TUESDAY, WEDNESDAY)
        assertEquals(DayDemand.OPTIONAL, Schedule.demandOn(threeTimes, SATURDAY, doneDates))
        assertEquals(DayDemand.OPTIONAL, Schedule.demandOn(threeTimes, SUNDAY, doneDates))
    }

    @Test
    fun `marcar el propio dia no cambia lo que se pedia ese dia`() {
        // Si no fuera así, el hábito "saltaría" de obligatorio a opcional al marcarlo.
        val before = Schedule.demandOn(threeTimes, FRIDAY, emptySet())
        val after = Schedule.demandOn(threeTimes, FRIDAY, setOf(FRIDAY))
        assertEquals(before, after)
    }

    @Test
    fun `las semanas anteriores no cuentan para el objetivo de esta`() {
        val lastWeek = setOf(MONDAY.minusDays(1), MONDAY.minusDays(2), MONDAY.minusDays(3))
        assertEquals(DayDemand.REQUIRED, Schedule.demandOn(threeTimes, FRIDAY, lastWeek))
    }

    @Test
    fun `el objetivo semanal se reduce en proporcion si el habito se creo a mitad de semana`() {
        // 4 veces por semana creado el jueves: 4 días disponibles de 7 -> 2 veces.
        val fromThursday = habit(frequency = Frequency.TimesPerWeek(4), createdOn = THURSDAY)
        assertEquals(2, Schedule.weeklyTarget(fromThursday, MONDAY))
        // La semana siguiente ya se pide el objetivo entero.
        assertEquals(4, Schedule.weeklyTarget(fromThursday, MONDAY.plusWeeks(1)))
        // Y antes de existir no se pide nada.
        assertEquals(0, Schedule.weeklyTarget(fromThursday, MONDAY.minusWeeks(1)))
    }

    @Test
    fun `el objetivo semanal reducido nunca baja de una vez ni supera los dias disponibles`() {
        // 1 vez por semana creado el domingo: sigue siendo 1.
        assertEquals(1, Schedule.weeklyTarget(habit(frequency = Frequency.TimesPerWeek(1), createdOn = SUNDAY), MONDAY))
        // 7 veces por semana creado el sábado: como mucho los 2 días que quedan.
        assertEquals(2, Schedule.weeklyTarget(habit(frequency = Frequency.TimesPerWeek(7), createdOn = SATURDAY), MONDAY))
    }

    @Test
    fun `un habito semanal recien creado no es obligatorio el primer dia si hay margen`() {
        val fromThursday = habit(frequency = Frequency.TimesPerWeek(4), createdOn = THURSDAY)
        assertEquals(DayDemand.OPTIONAL, Schedule.demandOn(fromThursday, THURSDAY, emptySet()))
        // El sábado, sin haber hecho ninguna de las 2, ya no queda margen.
        assertEquals(DayDemand.REQUIRED, Schedule.demandOn(fromThursday, SATURDAY, emptySet()))
    }

    @Test
    fun `weeklyTarget es cero para habitos que no son semanales`() {
        assertEquals(0, Schedule.weeklyTarget(habit(), MONDAY))
    }

    // --- Toques sobre hábitos con cantidad --------------------------------------------

    @Test
    fun `un habito sin cantidad se marca y desmarca con un toque`() {
        val habit = habit()
        assertEquals(1, habit.amountAfterTap(0))
        assertEquals(0, habit.amountAfterTap(1))
    }

    @Test
    fun `un objetivo pequenio avanza de uno en uno y al completarse se reinicia`() {
        val water = habit(target = Target(8, TargetUnit.GLASSES))
        assertEquals(1, water.amountAfterTap(0))
        assertEquals(8, water.amountAfterTap(7))
        assertEquals(0, water.amountAfterTap(8))
    }

    @Test
    fun `un objetivo grande se completa de un toque`() {
        val workout = habit(target = Target(45, TargetUnit.MINUTES))
        assertEquals(45, workout.amountAfterTap(0))
        assertEquals(45, workout.amountAfterTap(20))
        assertEquals(0, workout.amountAfterTap(45))
    }
}
