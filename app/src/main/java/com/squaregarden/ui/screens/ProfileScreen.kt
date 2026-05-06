package com.squaregarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.ProfileRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.model.Gender
import com.squaregarden.model.UserProfile
import com.squaregarden.ui.components.avatarList
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController, isFirstTime: Boolean = false) {
    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var avatarId by remember { mutableIntStateOf(0) }
    var yearOfBirth by remember { mutableIntStateOf(2000) }
    var gender by remember { mutableStateOf("prefer_not_to_say") }
    var themeId by remember { mutableStateOf("light") }
    var difficulty by remember { mutableStateOf("medium") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val profile = profileRepo.loadProfile()
        username = profile.username
        avatarId = profile.avatarId
        yearOfBirth = profile.yearOfBirth
        gender = profile.gender
        themeId = profile.themeId
        difficulty = profile.difficulty
        loaded = true
    }

    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = if (isFirstTime) "Create Your Profile" else "Edit Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Username ──
        Text("Username", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        OutlinedTextField(
            value = username,
            onValueChange = { newVal ->
                val filtered = newVal.filter { it.isLetterOrDigit() }
                if (filtered.length <= 15) username = filtered
            },
            placeholder = { Text("Letters and numbers only") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Avatar Picker ──
        Text("Choose Avatar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)

        // Show selected avatar large
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarList.getOrElse(avatarId) { avatarList[0] }.emoji,
                    fontSize = 36.sp
                )
            }
        }

        // Avatar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(avatarList) { avatar ->
                val selected = avatar.id == avatarId
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .then(
                            if (selected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(10.dp)
                            ) else Modifier
                        )
                        .clickable { avatarId = avatar.id },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.emoji, fontSize = 24.sp)
                }
            }
        }

        // ── Year of Birth ──
        Text("Year of Birth", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (yearOfBirth > 1940) yearOfBirth-- }
            ) {
                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = "$yearOfBirth",
                onValueChange = { text ->
                    val filtered = text.filter { it.isDigit() }.take(4)
                    if (filtered.isNotEmpty()) {
                        val year = filtered.toIntOrNull() ?: yearOfBirth
                        yearOfBirth = year.coerceIn(1, 9999)
                    }
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    yearOfBirth = yearOfBirth.coerceIn(1940, 2024)
                    focusManager.clearFocus()
                }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(100.dp)
            )
            IconButton(
                onClick = { if (yearOfBirth < 2024) yearOfBirth++ }
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // ── Gender ──
        Text("Gender", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Gender.entries.forEach { g ->
                val selected = g.id == gender
                Card(
                    onClick = { gender = g.id },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (selected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = g.label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // ── Theme Picker ──
        Text("Theme", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            items(allThemes) { theme ->
                val selected = theme.id == themeId
                Card(
                    onClick = { themeId = theme.id },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .aspectRatio(1.4f)
                        .then(
                            if (selected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    colors = CardDefaults.cardColors(containerColor = theme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Preview dots
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary)
                            )
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(theme.secondary)
                            )
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(theme.surface)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = theme.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = theme.onBackground,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ── Difficulty ──
        Text("Skill", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Difficulty.entries.forEach { diff ->
                val selected = diff.id == difficulty
                val description = when (diff) {
                    Difficulty.EASY -> "More moves\nStart: World 1"
                    Difficulty.MEDIUM -> "Standard\nStart: World 2"
                    Difficulty.HARD -> "Fewer moves\nStart: World 3"
                }
                Card(
                    onClick = { difficulty = diff.id },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (selected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = diff.label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Save Button ──
        Button(
            onClick = {
                scope.launch {
                    profileRepo.saveProfile(
                        UserProfile(
                            username = username.trim().ifBlank { "Player" },
                            avatarId = avatarId,
                            yearOfBirth = yearOfBirth,
                            gender = gender,
                            themeId = themeId,
                            difficulty = difficulty
                        )
                    )
                    if (isFirstTime) {
                        navController.navigate(Screen.ShapesExplainer.route) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            enabled = username.isNotBlank()
        ) {
            Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        if (!isFirstTime) {
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cancel")
            }
        }
    }
}
