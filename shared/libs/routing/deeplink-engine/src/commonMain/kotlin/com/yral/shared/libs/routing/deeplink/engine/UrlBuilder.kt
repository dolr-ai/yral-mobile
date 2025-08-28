package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties

/**
 * The engine for generating URL strings from type-safe AppRoute objects.
 */
class UrlBuilder(
    private val routingTable: RoutingTable,
    private val scheme: String,
    private val host: String,
) {
    /**
     * Build a URL string from a type-safe AppRoute object.
     * Returns null if the route cannot be built into a URL.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun build(route: AppRoute): String? = buildUrlFromRoute(route)

    /**
     * Build a URL from a route using type-safe serialization.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun buildUrlFromRoute(route: AppRoute): String? {
        val matchingDefinition = findRouteDefinition(route) ?: return null
        return buildUrlWithDefinition(matchingDefinition, route)
    }

    /**
     * Build URL with a specific route definition and route instance.
     */
    private fun <T : AppRoute> buildUrlWithDefinition(
        routeDefinition: RouteDefinition<T>,
        route: AppRoute,
    ): String? =
        try {
            // Safe cast - findRouteDefinition ensures the types match
            @Suppress("UNCHECKED_CAST")
            val typedRoute = route as T

            // Use serialization to automatically extract route properties
            val params = extractRouteParams(routeDefinition, typedRoute)

            // Build the URL path from the pattern
            val (path, usedParams) = routeDefinition.pattern.buildPath(params)
            val queryParams = params.filterKeys { it !in usedParams && it != "metadata" }

            // Build the complete URL
            val pathSegments =
                when {
                    path == "/" -> emptyList()
                    else -> {
                        // For all other paths, simply split and filter empty segments
                        // This handles both normal paths and paths with empty parameters
                        path.split("/").filter { it.isNotEmpty() }
                    }
                }

            val urlBuilder =
                URLBuilder(
                    protocol = URLProtocol.createOrDefault(scheme),
                    host = host,
                    pathSegments = pathSegments,
                )
            // Add query parameters
            queryParams.forEach { (key, value) ->
                urlBuilder.parameters.append(key, value)
            }
            urlBuilder.buildString()
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") _: Exception,
        ) {
            null
        }

    /**
     * Find the route definition that matches the given AppRoute instance.
     */
    private fun findRouteDefinition(route: AppRoute) = routingTable.findByClass(route::class)

    /**
     * Extract parameters from an AppRoute instance using serialization.
     * Excludes null values and metadata fields.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun <T : AppRoute> extractRouteParams(
        routeDefinition: RouteDefinition<T>,
        route: T,
    ): Map<String, String> =
        try {
            // Safe cast to handle generic variance
            val serializer = routeDefinition.serializer as kotlinx.serialization.KSerializer<T>
            val map = Properties.encodeToMap(serializer, route)

            // Convert map values to strings, excluding metadata and nulls
            map
                .mapNotNull { (key, value) ->
                    if (key == "metadata") {
                        null
                    } else {
                        key to value.toString()
                    }
                }.toMap()
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") _: Exception,
        ) {
            emptyMap()
        }
}
