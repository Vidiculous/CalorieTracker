package com.calorietracker.data.db.dao

import androidx.room.*
import com.calorietracker.data.db.entity.MealPlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan_items")
    fun getAllPlanItems(): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_items WHERE dateKey = :dateKey")
    fun getItemsForDate(dateKey: String): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_items WHERE dateKey = :dateKey AND mealType = :mealType")
    suspend fun getItemsForMeal(dateKey: String, mealType: String): List<MealPlanItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MealPlanItemEntity)

    @Query("DELETE FROM meal_plan_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM meal_plan_items WHERE dateKey = :dateKey AND mealType = :mealType")
    suspend fun deleteMealSlot(dateKey: String, mealType: String)
}
