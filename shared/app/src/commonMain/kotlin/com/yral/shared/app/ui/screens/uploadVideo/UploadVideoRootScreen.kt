package com.yral.shared.app.ui.screens.uploadVideo

import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.features.uploadvideo.nav.UploadVideoRootComponent
import com.yral.shared.features.uploadvideo.ui.FlowSelectionScreen
import com.yral.shared.features.uploadvideo.ui.aiVideoGen.AiVideoGenScreen
import com.yral.shared.features.uploadvideo.ui.fileUpload.UploadVideoScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UploadVideoRootScreen(
    component: UploadVideoRootComponent,
    bottomPadding: Dp,
) {
    Children(
        stack = component.stack,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            is UploadVideoRootComponent.Child.FlowSelection -> {
                FlowSelectionScreen(component = instance.component)
            }
            is UploadVideoRootComponent.Child.AiVideoGen -> {
                AiVideoGenScreen(
                    component = instance.component,
                    bottomPadding = bottomPadding,
                )
            }
            is UploadVideoRootComponent.Child.FileUpload -> {
                UploadVideoScreen(
                    component = instance.component,
                    bottomPadding = bottomPadding,
                )
            }
        }
    }
}
