package com.patterngarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.patterngarden.data.ProfileRepository
import com.patterngarden.data.ProgressRepository
import com.patterngarden.model.UserProfile
import com.patterngarden.ui.components.getAvatar
import com.patterngarden.ui.components.LogoMark
import com.patterngarden.ui.navigation.Screen
import com.patterngarden.ui.theme.DisplayFontFamily

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val progressRepo = remember { ProgressRepository(context) }
    val profileRepo = remember { ProfileRepository(context) }
    val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
    var profile by remember { mutableStateOf(UserProfile()) }

    LaunchedEffect(Unit) {
        profile = profileRepo.loadProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Top greeting bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${profile.username.ifBlank { "Gardener" }}!",
                    fontFamily = DisplayFontFamily,
                    fontSize = 65.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Avatar button
            Button(
                onClick = { navController.navigate(Screen.Profile.route) },
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = getAvatar(profile.avatarId).emoji,
                    fontSize = 28.sp
                )
            }
        }

        // Star count chip
        if (totalStars > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "\u2605", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "$totalStars stars",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Center content: logo + title
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoMark(size = 110.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Square",
                fontFamily = DisplayFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.03).sp
            )
            Text(
                text = "Garden",
                fontFamily = DisplayFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = (-0.03).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A calm puzzle game",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }

        // Bottom CTAs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate(Screen.WorldSelect.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "\u25B6  Play",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
