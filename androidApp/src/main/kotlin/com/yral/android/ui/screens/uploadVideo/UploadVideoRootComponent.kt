package com.yral.android.ui.screens.uploadVideo

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenComponent
import com.yral.android.ui.screens.uploadVideo.fileUpload.UploadVideoComponent
import com.yral.android.ui.screens.uploadVideo.flowSelection.FlowSelectionComponent

abstract class UploadVideoRootComponent {
    abstract val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class FlowSelection(
            val component: FlowSelectionComponent,
        ) : Child()
        class AiVideoGen(
            val component: AiVideoGenComponent,
        ) : Child()
        class ClassicUpload(
            val component: UploadVideoComponent,
        ) : Child()
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
            openAlertsRequestBottomSheet: () -> Unit,
        ): UploadVideoRootComponent =
            DefaultUploadVideoRootComponent(
                componentContext = componentContext,
                goToHome = goToHome,
                openAlertsRequestBottomSheet = openAlertsRequestBottomSheet,
            )
    }
}
