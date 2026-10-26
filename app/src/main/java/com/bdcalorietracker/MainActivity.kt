package com.bdcalorietracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
import com.bdcalorietracker.ui.MainViewModel
import com.bdcalorietracker.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val profile by viewModel.userProfile.collectAsState()
                val todayMeals by viewModel.todayMeals.collectAsState()
                val historyMeals by viewModel.allHistoryMeals.collectAsState()
                val chartSummaries by viewModel.last30DaysChart.collectAsState()
                val scanState by viewModel.scanState.collectAsState()
                val searchResults by viewModel.searchResults.collectAsState()

                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (profile != null && currentRoute in listOf("dashboard", "camera", "history")) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == "dashboard",
                                    onClick = { navController.navigate("dashboard") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(stringResource(R.string.dashboard)) }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "camera",
                                    onClick = { navController.navigate("camera") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    label = { Text(stringResource(R.string.scan_meal)) }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "history",
                                    onClick = { navController.navigate("history") { launchSingleTop = true } },
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text(stringResource(R.string.history)) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (profile == null) "onboarding" else "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                existingProfile = profile,
                                onSaveProfile = { newProfile ->
                                    viewModel.saveUserProfile(newProfile)
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            val activeProfile = profile
                            if (activeProfile != null) {
                                DashboardScreen(
                                    profile = activeProfile,
                                    todayMeals = todayMeals,
                                    onScanClick = { navController.navigate("camera") },
                                    onManualAddClick = {
                                        viewModel.startManualEntryWithPhoto(null)
                                        navController.navigate("result")
                                    },
                                    onEditGoalClick = { navController.navigate("onboarding") },
                                    onToggleLang = { viewModel.toggleLanguage(activeProfile.languageCode) }
                                )
                            }
                        }
                        composable("camera") {
                            CameraCaptureScreen(
                                onImageSelected = { uri ->
                                    viewModel.processSelectedImage(uri)
                                    navController.navigate("result")
                                },
                                onManualLogClick = {
                                    viewModel.startManualEntryWithPhoto(null)
                                    navController.navigate("result")
                                }
                            )
                        }
                        composable("result") {
                            MealResultScreen(
                                scanState = scanState,
                                searchResults = searchResults,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onUpdateGrams = viewModel::updateItemGrams,
                                onReplaceItem = viewModel::replacePlateItemWithFood,
                                onRemoveItem = viewModel::removePlateItem,
                                onStartManualOffline = viewModel::startManualEntryWithPhoto,
                                onSaveMeal = { mealType ->
                                    viewModel.saveCurrentMeal(mealType) {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                dailySummaries = chartSummaries,
                                pastMeals = historyMeals,
                                targetKcal = profile?.targetKcal ?: 2000
                            )
                        }
                    }
                }
            }
        }
    }
}
