package com.bdcalorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bdcalorietracker.data.local.UserProfileEntity
import com.bdcalorietracker.domain.MifflinStJeorCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    existingProfile: UserProfileEntity?,
    onSaveProfile: (UserProfileEntity) -> Unit
) {
    var age by remember { mutableStateOf(existingProfile?.age?.toString() ?: "28") }
    var sex by remember { mutableStateOf(existingProfile?.sex ?: "MALE") }
    var heightCm by remember { mutableStateOf(existingProfile?.heightCm?.toString() ?: "168") }
    var weightKg by remember { mutableStateOf(existingProfile?.weightKg?.toString() ?: "68") }
    var activity by remember { mutableStateOf(existingProfile?.activityLevel ?: "LIGHT") }
    var goal by remember { mutableStateOf(existingProfile?.goal ?: "MAINTAIN") }
    var reminder by remember { mutableStateOf(existingProfile?.reminderEnabled ?: true) }

    val calculated = remember(age, sex, heightCm, weightKg, activity, goal) {
        MifflinStJeorCalculator.calculate(
            age = age.toIntOrNull() ?: 28,
            sex = sex,
            heightCm = heightCm.toDoubleOrNull() ?: 168.0,
            weightKg = weightKg.toDoubleOrNull() ?: 68.0,
            activityLevel = activity,
            goal = goal
        )
    }

    var customKcal by remember(calculated.dailyKcal) { mutableStateOf(calculated.dailyKcal.toString()) }
    var customProtein by remember(calculated.proteinG) { mutableStateOf(calculated.proteinG.toString()) }
    var customCarbs by remember(calculated.carbsG) { mutableStateOf(calculated.carbsG.toString()) }
    var customFat by remember(calculated.fatG) { mutableStateOf(calculated.fatG.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Personalize Your Diet Plan • আপনার ডায়েট প্ল্যান",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = sex == "MALE",
                onClick = { sex = "MALE" },
                label = { Text("Male (পুরুষ)") }
            )
            FilterChip(
                selected = sex == "FEMALE",
                onClick = { sex = "FEMALE" },
                label = { Text("Female (নারী)") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age (বয়স)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightCm,
                onValueChange = { heightCm = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weightKg,
                onValueChange = { weightKg = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Activity Level", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SEDENTARY", "LIGHT", "MODERATE", "ACTIVE").forEach { lvl ->
                FilterChip(
                    selected = activity == lvl,
                    onClick = { activity = lvl },
                    label = { Text(lvl.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Text("Goal (লক্ষ্য)", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LOSE" to "Lose Weight", "MAINTAIN" to "Maintain", "GAIN" to "Gain").forEach { (key, label) ->
                FilterChip(
                    selected = goal == key,
                    onClick = { goal = key },
                    label = { Text(label) }
                )
            }
        }

        HorizontalDivider()
        Text(
            "Mifflin-St Jeor Daily Targets (Editable Override)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = customKcal,
            onValueChange = { customKcal = it },
            label = { Text("Daily Calorie Target (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = customProtein,
                onValueChange = { customProtein = it },
                label = { Text("Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = customCarbs,
                onValueChange = { customCarbs = it },
                label = { Text("Carbs (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = customFat,
                onValueChange = { customFat = it },
                label = { Text("Fat (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Daily Reminder Notification")
            Switch(checked = reminder, onCheckedChange = { reminder = it })
        }

        Button(
            onClick = {
                onSaveProfile(
                    UserProfileEntity(
                        age = age.toIntOrNull() ?: 28,
                        sex = sex,
                        heightCm = heightCm.toDoubleOrNull() ?: 168.0,
                        weightKg = weightKg.toDoubleOrNull() ?: 68.0,
                        activityLevel = activity,
                        goal = goal,
                        targetKcal = customKcal.toIntOrNull() ?: calculated.dailyKcal,
                        targetProteinG = customProtein.toIntOrNull() ?: calculated.proteinG,
                        targetCarbsG = customCarbs.toIntOrNull() ?: calculated.carbsG,
                        targetFatG = customFat.toIntOrNull() ?: calculated.fatG,
                        reminderEnabled = reminder,
                        languageCode = existingProfile?.languageCode ?: "en"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Diet Plan • সংরক্ষণ করুন")
        }
    }
}
