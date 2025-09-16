package com.yral.shared.libs.routing.routes.api

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
object Home : AppRoute {
    const val PATH = "/"
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
