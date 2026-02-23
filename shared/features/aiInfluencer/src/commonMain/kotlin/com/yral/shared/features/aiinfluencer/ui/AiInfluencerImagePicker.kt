package com.yral.shared.features.aiinfluencer.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberAiInfluencerImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit

@Composable
expect fun rememberAiInfluencerPhotoCapture(onImagePicked: (ByteArray) -> Unit): () -> Unit
