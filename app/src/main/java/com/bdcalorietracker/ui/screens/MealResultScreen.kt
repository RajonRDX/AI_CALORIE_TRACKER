package com.bdcalorietracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

private val QUICK_ADD_STAPLES = listOf(
    FoodEntity(901, "Black Coffee (Unsweetened)", "ব্ল্যাক কফি", 2.0, 0.3, 0.0, 0.0, 0.0, 200.0, "1 Mug = 200ml (4 kcal)"),
    FoodEntity(902, "White Sugar (1 tsp)", "চিনি (১ চামচ)", 398.0, 0.0, 99.8, 0.0, 0.0, 5.0, "1 tsp = 5g (20 kcal)"),
    FoodEntity(903, "Shingara", "সিঙ্গাড়া", 288.0, 5.2, 36.5, 13.5, 2.4, 65.0, "1 pc = 65g (187 kcal)"),
    FoodEntity(904, "Dudh Cha (Milk Tea)", "দুধ চা", 58.0, 1.8, 8.5, 1.8, 0.0, 150.0, "1 Cup = 150ml (87 kcal)"),
    FoodEntity(905, "Rong Cha (1 tsp Sugar)", "রং চা", 16.0, 0.1, 4.0, 0.0, 0.0, 150.0, "1 Cup = 150ml (24 kcal)"),
    FoodEntity(906, "Boiled Chicken Egg", "সিদ্ধ ডিম", 155.0, 12.6, 1.1, 10.6, 0.0, 50.0, "1 Egg = 50g (78 kcal)"),
    FoodEntity(907, "Samucha", "সমুচা", 315.0, 6.8, 34.0, 16.8, 1.8, 50.0, "1 pc = 50g (158 kcal)"),
    FoodEntity(908, "Steamed White Rice", "সিদ্ধ ভাত", 115.0, 2.5, 25.6, 0.2, 0.4, 200.0, "1 Cup = 200g (230 kcal)")
)

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
    var selectedMealType by remember { mutableStateOf(MealType.SNACK) }
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
                    Text("Add Food Items Manually")
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

                // 1-Tap Quick-Add Bar for Everyday Staples (Black Coffee, Sugar, Singara, Tea, Egg)
                item {
                    Text(
                        "⚡ Quick Add Everyday Items (1-Tap):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QUICK_ADD_STAPLES.forEach { staple ->
                            ElevatedSuggestionChip(
                                onClick = { onReplaceItem(null, staple) },
                                label = { Text("+ ${staple.name_bn} (${staple.typical_serving_g.toInt()}g)") }
                            )
                        }
                    }
                }

                if (scanState.photoPath != null) {
                    item {
                        AsyncImage(
                            model = scanState.photoPath,
                            contentDescription = "Captured Plate",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Live Meal Total: $totalKcal kcal",
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
                        Text("Search Full 160+ Bangladeshi Food Database")
                    }
                }

                item {
                    Text("Save Under Meal:", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
    val nameLower = item.nameEn.lowercase()
    val isDrink = nameLower.contains("coffee") || nameLower.contains("cha") || nameLower.contains("tea") || nameLower.contains("borhani") || nameLower.contains("lacchi")
    val isSugarOrGur = nameLower.contains("sugar") || nameLower.contains("gur")
    val isSnackPiece = nameLower.contains("shingara") || nameLower.contains("samucha") || nameLower.contains("egg") || nameLower.contains("pitha") || nameLower.contains("chop")
    val stepSize = if (isSugarOrGur) 5.0 else 15.0
    val unitLabel = if (isDrink) "ml" else "g"

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
                    else "✓ ${item.servingDescription}"
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
                    FilledTonalIconButton(onClick = { onUpdateGrams(item.grams - stepSize) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        "${item.grams.roundToInt()} $unitLabel",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(onClick = { onUpdateGrams(item.grams + stepSize) }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
                Text(
                    "${item.calculatedKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Smart Context-Aware Presets based on Food Type
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    isDrink -> {
                        AssistChip(onClick = { onUpdateGrams(120.0) }, label = { Text("Small Cup (120ml)") })
                        AssistChip(onClick = { onUpdateGrams(200.0) }, label = { Text("1 Mug (200ml)") })
                        AssistChip(onClick = { onUpdateGrams(300.0) }, label = { Text("Large Mug (300ml)") })
                    }
                    isSugarOrGur -> {
                        AssistChip(onClick = { onUpdateGrams(5.0) }, label = { Text("1 tsp (5g = 20 kcal)") })
                        AssistChip(onClick = { onUpdateGrams(10.0) }, label = { Text("2 tsp (10g = 40 kcal)") })
                        AssistChip(onClick = { onUpdateGrams(15.0) }, label = { Text("1 tbsp (15g = 60 kcal)") })
                    }
                    isSnackPiece -> {
                        val singleWt = item.matchedFood?.typical_serving_g ?: 60.0
                        AssistChip(onClick = { onUpdateGrams(singleWt) }, label = { Text("1 Pc (${singleWt.toInt()}g)") })
                        AssistChip(onClick = { onUpdateGrams(singleWt * 2) }, label = { Text("2 Pcs (${(singleWt * 2).toInt()}g)") })
                        AssistChip(onClick = { onUpdateGrams(singleWt * 3) }, label = { Text("3 Pcs (${(singleWt * 3).toInt()}g)") })
                    }
                    else -> {
                        AssistChip(onClick = { onUpdateGrams(200.0) }, label = { Text("1 Cup Rice (200g)") })
                        AssistChip(onClick = { onUpdateGrams(40.0) }, label = { Text("1 Ruti (40g)") })
                        AssistChip(onClick = { onUpdateGrams(60.0) }, label = { Text("Bhorta/Pc (60g)") })
                    }
                }
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
                    label = { Text("Search coffee, singara, sugar, ভাত...") },
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
                                    "${food.kcal_per_100g.toInt()} kcal/100g | Typical: ${food.typical_serving_g.toInt()}g (${food.source})",
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
