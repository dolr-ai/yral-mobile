package com.yral.android.ui.screens.uploadVideo.flowSelection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.uploadVideo.flowSelection.FlowSelectionScreenConstants.DELAY_TO_SHOW_CLICK
import com.yral.android.ui.widgets.YralButton
import com.yral.shared.features.uploadvideo.presentation.FlowSelectionViewModel
import com.yral.shared.features.uploadvideo.presentation.FlowType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FlowSelectionScreen(
    component: FlowSelectionComponent,
    modifier: Modifier = Modifier,
    viewModel: FlowSelectionViewModel = koinViewModel(),
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    Column(modifier.fillMaxSize()) {
        Header()
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowItem(
                icon = R.drawable.ic_magicpen,
                title = stringResource(R.string.create_ai_video),
                subTitle = stringResource(R.string.generate_video_by_giving_a_prompt_to_ai),
                button = stringResource(R.string.create_with_ai),
                isSelected = viewState.flowType == FlowType.AI_VIDEO_GEN,
                onClick = {
                    coroutineScope.launch {
                        viewModel.setFlowType(FlowType.AI_VIDEO_GEN)
                        delay(DELAY_TO_SHOW_CLICK)
                        viewModel.setFlowType(FlowType.AI_VIDEO_GEN)
                        component.onAiVideoGenClicked()
                    }
                },
                onButtonClick = { component.onAiVideoGenClicked() },
            )
            FlowItem(
                icon = R.drawable.ic_upload,
                title = stringResource(R.string.upload_video),
                subTitle = stringResource(R.string.add_video_from_device),
                button = stringResource(R.string.upload_video),
                isSelected = viewState.flowType == FlowType.UPLOAD_VIDEO,
                onClick = {
                    coroutineScope.launch {
                        viewModel.setFlowType(FlowType.UPLOAD_VIDEO)
                        delay(DELAY_TO_SHOW_CLICK)
                        viewModel.setFlowType(FlowType.UPLOAD_VIDEO)
                        component.onUploadVideoClicked()
                    }
                },
                onButtonClick = { component.onUploadVideoClicked() },
            )
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.add_video),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun FlowItem(
    icon: Int,
    title: String,
    subTitle: String,
    button: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
) {
    val shape = RoundedCornerShape(size = 8.dp)
    val borderColor = if (isSelected) YralColors.Pink300 else YralColors.Neutral700
    val background = if (isSelected) YralColors.ShadowPink else YralColors.Neutral900
    val buttonBackground = if (isSelected) YralColors.Pink400 else YralColors.Neutral900
    val buttonTextColor = if (isSelected) YralColors.Neutral50 else YralColors.Pink300
    val buttonBorder = if (isSelected) Color.Transparent else YralColors.Pink300
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = borderColor, shape = shape)
                .background(color = background, shape = shape)
                .clickable { onClick() }
                .padding(horizontal = 41.dp, vertical = 31.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = "",
                    contentScale = ContentScale.None,
                    modifier = Modifier.size(24.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = LocalAppTopography.current.mdBold,
                        textAlign = TextAlign.Center,
                        color = YralColors.NeutralTextPrimary,
                    )
                    Text(
                        text = subTitle,
                        style = LocalAppTopography.current.regMedium,
                        textAlign = TextAlign.Center,
                        color = YralColors.NeutralTextSecondary,
                    )
                }
            }
            YralButton(
                text = button,
                borderWidth = 1.dp,
                borderColor = buttonBorder,
                backgroundColor = buttonBackground,
                textStyle = TextStyle(color = buttonTextColor, fontWeight = FontWeight.SemiBold),
                onClick = onButtonClick,
                modifier = Modifier.wrapContentWidth(),
                paddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

private object FlowSelectionScreenConstants {
    const val DELAY_TO_SHOW_CLICK = 100L
}
