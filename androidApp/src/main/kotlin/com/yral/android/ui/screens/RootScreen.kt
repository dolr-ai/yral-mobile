package com.yral.android.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.android.R
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.rust.domain.models.FeedDetails
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                FeedContent(state.feedDetails)
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

@Composable
private fun FeedContent(feedDetails: List<FeedDetails>) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        items(feedDetails) { item ->
            FeedItem(item)
        }
    }
}

@Composable
private fun FeedItem(item: FeedDetails) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ).padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.canisterID,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.url.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
