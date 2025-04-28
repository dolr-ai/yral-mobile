package com.yral.android.ui.screens.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.yral.shared.features.root.viewmodels.RootViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RootScreen(viewModel: RootViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

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
        HomeScreen(
            feedDetails = state.feedDetails,
            currentPage = state.currentPageOfFeed,
            onCurrentPageChange = { viewModel.onCurrentPageChange(it) },
            isLoadingMore = state.isLoadingMore,
            accountInfo = viewModel.getAccountInfo(),
            loadMoreFeed = { viewModel.loadMoreFeed() },
        )
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
        if (initialAnimationComplete) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lightning_lottie))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                isPlaying = true,
            )

            LottieAnimation(
                modifier = Modifier.fillMaxSize(),
                composition = composition,
                progress = { progress },
            )
        } else {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_lottie))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                isPlaying = true,
            )

            LottieAnimation(
                modifier = Modifier.fillMaxSize(),
                composition = composition,
                progress = { progress },
            )
            LaunchedEffect(progress) {
                if (progress == 1f) {
                    onAnimationComplete()
                }
            }
        }
    }
}
