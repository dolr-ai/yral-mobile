package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.auth.utils.OAuthListener
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.propicFromPrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountsViewModel(
    appDispatchers: AppDispatchers,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val authClient: AuthClient,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)
    private val _state =
        MutableStateFlow(
            AccountsState(
                accountInfo = getAccountInfo(),
                isSocialSignInSuccessful = false,
            ),
        )
    val state: StateFlow<AccountsState> = _state.asStateFlow()
    val sessionState = sessionManager.state

    init {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    isSocialSignInSuccessful = isSocialSignInSuccessful(),
                ),
            )
        }
    }

    fun refreshAccountInfo() {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    accountInfo = getAccountInfo(),
                    isSocialSignInSuccessful = isSocialSignInSuccessful(),
                    bottomSheetType = AccountBottomSheet.None,
                ),
            )
        }
    }

    private fun getAccountInfo(): AccountInfo? {
        val canisterPrincipal = sessionManager.getCanisterPrincipal()
        val userPrincipal = sessionManager.getUserPrincipal()
        canisterPrincipal?.let { principal ->
            userPrincipal?.let { userPrincipal ->
                return AccountInfo(
                    profilePic = propicFromPrincipal(principal),
                    userPrincipal = userPrincipal,
                )
            }
        }
        return null
    }

    fun logout() {
        coroutineScope.launch {
            authClient.logout()
        }
    }

    fun signInWithGoogle() {
        coroutineScope
            .launch {
                try {
                    authClient.signInWithSocial(
                        provider = SocialProvider.GOOGLE,
                        oAuthListener =
                            object : OAuthListener {
                                override fun setLoading(loading: Boolean) {
                                    showLoading(loading)
                                }

                                override fun exception(e: YralException) {
                                    showLoading(false)
                                    setBottomSheetType(type = AccountBottomSheet.SignUpFailed)
                                }
                            },
                    )
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    crashlyticsManager.recordException(e)
                    showLoading(false)
                    setBottomSheetType(type = AccountBottomSheet.SignUpFailed)
                }
            }
    }

    private suspend fun isSocialSignInSuccessful(): Boolean =
        preferences
            .getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false

    fun showLoading(loading: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    isLoading = loading,
                ),
            )
        }
    }

    fun setBottomSheetType(type: AccountBottomSheet) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    bottomSheetType = type,
                ),
            )
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
        if (_state.value.isSocialSignInSuccessful) {
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

    fun getSocialLinks(): List<AccountHelpLink> {
        val links =
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
        return links
    }

    companion object {
        const val LOGOUT_URI = "yral://logout"
        const val DELETE_ACCOUNT_URI = "yral://deleteAccount"
        const val TALK_TO_TEAM_URL = "https://t.me/+c-LTX0Cp-ENmMzI1"
        const val TERMS_OF_SERVICE_URL = "https://yral.com/terms-ios"
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
    val accountInfo: AccountInfo?,
    val isSocialSignInSuccessful: Boolean,
    val isLoading: Boolean = false,
    val bottomSheetType: AccountBottomSheet = AccountBottomSheet.None,
)

sealed interface AccountBottomSheet {
    data object None : AccountBottomSheet
    data class ShowWebView(
        val linkToOpen: Pair<String, Boolean>,
    ) : AccountBottomSheet

    data object SignUpFailed : AccountBottomSheet
    data object SingUp : AccountBottomSheet
    data object DeleteAccount : AccountBottomSheet
}

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
)
