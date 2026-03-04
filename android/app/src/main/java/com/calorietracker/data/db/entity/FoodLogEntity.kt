package com.calorietracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val foodName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val quantity: String,
    val source: String,
    val mealType: String,
    val itemsJson: String = "[]"
)
