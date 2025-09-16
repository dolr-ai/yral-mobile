package com.yral.android.ui.screens.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.android.ui.screens.account.AccountScreen
import com.yral.android.ui.screens.profile.main.ProfileMainScreen
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    component: ProfileComponent,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    profileVideos: LazyPagingItems<FeedDetails>,
) {
    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
        modifier = modifier,
    ) { child ->
        when (val instance = child.instance) {
            is ProfileComponent.Child.Main -> {
                ProfileMainScreen(
                    component = instance.component,
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    profileVideos = profileVideos,
                )
            }
            is ProfileComponent.Child.Account -> {
                AccountScreen(
                    component = instance.component,
                )
            }
        }
    }
}
