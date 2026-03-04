package com.calorietracker.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.ui.screen.onboarding.AVAILABLE_MODELS
import com.calorietracker.ui.screen.onboarding.outlinedTextFieldColors
import com.calorietracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showApiKey by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    // Local editable state synchronized to settings
    var dailyGoal by remember(settings.dailyGoal) { mutableStateOf(settings.dailyGoal.toString()) }
    var proteinGoal by remember(settings.proteinGoal) { mutableStateOf(settings.proteinGoal.toString()) }
    var carbsGoal by remember(settings.carbsGoal) { mutableStateOf(settings.carbsGoal.toString()) }
    var fatGoal by remember(settings.fatGoal) { mutableStateOf(settings.fatGoal.toString()) }
    var currentWeight by remember(settings.currentWeight) { mutableStateOf(settings.currentWeight.toString()) }
    var goalWeight by remember(settings.goalWeight) { mutableStateOf(settings.goalWeight.toString()) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Neutral900,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Calorie Goals") {
                NumberField("Daily Calorie Goal", dailyGoal, "kcal") { dailyGoal = it }
                NumberField("Protein Goal", proteinGoal, "g") { proteinGoal = it }
                NumberField("Carbs Goal", carbsGoal, "g") { carbsGoal = it }
                NumberField("Fat Goal", fatGoal, "g") { fatGoal = it }
            }

            SettingsSection("Body Weight") {
                NumberField("Current Weight", currentWeight, "kg") { currentWeight = it }
                NumberField("Goal Weight", goalWeight, "kg") { goalWeight = it }
            }

            SettingsSection("AI Configuration") {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "Hide" else "Show", color = Rose500)
                        }
                    },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = AVAILABLE_MODELS.find { it.first == settings.selectedModel }?.second ?: settings.selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("AI Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                        modifier = Modifier.background(Neutral800)
                    ) {
                        AVAILABLE_MODELS.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = {
                                    viewModel.updateSettings { copy(selectedModel = id) }
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-submit voice", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.autoSubmit,
                        onCheckedChange = { viewModel.updateSettings { copy(autoSubmit = it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Rose500, checkedTrackColor = Rose500.copy(alpha = 0.5f))
                    )
                }
            }

            Button(
                onClick = {
                    val s = settings
                    viewModel.updateSettings {
                        copy(
                            dailyGoal = dailyGoal.toIntOrNull() ?: s.dailyGoal,
                            proteinGoal = proteinGoal.toIntOrNull() ?: s.proteinGoal,
                            carbsGoal = carbsGoal.toIntOrNull() ?: s.carbsGoal,
                            fatGoal = fatGoal.toIntOrNull() ?: s.fatGoal,
                            currentWeight = currentWeight.toFloatOrNull() ?: s.currentWeight,
                            goalWeight = goalWeight.toFloatOrNull() ?: s.goalWeight,
                            apiKey = apiKey
                        )
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose500)
            ) {
                Text("Save Settings", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Neutral400,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, suffix: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label) },
        suffix = { Text(suffix, color = Neutral400) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = outlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
