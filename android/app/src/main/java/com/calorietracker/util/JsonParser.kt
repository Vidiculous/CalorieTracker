package com.calorietracker.util

import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.remote.dto.AiAnalysisResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonParser @Inject constructor() {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val foodItemListType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
    private val foodItemListAdapter = moshi.adapter<List<FoodItem>>(foodItemListType)
    private val aiResultAdapter = moshi.adapter(AiAnalysisResult::class.java)

    fun parseFoodItems(json: String): List<FoodItem> =
        runCatching { foodItemListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())

    fun serializeFoodItems(items: List<FoodItem>): String =
        foodItemListAdapter.toJson(items)

    fun parseAiResult(rawText: String): AiAnalysisResult {
        val cleaned = cleanJson(rawText)
        return runCatching { aiResultAdapter.fromJson(cleaned) ?: AiAnalysisResult(error = "Parse error") }
            .getOrDefault(AiAnalysisResult(error = "Invalid JSON response"))
    }

    private fun cleanJson(text: String): String {
        val raw = text.replace("```json", "").replace("```", "").trim()
        val first = raw.indexOf('{')
        val last = raw.lastIndexOf('}')
        return if (first != -1 && last != -1) raw.substring(first, last + 1) else raw
    }
}
