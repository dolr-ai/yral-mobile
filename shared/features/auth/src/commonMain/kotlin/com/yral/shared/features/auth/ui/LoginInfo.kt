package com.yral.shared.features.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yral.shared.analytics.events.SignupPageName

/**
 * Information required to request a login screen.
 * Wraps all login request parameters for easier maintenance and extensibility.
 *
 * @param pageName The page/source from which login is initiated (for analytics) - MANDATORY
 * @param screenType The presentation style (OVERLAY or BOTTOM_SHEET(bottomSheetType))
 * @param mode The authentication methods to show (SOCIAL, PHONE, or BOTH)
 * @param onSuccess Optional callback invoked when login succeeds (for both social and phone/OTP)
 * @param onDismiss Optional callback invoked when the login screen is dismissed
 * @param bottomContent Optional bottom content for OVERLAY type (ignored for other types)
 */
data class LoginInfo(
    val pageName: SignupPageName,
    val screenType: LoginScreenType,
    val mode: LoginMode = LoginMode.BOTH,
    val onSuccess: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
    val bottomContent: @Composable () -> Unit = {},
)

/**
 * State holder for managing login info in a composable.
 * Provides a convenient way to manage login state and rendering across multiple screens.
 *
 * @property loginInfo The current login info, or null if no login is requested
 * @property requestLogin Function to request login with the specified parameters
 * @property clearLogin Function to manually clear the login state
 */
data class LoginInfoState(
    val loginInfo: LoginInfo?,
    val requestLogin: (
        pageName: SignupPageName,
        screenType: LoginScreenType,
        mode: LoginMode,
        onSuccess: (() -> Unit)?,
        onDismiss: (() -> Unit)?,
        bottomContent: @Composable () -> Unit,
    ) -> Unit,
    val clearLogin: () -> Unit,
)

/**
 * Remembers and manages login info state for a composable.
 * Automatically handles state cleanup on success/dismiss.
 *
 * @param requestLoginFactory The factory function to create login request composables
 * @param key Optional key to reset state when changed (useful for screen-specific resets)
 * @return [LoginInfoState] containing the current state and helper functions
 */
@Composable
fun rememberLoginInfo(
    requestLoginFactory: RequestLoginFactory,
    key: Any? = null,
): LoginInfoState {
    var loginInfo by remember(key) { mutableStateOf<LoginInfo?>(null) }
    var showAutomatically by remember(key) { mutableStateOf(false) }

    val requestLoginFn: (
        pageName: SignupPageName,
        screenType: LoginScreenType,
        mode: LoginMode,
        onSuccess: (() -> Unit)?,
        onDismiss: (() -> Unit)?,
        bottomContent: @Composable () -> Unit,
    ) -> Unit = { pageName, screenType, mode, onSuccess, onDismiss, bottomContent ->
        loginInfo =
            LoginInfo(
                pageName = pageName,
                screenType = screenType,
                mode = mode,
                onSuccess = {
                    loginInfo = null
                    onSuccess?.invoke()
                },
                onDismiss = {
                    loginInfo = null
                    onDismiss?.invoke()
                },
                bottomContent = bottomContent,
            )
        showAutomatically = screenType is LoginScreenType.BottomSheet
    }

    val clearLogin: () -> Unit = {
        loginInfo = null
    }

    if (showAutomatically) {
        // Automatically render login composable when state is set
        loginInfo?.let { loginInfo ->
            requestLoginFactory(loginInfo)
        }
        SideEffect { showAutomatically = false }
    }

    return LoginInfoState(
        loginInfo = loginInfo,
        requestLogin = requestLoginFn,
        clearLogin = clearLogin,
    )
}
