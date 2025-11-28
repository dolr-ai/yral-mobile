package com.yral.shared.libs.filedownloader

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUserDomainMask
import platform.Foundation.downloadTaskWithURL
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAssetResourceTypeVideo
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("MagicNumber")
@OptIn(ExperimentalForeignApi::class)
class IosFileDownloader : FileDownloader {
    private val logger = Logger.withTag("IosFileDownloader")

    private val httpClient =
        HttpClient(Darwin) {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT
                socketTimeoutMillis = TIMEOUT
            }
            expectSuccess = true
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean,
    ): Result<String, YralException> =
        withContext(Dispatchers.Default) {
            try {
                logger.d { "Starting download: $fileName from $url" }
                val tempPath = downloadWithDownloadTask(url)
                val stablePath = copyFileToDocuments(tempPath, fileName)

                logger.d { "Downloaded to temp file: $tempPath stable file: $stablePath" }
                if (saveToGallery) {
                    val fileURL = NSURL.fileURLWithPath(stablePath)
                    val result = saveToPhotoLibrary(fileURL, fileName)
                    if (result.isOk) {
                        deleteFileAtPath(stablePath)
                    }
                    return@withContext result
                } else {
                    Ok(tempPath)
                }
            } catch (e: Exception) {
                logger.e(e) { "Exception while downloading file" }
                Err(
                    YralException(
                        message = "Failed to download file: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

    @Suppress("UnusedPrivateMember")
    private suspend fun downloadWithHttp(
        url: String,
        fileName: String,
    ): String {
        val response: HttpResponse = httpClient.get(url)
        val fileDataChannel: ByteReadChannel = response.bodyAsChannel()
        val tempDir = NSTemporaryDirectory()
        val tempFilePath = "$tempDir/$fileName"
        val tempPath = Path(tempFilePath)
        SystemFileSystem.sink(tempPath).buffered().use {
            val buffer = ByteArray(BUFFER_SIZE)
            while (!fileDataChannel.isClosedForRead) {
                val bytesRead = fileDataChannel.readAvailable(buffer)
                if (bytesRead == -1) break
                it.write(buffer, 0, bytesRead)
            }
        }
        return tempFilePath
    }

    private suspend fun downloadWithDownloadTask(url: String): String =
        suspendCancellableCoroutine { cont ->
            val nsUrl = NSURL(string = url)
            val task =
                NSURLSession.sharedSession.downloadTaskWithURL(
                    url = nsUrl,
                    completionHandler = { tempLocation, _, error ->
                        if (error != null) {
                            cont.resumeWithException(Throwable(error.localizedDescription))
                            return@downloadTaskWithURL
                        }
                        tempLocation?.let { tempUrl ->
                            cont.resume(tempUrl.path ?: "")
                        } ?: cont.resumeWithException(Throwable("Temp file location is null"))
                    },
                )
            cont.invokeOnCancellation { task.cancel() }
            task.resume()
        }

    private fun copyFileToDocuments(
        tempPath: String,
        fileName: String,
    ): String {
        val fileManager = NSFileManager.defaultManager
        val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val destPath = "$docsDir/$fileName"

        val srcUrl = NSURL.fileURLWithPath(tempPath)
        val destUrl = NSURL.fileURLWithPath(destPath)

        // Remove if exists
        if (fileManager.fileExistsAtPath(destPath)) {
            fileManager.removeItemAtURL(destUrl, null)
        }

        val success = fileManager.copyItemAtURL(srcUrl, destUrl, null)
        if (!success) {
            throw YralException("Failed to copy file to Documents")
        }
        return destPath
    }

    private fun deleteFileAtPath(filePath: String) {
        val fileManager = NSFileManager.defaultManager
        val fileUrl = NSURL.fileURLWithPath(filePath)
        try {
            if (fileManager.fileExistsAtPath(filePath)) {
                fileManager.removeItemAtURL(fileUrl, null)
            }
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Throwable,
        ) {
            // Log but don’t crash if deletion fails
            logger.e { "Failed to delete file at $filePath: ${e.message}" }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun saveToPhotoLibrary(
        fileURL: NSURL,
        fileName: String,
    ): Result<String, YralException> {
        val resourceType =
            when (val fileType = fileName.getFileType()) {
                FileType.VIDEO -> PHAssetResourceTypeVideo
                FileType.IMAGE -> PHAssetResourceTypePhoto
                else -> return Err(YralException("Unsupported file type: $fileType"))
            }

        var authStatus = PHPhotoLibrary.authorizationStatus()
        if (authStatus == PHAuthorizationStatusNotDetermined) {
            authStatus = requestPhotoLibraryPermission()
        }
        if (authStatus != PHAuthorizationStatusAuthorized && authStatus != PHAuthorizationStatusLimited) {
            logger.e { "Photo library access denied: $authStatus" }
            return Err(
                YralException("Photo library access denied. Please enable photo library access in Settings."),
            )
        }

        logger.d { "Photo library access granted" }
        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    request.addResourceWithType(
                        type = resourceType,
                        fileURL = fileURL,
                        options = null,
                    )
                },
                completionHandler = { success, error ->
                    if (success) {
                        logger.d { "File saved to photo library successfully" }
                        continuation.resume(Ok("Photo Library"))
                    } else {
                        logger.e { "Failed to save to photo library: ${error?.localizedDescription()}" }
                        continuation.resume(
                            Err(
                                YralException(
                                    "Failed to save file: ${error?.localizedDescription() ?: "Unknown error"}",
                                ),
                            ),
                        )
                    }
                },
            )
        }
    }

    private suspend fun requestPhotoLibraryPermission(): PHAuthorizationStatus =
        suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorization { status ->
                continuation.resume(status)
            }
        }
}
