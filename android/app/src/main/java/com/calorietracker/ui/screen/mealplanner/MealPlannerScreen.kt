package com.calorietracker.ui.screen.mealplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.MealPlanItem
import com.calorietracker.data.model.MealTemplate
import com.calorietracker.data.model.Recipe
import com.calorietracker.ui.theme.*
import com.calorietracker.util.DateUtils
import com.calorietracker.util.NutritionUtils

private val MEAL_TYPES = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(
    viewModel: MealPlannerViewModel,
    onNavigateToFoodSearch: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val recentEntries by viewModel.recentEntries.collectAsStateWithLifecycle()
    var addSheetForMealType by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Indigo400)
                        Text("Meal Planner", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Neutral900, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.weekDays) { dayMillis ->
                    val isSelected = DateUtils.isSameDay(dayMillis, state.selectedDateMillis)
                    val isToday = DateUtils.isSameDay(dayMillis, System.currentTimeMillis())
                    val plannedCal = state.planItems
                        .filter { it.dateKey == DateUtils.formatPlanKey(dayMillis) }
                        .sumOf { it.calories }

                    DayChip(
                        dayMillis = dayMillis,
                        isSelected = isSelected,
                        isToday = isToday,
                        plannedCal = plannedCal,
                        onClick = { viewModel.selectDate(dayMillis) }
                    )
                }
            }

            Text(
                text = DateUtils.formatDisplayDate(state.selectedDateMillis),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MEAL_TYPES) { mealType ->
                    val mealItems = state.planItems.filter { it.mealType == mealType }
                    MealSlot(
                        mealType = mealType,
                        items = mealItems,
                        dateKey = DateUtils.formatPlanKey(state.selectedDateMillis),
                        onLogMeal = { viewModel.logPlannedMeal(DateUtils.formatPlanKey(state.selectedDateMillis), mealType) },
                        onRemoveItem = { id -> viewModel.removePlanItem(id) },
                        onAddItem = { addSheetForMealType = mealType }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    addSheetForMealType?.let { mealType ->
        val dateKey = DateUtils.formatPlanKey(state.selectedDateMillis)
        AddToPlanSheet(
            mealType = mealType,
            recipes = recipes,
            templates = templates,
            recentEntries = recentEntries,
            onAddRecipe = { recipe, portions -> viewModel.addRecipeToPlan(recipe, portions, mealType, dateKey) },
            onAddTemplate = { template -> viewModel.addTemplateToPlan(template, mealType, dateKey) },
            onAddEntry = { entry -> viewModel.addEntryToPlan(entry, mealType, dateKey) },
            onFoodSearch = { addSheetForMealType = null; onNavigateToFoodSearch() },
            onDismiss = { addSheetForMealType = null }
        )
    }
}

@Composable
private fun DayChip(
    dayMillis: Long,
    isSelected: Boolean,
    isToday: Boolean,
    plannedCal: Int,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Indigo400 else Neutral800
    val textColor = if (isSelected) Color.White else Neutral400

    Surface(
        modifier = Modifier.width(60.dp).clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isToday) Text("Today", color = if (isSelected) Color.White else Indigo400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(DateUtils.formatDayOfWeek(dayMillis), color = textColor, fontSize = 11.sp)
            Text(
                text = DateUtils.formatMonthDay(dayMillis).split(" ").last(),
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
            if (plannedCal > 0) {
                Text("${plannedCal}kcal", color = Amber400, fontSize = 9.sp)
            } else {
                Text("—", color = Neutral600, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MealSlot(
    mealType: String,
    items: List<MealPlanItem>,
    dateKey: String,
    onLogMeal: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddItem: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(mealType, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (items.isNotEmpty()) {
                        Text("${items.sumOf { it.calories }} kcal", color = Neutral400, fontSize = 12.sp)
                    }
                }
                if (items.isNotEmpty()) {
                    Button(
                        onClick = onLogMeal,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Log Now", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.foodName, color = Neutral300, fontSize = 13.sp)
                        Text(item.quantity, color = Neutral500, fontSize = 11.sp)
                    }
                    Text("${item.calories} kcal", color = Neutral400, fontSize = 13.sp)
                    IconButton(onClick = { onRemoveItem(item.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Neutral600, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            OutlinedButton(
                onClick = onAddItem,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral400),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Neutral700)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Item", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlanSheet(
    mealType: String,
    recipes: List<Recipe>,
    templates: List<MealTemplate>,
    recentEntries: List<FoodLogEntry>,
    onAddRecipe: (Recipe, Float) -> Unit,
    onAddTemplate: (MealTemplate) -> Unit,
    onAddEntry: (FoodLogEntry) -> Unit,
    onFoodSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Recipes", "Templates", "Recent")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Neutral900,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Neutral600) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add to $mealType",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Neutral400)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Indigo400,
                            selectedLabelColor = Color.White,
                            containerColor = Neutral800,
                            labelColor = Neutral400
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    if (recipes.isEmpty()) {
                        Text("No recipes saved yet.", color = Neutral500, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        recipes.forEach { recipe ->
                            RecipePickerRow(recipe = recipe, onAdd = { portions -> onAddRecipe(recipe, portions) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                1 -> {
                    if (templates.isEmpty()) {
                        Text("No templates saved yet.", color = Neutral500, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        templates.forEach { template ->
                            TemplatePickerRow(template = template, onAdd = { onAddTemplate(template) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                2 -> {
                    if (recentEntries.isEmpty()) {
                        Text("No recent entries.", color = Neutral500, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        recentEntries.forEach { entry ->
                            RecentEntryPickerRow(entry = entry, onAdd = { onAddEntry(entry) })
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onFoodSearch,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral400),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Neutral700)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Search Food Database", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RecipePickerRow(recipe: Recipe, onAdd: (Float) -> Unit) {
    var portions by remember { mutableFloatStateOf(1f) }
    val scaled = NutritionUtils.scaleMacros(recipe.calories, recipe.protein, recipe.carbs, recipe.fat, portions, recipe.servings)

    Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${scaled.calories} kcal", color = Neutral400, fontSize = 12.sp)
            }
            IconButton(onClick = { if (portions > 0.5f) portions -= 0.5f }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Text("$portions", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(28.dp))
            IconButton(onClick = { portions += 0.5f }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Button(
                onClick = { onAdd(portions) },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo400),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TemplatePickerRow(template: MealTemplate, onAdd: () -> Unit) {
    val totalCal = template.items.sumOf { it.calories }
    Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${template.items.size} items · $totalCal kcal", color = Neutral400, fontSize = 12.sp)
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo400),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RecentEntryPickerRow(entry: FoodLogEntry, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.foodName, color = Color.White, fontSize = 14.sp)
                Text("${entry.quantity} · ${entry.calories} kcal", color = Neutral400, fontSize = 12.sp)
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo400),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
