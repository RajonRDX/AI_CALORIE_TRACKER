package com.bdcalorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bdcalorietracker.R
import com.bdcalorietracker.data.local.FoodEntity
import com.bdcalorietracker.data.local.MealType
import com.bdcalorietracker.ui.EditablePlateItem
import com.bdcalorietracker.ui.ScanUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealResultScreen(
    scanState: ScanUiState,
    searchResults: List<FoodEntity>,
    onSearchQueryChange: (String) -> Unit,
    onUpdateGrams: (String, Double) -> Unit,
    onReplaceItem: (String?, FoodEntity) -> Unit,
    onRemoveItem: (String) -> Unit,
    onStartManualOffline: (String?) -> Unit,
    onSaveMeal: (MealType) -> Unit
) {
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var pickerTargetUid by remember { mutableStateOf<String?>(null) }
    var showFoodPicker by remember { mutableStateOf(false) }

    when (scanState) {
        is ScanUiState.Analyzing -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Identifying Bangladeshi dishes & portions with Gemini...")
                }
            }
        }

        is ScanUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.offline_error), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(scanState.message, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onStartManualOffline(scanState.photoPath) }) {
                    Text("Add Food Items Manually (Offline)")
                }
            }
        }

        is ScanUiState.Ready -> {
            val totalKcal = scanState.items.sumOf { it.calculatedKcal }.roundToInt()
            val totalP = scanState.items.sumOf { it.calculatedProtein }.roundToInt()
            val totalC = scanState.items.sumOf { it.calculatedCarbs }.roundToInt()
            val totalF = scanState.items.sumOf { it.calculatedFat }.roundToInt()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text(
                            text = stringResource(R.string.disclaimer_banner),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (scanState.photoPath != null) {
                    item {
                        AsyncImage(
                            model = scanState.photoPath,
                            contentDescription = "Captured Plate",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Live Plate Total: $totalKcal kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Protein: ${totalP}g • Carbs: ${totalC}g • Fat: ${totalF}g")
                        }
                    }
                }

                items(scanState.items, key = { it.uid }) { item ->
                    EditableFoodItemCard(
                        item = item,
                        onUpdateGrams = { newG -> onUpdateGrams(item.uid, newG) },
                        onSwapFoodClick = {
                            pickerTargetUid = item.uid
                            onSearchQueryChange("")
                            showFoodPicker = true
                        },
                        onRemoveClick = { onRemoveItem(item.uid) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            pickerTargetUid = null
                            onSearchQueryChange("")
                            showFoodPicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Another Dish From Database")
                    }
                }

                item {
                    Text("Save Under Meal:", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedMealType == type,
                                onClick = { selectedMealType = type },
                                label = { Text(type.name) }
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onSaveMeal(selectedMealType) },
                        enabled = scanState.items.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_meal))
                    }
                }
            }
        }

        ScanUiState.Idle -> {}
    }

    if (showFoodPicker) {
        SearchableFoodPickerDialog(
            foods = searchResults,
            onQueryChange = onSearchQueryChange,
            onDismiss = { showFoodPicker = false },
            onSelectFood = { food ->
                onReplaceItem(pickerTargetUid, food)
                showFoodPicker = false
            }
        )
    }
}

@Composable
fun EditableFoodItemCard(
    item: EditablePlateItem,
    onUpdateGrams: (Double) -> Unit,
    onSwapFoodClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${item.nameEn} (${item.nameBn})", fontWeight = FontWeight.Bold)
                    val badgeText = if (item.isApproximate) "⚠️ Approximate Match - Tap Edit to Pick Exact"
                    else "✓ Matched (${(item.confidence * 100).roundToInt()}% conf) • ${item.servingDescription}"
                    Text(badgeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onSwapFoodClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Swap Food")
                }
                IconButton(onClick = onRemoveClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { onUpdateGrams(item.grams - 15.0) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-15g")
                    }
                    Text(
                        "${item.grams.roundToInt()} g",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(onClick = { onUpdateGrams(item.grams + 15.0) }) {
                        Icon(Icons.Default.Add, contentDescription = "+15g")
                    }
                }
                Text(
                    "${item.calculatedKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = { onUpdateGrams(200.0) }, label = { Text("1 Cup Rice (200g)") })
                AssistChip(onClick = { onUpdateGrams(40.0) }, label = { Text("1 Ruti (40g)") })
                AssistChip(onClick = { onUpdateGrams(60.0) }, label = { Text("Bhorta/Pc (60g)") })
            }
        }
    }
}

@Composable
fun SearchableFoodPickerDialog(
    foods: List<FoodEntity>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectFood: (FoodEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bangladeshi Food (খাবার খুঁজুন)") },
        text = {
            Column(modifier = Modifier.height(380.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it)
                    },
                    label = { Text("Search English or বাংলা...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(foods) { food ->
                        Card(
                            onClick = { onSelectFood(food) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${food.name_en} • ${food.name_bn}", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${food.kcal_per_100g.toInt()} kcal/100g | Serving: ${food.typical_serving_g.toInt()}g (${food.source})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
