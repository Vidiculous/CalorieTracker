package com.calorietracker.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.FoodItem
import com.calorietracker.ui.screen.onboarding.outlinedTextFieldColors
import com.calorietracker.ui.theme.*
import java.util.UUID

val MEAL_TYPES = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    suggestions: List<FoodLogEntry>,
    onAdd: (FoodLogEntry) -> Unit,
    onSaveAsTemplate: (String, List<FoodItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("") }
    var templateSaved by remember { mutableStateOf(false) }

    val recents = remember(suggestions) { suggestions.take(12) }

    val filteredSuggestions = remember(foodName, suggestions) {
        if (foodName.length < 2) emptyList()
        else suggestions.filter { it.foodName.contains(foodName, ignoreCase = true) }.take(5)
    }

    fun fillFromEntry(entry: FoodLogEntry) {
        foodName = entry.foodName
        calories = entry.calories.toString()
        protein = entry.protein.toString()
        carbs = entry.carbs.toString()
        fat = entry.fat.toString()
        if (entry.mealType.isNotEmpty()) selectedMealType = entry.mealType
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Neutral900,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Neutral600) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Rose500)
                    Spacer(Modifier.width(8.dp))
                    Text("Manual Entry", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Neutral400)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Food name with inline autocomplete
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name") },
                colors = outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Autocomplete dropdown (when typing)
            if (filteredSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Neutral800),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    filteredSuggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fillFromEntry(suggestion) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(suggestion.foodName, color = Color.White, fontSize = 14.sp)
                                Text("${suggestion.quantity} · ${suggestion.calories} kcal", color = Neutral500, fontSize = 12.sp)
                            }
                            Icon(
                                Icons.Default.NorthWest,
                                contentDescription = null,
                                tint = Neutral600,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        HorizontalDivider(color = Neutral800.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }

            // Recents list (shown only when field is empty)
            if (foodName.isEmpty() && recents.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Recent", color = Neutral500, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))

                // Show up to 8 recents inline; sheet itself scrolls via ModalBottomSheet
                recents.take(8).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fillFromEntry(entry) }
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.foodName, color = Neutral300, fontSize = 14.sp)
                            Text(
                                buildString {
                                    if (entry.quantity.isNotEmpty()) append("${entry.quantity} · ")
                                    append("${entry.calories} kcal")
                                },
                                color = Neutral500,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            Icons.Default.NorthWest,
                            contentDescription = "Use this entry",
                            tint = Neutral600,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    HorizontalDivider(color = Neutral800, thickness = 0.5.dp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Meal type selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MEAL_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedMealType == type,
                        onClick = { selectedMealType = if (selectedMealType == type) "" else type },
                        label = { Text(type, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
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

            Spacer(Modifier.height(12.dp))

            // Nutrition inputs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NutritionField("Calories", calories, "kcal", Modifier.weight(1f)) { calories = it }
                NutritionField("Protein", protein, "g", Modifier.weight(1f)) { protein = it }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NutritionField("Carbs", carbs, "g", Modifier.weight(1f)) { carbs = it }
                NutritionField("Fat", fat, "g", Modifier.weight(1f)) { fat = it }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (foodName.isNotBlank() && calories.isNotBlank()) {
                        onAdd(
                            FoodLogEntry(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                foodName = foodName,
                                calories = calories.toIntOrNull() ?: 0,
                                protein = protein.toFloatOrNull() ?: 0f,
                                carbs = carbs.toFloatOrNull() ?: 0f,
                                fat = fat.toFloatOrNull() ?: 0f,
                                quantity = "",
                                source = "manual",
                                mealType = selectedMealType.ifEmpty { "Snacks" }
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = foodName.isNotBlank() && calories.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Entry", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    if (foodName.isNotBlank()) {
                        onSaveAsTemplate(foodName, listOf(
                            FoodItem(
                                id = UUID.randomUUID().toString(),
                                foodName = foodName,
                                calories = calories.toIntOrNull() ?: 0,
                                protein = protein.toFloatOrNull() ?: 0f,
                                carbs = carbs.toFloatOrNull() ?: 0f,
                                fat = fat.toFloatOrNull() ?: 0f
                            )
                        ))
                        templateSaved = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (templateSaved) Emerald400 else Amber400),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (templateSaved) Emerald400 else Amber400)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (templateSaved) "Template Saved!" else "Save as Template", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NutritionField(label: String, value: String, suffix: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        suffix = { Text(suffix, color = Neutral400, style = MaterialTheme.typography.bodySmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = outlinedTextFieldColors(),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
