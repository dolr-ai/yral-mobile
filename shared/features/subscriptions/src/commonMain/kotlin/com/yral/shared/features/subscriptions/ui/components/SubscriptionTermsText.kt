package com.yral.shared.features.subscriptions.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.subscriptions.generated.resources.Res
import yral_mobile.shared.features.subscriptions.generated.resources.subscription_active_terms

private const val TERMS_SEPARATOR = "  |  "

@Composable
fun SubscriptionTermsText(
    tncUrl: String,
    privacyPolicyUrl: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val fullText = stringResource(Res.string.subscription_active_terms)
    val tncText = fullText.substringBefore(TERMS_SEPARATOR).trim()
    val privacyText = fullText.substringAfter(TERMS_SEPARATOR).trim()
    val textStyle = LocalAppTopography.current.baseRegular
    val linkStyle =
        SpanStyle(
            color = YralColors.Neutral300,
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
        )
    val annotatedString =
        buildAnnotatedString {
            withLink(
                LinkAnnotation.Url(
                    url = tncUrl,
                    styles = TextLinkStyles(style = linkStyle),
                ) {
                    onOpenLink(tncUrl)
                },
            ) {
                append(tncText)
            }
            append(TERMS_SEPARATOR)
            withLink(
                LinkAnnotation.Url(
                    url = privacyPolicyUrl,
                    styles = TextLinkStyles(style = linkStyle),
                ) {
                    onOpenLink(privacyPolicyUrl)
                },
            ) {
                append(privacyText)
            }
        }
    Text(
        text = annotatedString,
        style = textStyle,
        color = YralColors.Neutral300,
        textAlign = textAlign,
        modifier = modifier,
    )
}
