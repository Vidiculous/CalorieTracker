package com.calorietracker.di

import android.content.Context
import androidx.room.Room
import com.calorietracker.data.db.AppDatabase
import com.calorietracker.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "calorie_tracker.db")
            .build()

    @Provides fun provideFoodLogDao(db: AppDatabase): FoodLogDao = db.foodLogDao()
    @Provides fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()
    @Provides fun provideWeightLogDao(db: AppDatabase): WeightLogDao = db.weightLogDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideMealTemplateDao(db: AppDatabase): MealTemplateDao = db.mealTemplateDao()
    @Provides fun provideMealPlanDao(db: AppDatabase): MealPlanDao = db.mealPlanDao()
}
