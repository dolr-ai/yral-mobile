package com.yral.shared.features.auth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.ui.LoginMode
import com.yral.shared.features.auth.ui.components.SignupViewConstants.DEFAULT_TOP_CONTENT_HEIGHT
import com.yral.shared.features.auth.ui.components.SignupViewConstants.DEFAULT_TOP_CONTENT_WIDTH
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.auth.utils.defaultSocialProviders
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralBrushes
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.continue_to_sign_up_for_free
import yral_mobile.shared.features.auth.generated.resources.join_yral
import yral_mobile.shared.features.auth.generated.resources.or_label
import yral_mobile.shared.features.auth.generated.resources.sign_up_disclaimer
import yral_mobile.shared.features.auth.generated.resources.signup_consent
import yral_mobile.shared.features.auth.generated.resources.signup_with_apple
import yral_mobile.shared.features.auth.generated.resources.signup_with_google
import yral_mobile.shared.features.auth.generated.resources.terms_of_service_signup_consent
import yral_mobile.shared.libs.designsystem.generated.resources.apple
import yral_mobile.shared.libs.designsystem.generated.resources.google
import yral_mobile.shared.libs.designsystem.generated.resources.login
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

/**
 * Signup view that supports social and phone authentication.
 *
 * @param pageName The page/source from which signup is initiated (for analytics)
 * @param openTerms Callback to open terms of service
 * @param headlineText Optional custom headline text
 * @param disclaimerText Optional disclaimer text; null = default, "" = hide
 * @param topIconContent Custom top icon/image content
 * @param mode The authentication methods to show (SOCIAL, PHONE, or BOTH)
 * @param onNavigateToCountrySelector Callback to navigate to country selector screen
 * @param authTelemetry Telemetry for tracking signup events (injected)
 * @param loginViewModel ViewModel managing auth state (injected)
 */

