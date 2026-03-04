package com.calorietracker.ui.screen.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.calorietracker.data.model.MealTemplate
import com.calorietracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTemplatesScreen(
    viewModel: MealTemplatesViewModel,
    onBack: () -> Unit
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val recentEntries by viewModel.recentEntries.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Amber400)
                        Text("Meal Templates", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Neutral900, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = Amber400,
                contentColor = Neutral900
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Template")
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No templates saved yet", color = Neutral500, fontSize = 16.sp)
                    Text("Tap + to create one from recent entries", color = Neutral600, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onDelete = { viewModel.deleteTemplate(template.id) },
                        onLog = { viewModel.logTemplate(template) }
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateTemplateSheet(
            recentEntries = recentEntries,
            onCreate = { name, entries -> viewModel.createTemplateFromEntries(name, entries) },
            onDismiss = { showCreateSheet = false }
        )
    }
}

@Composable
private fun TemplateCard(template: MealTemplate, onDelete: () -> Unit, onLog: () -> Unit) {
    val totalCal = template.items.sumOf { it.calories }
    val totalProt = template.items.sumOf { it.protein.toDouble() }.toInt()
    val totalCarbs = template.items.sumOf { it.carbs.toDouble() }.toInt()
    val totalFat = template.items.sumOf { it.fat.toDouble() }.toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${template.items.size} items", color = Neutral500, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${totalCal} kcal", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("P:${totalProt}g", color = Blue400, fontSize = 12.sp)
                    Text("C:${totalCarbs}g", color = Amber400, fontSize = 12.sp)
                    Text("F:${totalFat}g", color = Rose400, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onLog,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber400),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Now", color = Neutral900, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Neutral600, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTemplateSheet(
    recentEntries: List<FoodLogEntry>,
    onCreate: (name: String, entries: List<FoodLogEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<FoodLogEntry>() }

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
                Text("New Template", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Neutral400)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Amber400,
                    unfocusedBorderColor = Neutral700,
                    focusedLabelColor = Amber400,
                    unfocusedLabelColor = Neutral500,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Amber400,
                    focusedContainerColor = Neutral800,
                    unfocusedContainerColor = Neutral800
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Text("Select recent entries", color = Neutral400, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))

            if (recentEntries.isEmpty()) {
                Text("No recent entries found.", color = Neutral500, fontSize = 13.sp)
            } else {
                recentEntries.forEach { entry ->
                    val isSelected = entry in selected
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Neutral700 else Neutral800
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selected.add(entry) else selected.remove(entry)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Amber400,
                                    uncheckedColor = Neutral600,
                                    checkmarkColor = Neutral900
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.foodName, color = Color.White, fontSize = 14.sp)
                                Text("${entry.quantity} · ${entry.calories} kcal", color = Neutral500, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (templateName.isNotBlank() && selected.isNotEmpty()) {
                        onCreate(templateName, selected.toList())
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (templateName.isNotBlank() && selected.isNotEmpty()) Amber400 else Neutral700
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = templateName.isNotBlank() && selected.isNotEmpty()
            ) {
                Text(
                    "Save Template (${selected.size} items)",
                    color = if (templateName.isNotBlank() && selected.isNotEmpty()) Neutral900 else Neutral500,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
