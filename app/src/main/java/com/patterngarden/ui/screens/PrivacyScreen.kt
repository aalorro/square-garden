package com.patterngarden.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.patterngarden.ui.theme.DisplayFontFamily

@Composable
fun PrivacyScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    "\u2190", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Privacy Policy",
                fontFamily = DisplayFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PolicySection(
            title = "Overview",
            body = "Square Garden is designed with your privacy in mind. We believe " +
                "puzzle games should be fun without compromising your personal information."
        )

        PolicySection(
            title = "Data Collection",
            body = "Square Garden does not collect, transmit, or share any personal data. " +
                "All game progress, profile information, and settings are stored locally " +
                "on your device and never leave it."
        )

        PolicySection(
            title = "No Internet Required",
            body = "Square Garden works entirely offline. The game does not connect to " +
                "the internet, does not use analytics services, and does not contain " +
                "any tracking or advertising SDKs."
        )

        PolicySection(
            title = "Local Storage",
            body = "Your profile (username, avatar, preferences) and game progress " +
                "(stars, levels completed) are saved using Android's local DataStore. " +
                "This data remains on your device and can be cleared at any time " +
                "through the Settings menu or by uninstalling the app."
        )

        PolicySection(
            title = "Children's Privacy",
            body = "Square Garden is safe for players of all ages. Since we do not " +
                "collect any data, there are no special concerns regarding children's " +
                "privacy. No account creation, email, or personal details are ever required."
        )

        PolicySection(
            title = "Third-Party Services",
            body = "Square Garden does not integrate with any third-party services, " +
                "ad networks, or analytics platforms. Your gameplay experience is " +
                "completely private."
        )

        PolicySection(
            title = "Changes to This Policy",
            body = "If this privacy policy is updated in a future version, changes will " +
                "be reflected here within the app. No external action is required from you."
        )

        PolicySection(
            title = "Contact",
            body = "If you have questions about this privacy policy, please reach out " +
                "to ArtMondo through the app's store listing."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Last updated: May 2026",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
