package com.yral.shared.app.di

import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.ProviderFlags
import com.yral.featureflag.providers.FirebaseRemoteConfigProvider
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureFlagModule =
    module {

        factory {
            FirebaseRemoteConfigProvider()
        }

        factoryOf(::FeatureFlagListener) bind FeatureFlagManager.Listener::class

        single {
            val firebase: FirebaseRemoteConfigProvider = get()
            FeatureFlagManager(
                providersInPriority = listOf(firebase),
                localProviderId = "",
                providerControls =
                    mapOf(
                        // Register controllable providers here. Add more entries as new providers are added.
                        FirebaseRemoteConfigProvider.ID to ProviderFlags.FirebaseEnabled,
                    ),
                listener = get(),
            )
        }
    }

private class FeatureFlagListener(
    private val logger: YralLogger,
    private val crashlyticsManager: CrashlyticsManager,
) : FeatureFlagManager.Listener {
    override fun onRemoteFetchFailed(
        provider: String,
        throwable: Throwable,
    ) {
        logger.w(throwable) { "Failed to fetch and activate remote flags provider $provider" }
        crashlyticsManager.recordException(Exception(throwable))
    }
}
