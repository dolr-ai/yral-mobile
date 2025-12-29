package com.yral.shared.features.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.auth.utils.defaultSocialProviders
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.continue_to_sign_up_for_free
import yral_mobile.shared.features.auth.generated.resources.join_yral
import yral_mobile.shared.features.auth.generated.resources.sign_up_disclaimer
import yral_mobile.shared.features.auth.generated.resources.signup_consent
import yral_mobile.shared.features.auth.generated.resources.signup_with_apple
import yral_mobile.shared.features.auth.generated.resources.signup_with_google
import yral_mobile.shared.features.auth.generated.resources.terms_of_service_signup_consent
import yral_mobile.shared.libs.designsystem.generated.resources.apple
import yral_mobile.shared.libs.designsystem.generated.resources.google
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@Composable
fun SignupView(
    pageName: SignupPageName,
    termsLink: String,
    onSignupClicked: (SocialProvider) -> Unit,
    openTerms: () -> Unit,
    headlineText: AnnotatedString? = null,
    disclaimerText: String? = null,
    topIconContent: (@Composable () -> Unit)? = null,
    authTelemetry: AuthTelemetry = koinInject(),
) {
    LaunchedEffect(Unit) { authTelemetry.onSignupViewed(pageName) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(46.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        topIconContent?.invoke()
            ?: Image(
                painter = painterResource(Res.drawable.join_yral),
                contentDescription = "Join Yral",
                modifier =
                    Modifier
                        .padding(0.dp)
                        .width(240.dp)
                        .height(86.dp),
            )
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
                headlineText?.let {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = headlineText,
                        style = LocalAppTopography.current.xlSemiBold,
                        color = Color.White,
                    )
                }
                    ?: Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = stringResource(Res.string.continue_to_sign_up_for_free),
                        style = LocalAppTopography.current.xlSemiBold,
                        color = Color.White,
                    )
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
                defaultSocialProviders().forEach { provider ->
                    val buttonTextRes =
                        when (provider) {
                            SocialProvider.GOOGLE -> Res.string.signup_with_google
                            SocialProvider.APPLE -> Res.string.signup_with_apple
                        }
                    val iconRes =
                        when (provider) {
                            SocialProvider.GOOGLE -> DesignRes.drawable.google
                            SocialProvider.APPLE -> DesignRes.drawable.apple
                        }
                    YralButton(
                        text = stringResource(buttonTextRes),
                        icon = iconRes,
                    ) {
                        authTelemetry.onSignupJourneySelected(provider)
                        onSignupClicked(provider)
                    }
                }
                Text(
                    text = annotateText(termsLink, openTerms),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun annotateText(
    termsLink: String,
    openTerms: () -> Unit,
): AnnotatedString {
    val consentText = stringResource(Res.string.signup_consent)
    val termOfServiceText = stringResource(Res.string.terms_of_service_signup_consent)
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
                style = spanStyle.copy(color = Color.White),
            ) {
                append(consentText.substring(0, termsStart))
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
                    style = spanStyle.copy(color = Color.White),
                ) {
                    append(consentText.substring(termsEnd))
                }
            }
        } else {
            withStyle(
                style = spanStyle.copy(color = Color.White),
            ) {
                append(consentText)
            }
        }
    }
}
