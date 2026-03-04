package com.calorietracker.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.MealTemplate
import com.calorietracker.data.repository.FoodLogRepository
import com.calorietracker.data.repository.MealTemplateRepository
import com.calorietracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MealTemplatesViewModel @Inject constructor(
    private val mealTemplateRepository: MealTemplateRepository,
    private val foodLogRepository: FoodLogRepository
) : ViewModel() {

    val templates: StateFlow<List<MealTemplate>> = mealTemplateRepository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEntries: StateFlow<List<FoodLogEntry>> = foodLogRepository.getAllLogs()
        .map { it.take(50) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTemplate(id: String) {
        viewModelScope.launch { mealTemplateRepository.deleteById(id) }
    }

    fun logTemplate(template: MealTemplate) {
        viewModelScope.launch {
            val mealType = DateUtils.inferMealType(System.currentTimeMillis())
            template.items.forEach { item ->
                foodLogRepository.insert(
                    FoodLogEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        foodName = item.foodName,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        quantity = item.quantityDesc,
                        source = "template",
                        mealType = mealType
                    )
                )
            }
        }
    }

    fun createTemplateFromEntries(name: String, entries: List<FoodLogEntry>) {
        if (name.isBlank() || entries.isEmpty()) return
        viewModelScope.launch {
            val items = entries.map { entry ->
                FoodItem(
                    id = UUID.randomUUID().toString(),
                    foodName = entry.foodName,
                    calories = entry.calories,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    quantityDesc = entry.quantity,
                    confidence = "high"
                )
            }
            mealTemplateRepository.insert(
                MealTemplate(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    items = items
                )
            )
        }
    }
}
