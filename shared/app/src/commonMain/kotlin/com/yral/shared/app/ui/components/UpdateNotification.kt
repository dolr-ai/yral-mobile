package com.yral.shared.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.app.UpdateState
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.libs.designsystem.component.toast.ToastCTA
import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showInfo
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.flexible_downloaded_cta
import yral_mobile.shared.app.generated.resources.flexible_downloaded_message

@Composable
internal fun UpdateNotificationHost(rootComponent: RootComponent) {
    val updateState by rootComponent.updateState.subscribeAsState()
    val message = stringResource(Res.string.flexible_downloaded_message)
    val ctaText = stringResource(Res.string.flexible_downloaded_cta)
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
