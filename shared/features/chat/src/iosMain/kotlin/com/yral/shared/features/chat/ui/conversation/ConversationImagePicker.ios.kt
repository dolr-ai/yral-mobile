package com.yral.shared.features.chat.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.attachments.ChatImageAttachmentMetadata
import com.yral.shared.features.chat.attachments.ChatPickedImageFormat
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.attachments.buildPickedChatImageMetadata
import com.yral.shared.features.chat.attachments.persistUrlToChatCache
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val chatImagePickerLogger = Logger.withTag("ChatImagePicker")

@Composable
actual fun rememberChatImagePicker(onImagePicked: (FilePathChatAttachment) -> Unit): () -> Unit =
    rememberIosChatImagePicker(
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
        onImagePicked = onImagePicked,
    )

@Composable
actual fun rememberChatImageCapture(onImagePicked: (FilePathChatAttachment) -> Unit): () -> Unit =
    rememberIosChatImagePicker(
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
        onImagePicked = onImagePicked,
    )

@Composable
private fun rememberIosChatImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onImagePicked: (FilePathChatAttachment) -> Unit,
): () -> Unit {
    val latestOnImagePicked = rememberUpdatedState(onImagePicked)
    var coordinator by remember { mutableStateOf<IosChatImagePickerCoordinator?>(null) }

    return remember(sourceType) {
        {
            if (coordinator == null) {
                val newCoordinator =
                    IosChatImagePickerCoordinator(
                        sourceType = sourceType,
                        onImagePicked = { attachment -> latestOnImagePicked.value(attachment) },
                        onDismiss = { coordinator = null },
                    )
                val isPresented = newCoordinator.presentPicker()
                if (isPresented) {
                    coordinator = newCoordinator
                } else {
                    chatImagePickerLogger.w {
                        "Unable to present chat image picker (source=$sourceType)"
                    }
                }
            }
        }
    }
}

