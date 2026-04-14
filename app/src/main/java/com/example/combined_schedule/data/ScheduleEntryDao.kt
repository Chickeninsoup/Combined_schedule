package com.example.combined_schedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEntryDao {

    @Query("SELECT * FROM schedule_entries ORDER BY date ASC, time ASC")
    fun getAll(): Flow<List<ScheduleEntry>>

    @Query("SELECT COUNT(*) FROM schedule_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScheduleEntry)

    @Update
    suspend fun update(entry: ScheduleEntry)

    @Delete
    suspend fun delete(entry: ScheduleEntry)
}
