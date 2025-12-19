package com.yral.shared.features.chat.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.attachments.persistUrlToChatCache
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
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
                    val imageUrl = imageToTempUrl(image)
                    if (imageUrl != null) {
                        persistUrlToChatCache(url = imageUrl)
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
    private fun imageToTempUrl(image: UIImage): NSURL? {
        val imageData = UIImageJPEGRepresentation(image, 1.0) ?: UIImagePNGRepresentation(image)
        if (imageData == null) {
            chatImagePickerLogger.e { "Failed to convert image to data" }
            return null
        }

        val tempDir = NSTemporaryDirectory()
        val fileName = "chat_image_${Clock.System.now().toEpochMilliseconds()}.jpg"
        val tempUrl = NSURL.fileURLWithPath("$tempDir$fileName")

        if (imageData.writeToURL(tempUrl, atomically = true)) {
            return tempUrl
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