@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun SignupView(
    pageName: SignupPageName,
    openTerms: () -> Unit,
    headlineText: AnnotatedString? = null,
    disclaimerText: String? = null,
    topIconContent: (@Composable () -> Unit) = { DefaultTopContent() },
    mode: LoginMode = LoginMode.BOTH,
    onNavigateToCountrySelector: () -> Unit,
    onNavigateToOtpVerification: () -> Unit,
    authTelemetry: AuthTelemetry = koinInject(),
    loginViewModel: LoginViewModel = koinInject(),
) {
    LaunchedEffect(Unit) { authTelemetry.onSignupViewed(pageName) }

    // Get context for social sign-in (platform-specific)
    val context = getContext()

    // Get terms link from ViewModel
    val termsLink = remember { loginViewModel.getTncLink() }

    // Map LoginMode to SignupViewStyle
    val style =
        when (mode) {
            LoginMode.SOCIAL -> SignupViewStyle.ONLY_SOCIAL
            LoginMode.PHONE -> SignupViewStyle.ONLY_PHONE
            LoginMode.BOTH -> SignupViewStyle.SOCIAL_AND_PHONE
        }

    // Collect unified state
    val loginState by loginViewModel.state.collectAsState()

    // Extract individual states for convenience
    val selectedCountry = loginState.selectedCountry
    val phoneNumber = loginState.phoneNumber
    val phoneAuthState = loginState.phoneAuthState
    val phoneValidationError = loginState.phoneValidationError

    // Navigate to OTP screen when phone auth succeeds
    LaunchedEffect(phoneAuthState) {
        if (phoneAuthState is UiState.Success) {
            onNavigateToOtpVerification()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(46.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        topIconContent()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text =
                        headlineText
                            ?: getAnnotatedHeaderForLogin(
                                fullText = stringResource(Res.string.continue_to_sign_up_for_free),
                            ),
                    style = LocalAppTopography.current.xlSemiBold,
                    color = Color.White,
                )
                if (disclaimerText?.isEmpty() == true) return@Column
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = disclaimerText ?: stringResource(Res.string.sign_up_disclaimer),
                    style = LocalAppTopography.current.baseRegular,
                    color = Color.White,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Phone authentication section (for ONLY_PHONE and SOCIAL_AND_PHONE)
                if (style == SignupViewStyle.ONLY_PHONE || style == SignupViewStyle.SOCIAL_AND_PHONE) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Country picker and phone input
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CountryPickerButton(
                                selectedCountry = selectedCountry,
                                onClick = onNavigateToCountrySelector,
                                modifier = Modifier.width(90.dp),
                            )

                            PhoneInputField(
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = loginViewModel::onPhoneNumberChanged,
                                selectedCountry = selectedCountry,
                                isError = phoneValidationError != null,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Login button
                        YralGradientButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(DesignRes.string.login),
                            onClick = loginViewModel::onPhoneLoginClicked,
                            buttonState =
                                when {
                                    phoneAuthState is UiState.InProgress -> YralButtonState.Loading
                                    phoneNumber.trim().isEmpty() -> YralButtonState.Disabled
                                    selectedCountry == null -> YralButtonState.Disabled
                                    phoneValidationError != null -> YralButtonState.Disabled
                                    else -> {
                                        val numberLength = phoneNumber.length
                                        val minLength = selectedCountry.minLength
                                        val maxLength = selectedCountry.maxLength
                                        if (numberLength !in minLength..maxLength) {
                                            YralButtonState.Disabled
                                        } else {
                                            YralButtonState.Enabled
                                        }
                                    }
                                },
                        )

                        // Error message
                        phoneValidationError?.let { error ->
                            Text(
                                text = error,
                                style = LocalAppTopography.current.smRegular,
                                color = YralColors.ErrorRed,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // Terms of Service
                    Text(
                        text = annotateText(termsLink, openTerms),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                // "or" divider (only for SOCIAL_AND_PHONE)
                if (style == SignupViewStyle.SOCIAL_AND_PHONE) {
                    OrDivider()
                }

                // Social buttons (for ONLY_SOCIAL and SOCIAL_AND_PHONE)
                if (style == SignupViewStyle.ONLY_SOCIAL || style == SignupViewStyle.SOCIAL_AND_PHONE) {
                    if (style == SignupViewStyle.SOCIAL_AND_PHONE) {
                        // Icon-only buttons in a row for BOTH mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            defaultSocialProviders()
                                .filter { it != SocialProvider.PHONE_NUMBER }
                                .forEach { provider ->
                                    val iconRes =
                                        when (provider) {
                                            SocialProvider.GOOGLE -> DesignRes.drawable.google
                                            SocialProvider.APPLE -> DesignRes.drawable.apple
                                            SocialProvider.PHONE_NUMBER -> DesignRes.drawable.google
                                        }
                                    SocialIconButton(
                                        icon = iconRes,
                                        onClick = {
                                            context?.let {
                                                authTelemetry.onSignupJourneySelected(provider)
                                                loginViewModel.signInWithSocial(context, provider)
                                            }
                                        },
                                    )
                                }
                        }
                    } else {
                        // Full buttons with text for ONLY_SOCIAL mode
                        defaultSocialProviders()
                            .filter { it != SocialProvider.PHONE_NUMBER }
                            .forEach { provider ->
                                val buttonTextRes =
                                    when (provider) {
                                        SocialProvider.GOOGLE -> Res.string.signup_with_google
                                        SocialProvider.APPLE -> Res.string.signup_with_apple
                                        SocialProvider.PHONE_NUMBER -> Res.string.signup_with_google
                                    }
                                val iconRes =
                                    when (provider) {
                                        SocialProvider.GOOGLE -> DesignRes.drawable.google
                                        SocialProvider.APPLE -> DesignRes.drawable.apple
                                        SocialProvider.PHONE_NUMBER -> DesignRes.drawable.google
                                    }
                                YralButton(
                                    text = stringResource(buttonTextRes),
                                    icon = iconRes,
                                ) {
                                    context?.let {
                                        authTelemetry.onSignupJourneySelected(provider)
                                        loginViewModel.signInWithSocial(context, provider)
                                    }
                                }
                            }
                    }

                    // Terms for ONLY_SOCIAL
                    if (style == SignupViewStyle.ONLY_SOCIAL) {
                        Text(
                            text = annotateText(termsLink, openTerms),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultTopContent() {
    Image(
        painter = painterResource(Res.drawable.join_yral),
        contentDescription = "Join Yral",
        modifier =
            Modifier
                .padding(0.dp)
                .width(DEFAULT_TOP_CONTENT_WIDTH)
                .height(DEFAULT_TOP_CONTENT_HEIGHT),
    )
}

@Composable
fun getAnnotatedHeaderForLogin(
    fullText: String,
    maskedText: String = "",
) = buildAnnotatedString {
    val maskedStart = fullText.indexOf(maskedText)
    val maskedEnd = maskedStart + maskedText.length
    val textStyle = LocalAppTopography.current.xlSemiBold
    val spanStyle =
        SpanStyle(
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
        )
    if (maskedStart >= 0) {
        withStyle(
            style = spanStyle.copy(color = Color.White),
        ) { append(fullText.take(maskedStart)) }

        withStyle(
            style =
                spanStyle.copy(
                    brush = YralBrushes.GoldenTextBrush,
                    fontWeight = FontWeight.Bold,
                ),
        ) { append(fullText.substring(maskedStart, maskedEnd)) }

        if (maskedEnd < fullText.length) {
            withStyle(
                style = spanStyle.copy(color = Color.White),
            ) { append(fullText.substring(maskedEnd)) }
        }
    } else {
        withStyle(
            style = spanStyle.copy(color = Color.White),
        ) {
            append(fullText)
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = YralColors.Neutral700,
        )
        Text(
            text = stringResource(Res.string.or_label),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral500,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = YralColors.Neutral700,
        )
    }
}

@Composable
private fun annotateText(
    termsLink: String,
    openTerms: () -> Unit,
): AnnotatedString {
    val consentText = stringResource(Res.string.signup_consent)
    val termOfServiceText = stringResource(Res.string.terms_of_service_signup_consent)
    val defaultColor = YralColors.NeutralTextSecondary
    return buildAnnotatedString {
        val termsStart = consentText.indexOf(termOfServiceText)
        val termsEnd = termsStart + termOfServiceText.length
        val textStyle = LocalAppTopography.current.baseRegular
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
            )
        if (termsStart >= 0) {
            withStyle(
                style = spanStyle.copy(color = defaultColor),
            ) {
                append(consentText.take(termsStart))
            }
            withLink(
                LinkAnnotation.Url(
                    url = termsLink,
                    styles =
                        TextLinkStyles(
                            style =
                                spanStyle.copy(
                                    color = YralColors.Pink300,
                                    fontWeight = FontWeight.Bold,
                                ),
                        ),
                ) {
                    openTerms()
                },
            ) { append(consentText.substring(termsStart, termsEnd)) }
            if (termsEnd < consentText.length) {
                withStyle(
                    style = spanStyle.copy(color = defaultColor),
                ) {
                    append(consentText.substring(termsEnd))
                }
            }
        } else {
            withStyle(
                style = spanStyle.copy(color = defaultColor),
            ) {
                append(consentText)
            }
        }
    }
}

@Composable
private fun SocialIconButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .background(
                    color = YralColors.Neutral800,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
}

object SignupViewConstants {
    val DEFAULT_TOP_CONTENT_WIDTH = 240.dp
    val DEFAULT_TOP_CONTENT_HEIGHT = 86.dp
}

@Composable
internal expect fun getContext(): Any?
