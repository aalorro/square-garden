package com.squaregarden.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

object AvatarStorage {
    private const val AVATAR_DIR = "avatars"
    private const val MAX_SIZE = 512
    private const val MAX_FILE_BYTES = 10L * 1024 * 1024 // 10 MB

    /**
     * Copies the content URI to a local temp file and returns it.
     * Returns null if the URI can't be read or the file exceeds the size limit.
     * The caller should delete the temp file when done.
     */
    fun copyToTempFile(context: Context, uri: Uri): File? {
        val tempFile = File(context.cacheDir, "avatar_pick_${System.currentTimeMillis()}.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        } catch (_: Exception) {
            tempFile.delete()
            return null
        }
        if (tempFile.length() !in 1..MAX_FILE_BYTES) {
            tempFile.delete()
            return null
        }
        return tempFile
    }

    fun saveCroppedAvatar(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, AVATAR_DIR)
        if (!dir.exists()) dir.mkdirs()
        // Delete any existing avatar files before saving
        dir.listFiles()?.forEach { it.delete() }
        // Use unique filename so the path changes on each upload,
        // which triggers LaunchedEffect re-fire in MainActivity
        val file = File(dir, "avatar_${System.currentTimeMillis()}.png")

        val scaled = if (bitmap.width > MAX_SIZE || bitmap.height > MAX_SIZE) {
            val scale = MAX_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else bitmap

        file.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun loadAvatar(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(path)
    }

    fun decodeSampledBitmapFromFile(file: File, reqSize: Int = 1024): Bitmap? {
        val path = file.absolutePath

        // Decode bounds first
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

        // Apply EXIF rotation
        val rotation = try {
            val exif = ExifInterface(path)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) { 0f }

        return if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
    }

    fun deleteAvatar(context: Context) {
        val dir = File(context.filesDir, AVATAR_DIR)
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
