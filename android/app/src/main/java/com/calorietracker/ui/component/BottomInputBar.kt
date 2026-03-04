package com.calorietracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calorietracker.ui.theme.*

@Composable
fun BottomInputBar(
    modifier: Modifier = Modifier,
    onTextSubmit: (String) -> Unit = {},
    onCameraClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onRecipesClick: () -> Unit = {},
    onBarcodeClick: () -> Unit = {},
    onTemplatesClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Neutral900.copy(alpha = 0.97f),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Action icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCameraClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Neutral400, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onRecipesClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Recipes", tint = Neutral400, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onBarcodeClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = Neutral400, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onTemplatesClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Templates", tint = Amber400, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onSearchClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Neutral400, modifier = Modifier.size(22.dp))
                }
            }

            // Text input + voice row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Neutral800,
                    onClick = { onTextSubmit("") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Type, paste URL...",
                            color = Neutral500,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Rose500.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Rose500, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
