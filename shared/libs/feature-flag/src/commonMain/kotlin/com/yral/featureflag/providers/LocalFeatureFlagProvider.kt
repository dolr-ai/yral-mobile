package com.yral.featureflag.providers

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.yral.featureflag.core.FlagResult
import com.yral.featureflag.core.MutableFeatureFlagProvider

class LocalFeatureFlagProvider(
    private val settings: Settings,
    override val id: String = "local_overrides",
    override val name: String = "Local Overrides",
) : MutableFeatureFlagProvider {
    override fun getRaw(key: String): FlagResult<String> {
        val exists = settings.hasKey(key)
        return if (exists) FlagResult.Sourced(settings.getString(key, "")) else FlagResult.NotSet
    }

    override fun setRaw(
        key: String,
        value: String,
    ) {
        settings[key] = value
    }

    override fun clear(key: String) {
        settings.remove(key)
    }
}
