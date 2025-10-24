package com.yral.shared.analytics.providers.bigquery

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("LongParameterList")
class BigQueryAnalyticsProvider(
    private val apiService: BigQueryEventsApiService,
    private val crashlyticsService: CrashlyticsManager,
    private val json: Json,
    private val eventFilter: (EventData) -> Boolean = { true },
    private val batchSize: Int = ANALYTICS_BATCH_SIZE,
    private val autoFlushEvents: Boolean = true,
    private val autoFlushIntervalMs: Long = ANALYTICS_FLUSH_MS,
    override val name: String = "BigQueryAnalytics",
    private val extraFieldsProvider: () -> JsonObject = { buildJsonObject { } },
    private val dryRun: Boolean = false,
    private val log: (String) -> Unit = { println(it) },
) : AnalyticsProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var queue = mutableListOf<EventData>()
    private val mutex = Mutex()
    private var user: User? = null

    init {
        if (autoFlushEvents) {
            scope.launch {
                while (isActive) {
                    delay(autoFlushIntervalMs)
                    flush()
                }
            }
        }
    }

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        scope.launch {
            var shouldFlushNow: Boolean
            mutex.withLock {
                queue += event
                shouldFlushNow = autoFlushEvents && queue.size >= batchSize
            }
            if (shouldFlushNow) flush()
        }
    }

    override fun flush() {
        scope.launch { flushInternal() }
    }

    override fun setUserProperties(user: User) {
        this.user = user
    }

    override fun reset(resetOnlyProperties: Boolean) {
        user = null
        scope.launch { mutex.withLock { queue.clear() } }
    }

    override fun toValidKeyName(key: String): String = key

    @Suppress("TooGenericExceptionCaught")
    private suspend fun flushInternal() {
        val toSend: List<EventData> =
            mutex.withLock {
                if (queue.isEmpty()) return@withLock emptyList()
                queue.toList().also { queue.clear() }
            }

        if (toSend.isEmpty()) return
        val rows = toSend.map { toRow(it) }
        if (dryRun) {
            val payload =
                buildJsonArray {
                    rows.forEach { row ->
                        add(
                            buildJsonObject {
                                put("timestamp", JsonPrimitive(row.timestamp))
                                put("event_data", row.eventData)
                            },
                        )
                    }
                }
            val prettyJson = Json(from = json) { prettyPrint = true }
            val payloadString = prettyJson.encodeToString(payload)
            log("BQ trackEvent: $payloadString")
            return
        }
        try {
            apiService.sendRows(rows)
        } catch (e: Exception) {
            crashlyticsService.recordException(e)
            mutex.withLock { queue.addAll(0, toSend) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("MagicNumber")
    private fun toRow(event: EventData): BigQueryEventRow {
        val base: JsonObject = json.encodeToJsonElement(event).jsonObject
        val enriched: JsonObject =
            base
                .mergedWith(buildUserMeta(user))
                .mergedWith(extraFieldsProvider())

        val dt = Instant.fromEpochMilliseconds(event.timestamp).toLocalDateTime(TimeZone.UTC)
        val isoTs =
            buildString {
                append(dt.year.toString().padStart(4, '0'))
                append('-')
                append(
                    dt.month.number
                        .toString()
                        .padStart(2, '0'),
                )
                append('-')
                append(dt.day.toString().padStart(2, '0'))
                append('T')
                append(dt.hour.toString().padStart(2, '0'))
                append(':')
                append(dt.minute.toString().padStart(2, '0'))
                append(':')
                append(dt.second.toString().padStart(2, '0'))
                append('.')
                append((dt.nanosecond / 1_000_000).toString().padStart(3, '0'))
                append("+00:00")
            }

        return BigQueryEventRow(
            eventData = enriched,
            timestamp = isoTs,
        )
    }

    private fun buildUserMeta(user: User?): JsonObject =
        buildJsonObject {
            user?.let {
                put("user_id", JsonPrimitive(it.userId))
                put("canister_id", JsonPrimitive(it.canisterId))
                it.isLoggedIn?.let { value -> put("is_logged_in", JsonPrimitive(value)) }
                it.isCreator?.let { value -> put("is_creator", JsonPrimitive(value)) }
                it.walletBalance?.let { value -> put("wallet_balance", JsonPrimitive(value)) }
                it.tokenType?.let { value -> put("token_type", JsonPrimitive(value.serialName)) }
                // optional aliases for parity with web
                put("distinct_id", JsonPrimitive(it.userId))
                put("custom_device_id", JsonPrimitive(it.userId))
            }
        }

    private fun JsonObject.mergedWith(other: JsonObject): JsonObject =
        buildJsonObject {
            this@mergedWith.forEach { (key, value) -> put(key, value) }
            other.forEach { (key, value) -> put(key, value) }
        }

    private val TokenType.serialName: String
        get() =
            when (this) {
                TokenType.CENTS -> "cents"
                TokenType.SATS -> "sats"
                TokenType.YRAL -> "yral"
            }

    companion object {
        private const val ANALYTICS_BATCH_SIZE = 5
        private const val ANALYTICS_FLUSH_MS = 30_000L
    }
}
