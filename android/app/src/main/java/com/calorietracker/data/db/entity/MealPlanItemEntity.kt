package com.calorietracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plan_items")
data class MealPlanItemEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val mealType: String,
    val foodName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val quantity: String
)
