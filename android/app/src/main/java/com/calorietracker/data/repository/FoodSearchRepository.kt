package com.calorietracker.data.repository

import com.calorietracker.data.remote.OpenFoodFactsApi
import com.calorietracker.data.remote.dto.OFFProduct
import javax.inject.Inject
import javax.inject.Singleton

data class FoodSearchResult(
    val foodName: String,
    val brand: String?,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float
)

@Singleton
class FoodSearchRepository @Inject constructor(private val api: OpenFoodFactsApi) {

    suspend fun search(query: String): List<FoodSearchResult> {
        return try {
            val response = api.searchProducts(query)
            response.products
                ?.filter { it.productName != null && it.nutriments?.kcalPer100g != null }
                ?.map { it.toSearchResult() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getByBarcode(barcode: String): FoodSearchResult? {
        return try {
            val response = api.getProductByBarcode(barcode)
            if (response.status == 1) response.product?.toSearchResult() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun OFFProduct.toSearchResult() = FoodSearchResult(
        foodName = productName ?: "Unknown",
        brand = brands?.takeIf { it.isNotBlank() },
        caloriesPer100g = nutriments?.kcalPer100g ?: 0f,
        proteinPer100g = nutriments?.proteinPer100g ?: 0f,
        carbsPer100g = nutriments?.carbsPer100g ?: 0f,
        fatPer100g = nutriments?.fatPer100g ?: 0f
    )
}
