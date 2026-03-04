package com.calorietracker.data.preferences

data class AppSettings(
    val dailyGoal: Int = 2500,
    val proteinGoal: Int = 150,
    val carbsGoal: Int = 250,
    val fatGoal: Int = 70,
    val currentWeight: Float = 0f,
    val goalWeight: Float = 0f,
    val hasOnboarded: Boolean = false,
    val selectedModel: String = "gemini-2.0-flash-exp",
    val apiKey: String = "",
    val autoSubmit: Boolean = true
)
