package com.yral.shared.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.nav.RootComponent.Child
import com.yral.shared.app.ui.components.UpdateNotificationHost
import com.yral.shared.app.ui.screens.home.HomeScreen
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.root.viewmodels.RootError
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.toast.ToastHost
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
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
    val sessionState by viewModel.sessionManagerState.collectAsState()

    val analyticsUser = viewModel.analyticsUser.collectAsState(null)
    LaunchedEffect(analyticsUser.value) {
        viewModel.setUser(analyticsUser.value)
    }

    LaunchedEffect(sessionState) {
        if (sessionState != state.currentSessionState) {
            when (sessionState) {
                is SessionState.Initial -> viewModel.initialize()
                is SessionState.SignedIn -> viewModel.initialize()
                else -> Unit
            }
        }
    }

    rootComponent.setSplashActive(state.showSplash)

    Box(modifier = Modifier.fillMaxSize()) {
        Children(stack = rootComponent.stack, modifier = Modifier.fillMaxSize(), animation = stackAnimation(fade())) {
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
                    // Reset system bars to normal
                    HandleSystemBars(show = true)
                    HomeScreen(
                        component = child.component,
                        sessionState = sessionState,
                        bottomNavigationAnalytics = { viewModel.bottomNavigationClicked(it) },
                        updateProfileVideosCount = { viewModel.updateProfileVideosCount(it) },
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
        if (!state.showSplash && sessionState is SessionState.Loading) {
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
