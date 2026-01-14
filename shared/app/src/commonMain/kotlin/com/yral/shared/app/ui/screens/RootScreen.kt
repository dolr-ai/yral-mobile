package com.yral.shared.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.nav.RootComponent.Child
import com.yral.shared.app.ui.components.UpdateNotificationHost
import com.yral.shared.app.ui.screens.alertsrequest.AlertsRequestBottomSheet
import com.yral.shared.app.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.shared.app.ui.screens.home.HomeScreen
import com.yral.shared.app.ui.screens.tournament.TournamentGameScaffoldScreen
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.getKey
import com.yral.shared.features.aibot.ui.description.AIBotDescriptionScreen
import com.yral.shared.features.aibot.ui.description.AIBotDescriptionUiState
import com.yral.shared.features.aibot.ui.introduction.AIBotIntroductionScreen
import com.yral.shared.features.aibot.ui.introduction.AIBotIntroductionUiState
import com.yral.shared.features.aibot.ui.name.AIBotNameScreen
import com.yral.shared.features.aibot.ui.name.AIBotNameUiState
import com.yral.shared.features.aibot.ui.personality.AIBotPersonalityOption
import com.yral.shared.features.aibot.ui.personality.AIBotPersonalityScreen
import com.yral.shared.features.aibot.ui.personality.AIBotPersonalityUiState
import com.yral.shared.features.auth.ui.LoginBottomSheet
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.features.chat.ui.conversation.ChatConversationScreen
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.features.leaderboard.ui.LeaderboardScreen
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.features.profile.ui.EditProfileScreen
import com.yral.shared.features.profile.ui.ProfileMainScreen
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.root.viewmodels.NavigationTarget
import com.yral.shared.features.root.viewmodels.RootError
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.features.tournament.ui.TournamentLeaderboardScreen
import com.yral.shared.features.wallet.ui.WalletScreen
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.toast.ToastHost
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.error_retry
import yral_mobile.shared.app.generated.resources.error_timeout
import yral_mobile.shared.app.generated.resources.error_timeout_title

