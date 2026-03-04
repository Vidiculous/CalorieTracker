package com.calorietracker.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.MealPlanItem
import com.calorietracker.data.model.MealTemplate
import com.calorietracker.data.model.WeightLog
import com.calorietracker.data.preferences.AppSettings
import com.calorietracker.data.preferences.SettingsDataStore
import com.calorietracker.data.repository.*
import com.calorietracker.util.DateUtils
import com.calorietracker.util.NutritionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DashboardUiState(
    val currentDateMillis: Long = System.currentTimeMillis(),
    val settings: AppSettings = AppSettings(),
    val groupedLogs: Map<String, List<FoodLogEntry>> = emptyMap(),
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val streak: Int = 0,
    val plannedMeals: Map<String, List<MealPlanItem>> = emptyMap(),
    val todayWeight: Float? = null,
    val recentFoodNames: List<FoodLogEntry> = emptyList(),
    val showManualEntry: Boolean = false,
    val showWeightInput: Boolean = false,
    val editingEntry: FoodLogEntry? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val foodLogRepository: FoodLogRepository,
    private val weightRepository: WeightRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _currentDateMillis = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        _currentDateMillis,
        settingsDataStore.settings,
        foodLogRepository.getAllLogs(),
        weightRepository.getAllWeightLogs(),
        mealPlanRepository.getAllPlanItems()
    ) { dateMillis, settings, allLogs, allWeightLogs, allPlanItems ->
        val dateKey = DateUtils.formatPlanKey(dateMillis)
        val todayLogs = allLogs.filter { DateUtils.isSameDay(it.timestamp, dateMillis) }
        val totals = NutritionUtils.getTotals(todayLogs)
        val grouped = NutritionUtils.groupByMealType(todayLogs)
        val streak = DateUtils.calculateStreak(allLogs.map { it.timestamp })
        val planForDate = allPlanItems.filter { it.dateKey == dateKey }
        val plannedGrouped = planForDate.groupBy { it.mealType }
        val todayWeight = allWeightLogs.firstOrNull { DateUtils.isSameDay(it.date, dateMillis) }?.weight

        DashboardUiState(
            currentDateMillis = dateMillis,
            settings = settings,
            groupedLogs = grouped,
            totalCalories = totals.calories,
            totalProtein = totals.protein,
            totalCarbs = totals.carbs,
            totalFat = totals.fat,
            streak = streak,
            plannedMeals = plannedGrouped,
            todayWeight = todayWeight
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private val _showManualEntry = MutableStateFlow(false)
    val showManualEntry = _showManualEntry.asStateFlow()

    private val _showWeightInput = MutableStateFlow(false)
    val showWeightInput = _showWeightInput.asStateFlow()

    private val _editingEntry = MutableStateFlow<FoodLogEntry?>(null)
    val editingEntry = _editingEntry.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<FoodLogEntry>>(emptyList())
    val recentLogs = _recentLogs.asStateFlow()

    init {
        viewModelScope.launch {
            foodLogRepository.getAllLogs().collect { logs ->
                _recentLogs.value = logs.take(50)
            }
        }
    }

    fun previousDay() {
        _currentDateMillis.value = DateUtils.addDays(_currentDateMillis.value, -1)
    }

    fun nextDay() {
        _currentDateMillis.value = DateUtils.addDays(_currentDateMillis.value, 1)
    }

    fun showManualEntry() { _showManualEntry.value = true }
    fun hideManualEntry() { _showManualEntry.value = false }
    fun showWeightInput() { _showWeightInput.value = true }
    fun hideWeightInput() { _showWeightInput.value = false }
    fun editEntry(entry: FoodLogEntry) { _editingEntry.value = entry }
    fun clearEditingEntry() { _editingEntry.value = null }

    fun addLog(entry: FoodLogEntry) {
        viewModelScope.launch {
            // Stamp the log to the currently viewed date
            val stamped = entry.copy(
                timestamp = _currentDateMillis.value + (System.currentTimeMillis() % 86400000)
            )
            foodLogRepository.insert(stamped)
        }
    }

    fun updateLog(entry: FoodLogEntry) {
        viewModelScope.launch { foodLogRepository.update(entry) }
    }

    fun deleteLog(id: String) {
        viewModelScope.launch { foodLogRepository.deleteById(id) }
    }

    fun logWeight(weight: Float) {
        viewModelScope.launch {
            val dateMillis = _currentDateMillis.value
            val existing = weightRepository.getForDate(dateMillis)
            val log = WeightLog(
                id = existing?.id ?: UUID.randomUUID().toString(),
                date = dateMillis,
                weight = weight
            )
            weightRepository.insert(log)
            settingsDataStore.updateSettings { copy(currentWeight = weight) }
        }
    }

    fun saveTemplate(name: String, items: List<com.calorietracker.data.model.FoodItem>) {
        viewModelScope.launch {
            mealTemplateRepository.insert(
                com.calorietracker.data.model.MealTemplate(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    items = items
                )
            )
        }
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
}
