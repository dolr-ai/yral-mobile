package com.yral.shared.analytics.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CoreService(
    private val apiClient: ApiClient,
    private val batchSize: Int = 20,
    private val autoFlushEvents: Boolean = true,
    private val autoFlushIntervalMs: Long = 120000,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventQueue = mutableListOf<Event>()
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

    fun trackEvent(event: Event) {
        scope.launch {
            mutex.withLock {
                eventQueue.add(event)
                if (autoFlushEvents && eventQueue.size >= batchSize) {
                    flushEvents()
                }
            }
        }
    }

    fun flush() {
        scope.launch {
            mutex.withLock {
                flushEvents()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun flushEvents() {
        startAutoFlushTimer()
        if (eventQueue.isEmpty()) return

        val eventsToSend = eventQueue.toList()
        eventQueue.clear()
        try {
            apiClient.sendEvents(eventsToSend)
        } catch (e: Exception) {
            // Handle error: add back to queue or implement retry mechanism
            mutex.withLock {
                eventQueue.addAll(0, eventsToSend)
            }
        }
    }

    fun setUser(user: User) {
        this.user = user
    }
}
