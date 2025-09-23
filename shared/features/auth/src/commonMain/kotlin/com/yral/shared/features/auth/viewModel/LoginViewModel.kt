package com.yral.shared.features.auth.viewModel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)

    private val _state = MutableStateFlow<UiState<Unit>>(UiState.Initial)
    val state = _state.asStateFlow()

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                _state.value = UiState.Failure(e)
            }

    @Suppress("TooGenericExceptionCaught")
    fun signInWithGoogle(context: Any) {
        coroutineScope.launch {
            try {
                _state.value = UiState.InProgress()
                authClient.signInWithSocial(context, SocialProvider.GOOGLE)
                _state.value = UiState.Success(Unit)
            } catch (e: Exception) {
                crashlyticsManager.recordException(e)
                _state.value = UiState.Failure(e)
            }
        }
    }

    fun sheetDismissed() {
        _state.value = UiState.Initial
    }
}
