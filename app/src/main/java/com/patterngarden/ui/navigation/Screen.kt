package com.patterngarden.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object WorldSelect : Screen("world_select")
    data object LevelSelect : Screen("level_select/{worldId}") {
        fun create(worldId: Int) = "level_select/$worldId"
    }
    data object Game : Screen("game/{levelId}") {
        fun create(levelId: Int) = "game/$levelId"
    }
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
    data object ProfileSetup : Screen("profile_setup")
    data object Instructions : Screen("instructions")
    data object About : Screen("about")
    data object Privacy : Screen("privacy")
}
