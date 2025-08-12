package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.uniffi.generated.RateLimitStatus

internal class GetFreeCreditsStatusUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: IndividualUserRepository,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
) : UnitSuspendUseCase<RateLimitStatus>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit): RateLimitStatus {
        val canisterId =
            sessionManager.canisterID ?: throw YralException("Canister ID not found while fetching rate limit")
        val isRegistered = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false
        return repository.getFreeCreditsStatus(canisterId, isRegistered)
    }
}
