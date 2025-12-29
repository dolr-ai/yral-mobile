package com.yral.shared.features.chat.ui.conversation

import androidx.compose.runtime.Composable
import com.yral.shared.features.chat.attachments.FilePathChatAttachment

/**
 * Returns a function that launches the image picker from gallery.
 * When an image is selected, it's persisted to cache and returned as [FilePathChatAttachment].
 */
@Composable
expect fun rememberChatImagePicker(onImagePicked: (FilePathChatAttachment) -> Unit): () -> Unit

/**
 * Returns a function that launches the camera to capture an image.
 * When an image is captured, it's persisted to cache and returned as [FilePathChatAttachment].
 */
@Composable
expect fun rememberChatImageCapture(onImagePicked: (FilePathChatAttachment) -> Unit): () -> Unit
