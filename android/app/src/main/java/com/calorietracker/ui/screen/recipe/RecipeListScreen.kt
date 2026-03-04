package com.calorietracker.ui.screen.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.model.Recipe
import com.calorietracker.ui.component.MEAL_TYPES
import com.calorietracker.ui.theme.*
import com.calorietracker.util.NutritionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onBack: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Rose500)
                        Column {
                            Text("Recipe Book", fontWeight = FontWeight.Bold)
                            Text("Fast logging for saved meals", color = Neutral400, fontSize = 12.sp)
                        }
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
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Neutral600, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No recipes yet", color = Neutral500, fontSize = 16.sp)
                    Text("Ask the AI to save a recipe for you!", color = Neutral600, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeItem(
                        recipe = recipe,
                        onDelete = { viewModel.deleteRecipe(recipe.id) },
                        onUpdateServings = { newServings -> viewModel.updateServings(recipe, newServings) },
                        onUpdateItems = { items -> viewModel.updateItems(recipe, items) },
                        onLog = { portions, mealType -> viewModel.logRecipe(recipe, portions, mealType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeItem(
    recipe: Recipe,
    onDelete: () -> Unit,
    onUpdateServings: (Float) -> Unit,
    onUpdateItems: (List<FoodItem>) -> Unit,
    onLog: (Float, String) -> Unit
) {
    var portions by remember { mutableFloatStateOf(1f) }
    var selectedMealType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var editingServings by remember { mutableStateOf(false) }
    var servingsInput by remember { mutableStateOf(recipe.servings.toString()) }

    // Editable ingredients state — reset when recipe updates from DB
    var editedItems by remember(recipe.id, recipe.items) { mutableStateOf(recipe.items) }
    var editingIngredients by remember { mutableStateOf(false) }

    // Live batch totals from edited items when editing, otherwise from recipe
    val batchCalories = if (editingIngredients) editedItems.sumOf { it.calories }.toFloat() else recipe.calories
    val batchProtein = if (editingIngredients) editedItems.sumOf { it.protein.toDouble() }.toFloat() else recipe.protein
    val batchCarbs = if (editingIngredients) editedItems.sumOf { it.carbs.toDouble() }.toFloat() else recipe.carbs
    val batchFat = if (editingIngredients) editedItems.sumOf { it.fat.toDouble() }.toFloat() else recipe.fat

    val scaled = NutritionUtils.scaleMacros(batchCalories, batchProtein, batchCarbs, batchFat, portions, recipe.servings)

    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(recipe.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Batch: ${batchCalories.toInt()} kcal | ", color = Neutral400, fontSize = 12.sp)
                        if (editingServings) {
                            OutlinedTextField(
                                value = servingsInput,
                                onValueChange = { servingsInput = it },
                                modifier = Modifier.width(60.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Rose500,
                                    unfocusedBorderColor = Neutral700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )
                            IconButton(onClick = {
                                servingsInput.toFloatOrNull()?.let { onUpdateServings(it) }
                                editingServings = false
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Text("${recipe.servings} serv.", color = Neutral400, fontSize = 12.sp)
                            IconButton(onClick = { editingServings = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Neutral600, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Surface(color = Rose500.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "${scaled.calories} kcal",
                        color = Rose500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Portions stepper + meal selector + log button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log Amount", color = Neutral400, fontSize = 12.sp)
                IconButton(onClick = { if (portions > 0.5f) portions -= 0.5f }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Less", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text("$portions", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(30.dp))
                IconButton(onClick = { portions += 0.5f }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "More", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Neutral600, modifier = Modifier.size(16.dp))
                }
            }

            // Meal type chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MEAL_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedMealType == type,
                        onClick = { selectedMealType = if (selectedMealType == type) "" else type },
                        label = { Text(type, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Rose500,
                            selectedLabelColor = Color.White,
                            containerColor = Neutral700,
                            labelColor = Neutral400
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onLog(portions, selectedMealType) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log", fontWeight = FontWeight.SemiBold)
            }

            // Expandable / editable ingredients
            if (recipe.items.isNotEmpty()) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) "Hide ingredients" else "Edit ingredients",
                        color = Neutral400,
                        fontSize = 12.sp
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Neutral400,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (expanded) {
                    Spacer(Modifier.height(4.dp))

                    editedItems.forEachIndexed { index, item ->
                        IngredientEditorRow(
                            item = item,
                            onUpdate = { updated ->
                                editedItems = editedItems.toMutableList().also { it[index] = updated }
                                editingIngredients = true
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (editingIngredients) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    editedItems = recipe.items
                                    editingIngredients = false
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral400),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Neutral700)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Discard", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    onUpdateItems(editedItems)
                                    editingIngredients = false
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save (${batchCalories.toInt()} kcal)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientEditorRow(item: FoodItem, onUpdate: (FoodItem) -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.foodName) }
    var qty by remember(item.id) { mutableStateOf(item.quantityDesc) }
    var cal by remember(item.id) { mutableStateOf(item.calories.toString()) }
    var prot by remember(item.id) { mutableStateOf(item.protein.toString()) }
    var carbs by remember(item.id) { mutableStateOf(item.carbs.toString()) }
    var fat by remember(item.id) { mutableStateOf(item.fat.toString()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Rose500,
        unfocusedBorderColor = Neutral700,
        focusedLabelColor = Rose500,
        unfocusedLabelColor = Neutral600,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Rose500,
        focusedContainerColor = Neutral800,
        unfocusedContainerColor = Neutral800
    )

    fun commit() = onUpdate(
        item.copy(
            foodName = name,
            quantityDesc = qty,
            calories = cal.toIntOrNull() ?: item.calories,
            protein = prot.toFloatOrNull() ?: item.protein,
            carbs = carbs.toFloatOrNull() ?: item.carbs,
            fat = fat.toFloatOrNull() ?: item.fat
        )
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Name + quantity row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; commit() },
                    label = { Text("Ingredient", fontSize = 11.sp) },
                    colors = fieldColors,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it; commit() },
                    label = { Text("Qty", fontSize = 11.sp) },
                    colors = fieldColors,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
            // Macro row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("kcal", cal) { v: String -> cal = v.filter { it.isDigit() }; commit() },
                    Triple("P(g)", prot) { v: String -> prot = v.filter { it.isDigit() || it == '.' }; commit() },
                    Triple("C(g)", carbs) { v: String -> carbs = v.filter { it.isDigit() || it == '.' }; commit() },
                    Triple("F(g)", fat) { v: String -> fat = v.filter { it.isDigit() || it == '.' }; commit() }
                ).forEach { (label, value, handler) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = handler,
                        label = { Text(label, fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}
