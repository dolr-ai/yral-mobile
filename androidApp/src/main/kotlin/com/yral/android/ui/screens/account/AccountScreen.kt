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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.yral.android.R
import com.yral.android.ui.screens.account.AccountScreenConstants.SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.account.viewmodel.AccountBottomSheet
import com.yral.shared.features.account.viewmodel.AccountHelpLink
import com.yral.shared.features.account.viewmodel.AccountHelpLinkType
import com.yral.shared.features.account.viewmodel.AccountsState
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.account.viewmodel.ErrorType
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.features.auth.viewModel.LoginViewModel
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
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.libs.designsystem.generated.resources.alerts
import yral_mobile.shared.libs.designsystem.generated.resources.alerts_icon
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.delete
import androidx.compose.ui.res.stringResource as androidStringResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("SwallowedException", "LongMethod")
@Composable
fun AccountScreen(
    component: AccountComponent,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
    loginViewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loginState = loginViewModel.state.collectAsStateWithLifecycle()
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) {
            permissionsFactory.createPermissionsController()
        }
    BindEffect(permissionsController)
    val coroutineScope = rememberCoroutineScope()
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase = koinInject()
    val deregisterNotificationTokenUseCase: DeregisterNotificationTokenUseCase = koinInject()
    var initialAlertsSyncDone by remember { mutableStateOf(false) }
    val handleAlertsToggle =
        remember(permissionsController, registerNotificationTokenUseCase, deregisterNotificationTokenUseCase) {
            { enabled: Boolean ->
                coroutineScope.launch {
                    if (enabled) {
                        permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION)
                        try {
                            permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                            val granted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                            if (granted) {
                                val registered = registerNotificationToken(registerNotificationTokenUseCase)
                                viewModel.onAlertsToggleChanged(registered)
                            } else {
                                viewModel.onAlertsToggleChanged(false)
                            }
                        } catch (deniedAlways: DeniedAlwaysException) {
                            viewModel.onAlertsToggleChanged(false)
                        } catch (_: DeniedException) {
                            viewModel.onAlertsToggleChanged(false)
                        }
                    } else {
                        val deregistered = deregisterNotificationToken(deregisterNotificationTokenUseCase)
                        if (!deregistered) {
                            Logger.e("AccountScreen") { "Failed to deregister notifications" }
                        }
                        viewModel.onAlertsToggleChanged(false)
                    }
                }
                Unit
            }
        }
    LaunchedEffect(initialAlertsSyncDone, state.alertsEnabled) {
        if (!initialAlertsSyncDone) {
            if (state.alertsEnabled) {
                val granted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                if (granted) {
                    registerNotificationToken(registerNotificationTokenUseCase)
                } else {
                    deregisterNotificationToken(deregisterNotificationTokenUseCase)
                    viewModel.onAlertsToggleChanged(false)
                }
            }
            initialAlertsSyncDone = true
        }
    }
    LaunchedEffect(Unit) { viewModel.accountsTelemetry.onMenuScreenViewed() }
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    LaunchedEffect(loginState.value) {
        if (loginState.value is UiState.Failure) {
            viewModel.setBottomSheetType(AccountBottomSheet.SignUp)
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        AccountsTitle(state.isWalletEnabled) { component.onBack() }
        AccountScreenContent(
            state = state,
            viewModel = viewModel,
            onAlertsToggle = handleAlertsToggle,
        )
        SheetContent(
            bottomSheetState = bottomSheetState,
            bottomSheetType = state.bottomSheetType,
            tncLink = state.accountLinks.tnc,
            onDismissRequest = { viewModel.setBottomSheetType(AccountBottomSheet.None) },
            onDeleteAccount = { viewModel.deleteAccount() },
        )
    }
}

