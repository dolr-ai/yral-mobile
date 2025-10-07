package com.yral.shared.features.uploadvideo.nav.fileUpload

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel
import org.koin.core.component.KoinComponent

internal class DefaultUploadVideoComponent(
    componentContext: ComponentContext,
    private val goToHome: () -> Unit,
    private val onBack: () -> Unit,
) : UploadVideoComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun processEvent(value: UploadVideoViewModel.Event) {
        when (value) {
            UploadVideoViewModel.Event.GoToHome -> goToHome()
            else -> {}
        }
    }

    override fun onBack() {
        onBack.invoke()
    }
}
