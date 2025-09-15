package com.yral.android.ui.screens.feed.uiComponets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.components.signup.SignupView
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.account.WebViewBottomSheet
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.shared.analytics.events.SignupPageName

@Composable
fun SignupNudge(
    tncLink: String,
    onSignupClicked: () -> Unit,
) {
    var link by remember { mutableStateOf("") }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(YralColors.ScrimColor)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clickable { },
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(top = 58.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            SignupView(
                pageName = SignupPageName.HOME,
                termsLink = tncLink,
                openTerms = { link = tncLink },
                onSignupClicked = onSignupClicked,
            )
        }
        Column(
            Modifier.weight(1f).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = stringResource(R.string.scroll_to_next_video),
                style = LocalAppTopography.current.mdBold,
                color = YralColors.NeutralIconsActive,
            )
            YralLottieAnimation(
                modifier = Modifier.size(36.dp),
                R.raw.signup_scroll,
            )
        }
    }
    if (link.isNotEmpty()) {
        LinkSheet(
            link = link,
            onDismissRequest = { link = "" },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkSheet(
    link: String,
    onDismissRequest: () -> Unit,
) {
    val extraSheetState = rememberModalBottomSheetState()
    LaunchedEffect(link) {
        if (link.isEmpty()) {
            extraSheetState.hide()
        } else {
            extraSheetState.show()
        }
    }
    WebViewBottomSheet(
        link = link,
        bottomSheetState = extraSheetState,
        onDismissRequest = onDismissRequest,
    )
}
