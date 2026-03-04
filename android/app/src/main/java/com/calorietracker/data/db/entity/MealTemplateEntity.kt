package com.calorietracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val itemsJson: String = "[]"
)
