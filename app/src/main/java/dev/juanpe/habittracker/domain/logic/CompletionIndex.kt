package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.Completion
import java.time.LocalDate

/**
 * Índice en memoria de los registros para consultarlos rápido por hábito y día.
 * Evita recorrer la lista entera cada vez que se pregunta "¿se hizo este día?".
 */
class CompletionIndex(completions: List<Completion>) {

    private val byHabit: Map<Long, Map<LocalDate, Completion>> =
        completions.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }

    private val doneDatesByHabit: Map<Long, Set<LocalDate>> =
        byHabit.mapValues { (_, byDate) -> byDate.values.filter { it.done }.map { it.date }.toSet() }

    fun amount(habitId: Long, date: LocalDate): Int = byHabit[habitId]?.get(date)?.amount ?: 0

    fun isDone(habitId: Long, date: LocalDate): Boolean = date in doneDates(habitId)

    /** Días en los que el hábito quedó cumplido. */
    fun doneDates(habitId: Long): Set<LocalDate> = doneDatesByHabit[habitId] ?: emptySet()

    /** Todos los registros de un hábito (cumplidos o a medias), por fecha. */
    fun entries(habitId: Long): Map<LocalDate, Completion> = byHabit[habitId] ?: emptyMap()
}
