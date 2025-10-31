package com.yral.shared.app.ui.screens.alertsrequest

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.PushNotificationsEnabledEventData
import com.yral.shared.analytics.events.PushNotificationsPopupEventData
import com.yral.shared.app.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.not_now
import yral_mobile.shared.app.generated.resources.turn_on_alerts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlertsRequestBottomSheet(
    component: AlertsRequestComponent,
    analyticsManager: AnalyticsManager = koinInject(),
) {
    val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val controller: PermissionsController =
        remember(factory) { factory.createPermissionsController() }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    BindEffect(permissionsController = controller)

    HandleNotificationPermission(
        controller = controller,
        coroutineScope = coroutineScope,
        onPermissionStatusChanged = component::onPermissionChanged,
    )

    val showSheet by component.showSheet.collectAsState()

    if (showSheet) {
        LaunchedEffect(Unit) { analyticsManager.trackEvent(PushNotificationsPopupEventData()) }
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AlertSheet(
            bottomSheetState = bottomSheetState,
            onDismissRequest = component::onDismissClicked,
            type = component.type,
            onTurnOnAlertsClicked = {
                coroutineScope.launch {
                    controller.requestNotificationPermission(onPermissionGranted = {
                        analyticsManager.trackEvent(PushNotificationsEnabledEventData())
                        component.onPermissionChanged(true)
                    })
                }
            },
            onNotNowClicked = component::onDismissClicked,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AlertSheet(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    type: AlertsRequestType,
    onTurnOnAlertsClicked: () -> Unit,
    onNotNowClicked: () -> Unit,
) {
    val info = getAlterRequestInfo(type)
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = info.icon,
                contentDescription = null,
                modifier = Modifier.size(134.dp),
            )
            Spacer(Modifier.height(30.dp))
            Text(
                text = info.title,
                style = LocalAppTopography.current.xlSemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = info.subTitle,
                style = LocalAppTopography.current.baseRegular,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            YralGradientButton(
                text = stringResource(Res.string.turn_on_alerts),
                onClick = onTurnOnAlertsClicked,
            )
            if (info.isNotNowButtonVisible) {
                Spacer(Modifier.height(12.dp))
                YralButton(
                    text = stringResource(Res.string.not_now),
                    borderColor = YralColors.Neutral700,
                    borderWidth = 1.dp,
                    backgroundColor = YralColors.Neutral800,
                    textStyle = TextStyle(color = YralColors.NeutralTextPrimary),
                    onClick = onNotNowClicked,
                )
            }
        }
    }
}

@Composable
private fun HandleNotificationPermission(
    controller: PermissionsController,
    coroutineScope: CoroutineScope,
    onPermissionStatusChanged: (isGranted: Boolean) -> Unit,
) {
    LifecycleStartEffect(Unit) {
        val job =
            coroutineScope.launch {
                val permissionGranted = controller.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                onPermissionStatusChanged(permissionGranted)
            }
        onStopOrDispose {
            job.cancel()
        }
    }
}

private suspend fun PermissionsController.requestNotificationPermission(onPermissionGranted: () -> Unit) {
    val initialPermissionState = getPermissionState(Permission.REMOTE_NOTIFICATION)
    try {
        providePermission(Permission.REMOTE_NOTIFICATION)
        onPermissionGranted()
    } catch (_: DeniedAlwaysException) {
        // Don't open settings when transitioning from denied to always denied
        if (initialPermissionState != PermissionState.Denied) {
            openAppSettings()
        }
    } catch (_: DeniedException) {
        // Permission was denied.
    }
}
