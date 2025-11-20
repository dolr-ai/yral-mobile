package com.yral.shared.libs.fileio

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAssetResourceTypeVideo
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosFileDownloader : FileDownloader {
    private val logger = Logger.withTag("IosFileDownloader")

    @Suppress("TooGenericExceptionCaught")
    override suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean,
    ): Result<String, YralException> =
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Starting download: $fileName from $url" }

                val nsUrl = NSURL.URLWithString(url)
                if (nsUrl == null) {
                    logger.e { "Invalid URL: $url" }
                    return@withContext Err(
                        YralException("Invalid file URL"),
                    )
                }

                val fileData = NSData.dataWithContentsOfURL(nsUrl)
                if (fileData == null) {
                    logger.e { "Failed to download file data" }
                    return@withContext Err(
                        YralException("Failed to download file"),
                    )
                }

                val tempDir = NSTemporaryDirectory()
                val tempFilePath = "$tempDir/$fileName"
                val tempURL = NSURL.fileURLWithPath(tempFilePath)

                val writeSuccess = fileData.writeToURL(tempURL, atomically = true)
                if (!writeSuccess) {
                    logger.e { "Failed to write file to temp location" }
                    return@withContext Err(
                        YralException("Failed to save file temporarily"),
                    )
                }

                logger.d { "Downloaded to temp file: $tempFilePath" }

                if (saveToGallery) {
                    saveToPhotoLibrary(tempURL, fileName)
                } else {
                    Ok(tempFilePath)
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

    private suspend fun saveToPhotoLibrary(
        fileURL: NSURL,
        fileName: String,
    ): Result<String, YralException> {
        val authStatus = PHPhotoLibrary.authorizationStatus()
        if (authStatus != PHAuthorizationStatusAuthorized && authStatus != PHAuthorizationStatusLimited) {
            logger.e { "Photo library access denied" }
            return Err(
                YralException("Photo library access denied. Please enable photo library access in Settings."),
            )
        }

        val fileType = fileName.getFileType()

        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    val resourceType =
                        when (fileType) {
                            FileType.VIDEO -> PHAssetResourceTypeVideo
                            FileType.IMAGE -> PHAssetResourceTypePhoto
                            else -> PHAssetResourceTypeVideo
                        }

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
                        logger.e { "Failed to save to photo library: ${error?.localizedDescription}" }
                        continuation.resume(
                            Err(
                                YralException("Failed to save file: ${error?.localizedDescription ?: "Unknown error"}"),
                            ),
                        )
                    }
                },
            )
        }
    }
}
