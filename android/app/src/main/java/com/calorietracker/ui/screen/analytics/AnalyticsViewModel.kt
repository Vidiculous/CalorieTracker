package com.calorietracker.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.WeightLog
import com.calorietracker.data.preferences.AppSettings
import com.calorietracker.data.preferences.SettingsDataStore
import com.calorietracker.data.repository.FoodLogRepository
import com.calorietracker.data.repository.WeightRepository
import com.calorietracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class DayCalories(val label: String, val calories: Int, val isOverGoal: Boolean)
data class DayProtein(val label: String, val protein: Float)
data class TopFood(val name: String, val count: Int, val rank: Int)

data class AnalyticsUiState(
    val settings: AppSettings = AppSettings(),
    val weeklyCalories: List<DayCalories> = emptyList(),
    val weeklyProtein: List<DayProtein> = emptyList(),
    val avgCalories: Int = 0,
    val avgProtein: Int = 0,
    val totalLogs: Int = 0,
    val bestDay: String = "—",
    val topFoods: List<TopFood> = emptyList(),
    val weightLogs: List<WeightLog> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val foodLogRepository: FoodLogRepository,
    private val weightRepository: WeightRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        settingsDataStore.settings,
        foodLogRepository.getAllLogs(),
        weightRepository.getAllWeightLogs()
    ) { settings, allLogs, weightLogs ->
        computeState(settings, allLogs, weightLogs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    private fun computeState(
        settings: AppSettings,
        allLogs: List<FoodLogEntry>,
        weightLogs: List<WeightLog>
    ): AnalyticsUiState {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // Build 7-day data
        val weeklyCalories = (6 downTo 0).map { daysAgo ->
            val dayMillis = DateUtils.addDays(today, -daysAgo)
            val dayLogs = allLogs.filter { DateUtils.isSameDay(it.timestamp, dayMillis) }
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val label = dayLabels[(dayOfWeek + 5) % 7] // Convert Calendar Sunday=1 to Mon=0
            DayCalories(label, dayLogs.sumOf { it.calories }, dayLogs.sumOf { it.calories } > settings.dailyGoal)
        }

        val weeklyProtein = (6 downTo 0).map { daysAgo ->
            val dayMillis = DateUtils.addDays(today, -daysAgo)
            val dayLogs = allLogs.filter { DateUtils.isSameDay(it.timestamp, dayMillis) }
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val label = dayLabels[(dayOfWeek + 5) % 7]
            DayProtein(label, dayLogs.sumOf { it.protein.toDouble() }.toFloat())
        }

        val activeDays = weeklyCalories.count { it.calories > 0 }
        val avgCalories = if (activeDays > 0) weeklyCalories.sumOf { it.calories } / activeDays else 0
        val avgProtein = if (activeDays > 0) weeklyProtein.sumOf { it.protein.toDouble() }.toInt() / activeDays else 0

        // Best day = closest to goal
        val bestDay = weeklyCalories
            .filter { it.calories > 0 }
            .minByOrNull { Math.abs(it.calories - settings.dailyGoal) }?.label ?: "—"

        // Top foods by frequency
        val topFoods = allLogs
            .groupBy { it.foodName }
            .entries
            .sortedByDescending { it.value.size }
            .take(5)
            .mapIndexed { i, (name, logs) -> TopFood(name, logs.size, i + 1) }

        return AnalyticsUiState(
            settings = settings,
            weeklyCalories = weeklyCalories,
            weeklyProtein = weeklyProtein,
            avgCalories = avgCalories,
            avgProtein = avgProtein,
            totalLogs = allLogs.size,
            bestDay = bestDay,
            topFoods = topFoods,
            weightLogs = weightLogs.sortedBy { it.date }
        )
    }
}
