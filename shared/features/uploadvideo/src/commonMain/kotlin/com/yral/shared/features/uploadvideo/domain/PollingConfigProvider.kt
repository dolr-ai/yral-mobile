package com.yral.shared.features.uploadvideo.domain

interface PollingConfigProvider {
    val earlyPolls: Int
    val earlyIntervalMs: Long
    val initialIntervalMs: Long
    val maxIntervalMs: Long
    val minIntervalMs: Long
    val backoffMultiplier: Double
    val decayMultiplier: Double
}

internal object PollingDefaults {
    const val EARLY_POLLS: Int = 2
    const val EARLY_INTERVAL_MS: Long = 2_000L
    const val INITIAL_INTERVAL_MS: Long = 5_000L
    const val MAX_INTERVAL_MS: Long = 30_000L
    const val MIN_INTERVAL_MS: Long = 2_000L
    const val BACKOFF_MULTIPLIER: Double = 2.0
    const val DECAY_MULTIPLIER: Double = 2.0
}
internal class DefaultPollingConfigProvider : PollingConfigProvider {
    override val earlyPolls: Int = PollingDefaults.EARLY_POLLS
    override val earlyIntervalMs: Long = PollingDefaults.EARLY_INTERVAL_MS
    override val initialIntervalMs: Long = PollingDefaults.INITIAL_INTERVAL_MS
    override val maxIntervalMs: Long = PollingDefaults.MAX_INTERVAL_MS
    override val minIntervalMs: Long = PollingDefaults.MIN_INTERVAL_MS
    override val backoffMultiplier: Double = PollingDefaults.BACKOFF_MULTIPLIER
    override val decayMultiplier: Double = PollingDefaults.DECAY_MULTIPLIER
}
