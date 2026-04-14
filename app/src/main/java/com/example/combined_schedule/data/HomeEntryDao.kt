package com.example.combined_schedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeEntryDao {

    @Query("SELECT * FROM home_entries ORDER BY time ASC")
    fun getAll(): Flow<List<HomeEntry>>

    @Query("SELECT * FROM home_entries WHERE id = :id")
    suspend fun findById(id: String): HomeEntry?

    @Query("SELECT * FROM home_entries ORDER BY time ASC")
    suspend fun getAllList(): List<HomeEntry>

    @Query("SELECT * FROM home_entries WHERE title LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' ORDER BY time ASC")
    suspend fun search(query: String): List<HomeEntry>

    @Query("SELECT COUNT(*) FROM home_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HomeEntry)

    @Update
    suspend fun update(entry: HomeEntry)

    @Delete
    suspend fun delete(entry: HomeEntry)
}
