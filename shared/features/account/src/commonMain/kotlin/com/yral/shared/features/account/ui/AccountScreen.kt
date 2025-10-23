package com.yral.shared.features.account.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.account.ui.AccountScreenConstants.SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT
import com.yral.shared.features.account.viewmodel.AccountBottomSheet
import com.yral.shared.features.account.viewmodel.AccountHelpLink
import com.yral.shared.features.account.viewmodel.AccountHelpLinkType
import com.yral.shared.features.account.viewmodel.AccountsState
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.account.viewmodel.ErrorType
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.AccountInfoView
import com.yral.shared.libs.designsystem.component.DeleteConfirmationSheet
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.YralDimens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.account.generated.resources.Res
import yral_mobile.shared.features.account.generated.resources.accounts
import yral_mobile.shared.features.account.generated.resources.delete_account
import yral_mobile.shared.features.account.generated.resources.delete_account_disclaimer
import yral_mobile.shared.features.account.generated.resources.delete_account_question
import yral_mobile.shared.features.account.generated.resources.delete_your_account
import yral_mobile.shared.features.account.generated.resources.discord
import yral_mobile.shared.features.account.generated.resources.document
import yral_mobile.shared.features.account.generated.resources.error_delete_account
import yral_mobile.shared.features.account.generated.resources.error_delete_account_title
import yral_mobile.shared.features.account.generated.resources.follow_us_on
import yral_mobile.shared.features.account.generated.resources.lock
import yral_mobile.shared.features.account.generated.resources.logout
import yral_mobile.shared.features.account.generated.resources.no_take_me_back
import yral_mobile.shared.features.account.generated.resources.privacy_policy
import yral_mobile.shared.features.account.generated.resources.sms
import yral_mobile.shared.features.account.generated.resources.talk_to_the_team
import yral_mobile.shared.features.account.generated.resources.telegram
import yral_mobile.shared.features.account.generated.resources.twitter
import yral_mobile.shared.features.account.generated.resources.yes_delete
import yral_mobile.shared.libs.designsystem.generated.resources.alerts
import yral_mobile.shared.libs.designsystem.generated.resources.alerts_icon
import yral_mobile.shared.libs.designsystem.generated.resources.arrow
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login_desc
import yral_mobile.shared.libs.designsystem.generated.resources.delete
import yral_mobile.shared.libs.designsystem.generated.resources.ok
import yral_mobile.shared.libs.designsystem.generated.resources.terms_of_service
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun AccountScreen(
    component: AccountComponent,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
    loginState: UiState<*>,
    loginBottomSheet: LoginBottomSheetComposable,
    onAlertsToggleRequest: suspend (Boolean) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.accountsTelemetry.onMenuScreenViewed() }
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    LaunchedEffect(loginState) {
        if (loginState is UiState.Failure) {
            viewModel.setBottomSheetType(AccountBottomSheet.SignUp)
        }
    }
    val scope = rememberCoroutineScope()
    val handleAlertsToggle: (Boolean) -> Unit =
        { enabled: Boolean ->
            val previous = state.alertsEnabled
            viewModel.onAlertsToggleChanged(enabled)
            scope.launch {
                val success =
                    runCatching { onAlertsToggleRequest(enabled) }
                        .onFailure { error ->
                            Logger.e("AccountScreen") { "Alerts toggle failed: ${error.message}" }
                        }.getOrElse { false }
                if (!success) {
                    viewModel.onAlertsToggleChanged(previous)
                }
            }
        }
    Column(modifier = modifier.fillMaxSize()) {
        AccountsTitle(state.isWalletEnabled) { component.onBack() }
        AccountScreenContent(
            state = state,
            viewModel = viewModel,
            alertsEnabled = state.alertsEnabled,
            onAlertsToggle = handleAlertsToggle,
        )
        SheetContent(
            bottomSheetState = bottomSheetState,
            bottomSheetType = state.bottomSheetType,
            tncLink = state.accountLinks.tnc,
            onDismissRequest = { viewModel.setBottomSheetType(AccountBottomSheet.None) },
            onDeleteAccount = { viewModel.deleteAccount() },
            loginBottomSheet = loginBottomSheet,
        )
    }
}

