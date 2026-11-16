package dev.juanpe.habittracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.juanpe.habittracker.R
import dev.juanpe.habittracker.domain.logic.HabitStreak
import dev.juanpe.habittracker.domain.logic.StreakUnit
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Target
import dev.juanpe.habittracker.domain.model.TargetUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/*
 * Funciones que convierten datos del dominio en textos para mostrar.
 * La app está solo en español, así que las fechas se formatean siempre en ese idioma.
 */

val AppLocale: Locale = Locale.forLanguageTag("es-ES")

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", AppLocale)
private val mediumDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", AppLocale)

fun LocalTime.formatted(): String = format(timeFormatter)

/** "jueves, 12 de noviembre" */
fun LocalDate.longLabel(): String = format(longDateFormatter)

/** "12 de noviembre de 2026" */
fun LocalDate.mediumLabel(): String = format(mediumDateFormatter)

/** "lun", "mar"... */
fun DayOfWeek.shortLabel(): String = getDisplayName(TextStyle.SHORT, AppLocale).removeSuffix(".")

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** "Cada día", "De lunes a viernes", "lun · mié · vie", "3 veces por semana". */
@Composable
fun frequencyText(frequency: Frequency): String = when (frequency) {
    Frequency.Daily -> stringResource(R.string.frequency_daily)
    is Frequency.SpecificDays -> when {
        frequency.days.size == 7 -> stringResource(R.string.frequency_daily)
        frequency.days == WEEKDAYS -> stringResource(R.string.frequency_weekdays)
        frequency.days == WEEKEND -> stringResource(R.string.frequency_weekend)
        else -> frequency.days.sorted().joinToString(" · ") { it.shortLabel() }
    }
    is Frequency.TimesPerWeek ->
        pluralStringResource(R.plurals.frequency_times_per_week, frequency.times, frequency.times)
}

/** "45 min", "8 vasos", "2 veces"... */
@Composable
fun amountText(amount: Int, unit: TargetUnit): String {
    val plural = when (unit) {
        TargetUnit.MINUTES -> R.plurals.unit_minutes
        TargetUnit.REPS -> R.plurals.unit_reps
        TargetUnit.GLASSES -> R.plurals.unit_glasses
        TargetUnit.PAGES -> R.plurals.unit_pages
        TargetUnit.TIMES -> R.plurals.unit_times
    }
    return pluralStringResource(plural, amount, amount)
}

@Composable
fun targetText(target: Target): String = amountText(target.amount, target.unit)

/** Nombre de la unidad para los selectores: "Minutos", "Vasos"... */
@Composable
fun unitName(unit: TargetUnit): String = stringResource(
    when (unit) {
        TargetUnit.MINUTES -> R.string.unit_name_minutes
        TargetUnit.REPS -> R.string.unit_name_reps
        TargetUnit.GLASSES -> R.string.unit_name_glasses
        TargetUnit.PAGES -> R.string.unit_name_pages
        TargetUnit.TIMES -> R.string.unit_name_times
    },
)

/** "5 días" o "3 semanas", según cómo cuente la racha el hábito. */
@Composable
fun streakText(count: Int, unit: StreakUnit): String = pluralStringResource(
    if (unit == StreakUnit.DAYS) R.plurals.streak_days else R.plurals.streak_weeks,
    count,
    count,
)

@Composable
fun streakText(streak: HabitStreak): String = streakText(streak.current, streak.unit)
