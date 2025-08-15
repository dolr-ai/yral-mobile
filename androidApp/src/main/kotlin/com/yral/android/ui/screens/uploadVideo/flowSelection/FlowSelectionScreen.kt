package com.yral.android.ui.screens.uploadVideo.flowSelection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralButton

@Composable
fun FlowSelectionScreen(component: FlowSelectionComponent) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Choose upload flow",
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
        YralButton(text = "File Upload", onClick = component::onUploadVideoClicked)
        YralButton(text = "AI Video Gen", onClick = component::onAiVideoGenClicked)
    }
}
