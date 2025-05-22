package com.yral.android.ui.screens.home.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralGradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignUpFailedBottomSheet(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.could_not_login),
                    style = LocalAppTopography.current.xlSemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.could_not_login_desc),
                    style = LocalAppTopography.current.regRegular,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
            }
            YralGradientButton(
                text = stringResource(R.string.ok),
                onClick = onDismissRequest,
            )
        }
    }
}
