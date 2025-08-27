package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The engine for generating URL strings from type-safe AppRoute objects.
 */
class UrlBuilder<R : AppRoute>(
    private val routingTable: List<RouteDefinition<R>>,
    private val scheme: String,
    private val host: String,
) {
    /**
     * Build a URL string from a type-safe AppRoute object.
     * Returns null if the route cannot be built into a URL.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun build(route: AppRoute): String? {
        // Find the matching route definition
        val routeDefinition = findRouteDefinition(route) ?: return null

        return buildUrlFromRoute(routeDefinition, route)
    }

    @Suppress(
        "UNCHECKED_CAST",
        "TooGenericExceptionCaught",
        "SwallowedException",
    )
    private fun buildUrlFromRoute(
        routeDefinition: RouteDefinition<R>,
        route: AppRoute,
    ): String? {
        return try {
            // Use serialization to automatically extract route properties.
            val params = extractRouteParams(routeDefinition, route)

            // Build the URL path from the pattern
            val (path, queryParams) = buildPathAndQuery(routeDefinition.pattern, params)

            // Build the complete URL
            val urlBuilder =
                URLBuilder(
                    protocol = URLProtocol.createOrDefault(scheme),
                    host = host,
                    pathSegments = path.split("/").filter { it.isNotEmpty() },
                )
            // Add query parameters
            queryParams.forEach { (key, value) ->
                urlBuilder.parameters.append(key, value)
            }
            urlBuilder.buildString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find the route definition that matches the given AppRoute instance.
     */
    private fun findRouteDefinition(route: AppRoute): RouteDefinition<R>? {
        return routingTable.find { definition ->
            definition.routeClass == route::class
        }
    }

    /**
     * Build the URL path from the pattern by substituting parameter placeholders.
     */
    private fun buildPathAndQuery(
        pattern: String,
        params: Map<String, String>,
    ): Pair<String, Map<String, String>> {
        var path = pattern
        val queryParams = mutableMapOf<String, String>()

        // Replace parameter placeholders with actual values
        params.forEach { (key, value) ->
            val placeholder = "{$key}"
            if (path.contains(placeholder)) {
                path = path.replace(placeholder, value)
            } else {
                // If the parameter is not in the path, it's a query parameter
                queryParams[key] = value
            }
        }

        return Pair(path, queryParams)
    }

    /**
     * Extract parameters from an AppRoute instance using serialization.
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractRouteParams(
        routeDefinition: RouteDefinition<R>,
        route: AppRoute,
    ): Map<String, String> {
        val serializer = routeDefinition.serializer as kotlinx.serialization.KSerializer<AppRoute>
        val jsonElement = Json.encodeToJsonElement(serializer, route)
        if (jsonElement !is JsonObject) return emptyMap()

        return jsonElement.entries.associate { (key, value) ->
            key to value.jsonPrimitive.content
        }
    }
}
