package com.calorietracker.ui.screen.mealplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.MealPlanItem
import com.calorietracker.data.model.MealTemplate
import com.calorietracker.data.model.Recipe
import com.calorietracker.data.repository.FoodLogRepository
import com.calorietracker.data.repository.MealPlanRepository
import com.calorietracker.data.repository.MealTemplateRepository
import com.calorietracker.data.repository.RecipeRepository
import com.calorietracker.util.DateUtils
import com.calorietracker.util.NutritionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class MealPlannerUiState(
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val planItems: List<MealPlanItem> = emptyList(),
    val weekDays: List<Long> = emptyList()
)

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val foodLogRepository: FoodLogRepository,
    private val recipeRepository: RecipeRepository,
    private val mealTemplateRepository: MealTemplateRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))

    val uiState: StateFlow<MealPlannerUiState> = combine(
        _selectedDate,
        mealPlanRepository.getAllPlanItems()
    ) { selectedDate, allItems ->
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val weekDays = (0..6).map { DateUtils.addDays(today, it) }
        MealPlannerUiState(
            selectedDateMillis = selectedDate,
            planItems = allItems.filter { it.dateKey == DateUtils.formatPlanKey(selectedDate) },
            weekDays = weekDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealPlannerUiState())

    val recipes: StateFlow<List<Recipe>> = recipeRepository.getAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<MealTemplate>> = mealTemplateRepository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEntries: StateFlow<List<FoodLogEntry>> = foodLogRepository.getAllLogs()
        .map { it.take(50) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = DateUtils.startOfDay(dateMillis)
    }

    fun removePlanItem(id: String) {
        viewModelScope.launch { mealPlanRepository.deleteById(id) }
    }

    fun logPlannedMeal(dateKey: String, mealType: String) {
        viewModelScope.launch {
            val items = mealPlanRepository.getItemsForMeal(dateKey, mealType)
            val now = System.currentTimeMillis()
            items.forEach { planItem ->
                foodLogRepository.insert(
                    FoodLogEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = now,
                        foodName = planItem.foodName,
                        calories = planItem.calories,
                        protein = planItem.protein,
                        carbs = planItem.carbs,
                        fat = planItem.fat,
                        quantity = planItem.quantity,
                        source = "plan",
                        mealType = planItem.mealType
                    )
                )
            }
            mealPlanRepository.deleteMealSlot(dateKey, mealType)
        }
    }

    fun addRecipeToPlan(recipe: Recipe, portions: Float, mealType: String, dateKey: String) {
        viewModelScope.launch {
            val scaled = NutritionUtils.scaleMacros(
                recipe.calories, recipe.protein, recipe.carbs, recipe.fat, portions, recipe.servings
            )
            mealPlanRepository.insert(
                MealPlanItem(
                    id = UUID.randomUUID().toString(),
                    dateKey = dateKey,
                    mealType = mealType,
                    foodName = recipe.name,
                    calories = scaled.calories,
                    protein = scaled.protein,
                    carbs = scaled.carbs,
                    fat = scaled.fat,
                    quantity = "${portions}x portions"
                )
            )
        }
    }

    fun addTemplateToPlan(template: MealTemplate, mealType: String, dateKey: String) {
        viewModelScope.launch {
            template.items.forEach { item ->
                mealPlanRepository.insert(
                    MealPlanItem(
                        id = UUID.randomUUID().toString(),
                        dateKey = dateKey,
                        mealType = mealType,
                        foodName = item.foodName,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        quantity = item.quantityDesc
                    )
                )
            }
        }
    }

    fun addEntryToPlan(entry: FoodLogEntry, mealType: String, dateKey: String) {
        viewModelScope.launch {
            mealPlanRepository.insert(
                MealPlanItem(
                    id = UUID.randomUUID().toString(),
                    dateKey = dateKey,
                    mealType = mealType,
                    foodName = entry.foodName,
                    calories = entry.calories,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    quantity = entry.quantity
                )
            )
        }
    }

    fun getTotalCaloriesForDate(dateMillis: Long, allItems: List<MealPlanItem>): Int {
        val dateKey = DateUtils.formatPlanKey(dateMillis)
        return allItems.filter { it.dateKey == dateKey }.sumOf { it.calories }
    }
}
