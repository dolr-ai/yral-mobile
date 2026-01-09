package com.yral.shared.app.ui.screens.alertsrequest

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.libs.designsystem.component.YralButtonType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.a_little_nudge
import yral_mobile.shared.app.generated.resources.enable_follow_back_nudge_msg
import yral_mobile.shared.app.generated.resources.enable_notification_message
import yral_mobile.shared.app.generated.resources.enable_video_nudge_msg
import yral_mobile.shared.app.generated.resources.follow_back_nudge
import yral_mobile.shared.app.generated.resources.nudge_default
import yral_mobile.shared.app.generated.resources.nudge_follow
import yral_mobile.shared.app.generated.resources.nudge_tournament
import yral_mobile.shared.app.generated.resources.nudge_video
import yral_mobile.shared.app.generated.resources.tournament_nudge_subtitle
import yral_mobile.shared.app.generated.resources.tournament_nudge_title
import yral_mobile.shared.app.generated.resources.turn_on_alerts
import yral_mobile.shared.app.generated.resources.turn_on_notifications
import yral_mobile.shared.app.generated.resources.video_nudge

data class AlertsRequestInfo(
    val icon: Painter,
    val title: String,
    val subTitle: String,
    val isNotNowButtonVisible: Boolean = false,
    val buttonText: String,
    val buttonType: YralButtonType = YralButtonType.Pink,
)

@Composable
fun getAlertRequestInfo(type: AlertsRequestType): AlertsRequestInfo =
    when (type) {
        AlertsRequestType.FOLLOW_BACK ->
            AlertsRequestInfo(
                icon = painterResource(Res.drawable.nudge_follow),
                title = stringResource(Res.string.follow_back_nudge),
                subTitle = stringResource(Res.string.enable_follow_back_nudge_msg),
                buttonText = stringResource(Res.string.turn_on_alerts),
            )
        AlertsRequestType.VIDEO ->
            AlertsRequestInfo(
                icon = painterResource(Res.drawable.nudge_video),
                title = stringResource(Res.string.video_nudge),
                subTitle = stringResource(Res.string.enable_video_nudge_msg),
                buttonText = stringResource(Res.string.turn_on_alerts),
            )
        AlertsRequestType.DEFAULT ->
            AlertsRequestInfo(
                icon = painterResource(Res.drawable.nudge_default),
                title = stringResource(Res.string.a_little_nudge),
                subTitle = stringResource(Res.string.enable_notification_message),
                buttonText = stringResource(Res.string.turn_on_alerts),
            )
        AlertsRequestType.TOURNAMENT ->
            AlertsRequestInfo(
                icon = painterResource(Res.drawable.nudge_tournament),
                title = stringResource(Res.string.tournament_nudge_title),
                subTitle = stringResource(Res.string.tournament_nudge_subtitle),
                buttonText = stringResource(Res.string.turn_on_notifications),
                buttonType = YralButtonType.White,
            )
    }
