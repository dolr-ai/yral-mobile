package com.yral.shared.analytics

import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.providers.yral.CoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class AnalyticsManager(
    private val providers: List<AnalyticsProvider> = emptyList(),
    private val coreService: CoreService? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventBus = EventBus()
    private val pendingEvents = mutableListOf<EventData>()
    private val isReady = AtomicBoolean(false)
    private val mutex = Mutex()

    init {
        eventBus.events
            .onEach { event ->
                trackEventToProviders(event)
                if (coreService?.shouldTrackEvent(event) == true) {
                    coreService.trackEvent(event)
                }
            }.launchIn(scope)
    }

    internal fun addProvider(provider: AnalyticsProvider) =
        AnalyticsManager(
            providers = providers + provider,
            coreService = coreService,
        )

    internal fun setCoreService(service: CoreService) =
        AnalyticsManager(
            providers = providers,
            coreService = service,
        )

    fun trackEvent(event: EventData) {
        if (isReady.load()) {
            eventBus.publish(event)
        } else {
            scope.launch { mutex.withLock { pendingEvents += event } }
        }
    }

    private fun trackEventToProviders(event: EventData) {
        providers.forEach { provider ->
            if (provider.shouldTrackEvent(event)) {
                provider.trackEvent(event)
            }
        }
    }

    fun flush() {
        providers.forEach { it.flush() }
        coreService?.flush()
    }

    fun setUserProperties(user: User) {
        providers.forEach { it.setUserProperties(user) }
        coreService?.setUserProperties(user)
        if (isReady.compareAndSet(false, true)) {
            scope.launch {
                val toSend =
                    mutex.withLock {
                        pendingEvents.toList().also { pendingEvents.clear() }
                    }
                toSend.forEach { eventBus.publish(it) }
            }
        }
    }

    fun reset() {
        providers.forEach { it.flush() }
        providers.forEach { it.reset() }
        coreService?.reset()
        scope.launch { mutex.withLock { pendingEvents.clear() } }
        isReady.store(false)
    }
}
