package com.yral.shared.testsupport.featureflag

import com.yral.featureflag.core.FlagResult
import com.yral.featureflag.core.MutableFeatureFlagProvider

class FakeMutableFeatureFlagProvider(
    override val id: String,
    override val name: String,
    private val values: MutableMap<String, String> = mutableMapOf(),
    override val isRemote: Boolean = false,
) : MutableFeatureFlagProvider {
    override fun getRaw(key: String): FlagResult<String> =
        values[key]?.let { FlagResult.Sourced(it) }
            ?: FlagResult.NotSet

    override fun setRaw(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun clear(key: String) {
        values.remove(key)
    }
}
