package com.yral.shared.analytics

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.adTracking.createAdvertisingIdProperties
import com.yral.shared.analytics.adTracking.getAdvertisingID
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.IdentityTransitionEventData
import com.yral.shared.analytics.providers.yral.CoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AnalyticsManager(
    private val providers: List<AnalyticsProvider> = emptyList(),
    private val coreService: CoreService? = null,
    private val deviceInstallIdStore: DeviceInstallIdStore? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventBus = EventBus()

    init {
        eventBus.events
            .onEach { event ->
                trackEventToProviders(event)
                if (coreService?.shouldTrackEvent(event) == true) {
                    coreService.trackEvent(event)
                }
            }.launchIn(scope)

        applyCommonContextToProviders()
    }

    @Suppress("EmptyFunctionBlock")
    fun initialise() {}

    fun trackEvent(event: EventData) {
        eventBus.publish(event)
    }

    private fun trackEventToProviders(event: EventData) {
        trackEventToProviders(event = event, targetProviders = providers)
    }

    private fun trackEventToProviders(
        event: EventData,
        targetProviders: List<AnalyticsProvider>,
    ) {
        targetProviders.forEach { provider ->
            if (provider.shouldTrackEvent(event)) {
                provider.trackEvent(event)
            }
        }
    }

    private fun commonContext(): Map<String, Any?> {
        val deviceInstallId = deviceInstallIdStore?.getOrCreate()
        return buildMap {
            if (!deviceInstallId.isNullOrBlank()) put("device_install_id", deviceInstallId)
        }
    }

    private fun applyCommonContextToProviders() {
        val context = commonContext()
        if (context.isEmpty()) return
        providers.forEach { it.applyCommonContext(context) }
        initializeAdContext()
    }

    private fun initializeAdContext() {
        scope.launch {
            Logger.d("AdTracking") { "Starting fetching advertising id" }
            getAdvertisingID()
                .onSuccess { id ->
                    Logger.d("AdTracking") { "Advertising ID: $id" }
                    id?.let {
                        val common = createAdvertisingIdProperties(id)
                        providers.forEach { it.applyCommonContext(common) }
                    }
                }.onFailure {
                    Logger.e("AdTracking", it) { "Failed to fetch advertising id" }
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
    }

    fun reset() {
        resetInternal(reason = null)
    }

    fun resetWithReason(reason: String) {
        resetInternal(reason = reason)
    }

    private fun resetInternal(reason: String?) {
        val distinctIdBeforeReset: List<Pair<DistinctIdProvider, String>> =
            providers
                .filterIsInstance<DistinctIdProvider>()
                .map { it to it.currentDistinctId() }

        providers.forEach { it.flush() }
        providers.forEach { it.reset() }
        applyCommonContextToProviders()

        trackIdentityTransition(reason, distinctIdBeforeReset)

        coreService?.flush()
        coreService?.reset()
    }

    private fun trackIdentityTransition(
        reason: String?,
        distinctIdBeforeReset: List<Pair<DistinctIdProvider, String>>,
    ) {
        val resetReason = reason ?: "unknown"
        distinctIdBeforeReset.forEach { (distinctIdProvider, previousDistinctId) ->
            val provider = distinctIdProvider as? AnalyticsProvider ?: return@forEach
            val newDistinctId = distinctIdProvider.currentDistinctId()
            if (previousDistinctId != newDistinctId) {
                trackEventToProviders(
                    event =
                        IdentityTransitionEventData(
                            previousDistinctId = previousDistinctId,
                            newDistinctId = newDistinctId,
                            resetReason = resetReason,
                        ),
                    targetProviders = listOf(provider),
                )
            }
        }
    }
}
