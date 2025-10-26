package com.yral.shared.features.profile.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberProfileImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit

@Composable
expect fun rememberProfilePhotoCapture(onImagePicked: (ByteArray) -> Unit): () -> Unit
