package com.bdcalorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bdcalorietracker.R
import com.bdcalorietracker.data.local.MealItemEntity
import com.bdcalorietracker.data.local.MealWithItems
import com.bdcalorietracker.data.local.UserProfileEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: UserProfileEntity,
    todayMeals: List<MealWithItems>,
    onScanClick: () -> Unit,
    onManualAddClick: () -> Unit = {},
    onDeleteFoodItem: (MealItemEntity) -> Unit = {},
    onDeleteMeal: (Long) -> Unit = {},
    onEditGoalClick: () -> Unit,
    onToggleLang: () -> Unit
) {
    val eatenKcal = todayMeals.sumOf { it.meal.totalKcal }
    val eatenP = todayMeals.sumOf { it.meal.totalProteinG }.roundToInt()
    val eatenC = todayMeals.sumOf { it.meal.totalCarbsG }.roundToInt()
    val eatenF = todayMeals.sumOf { it.meal.totalFatG }.roundToInt()
    val isBn = profile.languageCode == "bn"
    val todayLabel = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(todayLabel, style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    TextButton(onClick = onToggleLang) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isBn) "EN" else "বাংলা", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onEditGoalClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Edit Goals")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onManualAddClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                    text = { Text(if (isBn) "ম্যানুয়ালি যোগ" else "Manual Add") }
                )
                ExtendedFloatingActionButton(
                    onClick = onScanClick,
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text(stringResource(R.string.scan_meal)) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CalorieRingChart(eaten = eatenKcal, target = profile.targetKcal)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${stringResource(R.string.eaten)}: $eatenKcal kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${stringResource(R.string.goal)}: ${profile.targetKcal} kcal", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        MacroProgressBar(stringResource(R.string.protein), eatenP, profile.targetProteinG)
                        Spacer(Modifier.height(8.dp))
                        MacroProgressBar(stringResource(R.string.carbs), eatenC, profile.targetCarbsG)
                        Spacer(Modifier.height(8.dp))
                        MacroProgressBar(stringResource(R.string.fat), eatenF, profile.targetFatG)
                    }
                }
            }

            item {
                Text(
                    text = if (isBn) "আজকের খাবার তালিকা" else "Today's Logged Meals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (todayMeals.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isBn) "আজ এখনো কোনো খাবার যোগ করা হয়নি। নিচে 'Manual Add' বা 'খাবার স্ক্যান' চাপুন!"
                            else "No meals logged today yet. Tap 'Manual Add' for quick coffee/snacks or 'Scan Meal' for a plate photo!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(todayMeals, key = { it.meal.id }) { mealWithItems ->
                    val timeLabel = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(mealWithItems.meal.timestamp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${mealWithItems.meal.mealType.name} • $timeLabel", fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${mealWithItems.meal.totalKcal} kcal",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { onDeleteMeal(mealWithItems.meal.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Entire Meal",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            mealWithItems.items.forEach { item ->
                                val title = if (isBn) "${item.name_bn} (${item.name_en})" else "${item.name_en} (${item.name_bn})"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• $title — ${item.grams.roundToInt()}g (${item.kcal.roundToInt()} kcal)",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteFoodItem(item) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete Food Item",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
fun CalorieRingChart(eaten: Int, target: Int) {
    val progress = (eaten.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val remaining = (target - eaten).coerceAtLeast(0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(135.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$remaining", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.calories_remaining), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MacroProgressBar(label: String, current: Int, target: Int) {
    val progress = (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("${current}g / ${target}g", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}
