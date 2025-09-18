package com.yral.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationSheet(
    bottomSheetState: SheetState,
    title: String,
    subTitle: String = "",
    confirmationMessage: String,
    cancelButton: String,
    deleteButton: String,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
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
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = LocalAppTopography.current.lgBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                if (subTitle.isNotEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = subTitle,
                        style = LocalAppTopography.current.mdMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = confirmationMessage,
                    style = LocalAppTopography.current.mdMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
            }
            DeleteSheetButtons(
                cancelButton = cancelButton,
                deleteButton = deleteButton,
                onDismissRequest = onDismissRequest,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun DeleteSheetButtons(
    cancelButton: String,
    deleteButton: String,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
    ) {
        YralButton(
            modifier = Modifier.weight(1f),
            text = cancelButton,
            borderColor = YralColors.Neutral700,
            borderWidth = 1.dp,
            backgroundColor = YralColors.Neutral800,
            textStyle =
                TextStyle(
                    color = YralColors.NeutralTextPrimary,
                ),
            onClick = onDismissRequest,
        )
        YralButton(
            modifier = Modifier.weight(1f),
            text = deleteButton,
            borderWidth = 1.dp,
            borderColor = YralColors.Red300,
            backgroundColor = YralColors.Red300,
            textStyle =
                TextStyle(
                    color = YralColors.NeutralTextPrimary,
                ),
            onClick = onDelete,
        )
    }
}
