package com.yral.shared.analytics

import com.github.michaelbull.result.Result

expect suspend fun getAdvertisingID(): Result<String?, Throwable>

expect fun saveADIDtoProperties(id: String): Map<String, String>
