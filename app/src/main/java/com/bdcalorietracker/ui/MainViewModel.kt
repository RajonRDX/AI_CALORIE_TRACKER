package com.bdcalorietracker.ui

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdcalorietracker.data.local.*
import com.bdcalorietracker.data.remote.GeminiVisionService
import com.bdcalorietracker.domain.FuzzyMatcher
import com.bdcalorietracker.domain.ImageCompressor
import com.bdcalorietracker.worker.DailyReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

data class EditablePlateItem(
    val uid: String = java.util.UUID.randomUUID().toString(),
    val matchedFood: FoodEntity?,
    val nameEn: String,
    val nameBn: String,
    val grams: Double,
    val servingDescription: String,
    val confidence: Double,
    val isApproximate: Boolean,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double
) {
    val calculatedKcal: Double get() = (kcalPer100g * grams) / 100.0
    val calculatedProtein: Double get() = (proteinPer100g * grams) / 100.0
    val calculatedCarbs: Double get() = (carbsPer100g * grams) / 100.0
    val calculatedFat: Double get() = (fatPer100g * grams) / 100.0
}

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object Analyzing : ScanUiState()
    data class Ready(val photoPath: String?, val items: List<EditablePlateItem>) : ScanUiState()
    data class Error(val message: String, val photoPath: String?) : ScanUiState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val userProfileDao: UserProfileDao,
    private val geminiVisionService: GeminiVisionService
) : ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = userProfileDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val todayEpoch = LocalDate.now().toEpochDay()

    val todayMeals: StateFlow<List<MealWithItems>> = mealDao.getMealsForDay(todayEpoch)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistoryMeals: StateFlow<List<MealWithItems>> = mealDao.getAllMealsHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val last30DaysChart: StateFlow<List<DailyCalorieSummary>> =
        mealDao.getDailySummariesSince(todayEpoch - 29)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<FoodEntity>> = _searchQuery
        .flatMapLatest { q -> foodDao.searchFoods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            CsvSeedLoader.seedIfNeeded(appContext, foodDao)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            userProfileDao.saveProfile(profile)
            DailyReminderWorker.schedule(appContext, profile.reminderEnabled)
        }
    }

    fun toggleLanguage(currentLang: String) {
        val nextLang = if (currentLang == "bn") "en" else "bn"
        viewModelScope.launch {
            userProfile.value?.let {
                userProfileDao.saveProfile(it.copy(languageCode = nextLang))
            }
            val appLocale = LocaleListCompat.forLanguageTags(nextLang)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun processSelectedImage(uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Analyzing
            try {
                val (bitmap, savedPath) = withContext(Dispatchers.IO) {
                    ImageCompressor.compressAndSave(appContext, uri, 512)
                }
                val catalog = withContext(Dispatchers.IO) { foodDao.getAllFoods() }
                val aiResult = geminiVisionService.analyzePlatePhoto(bitmap)

                aiResult.fold(
                    onSuccess = { detectedList ->
                        val mapped = detectedList.map { detected ->
                            val match = FuzzyMatcher.findBestMatch(
                                queryEn = detected.name_en,
                                queryBn = detected.name_bn,
                                catalog = catalog
                            )
                            val food = match.matchedFood
                            EditablePlateItem(
                                matchedFood = food,
                                nameEn = food?.name_en ?: detected.name_en,
                                nameBn = food?.name_bn ?: detected.name_bn,
                                grams = detected.estimated_grams.coerceIn(10.0, 1200.0),
                                servingDescription = detected.serving_description,
                                confidence = detected.confidence,
                                isApproximate = match.isApproximate || food == null,
                                kcalPer100g = food?.kcal_per_100g ?: 145.0,
                                proteinPer100g = food?.protein_g ?: 5.0,
                                carbsPer100g = food?.carbs_g ?: 20.0,
                                fatPer100g = food?.fat_g ?: 5.0
                            )
                        }
                        _scanState.value = ScanUiState.Ready(savedPath, mapped)
                    },
                    onFailure = { err ->
                        _scanState.value = ScanUiState.Error(
                            message = err.localizedMessage ?: "Offline or AI service unreachable",
                            photoPath = savedPath
                        )
                    }
                )
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(
                    message = e.localizedMessage ?: "Could not process image",
                    photoPath = null
                )
            }
        }
    }

    fun startManualEntryWithPhoto(photoPath: String?) {
        _scanState.value = ScanUiState.Ready(photoPath = photoPath, items = emptyList())
    }

    fun updateItemGrams(uid: String, newGrams: Double) {
        val current = _scanState.value as? ScanUiState.Ready ?: return
        val updated = current.items.map {
            if (it.uid == uid) it.copy(grams = newGrams.coerceIn(5.0, 2000.0)) else it
        }
        _scanState.value = current.copy(items = updated)
    }

    fun replacePlateItemWithFood(uid: String?, selectedFood: FoodEntity) {
        val current = _scanState.value as? ScanUiState.Ready ?: return
        val newItem = EditablePlateItem(
            matchedFood = selectedFood,
            nameEn = selectedFood.name_en,
            nameBn = selectedFood.name_bn,
            grams = selectedFood.typical_serving_g,
            servingDescription = "Typical serving (${selectedFood.typical_serving_g.toInt()}g)",
            confidence = 1.0,
            isApproximate = false,
            kcalPer100g = selectedFood.kcal_per_100g,
            proteinPer100g = selectedFood.protein_g,
            carbsPer100g = selectedFood.carbs_g,
            fatPer100g = selectedFood.fat_g
        )
        val updated = if (uid == null) {
            current.items + newItem
        } else {
            current.items.map { if (it.uid == uid) newItem.copy(uid = uid) else it }
        }
        _scanState.value = current.copy(items = updated)
    }

    fun removePlateItem(uid: String) {
        val current = _scanState.value as? ScanUiState.Ready ?: return
        _scanState.value = current.copy(items = current.items.filterNot { it.uid == uid })
    }

    fun saveCurrentMeal(mealType: MealType, onSaved: () -> Unit) {
        val current = _scanState.value as? ScanUiState.Ready ?: return
        if (current.items.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val totalKcal = current.items.sumOf { it.calculatedKcal }.roundToInt()
            val totalP = current.items.sumOf { it.calculatedProtein }
            val totalC = current.items.sumOf { it.calculatedCarbs }
            val totalF = current.items.sumOf { it.calculatedFat }

            val mealLog = MealLogEntity(
                timestamp = System.currentTimeMillis(),
                dateEpochDay = LocalDate.now().toEpochDay(),
                mealType = mealType,
                photoUri = current.photoPath,
                totalKcal = totalKcal,
                totalProteinG = totalP,
                totalCarbsG = totalC,
                totalFatG = totalF
            )

            val itemEntities = current.items.map {
                MealItemEntity(
                    mealLogId = 0,
                    foodId = it.matchedFood?.id,
                    name_en = it.nameEn,
                    name_bn = it.nameBn,
                    grams = it.grams,
                    kcal = it.calculatedKcal,
                    proteinG = it.calculatedProtein,
                    carbsG = it.calculatedCarbs,
                    fatG = it.calculatedFat,
                    isApproximate = it.isApproximate
                )
            }

            mealDao.saveMealWithItems(mealLog, itemEntities)
            withContext(Dispatchers.Main) {
                _scanState.value = ScanUiState.Idle
                onSaved()
            }
        }
    }
}
