package com.yral.android.ui.screens.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.components.DeleteConfirmationSheet
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralDimens
import com.yral.android.ui.screens.account.AccountScreenConstants.SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralErrorMessage
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.features.account.viewmodel.AccountBottomSheet
import com.yral.shared.features.account.viewmodel.AccountHelpLink
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
    @Suppress("UnusedParameter")
    component: AccountComponent,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.accountsTelemetry.onMenuScreenViewed() }
    Column(modifier = modifier.fillMaxSize()) {
        AccountsTitle()
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
        state.accountInfo?.let {
            AccountDetail(
                accountInfo = it,
                isSocialSignIn = viewModel.isLoggedIn(),
            ) {
                viewModel.setBottomSheetType(AccountBottomSheet.SignUp)
                viewModel.accountsTelemetry.signUpClicked(SignupPageName.MENU)
            }
        }
        Divider()
        HelpLinks(
            links = viewModel.getHelperLinks(),
            onLinkClicked = {
                viewModel.accountsTelemetry.onMenuClicked(it.menuCtaType)
                viewModel.handleHelpLink(it)
            },
        )
        Spacer(Modifier.weight(1f))
        SocialMediaHelpLinks(
            links = viewModel.getSocialLinks(),
        ) { link ->
            viewModel.accountsTelemetry.onMenuClicked(MenuCtaType.FOLLOW_ON)
            viewModel.setBottomSheetType(
                AccountBottomSheet.ShowWebView(
                    linkToOpen = link,
                ),
            )
        }
        Spacer(Modifier.weight(SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT))
    }
}

@Suppress("LongMethod")
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
            if (linkToOpen.openInExternalBrowser) {
                onDismissRequest()
                runCatching {
                    val uriHandler = LocalUriHandler.current
                    uriHandler.openUri(linkToOpen.link)
                }
            } else {
                WebViewBottomSheet(
                    link = linkToOpen.link,
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
            DeleteConfirmationSheet(
                bottomSheetState = bottomSheetState,
                title = stringResource(R.string.delete_your_account),
                subTitle = stringResource(R.string.delete_account_disclaimer),
                confirmationMessage = stringResource(R.string.delete_account_question),
                cancelButton = stringResource(R.string.no_take_me_back),
                deleteButton = stringResource(R.string.yes_delete),
                onDismissRequest = onDismissRequest,
                onDelete = onDeleteAccount,
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
                    R.string.error_delete_account_title to R.string.error_delete_account
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
                .padding(16.dp),
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
            YralAsyncImage(
                imageUrl = accountInfo.profilePic,
                modifier = Modifier.size(60.dp),
            )
            Text(
                text = accountInfo.userPrincipal,
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextSecondary,
            )
        }
        if (!isSocialSignIn) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
            ) {
                YralGradientButton(
                    text = stringResource(R.string.login),
                    onClick = onLoginClicked,
                )
                Text(
                    text = stringResource(R.string.anonymous_account_setup),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
    onLinkClicked: (link: AccountHelpLink) -> Unit,
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
    onLinkClicked: (link: AccountHelpLink) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(26.dp)
                .padding(top = 2.dp, bottom = 2.dp)
                .clickable { onLinkClicked(item) },
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
    onLinkClicked: (link: AccountHelpLink) -> Unit,
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
    onLinkClicked: (link: AccountHelpLink) -> Unit,
) {
    item.getSocialIcon()?.let {
        Image(
            modifier =
                Modifier
                    .width(45.dp)
                    .height(45.dp)
                    .clickable { onLinkClicked(item) },
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
