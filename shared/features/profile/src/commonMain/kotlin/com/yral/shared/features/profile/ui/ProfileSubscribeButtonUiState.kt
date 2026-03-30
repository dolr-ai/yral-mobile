package com.yral.shared.features.profile.ui

import com.yral.shared.features.profile.viewmodel.ViewState

internal data class ProfileSubscribeButtonUiState(
    val shouldShow: Boolean,
    val isSubscribed: Boolean,
    val isLoading: Boolean,
)

internal fun ViewState.profileSubscribeButtonUiState(): ProfileSubscribeButtonUiState =
    ProfileSubscribeButtonUiState(
        shouldShow = !isOwnProfile && isAiInfluencer && isLoggedIn,
        isSubscribed = isSubscribedToInfluencer,
        isLoading = isInfluencerSubscriptionStateLoading,
    )
