package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.featureflag.accountFeatureFlags.AccountLinksDto
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.utils.getAccountInfo
import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.domain.useCases.DeleteAccountUseCase
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.firebaseStore.getDownloadUrl
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import dev.gitlive.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class AccountsViewModel internal constructor(
    private val appDispatchers: AppDispatchers,
    private val authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    val accountsTelemetry: AccountsTelemetry,
    private val flagManager: FeatureFlagManager,
    private val firebaseStorage: FirebaseStorage,
    private val preferences: Preferences,
    private val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase,
    private val deregisterNotificationTokenUseCase: DeregisterNotificationTokenUseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                handleSignupFailed()
            }

    private val _state =
        MutableStateFlow(
            value =
                AccountsState(
                    accountInfo = sessionManager.getAccountInfo(),
                    accountLinks = flagManager.get(AccountFeatureFlags.AccountLinks.Links),
                    isWalletEnabled = flagManager.isEnabled(WalletFeatureFlags.Wallet.Enabled),
                    isSubscriptionEnabled = flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription),
                ),
        )
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            _state.value.accountLinks.supportIcon?.let { supportIcon ->
                getDownloadUrl(supportIcon, firebaseStorage)
                    .takeIf { url -> url.isNotEmpty() }
                    ?.let { iconUrl -> _state.update { it.copy(supportIcon = iconUrl) } }
            }
        }
        coroutineScope.launch {
            val isEnabled = preferences.getBoolean(PrefKeys.NOTIFICATION_ALERTS_ENABLED.name) ?: false
            _state.update { it.copy(alertsEnabled = isEnabled) }
        }
        coroutineScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }
                }
        }
        coroutineScope.launch {
            sessionManager
                .observeSessionState(transform = { sessionManager.getAccountInfo() })
                .collect { info: AccountInfo? -> _state.update { it.copy(accountInfo = info) } }
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

    private fun handleSignupFailed() {
        setBottomSheetType(
            type = AccountBottomSheet.ErrorMessage(ErrorType.SIGNUP_FAILED),
        )
    }

    fun setBottomSheetType(type: AccountBottomSheet) {
        _state.update { it.copy(bottomSheetType = type) }
    }

    fun getHelperLinks(): List<AccountHelpLink> {
        val accountLinks = _state.value.accountLinks
        val links =
            mutableListOf(
                AccountHelpLink(
                    type = AccountHelpLinkType.TALK_TO_TEAM,
                    link = accountLinks.support.trim(),
                    linkText = accountLinks.supportText,
                    linkRemoteIcon = _state.value.supportIcon,
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
        if (_state.value.isLoggedIn) {
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
        val accountLinks = _state.value.accountLinks
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

    fun onAlertsToggleChanged(isEnabled: Boolean) {
        coroutineScope.launch {
            preferences.putBoolean(PrefKeys.NOTIFICATION_ALERTS_ENABLED.name, isEnabled)
        }
        _state.update { it.copy(alertsEnabled = isEnabled) }
    }

    suspend fun registerAlerts(): Boolean =
        runCatching {
            registerNotificationTokenUseCase()
        }.onFailure { error ->
            Logger.e("AccountsViewModel") { "Failed to register notifications: ${error.message}" }
        }.isSuccess

    suspend fun deregisterAlerts(): Boolean =
        runCatching {
            deregisterNotificationTokenUseCase()
        }.onFailure { error ->
            Logger.e("AccountsViewModel") { "Failed to deregister notifications: ${error.message}" }
        }.isSuccess

    companion object {
        const val LOGOUT_URI = "yral://logout"
        const val DELETE_ACCOUNT_URI = "yral://deleteAccount"
    }
}

data class AccountHelpLink(
    val type: AccountHelpLinkType,
    val link: String,
    val linkText: String? = null,
    val linkRemoteIcon: String? = null,
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
    val accountInfo: AccountInfo?,
    val bottomSheetType: AccountBottomSheet = AccountBottomSheet.None,
    val accountLinks: AccountLinksDto,
    val supportIcon: String? = null,
    val isWalletEnabled: Boolean = false,
    val isSubscriptionEnabled: Boolean = false,
    val isLoggedIn: Boolean = false,
    val alertsEnabled: Boolean = false,
)

sealed interface AccountBottomSheet {
    data object None : AccountBottomSheet
    data class ShowWebView(
        val linkToOpen: AccountHelpLink,
    ) : AccountBottomSheet
    data object DeleteAccount : AccountBottomSheet
    data class ErrorMessage(
        val errorType: ErrorType,
    ) : AccountBottomSheet
}

enum class ErrorType {
    SIGNUP_FAILED,
    DELETE_ACCOUNT_FAILED,
}
