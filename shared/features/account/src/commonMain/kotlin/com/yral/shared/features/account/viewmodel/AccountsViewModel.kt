package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.domain.useCases.DeleteAccountUseCase
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountsViewModel(
    appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                handleSignupFailed()
            }

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()
    val sessionState = sessionManager.state

    init {
        coroutineScope.launch {
            val isSignedIn = isSocialSignInSuccessful()
            _state.update { currentState ->
                currentState.copy(
                    isSocialSignInSuccessful = isSignedIn,
                )
            }
        }
    }

    fun refreshAccountInfo() {
        coroutineScope.launch {
            val accountInfo = sessionManager.getAccountInfo()
            val isSignedIn = isSocialSignInSuccessful()
            _state.update { currentState ->
                currentState.copy(
                    accountInfo = accountInfo,
                    isSocialSignInSuccessful = isSignedIn,
                    bottomSheetType = AccountBottomSheet.None,
                )
            }
        }
    }

    private fun logout() {
        coroutineScope.launch {
            authClient.logout()
        }
    }

    fun deleteAccount() {
        coroutineScope.launch {
            deleteAccountUseCase
                .invoke(Unit)
                .onSuccess {
                    Logger.d("Delete account $it")
                    logout()
                }.onFailure {
                    Logger.e("Failed to delete account: ${it.message}")
                    setBottomSheetType(
                        type = AccountBottomSheet.ErrorMessage(ErrorType.DELETE_ACCOUNT_FAILED),
                    )
                }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun signInWithGoogle() {
        coroutineScope
            .launch {
                try {
                    authClient.signInWithSocial(SocialProvider.GOOGLE)
                } catch (e: Exception) {
                    crashlyticsManager.logMessage("sign in with google exception caught")
                    crashlyticsManager.recordException(e)
                    handleSignupFailed()
                }
            }
    }

    private fun handleSignupFailed() {
        setBottomSheetType(
            type = AccountBottomSheet.ErrorMessage(ErrorType.SIGNUP_FAILED),
        )
    }

    private suspend fun isSocialSignInSuccessful(): Boolean =
        preferences
            .getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false

    fun setBottomSheetType(type: AccountBottomSheet) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    bottomSheetType = type,
                )
            }
        }
    }

    fun getHelperLinks(): List<AccountHelpLink> {
        val links =
            mutableListOf(
                AccountHelpLink(
                    link = TALK_TO_TEAM_URL,
                    openInExternalBrowser = false,
                ),
                AccountHelpLink(
                    link = TERMS_OF_SERVICE_URL,
                    openInExternalBrowser = false,
                ),
                AccountHelpLink(
                    link = PRIVACY_POLICY_URL,
                    openInExternalBrowser = false,
                ),
            )
        val isSocialSignIn = _state.value.isSocialSignInSuccessful
        if (isSocialSignIn) {
            links.add(
                AccountHelpLink(
                    link = LOGOUT_URI,
                    openInExternalBrowser = true,
                ),
            )
            links.add(
                AccountHelpLink(
                    link = DELETE_ACCOUNT_URI,
                    openInExternalBrowser = true,
                ),
            )
        }
        return links
    }

    fun getSocialLinks(): List<AccountHelpLink> =
        listOf(
            AccountHelpLink(
                link = TELEGRAM_LINK,
                openInExternalBrowser = true,
            ),
            AccountHelpLink(
                link = DISCORD_LINK,
                openInExternalBrowser = true,
            ),
            AccountHelpLink(
                link = TWITTER_LINK,
                openInExternalBrowser = true,
            ),
        )

    fun handleHelpLink(
        link: String,
        shouldOpenOutside: Boolean,
    ) {
        when (link) {
            LOGOUT_URI -> logout()
            DELETE_ACCOUNT_URI -> setBottomSheetType(AccountBottomSheet.DeleteAccount)
            else ->
                setBottomSheetType(
                    AccountBottomSheet.ShowWebView(
                        linkToOpen = Pair(link, shouldOpenOutside),
                    ),
                )
        }
    }

    companion object {
        const val LOGOUT_URI = "yral://logout"
        const val DELETE_ACCOUNT_URI = "yral://deleteAccount"
        const val TALK_TO_TEAM_URL = "https://t.me/+c-LTX0Cp-ENmMzI1"
        const val TERMS_OF_SERVICE_URL = "https://yral.com/terms-android"
        const val PRIVACY_POLICY_URL = "https://yral.com/privacy-policy"
        const val TELEGRAM_LINK = "https://t.me/+c-LTX0Cp-ENmMzI1"
        const val DISCORD_LINK = "https://discord.com/invite/GZ9QemnZuj"
        const val TWITTER_LINK = "https://twitter.com/Yral_app"
    }
}

data class AccountHelpLink(
    val link: String,
    val openInExternalBrowser: Boolean,
)

data class AccountsState(
    val accountInfo: AccountInfo? = null,
    val isSocialSignInSuccessful: Boolean = false,
    val bottomSheetType: AccountBottomSheet = AccountBottomSheet.None,
)

sealed interface AccountBottomSheet {
    data object None : AccountBottomSheet
    data class ShowWebView(
        val linkToOpen: Pair<String, Boolean>,
    ) : AccountBottomSheet

    data object SignUp : AccountBottomSheet
    data object DeleteAccount : AccountBottomSheet
    data class ErrorMessage(
        val errorType: ErrorType,
    ) : AccountBottomSheet
}

enum class ErrorType {
    SIGNUP_FAILED,
    DELETE_ACCOUNT_FAILED,
}
