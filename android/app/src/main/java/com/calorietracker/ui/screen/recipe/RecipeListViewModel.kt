package com.calorietracker.ui.screen.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.Recipe
import com.calorietracker.data.repository.FoodLogRepository
import com.calorietracker.data.repository.RecipeRepository
import com.calorietracker.util.DateUtils
import com.calorietracker.util.NutritionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val foodLogRepository: FoodLogRepository
) : ViewModel() {

    val recipes: StateFlow<List<Recipe>> = recipeRepository.getAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRecipe(id: String) {
        viewModelScope.launch { recipeRepository.deleteById(id) }
    }

    fun updateServings(recipe: Recipe, newServings: Float) {
        viewModelScope.launch {
            recipeRepository.update(recipe.copy(servings = newServings))
        }
    }

    fun updateItems(recipe: Recipe, items: List<FoodItem>) {
        viewModelScope.launch {
            recipeRepository.update(
                recipe.copy(
                    calories = items.sumOf { it.calories }.toFloat(),
                    protein = items.sumOf { it.protein.toDouble() }.toFloat(),
                    carbs = items.sumOf { it.carbs.toDouble() }.toFloat(),
                    fat = items.sumOf { it.fat.toDouble() }.toFloat(),
                    items = items
                )
            )
        }
    }

    fun logRecipe(recipe: Recipe, portions: Float, mealType: String) {
        viewModelScope.launch {
            val mealTypeActual = mealType.ifEmpty { DateUtils.inferMealType(System.currentTimeMillis()) }
            val scaledTotals = NutritionUtils.scaleMacros(
                recipe.calories, recipe.protein, recipe.carbs, recipe.fat,
                portions, recipe.servings
            )
            val entry = FoodLogEntry(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                foodName = recipe.name,
                calories = scaledTotals.calories,
                protein = scaledTotals.protein,
                carbs = scaledTotals.carbs,
                fat = scaledTotals.fat,
                quantity = "${portions}x portions",
                source = "recipe",
                mealType = mealTypeActual,
                items = recipe.items
            )
            foodLogRepository.insert(entry)
        }
    }
}
