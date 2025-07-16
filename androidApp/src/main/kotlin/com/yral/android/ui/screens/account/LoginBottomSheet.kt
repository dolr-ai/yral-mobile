package com.yral.android.ui.screens.account

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.android.ui.components.signup.SignupView
import com.yral.android.ui.screens.account.LoginBottomSheetConstants.BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN
import com.yral.android.ui.widgets.YralBottomSheet

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginBottomSheet(
    termsLink: String,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSignupClicked: () -> Unit,
    openTerms: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
    ) {
        BoxWithConstraints {
            val maxHeight = maxHeight
            val adaptiveHeight = (maxHeight * BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN)
            Column(
                modifier =
                    Modifier
                        .padding(
                            start = 16.dp,
                            top = 45.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SignupView(
                    onSignupClicked = onSignupClicked,
                    termsLink = termsLink,
                    openTerms = openTerms,
                )
                Spacer(modifier = Modifier.height(adaptiveHeight))
            }
        }
    }
}

private object LoginBottomSheetConstants {
    const val BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN = 0.3f
}
