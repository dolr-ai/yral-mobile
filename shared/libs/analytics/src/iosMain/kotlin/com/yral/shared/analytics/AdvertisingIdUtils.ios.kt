package com.yral.shared.analytics

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result

actual suspend fun getAdvertisingID(): Result<String?, Throwable> = Err(Exception("Not implemented"))

internal actual fun saveADIDtoProperties(id: String): Map<String, String> = buildMap { put("IDFA", id) }
