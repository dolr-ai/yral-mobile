package com.yral.android.ui.screens.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.android.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.account.ui.AccountScreen
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.auth.ui.LoginBottomSheet
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.features.profile.ui.EditProfileScreen
import com.yral.shared.features.profile.ui.ProfileMainScreen
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    component: ProfileComponent,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    accountsViewModel: AccountsViewModel,
    profileVideos: LazyPagingItems<FeedDetails>,
) {
    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
        modifier = modifier,
    ) { child ->
        when (val instance = child.instance) {
            is ProfileComponent.Child.Main -> {
                val loginViewModel: LoginViewModel = koinViewModel()
                val loginState by loginViewModel.state.collectAsStateWithLifecycle()

                ProfileMainScreen(
                    component = instance.component,
                    modifier = Modifier.fillMaxSize(),
                    viewModel = profileViewModel,
                    profileVideos = profileVideos,
                    loginState = loginState,
                    loginBottomSheet = { bottomSheetState, onDismissRequest, termsLink, openTerms ->
                        LoginBottomSheet(
                            bottomSheetState = bottomSheetState,
                            onDismissRequest = onDismissRequest,
                            termsLink = termsLink,
                            openTerms = openTerms,
                        )
                    },
                    getPrefetchListener = { reel -> PrefetchVideoListenerImpl(reel) },
                )
            }
            is ProfileComponent.Child.Account -> {
                val loginViewModel: LoginViewModel = koinViewModel()
                val loginState by loginViewModel.state.collectAsStateWithLifecycle()
                AccountScreen(
                    component = instance.component,
                    viewModel = accountsViewModel,
                    loginState = loginState,
                    loginBottomSheet = { bottomSheetState, onDismissRequest, termsLink, openTerms ->
                        LoginBottomSheet(
                            bottomSheetState = bottomSheetState,
                            onDismissRequest = onDismissRequest,
                            termsLink = termsLink,
                            openTerms = openTerms,
                        )
                    },
                )
            }
            is ProfileComponent.Child.EditProfile -> {
                val editProfileViewModel: EditProfileViewModel = koinViewModel()
                EditProfileScreen(
                    component = instance.component,
                    viewModel = editProfileViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
