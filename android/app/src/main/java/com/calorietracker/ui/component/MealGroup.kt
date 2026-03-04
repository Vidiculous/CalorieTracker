package com.calorietracker.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.ui.theme.*

@Composable
fun MealGroup(
    mealType: String,
    logs: List<FoodLogEntry>,
    onEdit: (FoodLogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCalories = logs.sumOf { it.calories }
    val totalProtein = logs.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = logs.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = logs.sumOf { it.fat.toDouble() }.toFloat()

    val (icon, color) = mealIcon(mealType)

    var expanded by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                mealType.uppercase(),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$totalCalories kcal · P:${totalProtein.toInt()}g C:${totalCarbs.toInt()}g F:${totalFat.toInt()}g",
                color = Neutral400,
                fontSize = 11.sp
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                logs.forEach { entry ->
                    FoodLogItem(entry = entry, onEdit = onEdit)
                }
            }
        }
    }
}

private fun mealIcon(mealType: String): Pair<ImageVector, Color> = when (mealType) {
    "Breakfast" -> Icons.Default.LightMode to MealBreakfast
    "Lunch" -> Icons.Default.Restaurant to MealLunch
    "Dinner" -> Icons.Default.DinnerDining to MealDinner
    else -> Icons.Default.Cookie to MealSnacks
}
