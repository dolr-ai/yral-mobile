package com.yral.featureflag.core

interface FeatureFlagProvider {
    val id: String
    val name: String
    val isRemote: Boolean get() = false

    /** Return a cached raw string value. Do not block. Return NotSet if not explicitly set. */
    fun getRaw(key: String): FlagResult<String>

    /** Optional: kickoff any async fetch/activation. Should be idempotent. */
    suspend fun fetchAndActivate(): Unit = Unit
}

interface MutableFeatureFlagProvider : FeatureFlagProvider {
    fun setRaw(
        key: String,
        value: String,
    )
    fun clear(key: String)
}
