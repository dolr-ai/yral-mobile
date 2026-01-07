package com.yral.shared.analytics.adTracking

import com.github.michaelbull.result.Result
import com.yral.shared.koin.koinInstance

actual suspend fun getAdvertisingID(): Result<String?, Throwable> = koinInstance.get<GetADIDUseCase>().invoke()

internal actual fun createAdvertisingIdProperties(id: String): Map<String, String> = buildMap { put("IDFA", id) }
