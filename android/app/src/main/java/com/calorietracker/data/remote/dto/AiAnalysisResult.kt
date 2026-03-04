package com.calorietracker.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiAnalysisResult(
    val type: String = "conversation", // log | recipe | update | clarification | conversation
    @Json(name = "status_message") val statusMessage: String? = null,
    @Json(name = "update_target_id") val updateTargetId: String? = null,
    val transcription: String? = null,
    val explanation: String? = null,
    @Json(name = "meal_type") val mealType: String? = null,
    @Json(name = "meal_name") val mealName: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val items: List<AiFoodItem>? = null,
    @Json(name = "recipe_details") val recipeDetails: AiRecipeDetails? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class AiFoodItem(
    @Json(name = "food_name") val foodName: String = "",
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    @Json(name = "quantity_desc") val quantityDesc: String = "",
    val confidence: String = "high"
)

@JsonClass(generateAdapter = true)
data class AiRecipeDetails(
    val servings: Float = 1f,
    @Json(name = "prep_time") val prepTime: String? = null,
    val instructions: String? = null
)
