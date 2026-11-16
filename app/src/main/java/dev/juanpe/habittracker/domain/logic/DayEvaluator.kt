package dev.juanpe.habittracker.domain.logic

import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.Habit
import java.time.LocalDate

/** Balance de un día: cuántos hábitos tocaban y cuántos se hicieron. */
data class DayOutcome(
    val required: Int,
    val requiredDone: Int,
    /** Hábitos opcionales (semanales con margen) que se hicieron de todos modos. */
    val optionalDone: Int,
) {
    /** Hábitos obligatorios que faltan. */
    val pending: Int get() = required - requiredDone

    /**
     * Un día está completo si se hizo todo lo obligatorio. Si no había nada
     * obligatorio, basta con haber hecho algún opcional.
     */
    val isComplete: Boolean
        get() = if (required == 0) optionalDone > 0 else requiredDone == required

    /** Estado del día antes de aplicar comodines (nunca devuelve FROZEN). */
    val status: DayStatus
        get() = when {
            isComplete -> DayStatus.COMPLETE
            required == 0 -> DayStatus.REST
            else -> DayStatus.MISSED
        }
}

object DayEvaluator {

    fun evaluate(date: LocalDate, habits: List<Habit>, index: CompletionIndex): DayOutcome {
        var required = 0
        var requiredDone = 0
        var optionalDone = 0
        for (habit in habits) {
            val done = index.isDone(habit.id, date)
            when (Schedule.demandOn(habit, date, index.doneDates(habit.id))) {
                DayDemand.REQUIRED -> {
                    required++
                    if (done) requiredDone++
                }
                DayDemand.OPTIONAL -> if (done) optionalDone++
                DayDemand.NOT_SCHEDULED -> Unit
            }
        }
        return DayOutcome(required, requiredDone, optionalDone)
    }
}