private enum class TempAIBotStep { Description, Personality, Name, Introduction }
private const val TEMP_ROLE_LIMIT = 3
private const val TEMP_TRAIT_LIMIT = 5

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(
    rootComponent: RootComponent,
    viewModel: RootViewModel = koinViewModel(),
) {
    // TEMP: Inline flow for AIBot screens (Description -> Personality) for UI testing with sample data.
    var currentStep by remember { mutableStateOf(TempAIBotStep.Description) }

    // Sample data mimicking backend fetch.
    val tempSuggestions =
        remember {
            listOf(
                "A football AI that can talk strategy and banter",
                "A bookish AI to help curate your reading list",
                "A travel AI that plans witty itineraries",
            )
        }
    var tempDescription by remember { mutableStateOf("") }

    var selectedRoles by remember { mutableStateOf(setOf<String>()) }
    var selectedTraits by remember { mutableStateOf(setOf<String>()) }
    var roleOptions by remember {
        mutableStateOf(
            listOf(
                AIBotPersonalityOption("roleplay", "Roleplaying character"),
                AIBotPersonalityOption("friend", "Friend"),
                AIBotPersonalityOption("helper", "Helper"),
                AIBotPersonalityOption("random", "Random"),
                AIBotPersonalityOption("coach", "Coach"),
            ),
        )
    }
    var traitOptions by remember {
        mutableStateOf(
            listOf(
                AIBotPersonalityOption("playful", "Playful"),
                AIBotPersonalityOption("sassy", "Sassy"),
                AIBotPersonalityOption("confident", "Confident"),
                AIBotPersonalityOption("sarcastic", "Sarcastic"),
                AIBotPersonalityOption("empathetic", "Empathetic"),
                AIBotPersonalityOption("curious", "Curious"),
                AIBotPersonalityOption("witty", "Witty"),
            ),
        )
    }
    var tempName by remember { mutableStateOf("") }

    var tempIntroduction by remember { mutableStateOf("") }

    when (currentStep) {
        TempAIBotStep.Description ->
            AIBotDescriptionScreen(
                state =
                    AIBotDescriptionUiState(
                        description = tempDescription,
                        suggestions = tempSuggestions,
                    ),
                onBackClick = { tempDescription = "" },
                onDescriptionChange = { tempDescription = it },
                onSuggestionClick = { tempDescription = it },
                onNextClick = { currentStep = TempAIBotStep.Personality },
            )

        TempAIBotStep.Personality ->
            AIBotPersonalityScreen(
                state =
                    AIBotPersonalityUiState(
                        selectedRoles = selectedRoles,
                        selectedTraits = selectedTraits,
                        roleOptions = roleOptions,
                        traitOptions = traitOptions,
                    ),
                onBackClick = { currentStep = TempAIBotStep.Description },
                onRoleToggle = { id ->
                    selectedRoles =
                        if (selectedRoles.contains(id)) {
                            selectedRoles - id
                        } else {
                            (selectedRoles + id).take(TEMP_ROLE_LIMIT).toSet()
                        }
                },
                onTraitToggle = { id ->
                    selectedTraits =
                        if (selectedTraits.contains(id)) {
                            selectedTraits - id
                        } else {
                            (selectedTraits + id).take(TEMP_TRAIT_LIMIT).toSet()
                        }
                },
                onAddCustomRole = { label ->
                    val id = "custom_role_${roleOptions.size}"
                    roleOptions = roleOptions + AIBotPersonalityOption(id, label)
                    selectedRoles = (selectedRoles + id).take(TEMP_ROLE_LIMIT).toSet()
                },
                onAddCustomTrait = { label ->
                    val id = "custom_trait_${traitOptions.size}"
                    traitOptions = traitOptions + AIBotPersonalityOption(id, label)
                    selectedTraits = (selectedTraits + id).take(TEMP_TRAIT_LIMIT).toSet()
                },
                onNextClick = { currentStep = TempAIBotStep.Name },
            )

        TempAIBotStep.Name ->
            AIBotNameScreen(
                state =
                    AIBotNameUiState(
                        name = tempName,
                        suggestions = listOf("Sassy Byte", "Mood Swing", "Jester AI"),
                    ),
                onBackClick = { currentStep = TempAIBotStep.Personality },
                onNameChange = { tempName = it },
                onSuggestionClick = { tempName = it },
                onNextClick = {
                    currentStep = TempAIBotStep.Introduction
                },
            )

        TempAIBotStep.Introduction ->
            AIBotIntroductionScreen(
                state =
                    AIBotIntroductionUiState(
                        introduction = tempIntroduction,
                        suggestions =
                            listOf(
                                "Sarcasm mode activated, ready to roast and toast!",
                                "Ready to slay the conversation game with witty comebacks",
                                "Warm welcomes and quick quips—meet your new AI buddy",
                                "Hey there, I’m here to keep chats clever and fun",
                            ),
                    ),
                onBackClick = { currentStep = TempAIBotStep.Name },
                onIntroductionChange = { tempIntroduction = it },
                onSuggestionClick = { tempIntroduction = it },
                onNextClick = {
                    currentStep = TempAIBotStep.Description // placeholder: loop for now
                },
            )
    }
    return

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.navigationTarget) {
        when (state.navigationTarget) {
            is NavigationTarget.Splash -> rootComponent.navigateToSplash()
            is NavigationTarget.MandatoryLogin -> rootComponent.navigateToMandatoryLogin()
            is NavigationTarget.Home -> rootComponent.navigateToHome()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Children(
                stack = rootComponent.stack,
                modifier = Modifier.fillMaxSize(),
                animation =
                    stackAnimation { child ->
                        when (child.instance) {
                            is Child.Splash -> fade()
                            is Child.Home -> fade()
                            is Child.MandatoryLogin -> fade()
                            else -> fade() + slide()
                        }
                    },
            ) {
                when (val child = it.instance) {
                    is Child.Splash -> {
                        HandleSystemBars(show = false)
                        Splash(
                            modifier = Modifier.fillMaxSize(),
                            initialAnimationComplete = state.initialAnimationComplete,
                            onAnimationComplete = { viewModel.onSplashAnimationComplete() },
                            onScreenViewed = { viewModel.splashScreenViewed() },
                        )
                    }

                    is Child.Home -> {
                        HandleSystemBars(show = true)
                        HomeScreen(
                            component = child.component,
                            sessionState = state.sessionState,
                            bottomNavigationAnalytics = { viewModel.bottomNavigationClicked(it) },
                            updateProfileVideosCount = { viewModel.updateProfileVideosCount(it) },
                            isPendingLogin = viewModel.isPendingLogin(),
                        )
                    }

                    is Child.EditProfile -> {
                        val sessionKey = state.sessionState.getKey()
                        HandleSystemBars(show = true)
                        EditProfileScreen(
                            component = child.component,
                            viewModel = koinViewModel<EditProfileViewModel>(key = "edit-profile-$sessionKey"),
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.UserProfile -> {
                        HandleSystemBars(show = true)
                        val profileViewModel =
                            koinViewModel<ProfileViewModel>(
                                key = "profile-${child.component.userCanisterData?.userPrincipalId}",
                            ) { parametersOf(child.component.userCanisterData) }
                        val profileVideos =
                            profileViewModel.profileVideos.collectAsLazyPagingItems()
                        ProfileMainScreen(
                            component = child.component,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                            viewModel = profileViewModel,
                            profileVideos = profileVideos,
                            getPrefetchListener = { reel -> PrefetchVideoListenerImpl(reel) },
                        )
                    }

                    is Child.TournamentLeaderboard -> {
                        HandleSystemBars(show = true)
                        TournamentLeaderboardScreen(
                            tournamentId = child.tournamentId,
                            tournamentTitle = "",
                            showResult = child.showResult,
                            onBack = rootComponent::onBackClicked,
                            onOpenProfile = rootComponent::openProfile,
                        )
                    }

                    is Child.TournamentGame -> {
                        val sessionKey = state.sessionState.getKey()
                        HandleSystemBars(show = true)
                        TournamentGameScaffoldScreen(
                            component = child.component,
                            sessionKey = sessionKey,
                        )
                    }

                    is Child.Conversation -> {
                        HandleSystemBars(show = true)
                        ChatConversationScreen(
                            component = child.component,
                            viewModel = koinViewModel<ConversationViewModel>(),
                            modifier = Modifier.fillMaxSize().statusBarsPadding(),
                            bottomPadding = 0.dp,
                        )
                    }

                    is Child.Wallet -> {
                        HandleSystemBars(show = true)
                        WalletScreen(
                            component = child.component,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.Leaderboard -> {
                        HandleSystemBars(show = true)
                        val sessionKey = state.sessionState.getKey()
                        val leaderBoardViewModel = koinViewModel<LeaderBoardViewModel>(key = "leaderboard-$sessionKey")
                        LeaderboardScreen(
                            component = child.component,
                            leaderBoardViewModel = leaderBoardViewModel,
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        )
                    }

                    is Child.MandatoryLogin -> {
                        HandleSystemBars(show = false)
                        MandatoryLoginScreen(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // shows login error for both splash and account screen
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            LaunchedEffect(state.error) {
                if (state.error == null) {
                    sheetState.hide()
                }
            }
            state.error?.let { error ->
                YralErrorMessage(
                    title = stringResource(Res.string.error_timeout_title),
                    error = error.toErrorMessage(),
                    sheetState = sheetState,
                    cta = stringResource(Res.string.error_retry),
                    onDismiss = { viewModel.initialize() },
                    onClick = { viewModel.initialize() },
                )
            }
            // when session is loading & splash is not visible
            // 1. after logout on account screen during anonymous sign in
            // 2. after social sign in
            // 3. after delete account during anonymous sign in
            if (state.navigationTarget !is NavigationTarget.Splash && state.sessionState is SessionState.Loading) {
                BlockingLoader()
            }

            if (!rootComponent.isSplashActive()) {
                ToastHost(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .statusBarsPadding()
                            .padding(top = 12.dp),
                )
            }

            // Show update notifications (Snackbar) for flexible updates
            UpdateNotificationHost(
                rootComponent = rootComponent,
            )

            SlotContent(rootComponent)
        }
    }
}

@Composable
private fun BlockingLoader() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        YralLoader()
    }
}

@Composable
internal expect fun HandleSystemBars(show: Boolean)

@Composable
private fun Splash(
    modifier: Modifier,
    initialAnimationComplete: Boolean,
    onAnimationComplete: () -> Unit = {},
    onScreenViewed: () -> Unit,
) {
    LaunchedEffect(Unit) { onScreenViewed() }
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        // Crossfade between animations
        AnimatedVisibility(
            visible = !initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = LottieRes.SPLASH,
                iterations = 1,
                contentScale = ContentScale.Crop,
                onAnimationComplete = onAnimationComplete,
            )
        }

        AnimatedVisibility(
            visible = initialAnimationComplete,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = LottieRes.LIGHTNING,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun RootError.toErrorMessage(): String =
    stringResource(
        when (this) {
            RootError.TIMEOUT -> Res.string.error_timeout
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotContent(component: RootComponent) {
    val slot by component.slot.subscribeAsState()
    slot.child?.instance?.also { slotChild ->
        when (slotChild) {
            is RootComponent.SlotChild.AlertsRequestBottomSheet -> {
                AlertsRequestBottomSheet(component = slotChild.component)
            }
            is RootComponent.SlotChild.LoginBottomSheet -> {
                val loginViewModel: LoginViewModel = koinViewModel()
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val termsSheetState = rememberModalBottomSheetState()
                var termsLink by remember { mutableStateOf("") }
                LoginBottomSheet(
                    pageName = slotChild.pageName,
                    bottomSheetState = bottomSheetState,
                    onDismissRequest = slotChild.onDismissRequest,
                    onLoginSuccess = slotChild.onLoginSuccess,
                    openTerms = { termsLink = it },
                    loginViewModel = loginViewModel,
                    bottomSheetType = slotChild.loginBottomSheetType,
                )
                if (termsLink.isNotEmpty()) {
                    YralWebViewBottomSheet(
                        link = termsLink,
                        bottomSheetState = termsSheetState,
                        onDismissRequest = { termsLink = "" },
                    )
                }
            }
        }
    }
}
