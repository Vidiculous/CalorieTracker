package com.calorietracker.util

import com.calorietracker.data.model.FoodLogEntry

data class MacroTotals(
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f
)

object NutritionUtils {
    fun getTotals(logs: List<FoodLogEntry>): MacroTotals = MacroTotals(
        calories = logs.sumOf { it.calories },
        protein = logs.sumOf { it.protein.toDouble() }.toFloat(),
        carbs = logs.sumOf { it.carbs.toDouble() }.toFloat(),
        fat = logs.sumOf { it.fat.toDouble() }.toFloat()
    )

    fun groupByMealType(logs: List<FoodLogEntry>): Map<String, List<FoodLogEntry>> {
        val order = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
        return logs.groupBy { it.mealType }
            .entries
            .sortedBy { order.indexOf(it.key).let { i -> if (i == -1) 99 else i } }
            .associate { it.key to it.value }
    }

    fun scaleMacros(
        calories: Float, protein: Float, carbs: Float, fat: Float,
        portions: Float, servings: Float
    ): MacroTotals {
        val factor = if (servings > 0) portions / servings else 1f
        return MacroTotals(
            calories = (calories * factor).toInt(),
            protein = protein * factor,
            carbs = carbs * factor,
            fat = fat * factor
        )
    }
}
