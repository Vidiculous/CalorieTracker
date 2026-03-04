package com.calorietracker.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.ui.theme.Neutral800
import com.calorietracker.ui.theme.Rose500
import com.calorietracker.ui.screen.onboarding.outlinedTextFieldColors

@Composable
fun WeightInputDialog(
    currentWeight: Float,
    onSave: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var weightInput by remember { mutableStateOf(if (currentWeight > 0) currentWeight.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neutral800,
        title = { Text("Log Weight", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                    suffix = { Text("kg", color = Color.White.copy(alpha = 0.6f), fontSize = 24.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    weightInput.toFloatOrNull()?.let { onSave(it) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
