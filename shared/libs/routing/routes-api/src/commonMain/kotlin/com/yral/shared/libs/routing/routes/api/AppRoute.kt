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
interface AppRouteWithMetadata : AppRoute {
    @Transient
    val metadata: Map<String, Any>
}

// --- Example Route Definitions ---

@Serializable
data class ProductDetails(
    val productId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRouteWithMetadata, ExternallyExposedRoute

@Serializable
object Home : AppRoute

@Serializable
object Unknown : AppRoute

// --- Test Route Definitions for Testing ---

@Serializable
data class TestProductRoute(
    val productId: String,
    val category: String? = null,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRouteWithMetadata, ExternallyExposedRoute

@Serializable
data class TestUserRoute(
    val userId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRouteWithMetadata, ExternallyExposedRoute

@Serializable
object TestHomeRoute : AppRoute, ExternallyExposedRoute

@Serializable
data class TestInternalRoute(
    val internalId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRouteWithMetadata // Note: does NOT implement com.yral.shared.libs.routing.routes.api.ExternallyExposedRoute

@Serializable
object TestUnknownRoute : AppRoute
