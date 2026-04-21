package com.yral.featureflag.providers

import com.yral.featureflag.core.FeatureFlagProvider
import com.yral.featureflag.core.FlagResult
import kotlin.time.Duration

class FirebaseRemoteConfigProvider(
    override val id: String = ID,
    override val name: String = NAME,
    private val isDevMode: Boolean = false,
) : FeatureFlagProvider {
    private val remoteConfig: FirebaseRemoteConfigPlatform = createFirebaseRemoteConfigPlatform()
    override val isRemote: Boolean = true

    override fun getRaw(key: String): FlagResult<String> {
        val value = remoteConfig.getStringOrNull(key)
        return value?.let { FlagResult.Sourced(it) } ?: FlagResult.NotSet
    }

    override suspend fun fetchAndActivate() {
        remoteConfig.fetchAndActivate(
            minimumFetchInterval = if (isDevMode) Duration.parse("1m") else null,
        )
    }

    companion object {
        const val ID = "firebase_remote_config"
        const val NAME = "Firebase Remote Config"
    }
}

internal interface FirebaseRemoteConfigPlatform {
    fun getStringOrNull(key: String): String?

    suspend fun fetchAndActivate(minimumFetchInterval: Duration?)
}

internal expect fun createFirebaseRemoteConfigPlatform(): FirebaseRemoteConfigPlatform
