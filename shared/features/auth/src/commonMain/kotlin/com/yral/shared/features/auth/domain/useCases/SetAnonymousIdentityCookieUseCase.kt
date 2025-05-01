package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.SuspendUseCase
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.http.CookieType
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences

class SetAnonymousIdentityCookieUseCase(
    appDispatchers: AppDispatchers,
    private val preferences: Preferences,
    private val authRepository: AuthRepository,
) : SuspendUseCase<Unit, Unit>(appDispatchers.io) {
    override suspend fun execute(parameter: Unit) {
        preferences.remove(PrefKeys.IDENTITY_DATA.name)
        preferences.remove(CookieType.USER_IDENTITY.value)
        authRepository.setAnonymousIdentityCookie()
    }
}
