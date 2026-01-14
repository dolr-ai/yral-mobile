package com.yral.shared.features.auth.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.YralAuthException
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.libs.phonevalidation.DeviceLocaleDetector
import com.yral.shared.libs.phonevalidation.PhoneNumberFormat
import com.yral.shared.libs.phonevalidation.PhoneValidator
import com.yral.shared.libs.phonevalidation.countries.Country
import com.yral.shared.libs.phonevalidation.countries.CountryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val crashlyticsManager: CrashlyticsManager,
    private val flagManager: FeatureFlagManager,
    private val phoneValidator: PhoneValidator,
    private val deviceLocaleDetector: DeviceLocaleDetector,
    private val countryRepository: CountryRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.main)

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var resendTimerJob: Job? = null

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                _state.update {
                    it.copy(
                        socialAuthState =
                            when (it.socialAuthState) {
                                is UiState.InProgress -> UiState.Failure(e)
                                else -> it.socialAuthState
                            },
                        otpAuthState =
                            when (it.otpAuthState) {
                                is UiState.InProgress -> UiState.Failure(e)
                                else -> it.otpAuthState
                            },
                    )
                }
            }

    companion object {
        const val OTP_RESEND_TIMER_SECONDS = 15
        private const val TIMER_DELAY_MILLIS = 1000L
    }

    init {
        // Auto-detect user's country from device locale (no permissions needed)
        val deviceRegion = deviceLocaleDetector.getDeviceRegionCode()
        val detectedCountry =
            deviceRegion
                ?.let { countryRepository.getCountryByCode(it) }
                ?: countryRepository.getCountryByCode("US") // Fallback to US

        _state.update { it.copy(selectedCountry = detectedCountry) }
        Logger.d("LoginViewModel") { "Auto-detected country: ${detectedCountry?.name} (${detectedCountry?.code})" }

        viewModelScope.launch {
            sessionManager
                .observeSessionStateWithProperty { state, properties ->
                    state to properties.isSocialSignIn
                }.distinctUntilChanged()
                .collect { value ->
                    if (value.first is SessionState.SignedIn && value.second == true) {
                        _state.update {
                            it.copy(
                                socialAuthState =
                                    when (it.socialAuthState) {
                                        is UiState.InProgress -> UiState.Success(Unit)
                                        else -> it.socialAuthState
                                    },
                                otpAuthState =
                                    when (it.otpAuthState) {
                                        is UiState.InProgress -> UiState.Success(Unit)
                                        else -> it.otpAuthState
                                    },
                            )
                        }
                    }
                }
        }
    }

    fun signInWithSocial(
        context: Any,
        provider: SocialProvider,
    ) {
        coroutineScope.launch {
            try {
                _state.update { it.copy(socialAuthState = UiState.InProgress()) }
                authClient.signInWithSocial(context, provider)
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
                _state.update { it.copy(socialAuthState = UiState.Failure(e)) }
            }
        }
    }

    fun resetState() {
        resendTimerJob?.cancel()
        _state.update {
            it.copy(
                socialAuthState = UiState.Initial,
                phoneAuthState = UiState.Initial,
                otpAuthState = UiState.Initial,
                phoneValidationError = null,
                otpCode = "",
                otpValidationError = null,
                resendTimerSeconds = null,
            )
        }
    }

    fun onCountrySelected(country: Country) {
        _state.update { it.copy(selectedCountry = country) }
        validatePhoneNumber()
    }

    fun onPhoneNumberChanged(number: String) {
        _state.update {
            it.copy(
                phoneNumber = number,
                phoneValidationError = null,
            )
        }
    }

    private fun validatePhoneNumber() {
        val currentState = _state.value
        val country = currentState.selectedCountry ?: return
        val number = currentState.phoneNumber
        when {
            number.isBlank() -> _state.update { it.copy(phoneValidationError = null) }
            else -> {
                val isValid = phoneValidator.isValid(number, country.code)
                if (isValid) {
                    _state.update { it.copy(phoneValidationError = null) }
                } else {
                    _state.update { it.copy(phoneValidationError = "Invalid phone number") }
                }
            }
        }
    }

    @Suppress("ReturnCount")
    fun onPhoneLoginClicked() {
        val currentState = _state.value
        val country = currentState.selectedCountry
        val number = currentState.phoneNumber

        if (country == null) {
            _state.update { it.copy(phoneValidationError = "Please select a country") }
            return
        }

        if (number.isBlank()) {
            _state.update { it.copy(phoneValidationError = "Please enter a phone number") }
            return
        }

        // Validate phone number
        if (!phoneValidator.isValid(number, country.code)) {
            _state.update { it.copy(phoneValidationError = "Invalid phone number format") }
            return
        }

        // Format to E.164 format
        val formattedNumber =
            phoneValidator.format(
                number,
                country.code,
                PhoneNumberFormat.E164,
            )

        _state.update {
            it.copy(
                phoneAuthState = UiState.InProgress(),
                phoneValidationError = null,
            )
        }
        Logger.d("LoginViewModel") { "Initiating phone auth for: $formattedNumber" }
        coroutineScope.launch {
            try {
                authClient.phoneAuthLogin(formattedNumber)
                Logger.d("LoginViewModel") { "Phone auth successful for: $formattedNumber" }
                _state.update {
                    it.copy(
                        phoneAuthState = UiState.Success(PhoneAuthData(phoneNumber = formattedNumber)),
                    )
                }
                // Start resend timer
                startResendTimer()
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                Logger.e("LoginViewModel", e) { "Phone auth failed" }
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
                _state.update {
                    it.copy(
                        phoneAuthState = UiState.Failure(e),
                        phoneValidationError = "Failed to send verification code",
                    )
                }
            }
        }
    }

    private fun startResendTimer() {
        resendTimerJob?.cancel()
        resendTimerJob =
            coroutineScope.launch {
                for (seconds in OTP_RESEND_TIMER_SECONDS downTo 0) {
                    _state.update { it.copy(resendTimerSeconds = seconds) }
                    if (seconds > 0) {
                        delay(TIMER_DELAY_MILLIS)
                    }
                }
                _state.update { it.copy(resendTimerSeconds = null) }
            }
    }

    fun onResendOtp() {
        val currentState = _state.value
        val phoneAuthData = (currentState.phoneAuthState as? UiState.Success)?.data

        if (phoneAuthData == null || currentState.resendTimerSeconds != null) {
            return
        }
        _state.update { it.copy(otpValidationError = null) }
        Logger.d("LoginViewModel") { "Resending OTP for: ${phoneAuthData.phoneNumber}" }
        coroutineScope.launch {
            try {
                authClient.phoneAuthLogin(phoneAuthData.phoneNumber)
                Logger.d("LoginViewModel") { "OTP resent successfully" }
                startResendTimer()
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                Logger.e("LoginViewModel", e) { "Failed to resend OTP" }
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
                _state.update { it.copy(otpValidationError = "Failed to resend OTP") }
            }
        }
    }

    fun onOtpCodeChanged(code: String) {
        _state.update {
            it.copy(
                otpCode = code,
                otpValidationError = null,
            )
        }
    }

    @Suppress("ReturnCount")
    fun onVerifyOtpClicked() {
        val currentState = _state.value
        val otpCode = currentState.otpCode
        val phoneAuthData = (currentState.phoneAuthState as? UiState.Success)?.data

        if (phoneAuthData == null || otpCode.isBlank()) {
            _state.update { it.copy(otpValidationError = "Phone authentication data not found") }
            return
        }

        if (otpCode.isBlank()) {
            _state.update { it.copy(otpValidationError = "Please enter OTP code") }
            return
        }

        _state.update {
            it.copy(
                otpAuthState = UiState.InProgress(),
                otpValidationError = null,
            )
        }
        Logger.d("LoginViewModel") { "Verifying OTP for: ${phoneAuthData.phoneNumber}" }
        coroutineScope.launch {
            try {
                authClient.verifyPhoneAuth(
                    phoneNumber = phoneAuthData.phoneNumber,
                    code = otpCode,
                )
                Logger.d("LoginViewModel") { "OTP verification successful" }
            } catch (e: YralAuthException) {
                Logger.e("LoginViewModel", e) { "OTP verification failed" }
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
                _state.update {
                    it.copy(
                        otpAuthState = UiState.Failure(e),
                        otpValidationError = "Failed to verify OTP code",
                    )
                }
            }
        }
    }

    fun getTncLink(): String = flagManager.get(AccountFeatureFlags.AccountLinks.Links).tnc

    fun getInitialBalanceReward(): Int = flagManager.get(AppFeatureFlags.Common.InitialBalanceReward)

    fun getAllCountries(): List<Country> = countryRepository.getAllCountries()

    fun searchCountries(query: String): List<Country> = countryRepository.searchCountries(query)
}

data class LoginState(
    val socialAuthState: UiState<Unit> = UiState.Initial,
    val phoneAuthState: UiState<PhoneAuthData> = UiState.Initial,
    val otpAuthState: UiState<Unit> = UiState.Initial,
    val selectedCountry: Country? = null,
    val phoneNumber: String = "",
    val phoneValidationError: String? = null,
    val otpCode: String = "",
    val otpValidationError: String? = null,
    val resendTimerSeconds: Int? = null,
) {
    fun isLoginComplete() = socialAuthState is UiState.Success || otpAuthState is UiState.Success
}

data class PhoneAuthData(
    val phoneNumber: String,
)
