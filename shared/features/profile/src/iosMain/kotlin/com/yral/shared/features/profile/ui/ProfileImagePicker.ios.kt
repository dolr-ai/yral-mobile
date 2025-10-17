package com.yral.shared.features.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Suppress("ForbiddenComment")
@Composable
actual fun rememberProfileImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit =
    remember {
        { /*Implement iOS image picker */ }
    }

@Composable
actual fun rememberProfilePhotoCapture(onImagePicked: (ByteArray) -> Unit): () -> Unit =
    remember {
        { /*Implement iOS camera capture */ }
    }
