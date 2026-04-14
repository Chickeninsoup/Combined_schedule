package com.example.combined_schedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedBusTripDao {

    @Query("SELECT * FROM bus_trips ORDER BY routeName ASC")
    fun getAll(): Flow<List<SavedBusTrip>>

    @Query("SELECT COUNT(*) FROM bus_trips")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: SavedBusTrip)

    @Update
    suspend fun update(trip: SavedBusTrip)

    @Delete
    suspend fun delete(trip: SavedBusTrip)
}
