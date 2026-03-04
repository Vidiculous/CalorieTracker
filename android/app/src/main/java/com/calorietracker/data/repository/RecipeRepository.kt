package com.calorietracker.data.repository

import com.calorietracker.data.db.dao.RecipeDao
import com.calorietracker.data.db.entity.RecipeEntity
import com.calorietracker.data.model.Recipe
import com.calorietracker.util.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val dao: RecipeDao,
    private val jsonParser: JsonParser
) {
    fun getAllRecipes(): Flow<List<Recipe>> =
        dao.getAllRecipes().map { entities -> entities.map { it.toDomain(jsonParser) } }

    suspend fun insert(recipe: Recipe) =
        dao.insert(recipe.toEntity(jsonParser))

    suspend fun update(recipe: Recipe) =
        dao.update(recipe.toEntity(jsonParser))

    suspend fun deleteById(id: String) =
        dao.deleteById(id)
}

private fun RecipeEntity.toDomain(jsonParser: JsonParser): Recipe = Recipe(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    servings = servings,
    description = description,
    items = jsonParser.parseFoodItems(itemsJson),
    createdAt = createdAt
)

private fun Recipe.toEntity(jsonParser: JsonParser): RecipeEntity = RecipeEntity(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    servings = servings,
    description = description,
    itemsJson = jsonParser.serializeFoodItems(items),
    createdAt = createdAt
)
