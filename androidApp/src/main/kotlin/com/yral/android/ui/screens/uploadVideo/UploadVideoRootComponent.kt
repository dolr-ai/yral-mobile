package com.yral.android.ui.screens.uploadVideo

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeChildSnapshotProvider
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenComponent
import com.yral.android.ui.screens.uploadVideo.fileUpload.UploadVideoComponent
import com.yral.android.ui.screens.uploadVideo.flowSelection.FlowSelectionComponent
import kotlinx.serialization.Serializable

abstract class UploadVideoRootComponent : HomeChildSnapshotProvider {
    abstract val stack: Value<ChildStack<*, Child>>

    // Handle back presses within the upload flow. Returns true if consumed.
    abstract fun onBackClicked(): Boolean

    sealed class Child {
        class FlowSelection(
            val component: FlowSelectionComponent,
        ) : Child()
        class AiVideoGen(
            val component: AiVideoGenComponent,
        ) : Child()
        class FileUpload(
            val component: UploadVideoComponent,
        ) : Child()
    }

    @Serializable
    data class Snapshot(
        val routes: List<Route>,
    ) {
        @Serializable
        enum class Route { FlowSelection, AiVideoGen, FileUpload }
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
            snapshot: Snapshot?,
        ): UploadVideoRootComponent =
            DefaultUploadVideoRootComponent(
                componentContext = componentContext,
                goToHome = goToHome,
                snapshot = snapshot,
            )
    }
}
