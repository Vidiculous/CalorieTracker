package com.calorietracker.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.ui.theme.*

@Composable
fun FoodLogItem(
    entry: FoodLogEntry,
    onEdit: (FoodLogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSubItems = entry.items.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (hasSubItems) expanded = !expanded else onEdit(entry) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.foodName,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.quantity.isNotBlank()) {
                        Text(
                            text = entry.quantity,
                            color = Neutral400,
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroBadge("P", entry.protein.toInt(), Blue400)
                        MacroBadge("C", entry.carbs.toInt(), Amber400)
                        MacroBadge("F", entry.fat.toInt(), Rose400)
                    }
                }

                Text(
                    text = "${entry.calories}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = " kcal",
                    color = Neutral400,
                    fontSize = 12.sp
                )

                if (hasSubItems) {
                    IconButton(onClick = { onEdit(entry) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Neutral400, modifier = Modifier.size(16.dp))
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Neutral400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded && hasSubItems) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 8.dp)) {
                    entry.items.forEach { subItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(subItem.foodName, color = Neutral400, fontSize = 13.sp)
                                if (subItem.quantityDesc.isNotBlank()) {
                                    Text(subItem.quantityDesc, color = Neutral500, fontSize = 11.sp)
                                }
                            }
                            Text("${subItem.calories} kcal", color = Neutral400, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBadge(label: String, value: Int, color: Color) {
    Text(
        text = "$label:${value}g",
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}
