package com.yral.android.ui.screens.profile.nav

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow

abstract class ProfileComponent {
    abstract val pendingVideoNavigation: Flow<String?>
    abstract fun onUploadVideoClick()
    abstract fun handleNavigation(destination: String)

    companion object Companion {
        const val DEEPLINK = "yralm://profile"
        const val DEEPLINK_VIDEO_PREFIX = "$DEEPLINK/videos"
        operator fun invoke(
            componentContext: ComponentContext,
            onUploadVideoClicked: () -> Unit,
        ): ProfileComponent = DefaultProfileComponent(componentContext, onUploadVideoClicked)
    }
}
