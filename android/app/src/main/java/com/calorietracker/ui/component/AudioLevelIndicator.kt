package com.calorietracker.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calorietracker.ui.theme.Rose500
import kotlin.random.Random

@Composable
fun AudioLevelIndicator(audioLevel: Float, modifier: Modifier = Modifier) {
    val barCount = 7
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { i ->
            val baseHeight = 8
            val maxExtra = 24
            val factor = if (i == barCount / 2) 1f else (1f - (Math.abs(i - barCount / 2).toFloat() / barCount) * 0.6f)
            val targetHeight = baseHeight + (maxExtra * audioLevel * factor).toInt()

            val animatedHeight by animateIntAsState(
                targetValue = targetHeight,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .background(Rose500.copy(alpha = 0.7f + 0.3f * factor), RoundedCornerShape(2.dp))
            )
        }
    }
}
