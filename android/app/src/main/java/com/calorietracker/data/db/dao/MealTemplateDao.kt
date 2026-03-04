package com.calorietracker.data.db.dao

import androidx.room.*
import com.calorietracker.data.db.entity.MealTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {
    @Query("SELECT * FROM meal_templates")
    fun getAllTemplates(): Flow<List<MealTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MealTemplateEntity)

    @Query("DELETE FROM meal_templates WHERE id = :id")
    suspend fun deleteById(id: String)
}
