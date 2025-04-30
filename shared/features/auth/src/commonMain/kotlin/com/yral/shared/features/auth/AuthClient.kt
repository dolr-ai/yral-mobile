package com.yral.shared.features.auth

interface AuthClient {
    suspend fun initialize()
    suspend fun refreshAuthIfNeeded()
}
