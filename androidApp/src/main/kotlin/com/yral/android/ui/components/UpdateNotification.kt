package com.yral.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.android.R
import com.yral.android.ui.nav.RootComponent
import com.yral.android.update.UpdateState

@Composable
fun UpdateNotificationHost(rootComponent: RootComponent) {
    val updateState by rootComponent.updateState.subscribeAsState()
    val message = stringResource(id = R.string.flexible_downloaded_message)
    val ctaText = stringResource(id = R.string.flexible_downloaded_cta)
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.FlexibleDownloaded -> {
                ToastManager.showInfo(
                    ToastType.Small(
                        message = message,
                    ),
                    ToastCTA(
                        text = ctaText,
                        onClick = rootComponent::onCompleteUpdateClicked,
                    ),
                    ToastDuration.INDEFINITE,
                )
            }

            else -> { // Do nothing, will be invisible
            }
        }
    }
}
