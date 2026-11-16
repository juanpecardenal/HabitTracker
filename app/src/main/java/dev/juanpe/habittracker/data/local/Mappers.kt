package dev.juanpe.habittracker.data.local

import dev.juanpe.habittracker.domain.model.Completion
import dev.juanpe.habittracker.domain.model.Frequency
import dev.juanpe.habittracker.domain.model.Habit
import dev.juanpe.habittracker.domain.model.Profile
import dev.juanpe.habittracker.domain.model.StreakState
import dev.juanpe.habittracker.domain.model.Target
import java.time.DayOfWeek
import java.time.LocalTime

/*
 * Conversión entre las entidades de Room (cómo se guarda) y los modelos de
 * dominio (cómo se usa en el resto de la app).
 */

// --- Días de la semana <-> máscara de bits ---------------------------------

fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

fun Int.toDaysOfWeek(): Set<DayOfWeek> =
    DayOfWeek.entries.filter { day -> this and (1 shl (day.value - 1)) != 0 }.toSet()

// --- Hora <-> minutos desde medianoche --------------------------------------

fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute

fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

// --- Hábitos -----------------------------------------------------------------

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    area = area,
    frequency = when (frequencyType) {
        FrequencyType.DAILY -> Frequency.Daily
        FrequencyType.SPECIFIC_DAYS -> Frequency.SpecificDays(daysMask.toDaysOfWeek())
        FrequencyType.TIMES_PER_WEEK -> Frequency.TimesPerWeek(timesPerWeek)
    },
    target = if (targetAmount != null && targetUnit != null) Target(targetAmount, targetUnit) else null,
    reminder = reminderMinutes?.toLocalTime(),
    createdOn = createdOn,
    archivedOn = archivedOn,
)

fun Habit.toEntity(): HabitEntity {
    val frequency = frequency
    return HabitEntity(
        id = id,
        name = name,
        area = area,
        frequencyType = when (frequency) {
            Frequency.Daily -> FrequencyType.DAILY
            is Frequency.SpecificDays -> FrequencyType.SPECIFIC_DAYS
            is Frequency.TimesPerWeek -> FrequencyType.TIMES_PER_WEEK
        },
        daysMask = (frequency as? Frequency.SpecificDays)?.days?.toMask() ?: 0,
        timesPerWeek = (frequency as? Frequency.TimesPerWeek)?.times ?: 0,
        targetAmount = target?.amount,
        targetUnit = target?.unit,
        reminderMinutes = reminder?.toMinutesOfDay(),
        createdOn = createdOn,
        archivedOn = archivedOn,
    )
}

// --- Registros ---------------------------------------------------------------

fun CompletionEntity.toDomain() = Completion(habitId, date, amount, done)

// --- Perfil ------------------------------------------------------------------

fun ProfileEntity.toDomain(): Profile = Profile(
    streak = StreakState(
        current = streakCurrent,
        best = streakBest,
        freezes = freezes,
        freezeProgress = freezeProgress,
        freezesUsed = freezesUsed,
        comebacks = comebacks,
        pendingComeback = pendingComeback,
    ),
    lastClosedDate = lastClosedDate,
    lastCelebratedDay = lastCelebratedDay,
    maxLevelCelebrated = maxLevelCelebrated,
    eveningReminderEnabled = eveningReminderEnabled,
    eveningReminderTime = eveningReminderMinutes.toLocalTime(),
    notificationBannerDismissed = notificationBannerDismissed,
)

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    lastClosedDate = lastClosedDate,
    streakCurrent = streak.current,
    streakBest = streak.best,
    freezes = streak.freezes,
    freezeProgress = streak.freezeProgress,
    freezesUsed = streak.freezesUsed,
    comebacks = streak.comebacks,
    pendingComeback = streak.pendingComeback,
    lastCelebratedDay = lastCelebratedDay,
    maxLevelCelebrated = maxLevelCelebrated,
    eveningReminderEnabled = eveningReminderEnabled,
    eveningReminderMinutes = eveningReminderTime.toMinutesOfDay(),
    notificationBannerDismissed = notificationBannerDismissed,
)
