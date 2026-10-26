package com.bdcalorietracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [Index("name_en"), Index("name_bn")]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name_en: String,
    val name_bn: String,
    val kcal_per_100g: Double,
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double,
    val fiber_g: Double,
    val typical_serving_g: Double,
    val source: String
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val dateEpochDay: Long,
    val mealType: MealType,
    val photoUri: String?,
    val totalKcal: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealLogId")]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealLogId: Long,
    val foodId: Long?,
    val name_en: String,
    val name_bn: String,
    val grams: Double,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val isApproximate: Boolean
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val age: Int,
    val sex: String,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: String,
    val goal: String,
    val targetKcal: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int,
    val reminderEnabled: Boolean = false,
    val languageCode: String = "en"
)
