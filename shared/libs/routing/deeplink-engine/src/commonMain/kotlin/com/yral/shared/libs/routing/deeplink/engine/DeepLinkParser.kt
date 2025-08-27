package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.ExternallyExposedRoute
import com.yral.shared.libs.routing.routes.api.Unknown
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The central engine for parsing URLs. It is initialized with a pre-built
 * routing table created by the configuration DSL.
 * 
 * All operations use proper generic constraints for type safety and 
 * compile-time error checking.
 */
class DeepLinkParser(
    private val routingTable: RoutingTable,
) {
    
    /**
     * Type constraint helper to ensure route definitions are valid.
     */
    init {
        require(routingTable.all.isNotEmpty()) { "Routing table cannot be empty" }
        // Verify all route IDs are unique
        val routeIds = routingTable.all.map { it.routeId }
        require(routeIds.size == routeIds.distinct().size) { 
            "Route IDs must be unique. Duplicates: ${routeIds.groupBy { it }.filter { it.value.size > 1 }.keys}" 
        }
    }
    /**
     * Parse a URL string into a type-safe AppRoute object.
     * Returns AppRoute.Unknown if the URL cannot be parsed or if the resulting
     * route does not implement ExternallyExposedRoute.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun parse(url: String): AppRoute {
        return parseUrlInternal(url)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun parseUrlInternal(url: String): AppRoute {
        // Handle empty or whitespace-only URLs
        if (url.isBlank()) {
            return Unknown
        }
        
        return try {
            val parsedUrl = Url(url)
            val pathSegments = parsedUrl.segments
            val queryParams = mutableMapOf<String, List<String>>()
            parsedUrl.parameters.forEach { key, values ->
                queryParams[key] = values
            }

            findMatchingRoute(pathSegments, queryParams) ?: Unknown
        } catch (e: Exception) {
            Unknown
        }
    }

    private fun findMatchingRoute(
        pathSegments: List<String>,
        queryParams: Map<String, List<String>>,
    ): AppRoute? {
        for (routeDefinition in routingTable.all) {
            val extractedParams = extractParameters(routeDefinition.pattern, pathSegments, queryParams)
            if (extractedParams != null) {
                val result = parseWithRouteDefinition(routeDefinition, extractedParams)
                if (result !is Unknown) {
                    return result
                }
            }
        }
        return null
    }

    /**
     * Parse a map of parameters into a type-safe AppRoute object.
     * A "route_id" parameter must be provided in the map to specify
     * which route to deserialize into.
     *
     * Returns AppRoute.Unknown if the parameters cannot be parsed, if "route_id" is missing,
     * or if the resulting route does not implement ExternallyExposedRoute.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun parse(params: Map<String, String>): AppRoute {
        val routeId = params["route_id"] ?: return Unknown

        // Use route_id for direct lookup
        return parseWithRouteId(params, routeId)
    }

    /**
     * Parse parameters using a specific route_id.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun parseWithRouteId(params: Map<String, String>, routeId: String): AppRoute {
        val routeDefinition = routingTable.findById(routeId) ?: return Unknown

        return try {
            parseWithRouteDefinition(routeDefinition, params)
        } catch (e: Exception) {
            Unknown
        }
    }
    
    /**
     * Parse parameters with a specific route definition.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun <T : AppRoute> parseWithRouteDefinition(
        routeDefinition: RouteDefinition<T>,
        params: Map<String, String>,
    ): AppRoute {
        return try {
            val route = deserializeRoute(routeDefinition, params)
            if (route != null && isExternallyExposed(route)) {
                route
            } else {
                Unknown
            }
        } catch (e: Exception) {
            Unknown
        }
    }

    /**
     * Extract parameters from URL path and query based on the route pattern.
     * Returns null if the pattern doesn't match.
     */
    @Suppress("ReturnCount")
    private fun extractParameters(
        pattern: String,
        pathSegments: List<String>,
        queryParams: Map<String, List<String>>,
    ): Map<String, String>? {
        val patternSegments = pattern.split("/").filter { it.isNotEmpty() }

        if (pathSegments.size != patternSegments.size) {
            return null
        }

        val extractedParams = mutableMapOf<String, String>()

        // Extract path parameters
        for (i in patternSegments.indices) {
            val patternSegment = patternSegments[i]
            val pathSegment = pathSegments[i]

            when {
                patternSegment.startsWith("{") && patternSegment.endsWith("}") -> {
                    // This is a parameter placeholder
                    val paramName = patternSegment.substring(1, patternSegment.length - 1)
                    extractedParams[paramName] = pathSegment
                }
                patternSegment != pathSegment -> {
                    // Static segment doesn't match
                    return null
                }
            }
        }

        // Add query parameters
        queryParams.forEach { (key, values) ->
            if (values.isNotEmpty()) {
                extractedParams[key] = values.first()
            }
        }

        return extractedParams
    }

    /**
     * Deserialize parameters into an AppRoute object using the route's serializer.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun <T : AppRoute> deserializeRoute(
        routeDefinition: RouteDefinition<T>,
        params: Map<String, String>,
    ): T? {
        return try {
            // Filter out route_id from params for serialization
            val filteredParams = params.filter { it.key != "route_id" }
            
            // Convert parameters to JSON for serialization
            val jsonParams = filteredParams.mapValues { JsonPrimitive(it.value) }
            val jsonObject = JsonObject(jsonParams)

            // No cast needed - serializer returns T directly
            Json.decodeFromJsonElement(routeDefinition.serializer, jsonObject)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if the route implements ExternallyExposedRoute (security check).
     */
    private fun isExternallyExposed(route: AppRoute): Boolean {
        return route is ExternallyExposedRoute
    }
}
