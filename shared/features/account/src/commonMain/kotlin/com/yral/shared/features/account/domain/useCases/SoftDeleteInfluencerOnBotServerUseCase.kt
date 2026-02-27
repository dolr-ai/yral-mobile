package com.yral.shared.features.account.domain.useCases

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences

private const val SOFT_DELETE_LOG_TAG = "BotDeleteFlow"

class SoftDeleteInfluencerOnBotServerUseCase(
    private val commonApis: CommonApis,
    private val preferences: Preferences,
    private val chatBaseUrl: String,
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<String?, Unit>(dispatchers.network, useCaseFailureListener) {
    override suspend fun executeWith(parameter: String?): Result<Unit, Throwable> {
        val idToken = preferences.getString(PrefKeys.ID_TOKEN.name).orEmpty()
        val result =
            when {
                parameter.isNullOrBlank() -> {
                    Logger.w(SOFT_DELETE_LOG_TAG) { "bot-server soft delete skipped: principal missing" }
                    Ok(Unit)
                }
                idToken.isBlank() -> {
                    Logger.w(SOFT_DELETE_LOG_TAG) {
                        "bot-server soft delete skipped: id token missing principal=$parameter"
                    }
                    Ok(Unit)
                }
                else -> {
                    Logger.d(SOFT_DELETE_LOG_TAG) {
                        "calling bot-server soft delete influencerId=$parameter chatBaseUrl=$chatBaseUrl"
                    }
                    commonApis
                        .softDeleteInfluencer(parameter, idToken, chatBaseUrl)
                        .fold(
                            onSuccess = {
                                Logger.d(SOFT_DELETE_LOG_TAG) {
                                    "bot-server soft delete success influencerId=$parameter"
                                }
                                Ok(Unit)
                            },
                            onFailure = { error ->
                                Logger.w(SOFT_DELETE_LOG_TAG, error) {
                                    "bot-server soft delete failed; continuing with delete_user_info " +
                                        "influencerId=$parameter message=${error.message}"
                                }
                                Err(error)
                            },
                        )
                }
            }
        return result
    }

    override suspend fun execute(parameter: String?) {
        executeWith(parameter).getOrThrow()
    }
}
