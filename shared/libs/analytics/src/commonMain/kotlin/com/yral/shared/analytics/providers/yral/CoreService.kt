package com.yral.shared.analytics.providers.yral

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CoreService(
    private val analyticsApiService: AnalyticsApiService,
    private val crashlyticsManager: CrashlyticsManager,
    private val eventFilter: (EventData) -> Boolean = { true },
    private val batchSize: Int = ANALYTICS_BATCH_SIZE,
    private val autoFlushEvents: Boolean = true,
    private val autoFlushIntervalMs: Long = ANALYTICS_FLUSH_MS,
    override val name: String = "CoreAnalytics",
) : AnalyticsProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventQueue = mutableListOf<EventData>()
    private val mutex = Mutex()
    private var user: User? = null

    init {
        if (autoFlushEvents) {
            startAutoFlushTimer()
        }
    }

    private fun startAutoFlushTimer() {
        scope.launch {
            while (isActive) {
                delay(autoFlushIntervalMs)
                flush()
            }
        }
    }

    override fun trackEvent(event: EventData) {
        println("xxxx event added")
        scope.launch {
            var shouldFlush: Boolean
            mutex.withLock {
                eventQueue.add(event)
                shouldFlush = autoFlushEvents && eventQueue.size >= batchSize
            }
            if (shouldFlush) {
                flush()
            }
        }
    }

    override fun flush() {
        scope.launch {
            flushEvents()
        }
    }

    override fun setUserProperties(user: User) {
        this.user = user
    }

    override fun reset() {
        this.user = null
    }

    override fun toValidKeyName(key: String): String = key

    @Suppress("TooGenericExceptionCaught")
    private suspend fun flushEvents() {
        println("xxxx flusing events")
        // Don't return early if queue is empty because it might have been modified
        // since we're not under a mutex lock yet
        val eventsToSend =
            mutex.withLock {
                if (eventQueue.isEmpty()) return@withLock emptyList()
                val events = eventQueue.toList()
                eventQueue.clear()
                events
            }
        if (eventsToSend.isEmpty()) return
        try {
            analyticsApiService.sendEvents(eventsToSend)
        } catch (e: Exception) {
            println("xxxx request failed")
            crashlyticsManager.recordException(e)
            mutex.withLock {
                // Add the unsent events back to the beginning of the queue
                eventQueue.addAll(0, eventsToSend)
            }
        }
    }

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    companion object {
        private const val ANALYTICS_BATCH_SIZE = 10
        private const val ANALYTICS_FLUSH_MS = 120000L
    }
}
