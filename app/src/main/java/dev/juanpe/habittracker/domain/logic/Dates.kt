package dev.juanpe.habittracker.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Lunes de la semana a la que pertenece la fecha (las semanas van de lunes a domingo). */
fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Domingo de la semana a la que pertenece la fecha. */
fun LocalDate.endOfWeek(): LocalDate = startOfWeek().plusDays(6)

/** Todos los días desde [from] hasta [to], ambos incluidos. Vacío si [to] es anterior. */
fun daysBetween(from: LocalDate, to: LocalDate): Sequence<LocalDate> =
    generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
