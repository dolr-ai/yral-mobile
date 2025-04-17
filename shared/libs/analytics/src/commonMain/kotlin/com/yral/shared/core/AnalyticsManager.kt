package com.yral.shared.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AnalyticsManager(
    private val providers: List<AnalyticsProvider> = emptyList(),
    private val coreService: CoreService? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventBus = EventBus()

    init {
        eventBus.events
            .onEach { event ->
                trackEventToProviders(event)
                coreService?.trackEvent(event)
            }.launchIn(scope)
    }

    fun addProvider(provider: AnalyticsProvider) =
        AnalyticsManager(
            providers = providers + provider,
            coreService = coreService,
        )

    fun setCoreService(service: CoreService) =
        AnalyticsManager(
            providers = providers,
            coreService = service,
        )

    fun trackEvent(event: Event) {
        eventBus.publish(event)
    }

    fun trackEvent(
        name: String,
        properties: Map<String, Any> = emptyMap(),
    ) {
        trackEvent(Event(name, properties))
    }

    private fun trackEventToProviders(event: Event) {
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
}
