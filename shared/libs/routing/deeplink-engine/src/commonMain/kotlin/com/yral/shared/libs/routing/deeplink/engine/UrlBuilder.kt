package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.parameters
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
    fun build(route: AppRoute): String? = buildUrlFromRoute(route)

    /**
     * Build a URL from a route using type-safe serialization.
     */
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

            // Let RoutePattern produce both path segments and query params
            val components = routeDefinition.pattern.buildComponents(params)
            val pathSegments = components.pathSegments
            val filteredQueryParams = components.queryParams

            // For host-less deep links (custom scheme with no explicit host):
            // If host is blank and there is at least one path segment, treat the
            // first segment as the host (authority) and the remaining as the path.
            // Apply this only for non-HTTP(S) schemes.
            var finalHost = host
            var finalSegments = pathSegments
            if (shouldTreatFirstSegmentAsHost(finalHost, scheme, finalSegments)) {
                finalHost = finalSegments.first()
                finalSegments = finalSegments.drop(1)
            }

            // Build the complete URL
            val urlBuilder =
                URLBuilder(
                    protocol = URLProtocol.createOrDefault(scheme),
                    host = finalHost,
                    pathSegments = finalSegments,
                    parameters =
                        parameters {
                            filteredQueryParams.forEach { (key, value) ->
                                append(key, value)
                            }
                        },
                )
            urlBuilder.buildString()
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") _: Exception,
        ) {
            null
        }

    private fun shouldTreatFirstSegmentAsHost(
        currentHost: String,
        currentScheme: String,
        segments: List<String>,
    ): Boolean {
        val hostBlank = currentHost.isBlank()
        val hasSegments = segments.isNotEmpty()
        val isHttp = currentScheme.equals("http", ignoreCase = true)
        val isHttps = currentScheme.equals("https", ignoreCase = true)
        val isWebScheme = isHttp || isHttps
        return hostBlank && hasSegments && !isWebScheme
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
                    if (key == "metadata" || value == null) {
                        null
                    } else {
                        val s = value.toString()
                        if (s.isBlank() || s == "null") null else key to s
                    }
                }.toMap()
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") _: Exception,
        ) {
            emptyMap()
        }
}
