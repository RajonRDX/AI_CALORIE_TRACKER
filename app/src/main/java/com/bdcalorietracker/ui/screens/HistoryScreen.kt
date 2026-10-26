package com.bdcalorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bdcalorietracker.data.local.DailyCalorieSummary
import com.bdcalorietracker.data.local.MealWithItems
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    dailySummaries: List<DailyCalorieSummary>,
    pastMeals: List<MealWithItems>,
    targetKcal: Int
) {
    var daysRange by remember { mutableIntStateOf(7) }
    val filteredChartData = remember(dailySummaries, daysRange) {
        dailySummaries.takeLast(daysRange)
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Calorie Trends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = daysRange == 7,
                        onClick = { daysRange = 7 },
                        label = { Text("7 Days") }
                    )
                    FilterChip(
                        selected = daysRange == 30,
                        onClick = { daysRange = 30 },
                        label = { Text("30 Days") }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BarChartView(data = filteredChartData, maxTarget = targetKcal)
                }
            }
        }

        item {
            Text("Past Meals with Photos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(pastMeals) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (entry.meal.photoUri != null) {
                        AsyncImage(
                            model = entry.meal.photoUri,
                            contentDescription = "Meal Photo",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${entry.meal.mealType.name} • ${entry.meal.totalKcal} kcal",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            dateFormat.format(Date(entry.meal.timestamp)),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            entry.items.joinToString { "${it.name_bn} (${it.grams.toInt()}g)" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartView(data: List<DailyCalorieSummary>, maxTarget: Int) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxVal = (data.maxOfOrNull { it.totalKcal } ?: maxTarget).coerceAtLeast(maxTarget).toFloat()

    if (data.isEmpty()) {
        Text("Log meals to see your 7-day and 30-day calorie chart.")
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val count = data.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.6f).coerceAtMost(48f)

        data.forEachIndexed { index, summary ->
            val ratio = (summary.totalKcal / maxVal).coerceIn(0.05f, 1f)
            val barHeight = size.height * ratio
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}
