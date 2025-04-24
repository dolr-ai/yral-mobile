package com.yral.android.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralDimens
import com.yral.android.ui.screens.home.AccountScreenConstants.SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT
import com.yral.android.ui.widgets.YralWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(modifier: Modifier = Modifier) {
    var linkToOpen by remember { mutableStateOf(Pair("", false)) }
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(linkToOpen) {
        if (linkToOpen.first.isNotEmpty()) {
            if (linkToOpen.second) {
                val intent = Intent(Intent.ACTION_VIEW, linkToOpen.first.toUri())
                context.startActivity(intent)
                linkToOpen = Pair("", false)
            } else {
                showBottomSheet = true
            }
        } else {
            showBottomSheet = false
        }
    }
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccountsTitle()
        AccountDetail()
        Divider()
        HelpLinks { link, shouldOpenOutside ->
            linkToOpen = Pair(link, shouldOpenOutside)
        }
        Spacer(Modifier.weight(1f))
        SocialMediaHelpLinks { link, shouldOpenOutside ->
            linkToOpen = Pair(link, shouldOpenOutside)
        }
        Spacer(Modifier.weight(SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT))
    }

    if (showBottomSheet) {
        WebViewBottomSheet(
            link = linkToOpen.first,
            bottomSheetState = bottomSheetState,
        ) {
            showBottomSheet = false
            linkToOpen = Pair("", false)
        }
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
    val openInExternalBrowser: Boolean,
)

@Composable
private fun HelpLinks(onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit) {
    val links =
        listOf(
            HelpLink(
                icon = R.drawable.sms,
                text = stringResource(R.string.talk_to_the_team),
                link = "https://t.me/+c-LTX0Cp-ENmMzI1",
                openInExternalBrowser = false,
            ),
            HelpLink(
                icon = R.drawable.document,
                text = stringResource(R.string.terms_of_service),
                link = "https://yral.com/terms-ios",
                openInExternalBrowser = false,
            ),
            HelpLink(
                icon = R.drawable.lock,
                text = stringResource(R.string.privacy_policy),
                link = "https://yral.com/privacy-policy",
                openInExternalBrowser = false,
            ),
//            HelpLink(
//                icon = R.drawable.logout,
//                text = stringResource(R.string.logout),
//                link = "",
//                openInExternalBrowser = false,
//            ),
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
            HelpLinkItem(
                item = it,
                onLinkClicked = onLinkClicked,
            )
        }
    }
}

@Composable
private fun HelpLinkItem(
    item: HelpLink,
    onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(26.dp)
                .padding(top = 2.dp, bottom = 2.dp)
                .clickable { onLinkClicked(item.link, item.openInExternalBrowser) },
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
private fun SocialMediaHelpLinks(onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit) {
    val links =
        listOf(
            HelpLink(
                icon = R.drawable.telegram,
                text = "",
                link = "https://t.me/+c-LTX0Cp-ENmMzI1",
                openInExternalBrowser = true,
            ),
            HelpLink(
                icon = R.drawable.discord,
                text = "",
                link = "https://discord.com/invite/GZ9QemnZuj",
                openInExternalBrowser = true,
            ),
            HelpLink(
                icon = R.drawable.twitter,
                text = "",
                link = "https://twitter.com/Yral_app",
                openInExternalBrowser = true,
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
                SocialMediaHelpLinkItem(
                    item = it,
                    onLinkClicked = onLinkClicked,
                )
            }
        }
    }
}

@Composable
private fun SocialMediaHelpLinkItem(
    item: HelpLink,
    onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit,
) {
    Image(
        modifier =
            Modifier
                .padding(0.dp)
                .width(45.dp)
                .height(45.dp)
                .clickable { onLinkClicked(item.link, item.openInExternalBrowser) },
        painter = painterResource(id = item.icon),
        contentDescription = "image description",
        contentScale = ContentScale.None,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewBottomSheet(
    link: String,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier.safeDrawingPadding(),
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
        containerColor = YralColors.Neutral900,
        dragHandle = {
            DragHandle(
                color = YralColors.Neutral50,
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    YralWebView(
                        url = link,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    )
}

object AccountScreenConstants {
    const val SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT = 0.2f
}
