package com.yral.shared.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.nav.RootComponent.Child
import com.yral.shared.app.ui.components.UpdateNotificationHost
import com.yral.shared.app.ui.screens.alertsrequest.AlertsRequestBottomSheet
import com.yral.shared.app.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.shared.app.ui.screens.home.HomeScreen
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.getKey
import com.yral.shared.features.auth.ui.LoginBottomSheet
import com.yral.shared.features.profile.ui.EditProfileScreen
import com.yral.shared.features.profile.ui.ProfileMainScreen
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.root.viewmodels.RootError
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.toast.ToastHost
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.error_retry
import yral_mobile.shared.app.generated.resources.error_timeout
import yral_mobile.shared.app.generated.resources.error_timeout_title

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(
    rootComponent: RootComponent,
    viewModel: RootViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    rootComponent.setSplashActive(state.showSplash)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Children(
                stack = rootComponent.stack,
                modifier = Modifier.fillMaxSize(),
                animation = stackAnimation(fade()),
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
                            loginState = UiState.Initial,
                            loginBottomSheet = { bottomSheetState, onDismissRequest, termsLink, openTerms ->
                                LoginBottomSheet(
                                    bottomSheetState = bottomSheetState,
                                    onDismissRequest = onDismissRequest,
                                    termsLink = termsLink,
                                    openTerms = openTerms,
                                )
                            },
                            getPrefetchListener = { reel -> PrefetchVideoListenerImpl(reel) },
                        )
                    }
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
            if (!state.showSplash && state.sessionState is SessionState.Loading) {
                BlockingLoader()
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

            // Show update notifications (Snackbar) for flexible updates
            UpdateNotificationHost(
                rootComponent = rootComponent,
            )

            SlotContent(rootComponent)
        }
    }
}

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

@Composable
private fun SlotContent(component: RootComponent) {
    val slot by component.slot.subscribeAsState()
    slot.child?.instance?.also { slotChild ->
        when (slotChild) {
            is RootComponent.SlotChild.AlertsRequestBottomSheet -> {
                AlertsRequestBottomSheet(component = slotChild.component)
            }
        }
    }
}
