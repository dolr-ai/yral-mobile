package com.yral.android.ui.screens.uploadVideo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenScreen
import com.yral.android.ui.screens.uploadVideo.fileUpload.UploadVideoScreen
import com.yral.android.ui.screens.uploadVideo.flowSelection.FlowSelectionScreen

@Composable
fun UploadVideoRootScreen(
    component: UploadVideoRootComponent,
    bottomPadding: Dp,
) {
    val stack by component.stack.subscribeAsState()
    val canPop = stack.items.size > 1
    BackHandler(enabled = canPop, onBack = { component.onBackClicked() })
    Children(
        stack = component.stack,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
    ) { child ->
        when (val instance = child.instance) {
            is UploadVideoRootComponent.Child.FlowSelection -> {
                FlowSelectionScreen(component = instance.component)
            }
            is UploadVideoRootComponent.Child.AiVideoGen -> {
                AiVideoGenScreen(component = instance.component)
            }
            is UploadVideoRootComponent.Child.FileUpload -> {
                UploadVideoScreen(component = instance.component, bottomPadding = bottomPadding)
            }
        }
    }
}
