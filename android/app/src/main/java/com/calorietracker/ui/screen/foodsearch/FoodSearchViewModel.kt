package com.calorietracker.ui.screen.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.repository.FoodLogRepository
import com.calorietracker.data.repository.FoodSearchRepository
import com.calorietracker.data.repository.FoodSearchResult
import com.calorietracker.data.repository.MealPlanRepository
import com.calorietracker.data.model.MealPlanItem
import com.calorietracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FoodSearchUiState(
    val query: String = "",
    val results: List<FoodSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val selected: FoodSearchResult? = null,
    val servingGrams: Float = 100f,
    val selectedMealType: String = "",
    val logSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val foodSearchRepository: FoodSearchRepository,
    private val foodLogRepository: FoodLogRepository,
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodSearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(results = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _uiState.update { it.copy(isLoading = true) }
            val results = foodSearchRepository.search(query)
            _uiState.update { it.copy(results = results, isLoading = false) }
        }
    }

    fun selectResult(result: FoodSearchResult) {
        _uiState.update { it.copy(selected = result, servingGrams = 100f) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selected = null) }
    }

    fun setServingGrams(grams: Float) {
        _uiState.update { it.copy(servingGrams = grams) }
    }

    fun setMealType(mealType: String) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun logSelected() {
        val state = _uiState.value
        val item = state.selected ?: return
        val factor = state.servingGrams / 100f
        val mealType = state.selectedMealType.ifEmpty { DateUtils.inferMealType(System.currentTimeMillis()) }

        viewModelScope.launch {
            foodLogRepository.insert(
                FoodLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    foodName = item.foodName,
                    calories = (item.caloriesPer100g * factor).toInt(),
                    protein = item.proteinPer100g * factor,
                    carbs = item.carbsPer100g * factor,
                    fat = item.fatPer100g * factor,
                    quantity = "${state.servingGrams.toInt()}g",
                    source = "fooddb",
                    mealType = mealType
                )
            )
            _uiState.update { it.copy(logSuccess = true) }
        }
    }

    fun addToPlan(dateKey: String, mealType: String) {
        val state = _uiState.value
        val item = state.selected ?: return
        val factor = state.servingGrams / 100f
        val mt = mealType.ifEmpty { state.selectedMealType.ifEmpty { "Lunch" } }
        viewModelScope.launch {
            mealPlanRepository.insert(
                MealPlanItem(
                    id = UUID.randomUUID().toString(),
                    dateKey = dateKey,
                    mealType = mt,
                    foodName = item.foodName,
                    calories = (item.caloriesPer100g * factor).toInt(),
                    protein = item.proteinPer100g * factor,
                    carbs = item.carbsPer100g * factor,
                    fat = item.fatPer100g * factor,
                    quantity = "${state.servingGrams.toInt()}g"
                )
            )
            _uiState.update { it.copy(logSuccess = true) }
        }
    }
}
