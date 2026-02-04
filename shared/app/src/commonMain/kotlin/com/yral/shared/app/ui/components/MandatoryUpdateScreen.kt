package com.yral.shared.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.app.openAppStoreForUpdate
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.GradientAngleConvention
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.angledGradientBackground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.in_app_update
import yral_mobile.shared.app.generated.resources.mandatory_update_cta
import yral_mobile.shared.app.generated.resources.mandatory_update_message
import yral_mobile.shared.app.generated.resources.mandatory_update_title
import yral_mobile.shared.app.generated.resources.mandatory_update_title_highlight

@Suppress("MagicNumber")
private val mandatoryUpdateGradientStops =
    arrayOf(
        0.5141f to Color(0x99171717),
        1f to Color(0x99158F5C),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryUpdateScreen() {
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden },
        )
    YralBottomSheet(
        onDismissRequest = { },
        bottomSheetState = bottomSheetState,
        dragHandle = null,
        shouldDismissOnBackPress = false,
    ) {
        MandatoryUpdateContent()
    }
}

@Composable
private fun MandatoryUpdateContent() {
    Box(modifier = Modifier.fillMaxWidth()) {
        MandatoryUpdateGradientBackground(Modifier.matchParentSize())
        MandatoryUpdateBody()
    }
}

@Composable
private fun MandatoryUpdateGradientBackground(modifier: Modifier) {
    Box(
        modifier =
            modifier
                .angledGradientBackground(
                    colorStops = mandatoryUpdateGradientStops,
                    degrees = 131f,
                    angleConvention = GradientAngleConvention.CssDegrees,
                ),
    )
}

@Suppress("MagicNumber")
@Composable
private fun MandatoryUpdateBody() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MandatoryUpdateImage()
        MandatoryUpdateTitleAndMessage()
        MandatoryUpdateButton()
    }
}

@Composable
private fun MandatoryUpdateImage() {
    Image(
        painter = painterResource(Res.drawable.in_app_update),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(120.dp),
    )
}

@Composable
private fun MandatoryUpdateTitleAndMessage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val textStyle = LocalAppTopography.current.xlBold
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
                color = YralColors.Neutral50,
            )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text =
                buildAnnotatedString {
                    withStyle(spanStyle) {
                        append(stringResource(Res.string.mandatory_update_title))
                    }
                    withStyle(spanStyle.copy(color = YralColors.Yellow200)) {
                        append(stringResource(Res.string.mandatory_update_title_highlight))
                    }
                },
            style = LocalAppTopography.current.xlSemiBold,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.mandatory_update_message),
            style = LocalAppTopography.current.baseRegular,
            textAlign = TextAlign.Center,
            color = YralColors.Neutral200,
        )
    }
}

@Composable
private fun MandatoryUpdateButton() {
    YralGradientButton(
        text = stringResource(Res.string.mandatory_update_cta),
        buttonType = YralButtonType.White,
        onClick = { openAppStoreForUpdate() },
    )
}
