package com.yral.android.ui.components.signup

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralButton
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.analytics.AuthTelemetry
import org.koin.compose.koinInject

@Suppress("LongMethod")
@Composable
fun SignupView(
    pageName: SignupPageName,
    termsLink: String,
    onSignupClicked: () -> Unit,
    openTerms: () -> Unit,
    authTelemetry: AuthTelemetry = koinInject(),
) {
    LaunchedEffect(Unit) { authTelemetry.onSignupViewed(pageName) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(46.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.join_yral),
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
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.continue_to_sign_up_for_free),
                    style = LocalAppTopography.current.xlSemiBold,
                    color = Color.White,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.sign_up_disclaimer),
                    style = LocalAppTopography.current.baseRegular,
                    color = Color.White,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                YralButton(
                    text = stringResource(R.string.signup_with_google),
                    icon = R.drawable.google,
                ) {
                    authTelemetry.onSignupJourneySelected()
                    onSignupClicked()
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
    val consentText = stringResource(R.string.signup_consent)
    val termOfServiceText = stringResource(R.string.terms_of_service_signup_consent)
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
