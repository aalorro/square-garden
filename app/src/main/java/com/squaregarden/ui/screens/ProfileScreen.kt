package com.squaregarden.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.squaregarden.data.AvatarStorage
import com.squaregarden.data.BadgeExporter
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.model.Difficulty
import com.squaregarden.model.Gender
import com.squaregarden.model.UserProfile
import com.squaregarden.ui.components.AvatarCropDialog
import com.squaregarden.ui.components.BasReliefAvatar
import com.squaregarden.ui.components.CUSTOM_AVATAR_ID
import com.squaregarden.ui.components.avatarList
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var leaderboardOptIn by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var masteryBadgeEarned by remember { mutableStateOf(false) }

    // Custom avatar state
    var customAvatarPath by remember { mutableStateOf("") }
    var customAvatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var sourceBitmapForCrop by remember { mutableStateOf<ImageBitmap?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    // Copy URI to local temp file immediately (avoids URI permission loss
                    // after Activity recreation and unreliable InputStream.available())
                    val tempFile = AvatarStorage.copyToTempFile(context, it) ?: return@withContext null
                    try {
                        AvatarStorage.decodeSampledBitmapFromFile(tempFile)
                    } finally {
                        tempFile.delete()
                    }
                }
                if (bmp != null) {
                    sourceBitmapForCrop = bmp.asImageBitmap()
                    showCropDialog = true
                } else {
                    Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val profile = profileRepo.loadProfile()
        username = profile.username
        avatarId = profile.avatarId
        customAvatarPath = profile.customAvatarPath
        yearOfBirth = profile.yearOfBirth
        gender = profile.gender
        themeId = profile.themeId
        difficulty = profile.difficulty
        leaderboardOptIn = profile.leaderboardOptIn
        masteryBadgeEarned = profile.masteryBadgeEarned
        if (profile.hasCustomAvatar) {
            withContext(Dispatchers.IO) {
                customAvatarBitmap = AvatarStorage.loadAvatar(profile.customAvatarPath)
                    ?.asImageBitmap()
            }
        }
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

        // Show selected avatar large (bas-relief)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BasReliefAvatar(
                emoji = if (avatarId == CUSTOM_AVATAR_ID) "" else avatarList.getOrElse(avatarId) { avatarList[0] }.emoji,
                size = 80.dp,
                imageBitmap = if (avatarId == CUSTOM_AVATAR_ID) customAvatarBitmap else null,
                masteryBadge = masteryBadgeEarned
            )
        }

        // Upload photo button
        OutlinedButton(
            onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (customAvatarBitmap != null) "Change Photo" else "Upload Photo")
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
            // Custom avatar cell (if one exists)
            if (customAvatarBitmap != null) {
                item {
                    val selected = avatarId == CUSTOM_AVATAR_ID
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
                            .clickable { avatarId = CUSTOM_AVATAR_ID },
                        contentAlignment = Alignment.Center
                    ) {
                        BasReliefAvatar(
                            emoji = "",
                            size = 44.dp,
                            animate = selected,
                            imageBitmap = customAvatarBitmap
                        )
                    }
                }
            }
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
                    BasReliefAvatar(
                        emoji = avatar.emoji,
                        size = 44.dp,
                        animate = selected
                    )
                }
            }
        }

        // Crop dialog
        if (showCropDialog && sourceBitmapForCrop != null) {
            AvatarCropDialog(
                bitmap = sourceBitmapForCrop!!,
                onCropped = { croppedBitmap ->
                    scope.launch {
                        val path = withContext(Dispatchers.IO) {
                            AvatarStorage.saveCroppedAvatar(context, croppedBitmap)
                        }
                        customAvatarPath = path
                        customAvatarBitmap = croppedBitmap.asImageBitmap()
                        avatarId = CUSTOM_AVATAR_ID
                        showCropDialog = false
                    }
                },
                onDismiss = { showCropDialog = false }
            )
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

        // ── Skill ──
        Text("Skill", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)

        if (!isFirstTime) {
            // Locked — show current skill as read-only
            val currentSkill = Difficulty.fromId(difficulty)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = currentSkill.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Skill is locked. Reset progress in Settings to change.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PRO_PLUS is reached only via upgrade after completing world 10 — exclude from initial setup.
                Difficulty.entries.filter { it != Difficulty.PRO_PLUS }.forEach { diff ->
                    val selected = diff.id == difficulty
                    val description = when (diff) {
                        Difficulty.EASY -> "More moves\nStart: World 1"
                        Difficulty.MEDIUM -> "Standard\nStart: World 2"
                        Difficulty.HARD -> "Fewer moves\nStart: World 3"
                        Difficulty.PRO_PLUS -> "Upgrade-only\nStart: World 11"
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
        }

        // ── Leaderboard Opt-In ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Leaderboards", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Share your scores on global leaderboards",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = leaderboardOptIn,
                onCheckedChange = { leaderboardOptIn = it }
            )
        }

        // ── Mastery Badge ──
        if (masteryBadgeEarned) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Mastery Badge",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val progressRepo = ProgressRepository(context)
                        val stars = progressRepo.totalStarsFlow.first()
                        val perfGames = progressRepo.perfectGamesFlow.first()
                        val diffLabel = Difficulty.fromId(difficulty).label
                        val dateStr = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(progressRepo.getGameCompletedTimestamp()))
                        val uri = BadgeExporter.exportBadge(
                            context, username.ifBlank { "Player" }, diffLabel, stars, perfGames, dateStr
                        )
                        if (uri != null) {
                            val shareIntent = BadgeExporter.shareIntent(uri)
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share Mastery Badge")
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Share Mastery Badge")
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
                            customAvatarPath = if (avatarId == CUSTOM_AVATAR_ID) customAvatarPath else "",
                            yearOfBirth = yearOfBirth,
                            gender = gender,
                            themeId = themeId,
                            difficulty = difficulty,
                            leaderboardOptIn = leaderboardOptIn
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
