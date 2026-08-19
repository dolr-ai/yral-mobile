package com.yral.shared.analytics.providers.yral

import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.AppConfigurations
import com.yral.shared.http.httpPostWithStringResponse
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.Serializable

class AnalyticsApiService internal constructor(
    private val client: HttpClient,
    private val preferences: Preferences,
) {
    suspend fun sendEvents(events: List<EventData>) {
        val idToken = preferences.getString(PrefKeys.ID_TOKEN.name) ?: return
        val params = BulkEvent(events = events)
        httpPostWithStringResponse(client) {
            url {
                host = AppConfigurations.OFF_CHAIN_BASE_URL
                path(BULK_EVENTS_END_POINT)
            }
            contentType(ContentType.Application.Json)
            headers { append("authorization", "Bearer $idToken") }
            setBody(params)
        }
    }

    companion object {
        private const val BULK_EVENTS_END_POINT = "/api/v2/events/bulk"
    }

    @Serializable
    private data class BulkEvent(
        val events: List<EventData>,
    )
}
