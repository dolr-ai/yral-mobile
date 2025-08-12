package com.yral.shared.app.di

import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.ProviderFlags
import com.yral.featureflag.providers.FirebaseRemoteConfigProvider
import org.koin.dsl.module

val featureFlagModule =
    module {

        factory {
            FirebaseRemoteConfigProvider()
        }

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
            )
        }
    }
