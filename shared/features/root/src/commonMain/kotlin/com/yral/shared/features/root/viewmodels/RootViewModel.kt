package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.services.IndividualUserServiceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("TooGenericExceptionCaught")
class RootViewModel(
    private val appDispatchers: AppDispatchers,
    private val authClient: AuthClient,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val getInitialFeedUseCase: GetInitialFeedUseCase,
    private val fetchFeedDetailsUseCase: FetchFeedDetailsUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    companion object {
        const val MIN_REQUIRED_ITEMS = 3
    }

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        coroutineScope.launch {
            if (authClient.canisterPrincipal == null) {
                withContext(appDispatchers.io) {
                    try {
                        authClient.initialize()
                        authClient.canisterPrincipal?.let { principal ->
                            authClient.identity?.let { identity ->
                                individualUserServiceFactory.initialize(
                                    principal = principal,
                                    identityData = identity,
                                )
                                loadFeedData(principal)
                            } ?: error("Identity is null")
                        } ?: error("Principal is null after initialization")
                    } catch (e: Exception) {
                        crashlyticsManager.recordException(e)
                    }
                }
            } else {
                loadFeedData(authClient.canisterPrincipal!!)
            }
        }
    }

    private suspend fun loadFeedData(principal: String) {
        getInitialFeedUseCase
            .invoke(
                parameter =
                    GetInitialFeedUseCase.Params(
                        canisterID = principal,
                        filterResults = emptyList(),
                    ),
            ).mapBoth(
                success = { result ->
                    val posts = result.posts
                    if (posts.isNotEmpty()) {
                        val feedDetailsList = mutableListOf<FeedDetails>()
                        posts.forEach { post ->
                            fetchFeedDetailsUseCase
                                .invoke(post)
                                .mapBoth(
                                    success = { detail ->
                                        feedDetailsList.add(detail)
                                        _state.emit(
                                            _state.value.copy(
                                                feedDetails = feedDetailsList.toList(),
                                                showSplash = feedDetailsList.size < MIN_REQUIRED_ITEMS,
                                            ),
                                        )
                                    },
                                    failure = { error ->
                                        error("Error loading feed details: $error")
                                    },
                                )
                        }
                    }
                },
                failure = { error ->
                    error("Error loading initial posts: $error")
                },
            )
    }

    fun onSplashAnimationComplete() {
        coroutineScope.launch {
            _state.emit(_state.value.copy(initialAnimationComplete = true))
        }
    }
}

data class RootState(
    val feedDetails: List<FeedDetails> = emptyList(),
    val showSplash: Boolean = true,
    val initialAnimationComplete: Boolean = false,
)
