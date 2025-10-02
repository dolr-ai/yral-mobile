package com.yral.shared.features.uploadvideo.nav.fileUpload

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel

abstract class UploadVideoComponent {
    abstract fun processEvent(value: UploadVideoViewModel.Event)
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
            onBack: () -> Unit,
        ): UploadVideoComponent = DefaultUploadVideoComponent(componentContext, goToHome, onBack)
    }
}
