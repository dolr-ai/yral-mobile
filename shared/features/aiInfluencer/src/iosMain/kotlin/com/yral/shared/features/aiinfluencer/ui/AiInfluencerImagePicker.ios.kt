package com.yral.shared.features.aiinfluencer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
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
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.posix.memcpy

private val influencerPickerLogger = Logger.withTag("AiInfluencerImagePicker")

@Composable
actual fun rememberAiInfluencerImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit =
    rememberIosInfluencerImagePicker(
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
        onImagePicked = onImagePicked,
    )

@Composable
actual fun rememberAiInfluencerPhotoCapture(onImagePicked: (ByteArray) -> Unit): () -> Unit =
    rememberIosInfluencerImagePicker(
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
        onImagePicked = onImagePicked,
    )

@Composable
private fun rememberIosInfluencerImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onImagePicked: (ByteArray) -> Unit,
): () -> Unit {
    val latestOnImagePicked = rememberUpdatedState(onImagePicked)
    var coordinator by remember { mutableStateOf<IosInfluencerImagePickerCoordinator?>(null) }

    return remember(sourceType) {
        {
            if (coordinator == null) {
                val newCoordinator =
                    IosInfluencerImagePickerCoordinator(
                        sourceType = sourceType,
                        onImagePicked = { bytes -> latestOnImagePicked.value(bytes) },
                        onDismiss = { coordinator = null },
                    )
                val isPresented = newCoordinator.presentPicker()
                if (isPresented) {
                    coordinator = newCoordinator
                } else {
                    influencerPickerLogger.w {
                        "Unable to present influencer image picker (source=$sourceType)"
                    }
                }
            }
        }
    }
}

private class IosInfluencerImagePickerCoordinator(
    private val sourceType: UIImagePickerControllerSourceType,
    private val onImagePicked: (ByteArray) -> Unit,
    private val onDismiss: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    @Suppress("ReturnCount")
    fun presentPicker(): Boolean {
        val rootController = findRootViewController()
        if (rootController == null) {
            influencerPickerLogger.e { "Unable to find root view controller for influencer picker" }
            return false
        }

        if (!UIImagePickerController.Companion.isSourceTypeAvailable(sourceType)) {
            influencerPickerLogger.w { "Source type $sourceType not available for influencer picker" }
            return false
        }

        val picker = UIImagePickerController()
        picker.delegate = this
        picker.sourceType = sourceType
        picker.allowsEditing = false
        picker.modalPresentationStyle = UIModalPresentationFullScreen
        if (sourceType ==
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        ) {
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
            val bytes = image?.toByteArray()
            if (bytes != null) {
                onImagePicked(bytes)
            } else {
                influencerPickerLogger.e { "Failed to convert selected influencer image to bytes" }
            }
            onDismiss()
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            onDismiss()
        }
    }
}

@Suppress("ReturnCount")
private fun findRootViewController(): UIViewController? {
    val application = UIApplication.sharedApplication

    application.windows.let { windows ->
        for (windowObj in windows) {
            val window = windowObj as? UIWindow ?: continue
            if (window.isKeyWindow()) {
                return topViewController(window.rootViewController)
            }
        }
    }

    application.keyWindow?.let { keyWindow ->
        return topViewController(keyWindow.rootViewController)
    }

    val connectedScenes = application.connectedScenes
    for (sceneObj in connectedScenes) {
        val windowScene = sceneObj as? UIWindowScene ?: continue
        val windows = windowScene.windows
        for (windowObj in windows) {
            val window = windowObj as? UIWindow ?: continue
            if (window.isKeyWindow()) {
                return topViewController(window.rootViewController)
            }
        }
    }

    return null
}

private fun topViewController(controller: UIViewController?): UIViewController? {
    val presented = controller?.presentedViewController
    return when {
        controller == null -> null
        presented != null -> topViewController(presented)
        controller is UINavigationController -> topViewController(controller.visibleViewController)
        controller is UITabBarController -> topViewController(controller.selectedViewController)
        else -> controller
    }
}

@Suppress("DEPRECATION")
private fun UIImage.toByteArray(): ByteArray? {
    val jpegData = UIImageJPEGRepresentation(this, JPEG_COMPRESSION_QUALITY)
    if (jpegData != null) {
        return jpegData.toByteArraySafe()
    }
    val pngData = UIImagePNGRepresentation(this)
    return pngData?.toByteArraySafe()
}

private const val JPEG_COMPRESSION_QUALITY = 0.9

@Suppress("ReturnCount")
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArraySafe(): ByteArray? {
    val length = this.length.toInt()
    if (length == 0) {
        return ByteArray(0)
    }
    val byteArray = ByteArray(length)
    val pinned = byteArray.usePinned { it.addressOf(0) }
    memcpy(pinned, this.bytes, length.toULong())
    return byteArray
}
