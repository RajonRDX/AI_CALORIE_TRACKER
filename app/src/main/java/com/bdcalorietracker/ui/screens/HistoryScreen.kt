package com.bdcalorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    dailySummaries: List<DailyCalorieSummary>,
    pastMeals: List<MealWithItems>,
    targetKcal: Int
) {
    var daysRange by remember { mutableIntStateOf(7) }
    var selectedEpochDay by remember { mutableStateOf<Long?>(null) }

    val filteredChartData = remember(dailySummaries, daysRange) {
        dailySummaries.takeLast(daysRange)
    }

    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM yyyy") }
    val shortDayFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Group all logged meals by calendar date (newest date first)
    val mealsByDate = remember(pastMeals) {
        pastMeals.groupBy { it.meal.dateEpochDay }.toSortedMap(compareByDescending { it })
    }

    val displayedDates = remember(mealsByDate, selectedEpochDay) {
        if (selectedEpochDay == null) mealsByDate
        else mealsByDate.filterKeys { it == selectedEpochDay }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Date-Wise Calorie History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = daysRange == 7,
                        onClick = { daysRange = 7 },
                        label = { Text("7D") }
                    )
                    FilterChip(
                        selected = daysRange == 30,
                        onClick = { daysRange = 30 },
                        label = { Text("30D") }
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

        // Date Filter Strip
        if (mealsByDate.isNotEmpty()) {
            item {
                Text("Filter by Date:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEpochDay == null,
                        onClick = { selectedEpochDay = null },
                        label = { Text("All Dates") }
                    )
                    mealsByDate.keys.forEach { epochDay ->
                        val dateLabel = LocalDate.ofEpochDay(epochDay).format(shortDayFormatter)
                        val dayTotal = mealsByDate[epochDay]?.sumOf { it.meal.totalKcal } ?: 0
                        FilterChip(
                            selected = selectedEpochDay == epochDay,
                            onClick = { selectedEpochDay = epochDay },
                            label = { Text("$dateLabel ($dayTotal kcal)") }
                        )
                    }
                }
            }
        }

        // Daily Summary + Meals Breakdown per Date
        displayedDates.forEach { (epochDay, mealsForThisDate) ->
            val formattedDate = LocalDate.ofEpochDay(epochDay).format(dayFormatter)
            val dateTotalKcal = mealsForThisDate.sumOf { it.meal.totalKcal }
            val dateProtein = mealsForThisDate.sumOf { it.meal.totalProteinG }.roundToInt()
            val dateCarbs = mealsForThisDate.sumOf { it.meal.totalCarbsG }.roundToInt()
            val dateFat = mealsForThisDate.sumOf { it.meal.totalFatG }.roundToInt()

            item(key = "header_$epochDay") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "P: ${dateProtein}g • C: ${dateCarbs}g • F: ${dateFat}g",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$dateTotalKcal kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Goal: $targetKcal kcal", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            items(mealsForThisDate, key = { it.meal.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (entry.meal.photoUri != null) {
                            AsyncImage(
                                model = entry.meal.photoUri,
                                contentDescription = "Meal Photo",
                                modifier = Modifier.size(68.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${entry.meal.mealType.name} • ${timeFormatter.format(Date(entry.meal.timestamp))}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${entry.meal.totalKcal} kcal", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                entry.items.joinToString { "${it.name_bn} (${it.grams.toInt()}g = ${it.kcal.roundToInt()} kcal)" },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
    val shortFormatter = remember { DateTimeFormatter.ofPattern("dd/MM") }

    if (data.isEmpty()) {
        Text("Log meals to see your 7-day and 30-day calorie chart.")
        return
    }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val count = data.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.55f).coerceAtMost(44f)

            data.forEachIndexed { index, summary ->
                val ratio = (summary.totalKcal / maxVal).coerceIn(0.06f, 1f)
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
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.takeLast(7).forEach { summary ->
                val label = LocalDate.ofEpochDay(summary.dateEpochDay).format(shortFormatter)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                    Text("${summary.totalKcal}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
