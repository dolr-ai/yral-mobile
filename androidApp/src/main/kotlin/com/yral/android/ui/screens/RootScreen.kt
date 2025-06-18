package com.yral.android.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.screens.home.HomeScreen
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLottieAnimation
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
    LaunchedEffect(sessionState) {
        if (sessionState != state.currentSessionState) {
            viewModel.initialize()
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
                ErrorState(
                    error = error.toErrorMessage(),
                    sheetState = sheetState,
                    onRetry = { viewModel.initialize() },
                )
            }
        } else {
            // Reset system bars to normal
            HandleSystemBars(show = true)
            val feedViewModel = remember { koinInstance.get<FeedViewModel>() }
            HomeScreen(
                createFeedViewModel = { feedViewModel },
                currentTab = state.currentHomePageTab,
                updateCurrentTab = { viewModel.updateCurrentTab(it) },
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorState(
    error: String,
    sheetState: SheetState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YralBottomSheet(
        onDismissRequest = onRetry,
        bottomSheetState = sheetState,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.padding(16.dp),
        ) {
            Text(
                text = error,
                style = LocalAppTopography.current.mdMedium,
                textAlign = TextAlign.Start,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            YralGradientButton(
                text = stringResource(R.string.error_retry),
                onClick = onRetry,
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
