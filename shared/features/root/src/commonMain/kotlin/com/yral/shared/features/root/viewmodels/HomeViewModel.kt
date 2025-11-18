package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val flagManager: FeatureFlagManager,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignedIn ->
                    _state.update { it.copy(isSocialSignedIn = isSocialSignedIn) }
                }
        }
    }

    fun onSignupPromptShown(pageName: SignupPageName) {
        _state.update {
            it.copy(hasShownSignupPrompt = _state.value.hasShownSignupPrompt + (pageName to true))
        }
    }

    fun showSignupPrompt(
        show: Boolean,
        pageName: SignupPageName?,
    ) {
        _state.update { it.copy(showSignupPrompt = show, pageName = pageName) }
    }

    fun getTncLink(): String = flagManager.get(AccountFeatureFlags.AccountLinks.Links).tnc
}

data class HomeState(
    val hasShownSignupPrompt: Map<SignupPageName, Boolean> = emptyMap(),
    val showSignupPrompt: Boolean = false,
    val pageName: SignupPageName? = null,
    val isSocialSignedIn: Boolean = false,
)
