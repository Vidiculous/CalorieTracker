package com.calorietracker.data.model

data class FoodLogEntry(
    val id: String,
    val timestamp: Long,
    val foodName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val quantity: String,
    val source: String, // ai | manual | recipe | barcode | template | plan | fooddb
    val mealType: String, // Breakfast | Lunch | Dinner | Snacks
    val items: List<FoodItem> = emptyList()
)
