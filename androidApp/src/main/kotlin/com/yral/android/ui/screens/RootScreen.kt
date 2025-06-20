package com.yral.android.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yral.android.R
import com.yral.android.ui.screens.home.HomeScreen
import com.yral.android.ui.widgets.YralErrorMessage
import com.yral.android.ui.widgets.YralLoader
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.root.viewmodels.RootError
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.koin.koinInstance
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(viewModel: RootViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val sessionState by viewModel.sessionManagerState.collectAsState()
    val feedViewModel by remember { mutableStateOf(koinInstance.get<FeedViewModel>()) }
    LaunchedEffect(sessionState) {
        if (sessionState != state.currentSessionState) {
            if (sessionState is SessionState.Initial) {
                // first app open or after logout
                viewModel.initialize()
            }
            if (sessionState is SessionState.SignedIn) {
                // initialize rust and close splash if visible
                viewModel.initialize()
                // cold start feed
                feedViewModel.initialize()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.showSplash) {
            HandleSystemBars(show = false)
            Splash(
                modifier = Modifier.fillMaxSize(),
                initialAnimationComplete = state.initialAnimationComplete,
                onAnimationComplete = { viewModel.onSplashAnimationComplete() },
            )
        } else {
            // Reset system bars to normal
            HandleSystemBars(show = true)
            HomeScreen(
                feedViewModel = feedViewModel,
                currentTab = state.currentHomePageTab,
                updateCurrentTab = { viewModel.updateCurrentTab(it) },
            )
        }
        // shows login error for both splash and account screen
        val sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        LaunchedEffect(state.error) {
            if (state.error == null) {
                sheetState.hide()
            }
        }
        state.error?.let { error ->
            YralErrorMessage(
                error = error.toErrorMessage(),
                sheetState = sheetState,
                cta = stringResource(R.string.error_retry),
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
private fun HandleSystemBars(show: Boolean) {
    val window = (LocalActivity.current as? ComponentActivity)?.window
    if (show) {
        LaunchedEffect(Unit) {
            window?.let { w ->
                WindowInsetsControllerCompat(
                    w,
                    w.decorView,
                ).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    } else {
        LaunchedEffect(Unit) {
            window?.let { w ->
                WindowInsetsControllerCompat(
                    w,
                    w.decorView,
                ).hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
private fun Splash(
    modifier: Modifier,
    initialAnimationComplete: Boolean,
    onAnimationComplete: () -> Unit = {},
) {
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
                rawRes = R.raw.splash_lottie,
                iterations = 1,
            ) {
                onAnimationComplete()
            }
        }

        AnimatedVisibility(
            visible = initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = R.raw.lightning_lottie,
            )
        }
    }
}

@Composable
fun RootError.toErrorMessage(): String =
    stringResource(
        when (this) {
            RootError.TIMEOUT -> R.string.error_timeout
        },
    )
