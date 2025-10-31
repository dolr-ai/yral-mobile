package com.yral.shared.features.uploadvideo.ui.fileUpload

import com.github.michaelbull.result.fold
import com.yral.shared.features.uploadvideo.utils.VideoValidationError
import com.yral.shared.features.uploadvideo.utils.VideoValidator
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerQualityTypeHigh
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MOVIE_MEDIA_TYPE = "public.movie"
private const val DEFAULT_VIDEO_EXTENSION = "mp4"

private object IosVideoPickerStrings {
    const val PICKER_OPTIONS_TITLE = "Select Video Source"
    const val CAMERA_ACTION_TITLE = "Camera"
    const val PHOTO_ACTION_TITLE = "Photo Library"
    const val FILE_MANAGER_ACTION_TITLE = "File Manager"
    const val CANCEL_ACTION_TITLE = "Cancel"
}

internal class IosVideoPickerCoordinator(
    private val maxSeconds: Int,
    private val coroutineScope: CoroutineScope,
    private val videoValidator: VideoValidator,
    private val onProcessingStateChange: (Boolean) -> Unit,
    private val onVideoSelected: (String) -> Unit,
    private val onError: (VideoPickerError) -> Unit,
    private val onDismiss: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol,
    UIDocumentPickerDelegateProtocol {
    private var presentingViewController: UIViewController? = null

    @OptIn(ExperimentalForeignApi::class)
    fun presentPicker(): Boolean {
        val rootController = findRootViewController() ?: return false
        presentingViewController = rootController

        val alertController =
            UIAlertController.Companion.alertControllerWithTitle(
                title = IosVideoPickerStrings.PICKER_OPTIONS_TITLE,
                message = null,
                preferredStyle = UIAlertControllerStyleActionSheet,
            )

        if (UIImagePickerController.Companion.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
            )
        ) {
            alertController.addAction(
                UIAlertAction.Companion.actionWithTitle(
                    title = IosVideoPickerStrings.CAMERA_ACTION_TITLE,
                    style = UIAlertActionStyleDefault,
                    handler = { _ ->
                        openCamera()
                    },
                ),
            )
        }

        if (UIImagePickerController.Companion.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
            )
        ) {
            alertController.addAction(
                UIAlertAction.Companion.actionWithTitle(
                    title = IosVideoPickerStrings.PHOTO_ACTION_TITLE,
                    style = UIAlertActionStyleDefault,
                    handler = { _ ->
                        openPhotoLibrary()
                    },
                ),
            )
        }

        alertController.addAction(
            UIAlertAction.Companion.actionWithTitle(
                title = IosVideoPickerStrings.FILE_MANAGER_ACTION_TITLE,
                style = UIAlertActionStyleDefault,
                handler = { _ ->
                    openDocumentPicker()
                },
            ),
        )

        alertController.addAction(
            UIAlertAction.Companion.actionWithTitle(
                title = IosVideoPickerStrings.CANCEL_ACTION_TITLE,
                style = UIAlertActionStyleCancel,
                handler = { _ ->
                    onDismiss()
                },
            ),
        )

        rootController.presentViewController(alertController, true, null)
        return true
    }

    private fun openCamera() {
        val controller = presentingViewController ?: return
        val picker = UIImagePickerController()
        picker.delegate = this
        picker.sourceType =
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.mediaTypes = listOf(MOVIE_MEDIA_TYPE)
        picker.videoQuality = UIImagePickerControllerQualityTypeHigh
        picker.cameraCaptureMode =
            UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModeVideo
        picker.videoMaximumDuration = maxSeconds.toDouble()
        controller.presentViewController(picker, true, null)
    }

    private fun openPhotoLibrary() {
        val controller = presentingViewController ?: return
        val picker = UIImagePickerController()
        picker.delegate = this
        picker.sourceType =
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.mediaTypes = listOf(MOVIE_MEDIA_TYPE)
        picker.modalPresentationStyle = UIModalPresentationFullScreen
        controller.presentViewController(picker, true, null)
    }

    private fun openDocumentPicker() {
        val controller = presentingViewController ?: return
        val documentPicker =
            UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeMovie),
            )
        documentPicker.delegate = this
        documentPicker.allowsMultipleSelection = false
        controller.presentViewController(documentPicker, true, null)
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val mediaUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
        picker.dismissViewControllerAnimated(true) {
            if (mediaUrl != null) {
                handlePickedUrl(mediaUrl, isSecurityScoped = false)
            } else {
                onError(VideoPickerError.ProcessingFailed)
                onDismiss()
            }
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            onDismiss()
        }
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        controller.dismissViewControllerAnimated(true, null)
        if (url != null) {
            handlePickedUrl(url, isSecurityScoped = true)
        } else {
            onError(VideoPickerError.ProcessingFailed)
            onDismiss()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, null)
        onDismiss()
    }

    private fun handlePickedUrl(
        url: NSURL,
        isSecurityScoped: Boolean,
    ) {
        coroutineScope.launch {
            val startedAccess =
                if (isSecurityScoped) {
                    url.startAccessingSecurityScopedResource()
                } else {
                    false
                }

            if (isSecurityScoped && !startedAccess) {
                onError(VideoPickerError.ProcessingFailed)
                onDismiss()
                return@launch
            }

            onProcessingStateChange(true)

            val processingResult =
                runCatching { processVideo(url) }
                    .getOrElse { ProcessingResult.Failure }

            if (startedAccess) {
                url.stopAccessingSecurityScopedResource()
            }

            when (processingResult) {
                is ProcessingResult.Success -> onVideoSelected(processingResult.filePath)
                is ProcessingResult.ValidationFailure ->
                    onError(VideoPickerError.Validation(processingResult.error))
                ProcessingResult.Failure -> onError(VideoPickerError.ProcessingFailed)
            }

            onProcessingStateChange(false)
            onDismiss()
        }
    }

    private suspend fun processVideo(url: NSURL): ProcessingResult =
        withContext(Dispatchers.Default) {
            videoValidator
                .validateVideo(
                    getDuration = { getVideoDurationSeconds(url) },
                    getFileSize = { getVideoFileSize(url) },
                ).fold(
                    success = {
                        if (url.isFileURL()) {
                            ProcessingResult.Success(requireNotNull(url.path))
                        } else {
                            ProcessingResult.Failure
                        }
//                        val copiedPath = copyVideoToTemporaryLocation(url)
//                        if (copiedPath != null) {
//                            ProcessingResult.Success(url)
//                        } else {
//                            ProcessingResult.Failure
//                        }
                    },
                    failure = { error ->
                        ProcessingResult.ValidationFailure(error)
                    },
                )
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun getVideoDurationSeconds(url: NSURL): Double? {
        val asset = AVURLAsset.Companion.URLAssetWithURL(url, options = null)
        val durationSeconds = CMTimeGetSeconds(asset.duration)
        return if (durationSeconds.isFinite() && !durationSeconds.isNaN()) {
            durationSeconds
        } else {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun getVideoFileSize(url: NSURL): Long? {
        val path = url.path ?: return null
        return memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            errorPtr.value = null
            val attributes =
                NSFileManager.Companion.defaultManager.attributesOfItemAtPath(path, errorPtr.ptr)
            if (errorPtr.value != null) {
                null
            } else {
                val sizeValue = attributes?.get(NSFileSize) as? NSNumber
                sizeValue?.longLongValue
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    @Suppress("UnusedPrivateMember")
    private fun copyVideoToTemporaryLocation(url: NSURL): String? {
        val tempDir = NSTemporaryDirectory()
        val sanitizedDirectory =
            if (tempDir.endsWith("/")) {
                tempDir
            } else {
                "$tempDir/"
            }
        val extension =
            url.pathExtension?.takeIf { it.isNotBlank() } ?: DEFAULT_VIDEO_EXTENSION
        val destinationPath = "$sanitizedDirectory${buildFileName(extension)}"
        val destinationUrl = NSURL.Companion.fileURLWithPath(destinationPath)

        return memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            errorPtr.value = null
            if (NSFileManager.Companion.defaultManager.fileExistsAtPath(destinationPath)) {
                NSFileManager.Companion.defaultManager.removeItemAtPath(
                    destinationPath,
                    errorPtr.ptr,
                )
                errorPtr.value = null
            }

            val copyErrorPtr = alloc<ObjCObjectVar<NSError?>>()
            copyErrorPtr.value = null
            val success =
                NSFileManager.Companion.defaultManager.copyItemAtURL(
                    url,
                    destinationUrl,
                    copyErrorPtr.ptr,
                )
            if (success) {
                destinationPath
            } else {
                null
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun buildFileName(extension: String): String {
        val currentTimeInMs = Clock.System.now().toEpochMilliseconds()
        return "selected_video_$currentTimeInMs.$extension"
    }

    private sealed interface ProcessingResult {
        data class Success(
            val filePath: String,
        ) : ProcessingResult
        data class ValidationFailure(
            val error: VideoValidationError,
        ) : ProcessingResult
        data object Failure : ProcessingResult
    }
}

@Suppress("ReturnCount")
private fun findRootViewController(): UIViewController? {
    val application = UIApplication.Companion.sharedApplication

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
