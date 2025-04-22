package com.yral.shared.http

import kotlinx.serialization.json.Json

fun createClientJson() =
    Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