@Composable
private fun AccountScreenContent(
    state: AccountsState,
    viewModel: AccountsViewModel,
    onAlertsToggle: (Boolean) -> Unit,
) {
    val helperLinks = remember(state.isLoggedIn) { viewModel.getHelperLinks() }
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
            AccountInfoView(
                accountInfo = it,
                isSocialSignIn = state.isLoggedIn,
                showEditProfile = false,
                onLoginClicked = {
                    viewModel.setBottomSheetType(AccountBottomSheet.SignUp)
                    viewModel.accountsTelemetry.signUpClicked(SignupPageName.MENU)
                },
                onEditProfileClicked = {},
            )
        }
        HelpLinks(
            links = helperLinks,
            alertsEnabled = state.alertsEnabled,
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

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(
    bottomSheetState: SheetState,
    bottomSheetType: AccountBottomSheet,
    tncLink: String,
    onDismissRequest: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var extraSheetLink by remember { mutableStateOf("") }
    val extraSheetState = rememberModalBottomSheetState()
    when (bottomSheetType) {
        is AccountBottomSheet.SignUp -> {
            LoginBottomSheet(
                bottomSheetState = bottomSheetState,
                onDismissRequest = onDismissRequest,
                termsLink = tncLink,
                openTerms = { extraSheetLink = tncLink },
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
                title = androidStringResource(R.string.delete_your_account),
                subTitle = androidStringResource(R.string.delete_account_disclaimer),
                confirmationMessage = androidStringResource(R.string.delete_account_question),
                cancelButton = androidStringResource(R.string.no_take_me_back),
                deleteButton = androidStringResource(R.string.yes_delete),
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
                    R.string.could_not_login to R.string.could_not_login_desc
                }

                ErrorType.DELETE_ACCOUNT_FAILED -> {
                    R.string.error_delete_account_title to R.string.error_delete_account
                }
            }
        }
    YralErrorMessage(
        title = androidStringResource(title),
        error = androidStringResource(error),
        sheetState = bottomSheetState,
        cta = androidStringResource(R.string.ok),
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
            text = androidStringResource(R.string.accounts),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .offset(x = (-12).dp),
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
                    top = YralDimens.paddingLg,
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
                    painter =
                        if (it is DrawableResource) {
                            painterResource(it)
                        } else {
                            // temporary unless we completely migrate to drawableResource
                            painterResource(id = it as Int)
                        },
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
            text = androidStringResource(R.string.follow_us_on),
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

private suspend fun registerNotificationToken(registerUseCase: RegisterNotificationTokenUseCase): Boolean {
    val tokenResult = runCatching { FirebaseMessaging.getInstance().token.await() }
    val token = tokenResult.getOrNull()
    if (!tokenResult.isSuccess || token.isNullOrBlank()) {
        Logger.e("AccountScreen") {
            "Failed to fetch FCM token for registration: ${tokenResult.exceptionOrNull()?.message}"
        }
        return false
    }
    return runCatching {
        registerUseCase(RegisterNotificationTokenUseCase.Parameter(token = token))
    }.onFailure { error ->
        Logger.e("AccountScreen") {
            "Failed to register notifications: ${error.message}"
        }
    }.isSuccess
}

private suspend fun deregisterNotificationToken(deregisterUseCase: DeregisterNotificationTokenUseCase): Boolean {
    val tokenResult = runCatching { FirebaseMessaging.getInstance().token.await() }
    val token = tokenResult.getOrNull()
    if (!tokenResult.isSuccess || token.isNullOrBlank()) {
        Logger.e("AccountScreen") {
            "Failed to fetch FCM token for deregistration: ${tokenResult.exceptionOrNull()?.message}"
        }
        return false
    }
    return runCatching {
        deregisterUseCase(DeregisterNotificationTokenUseCase.Parameter(token = token))
    }.onFailure { error ->
        Logger.e("AccountScreen") {
            "Failed to deregister notifications: ${error.message}"
        }
    }.isSuccess
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
    when (type) {
        AccountHelpLinkType.TALK_TO_TEAM -> R.drawable.sms
        AccountHelpLinkType.TERMS_OF_SERVICE -> R.drawable.document
        AccountHelpLinkType.PRIVACY_POLICY -> R.drawable.lock
        AccountHelpLinkType.LOGOUT -> R.drawable.logout
        AccountHelpLinkType.DELETE_ACCOUNT -> DesignRes.drawable.delete
        else -> null
    }

private fun AccountHelpLink.getSocialIcon() =
    when (type) {
        AccountHelpLinkType.TELEGRAM -> R.drawable.telegram
        AccountHelpLinkType.DISCORD -> R.drawable.discord
        AccountHelpLinkType.TWITTER -> R.drawable.twitter
        else -> null
    }

@Composable
private fun AccountHelpLink.getText() =
    when (type) {
        AccountHelpLinkType.TALK_TO_TEAM -> linkText ?: androidStringResource(R.string.talk_to_the_team)
        AccountHelpLinkType.TERMS_OF_SERVICE -> androidStringResource(R.string.terms_of_service)
        AccountHelpLinkType.PRIVACY_POLICY -> androidStringResource(R.string.privacy_policy)
        AccountHelpLinkType.LOGOUT -> androidStringResource(R.string.logout)
        AccountHelpLinkType.DELETE_ACCOUNT -> androidStringResource(R.string.delete_account)
        else -> null
    }

object AccountScreenConstants {
    const val SOCIAL_MEDIA_LINK_BOTTOM_SPACER_WEIGHT = 0.2f
}
