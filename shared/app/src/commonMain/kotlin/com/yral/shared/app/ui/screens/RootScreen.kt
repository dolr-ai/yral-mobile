package com.yral.shared.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.nav.RootComponent.Child
import com.yral.shared.app.ui.components.MandatoryUpdateScreen
import com.yral.shared.app.ui.components.UpdateNotificationHost
import com.yral.shared.app.ui.screens.alertsrequest.AlertsRequestBottomSheet
import com.yral.shared.app.ui.screens.dailystreak.DailyStreakCelebrationScreen
import com.yral.shared.app.ui.screens.home.HomeScreen
import com.yral.shared.app.ui.screens.login.LoginBottomSheetSlotContent
import com.yral.shared.app.ui.screens.login.LoginScreenContent
import com.yral.shared.app.ui.screens.subscription.SubscriptionAccountMismatchSheet
import com.yral.shared.app.ui.screens.subscription.SubscriptionNudgeBottomSheet
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.getKey
import com.yral.shared.features.aiinfluencer.ui.CreateAIInfluencerScreen
import com.yral.shared.features.aiinfluencer.viewmodel.AiInfluencerViewModel
import com.yral.shared.features.chat.ui.conversation.ChatConversationScreen
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.features.profile.ui.EditProfileScreen
import com.yral.shared.features.profile.ui.ProfileMainScreen
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.root.viewmodels.AccountDialogInfo
import com.yral.shared.features.root.viewmodels.AccountUi
import com.yral.shared.features.root.viewmodels.NavigationTarget
import com.yral.shared.features.root.viewmodels.RootError
import com.yral.shared.features.root.viewmodels.RootEvent
import com.yral.shared.features.subscriptions.ui.SubscriptionsScreen
import com.yral.shared.features.uploadvideo.presentation.VideoDraftPollingManager
import com.yral.shared.features.wallet.ui.WalletScreen
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.toast.ToastHost
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.ai_influence_profile
import yral_mobile.shared.app.generated.resources.error_retry
import yral_mobile.shared.app.generated.resources.error_timeout
import yral_mobile.shared.app.generated.resources.error_timeout_title

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(rootComponent: RootComponent) {
    val viewModel = rootComponent.rootViewModel
    val videoDraftPollingManager: VideoDraftPollingManager = koinInject()
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    LifecycleResumeEffect(videoDraftPollingManager, state.sessionState) {
        videoDraftPollingManager.onAppForegrounded()
        onPauseOrDispose {
            videoDraftPollingManager.onAppBackgrounded()
        }
    }
    ObserveDraftCreatedNotifications(videoDraftPollingManager)
    LaunchedEffect(state.navigationTarget) {
        when (state.navigationTarget) {
            is NavigationTarget.Splash -> rootComponent.navigateToSplash()
            is NavigationTarget.MandatoryLogin -> rootComponent.navigateToMandatoryLogin()
            is NavigationTarget.Home -> rootComponent.navigateToHome()
        }
    }

    LaunchedEffect(state.showAccountDialog) {
        if (state.showAccountDialog) {
            rootComponent.showAccountSwitcherSlot()
        } else {
            rootComponent.dismissAccountSwitcherSlot()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.rootEvents.collect { event ->
            when (event) {
                is RootEvent.ShowDailyStreakCelebration -> {
                    rootComponent.showDailyStreakCelebration(event.streakCount)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            Modifier
                .fillMaxSize()
                .clearFocusOnUnconsumedTap(focusManager),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Children(
                stack = rootComponent.stack,
                modifier = Modifier.fillMaxSize(),
                animation =
                    stackAnimation { child ->
                        when (child.instance) {
                            is Child.Splash -> fade()
                            is Child.Home -> fade()
                            is Child.MandatoryLogin -> fade()
                            else -> fade() + slide()
                        }
                    },
            ) {
                when (val child = it.instance) {
                    is Child.Splash -> {
                        HandleSystemBars(show = false)
                        Splash(
                            modifier = Modifier.fillMaxSize(),
                            initialAnimationComplete = state.initialAnimationComplete,
                            onAnimationComplete = { viewModel.onSplashAnimationComplete() },
                            onScreenViewed = { viewModel.splashScreenViewed() },
                        )
                    }

                    is Child.Home -> {
                        HandleSystemBars(show = true)
                        HomeScreen(
                            component = child.component,
                            sessionState = state.sessionState,
                            bottomNavigationAnalytics = { viewModel.bottomNavigationClicked(it) },
                            updateProfileVideosCount = { viewModel.updateProfileVideosCount(it) },
                            isPendingLogin = viewModel.isPendingLogin(),
                            onDailyStreakClick = rootComponent::showDailyStreakCelebration,
                        )
                    }

                    is Child.EditProfile -> {
                        val sessionKey = state.sessionState.getKey()
                        HandleSystemBars(show = true)
                        EditProfileScreen(
                            component = child.component,
                            viewModel = koinViewModel<EditProfileViewModel>(key = "edit-profile-$sessionKey"),
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.Coach -> {
                        HandleSystemBars(show = true)
                        com.yral.shared.features.coach.ui.CoachScreen(
                            component = child.component,
                        )
                    }

                    is Child.SoulFile -> {
                        HandleSystemBars(show = true)
                        com.yral.shared.features.coach.ui.SoulFileScreen(
                            component = child.component,
                        )
                    }

                    is Child.UserProfile -> {
                        HandleSystemBars(show = true)
                        val profileViewModel =
                            koinViewModel<ProfileViewModel>(
                                key = "profile-${child.component.userCanisterData?.userPrincipalId}",
                            ) { parametersOf(child.component.userCanisterData) }
                        val profileVideos =
                            profileViewModel.profileVideos.collectAsLazyPagingItems()
                        ProfileMainScreen(
                            component = child.component,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                            viewModel = profileViewModel,
                            profileVideos = profileVideos,
                        )
                    }

                    is Child.Conversation -> {
                        HandleSystemBars(show = true)
                        ChatConversationScreen(
                            component = child.component,
                            viewModel = koinViewModel<ConversationViewModel>(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is Child.Wallet -> {
                        HandleSystemBars(show = true)
                        WalletScreen(
                            component = child.component,
                            onCreateInfluencerClick = { child.component.onCreateInfluencer() },
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.CreateInfluencer -> {
                        HandleSystemBars(show = true)
                        val aiInfluencerViewModel = koinViewModel<AiInfluencerViewModel>()
                        LaunchedEffect(Unit) { aiInfluencerViewModel.resetFlow() }
                        CreateAIInfluencerScreen(
                            component = child.component,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                            viewModel = aiInfluencerViewModel,
                        )
                    }

                    is Child.Subscription -> {
                        HandleSystemBars(show = true)
                        SubscriptionsScreen(
                            component = child.component,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.CountrySelector -> {
                        HandleSystemBars(show = true)
                        LoginScreenContent(
                            child = child,
                            rootComponent = rootComponent,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.OtpVerification -> {
                        HandleSystemBars(show = true)
                        LoginScreenContent(
                            child = child,
                            rootComponent = rootComponent,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.MandatoryLogin -> {
                        HandleSystemBars(show = false)
                        LoginScreenContent(
                            child = child,
                            rootComponent = rootComponent,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }
                }
                if (!rootComponent.isSplashActive()) {
                    ToastHost(
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp)
                                .statusBarsPadding()
                                .padding(top = 12.dp),
                    )
                }
            }

            // shows login error for both splash and account screen
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            LaunchedEffect(state.error) {
                if (state.error == null) {
                    sheetState.hide()
                }
            }
            state.error?.let { error ->
                YralErrorMessage(
                    title = stringResource(Res.string.error_timeout_title),
                    error = error.toErrorMessage(),
                    sheetState = sheetState,
                    cta = stringResource(Res.string.error_retry),
                    onDismiss = { viewModel.initialize() },
                    onClick = { viewModel.initialize() },
                )
            }
            // when session is loading & splash is not visible
            // 1. after logout on account screen during anonymous sign in
            // 2. after social sign in
            // 3. after delete account during anonymous sign in
            if (state.navigationTarget !is NavigationTarget.Splash && state.sessionState is SessionState.Loading) {
                BlockingLoader()
            }

            // Show update notifications (Snackbar) for flexible updates
            UpdateNotificationHost(
                rootComponent = rootComponent,
            )

            SlotContent(rootComponent)
        }
    }
}

private fun Modifier.clearFocusOnUnconsumedTap(focusManager: FocusManager): Modifier =
    pointerInput(focusManager) {
        awaitEachGesture {
            val down =
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Final,
                )
            var moved = false
            var upConsumed = true
            var trackingPointer = true
            while (trackingPointer) {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    trackingPointer = false
                } else {
                    if (change.positionChangeIgnoreConsumed() != Offset.Zero) {
                        moved = true
                    }
                    if (!change.pressed) {
                        upConsumed = change.isConsumed
                        trackingPointer = false
                    }
                }
            }
            if (!moved && !down.isConsumed && !upConsumed) {
                focusManager.clearFocus()
            }
        }
    }

@Composable
private fun AccountSwitchSheet(
    info: AccountDialogInfo,
    selectionEnabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 36.dp),
    ) {
        SheetSection(
            title = "Main Profile",
            accounts = listOfNotNull(info.mainAccount),
            selectionEnabled = selectionEnabled,
            onSelect = onSelect,
        )
        if (info.botAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SheetSection(
                title = stringResource(Res.string.ai_influence_profile),
                accounts = info.botAccounts,
                selectionEnabled = selectionEnabled,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun SheetSection(
    title: String,
    accounts: List<AccountUi>,
    selectionEnabled: Boolean,
    onSelect: (String) -> Unit,
) {
    if (accounts.isEmpty()) return
    Text(
        text = title,
        style = LocalAppTopography.current.baseSemiBold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 10.dp),
    )
    accounts.forEachIndexed { index, account ->
        val isFirst = index == 0
        val isLast = index == accounts.lastIndex
        AccountRow(
            account = account,
            selectionEnabled = selectionEnabled,
            onSelect = onSelect,
            isFirst = isFirst,
            isLast = isLast,
        )
    }
}

@Composable
private fun AccountRow(
    account: AccountUi,
    selectionEnabled: Boolean,
    onSelect: (String) -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val shape = accountRowShape(isFirst = isFirst, isLast = isLast)
    Surface(
        shape = shape,
        color = YralColors.Neutral900,
        tonalElevation = 0.dp,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(width = 1.dp, color = YralColors.Neutral700, shape = shape)
                .clickable(enabled = selectionEnabled) { onSelect(account.principal) },
    ) {
        AccountRowContent(account = account)
    }
}

@Composable
private fun AccountRowContent(account: AccountUi) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YralAsyncImage(
            imageUrl = account.avatarUrl,
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(YralColors.Neutral800),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = account.name,
            style = LocalAppTopography.current.mdMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (account.isActive) {
            ActiveAccountIndicator()
        }
    }
}

@Composable
private fun ActiveAccountIndicator() {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(YralColors.Pink300),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val strokeWidth = ACTIVE_ACCOUNT_CHECK_STROKE_WIDTH_DP.dp.toPx()
            drawLine(
                color = Color.White,
                start = Offset(size.width * ACTIVE_ACCOUNT_CHECK_START_X, size.height * ACTIVE_ACCOUNT_CHECK_START_Y),
                end = Offset(size.width * ACTIVE_ACCOUNT_CHECK_MID_X, size.height * ACTIVE_ACCOUNT_CHECK_MID_Y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * ACTIVE_ACCOUNT_CHECK_MID_X, size.height * ACTIVE_ACCOUNT_CHECK_MID_Y),
                end = Offset(size.width * ACTIVE_ACCOUNT_CHECK_END_X, size.height * ACTIVE_ACCOUNT_CHECK_END_Y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun accountRowShape(
    isFirst: Boolean,
    isLast: Boolean,
): RoundedCornerShape =
    when {
        isFirst && isLast -> RoundedCornerShape(8.dp)
        isFirst -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        isLast -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
        else -> RoundedCornerShape(0.dp)
    }

private const val ACTIVE_ACCOUNT_CHECK_START_X = 0.22f
private const val ACTIVE_ACCOUNT_CHECK_START_Y = 0.52f
private const val ACTIVE_ACCOUNT_CHECK_MID_X = 0.43f
private const val ACTIVE_ACCOUNT_CHECK_MID_Y = 0.72f
private const val ACTIVE_ACCOUNT_CHECK_END_X = 0.78f
private const val ACTIVE_ACCOUNT_CHECK_END_Y = 0.30f
private const val ACTIVE_ACCOUNT_CHECK_STROKE_WIDTH_DP = 2.5f

@Composable
private fun BlockingLoader() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        YralLoader()
    }
}

@Composable
internal expect fun HandleSystemBars(show: Boolean)

@Composable
private fun Splash(
    modifier: Modifier,
    initialAnimationComplete: Boolean,
    onAnimationComplete: () -> Unit = {},
    onScreenViewed: () -> Unit,
) {
    LaunchedEffect(Unit) { onScreenViewed() }
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        // Crossfade between animations
        AnimatedVisibility(
            visible = !initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = LottieRes.SPLASH,
                iterations = 1,
                contentScale = ContentScale.Crop,
                onAnimationComplete = onAnimationComplete,
            )
        }

        AnimatedVisibility(
            visible = initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = LottieRes.LIGHTNING,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun RootError.toErrorMessage(): String =
    stringResource(
        when (this) {
            RootError.TIMEOUT -> Res.string.error_timeout
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSwitcherSlotContent(component: RootComponent) {
    val viewModel = component.rootViewModel
    val state by viewModel.state.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    YralBottomSheet(
        onDismissRequest = { viewModel.dismissAccountDialog() },
        bottomSheetState = bottomSheetState,
        dragHandle = { DragHandle(color = YralColors.Neutral500) },
    ) {
        val accountDialogInfo = state.accountDialogInfo
        if (accountDialogInfo == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            AccountSwitchSheet(
                info = accountDialogInfo,
                selectionEnabled = !state.isAccountSwitchInProgress,
                onSelect = { principal ->
                    viewModel.dismissAccountDialog()
                    viewModel.switchToAccount(principal)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotContent(component: RootComponent) {
    val slot by component.slot.subscribeAsState()
    slot.child?.instance?.also { slotChild ->
        when (slotChild) {
            is RootComponent.SlotChild.AlertsRequestBottomSheet -> {
                AlertsRequestBottomSheet(component = slotChild.component)
            }

            is RootComponent.SlotChild.LoginBottomSheet -> {
                LoginBottomSheetSlotContent(rootComponent = component)
            }

            is RootComponent.SlotChild.SubscriptionAccountMismatchSheet -> {
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                SubscriptionAccountMismatchSheet(
                    bottomSheetState = bottomSheetState,
                    onDismissRequest = {
                        component.getSubscriptionCoordinator().dismissSubscriptionBottomSheet()
                    },
                    onUseAnotherAccount = {
                        component.getSubscriptionCoordinator().dismissSubscriptionBottomSheet()
                    },
                )
            }

            is RootComponent.SlotChild.SubscriptionNudge -> {
                val coordinator = component.getSubscriptionCoordinator()
                val nudgeContent = coordinator.subscriptionNudgeContent
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                if (nudgeContent != null) {
                    SubscriptionNudgeBottomSheet(
                        content = nudgeContent,
                        bottomSheetState = bottomSheetState,
                        onDismissRequest = { coordinator.dismissSubscriptionNudge() },
                        onSubscribe = {
                            coordinator.buySubscription()
                            coordinator.dismissSubscriptionNudge()
                        },
                    )
                } else {
                    LaunchedEffect(Unit) {
                        coordinator.dismissSubscriptionNudge()
                    }
                }
            }

            is RootComponent.SlotChild.MandatoryUpdate -> {
                MandatoryUpdateScreen()
            }

            is RootComponent.SlotChild.AccountSwitcher -> {
                AccountSwitcherSlotContent(component = component)
            }

            is RootComponent.SlotChild.DailyStreak -> {
                DailyStreakCelebrationScreen(
                    streakCount = slotChild.streakCount,
                    onDismiss = component::dismissDailyStreakCelebration,
                )
            }
        }
    }
}
