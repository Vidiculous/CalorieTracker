package com.calorietracker.data.db.dao

import androidx.room.*
import com.calorietracker.data.db.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date(:dateMillis/1000, 'unixepoch', 'localtime') ORDER BY timestamp DESC")
    fun getLogsForDate(dateMillis: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FoodLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FoodLogEntity)

    @Update
    suspend fun update(log: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT DISTINCT foodName FROM food_logs ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentFoodNames(): List<String>
}
