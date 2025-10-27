package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.ic_error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YralErrorMessage(
    title: String,
    error: String,
    showDragHandle: Boolean = false,
    showErrorIcon: Boolean = false,
    sheetState: SheetState,
    cta: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onDismiss,
        bottomSheetState = sheetState,
        dragHandle = { if (showDragHandle) DragHandle(color = YralColors.Neutral500) else null },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 24.dp,
                        end = 16.dp,
                        bottom = 36.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = LocalAppTopography.current.xlSemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                if (showErrorIcon) {
                    Image(
                        painter = painterResource(Res.drawable.ic_error),
                        contentDescription = "error",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(vertical = 28.dp).size(120.dp),
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = error,
                    style = LocalAppTopography.current.regRegular,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
            }
            YralGradientButton(
                text = cta,
                onClick = onClick,
            )
        }
    }
}
