package com.calorietracker.data.model

data class MealPlanItem(
    val id: String,
    val dateKey: String, // "Mon Mar 03 2026"
    val mealType: String, // Breakfast | Lunch | Dinner | Snacks
    val foodName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val quantity: String
)
