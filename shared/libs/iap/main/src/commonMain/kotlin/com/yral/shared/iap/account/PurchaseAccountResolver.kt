package com.yral.shared.iap.account

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

internal interface PurchaseAccountResolver {
    suspend fun resolve(userId: String): String
}

internal expect fun createPurchaseAccountResolver(
    httpClient: HttpClient,
    json: Json,
    billingBaseUrl: String,
): PurchaseAccountResolver
