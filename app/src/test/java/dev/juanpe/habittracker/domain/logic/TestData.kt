package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Completion
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Target
import java.time.LocalDate

/*
 * Ayudas compartidas por los tests.
 *
 * Todas las fechas giran alrededor de una semana conocida de noviembre de 2026:
 * el lunes 9 ([MONDAY]) y los días siguientes, para que los tests no dependan
 * del día en que se ejecuten.
 */

val MONDAY: LocalDate = LocalDate.of(2026, 11, 9)
val TUESDAY: LocalDate = MONDAY.plusDays(1)
val WEDNESDAY: LocalDate = MONDAY.plusDays(2)
val THURSDAY: LocalDate = MONDAY.plusDays(3)
val FRIDAY: LocalDate = MONDAY.plusDays(4)
val SATURDAY: LocalDate = MONDAY.plusDays(5)
val SUNDAY: LocalDate = MONDAY.plusDays(6)

/** Crea un hábito de prueba; por defecto diario y creado hace mucho. */
fun habit(
    id: Long = 1,
    frequency: Frequency = Frequency.Daily,
    area: Area = Area.DIET,
    target: Target? = null,
    createdOn: LocalDate = MONDAY.minusWeeks(10),
    archivedOn: LocalDate? = null,
) = Habit(
    id = id,
    name = "Hábito $id",
    area = area,
    frequency = frequency,
    target = target,
    createdOn = createdOn,
    archivedOn = archivedOn,
)

/** Registro cumplido de un hábito en una fecha. */
fun done(habitId: Long, date: LocalDate) = Completion(habitId, date, amount = 1, done = true)

/** Índice con el hábito [habitId] cumplido en las fechas indicadas. */
fun indexOf(habitId: Long, vararg dates: LocalDate) = CompletionIndex(dates.map { done(habitId, it) })
