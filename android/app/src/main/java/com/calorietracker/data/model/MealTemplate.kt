package com.calorietracker.data.model

data class MealTemplate(
    val id: String,
    val name: String,
    val items: List<FoodItem>
)
