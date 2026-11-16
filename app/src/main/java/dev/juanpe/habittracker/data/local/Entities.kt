package dev.juanpe.habittracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.juanpe.habittracker.domain.model.Area
import dev.juanpe.habittracker.domain.model.DayStatus
import dev.juanpe.habittracker.domain.model.TargetUnit
import java.time.LocalDate

/*
 * Tablas de la base de datos. Las fechas (LocalDate) se guardan como número
 * de día gracias a Converters, y los enums como texto.
 */

enum class FrequencyType { DAILY, SPECIFIC_DAYS, TIMES_PER_WEEK }

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val area: Area,
    val frequencyType: FrequencyType,
    /** Días de la semana como máscara de bits (bit 0 = lunes ... bit 6 = domingo). */
    val daysMask: Int,
    val timesPerWeek: Int,
    val targetAmount: Int?,
    val targetUnit: TargetUnit?,
    /** Hora del recordatorio en minutos desde medianoche, o null. */
    val reminderMinutes: Int?,
    val createdOn: LocalDate,
    val archivedOn: LocalDate?,
)

/** Lo registrado para un hábito en un día. Si se borra el hábito, se borra su historial. */
@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("date")],
)
data class CompletionEntity(
    val habitId: Long,
    val date: LocalDate,
    val amount: Int,
    val done: Boolean,
)

enum class XpEventType { HABIT, DAY_BONUS }

/**
 * Libro de experiencia: una fila por cada hábito cumplido y por cada bonus de día.
 * La XP total es la suma de [amount]. No tiene clave foránea a propósito:
 * si se borra un hábito, la experiencia ganada con él se conserva.
 */
@Entity(tableName = "xp_events", primaryKeys = ["date", "habitId", "type"])
data class XpEventEntity(
    val date: LocalDate,
    /** Id del hábito, o [DAY_BONUS_HABIT_ID] para el bonus de día completo. */
    val habitId: Long,
    val type: XpEventType,
    val area: Area?,
    val amount: Int,
) {
    companion object {
        const val DAY_BONUS_HABIT_ID = 0L
    }
}

/** Foto fija de cómo acabó cada día ya cerrado. */
@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val date: LocalDate,
    val status: DayStatus,
)

/** Fila única (id = 0) con la racha global, los comodines y los ajustes. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val lastClosedDate: LocalDate,
    val streakCurrent: Int,
    val streakBest: Int,
    val freezes: Int,
    val freezeProgress: Int,
    val freezesUsed: Int,
    val comebacks: Int,
    val pendingComeback: Boolean,
    val lastCelebratedDay: LocalDate?,
    val maxLevelCelebrated: Int,
    val eveningReminderEnabled: Boolean,
    val eveningReminderMinutes: Int,
    val notificationBannerDismissed: Boolean,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievementEntity(
    /** Nombre del enum Achievement. */
    @PrimaryKey val id: String,
    val unlockedOn: LocalDate,
)
