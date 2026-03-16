package com.yral.shared.app.ui.screens.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedActionsRight
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.viewmodel.FeedViewModel

@Composable
fun FeedScaffoldScreen(
    component: FeedComponent,
    feedViewModel: FeedViewModel,
) {
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()

    FeedScreen(
        component = component,
        viewModel = feedViewModel,
        topOverlay = { _ -> },
        bottomOverlay = { _, _ -> },
        actionsRight = { pageNo ->
            FeedActionsRight(
                pageNo = pageNo,
                state = feedState,
                feedViewModel = feedViewModel,
                openProfile = component::openProfile,
            )
        },
        onPageChanged = { _, _ -> },
        onEdgeScrollAttempt = { },
        limitReelCount = feedState.feedDetails.size,
    )
}
