package com.bdcalorietracker.domain

import kotlin.math.roundToInt

data class MacroTargets(
    val dailyKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

object MifflinStJeorCalculator {
    fun calculate(
        age: Int,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        activityLevel: String,
        goal: String
    ): MacroTargets {
        val baseBmr = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        val bmr = if (sex.equals("MALE", ignoreCase = true)) baseBmr + 5 else baseBmr - 161

        val multiplier = when (activityLevel) {
            "SEDENTARY" -> 1.2
            "LIGHT" -> 1.375
            "MODERATE" -> 1.55
            "ACTIVE" -> 1.725
            else -> 1.375
        }

        val tdee = bmr * multiplier
        val goalAdjustedKcal = when (goal) {
            "LOSE" -> (tdee - 450).coerceAtLeast(1200.0)
            "GAIN" -> tdee + 350
            else -> tdee
        }.roundToInt()

        val proteinG = ((goalAdjustedKcal * 0.22) / 4.0).roundToInt()
        val carbsG = ((goalAdjustedKcal * 0.53) / 4.0).roundToInt()
        val fatG = ((goalAdjustedKcal * 0.25) / 9.0).roundToInt()

        return MacroTargets(
            dailyKcal = goalAdjustedKcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG
        )
    }
}
