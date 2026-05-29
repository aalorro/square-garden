package com.squaregarden.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.squaregarden.ui.components.MasteryBadgeRenderer

object BadgeExporter {

    /**
     * Renders and saves the mastery badge PNG to the device Pictures folder.
     * Returns the content URI on success, null on failure.
     */
    fun exportBadge(
        context: Context,
        playerName: String,
        difficultyLabel: String,
        totalStars: Int,
        perfectGames: Int,
        dateString: String
    ): Uri? {
        val bitmap = MasteryBadgeRenderer.render(
            playerName = playerName,
            difficultyLabel = difficultyLabel,
            totalStars = totalStars,
            perfectGames = perfectGames,
            dateString = dateString
        )

        val filename = "SquareGarden_Mastery_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Square Garden")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            bitmap.recycle()
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            bitmap.recycle()
            null
        }
    }

    /**
     * Creates a share intent for the badge image.
     */
    fun shareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
