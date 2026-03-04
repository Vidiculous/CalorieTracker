package com.calorietracker.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.sp
import com.calorietracker.data.remote.dto.AiFoodItem
import com.calorietracker.ui.screen.onboarding.outlinedTextFieldColors
import com.calorietracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmLogSheet(
    items: List<AiFoodItem>,
    mealType: String,
    onLogAll: (List<AiFoodItem>) -> Unit,
    onDiscard: () -> Unit
) {
    var editableItems by remember(items) { mutableStateOf(items) }

    ModalBottomSheet(
        onDismissRequest = onDiscard,
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
                Column {
                    Text("Review Detected Items", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    if (mealType.isNotBlank()) Text(mealType, color = Neutral400, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDiscard) {
                    Icon(Icons.Default.Close, contentDescription = "Discard", tint = Neutral400)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Edit values before logging. Items with 🔴 low confidence may need adjustment.",
                color = Neutral400,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            editableItems.forEachIndexed { index, item ->
                ConfirmItemCard(
                    item = item,
                    onUpdate = { updated -> editableItems = editableItems.toMutableList().also { it[index] = updated } },
                    onDelete = { editableItems = editableItems.toMutableList().also { it.removeAt(index) } }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Footer summary
            val totalKcal = editableItems.sumOf { it.calories }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${editableItems.size} items · $mealType", color = Neutral400, fontSize = 13.sp)
                Text("$totalKcal kcal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral400),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Neutral700)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = { onLogAll(editableItems.filter { it.calories > 0 }) },
                    enabled = editableItems.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log All", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ConfirmItemCard(item: AiFoodItem, onUpdate: (AiFoodItem) -> Unit, onDelete: () -> Unit) {
    var cal by remember(item.foodName) { mutableStateOf(item.calories.toString()) }
    var prot by remember(item.foodName) { mutableStateOf(item.protein.toString()) }
    var cb by remember(item.foodName) { mutableStateOf(item.carbs.toString()) }
    var ft by remember(item.foodName) { mutableStateOf(item.fat.toString()) }

    val confidenceColor = when (item.confidence) {
        "high" -> Emerald400
        "medium" -> Amber400
        else -> Red400
    }
    val confidenceEmoji = when (item.confidence) {
        "high" -> "🟢"
        "medium" -> "🟡"
        else -> "🔴"
    }

    Card(colors = CardDefaults.cardColors(containerColor = Neutral800), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.foodName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (item.quantityDesc.isNotBlank()) Text(item.quantityDesc, color = Neutral400, fontSize = 12.sp)
                    Text("$confidenceEmoji ${item.confidence.replaceFirstChar { it.uppercaseChar() }}", color = confidenceColor, fontSize = 11.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Neutral500, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = cal,
                    onValueChange = { cal = it.filter { c -> c.isDigit() }; onUpdate(item.copy(calories = it.toIntOrNull() ?: 0)) },
                    label = { Text("kcal", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prot,
                    onValueChange = { prot = it.filter { c -> c.isDigit() || c == '.' }; onUpdate(item.copy(protein = it.toFloatOrNull() ?: 0f)) },
                    label = { Text("P(g)", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = cb,
                    onValueChange = { cb = it.filter { c -> c.isDigit() || c == '.' }; onUpdate(item.copy(carbs = it.toFloatOrNull() ?: 0f)) },
                    label = { Text("C(g)", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ft,
                    onValueChange = { ft = it.filter { c -> c.isDigit() || c == '.' }; onUpdate(item.copy(fat = it.toFloatOrNull() ?: 0f)) },
                    label = { Text("F(g)", style = MaterialTheme.typography.labelSmall) },
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
