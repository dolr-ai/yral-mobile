package com.yral.shared.features.uploadvideo.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import co.touchlab.kermit.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class VideoFileManager {
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
}
