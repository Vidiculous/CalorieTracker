package com.calorietracker.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodItem(
    val id: String = "",
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val quantityDesc: String = "",
    val confidence: String = "high" // high | medium | low
)
