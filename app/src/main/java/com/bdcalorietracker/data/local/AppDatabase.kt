package com.bdcalorietracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import java.io.BufferedReader
import java.io.InputStreamReader

@Database(
    entities = [FoodEntity::class, MealLogEntity::class, MealItemEntity::class, UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun userProfileDao(): UserProfileDao
}

object CsvSeedLoader {
    suspend fun seedIfNeeded(context: Context, foodDao: FoodDao) {
        if (foodDao.count() > 0) return
        val foods = mutableListOf<FoodEntity>()
        context.assets.open("foods.csv").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val tokens = line.split(",")
                    if (tokens.size >= 9) {
                        foods.add(
                            FoodEntity(
                                name_en = tokens[0].trim(),
                                name_bn = tokens[1].trim(),
                                kcal_per_100g = tokens[2].trim().toDoubleOrNull() ?: 100.0,
                                protein_g = tokens[3].trim().toDoubleOrNull() ?: 0.0,
                                carbs_g = tokens[4].trim().toDoubleOrNull() ?: 0.0,
                                fat_g = tokens[5].trim().toDoubleOrNull() ?: 0.0,
                                fiber_g = tokens[6].trim().toDoubleOrNull() ?: 0.0,
                                typical_serving_g = tokens[7].trim().toDoubleOrNull() ?: 100.0,
                                source = tokens[8].trim()
                            )
                        )
                    }
                }
            }
        }
        foodDao.insertAll(foods)
    }
}
