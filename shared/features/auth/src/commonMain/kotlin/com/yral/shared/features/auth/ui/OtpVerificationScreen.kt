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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.core.utils.formatRemainingDuration
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.nav.otpverification.OtpVerificationComponent
import com.yral.shared.features.auth.ui.components.OtpInput
import com.yral.shared.features.auth.ui.components.OtpInputConfig
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.features.auth.viewModel.OtpErrorContent
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.didnt_receive_otp
import yral_mobile.shared.features.auth.generated.resources.invalid_otp
import yral_mobile.shared.features.auth.generated.resources.otp_sent_to
import yral_mobile.shared.features.auth.generated.resources.otp_verification
import yral_mobile.shared.features.auth.generated.resources.resend_otp
import yral_mobile.shared.features.auth.generated.resources.resend_otp_in
import yral_mobile.shared.features.auth.generated.resources.verify
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background
import kotlin.time.Duration.Companion.seconds
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@Composable
fun OtpVerificationScreen(
    component: OtpVerificationComponent,
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier,
    authTelemetry: AuthTelemetry = koinInject(),
) {
    // Collect state from ViewModel
    val loginState by loginViewModel.state.collectAsState()
    val phoneAuthData = (loginState.phoneAuthState as? UiState.Success)?.data
    val otpAuthState = loginState.otpAuthState
    val resendTimerSeconds = loginState.resendTimerSeconds

    LaunchedEffect(phoneAuthData?.phoneNumber) {
        val phoneNumber = phoneAuthData?.phoneNumber
        if (!phoneNumber.isNullOrBlank()) {
            authTelemetry.otpScreenViewed(phoneNumber)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.Neutral950)
                .padding(bottom = 16.dp),
    ) {
        // Header with back button
        Header(
            onBack = {
                authTelemetry.otpDismissed()
                component.onBack()
            },
        )

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
                errorContent = loginViewModel.getOtpErrorContent(),
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
                        loginState.otpValidationError != null -> YralButtonState.Disabled
                        else -> YralButtonState.Enabled
                    },
            )
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
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
                    .clickable { onBack() },
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
    errorContent: OtpErrorContent?,
    onResendClick: () -> Unit,
) {
    val isTimerRunning = timerSeconds != null && timerSeconds > 0

    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        when {
            // Error takes priority over timer
            errorContent != null -> {
                ErrorMessage(errorContent, onResendClick)
            }
            // Scenario 2: Timer is running - "Resend in 00:12"
            isTimerRunning -> {
                Text(
                    text =
                        stringResource(
                            Res.string.resend_otp_in,
                            formatRemainingDuration(timerSeconds.seconds),
                        ),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.Neutral600,
                    textAlign = TextAlign.Center,
                )
            }
            // Scenario 1: First time - "Didn't receive the OTP?  Resend"
            else -> {
                Text(
                    text = stringResource(Res.string.didnt_receive_otp).plus("  "),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextPrimary,
                    textAlign = TextAlign.Center,
                )
                YralMaskedVectorTextV2(
                    text = stringResource(Res.string.resend_otp),
                    drawableRes = DesignRes.drawable.pink_gradient_background,
                    textStyle =
                        LocalAppTopography
                            .current
                            .baseRegular
                            .plus(TextStyle(textAlign = TextAlign.Center)),
                    modifier = Modifier.clickable { onResendClick() },
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    errorContent: OtpErrorContent,
    onResendClick: () -> Unit,
) {
    when (errorContent) {
        is OtpErrorContent.InvalidOtpWithResend -> {
            // Scenario: Invalid OTP with resend - "Invalid OTP!  Resend"
            Text(
                text = stringResource(Res.string.invalid_otp).plus("  "),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
            YralMaskedVectorTextV2(
                text = stringResource(Res.string.resend_otp),
                drawableRes = DesignRes.drawable.pink_gradient_background,
                textStyle =
                    LocalAppTopography
                        .current
                        .baseRegular
                        .plus(TextStyle(textAlign = TextAlign.Center)),
                modifier = Modifier.clickable { onResendClick() },
            )
        }

        is OtpErrorContent.SimpleMessage -> {
            // Scenario: Simple error message - show message as is
            Text(
                text = errorContent.message,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
