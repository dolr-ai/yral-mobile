package com.yral.android.ui.screens.home.account

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteAccountSheet(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    logout: () -> Unit,
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
                    text = stringResource(R.string.delete_your_account),
                    style = LocalAppTopography.current.lgBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.delete_account_disclaimer),
                    style = LocalAppTopography.current.mdMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.delete_account_question),
                    style = LocalAppTopography.current.mdMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
            }
            DeleteSheetButtons(
                onDismissRequest = onDismissRequest,
                logout = logout,
            )
        }
    }
}

@Composable
private fun DeleteSheetButtons(
    onDismissRequest: () -> Unit,
    logout: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
    ) {
        YralButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.no_take_me_back),
            borderColor = YralColors.Neutral700,
            borderWidth = 1.dp,
            backgroundColor = YralColors.Neutral800,
            textStyle =
                TextStyle(
                    color = YralColors.NeutralTextPrimary,
                ),
        ) { onDismissRequest() }
        YralButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.yes_delete),
            borderWidth = 1.dp,
            borderColor = YralColors.Red300,
            backgroundColor = YralColors.Red300,
            textStyle =
                TextStyle(
                    color = YralColors.NeutralTextPrimary,
                ),
        ) { logout() }
    }
}
