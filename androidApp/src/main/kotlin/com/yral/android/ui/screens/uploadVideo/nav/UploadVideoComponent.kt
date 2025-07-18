package com.yral.android.ui.screens.uploadVideo.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel

abstract class UploadVideoComponent {
    abstract fun processEvent(value: UploadVideoViewModel.Event)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
        ): UploadVideoComponent = DefaultUploadVideoComponent(componentContext, goToHome)
    }
}
