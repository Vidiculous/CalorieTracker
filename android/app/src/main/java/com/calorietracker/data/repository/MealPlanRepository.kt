package com.calorietracker.data.repository

import com.calorietracker.data.db.dao.MealPlanDao
import com.calorietracker.data.db.entity.MealPlanItemEntity
import com.calorietracker.data.model.MealPlanItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepository @Inject constructor(private val dao: MealPlanDao) {

    fun getAllPlanItems(): Flow<List<MealPlanItem>> =
        dao.getAllPlanItems().map { it.map(MealPlanItemEntity::toDomain) }

    fun getItemsForDate(dateKey: String): Flow<List<MealPlanItem>> =
        dao.getItemsForDate(dateKey).map { it.map(MealPlanItemEntity::toDomain) }

    suspend fun getItemsForMeal(dateKey: String, mealType: String): List<MealPlanItem> =
        dao.getItemsForMeal(dateKey, mealType).map { it.toDomain() }

    suspend fun insert(item: MealPlanItem) =
        dao.insert(item.toEntity())

    suspend fun deleteById(id: String) =
        dao.deleteById(id)

    suspend fun deleteMealSlot(dateKey: String, mealType: String) =
        dao.deleteMealSlot(dateKey, mealType)
}

private fun MealPlanItemEntity.toDomain() = MealPlanItem(id, dateKey, mealType, foodName, calories, protein, carbs, fat, quantity)
private fun MealPlanItem.toEntity() = MealPlanItemEntity(id, dateKey, mealType, foodName, calories, protein, carbs, fat, quantity)
