package com.example.combined_schedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {

    @Query("SELECT * FROM works ORDER BY isCompleted ASC, dueDate ASC")
    fun getAll(): Flow<List<Work>>

    @Query("SELECT COUNT(*) FROM works")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(work: Work)

    @Update
    suspend fun update(work: Work)

    @Delete
    suspend fun delete(work: Work)
}
