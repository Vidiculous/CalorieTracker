package com.calorietracker.data.db.dao

import androidx.room.*
import com.calorietracker.data.db.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    fun getAllWeightLogs(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE date(date/1000, 'unixepoch', 'localtime') = date(:dateMillis/1000, 'unixepoch', 'localtime') LIMIT 1")
    suspend fun getForDate(dateMillis: Long): WeightLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}
