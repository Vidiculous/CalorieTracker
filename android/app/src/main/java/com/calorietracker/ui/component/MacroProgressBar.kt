package com.calorietracker.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorietracker.ui.theme.Neutral700

@Composable
fun MacroCard(
    label: String,
    current: Float,
    goal: Int,
    unit: String = "g",
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "macro_$label"
    )

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            Text(
                "${current.toInt()} / ${goal}$unit",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Neutral700)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun MacroGrid(
    proteinCurrent: Float, proteinGoal: Int,
    carbsCurrent: Float, carbsGoal: Int,
    fatCurrent: Float, fatGoal: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MacroCard(
            label = "Protein",
            current = proteinCurrent,
            goal = proteinGoal,
            color = com.calorietracker.ui.theme.Blue400,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            label = "Carbs",
            current = carbsCurrent,
            goal = carbsGoal,
            color = com.calorietracker.ui.theme.Amber400,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            label = "Fat",
            current = fatCurrent,
            goal = fatGoal,
            color = com.calorietracker.ui.theme.Rose400,
            modifier = Modifier.weight(1f)
        )
    }
}
