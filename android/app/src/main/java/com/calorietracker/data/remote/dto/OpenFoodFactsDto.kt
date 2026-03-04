package com.calorietracker.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OFFProductResponse(
    val status: Int = 0,
    val product: OFFProduct?
)

@JsonClass(generateAdapter = true)
data class OFFSearchResponse(
    val products: List<OFFProduct>?
)

@JsonClass(generateAdapter = true)
data class OFFProduct(
    @Json(name = "product_name") val productName: String?,
    val brands: String?,
    val nutriments: OFFNutriments?
)

@JsonClass(generateAdapter = true)
data class OFFNutriments(
    @Json(name = "energy-kcal_100g") val kcalPer100g: Float?,
    @Json(name = "proteins_100g") val proteinPer100g: Float?,
    @Json(name = "carbohydrates_100g") val carbsPer100g: Float?,
    @Json(name = "fat_100g") val fatPer100g: Float?
)
