package com.yral.shared.data.domain.useCases

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.DailyStreak
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences

class FetchDailyStreakUseCase(
    private val commonApis: CommonApis,
    private val preferences: Preferences,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<FetchDailyStreakUseCase.Params, DailyStreak>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Params): DailyStreak {
        val idToken =
            preferences.getString(PrefKeys.ID_TOKEN.name)
                ?: throw YralException("Authorisation not found")
        return commonApis.fetchDailyStreak(parameter.userPrincipal, idToken)
    }

    data class Params(
        val userPrincipal: String,
    )
}
