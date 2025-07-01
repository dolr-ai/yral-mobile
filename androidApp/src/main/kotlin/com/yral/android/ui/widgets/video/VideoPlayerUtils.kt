package com.yral.android.ui.widgets.video

import android.content.Context
import android.net.Uri
import android.os.Environment
import co.touchlab.kermit.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

@Suppress("TooManyFunctions")
object VideoPlayerUtils {
    /**
     * Get the absolute path for internal storage video files
     */
    fun getInternalVideoPath(
        context: Context,
        fileName: String,
    ): String =
        File(context.filesDir, "videos")
            .apply {
                if (!exists()) mkdirs()
            }.absolutePath + "/$fileName"

    /**
     * Get the absolute path for external storage video files
     */
    fun getExternalVideoPath(fileName: String): String? =
        try {
            val moviesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (!moviesDir.exists()) moviesDir.mkdirs()
            "${moviesDir.absolutePath}/$fileName"
        } catch (e: SecurityException) {
            Logger.e("Permission denied accessing external storage", e)
            null
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid directory type or filename: $fileName", e)
            null
        }

    /**
     * Check if a video file exists at the given path
     */
    fun videoFileExists(filePath: String): Boolean =
        try {
            val file = File(filePath)
            file.exists() && file.isFile && file.canRead()
        } catch (e: SecurityException) {
            Logger.e("Permission denied accessing file: $filePath", e)
            false
        }

    /**
     * Get video file size in bytes
     */
    fun getVideoFileSize(filePath: String): Long =
        try {
            File(filePath).length()
        } catch (e: SecurityException) {
            Logger.e("Permission denied accessing file: $filePath", e)
            0L
        }

    /**
     * Copy video from assets to internal storage
     * Useful for bundled demo videos or widget preview videos
     */
    fun copyVideoFromAssets(
        context: Context,
        assetFileName: String,
        targetFileName: String,
    ): String? {
        val targetPath = getInternalVideoPath(context, targetFileName)
        return try {
            val targetFile = File(targetPath)

            // Don't copy if file already exists
            if (targetFile.exists()) {
                return targetPath
            }

            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            targetPath
        } catch (e: FileNotFoundException) {
            Logger.e("Asset file not found: $assetFileName", e)
            null
        } catch (e: IOException) {
            Logger.e("IO error copying video from assets: $assetFileName", e)
            null
        } catch (e: SecurityException) {
            Logger.e("Permission denied copying video to: $targetPath", e)
            null
        }
    }

    /**
     * Copy video from URI to internal storage
     * Useful for user-selected videos
     */
    fun copyVideoFromUri(
        context: Context,
        sourceUri: Uri,
        targetFileName: String,
    ): String? {
        val targetPath = getInternalVideoPath(context, targetFileName)
        return try {
            val targetFile = File(targetPath)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetPath
        } catch (e: FileNotFoundException) {
            Logger.e("Source URI not found: $sourceUri", e)
            null
        } catch (e: IOException) {
            Logger.e("IO error copying video from URI: $sourceUri", e)
            null
        } catch (e: SecurityException) {
            Logger.e("Permission denied accessing URI or target file: $sourceUri -> $targetPath", e)
            null
        }
    }

    /**
     * Delete video file
     */
    fun deleteVideoFile(filePath: String): Boolean =
        try {
            File(filePath).delete()
        } catch (e: SecurityException) {
            Logger.e("Permission denied deleting file: $filePath", e)
            false
        }

    /**
     * Get all video files in internal storage videos directory
     */
    fun getInternalVideoFiles(context: Context): List<String> {
        return try {
            val videosDir = File(context.filesDir, "videos")
            if (!videosDir.exists()) return emptyList()

            videosDir
                .listFiles { file ->
                    file.isFile && isVideoFile(file.name)
                }?.map { it.absolutePath } ?: emptyList()
        } catch (e: SecurityException) {
            Logger.e("Permission denied accessing videos directory", e)
            emptyList()
        }
    }

    /**
     * Check if file extension indicates a video file
     */
    fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "webm", "3gp", "m4v")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in videoExtensions
    }

    /**
     * Format file size for display
     */
    @Suppress("MagicNumber")
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toFloat()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }

    /**
     * Configuration for widget video players
     */
    data class WidgetVideoConfig(
        val autoPlay: Boolean = true,
        val loop: Boolean = true,
        val showControls: Boolean = false,
        val muted: Boolean = true, // Widgets should typically be muted
    )

    /**
     * Get optimal configuration for widget video player
     */
    fun getWidgetVideoConfig(): WidgetVideoConfig =
        WidgetVideoConfig(
            autoPlay = true,
            loop = true,
            showControls = false,
            muted = true,
        )
}

/**
 * Data class for video file information
 */
data class VideoFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val exists: Boolean,
    val formattedSize: String = VideoPlayerUtils.formatFileSize(size),
)

/**
 * Extension function to get video file info
 */
fun String.toVideoFileInfo(): VideoFileInfo {
    val file = File(this)
    return VideoFileInfo(
        path = this,
        name = file.name,
        size = if (file.exists()) file.length() else 0L,
        exists = file.exists(),
    )
}
