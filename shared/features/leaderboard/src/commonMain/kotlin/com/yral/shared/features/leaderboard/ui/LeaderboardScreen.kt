package com.yral.shared.features.leaderboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.leaderboard.ui.history.LeaderboardDetailsScreen
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreen
import com.yral.shared.features.leaderboard.viewmodel.BottomSheetType
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    component: LeaderboardComponent,
    leaderBoardViewModel: LeaderBoardViewModel,
    modifier: Modifier = Modifier,
    loginState: UiState<*>,
    loginBottomSheet: LoginBottomSheetComposable,
) {
    val viewState by leaderBoardViewModel.state.collectAsStateWithLifecycle()
    val loginSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extraSheetState = rememberModalBottomSheetState()
    var extraSheetLink by remember { mutableStateOf("") }
    val tncLink = remember { leaderBoardViewModel.getTncLink() }

    LaunchedEffect(viewState.isSocialSignedIn) {
        if (viewState.isSocialSignedIn) {
            leaderBoardViewModel.setBottomSheetType(BottomSheetType.None)
            leaderBoardViewModel.setSignupPromptShown(false)
        } else if (!viewState.hasShownSignupPrompt) {
            leaderBoardViewModel.setSignupPromptShown(true)
            leaderBoardViewModel.setBottomSheetType(BottomSheetType.Signup)
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is UiState.Failure) {
            leaderBoardViewModel.setBottomSheetType(BottomSheetType.Signup)
            leaderBoardViewModel.setSignupPromptShown(true)
        }
    }

    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
        modifier = modifier,
    ) { child ->
        when (val instance = child.instance) {
            is LeaderboardComponent.Child.Main -> {
                LeaderboardMainScreen(
                    component = instance.component,
                    viewModel = leaderBoardViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is LeaderboardComponent.Child.Details -> {
                LeaderboardDetailsScreen(component = instance.component)
            }
        }
    }

    when (viewState.bottomSheetType) {
        BottomSheetType.Signup -> {
            loginBottomSheet(
                SignupPageName.LEADERBOARD,
                loginSheetState,
                { leaderBoardViewModel.setBottomSheetType(BottomSheetType.None) },
                tncLink,
                { extraSheetLink = tncLink },
            )
        }
        BottomSheetType.None -> Unit
    }

    if (extraSheetLink.isNotEmpty()) {
        YralWebViewBottomSheet(
            link = extraSheetLink,
            bottomSheetState = extraSheetState,
            onDismissRequest = { extraSheetLink = "" },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
typealias LoginBottomSheetComposable = @Composable (
    pageName: SignupPageName,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    termsLink: String,
    openTerms: () -> Unit,
) -> Unit
