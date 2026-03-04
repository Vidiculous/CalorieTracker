package com.calorietracker.data.db

import androidx.room.TypeConverter
import com.calorietracker.data.model.FoodItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, FoodItem::class.java)
    private val adapter = moshi.adapter<List<FoodItem>>(type)

    @TypeConverter
    fun fromFoodItemList(items: List<FoodItem>?): String =
        adapter.toJson(items ?: emptyList())

    @TypeConverter
    fun toFoodItemList(json: String?): List<FoodItem> =
        if (json.isNullOrBlank() || json == "[]") emptyList()
        else adapter.fromJson(json) ?: emptyList()
}
