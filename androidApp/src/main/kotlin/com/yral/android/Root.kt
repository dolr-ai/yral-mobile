package com.yral.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.Greeting
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.useCases.FetchMoreFeedUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.domain.models.toFilteredResult
import com.yral.shared.rust.services.IndividualUserServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("LongMethod")
@Composable
fun Root() {
    val defaultAuthClient = remember { koinInstance.get<AuthClient>() }
    val individualUserServiceFactory = remember { koinInstance.get<IndividualUserServiceFactory>() }
    val getInitialPostsUseCase = remember { koinInstance.get<GetInitialFeedUseCase>() }
    val fetchMoreFeedUseCase = remember { koinInstance.get<FetchMoreFeedUseCase>() }
    var initialised by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            defaultAuthClient.initialize()
            defaultAuthClient.canisterPrincipal?.let { principal ->
                defaultAuthClient.identity?.let { identity ->
                    individualUserServiceFactory.initialize(
                        principal = principal,
                        identityData = identity,
                    )
                    initialised = true
                }
            }
        }
    }
    LaunchedEffect(initialised) {
        if (initialised && defaultAuthClient.canisterPrincipal != null) {
            withContext(Dispatchers.IO) {
                defaultAuthClient.canisterPrincipal?.let { principal ->
                    getInitialPostsUseCase
                        .invoke(
                            parameter =
                                GetInitialFeedUseCase.Params(
                                    canisterID = principal,
                                    filterResults = emptyList(),
                                ),
                        ).mapBoth(
                            success = { initialPosts ->
                                fetchMoreFeedUseCase
                                    .invoke(
                                        parameter =
                                            FetchMoreFeedUseCase.Params(
                                                canisterID = principal,
                                                filterResults =
                                                    initialPosts.posts.map { post ->
                                                        post.toFilteredResult()
                                                    },
                                            ),
                                    ).mapBoth(
                                        success = { moreFeed ->
                                            println(
                                                "xxxx duplicate post found: ${
                                                    initialPosts.posts.filter { post ->
                                                        moreFeed.posts.any { feed -> feed.videoID == post.videoID }
                                                    }
                                                }",
                                            )
                                        },
                                        failure = { println("xxxx error: $it") },
                                    )
                            },
                            failure = { println("xxxx error: $it") },
                        )
                }
            }
        }
    }
    MyApplicationTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Column {
                    GreetingView(Greeting().greet().plus(" from Native!!"))
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}
