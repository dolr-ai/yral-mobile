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
    fun getInternalVideoPath(
        context: Context,
        fileName: String,
    ): String =
        File(context.filesDir, "videos")
            .apply {
                if (!exists()) mkdirs()
            }.absolutePath + "/$fileName"

    fun getExternalVideoPath(fileName: String): String? =
        runCatching {
            val moviesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (!moviesDir.exists()) moviesDir.mkdirs()
            "${moviesDir.absolutePath}/$fileName"
        }.onFailure { exception ->
            val errorMessage =
                when (exception) {
                    is SecurityException -> "Permission denied accessing external storage"
                    is IllegalArgumentException -> "Invalid directory type or filename: $fileName"
                    else -> "Unexpected error accessing external storage for: $fileName"
                }
            Logger.e(errorMessage, exception)
        }.getOrNull()

    fun videoFileExists(filePath: String): Boolean =
        runCatching {
            val file = File(filePath)
            file.exists() && file.isFile && file.canRead()
        }.onFailure { exception ->
            Logger.e("Permission denied accessing file: $filePath", exception)
        }.getOrDefault(false)

    fun getVideoFileSize(filePath: String): Long =
        runCatching {
            File(filePath).length()
        }.onFailure { exception ->
            Logger.e("Permission denied accessing file: $filePath", exception)
        }.getOrDefault(0L)

    fun copyVideoFromAssets(
        context: Context,
        assetFileName: String,
        targetFileName: String,
    ): String? {
        val targetPath = getInternalVideoPath(context, targetFileName)
        return runCatching {
            val targetFile = File(targetPath)
            // Don't copy if file already exists
            if (targetFile.exists()) {
                return@runCatching targetPath
            }
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetPath
        }.onFailure { exception ->
            val errorMessage =
                when (exception) {
                    is FileNotFoundException -> "Asset file not found: $assetFileName"
                    is IOException -> "IO error copying video from assets: $assetFileName"
                    is SecurityException -> "Permission denied copying video to: $targetPath"
                    else -> "Unexpected error copying video from assets: $assetFileName"
                }
            Logger.e(errorMessage, exception)
        }.getOrNull()
    }

    fun copyVideoFromUri(
        context: Context,
        sourceUri: Uri,
        targetFileName: String,
    ): String? {
        val targetPath = getInternalVideoPath(context, targetFileName)
        return runCatching {
            val targetFile = File(targetPath)
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetPath
        }.onFailure { exception ->
            val errorMessage =
                when (exception) {
                    is FileNotFoundException -> "Source URI not found: $sourceUri"
                    is IOException -> "IO error copying video from URI: $sourceUri"
                    is SecurityException -> "Permission denied accessing URI or target file: $sourceUri -> $targetPath"
                    else -> "Unexpected error copying video from URI: $sourceUri"
                }
            Logger.e(errorMessage, exception)
        }.getOrNull()
    }

    fun deleteVideoFile(filePath: String): Boolean =
        runCatching {
            File(filePath).delete()
        }.onFailure { exception ->
            Logger.e("Permission denied deleting file: $filePath", exception)
        }.getOrDefault(false)

    fun getInternalVideoFiles(context: Context): List<String> {
        return runCatching {
            val videosDir = File(context.filesDir, "videos")
            if (!videosDir.exists()) return@runCatching emptyList()
            videosDir
                .listFiles { file ->
                    file.isFile && isVideoFile(file.name)
                }?.map { it.absolutePath } ?: emptyList()
        }.onFailure { exception ->
            Logger.e("Permission denied accessing videos directory", exception)
        }.getOrDefault(emptyList())
    }

    fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "webm", "3gp", "m4v")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in videoExtensions
    }

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

    data class WidgetVideoConfig(
        val autoPlay: Boolean = true,
        val loop: Boolean = true,
        val showControls: Boolean = false,
        val muted: Boolean = true, // Widgets should typically be muted
    )

    fun getWidgetVideoConfig(): WidgetVideoConfig =
        WidgetVideoConfig(
            autoPlay = true,
            loop = true,
            showControls = false,
            muted = true,
        )
}

data class VideoFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val exists: Boolean,
    val formattedSize: String = VideoPlayerUtils.formatFileSize(size),
)

fun String.toVideoFileInfo(): VideoFileInfo =
    runCatching {
        val file = File(this)
        VideoFileInfo(
            path = this,
            name = file.name,
            size = if (file.exists()) file.length() else 0L,
            exists = file.exists(),
        )
    }.onFailure { exception ->
        Logger.e("Error getting video file info for: $this", exception)
    }.getOrElse {
        VideoFileInfo(
            path = this,
            name = File(this).name,
            size = 0L,
            exists = false,
        )
    }
