package com.yral.android.ui.screens.profile.nav

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.koin.core.component.KoinComponent

internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val onUploadVideoClicked: () -> Unit,
) : ProfileComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val _pendingVideoNavigation = Channel<String?>(Channel.CONFLATED)
    override val pendingVideoNavigation: Flow<String?> = _pendingVideoNavigation.consumeAsFlow()

    override fun onUploadVideoClick() {
        onUploadVideoClicked.invoke()
    }

    override fun handleNavigation(destination: String) {
        Logger.d("DefaultProfileComponent") { "handleNavigation: $destination" }
        when {
            destination.startsWith(DEEPLINK_VIDEO_PREFIX) -> {
                val videoId = destination.substringAfterLast("/videos/")
                val channelResult = _pendingVideoNavigation.trySend(videoId)
                Logger.d("DefaultProfileComponent") { "handleNavigation: channelResult: $channelResult" }
            }
        }
    }
}
