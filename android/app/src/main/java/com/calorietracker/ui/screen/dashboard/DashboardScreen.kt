package com.calorietracker.ui.screen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.ui.component.*
import com.calorietracker.ui.navigation.Screen
import com.calorietracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showManualEntry by viewModel.showManualEntry.collectAsStateWithLifecycle()
    val showWeightInput by viewModel.showWeightInput.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()

    val settings = state.settings
    val remaining = settings.dailyGoal - state.totalCalories
    val isOver = state.totalCalories > settings.dailyGoal

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CalorieTracker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = Brush.linearGradient(listOf(Rose500, Orange500))
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Neutral900)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DateNavigator(
                        currentDateMillis = state.currentDateMillis,
                        streak = state.streak,
                        onPrevious = viewModel::previousDay,
                        onNext = viewModel::nextDay
                    )
                }

                // Calorie ring + remaining badge
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CalorieProgressRing(
                            calories = state.totalCalories,
                            goal = settings.dailyGoal
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = if (isOver) Red500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = if (isOver) "${-remaining} kcal Over Limit!" else "$remaining kcal left",
                                color = if (isOver) Red400 else Emerald400,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Macro grid
                item {
                    MacroGrid(
                        proteinCurrent = state.totalProtein, proteinGoal = settings.proteinGoal,
                        carbsCurrent = state.totalCarbs, carbsGoal = settings.carbsGoal,
                        fatCurrent = state.totalFat, fatGoal = settings.fatGoal
                    )
                }

                // Macro suggestion card
                if (state.totalCalories > 0) {
                    item {
                        MacroSuggestionCard(
                            remaining = remaining,
                            proteinRemaining = settings.proteinGoal - state.totalProtein.toInt(),
                            carbsRemaining = settings.carbsGoal - state.totalCarbs.toInt(),
                            fatRemaining = settings.fatGoal - state.totalFat.toInt()
                        )
                    }
                }

                // Action grid
                item {
                    ActionGrid(
                        items = listOf(
                            ActionItem("Food", Icons.Default.Restaurant, Emerald500, onClick = viewModel::showManualEntry),
                            ActionItem(
                                "Weight",
                                Icons.Default.MonitorWeight,
                                Purple400,
                                badge = state.todayWeight?.let { "${it}kg" },
                                onClick = viewModel::showWeightInput
                            ),
                            ActionItem("Stats", Icons.Default.BarChart, Amber400, onClick = { onNavigate(Screen.Analytics.route) }),
                            ActionItem("Plan", Icons.Default.CalendarMonth, Indigo400, onClick = { onNavigate(Screen.MealPlanner.route) })
                        )
                    )
                }

                // Planned meals
                if (state.plannedMeals.isNotEmpty()) {
                    item {
                        PlannedMealsSection(
                            plannedMeals = state.plannedMeals,
                            dateKey = com.calorietracker.util.DateUtils.formatPlanKey(state.currentDateMillis),
                            onLogMeal = { mealType ->
                                viewModel.logPlannedMeal(
                                    com.calorietracker.util.DateUtils.formatPlanKey(state.currentDateMillis),
                                    mealType
                                )
                            }
                        )
                    }
                }

                // Food logs section header
                item {
                    if (state.groupedLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No food logged for this day", color = Neutral500)
                        }
                    }
                }

                // Grouped food logs
                state.groupedLogs.forEach { (mealType, logs) ->
                    item {
                        MealGroup(
                            mealType = mealType,
                            logs = logs,
                            onEdit = { entry -> viewModel.editEntry(entry) }
                        )
                    }
                }

                // Bottom padding for BottomInputBar (two-row bar ~170dp + nav bar)
                item { Spacer(Modifier.height(180.dp)) }
            }

            // Floating bottom input bar
            BottomInputBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onTextSubmit = { text ->
                    onNavigate(Screen.Chat.withMode("text"))
                },
                onCameraClick = { onNavigate(Screen.Chat.withMode("camera")) },
                onVoiceClick = { onNavigate(Screen.Chat.withMode("voice")) },
                onRecipesClick = { onNavigate(Screen.RecipeList.route) },
                onBarcodeClick = { onNavigate(Screen.BarcodeScanner.route) },
                onTemplatesClick = { onNavigate(Screen.MealTemplates.route) },
                onSearchClick = { onNavigate(Screen.FoodSearch.open()) }
            )
        }
    }

    // Sheets / Dialogs
    if (showManualEntry) {
        ManualEntrySheet(
            suggestions = recentLogs,
            onAdd = { entry -> viewModel.addLog(entry) },
            onSaveAsTemplate = { name, items -> viewModel.saveTemplate(name, items) },
            onDismiss = viewModel::hideManualEntry
        )
    }

    if (showWeightInput) {
        WeightInputDialog(
            currentWeight = state.todayWeight ?: state.settings.currentWeight,
            onSave = { weight -> viewModel.logWeight(weight); viewModel.hideWeightInput() },
            onDismiss = viewModel::hideWeightInput
        )
    }

    editingEntry?.let { entry ->
        EditLogSheet(
            entry = entry,
            onSave = { updated -> viewModel.updateLog(updated) },
            onDelete = { viewModel.deleteLog(entry.id) },
            onDismiss = viewModel::clearEditingEntry
        )
    }
}

@Composable
private fun MacroSuggestionCard(
    remaining: Int,
    proteinRemaining: Int,
    carbsRemaining: Int,
    fatRemaining: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Neutral500, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = buildRemainingText(remaining, proteinRemaining, carbsRemaining, fatRemaining),
                color = Neutral400,
                fontSize = 12.sp
            )
        }
    }
}

private fun buildRemainingText(remaining: Int, protein: Int, carbs: Int, fat: Int): String {
    val parts = mutableListOf<String>()
    if (remaining > 0) parts.add("$remaining kcal left")
    if (protein > 0) parts.add("Protein ${protein}g short") else if (protein <= 0) parts.add("Protein on track")
    if (carbs > 0) parts.add("Carbs ${carbs}g short") else if (carbs <= 0) parts.add("Carbs on track")
    if (fat > 0) parts.add("Fat ${fat}g short") else if (fat <= 0) parts.add("Fat on track")
    return parts.joinToString(" · ")
}

@Composable
private fun PlannedMealsSection(
    plannedMeals: Map<String, List<com.calorietracker.data.model.MealPlanItem>>,
    dateKey: String,
    onLogMeal: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Indigo400, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Planned Meals", color = Indigo400, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = Neutral400, modifier = Modifier.size(18.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plannedMeals.forEach { (mealType, items) ->
                    Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(mealType, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Button(
                                    onClick = { onLogMeal(mealType) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Log Meal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.foodName, color = Neutral400, fontSize = 13.sp)
                                    Text("${item.calories} kcal", color = Neutral500, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
