package com.yral.shared.analytics

import com.github.michaelbull.result.Result
import com.yral.shared.koin.koinInstance

actual suspend fun getAdvertisingID(): Result<String?, Throwable> = koinInstance.get<GetGAIDUseCase>().invoke()

actual fun saveADIDtoProperties(id: String): Map<String, String> = buildMap { put("GAID", id) }
