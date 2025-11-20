package com.yral.shared.features.feed.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.SignupView
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.feed.generated.resources.Res
import yral_mobile.shared.features.feed.generated.resources.scroll_to_next_video

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupNudge(
    tncLink: String,
    onSignupClicked: (SocialProvider) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
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
                text = stringResource(Res.string.scroll_to_next_video),
                style = LocalAppTopography.current.mdBold,
                color = YralColors.NeutralIconsActive,
            )
            YralLottieAnimation(
                modifier = Modifier.size(36.dp),
                LottieRes.SIGNUP_SCROLL,
            )
        }
    }
    if (link.isNotEmpty()) {
        YralWebViewBottomSheet(
            link = link,
            bottomSheetState = sheetState,
            onDismissRequest = { link = "" },
        )
    }
}
