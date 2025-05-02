package com.yral.android.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.android.R
import com.yral.android.ui.screens.home.HomeScreen
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.root.viewmodels.RootViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RootScreen(viewModel: RootViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val sessionState by viewModel.sessionManagerState.collectAsState()
    LaunchedEffect(sessionState) {
        if (sessionState != state.currentSessionState) {
            viewModel.initialize()
        }
    }
    if (state.showSplash) {
        val window = (LocalActivity.current as? ComponentActivity)?.window
        LaunchedEffect(Unit) {
            window?.let { w ->
                WindowInsetsControllerCompat(
                    w,
                    w.decorView,
                ).hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        Splash(
            modifier = Modifier.fillMaxSize(),
            initialAnimationComplete = state.initialAnimationComplete,
            onAnimationComplete = { viewModel.onSplashAnimationComplete() },
        )
    } else {
        // Reset system bars to normal
        val window = (LocalActivity.current as? ComponentActivity)?.window
        LaunchedEffect(Unit) {
            window?.let { w ->
                WindowInsetsControllerCompat(
                    w,
                    w.decorView,
                ).show(WindowInsetsCompat.Type.systemBars())
            }
        }
        val feedViewModel = remember { viewModel.createFeedViewModel() }
        HomeScreen(
            createFeedViewModel = { feedViewModel },
            currentTab = state.currentHomePageTab,
            updateCurrentTab = { viewModel.updateCurrentTab(it) },
        )
        if (state.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                YralLoader()
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
        // The initial splash animation
        val splashComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_lottie))
        val splashProgress by animateLottieCompositionAsState(
            composition = splashComposition,
            iterations = 1,
            isPlaying = true,
        )

        // The continuous lightning animation
        val lightningComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lightning_lottie))
        val lightningProgress by animateLottieCompositionAsState(
            composition = lightningComposition,
            iterations = LottieConstants.IterateForever,
            isPlaying = initialAnimationComplete, // Only start playing when initial animation is complete
        )

        // Crossfade between animations
        AnimatedVisibility(
            visible = !initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LottieAnimation(
                modifier = Modifier.fillMaxSize(),
                composition = splashComposition,
                progress = { splashProgress },
            )
        }

        AnimatedVisibility(
            visible = initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LottieAnimation(
                modifier = Modifier.fillMaxSize(),
                composition = lightningComposition,
                progress = { lightningProgress },
            )
        }

        LaunchedEffect(splashProgress) {
            if (splashProgress == 1f) {
                onAnimationComplete()
            }
        }
    }
}
