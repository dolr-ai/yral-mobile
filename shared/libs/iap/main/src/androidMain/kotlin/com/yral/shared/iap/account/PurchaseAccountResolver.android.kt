package com.yral.shared.iap.account

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

internal actual fun createPurchaseAccountResolver(
    httpClient: HttpClient,
    json: Json,
    billingBaseUrl: String,
): PurchaseAccountResolver =
    object : PurchaseAccountResolver {
        override suspend fun resolve(userId: String): String = userId
    }
