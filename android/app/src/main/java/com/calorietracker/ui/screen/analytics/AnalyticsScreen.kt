package com.calorietracker.ui.screen.analytics

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.ui.theme.*
import com.calorietracker.util.DateUtils
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.*
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.shader.DynamicShader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = Purple400)
                        Column {
                            Text("Insights", fontWeight = FontWeight.Bold)
                            Text("Your weekly performance", color = Neutral400, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Neutral900, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 4-stat grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Default.LocalFireDepartment, "Avg Calories", "${state.avgCalories}", "Goal: ${state.settings.dailyGoal}", Rose500, Modifier.weight(1f))
                StatCard(Icons.Default.FitnessCenter, "Avg Protein", "${state.avgProtein}g", "Goal: ${state.settings.proteinGoal}g", Blue400, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Default.Restaurant, "Total Logs", "${state.totalLogs}", "Lifetime", Amber400, Modifier.weight(1f))
                StatCard(Icons.Default.EmojiEvents, "Best Day", state.bestDay, "Closest to Goal", Emerald400, Modifier.weight(1f))
            }

            // Calorie trend bar chart
            SectionCard(title = "Calorie Trend (7 Days)") {
                if (state.weeklyCalories.isNotEmpty()) {
                    CalorieTrendChart(state)
                }
            }

            // Protein trend
            SectionCard(title = "Protein Consistency") {
                if (state.weeklyProtein.isNotEmpty()) {
                    ProteinTrendChart(state)
                }
            }

            // Top favorites
            if (state.topFoods.isNotEmpty()) {
                SectionCard(title = "Top Favorites") {
                    state.topFoods.forEach { food ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#${food.rank} · ${food.name}", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("${food.count}x", color = Neutral400, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Weight history
            if (state.weightLogs.isNotEmpty()) {
                SectionCard(title = "Weight History") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current: ${state.settings.currentWeight}kg", color = Purple400, fontSize = 13.sp)
                        Text("Goal: ${state.settings.goalWeight}kg", color = Emerald400, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    WeightHistoryChart(state)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CalorieTrendChart(state: AnalyticsUiState) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(state.weeklyCalories) {
        modelProducer.runTransaction {
            columnSeries { series(state.weeklyCalories.map { it.calories }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    state.weeklyCalories.map { day ->
                        LineComponent(
                            color = (if (day.isOverGoal) Red500 else Emerald500).toArgb(),
                            thicknessDp = 24f,
                            shape = com.patrykandpatrick.vico.core.common.shape.CorneredShape.rounded(4)
                        )
                    }
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    state.weeklyCalories.getOrNull(value.toInt())?.label ?: ""
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(180.dp)
    )
}

@Composable
private fun ProteinTrendChart(state: AnalyticsUiState) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(state.weeklyProtein) {
        modelProducer.runTransaction {
            lineSeries { series(state.weeklyProtein.map { it.protein }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = remember { LineCartesianLayer.LineFill.single(fill(Blue400)) },
                        areaFill = remember {
                            LineCartesianLayer.AreaFill.single(
                                fill(DynamicShader.verticalGradient(Blue400.copy(alpha = 0.4f).toArgb(), Color.Transparent.toArgb()))
                            )
                        }
                    )
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    state.weeklyProtein.getOrNull(value.toInt())?.label ?: ""
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(160.dp)
    )
}

@Composable
private fun WeightHistoryChart(state: AnalyticsUiState) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(state.weightLogs) {
        modelProducer.runTransaction {
            lineSeries { series(state.weightLogs.map { it.weight }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(Purple400)) })
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    state.weightLogs.getOrNull(value.toInt())?.let {
                        DateUtils.formatShortDate(it.date)
                    } ?: ""
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(180.dp)
    )
}

@Composable
private fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(title, color = Neutral400, fontSize = 12.sp)
            Text(sub, color = Neutral600, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Neutral400, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}