@Composable
private fun AccountScreenContent(
    state: AccountsState,
    viewModel: AccountsViewModel,
    alertsEnabled: Boolean,
    onAlertsToggle: (Boolean) -> Unit,
) {
    val helperLinks = remember(state.isLoggedIn) { viewModel.getHelperLinks() }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val accountInfo = state.accountInfo
        if (accountInfo != null) {
            AccountInfoView(
                accountInfo = accountInfo,
                isSocialSignIn = state.isLoggedIn,
                onLoginClicked = {
                    viewModel.setBottomSheetType(AccountBottomSheet.SignUp)
                    viewModel.accountsTelemetry.signUpClicked(SignupPageName.MENU)
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
        HelpLinks(
            links = helperLinks,
            alertsEnabled = alertsEnabled,
            onAlertsToggle = onAlertsToggle,
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

@OptIn(ExperimentalMaterial3Api::class)
typealias LoginBottomSheetComposable = @Composable (
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    termsLink: String,
    openTerms: () -> Unit,
) -> Unit

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(
    bottomSheetState: SheetState,
    bottomSheetType: AccountBottomSheet,
    tncLink: String,
    onDismissRequest: () -> Unit,
    onDeleteAccount: () -> Unit,
    loginBottomSheet: LoginBottomSheetComposable,
) {
    var extraSheetLink by remember { mutableStateOf("") }
    val extraSheetState = rememberModalBottomSheetState()
    when (bottomSheetType) {
        is AccountBottomSheet.SignUp -> {
            loginBottomSheet(
                bottomSheetState,
                onDismissRequest,
                tncLink,
                { extraSheetLink = tncLink },
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
                YralWebViewBottomSheet(
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
                title = stringResource(Res.string.delete_your_account),
                subTitle = stringResource(Res.string.delete_account_disclaimer),
                confirmationMessage = stringResource(Res.string.delete_account_question),
                cancelButton = stringResource(Res.string.no_take_me_back),
                deleteButton = stringResource(Res.string.yes_delete),
                onDismissRequest = onDismissRequest,
                onDelete = onDeleteAccount,
            )
        }

        is AccountBottomSheet.None -> Unit
    }
    if (extraSheetLink.isNotEmpty()) {
        YralWebViewBottomSheet(
            link = extraSheetLink,
            bottomSheetState = extraSheetState,
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
                    DesignRes.string.could_not_login to DesignRes.string.could_not_login_desc
                }

                ErrorType.DELETE_ACCOUNT_FAILED -> {
                    Res.string.error_delete_account_title to Res.string.error_delete_account
                }
            }
        }
    YralErrorMessage(
        title = stringResource(title),
        error = stringResource(error),
        sheetState = bottomSheetState,
        cta = stringResource(DesignRes.string.ok),
        onClick = onDismissRequest,
        onDismiss = onDismissRequest,
    )
}

@Composable
private fun AccountsTitle(
    isBackVisible: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isBackVisible) {
            Icon(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "back",
                tint = Color.White,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable { onBack() },
            )
        }
        Text(
            text = stringResource(Res.string.accounts),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).offset(x = (-12).dp),
        )
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
    alertsEnabled: Boolean,
    onAlertsToggle: (Boolean) -> Unit,
    onLinkClicked: (link: AccountHelpLink) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = YralDimens.paddingLg,
                ),
        horizontalAlignment = Alignment.End,
    ) {
        AlertsToggleRow(
            isEnabled = alertsEnabled,
            onToggle = onAlertsToggle,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(
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
}

@Composable
private fun AlertsToggleRow(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 26.dp)
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.alerts_icon),
            contentDescription = stringResource(DesignRes.string.alerts),
            contentScale = ContentScale.None,
        )
        Text(
            text = stringResource(DesignRes.string.alerts),
            style = LocalAppTopography.current.mdRegular,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = YralColors.NeutralTextPrimary,
                    checkedTrackColor = YralColors.Pink300,
                    uncheckedThumbColor = YralColors.NeutralTextPrimary,
                    uncheckedTrackColor = YralColors.Neutral700,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                ),
        )
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
            item.linkRemoteIcon?.let { url ->
                YralAsyncImage(
                    imageUrl = getSVGImageModel(url),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(20.dp),
                    shape = RectangleShape,
                )
            } ?: item.getIcon()?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = "support",
                    contentScale = ContentScale.None,
                )
            }
            item.getText()?.let {
                Text(
                    text = it,
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.NeutralTextPrimary,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(DesignRes.drawable.arrow),
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
            text = stringResource(Res.string.follow_us_on),
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
            painter = painterResource(it),
            contentDescription = "social account icon",
            contentScale = ContentScale.None,
        )
    }
}

private fun AccountHelpLink.getIcon() =
    when (type) {
        AccountHelpLinkType.TALK_TO_TEAM -> Res.drawable.sms
        AccountHelpLinkType.TERMS_OF_SERVICE -> Res.drawable.document
        AccountHelpLinkType.PRIVACY_POLICY -> Res.drawable.lock
        AccountHelpLinkType.LOGOUT -> Res.drawable.logout
        AccountHelpLinkType.DELETE_ACCOUNT -> DesignRes.drawable.delete
        else -> null
    }

private fun AccountHelpLink.getSocialIcon() =
    when (type) {
        AccountHelpLinkType.TELEGRAM -> Res.drawable.telegram
        AccountHelpLinkType.DISCORD -> Res.drawable.discord
        AccountHelpLinkType.TWITTER -> Res.drawable.twitter
        else -> null
    }

@Composable
private fun AccountHelpLink.getText() =
    when (type) {
        AccountHelpLinkType.TALK_TO_TEAM -> linkText ?: stringResource(Res.string.talk_to_the_team)
        AccountHelpLinkType.TERMS_OF_SERVICE -> stringResource(DesignRes.string.terms_of_service)
        AccountHelpLinkType.PRIVACY_POLICY -> stringResource(Res.string.privacy_policy)
        AccountHelpLinkType.LOGOUT -> stringResource(Res.string.logout)
        AccountHelpLinkType.DELETE_ACCOUNT -> stringResource(Res.string.delete_account)
        else -> null
    }

object AccountScreenConstants {
    const val SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT = 0.2f
}
