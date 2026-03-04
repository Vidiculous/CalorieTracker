package com.calorietracker.data.repository

import com.calorietracker.data.db.dao.MealTemplateDao
import com.calorietracker.data.db.entity.MealTemplateEntity
import com.calorietracker.data.model.MealTemplate
import com.calorietracker.util.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealTemplateRepository @Inject constructor(
    private val dao: MealTemplateDao,
    private val jsonParser: JsonParser
) {
    fun getAllTemplates(): Flow<List<MealTemplate>> =
        dao.getAllTemplates().map { entities -> entities.map { it.toDomain(jsonParser) } }

    suspend fun insert(template: MealTemplate) =
        dao.insert(template.toEntity(jsonParser))

    suspend fun deleteById(id: String) =
        dao.deleteById(id)
}

private fun MealTemplateEntity.toDomain(jsonParser: JsonParser) =
    MealTemplate(id, name, jsonParser.parseFoodItems(itemsJson))

private fun MealTemplate.toEntity(jsonParser: JsonParser) =
    MealTemplateEntity(id, name, jsonParser.serializeFoodItems(items))
