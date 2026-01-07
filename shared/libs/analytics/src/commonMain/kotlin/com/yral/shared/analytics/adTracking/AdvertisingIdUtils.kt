package com.yral.shared.analytics.adTracking

import com.github.michaelbull.result.Result
import com.yral.shared.koin.koinInstance

suspend fun getAdvertisingID(): Result<String?, Throwable> = koinInstance.get<GetADIDUseCase>().invoke()

fun getAdvertisingIdKey(): String = koinInstance.get<AdvertisingIdProvider>().getAdvertisingIdKey()
