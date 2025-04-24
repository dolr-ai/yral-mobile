package com.yral.android.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralDimens
import com.yral.android.ui.screens.home.AccountScreenConstants.SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT

@Composable
fun AccountScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccountsTitle()
        AccountDetail()
        Divider()
        HelpLinks()
        Spacer(Modifier.weight(1f))
        SocialMediaHelpLinks()
        Spacer(Modifier.weight(SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT))
    }
}

@Composable
private fun AccountsTitle() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = YralDimens.paddingLg,
                    end = 16.dp,
                    bottom = YralDimens.paddingLg,
                ),
        horizontalArrangement = Arrangement.spacedBy(91.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.accounts),
            style = LocalAppTopography.current.xlBold,
        )
    }
}

@Composable
private fun AccountDetail() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = YralDimens.paddingLg,
                    end = 16.dp,
                    bottom = YralDimens.paddingLg,
                ),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                Image(
//                    painter = painterResource(id = R.drawable.image5),
//                    contentDescription = "image description",
//                    contentScale = ContentScale.FillBounds
//                )
//                Text(
//                    text = "mqxpy-vp4st-vhw6p-poxzk-i363n-y4fagmqxpy-vp4st",
//                    style = LocalAppTopography.current.baseMedium,
//                )
//            }
//            YralButton(
//                text = stringResource(R.string.login)
//            ) { }
        Text(
            text = stringResource(R.string.coming_soon),
            style = LocalAppTopography.current.baseMedium,
        )
    }
}

@Composable
private fun Divider() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = YralColors.Divider),
    ) {}
}

data class HelpLink(
    val icon: Int,
    val text: String,
    val link: String,
)

@Composable
private fun HelpLinks() {
    val links =
        listOf(
            HelpLink(
                icon = R.drawable.sms,
                text = stringResource(R.string.talk_to_the_team),
                link = "https://t.me/+c-LTX0Cp-ENmMzI1",
            ),
            HelpLink(
                icon = R.drawable.document,
                text = stringResource(R.string.terms_of_service),
                link = "https://yral.com/terms-ios",
            ),
            HelpLink(
                icon = R.drawable.lock,
                text = stringResource(R.string.privacy_policy),
                link = "https://yral.com/privacy-policy",
            ),
            HelpLink(
                icon = R.drawable.logout,
                text = stringResource(R.string.logout),
                link = "",
            ),
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = YralDimens.paddingLg,
                    end = 16.dp,
                    bottom = YralDimens.paddingLg,
                ),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.End,
    ) {
        links.forEach {
            HelpLinkItem(it) { }
        }
    }
}

@Composable
private fun HelpLinkItem(
    item: HelpLink,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(26.dp)
                .padding(top = 2.dp, bottom = 2.dp)
                .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = item.icon),
                contentDescription = "image description",
                contentScale = ContentScale.None,
            )
            Text(
                text = item.text,
                style = LocalAppTopography.current.mdRegular,
                color = YralColors.NeutralTextPrimary,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.arrow),
            contentDescription = "image description",
            contentScale = ContentScale.None,
        )
    }
}

@Composable
private fun SocialMediaHelpLinks() {
    val links =
        listOf(
            HelpLink(
                icon = R.drawable.telegram,
                text = "",
                link = "https://t.me/+c-LTX0Cp-ENmMzI1",
            ),
            HelpLink(
                icon = R.drawable.discord,
                text = "",
                link = "https://discord.com/invite/GZ9QemnZuj",
            ),
            HelpLink(
                icon = R.drawable.twitter,
                text = "",
                link = "https://twitter.com/Yral_app",
            ),
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = YralDimens.paddingLg,
                    end = 16.dp,
                    bottom = YralDimens.paddingLg,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.follow_us_on),
            style = LocalAppTopography.current.regRegular,
            color = YralColors.Neutral500,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            links.forEach {
                SocialMediaHelpLinkItem(it.icon) { }
            }
        }
    }
}

@Composable
private fun SocialMediaHelpLinkItem(
    icon: Int,
    onClick: () -> Unit,
) {
    Image(
        modifier =
            Modifier
                .padding(0.dp)
                .width(45.dp)
                .height(45.dp)
                .clickable { onClick() },
        painter = painterResource(id = icon),
        contentDescription = "image description",
        contentScale = ContentScale.None,
    )
}

object AccountScreenConstants {
    const val SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT = 0.2f
}
