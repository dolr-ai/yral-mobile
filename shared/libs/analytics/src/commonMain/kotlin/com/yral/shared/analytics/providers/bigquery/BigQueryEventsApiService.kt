// com/yral/shared/analytics/providers/bigquery/BigQueryEventsApiService.kt
package com.yral.shared.analytics.providers.bigquery

import com.yral.shared.core.AppConfigurations
import com.yral.shared.http.httpPostWithStringResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class BigQueryEventsApiService internal constructor(
    private val client: HttpClient,
) {
    suspend fun sendRows(rows: List<BigQueryEventRow>) {
        val payload =
            BulkRows(
                rows = rows,
            )

        httpPostWithStringResponse(client) {
            url {
                host = AppConfigurations.ANALYTICS_BASE_URL
                path(BQ_BULK_EVENTS_END_POINT)
            }
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    companion object {
        private const val BQ_BULK_EVENTS_END_POINT = "/api/send_bigquery"
    }

    @Serializable
    private data class BulkRows(
        val rows: List<BigQueryEventRow>,
    )
}

@Serializable
data class BigQueryEventRow(
    @SerialName("event_data") val eventData: JsonObject,
    val timestamp: String,
)
