package com.yral.shared.features.uploadvideo.ui.aiVideoGen

import androidx.compose.runtime.Composable

@Composable
expect fun rememberAiVideoImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit
