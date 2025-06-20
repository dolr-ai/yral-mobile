package com.yral.android.ui.screens.account

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralDimens
import com.yral.android.ui.screens.account.AccountScreenConstants.SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT
import com.yral.android.ui.widgets.YralErrorMessage
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.shared.features.account.viewmodel.AccountBottomSheet
import com.yral.shared.features.account.viewmodel.AccountHelpLink
import com.yral.shared.features.account.viewmodel.AccountInfo
import com.yral.shared.features.account.viewmodel.AccountsState
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.DELETE_ACCOUNT_URI
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.DISCORD_LINK
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.LOGOUT_URI
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.PRIVACY_POLICY_URL
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.TALK_TO_TEAM_URL
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.TELEGRAM_LINK
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.TERMS_OF_SERVICE_URL
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.TWITTER_LINK
import com.yral.shared.features.account.viewmodel.ErrorType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    LaunchedEffect(sessionState) {
        viewModel.refreshAccountInfo()
    }
    Box(
        modifier =
            modifier
                .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AccountScreenContent(
            state = state,
            viewModel = viewModel,
        )
        SheetContent(
            bottomSheetType = state.bottomSheetType,
            onDismissRequest = { viewModel.setBottomSheetType(AccountBottomSheet.None) },
            signInWithGoogle = { viewModel.signInWithGoogle() },
            onDeleteAccount = { viewModel.deleteAccount() },
        )
    }
}

