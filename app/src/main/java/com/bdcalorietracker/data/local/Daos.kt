package com.bdcalorietracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class MealWithItems(
    @Embedded val meal: MealLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealLogId"
    )
    val items: List<MealItemEntity>
)

data class DailyCalorieSummary(
    val dateEpochDay: Long,
    val totalKcal: Int
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name_en ASC")
    suspend fun getAllFoods(): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE name_en LIKE '%' || :q || '%' OR name_bn LIKE '%' || :q || '%' LIMIT 50")
    fun searchFoods(q: String): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int
}

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meal_logs WHERE dateEpochDay = :epochDay ORDER BY timestamp DESC")
    fun getMealsForDay(epochDay: Long): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllMealsHistory(): Flow<List<MealWithItems>>

    @Query("""
        SELECT dateEpochDay, SUM(totalKcal) as totalKcal 
        FROM meal_logs 
        WHERE dateEpochDay >= :minEpochDay 
        GROUP BY dateEpochDay 
        ORDER BY dateEpochDay ASC
    """)
    fun getDailySummariesSince(minEpochDay: Long): Flow<List<DailyCalorieSummary>>

    @Insert
    suspend fun insertMeal(meal: MealLogEntity): Long

    @Insert
    suspend fun insertItems(items: List<MealItemEntity>)

    @Transaction
    suspend fun saveMealWithItems(meal: MealLogEntity, items: List<MealItemEntity>) {
        val mealId = insertMeal(meal)
        insertItems(items.map { it.copy(mealLogId = mealId) })
    }
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)
}
