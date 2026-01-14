package com.yral.shared.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.features.auth.nav.otpverification.OtpVerificationComponent
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

            // OTP Input Boxes
            OtpInputField(
                value = loginState.otpCode,
                onValueChange = { loginViewModel.onOtpCodeChanged(it) },
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
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        IconButton(
            onClick = component::onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "Back",
                tint = YralColors.NeutralIconsActive,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = stringResource(Res.string.otp_verification),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralIconsActive,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
@Composable
private fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    // Maintain OTP as a fixed 6-position array (null = empty position, no shifting)
    var otpArray by remember(value) {
        val array = Array<Char?>(6) { null }
        // Map existing value to array positions sequentially
        value.forEachIndexed { index, char ->
            if (index < 6) {
                array[index] = char
            }
        }
        mutableStateOf(array)
    }

    // Track which box is focused
    var focusedBoxIndex by remember { mutableStateOf(0) }

    // Sync array changes back to parent (filter out nulls to get actual OTP string)
    LaunchedEffect(otpArray) {
        val otpString = otpArray.filterNotNull().joinToString("")
        if (otpString != value) {
            onValueChange(otpString)
        }
    }

    // Sync value changes from parent to array
    LaunchedEffect(value) {
        val newArray = Array<Char?>(6) { null }
        value.forEachIndexed { index, char ->
            if (index < 6) {
                newArray[index] = char
            }
        }
        otpArray = newArray
    }

    // Auto-focus when composable is first created
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(6) { index ->
            val char = otpArray[index]?.toString() ?: ""
            val isFocused = focusedBoxIndex == index

            Box(
                modifier =
                    Modifier
                        .width(43.dp)
                        .height(53.dp)
                        .clickable {
                            // Set focused box index
                            focusedBoxIndex = index
                            focusRequester.requestFocus()
                        }.background(
                            color = YralColors.Neutral900,
                            shape = RoundedCornerShape(8.dp),
                        ).border(
                            width = 1.dp,
                            color =
                                if (isFocused) {
                                    YralColors.Pink300
                                } else {
                                    YralColors.Neutral700
                                },
                            shape = RoundedCornerShape(8.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = char,
                    style = LocalAppTopography.current.xlBold,
                    color = YralColors.Neutral0,
                )
            }
        }
    }

    // Text field for actual input (positioned off-screen but still functional)
    // We maintain a string representation for the text field, but map it to the array
    val textFieldString = otpArray.filterNotNull().joinToString("")
    var textFieldValue by remember(textFieldString) {
        mutableStateOf(TextFieldValue(textFieldString, TextRange(textFieldString.length)))
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newTextFieldValue ->
            val oldText = textFieldValue.text
            val newText = newTextFieldValue.text

            // Handle deletion (backspace) - detect when text is shorter
            if (newText.length < oldText.length) {
                // User deleted a character - clear the digit at focused box position (no shifting)
                if (focusedBoxIndex < 6) {
                    otpArray =
                        otpArray.copyOf().apply {
                            this[focusedBoxIndex] = null
                        }
                }

                // Update text field to reflect the change
                val updatedText = otpArray.filterNotNull().joinToString("")
                textFieldValue = TextFieldValue(updatedText, TextRange(updatedText.length))
            } else {
                // Handle insertion - filter to only allow digits
                val digitsOnly = newText.filter { it.isDigit() }

                if (digitsOnly.isNotEmpty() && focusedBoxIndex < 6) {
                    // Get the new digit (last character typed)
                    val newDigit = digitsOnly.last()

                    // Update the array at focused box position
                    otpArray =
                        otpArray.copyOf().apply {
                            this[focusedBoxIndex] = newDigit
                        }

                    // Move focus to next empty box, or next box if all filled
                    focusedBoxIndex = (focusedBoxIndex + 1).coerceAtMost(5)

                    // Update text field
                    val updatedText = otpArray.filterNotNull().joinToString("")
                    textFieldValue = TextFieldValue(updatedText, TextRange(updatedText.length))
                } else {
                    // Just update text field value
                    textFieldValue = newTextFieldValue
                }
            }
        },
        modifier =
            Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(fontSize = 0.sp), // Hide text visually
    )
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