@Composable
private fun AccountScreenContent(
    state: AccountsState,
    viewModel: AccountsViewModel,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccountsTitle()
        state.accountInfo?.let {
            AccountDetail(
                accountInfo = it,
                isSocialSignIn = state.isSocialSignInSuccessful,
            ) {
                viewModel.setBottomSheetType(AccountBottomSheet.SignUp)
            }
        }
        Divider()
        HelpLinks(
            links = viewModel.getHelperLinks(),
        ) { link, shouldOpenOutside ->
            viewModel.handleHelpLink(link, shouldOpenOutside)
        }
        Spacer(Modifier.weight(1f))
        SocialMediaHelpLinks(
            links = viewModel.getSocialLinks(),
        ) { link, shouldOpenOutside ->
            viewModel.setBottomSheetType(
                AccountBottomSheet.ShowWebView(
                    Pair(link, shouldOpenOutside),
                ),
            )
        }
        Spacer(Modifier.weight(SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(
    bottomSheetType: AccountBottomSheet,
    onDismissRequest: () -> Unit,
    signInWithGoogle: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    var extraSheetLink by remember { mutableStateOf("") }
    when (bottomSheetType) {
        is AccountBottomSheet.SignUp -> {
            LoginBottomSheet(
                bottomSheetState = bottomSheetState,
                onDismissRequest = onDismissRequest,
                onSignupClicked = { signInWithGoogle() },
                termsLink = TERMS_OF_SERVICE_URL,
                openTerms = {
                    extraSheetLink = TERMS_OF_SERVICE_URL
                },
            )
        }

        is AccountBottomSheet.ShowWebView -> {
            val linkToOpen = bottomSheetType.linkToOpen
            if (linkToOpen.second) {
                val context = LocalContext.current
                val intent = Intent(Intent.ACTION_VIEW, linkToOpen.first.toUri())
                context.startActivity(intent)
                onDismissRequest()
            } else {
                WebViewBottomSheet(
                    link = linkToOpen.first,
                    bottomSheetState = bottomSheetState,
                    onDismissRequest = onDismissRequest,
                )
            }
        }

        is AccountBottomSheet.ErrorMessage -> {
            ErrorMessageSheet(
                errorType = bottomSheetType.errorType,
                bottomSheetState = bottomSheetState,
                onDismissRequest = onDismissRequest,
            )
        }

        is AccountBottomSheet.DeleteAccount -> {
            DeleteAccountSheet(
                bottomSheetState = bottomSheetState,
                onDismissRequest = onDismissRequest,
                onDeleteAccount = onDeleteAccount,
            )
        }

        is AccountBottomSheet.None -> Unit
    }
    if (extraSheetLink.isNotEmpty()) {
        ExtraLinkSheet(
            extraSheetLink = extraSheetLink,
            onDismissRequest = { extraSheetLink = "" },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorMessageSheet(
    errorType: ErrorType,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
) {
    val (title, error) =
        remember {
            when (errorType) {
                ErrorType.SIGNUP_FAILED -> {
                    R.string.could_not_login to R.string.could_not_login_desc
                }

                ErrorType.DELETE_ACCOUNT_FAILED -> {
                    R.string.could_not_login to R.string.could_not_login_desc
                }
            }
        }
    YralErrorMessage(
        title = stringResource(title),
        error = stringResource(error),
        sheetState = bottomSheetState,
        cta = stringResource(R.string.ok),
        onClick = onDismissRequest,
        onDismiss = onDismissRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraLinkSheet(
    extraSheetLink: String,
    onDismissRequest: () -> Unit,
) {
    val extraSheetState = rememberModalBottomSheetState()
    LaunchedEffect(extraSheetLink) {
        if (extraSheetLink.isEmpty()) {
            extraSheetState.hide()
        } else {
            extraSheetState.show()
        }
    }
    WebViewBottomSheet(
        link = extraSheetLink,
        bottomSheetState = extraSheetState,
        onDismissRequest = onDismissRequest,
    )
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
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("UnusedParameter")
@Composable
private fun AccountDetail(
    accountInfo: AccountInfo,
    isSocialSignIn: Boolean,
    onLoginClicked: () -> Unit,
) {
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                modifier =
                    Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clip(CircleShape)
                        .background(color = Color.Blue),
                contentScale = ContentScale.FillBounds,
                model = accountInfo.profilePic,
                contentDescription = "Profile pic",
            )
            Text(
                text = accountInfo.userPrincipal,
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextSecondary,
            )
        }
        if (!isSocialSignIn) {
            YralGradientButton(
                text = stringResource(R.string.login),
                onClick = onLoginClicked,
            )
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp),
        color = YralColors.Divider,
    )
}

@Composable
private fun HelpLinks(
    links: List<AccountHelpLink>,
    onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit,
) {
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
    item: AccountHelpLink,
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
            item.getIcon()?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = "image description",
                    contentScale = ContentScale.None,
                )
            }
            item.getText()?.let {
                Text(
                    text = stringResource(it),
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.NeutralTextPrimary,
                )
            }
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
private fun SocialMediaHelpLinks(
    links: List<AccountHelpLink>,
    onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit,
) {
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
            textAlign = TextAlign.Center,
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
    item: AccountHelpLink,
    onLinkClicked: (link: String, shouldOpenOutside: Boolean) -> Unit,
) {
    item.getSocialIcon()?.let {
        Image(
            modifier =
                Modifier
                    .width(45.dp)
                    .height(45.dp)
                    .clickable { onLinkClicked(item.link, item.openInExternalBrowser) },
            painter = painterResource(id = it),
            contentDescription = "social account icon",
            contentScale = ContentScale.None,
        )
    }
}

private fun AccountHelpLink.getIcon() =
    when (link) {
        TALK_TO_TEAM_URL -> R.drawable.sms
        TERMS_OF_SERVICE_URL -> R.drawable.document
        PRIVACY_POLICY_URL -> R.drawable.lock
        LOGOUT_URI -> R.drawable.logout
        DELETE_ACCOUNT_URI -> R.drawable.delete
        else -> null
    }

private fun AccountHelpLink.getSocialIcon() =
    when (link) {
        TELEGRAM_LINK -> R.drawable.telegram
        DISCORD_LINK -> R.drawable.discord
        TWITTER_LINK -> R.drawable.twitter
        else -> null
    }

private fun AccountHelpLink.getText() =
    when (link) {
        TALK_TO_TEAM_URL -> R.string.talk_to_the_team
        TERMS_OF_SERVICE_URL -> R.string.terms_of_service
        PRIVACY_POLICY_URL -> R.string.privacy_policy
        LOGOUT_URI -> R.string.logout
        DELETE_ACCOUNT_URI -> R.string.delete_account
        else -> null
    }

object AccountScreenConstants {
    const val SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT = 0.2f
}
