package com.calorietracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.calorietracker.data.db.dao.*
import com.calorietracker.data.db.entity.*

@Database(
    entities = [
        FoodLogEntity::class,
        RecipeEntity::class,
        WeightLogEntity::class,
        ChatMessageEntity::class,
        MealTemplateEntity::class,
        MealPlanItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun recipeDao(): RecipeDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun mealPlanDao(): MealPlanDao
}
