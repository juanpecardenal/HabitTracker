package dev.juanpe.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

/** Enseña a Room a guardar fechas: LocalDate <-> número de día desde 1970. */
class Converters {
    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()
}

@Database(
    entities = [
        HabitEntity::class,
        CompletionEntity::class,
        XpEventEntity::class,
        DayRecordEntity::class,
        ProfileEntity::class,
        UnlockedAchievementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun xpDao(): XpDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun profileDao(): ProfileDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "habits.db").build()
    }
}
