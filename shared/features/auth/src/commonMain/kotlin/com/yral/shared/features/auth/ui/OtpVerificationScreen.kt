package com.yral.shared.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.auth.nav.otpverification.OtpVerificationComponent
import com.yral.shared.features.auth.ui.components.OtpInput
import com.yral.shared.features.auth.ui.components.OtpInputConfig
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.otp_sent_to
import yral_mobile.shared.features.auth.generated.resources.otp_verification
import yral_mobile.shared.features.auth.generated.resources.resend_otp
import yral_mobile.shared.features.auth.generated.resources.resend_otp_in
import yral_mobile.shared.features.auth.generated.resources.verify
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun OtpVerificationScreen(
    component: OtpVerificationComponent,
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    // Collect state from ViewModel
    val loginState by loginViewModel.state.collectAsState()
    val phoneAuthData = (loginState.phoneAuthState as? UiState.Success)?.data
    val otpAuthState = loginState.otpAuthState
    val resendTimerSeconds = loginState.resendTimerSeconds

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.Neutral950)
                .padding(bottom = 16.dp),
    ) {
        // Header with back button
        Header(component)

        // Content
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Phone number text
            Text(
                text = stringResource(Res.string.otp_sent_to, phoneAuthData?.phoneNumber ?: ""),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral200,
                textAlign = TextAlign.Center,
            )

            OtpInput(
                value = loginState.otpCode,
                onValueChange = { loginViewModel.onOtpCodeChanged(it) },
                onOtpComplete = { loginViewModel.onVerifyOtpClicked() },
                config = OtpInputConfig(length = 6),
            )

            // Resend text / timer
            ResendOtpText(
                timerSeconds = resendTimerSeconds,
                hasError = loginState.otpValidationError != null,
                onResendClick = loginViewModel::onResendOtp,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Verify Button
            YralGradientButton(
                text = stringResource(Res.string.verify),
                onClick = { loginViewModel.onVerifyOtpClicked() },
                modifier = Modifier.fillMaxWidth(),
                buttonState =
                    when {
                        otpAuthState is UiState.InProgress -> YralButtonState.Loading
                        loginState.otpCode.length < 6 -> YralButtonState.Disabled
                        else -> YralButtonState.Enabled
                    },
            )
        }
    }
}

@Composable
private fun Header(component: OtpVerificationComponent) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back",
            tint = Color.White,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable { component.onBack() },
        )
        Text(
            text = stringResource(Res.string.otp_verification),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).offset(x = (-12).dp),
        )
    }
}

@Composable
private fun ResendOtpText(
    timerSeconds: Int?,
    hasError: Boolean,
    onResendClick: () -> Unit,
) {
    val text =
        when {
            timerSeconds != null && timerSeconds > 0 -> stringResource(Res.string.resend_otp_in, timerSeconds)
            hasError -> stringResource(Res.string.resend_otp)
            else -> stringResource(Res.string.resend_otp)
        }
    val isClickable = timerSeconds == null || timerSeconds == 0
    Text(
        text = text,
        style = LocalAppTopography.current.baseRegular,
        color =
            if (isClickable) {
                YralColors.Pink300
            } else {
                YralColors.Neutral600
            },
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .clickable(enabled = isClickable) { onResendClick() }
                .padding(vertical = 8.dp),
    )
}
