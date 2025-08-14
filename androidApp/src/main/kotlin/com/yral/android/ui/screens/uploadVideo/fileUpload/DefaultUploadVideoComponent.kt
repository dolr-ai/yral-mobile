package com.yral.android.ui.screens.uploadVideo.fileUpload

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel
import org.koin.core.component.KoinComponent

internal class DefaultUploadVideoComponent(
    componentContext: ComponentContext,
    private val goToHome: () -> Unit,
) : UploadVideoComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun processEvent(value: UploadVideoViewModel.Event) {
        when (value) {
            UploadVideoViewModel.Event.GoToHome -> goToHome()
            else -> {}
        }
    }
}
