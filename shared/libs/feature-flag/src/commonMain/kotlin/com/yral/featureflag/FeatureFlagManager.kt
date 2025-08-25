package com.yral.featureflag

import co.touchlab.kermit.Logger
import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FeatureFlagProvider
import com.yral.featureflag.core.FlagResult
import com.yral.featureflag.core.MutableFeatureFlagProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

class FeatureFlagManager(
    private val providersInPriority: List<FeatureFlagProvider>,
    private val localProviderId: String,
    private val providerControls: Map<String, FeatureFlag<Boolean>> = emptyMap(),
    private val logger: Logger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val resolvedValues = mutableMapOf<String, Any?>()
    private var activeProviders: List<FeatureFlagProvider> = emptyList()

    private val remoteFetchReady = CompletableDeferred<Unit>()

    internal val localProvider = providersInPriority.firstOrNull { it.id == localProviderId }

    init {
        recomputeActiveProviders()
        hydrateAndFetchRemotesAsync()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(flag: FeatureFlag<T>): T = resolvedValues.getOrPut(flag.key) { resolve(flag) } as T

    fun isEnabled(flag: FeatureFlag<Boolean>): Boolean = get(flag)

    private fun <T> resolve(flag: FeatureFlag<T>): T {
        for (provider in activeProviders) {
            when (val res = provider.getRaw(flag.key)) {
                is FlagResult.Sourced -> {
                    val decoded = flag.codec.decode(res.value)
                    if (decoded != null) return decoded
                }

                FlagResult.NotSet -> {}
            }
        }
        return flag.defaultValue
    }

    // no source tracking variant

    suspend fun hydrateAndFetchRemotes() {
        // Kick off fetch/activate for all remote providers (all except local)
        val jobs =
            activeProviders
                .filter { it.id != localProviderId && it.isRemote }
                .map { provider ->
                    scope.async {
                        try {
                            provider.fetchAndActivate()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                            logger.w(e) { "Failed to fetch and activate remote flags provider ${provider.name}" }
                        }
                    }
                }
        jobs.awaitAll()
        if (!remoteFetchReady.isCompleted) remoteFetchReady.complete(Unit)
        // Clear caches so subsequent reads re-resolve with fresh remote values
        clearResolvedCaches()
    }

    /** Fire-and-forget hydration in background. Pair with awaitRemoteFetch(timeout) if you need to wait. */
    fun hydrateAndFetchRemotesAsync() {
        scope.launch { hydrateAndFetchRemotes() }
    }

    suspend fun awaitRemoteFetch(timeout: Duration): Boolean {
        val result = withTimeoutOrNull(timeout) { remoteFetchReady.await() }
        return result != null
    }

    internal fun clearResolvedCaches() {
        resolvedValues.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> setLocalOverride(
        flag: FeatureFlag<T>,
        value: T,
    ) {
        changeLocalOverride(flag) { local ->
            local.setRaw(flag.key, flag.codec.encode(value))
            resolvedValues[flag.key] = value as Any
        }
    }

    fun clearLocalOverride(flag: FeatureFlag<*>) {
        changeLocalOverride(flag) { local ->
            local.clear(flag.key)
            resolvedValues.remove(flag.key)
        }
    }

    private inline fun changeLocalOverride(
        flag: FeatureFlag<*>,
        block: (local: MutableFeatureFlagProvider) -> Unit,
    ) {
        val local = localProvider as? MutableFeatureFlagProvider
        local?.let { block(it) }
        if (isProviderToggle(flag)) {
            recomputeActiveProviders()
            clearResolvedCaches()
        }
    }

    fun isOverridden(flag: FeatureFlag<*>): Boolean {
        val local = localProvider ?: return false
        return local.getRaw(flag.key) is FlagResult.Sourced
    }

    private fun isProviderToggle(flag: FeatureFlag<*>): Boolean = providerControls.values.any { it.key == flag.key }

    private fun recomputeActiveProviders() {
        val enabledById: Map<String, Boolean> =
            providersInPriority.associate { provider ->
                val toggle = providerControls[provider.id]
                if (toggle == null) {
                    provider.id to true
                } else {
                    val res = localProvider?.getRaw(toggle.key)
                    val value =
                        if (res is FlagResult.Sourced) {
                            toggle.codec.decode(res.value)
                                ?: toggle.defaultValue
                        } else {
                            toggle.defaultValue
                        }
                    provider.id to (value)
                }
            }
        activeProviders =
            providersInPriority.filter { provider ->
                provider.id == localProviderId || (enabledById[provider.id] ?: true)
            }
    }
}
