package com.patterngarden.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.patterngarden.ui.screens.*

@Composable
fun PatternGardenNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.WorldSelect.route) {
            WorldSelectScreen(navController = navController)
        }
        composable(
            route = Screen.LevelSelect.route,
            arguments = listOf(navArgument("worldId") { type = NavType.IntType })
        ) { backStack ->
            LevelSelectScreen(
                worldId = backStack.arguments?.getInt("worldId") ?: 1,
                navController = navController
            )
        }
        composable(
            route = Screen.Game.route,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { backStack ->
            GameScreen(
                levelId = backStack.arguments?.getInt("levelId") ?: 1,
                navController = navController
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController, isFirstTime = false)
        }
        composable(Screen.ProfileSetup.route) {
            ProfileScreen(navController = navController, isFirstTime = true)
        }
        composable(Screen.Instructions.route) {
            InstructionsScreen(navController = navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
        composable(Screen.Privacy.route) {
            PrivacyScreen(navController = navController)
        }
    }
}
