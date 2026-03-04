package com.calorietracker.data.model

data class Recipe(
    val id: String,
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servings: Float,
    val description: String?,
    val items: List<FoodItem>,
    val createdAt: Long
)
