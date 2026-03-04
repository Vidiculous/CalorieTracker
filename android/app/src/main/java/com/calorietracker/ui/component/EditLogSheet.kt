package com.calorietracker.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.ui.screen.onboarding.outlinedTextFieldColors
import com.calorietracker.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogSheet(
    entry: FoodLogEntry,
    onSave: (FoodLogEntry) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val isComposite = entry.items.isNotEmpty()
    var foodName by remember(entry) { mutableStateOf(entry.foodName) }
    var quantity by remember(entry) { mutableStateOf(entry.quantity) }
    var selectedMealType by remember(entry) { mutableStateOf(entry.mealType) }
    var multiplier by remember(entry) { mutableStateOf("1") }

    // Base values locked at open time — used for multiplier scaling
    val baseCalories = remember(entry) { entry.calories.toFloat() }
    val baseProtein = remember(entry) { entry.protein }
    val baseCarbs = remember(entry) { entry.carbs }
    val baseFat = remember(entry) { entry.fat }

    // Single-item fields
    var calories by remember(entry) { mutableStateOf(entry.calories.toString()) }
    var protein by remember(entry) { mutableStateOf(entry.protein.toString()) }
    var carbs by remember(entry) { mutableStateOf(entry.carbs.toString()) }
    var fat by remember(entry) { mutableStateOf(entry.fat.toString()) }

    // Composite sub-items (mutable)
    var subItems by remember(entry) { mutableStateOf(entry.items.toMutableList()) }

    val totalCalories = if (isComposite) subItems.sumOf { it.calories } else calories.toIntOrNull() ?: 0
    val totalProtein = if (isComposite) subItems.sumOf { it.protein.toDouble() }.toFloat() else protein.toFloatOrNull() ?: 0f
    val totalCarbs = if (isComposite) subItems.sumOf { it.carbs.toDouble() }.toFloat() else carbs.toFloatOrNull() ?: 0f
    val totalFat = if (isComposite) subItems.sumOf { it.fat.toDouble() }.toFloat() else fat.toFloatOrNull() ?: 0f

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
                Text("Edit Entry", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Neutral400)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name") },
                colors = outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("e.g. 100g, 2 dl, 1 msk", color = Neutral500) },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                if (!isComposite) {
                    OutlinedTextField(
                        value = multiplier,
                        onValueChange = { v ->
                            multiplier = v.filter { it.isDigit() || it == '.' }
                            val m = multiplier.toFloatOrNull() ?: return@OutlinedTextField
                            if (m > 0f) {
                                calories = (baseCalories * m).roundToInt().coerceAtLeast(0).toString()
                                protein = "%.1f".format((baseProtein * m).coerceAtLeast(0f))
                                carbs = "%.1f".format((baseCarbs * m).coerceAtLeast(0f))
                                fat = "%.1f".format((baseFat * m).coerceAtLeast(0f))
                            }
                        },
                        label = { Text("×") },
                        placeholder = { Text("1", color = Neutral500) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Meal type selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MEAL_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedMealType == type,
                        onClick = { selectedMealType = type },
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

            // Totals display
            Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TotalChip("KCAL", totalCalories.toString(), Color.White)
                    TotalChip("PROT", "${totalProtein.toInt()}g", Blue400)
                    TotalChip("CARBS", "${totalCarbs.toInt()}g", Amber400)
                    TotalChip("FAT", "${totalFat.toInt()}g", Rose400)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!isComposite) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditNutritionField("Calories", calories, "kcal", Modifier.weight(1f)) { calories = it }
                    EditNutritionField("Protein", protein, "g", Modifier.weight(1f)) { protein = it }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditNutritionField("Carbs", carbs, "g", Modifier.weight(1f)) { carbs = it }
                    EditNutritionField("Fat", fat, "g", Modifier.weight(1f)) { fat = it }
                }
            } else {
                Text("Items", color = Neutral400, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
                subItems.forEachIndexed { index, item ->
                    SubItemEditor(
                        item = item,
                        onUpdate = { updated -> subItems = subItems.toMutableList().also { it[index] = updated } },
                        onDelete = { subItems = subItems.toMutableList().also { it.removeAt(index) } }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Red400)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val saved = entry.copy(
                            foodName = foodName,
                            quantity = quantity,
                            mealType = selectedMealType,
                            calories = totalCalories,
                            protein = totalProtein,
                            carbs = totalCarbs,
                            fat = totalFat,
                            items = if (isComposite) subItems else emptyList()
                        )
                        onSave(saved)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", color = Neutral900, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TotalChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Neutral500, style = MaterialTheme.typography.labelSmall)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditNutritionField(label: String, value: String, suffix: String, modifier: Modifier, onValueChange: (String) -> Unit) {
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

@Composable
private fun SubItemEditor(item: FoodItem, onUpdate: (FoodItem) -> Unit, onDelete: () -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.foodName) }
    var cal by remember(item.id) { mutableStateOf(item.calories.toString()) }
    var prot by remember(item.id) { mutableStateOf(item.protein.toString()) }
    var cb by remember(item.id) { mutableStateOf(item.carbs.toString()) }
    var ft by remember(item.id) { mutableStateOf(item.fat.toString()) }

    Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onUpdate(item.copy(foodName = it)) },
                    label = { Text("Item name") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = Red400, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("kcal", cal) { v: String -> cal = v; onUpdate(item.copy(foodName = name, calories = v.toIntOrNull() ?: 0)) },
                    Triple("P(g)", prot) { v: String -> prot = v; onUpdate(item.copy(foodName = name, protein = v.toFloatOrNull() ?: 0f)) },
                    Triple("C(g)", cb) { v: String -> cb = v; onUpdate(item.copy(foodName = name, carbs = v.toFloatOrNull() ?: 0f)) },
                    Triple("F(g)", ft) { v: String -> ft = v; onUpdate(item.copy(foodName = name, fat = v.toFloatOrNull() ?: 0f)) }
                ).forEach { (label, value, handler) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { handler(it.filter { c -> c.isDigit() || c == '.' }) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}
