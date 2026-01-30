package com.yral.shared.app.ui.screens.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.app.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.account.ui.AccountScreen
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.profile.ui.EditProfileScreen
import com.yral.shared.features.profile.ui.ProfileMainScreen
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
internal fun ProfileScreen(
    component: ProfileComponent,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    accountsViewModel: AccountsViewModel,
    profileVideos: LazyPagingItems<FeedDetails>,
    onAlertsToggleRequest: suspend (Boolean) -> Boolean,
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
                    viewModel = profileViewModel,
                    profileVideos = profileVideos,
                )
            }

            is ProfileComponent.Child.Account -> {
                AccountScreen(
                    component = instance.component,
                    viewModel = accountsViewModel,
                    onAlertsToggleRequest = onAlertsToggleRequest,
                )
            }

            is ProfileComponent.Child.EditProfile ->
                EditProfileScreen(
                    component = instance.component,
                    viewModel = koinViewModel<EditProfileViewModel>(),
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}
