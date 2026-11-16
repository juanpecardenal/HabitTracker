package dev.juanpe.habittracker.data

import android.content.Context
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Target
import dev.juanpe.habittracker.domain.model.TargetUnit
import java.time.DayOfWeek
import java.time.LocalDate

/** Hábitos de ejemplo que se crean la primera vez. Se pueden editar o borrar. */
object SeedData {

    private val WEEKDAYS = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    fun exampleHabits(context: Context, today: LocalDate): List<Habit> = listOf(
        Habit(
            name = context.getString(R.string.seed_diet_no_processed),
            area = Area.DIET,
            createdOn = today,
        ),
        Habit(
            name = context.getString(R.string.seed_diet_water),
            area = Area.DIET,
            target = Target(8, TargetUnit.GLASSES),
            createdOn = today,
        ),
        Habit(
            name = context.getString(R.string.seed_training_workout),
            area = Area.TRAINING,
            frequency = Frequency.TimesPerWeek(4),
            target = Target(45, TargetUnit.MINUTES),
            createdOn = today,
        ),
        Habit(
            name = context.getString(R.string.seed_study_pomodoros),
            area = Area.STUDY,
            frequency = Frequency.SpecificDays(WEEKDAYS),
            target = Target(2, TargetUnit.TIMES),
            createdOn = today,
        ),
        Habit(
            name = context.getString(R.string.seed_relations_compliment),
            area = Area.RELATIONS,
            createdOn = today,
        ),
        Habit(
            name = context.getString(R.string.seed_relations_listen),
            area = Area.RELATIONS,
            createdOn = today,
        ),
    )
}
