package com.yral.shared.libs.filedownloader

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException

class AndroidFileDownloader(
    private val context: Context,
) : FileDownloader {
    private val logger = Logger.withTag("AndroidFileDownloader")

    private val httpClient =
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT
                socketTimeoutMillis = TIMEOUT
            }
            expectSuccess = true
        }

    override suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean,
    ): Result<String, YralException> =
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Starting download: $fileName from $url" }

                val tempFile = downloadWithHttp(url, fileName)

                logger.d { "Downloaded to temp file: ${tempFile.absolutePath}" }
                val savedPath =
                    if (saveToGallery) {
                        saveToGalleryStorage(tempFile, fileName)
                    } else {
                        saveToAppStorage(tempFile, fileName)
                    }
                try {
                    tempFile.delete()
                    logger.d { "File saved to: $savedPath" }
                } catch (
                    @Suppress("TooGenericExceptionCaught")
                    e: Exception,
                ) {
                    logger.e(e) { "Error deleting temp file: $e" }
                }
                Ok(savedPath)
            } catch (e: SocketTimeoutException) {
                logger.e(e) { "Timeout downloading file: $fileName" }
                Err(
                    YralException(
                        message = "Download timeout. Please check your internet connection and try again.",
                        cause = e,
                    ),
                )
            } catch (e: TimeoutCancellationException) {
                logger.e(e) { "Connection timeout downloading file: $fileName" }
                Err(
                    YralException(
                        message = "Connection timeout. Please check your internet connection and try again.",
                        cause = e,
                    ),
                )
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                logger.e(e) { "Failed to download file: $fileName" }
                Err(
                    YralException(
                        message = "Failed to download file: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

    private suspend fun downloadWithHttp(
        url: String,
        fileName: String,
    ): File {
        // Download file using Ktor with timeout
        val response: HttpResponse = httpClient.get(url)
        val fileData: ByteReadChannel = response.bodyAsChannel()
        // Save to temp file
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (!fileData.isClosedForRead) {
                val bytesRead = fileData.readAvailable(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
            }
        }
        return tempFile
    }

    private fun saveToGalleryStorage(
        tempFile: File,
        fileName: String,
    ): String {
        val fileType = fileName.getFileType()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStoreQ(tempFile, fileName, fileType)
        } else {
            saveToLegacyStorage(tempFile, fileName, fileType)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStoreQ(
        tempFile: File,
        fileName: String,
        fileType: FileType,
    ): String {
        val (collection, mimeType, relativePath) =
            when (fileType) {
                FileType.VIDEO ->
                    Triple(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        "video/mp4",
                        Environment.DIRECTORY_MOVIES + "/YRAL",
                    )

                FileType.IMAGE ->
                    Triple(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "image/*",
                        Environment.DIRECTORY_PICTURES + "/YRAL",
                    )

                else ->
                    Triple(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "application/octet-stream",
                        Environment.DIRECTORY_DOWNLOADS + "/YRAL",
                    )
            }

        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

        val resolver = context.contentResolver
        val uri =
            resolver.insert(collection, contentValues)
                ?: error("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: error("Failed to open output stream")

        return uri.toString()
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyStorage(
        tempFile: File,
        fileName: String,
        fileType: FileType,
    ): String {
        val baseDir =
            when (fileType) {
                FileType.VIDEO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                FileType.IMAGE -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }

        val yralDir = File(baseDir, "YRAL")
        if (!yralDir.exists()) {
            yralDir.mkdirs()
        }

        val destFile = File(yralDir, fileName)
        tempFile.copyTo(destFile, overwrite = true)

        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    when (fileType) {
                        FileType.VIDEO -> "video/mp4"
                        FileType.IMAGE -> "image/*"
                        else -> "application/octet-stream"
                    },
                )
            }

        val collection =
            when (fileType) {
                FileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                FileType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }

        context.contentResolver.insert(collection, contentValues)

        return destFile.absolutePath
    }

    private fun saveToAppStorage(
        tempFile: File,
        fileName: String,
    ): String {
        val downloadsDir = File(context.filesDir, "downloads")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val destFile = File(downloadsDir, fileName)
        tempFile.copyTo(destFile, overwrite = true)

        return destFile.absolutePath
    }
}
