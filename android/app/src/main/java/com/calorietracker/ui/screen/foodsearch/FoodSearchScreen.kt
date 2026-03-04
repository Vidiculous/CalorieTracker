package com.calorietracker.ui.screen.foodsearch

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.data.repository.FoodSearchResult
import com.calorietracker.ui.component.MEAL_TYPES
import com.calorietracker.ui.screen.onboarding.outlinedTextFieldColors
import com.calorietracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    viewModel: FoodSearchViewModel,
    returnToChat: Boolean,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.logSuccess) {
        if (state.logSuccess) onBack()
    }

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = { Text("Food Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Neutral900, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (state.selected == null) {
                // Search view
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search foods (e.g. oats, chicken)...", color = Neutral500) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Neutral400) },
                    trailingIcon = {
                        if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Rose500, strokeWidth = 2.dp)
                    },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true
                )

                if (state.results.isEmpty() && state.query.isBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Start typing to search the food database", color = Neutral500, fontSize = 14.sp)
                    }
                } else if (state.results.isEmpty() && !state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results found", color = Neutral500, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.results) { result ->
                            FoodResultItem(result = result, onClick = { viewModel.selectResult(result) })
                        }
                    }
                }
            } else {
                // Detail view
                val item = state.selected!!
                val factor = state.servingGrams / 100f

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    IconButton(onClick = viewModel::clearSelection) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text(item.foodName, color = Color.White, fontWeight = FontWeight.Bold)
                        item.brand?.let { Text(it, color = Neutral400, fontSize = 13.sp) }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Neutral800),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Serving Size", color = Neutral400, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.servingGrams.toInt().toString(),
                            onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.setServingGrams(v) } },
                            suffix = { Text("g", color = Neutral400) },
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            NutritionStat("Calories", "${(item.caloriesPer100g * factor).toInt()}", "kcal", Color.White)
                            NutritionStat("Protein", "${(item.proteinPer100g * factor).toInt()}", "g", Blue400)
                            NutritionStat("Carbs", "${(item.carbsPer100g * factor).toInt()}", "g", Amber400)
                            NutritionStat("Fat", "${(item.fatPer100g * factor).toInt()}", "g", Rose400)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("Meal Type", color = Neutral400, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MEAL_TYPES.forEach { type ->
                        FilterChip(
                            selected = state.selectedMealType == type,
                            onClick = { viewModel.setMealType(if (state.selectedMealType == type) "" else type) },
                            label = { Text(type, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Rose500,
                                selectedLabelColor = Color.White,
                                containerColor = Neutral800,
                                labelColor = Neutral400
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = viewModel::logSelected,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Log ${(item.caloriesPer100g * factor).toInt()} kcal",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodResultItem(result: FoodSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(result.foodName, color = Color.White, fontWeight = FontWeight.Medium)
                result.brand?.let { Text(it, color = Neutral500, fontSize = 12.sp) }
                Text(
                    "P:${result.proteinPer100g.toInt()}g · C:${result.carbsPer100g.toInt()}g · F:${result.fatPer100g.toInt()}g",
                    color = Neutral400, fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${result.caloriesPer100g.toInt()} kcal", color = Color.White, fontWeight = FontWeight.Bold)
                Text("/ 100g", color = Neutral500, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun NutritionStat(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(unit, color = Neutral400, fontSize = 11.sp)
        Text(label, color = Neutral500, fontSize = 11.sp)
    }
}
