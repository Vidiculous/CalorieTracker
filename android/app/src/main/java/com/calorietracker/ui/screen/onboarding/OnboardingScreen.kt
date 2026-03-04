package com.calorietracker.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.ui.theme.*

val AVAILABLE_MODELS = listOf(
    "gemini-2.0-flash-exp" to "Gemini 2.0 Flash (Recommended)",
    "gemini-2.5-flash" to "Gemini 2.5 Flash",
    "gemini-3-flash-preview" to "Gemini 3 Flash Preview"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    var dailyGoal by remember { mutableStateOf("2500") }
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(AVAILABLE_MODELS[0].first) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Flame icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(listOf(Rose500.copy(alpha = 0.3f), Neutral900)),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Rose500,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Let's set up your calorie tracker",
            style = MaterialTheme.typography.bodyMedium,
            color = Neutral400,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(40.dp))

        // Daily calorie goal
        OutlinedTextField(
            value = dailyGoal,
            onValueChange = { dailyGoal = it.filter { c -> c.isDigit() } },
            label = { Text("Daily Calorie Goal") },
            suffix = { Text("kcal", color = Neutral400) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = outlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        // API Key
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

        Spacer(Modifier.height(16.dp))

        // Model selector
        ExposedDropdownMenuBox(
            expanded = modelExpanded,
            onExpandedChange = { modelExpanded = it }
        ) {
            OutlinedTextField(
                value = AVAILABLE_MODELS.find { it.first == selectedModel }?.second ?: selectedModel,
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
                        text = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { selectedModel = id; modelExpanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // Get Started button
        Button(
            onClick = {
                viewModel.completeOnboarding(
                    dailyGoal = dailyGoal.toIntOrNull() ?: 2500,
                    apiKey = apiKey,
                    model = selectedModel
                )
                onComplete()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Rose500)
        ) {
            Text("Get Started", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Rose500,
    unfocusedBorderColor = Neutral700,
    focusedLabelColor = Rose500,
    unfocusedLabelColor = Neutral500,
    cursorColor = Rose500,
    focusedTextColor = androidx.compose.ui.graphics.Color.White,
    unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
    focusedContainerColor = Neutral800,
    unfocusedContainerColor = Neutral800
)
