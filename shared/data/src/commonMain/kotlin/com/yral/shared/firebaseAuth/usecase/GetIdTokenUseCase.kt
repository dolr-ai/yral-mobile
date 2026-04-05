package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences

class GetIdTokenUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val preferences: Preferences,
) : SuspendUseCase<GetIdTokenUseCase.Parameters, String>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Parameters): String =
        preferences.getString(PrefKeys.ID_TOKEN.name).orEmpty().ifEmpty {
            error("ID token is missing")
        }

    data object Parameters

    companion object {
        val DEFAULT: Parameters = Parameters
    }
}
