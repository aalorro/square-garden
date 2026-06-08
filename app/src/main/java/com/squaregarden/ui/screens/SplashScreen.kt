package com.squaregarden.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.GameStateSerializer
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.SavedGameRepository
import com.squaregarden.data.SettingsRepository
import com.squaregarden.ui.components.LogoMark
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.DisplayFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(navController: NavHostController) {
    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val savedGameRepo = remember { SavedGameRepository(context) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
        delay(1200)

        val profile = profileRepo.loadProfile()
        val destination = when {
            !profile.isSetUp -> Screen.ProfileSetup.route
            !settingsRepo.shapesExplainerDismissed.first() -> Screen.ShapesExplainer.route
            else -> {
                // Redirect to saved in-progress game if one exists
                val savedJson = savedGameRepo.loadGame()
                if (savedJson != null) {
                    try {
                        val savedLevelId = GameStateSerializer.extractLevelId(savedJson)
                        Screen.Game.create(savedLevelId)
                    } catch (_: Exception) {
                        savedGameRepo.clearSavedGame()
                        Screen.Home.route
                    }
                } else {
                    Screen.Home.route
                }
            }
        }

        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            LogoMark(size = 120.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Square",
                fontFamily = DisplayFontFamily,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.03).sp
            )
            Text(
                text = "Garden",
                fontFamily = DisplayFontFamily,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = (-0.03).sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "A calm puzzle game",
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
