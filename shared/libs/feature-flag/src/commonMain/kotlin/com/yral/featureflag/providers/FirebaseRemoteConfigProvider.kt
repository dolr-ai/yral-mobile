package com.yral.featureflag.providers

import com.yral.featureflag.core.FeatureFlagProvider
import com.yral.featureflag.core.FlagResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import dev.gitlive.firebase.remoteconfig.ValueSource
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlin.time.Duration.Companion.minutes

class FirebaseRemoteConfigProvider(
    override val id: String = ID,
    override val name: String = NAME,
    private val isDevMode: Boolean = false,
) : FeatureFlagProvider {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    override val isRemote: Boolean = true

    override fun getRaw(key: String): FlagResult<String> {
        val value = remoteConfig.getValue(key)
        return if (value.getSource() != ValueSource.Static) {
            FlagResult.Sourced(value.asString())
        } else {
            FlagResult.NotSet
        }
    }

    override suspend fun fetchAndActivate() {
        if (isDevMode) {
            remoteConfig.fetch(1.minutes)
            remoteConfig.activate()
        } else {
            remoteConfig.fetchAndActivate()
        }
    }

    companion object {
        const val ID = "firebase_remote_config"
        const val NAME = "Firebase Remote Config"
    }
}
