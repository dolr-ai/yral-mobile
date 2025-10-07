package com.yral.shared.libs.routing.routes.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents every possible navigation destination in the app.
 */
sealed interface AppRoute

/**
 * A sub-interface for AppRoutes that can carry additional, non-navigational
 * metadata for analytics or other purposes.
 */
interface WithMetadata {
    @Transient
    val metadata: Map<String, Any>
}

// --- Example Route Definitions ---

@Serializable
data class ProductDetails(
    val productId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute,
    WithMetadata,
    ExternallyExposedRoute

@Serializable
object Home : AppRoute, ExternallyExposedRoute {
    const val PATH = "/"
}

@Serializable
object Wallet : AppRoute, ExternallyExposedRoute {
    const val PATH = "/wallet"
}

@Serializable
object Leaderboard : AppRoute, ExternallyExposedRoute {
    const val PATH = "/leaderboard"
}

@Serializable
object AddVideo : AppRoute, ExternallyExposedRoute {
    const val PATH = "/addVideo"
}

@Serializable
object GenerateAIVideo : AppRoute, ExternallyExposedRoute {
    const val PATH = "/addVideo/aiGen"
}

@Serializable
object Profile : AppRoute, ExternallyExposedRoute {
    const val PATH = "/profile"
}

@Serializable
object Unknown : AppRoute {
    const val PATH = "/unknown"
}

@Serializable
data class PostDetailsRoute(
    val canisterId: String,
    val postId: String,
) : AppRoute,
    ExternallyExposedRoute {
    companion object Companion {
        const val PATH = "post/details/{canisterId}/{postId}"
    }
}

@Serializable
data class RewardsReceived(
    val token: String,
    @SerialName("reward_on")
    val rewardOn: RewardOn,
    @SerialName("creator_id")
    val creatorId: String? = null,
    @SerialName("video_id")
    val videoID: String? = null,
    @SerialName("reward_btc")
    val rewardBtc: String? = null,
    @SerialName("reward_inr")
    val rewardInr: String? = null,
    @SerialName("timestamp")
    val timestamp: String? = null,
    @SerialName("view_count")
    val viewCount: String? = null,
) : AppRoute,
    ExternallyExposedRoute {
    companion object {
        const val PATH =
            "rewardsReceived?" +
                "token={token}" +
                "&reward_on={reward_on}" +
                "&creator_id={creator_id}" +
                "&video_id={video_id}" +
                "&reward_btc={reward_btc}" +
                "&reward_inr={reward_inr}" +
                "&view_count={view_count}" +
                "&timestamp={timestamp}"
    }
}

@Serializable
data class VideoUploadSuccessful(
    @SerialName("video_id")
    val videoID: String? = null,
) : AppRoute,
    ExternallyExposedRoute {
    companion object {
        const val PATH = "profile/videos?video_id={video_id}"
    }
}

@Serializable
enum class RewardOn {
    @SerialName("video_views")
    VIDEO_VIEWS,
}

// --- Test Route Definitions for Testing ---

@Serializable
data class TestProductRoute(
    val productId: String,
    val category: String? = null,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute,
    WithMetadata,
    ExternallyExposedRoute {
    companion object {
        const val PATH = "/test/product/{productId}"
    }
}

@Serializable
data class TestUserRoute(
    val userId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute,
    WithMetadata,
    ExternallyExposedRoute {
    companion object {
        const val PATH = "/test/user/{userId}"
    }
}

@Serializable
object TestHomeRoute : AppRoute, ExternallyExposedRoute {
    const val PATH = "/"
}

@Serializable
data class TestInternalRoute(
    val internalId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute,
    WithMetadata // Note: does NOT implement com.yral.shared.libs.routing.routes.api.ExternallyExposedRoute

@Serializable
object TestUnknownRoute : AppRoute