private class IosChatImagePickerCoordinator(
    private val sourceType: UIImagePickerControllerSourceType,
    private val onImagePicked: (FilePathChatAttachment) -> Unit,
    private val onDismiss: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    @Suppress("ReturnCount")
    fun presentPicker(): Boolean {
        val rootController = findRootViewController()
        if (rootController == null) {
            chatImagePickerLogger.e { "Unable to find root view controller for chat image picker" }
            return false
        }

        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
            chatImagePickerLogger.w { "Source type $sourceType not available for chat image picker" }
            return false
        }

        val picker = UIImagePickerController()
        picker.delegate = this
        picker.sourceType = sourceType
        picker.allowsEditing = false
        picker.modalPresentationStyle = UIModalPresentationFullScreen

        if (sourceType == UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) {
            picker.cameraCaptureMode =
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto
        }

        rootController.presentViewController(picker, true, null)
        return true
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image =
            (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage)
                ?: (didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)

        picker.dismissViewControllerAnimated(true) {
            if (image != null) {
                runCatching {
                    val tempImage = imageToTempFile(image)
                    if (tempImage != null) {
                        persistUrlToChatCache(
                            url = tempImage.url,
                            contentTypeOverride = tempImage.metadata.contentType,
                            fileNameOverride = tempImage.metadata.fileName,
                        )
                    } else {
                        error("Failed to create temp URL for image")
                    }
                }.onSuccess { attachment ->
                    onImagePicked(attachment)
                }.onFailure { throwable ->
                    chatImagePickerLogger.e(throwable) { "Failed to persist selected image to cache" }
                }
            } else {
                chatImagePickerLogger.e { "Failed to get image from picker" }
            }
            onDismiss()
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            onDismiss()
        }
    }

    @Suppress("ReturnCount")
    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    private fun imageToTempFile(image: UIImage): PickedChatImageTempFile? {
        val timestampMs = Clock.System.now().toEpochMilliseconds()
        val preparedImage = image.downscaledForChatUpload()
        val jpegData = preparedImage.encodedAsChatJpeg()
        val (imageData, metadata) =
            if (jpegData != null) {
                jpegData to buildPickedChatImageMetadata(timestampMs, ChatPickedImageFormat.JPEG)
            } else {
                UIImagePNGRepresentation(preparedImage) to
                    buildPickedChatImageMetadata(timestampMs, ChatPickedImageFormat.PNG)
            }

        if (imageData == null) {
            chatImagePickerLogger.e { "Failed to convert image to data" }
            return null
        }
        chatImagePickerLogger.d {
            "Prepared chat image upload bytes=${imageData.length.toLong()} format=${metadata.contentType}"
        }

        val tempDir = NSTemporaryDirectory()
        val tempUrl = NSURL.fileURLWithPath("$tempDir${metadata.fileName}")

        if (imageData.writeToURL(tempUrl, atomically = true)) {
            return PickedChatImageTempFile(url = tempUrl, metadata = metadata)
        } else {
            chatImagePickerLogger.e { "Failed to write image data to temp URL" }
            return null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("ReturnCount")
    private fun findRootViewController(): UIViewController? {
        val application = UIApplication.sharedApplication

        // Try application.windows first (deprecated but still works)
        application.windows.let { windows ->
            for (windowObj in windows) {
                val window = windowObj as? UIWindow ?: continue
                if (window.isKeyWindow()) {
                    return window.rootViewController
                }
            }
        }

        // Try keyWindow (deprecated but fallback)
        application.keyWindow?.let { keyWindow ->
            return keyWindow.rootViewController
        }

        // Try connectedScenes (iOS 13+)
        val connectedScenes = application.connectedScenes
        for (sceneObj in connectedScenes) {
            val windowScene = sceneObj as? UIWindowScene ?: continue
            val windows = windowScene.windows
            for (windowObj in windows) {
                val window = windowObj as? UIWindow ?: continue
                if (window.isKeyWindow()) {
                    return window.rootViewController
                }
            }
        }

        return null
    }
}

private data class PickedChatImageTempFile(
    val url: NSURL,
    val metadata: ChatImageAttachmentMetadata,
)

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.downscaledForChatUpload(): UIImage {
    val imageSize =
        size.useContents {
            width to height
        }
    val currentWidth = imageSize.first
    val currentHeight = imageSize.second
    val longestEdge = maxOf(currentWidth, currentHeight)
    if (longestEdge <= CHAT_IMAGE_MAX_DIMENSION_PX) {
        return this
    }

    val scaleRatio = CHAT_IMAGE_MAX_DIMENSION_PX / longestEdge
    val targetWidth = currentWidth * scaleRatio
    val targetHeight = currentHeight * scaleRatio

    UIGraphicsBeginImageContextWithOptions(
        size = CGSizeMake(width = targetWidth, height = targetHeight),
        opaque = false,
        scale = 1.0,
    )
    drawInRect(
        CGRectMake(
            x = 0.0,
            y = 0.0,
            width = targetWidth,
            height = targetHeight,
        ),
    )
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return resizedImage ?: this
}

private fun UIImage.encodedAsChatJpeg() =
    CHAT_IMAGE_JPEG_QUALITIES
        .firstNotNullOfOrNull { quality ->
            UIImageJPEGRepresentation(this, quality)
                ?.takeIf { data -> data.length.toLong() <= CHAT_IMAGE_MAX_UPLOAD_BYTES }
        } ?: UIImageJPEGRepresentation(this, CHAT_IMAGE_JPEG_QUALITIES.last())

private const val CHAT_IMAGE_MAX_DIMENSION_PX = 1920.0
private const val CHAT_IMAGE_MAX_UPLOAD_BYTES = 2L * 1024L * 1024L
private const val CHAT_IMAGE_JPEG_QUALITY_HIGH = 0.85
private const val CHAT_IMAGE_JPEG_QUALITY_MEDIUM = 0.75
private const val CHAT_IMAGE_JPEG_QUALITY_LOW = 0.65
private const val CHAT_IMAGE_JPEG_QUALITY_MIN = 0.55
private val CHAT_IMAGE_JPEG_QUALITIES =
    listOf(
        CHAT_IMAGE_JPEG_QUALITY_HIGH,
        CHAT_IMAGE_JPEG_QUALITY_MEDIUM,
        CHAT_IMAGE_JPEG_QUALITY_LOW,
        CHAT_IMAGE_JPEG_QUALITY_MIN,
    )
