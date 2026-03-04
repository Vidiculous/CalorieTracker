package com.calorietracker.data.repository

import com.calorietracker.data.db.dao.FoodLogDao
import com.calorietracker.data.db.entity.FoodLogEntity
import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.util.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodLogRepository @Inject constructor(
    private val dao: FoodLogDao,
    private val jsonParser: JsonParser
) {
    fun getAllLogs(): Flow<List<FoodLogEntry>> =
        dao.getAllLogs().map { entities -> entities.map { it.toDomain(jsonParser) } }

    fun getLogsForDate(dateMillis: Long): Flow<List<FoodLogEntry>> =
        dao.getLogsForDate(dateMillis).map { entities -> entities.map { it.toDomain(jsonParser) } }

    suspend fun getById(id: String): FoodLogEntry? =
        dao.getById(id)?.toDomain(jsonParser)

    suspend fun insert(entry: FoodLogEntry) =
        dao.insert(entry.toEntity(jsonParser))

    suspend fun update(entry: FoodLogEntry) =
        dao.update(entry.toEntity(jsonParser))

    suspend fun deleteById(id: String) =
        dao.deleteById(id)

    suspend fun getRecentFoodNames(): List<String> =
        dao.getRecentFoodNames()
}

private fun FoodLogEntity.toDomain(jsonParser: JsonParser): FoodLogEntry = FoodLogEntry(
    id = id,
    timestamp = timestamp,
    foodName = foodName,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    quantity = quantity,
    source = source,
    mealType = mealType,
    items = jsonParser.parseFoodItems(itemsJson)
)

private fun FoodLogEntry.toEntity(jsonParser: JsonParser): FoodLogEntity = FoodLogEntity(
    id = id,
    timestamp = timestamp,
    foodName = foodName,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    quantity = quantity,
    source = source,
    mealType = mealType,
    itemsJson = jsonParser.serializeFoodItems(items)
)
