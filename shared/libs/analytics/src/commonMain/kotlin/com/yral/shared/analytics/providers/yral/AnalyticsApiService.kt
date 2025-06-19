package com.yral.shared.analytics.providers.yral

import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.AppConfigurations
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AnalyticsApiService(
    private val client: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
) {
    suspend fun sendEvents(events: List<EventData>) {
        val identityWire = preferences.getBytes(PrefKeys.IDENTITY.name)
        identityWire?.let {
            val identityWireJson = delegatedIdentityWireToJson(identityWire)
            val delegatedIdentity =
                json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)
            val params =
                BulkEvent(
                    delegatedIdentity = delegatedIdentity,
                    events = events,
                )
            client.post {
                url {
                    host = AppConfigurations.OFF_CHAIN_BASE_URL
                    path(BULK_EVENTS_END_POINT)
                }
                contentType(ContentType.Application.Json)
                setBody(params)
            }
        }
    }

    companion object {
        private const val BULK_EVENTS_END_POINT = "/api/v1/events/bulk"
    }

    @Serializable
    private data class BulkEvent(
        @SerialName("delegated_identity_wire") val delegatedIdentity: KotlinDelegatedIdentityWire,
        val events: List<EventData>,
    )
}
