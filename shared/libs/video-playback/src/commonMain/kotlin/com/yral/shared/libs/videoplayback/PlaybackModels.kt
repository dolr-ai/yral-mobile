package com.yral.shared.libs.videoplayback

enum class ContainerHint {
    MP4,
    HLS,
    DASH,
    UNKNOWN,
}

data class MediaDescriptor(
    val id: String,
    val uri: String,
    val containerHint: ContainerHint = ContainerHint.UNKNOWN,
    val headers: Map<String, String> = emptyMap(),
)

data class PreloadPolicy(
    val preparedPrev: Int = 1,
    val preparedNext: Int = 1,
    val diskPrefetchNext: Int = 3,
    val maxConcurrentPrefetch: Int = 2,
    val preloadTargetBytes: Long = 1_500_000,
    val cacheMaxBytes: Long = 250_000_000,
    val usePreparedNextPlayer: Boolean = defaultUsePreparedNextPlayer(),
)

expect fun defaultUsePreparedNextPlayer(): Boolean

data class CoordinatorDeps(
    val policy: PreloadPolicy = PreloadPolicy(),
    val reporter: PlaybackEventReporter = NoopPlaybackEventReporter,
    val progressTickIntervalMs: Long = 1000,
)
