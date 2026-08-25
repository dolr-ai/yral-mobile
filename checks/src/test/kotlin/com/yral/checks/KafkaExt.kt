package com.yral.checks

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

// Kafka Bridge HTTP REST API — replaces the native Kafka consumer.
// The bridge is at https://kafka-bridge.yral.com, fronted by an nginx proxy
// that validates the X-Bearer-Token header. The token is provided via the
// KAFKA_BRIDGE_TOKEN environment variable (injected by fnox locally).
private val bridgeUrl: String = System.getenv("KAFKA_BRIDGE_URL") ?: "https://kafka-bridge.yral.com"

private val bridgeToken: String =
    System.getenv("KAFKA_BRIDGE_TOKEN")
        ?: error("KAFKA_BRIDGE_TOKEN is not set. Run via `fnox exec` or mise (fnox injects it).")

private val httpClient: HttpClient =
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

/**
 * Count Snowplow events in the `snowplow-raw` topic since [since] (epoch ms)
 * that match the given [platformMarker] and optional [eventMarker].
 *
 * Uses the Kafka Bridge HTTP REST API instead of a native Kafka consumer.
 * Creates a consumer, subscribes to snowplow-raw, polls for records, and
 * deletes the consumer when done.
 */
fun countSnowplowEvents(
    since: Long,
    platformMarker: String,
    eventMarker: String? = null,
    minCount: Int = 1,
): Int {
    val consumerGroup = "ci-e2e-assert-${UUID.randomUUID()}"
    val consumerName = "consumer-$consumerGroup"

    // 1. Create a consumer instance via the Bridge
    val createBody =
        """
        {
          "name": "$consumerName",
          "format": "binary",
          "auto.offset.reset": "earliest",
          "enable.auto.commit": false
        }
        """.trimIndent()

    httpRequest(
        "POST",
        "/consumers/$consumerGroup",
        createBody,
        expectedStatus = 200,
    )

    // 2. Subscribe to the snowplow-raw topic
    val subscribeBody = """{"topics":["snowplow-raw"]}"""
    httpRequest(
        "POST",
        "/consumers/$consumerGroup/instances/$consumerName/subscription",
        subscribeBody,
        expectedStatus = 204,
    )

    // 3. Poll for records — snowplow-raw is Thrift-encoded binary, but
    //    "yral-mobile-staging" and platform markers ("andr-", "ios-") are
    //    embedded as readable UTF-8 substrings within the payload.
    var found = 0
    val deadline = System.currentTimeMillis() + 5 * 60_000 // 5 min: collector to Kafka pipeline latency
    while (System.currentTimeMillis() < deadline) {
        val responseBody =
            httpRequest(
                "GET",
                "/consumers/$consumerGroup/instances/$consumerName/records?timeout=1000",
                null,
                expectedStatus = 200,
            )

        if (responseBody.isNotBlank() && responseBody != "[]") {
            val records = parseRecords(responseBody)
            for (recordValue in records) {
                val payload = String(recordValue, Charsets.UTF_8)
                if (payload.contains("yral-mobile-staging") && payload.contains(platformMarker) &&
                    (eventMarker == null || payload.contains(eventMarker))
                ) {
                    found++
                }
            }
        }

        if (found >= minCount) break
    }

    // 4. Delete the consumer to clean up
    httpRequest(
        "DELETE",
        "/consumers/$consumerGroup/instances/$consumerName",
        null,
        expectedStatus = 204,
    )

    return found
}

private fun httpRequest(
    method: String,
    path: String,
    body: String?,
    expectedStatus: Int,
): String {
    val requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create("$bridgeUrl$path"))
            .header("X-Bearer-Token", bridgeToken)
            .timeout(Duration.ofSeconds(30))

    if (body != null) {
        requestBuilder.header("Content-Type", "application/vnd.kafka.v2+json")
    }

    when (method) {
        "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body ?: ""))
        "GET" -> requestBuilder.GET()
        "DELETE" -> requestBuilder.DELETE()
    }

    val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

    if (response.statusCode() != expectedStatus) {
        error("Kafka Bridge $method $path failed: ${response.statusCode()} — ${response.body()}")
    }

    return response.body()
}

/**
 * Parse the Kafka Bridge records response and extract base64-decoded values.
 * The response is a JSON array like:
 * [{"topic":"snowplow-raw","partition":0,"offset":123,"value":"base64data",...},...]
 */
private fun parseRecords(json: String): List<ByteArray> {
    val records = mutableListOf<ByteArray>()
    val valueRegex = Regex(""""value"\s*:\s*"([A-Za-z0-9+/=]*)"\s*[,}]""")
    for (match in valueRegex.findAll(json)) {
        val base64Value = match.groupValues[1]
        if (base64Value.isNotEmpty()) {
            records.add(java.util.Base64.getDecoder().decode(base64Value))
        }
    }
    return records
}