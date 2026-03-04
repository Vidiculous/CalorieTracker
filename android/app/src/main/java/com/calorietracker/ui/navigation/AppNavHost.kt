package com.calorietracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calorietracker.ui.screen.analytics.AnalyticsScreen
import com.calorietracker.ui.screen.barcode.BarcodeScannerScreen
import com.calorietracker.ui.screen.chat.ChatScreen
import com.calorietracker.ui.screen.dashboard.DashboardScreen
import com.calorietracker.ui.screen.foodsearch.FoodSearchScreen
import com.calorietracker.ui.screen.mealplanner.MealPlannerScreen
import com.calorietracker.ui.screen.onboarding.OnboardingScreen
import com.calorietracker.ui.screen.onboarding.OnboardingViewModel
import com.calorietracker.ui.screen.recipe.RecipeListScreen
import com.calorietracker.ui.screen.settings.SettingsScreen
import com.calorietracker.ui.screen.templates.MealTemplatesScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val hasOnboarded by onboardingViewModel.hasOnboarded.collectAsState()

    val startDestination = if (hasOnboarded) Screen.Dashboard.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = hiltViewModel(),
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("mode") {
                type = NavType.StringType
                defaultValue = "text"
            })
        ) { backStack ->
            ChatScreen(
                viewModel = hiltViewModel(),
                initialMode = backStack.arguments?.getString("mode") ?: "text",
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecipeList.route) {
            RecipeListScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BarcodeScanner.route) {
            BarcodeScannerScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FoodSearch.route,
            arguments = listOf(navArgument("returnToChat") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStack ->
            FoodSearchScreen(
                viewModel = hiltViewModel(),
                returnToChat = backStack.arguments?.getBoolean("returnToChat") ?: false,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MealPlanner.route) {
            MealPlannerScreen(
                viewModel = hiltViewModel(),
                onNavigateToFoodSearch = { navController.navigate(Screen.FoodSearch.open(false)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MealTemplates.route) {
            MealTemplatesScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
