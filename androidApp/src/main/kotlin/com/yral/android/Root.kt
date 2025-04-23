package com.yral.android

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.services.IndividualUserServiceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

const val MIN_REQUIRED_ITEMS = 3

@Suppress("LongMethod")
@Composable
fun Root() {
    val appDispatchers = koinInstance.get<AppDispatchers>()
    val defaultAuthClient = koinInstance.get<AuthClient>()
    val individualUserServiceFactory = koinInstance.get<IndividualUserServiceFactory>()

    // State management
    var showSplash by remember { mutableStateOf(true) }
    var feedDetails by remember { mutableStateOf(emptyList<FeedDetails>()) }

    // Create StateFlow for feed updates
    val feedFlow = remember { MutableStateFlow(emptyList<FeedDetails>()) }

    // Collect the flow changes and update state
    LaunchedEffect(key1 = feedFlow) {
        feedFlow.collect { newFeedDetails ->
            feedDetails = newFeedDetails
            showSplash = defaultAuthClient.canisterPrincipal == null ||
                newFeedDetails.size < MIN_REQUIRED_ITEMS
        }
    }

    LaunchedEffect(Unit) {
        if (defaultAuthClient.canisterPrincipal == null) {
            withContext(appDispatchers.io) {
                defaultAuthClient.initialize()
                defaultAuthClient.canisterPrincipal?.let { principal ->
                    defaultAuthClient.identity?.let { identity ->
                        individualUserServiceFactory.initialize(
                            principal = principal,
                            identityData = identity,
                        )
                        loadFeedData(
                            principal = principal,
                            feedFlow = feedFlow,
                        )
                    } ?: error("Identity is null")
                } ?: error("Principal is null after initialization")
            }
        }
    }
    if (showSplash) {
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
        )
    } else {
        // Reset system bars to normal when initialized
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
            modifier =
                Modifier
                    .fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                FeedContent(feedDetails)
            }
        }
    }
}

@Composable
private fun Splash(modifier: Modifier) {
    var initialAnimationComplete by remember { mutableStateOf(false) }
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
                    initialAnimationComplete = true
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

private suspend fun loadFeedData(
    principal: String,
    feedFlow: MutableStateFlow<List<FeedDetails>>,
) {
    val getInitialPostsUseCase = koinInstance.get<GetInitialFeedUseCase>()
    val fetchFeedDetailsUseCase = koinInstance.get<FetchFeedDetailsUseCase>()

    var initialPosts = emptyList<Post>()
    var feedDetails = emptyList<FeedDetails>()

    getInitialPostsUseCase
        .invoke(
            parameter =
                GetInitialFeedUseCase.Params(
                    canisterID = principal,
                    filterResults = emptyList(),
                ),
        ).mapBoth(
            success = { initialPosts = it.posts },
            failure = { error("Error loading initial posts: $it") },
        )

    if (initialPosts.isNotEmpty()) {
        initialPosts.forEach { post ->
            fetchFeedDetailsUseCase
                .invoke(post)
                .mapBoth(
                    success = { detail ->
                        feedDetails = feedDetails + detail
                        feedFlow.value = feedDetails
                    },
                    failure = { error("Error loading feed details: $it") },
                )
        }
    }
}
