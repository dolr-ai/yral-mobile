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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.home.HomeScreen
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.root.viewmodels.RootViewModel
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
        if (state.showSignupFailedBottomSheet) {
            SingUpFailedBottomSheet(
                bottomSheetState =
                    rememberModalBottomSheetState(
                        skipPartiallyExpanded = true,
                    ),
                onDismissRequest = { viewModel.setShowSignupFailedBottomSheet(false) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingUpFailedBottomSheet(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier.safeDrawingPadding(),
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
        containerColor = YralColors.Neutral900,
        dragHandle = null,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = 24.dp,
                            end = 16.dp,
                            bottom = 36.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.could_not_login),
                        style = LocalAppTopography.current.xlSemiBold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.could_not_login_desc),
                        style = LocalAppTopography.current.regRegular,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )
                }
                YralGradientButton(
                    text = stringResource(R.string.ok),
                    onClick = onDismissRequest,
                )
            }
        },
    )
}
