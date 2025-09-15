package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.domain.useCases.DeleteAccountUseCase
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
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
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val crashlyticsManager: CrashlyticsManager,
    val accountsTelemetry: AccountsTelemetry,
    private val flagManager: FeatureFlagManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                handleSignupFailed()
            }

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
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
    fun signInWithGoogle(context: Any) {
        coroutineScope.launch {
            try {
                authClient.signInWithSocial(context, SocialProvider.GOOGLE)
            } catch (e: Exception) {
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

    fun setBottomSheetType(type: AccountBottomSheet) {
        _state.update { it.copy(bottomSheetType = type) }
    }

    fun getTncLink(): String = flagManager.get(AccountFeatureFlags.AccountLinks.Links).tnc

    fun getHelperLinks(): List<AccountHelpLink> {
        val accountLinks = flagManager.get(AccountFeatureFlags.AccountLinks.Links)
        val links =
            mutableListOf(
                AccountHelpLink(
                    type = AccountHelpLinkType.TALK_TO_TEAM,
                    link = accountLinks.support,
                    linkText = accountLinks.supportText,
                    openInExternalBrowser = true,
                    menuCtaType = MenuCtaType.TALK_TO_THE_TEAM,
                ),
                AccountHelpLink(
                    type = AccountHelpLinkType.TERMS_OF_SERVICE,
                    link = accountLinks.tnc,
                    openInExternalBrowser = false,
                    menuCtaType = MenuCtaType.TERMS_OF_SERVICE,
                ),
                AccountHelpLink(
                    type = AccountHelpLinkType.PRIVACY_POLICY,
                    link = accountLinks.privacyPolicy,
                    openInExternalBrowser = false,
                    menuCtaType = MenuCtaType.PRIVACY_POLICY,
                ),
            )
        val isSocialSignIn = isLoggedIn()
        if (isSocialSignIn) {
            links.add(
                AccountHelpLink(
                    type = AccountHelpLinkType.LOGOUT,
                    link = LOGOUT_URI,
                    openInExternalBrowser = true,
                    menuCtaType = MenuCtaType.LOG_OUT,
                ),
            )
            links.add(
                AccountHelpLink(
                    type = AccountHelpLinkType.DELETE_ACCOUNT,
                    link = DELETE_ACCOUNT_URI,
                    openInExternalBrowser = true,
                    menuCtaType = MenuCtaType.DELETE_ACCOUNT,
                ),
            )
        }
        return links
    }

    fun getSocialLinks(): List<AccountHelpLink> {
        val accountLinks = flagManager.get(AccountFeatureFlags.AccountLinks.Links)
        return listOf(
            AccountHelpLink(
                type = AccountHelpLinkType.TELEGRAM,
                link = accountLinks.telegram,
                openInExternalBrowser = true,
                menuCtaType = MenuCtaType.FOLLOW_ON,
            ),
            AccountHelpLink(
                type = AccountHelpLinkType.DISCORD,
                link = accountLinks.discord,
                openInExternalBrowser = true,
                menuCtaType = MenuCtaType.FOLLOW_ON,
            ),
            AccountHelpLink(
                type = AccountHelpLinkType.TWITTER,
                link = accountLinks.twitter,
                openInExternalBrowser = true,
                menuCtaType = MenuCtaType.FOLLOW_ON,
            ),
        )
    }

    fun handleHelpLink(link: AccountHelpLink) {
        when (link.link) {
            LOGOUT_URI -> logout()
            DELETE_ACCOUNT_URI -> setBottomSheetType(AccountBottomSheet.DeleteAccount)
            else ->
                setBottomSheetType(
                    AccountBottomSheet.ShowWebView(
                        linkToOpen = link,
                    ),
                )
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isSocialSignIn()

    companion object {
        const val LOGOUT_URI = "yral://logout"
        const val DELETE_ACCOUNT_URI = "yral://deleteAccount"
    }
}

data class AccountHelpLink(
    val type: AccountHelpLinkType,
    val link: String,
    val linkText: String? = null,
    val openInExternalBrowser: Boolean,
    val menuCtaType: MenuCtaType,
)

enum class AccountHelpLinkType {
    TALK_TO_TEAM,
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    TELEGRAM,
    DISCORD,
    TWITTER,
    LOGOUT,
    DELETE_ACCOUNT,
}

data class AccountsState(
    val accountInfo: AccountInfo? = null,
    val bottomSheetType: AccountBottomSheet = AccountBottomSheet.None,
)

sealed interface AccountBottomSheet {
    data object None : AccountBottomSheet
    data class ShowWebView(
        val linkToOpen: AccountHelpLink,
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
