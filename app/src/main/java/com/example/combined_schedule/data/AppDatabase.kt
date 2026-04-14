package com.example.combined_schedule.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        HomeEntry::class,
        SavedBusTrip::class,
        Work::class,
        SavedLocation::class,
        ScheduleEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun homeEntryDao(): HomeEntryDao
    abstract fun savedBusTripDao(): SavedBusTripDao
    abstract fun workDao(): WorkDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun scheduleEntryDao(): ScheduleEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "combined_schedule.db"
                ).build().also { INSTANCE = it }
            }
    }
}
