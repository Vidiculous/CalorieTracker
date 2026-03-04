package com.calorietracker.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Chat : Screen("chat?mode={mode}") {
        fun withMode(mode: String) = "chat?mode=$mode"
        fun default() = "chat?mode=text"
    }
    object Analytics : Screen("analytics")
    object RecipeList : Screen("recipes")
    object BarcodeScanner : Screen("barcode")
    object FoodSearch : Screen("foodsearch?returnToChat={returnToChat}") {
        fun open(returnToChat: Boolean = false) = "foodsearch?returnToChat=$returnToChat"
    }
    object MealPlanner : Screen("mealplanner")
    object Settings : Screen("settings")
    object MealTemplates : Screen("templates")
}
