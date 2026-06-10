package com.squaregarden

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.squaregarden.data.AvatarStorage
import com.squaregarden.data.ProfileRepository
import com.squaregarden.data.ProgressRepository
import com.squaregarden.data.SettingsRepository
import com.squaregarden.model.UserProfile
import com.squaregarden.ui.components.PlayerBadge
import com.squaregarden.ui.components.getAvatar
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.squaregarden.audio.MusicManager
import com.squaregarden.data.MasterModeRepository
import com.squaregarden.data.SavedGameRepository
import com.squaregarden.ui.navigation.SquareGardenNavGraph
import com.squaregarden.ui.navigation.Screen
import com.squaregarden.viewmodel.GameViewModel
import com.squaregarden.ui.theme.SquareGardenTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { /* User accepted or declined — dismissal already tracked */ }

        setContent {
            val context = LocalContext.current
            val profileRepo = remember { ProfileRepository(context) }
            val progressRepo = remember { ProgressRepository(context) }

            // Observe profile and progress reactively
            val profile by profileRepo.profileFlow.collectAsState(initial = UserProfile())
            val totalStars by progressRepo.totalStarsFlow.collectAsState(initial = 0)
            val gamesPlayed by progressRepo.gamesPlayedFlow.collectAsState(initial = 0)
            val lives by progressRepo.livesFlow.collectAsState(initial = 3)
            val perfectGames by progressRepo.perfectGamesFlow.collectAsState(initial = 0)
            val cooldownUntil by progressRepo.cooldownUntilFlow.collectAsState(initial = 0L)
            val masterModeRepo = remember { MasterModeRepository(context) }
            val masterStars by masterModeRepo.totalMasterStarsFlow.collectAsState(initial = 0)
            val savedGameRepo = remember { SavedGameRepository(context) }
            val hasSavedGame by savedGameRepo.hasSavedGameFlow.collectAsState(initial = false)
            val scope = rememberCoroutineScope()
            val settingsRepo = remember { SettingsRepository(context) }
            val snackbarHostState = remember { SnackbarHostState() }

            // One-time migration + restore lives on launch
            LaunchedEffect(Unit) {
                progressRepo.migrateStarsIfNeeded()
                progressRepo.checkAndRestoreLives()
                MusicManager.observeSettings(context, scope)
            }

            // Check for app updates
            LaunchedEffect(Unit) {
                checkForUpdate(settingsRepo, snackbarHostState, scope)
            }

            SquareGardenTheme(themeId = profile.themeId) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    SquareGardenNavGraph(navController = navController)

                    // Cooldown overlay when lives are exhausted
                    if (lives <= 0 && cooldownUntil > 0L) {
                        CooldownOverlay(
                            cooldownUntil = cooldownUntil,
                            onExpired = {
                                scope.launch {
                                    progressRepo.checkAndRestoreLives()
                                }
                            }
                        )
                    }

                    // Load custom avatar bitmap
                    var customAvatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(profile.customAvatarPath, profile.avatarId) {
                        customAvatarBitmap = if (profile.hasCustomAvatar) {
                            withContext(Dispatchers.IO) {
                                AvatarStorage.loadAvatar(profile.customAvatarPath)?.asImageBitmap()
                            }
                        } else null
                    }

                    // Detect Master Mode from current nav route
                    val navEntry by navController.currentBackStackEntryAsState()
                    val inMasterMode = remember(navEntry) {
                        val route = navEntry?.destination?.route ?: ""
                        if (route == Screen.Game.route) {
                            val levelId = navEntry?.arguments?.getInt("levelId") ?: 0
                            levelId == GameViewModel.MASTER_MODE_SIGNAL
                        } else route == Screen.MasterMode.route
                    }

                    // Player badge in top-right corner
                    if (profile.isSetUp) {
                        val avatar = getAvatar(profile.avatarId)
                        PlayerBadge(
                            avatarEmoji = avatar.emoji,
                            avatarImageBitmap = customAvatarBitmap,
                            playerLevel = profile.playerLevel,
                            totalStars = if (inMasterMode) masterStars else totalStars,
                            gamesPlayed = gamesPlayed,
                            lives = lives,
                            perfectGames = perfectGames,
                            masteryBadge = profile.masteryBadgeEarned,
                            masterMode = inMasterMode,
                            onHomeClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            onMasterModeClick = if (profile.masteryBadgeEarned) {
                                { navController.navigate(Screen.MasterMode.route) }
                            } else null,
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onExitClick = {
                                finish()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .systemBarsPadding()
                                .padding(top = 8.dp, end = 12.dp)
                        )
                    }

                    // Snackbar for in-app update download completion
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .systemBarsPadding()
                            .padding(bottom = 16.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    private suspend fun checkForUpdate(
        settingsRepo: SettingsRepository,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope
    ) {
        try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                val availableVersion = appUpdateInfo.availableVersionCode()
                val dismissedVersion = settingsRepo.dismissedUpdateVersion.first()
                if (availableVersion <= dismissedVersion) return

                settingsRepo.setDismissedUpdateVersion(availableVersion)

                val listener = InstallStateUpdatedListener { state ->
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Update downloaded",
                                actionLabel = "Restart",
                                withDismissAction = true
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                appUpdateManager.completeUpdate()
                            }
                        }
                    }
                }
                appUpdateManager.registerListener(listener)

                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                val result = snackbarHostState.showSnackbar(
                    message = "Update ready to install",
                    actionLabel = "Restart",
                    withDismissAction = true
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeUpdate()
                }
            }
        } catch (e: Exception) {
            Log.d("InAppUpdate", "Update check failed: ${e.message}")
        }
    }
}

@Composable
private fun CooldownOverlay(cooldownUntil: Long, onExpired: () -> Unit) {
    var remainingMs by remember { mutableLongStateOf(cooldownUntil - System.currentTimeMillis()) }
    var minimized by remember { mutableStateOf(false) }

    LaunchedEffect(cooldownUntil) {
        while (true) {
            remainingMs = cooldownUntil - System.currentTimeMillis()
            if (remainingMs <= 0) {
                onExpired()
                break
            }
            delay(1000)
        }
    }

    if (remainingMs <= 0) return

    val minutes = (remainingMs / 60000).toInt()
    val seconds = ((remainingMs % 60000) / 1000).toInt()
    val timeText = "%d:%02d".format(minutes, seconds)

    if (minimized) {
        // Minimized: small floating chip in bottom-center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                    .clickable { minimized = false }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "\u2764\uFE0F $timeText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    } else {
        // Fullscreen overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(40.dp)
            ) {
                Text(
                    text = "No Lives Left",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "You've used all your lives.\nPlease wait to continue playing.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = timeText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "until lives restore",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { minimized = true },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Minimize", fontSize = 13.sp)
                }
            }
        }
    }
}
