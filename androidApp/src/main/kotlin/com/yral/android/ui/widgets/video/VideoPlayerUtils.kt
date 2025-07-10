package com.yral.android.ui.widgets.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

@Suppress("TooManyFunctions")
object VideoPlayerUtils {
    // Video validation constants (matching iOS implementation)
    const val VIDEO_MAX_DURATION_SECONDS = 60.0
    const val VIDEO_MAX_FILE_SIZE_BYTES = 200L * 1024L * 1024L // 200 MB

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

    /**
     * Get video duration in seconds from a file path
     */
    fun getVideoDuration(filePath: String): Double? =
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toDoubleOrNull()?.div(1000.0) // Convert milliseconds to seconds
        }.onFailure { exception ->
            Logger.e("Error getting video duration for: $filePath", exception)
        }.getOrNull()

    /**
     * Get video duration in seconds from a URI
     */
    fun getVideoDuration(context: Context, uri: Uri): Double? =
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toDoubleOrNull()?.div(1000.0) // Convert milliseconds to seconds
        }.onFailure { exception ->
            Logger.e("Error getting video duration for URI: $uri", exception)
        }.getOrNull()

    /**
     * Get video file size from a URI
     */
    fun getVideoFileSize(context: Context, uri: Uri): Long? =
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                fileDescriptor.statSize
            }
        }.onFailure { exception ->
            Logger.e("Error getting video file size for URI: $uri", exception)
        }.getOrNull()

    fun validateVideoFromUri(context: Context, uri: Uri): Result<ValidationSuccess, ValidationError> =
        validateVideo(
            getDuration = { getVideoDuration(context, uri) },
            getFileSize = { getVideoFileSize(context, uri) }
        )

    fun validateVideoFromPath(filePath: String): Result<ValidationSuccess, ValidationError> =
        validateVideo(
            getDuration = { getVideoDuration(filePath) },
            getFileSize = { getVideoFileSize(filePath) }
        )

    private fun validateVideo(
        getDuration: () -> Double?,
        getFileSize: () -> Long?
    ): Result<ValidationSuccess, ValidationError> {
        val duration = getDuration()
        if (duration == null) {
            return Err(ValidationError.UnableToReadDuration)
        }
        if (duration > VIDEO_MAX_DURATION_SECONDS) {
            return Err(ValidationError.DurationExceedsLimit(duration, VIDEO_MAX_DURATION_SECONDS))
        }

        val fileSize = getFileSize()
        if (fileSize == null) {
            return Err(ValidationError.UnableToReadFileSize)
        }
        if (fileSize > VIDEO_MAX_FILE_SIZE_BYTES) {
            return Err(ValidationError.FileSizeExceedsLimit(fileSize, VIDEO_MAX_FILE_SIZE_BYTES))
        }

        return Ok(ValidationSuccess(duration, fileSize))
    }

    sealed class ValidationError {
        object UnableToReadDuration : ValidationError()
        object UnableToReadFileSize : ValidationError()
        data class DurationExceedsLimit(val actual: Double, val limit: Double) : ValidationError()
        data class FileSizeExceedsLimit(val actual: Long, val limit: Long) : ValidationError()
    }

    data class ValidationSuccess(
        val duration: Double,
        val fileSize: Long
    )

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
    fun formatFileSize(bytes: Long, precision: Int = 1): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toFloat()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.${precision}f %s".format(size, units[unitIndex])
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
