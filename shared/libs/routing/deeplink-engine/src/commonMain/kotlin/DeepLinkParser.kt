import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The central engine for parsing URLs. It is initialized with a pre-built
 * routing table created by the configuration DSL.
 */
class DeepLinkParser<R : AppRoute>(
    private val routingTable: List<RouteDefinition<R>>,
) {
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
        for (routeDefinition in routingTable) {
            val extractedParams = extractParameters(routeDefinition.pattern, pathSegments, queryParams)
            if (extractedParams != null) {
                val route = deserializeRoute(routeDefinition, extractedParams)
                if (route != null && isExternallyExposed(route)) {
                    return route
                }
            }
        }
        return null
    }

    /**
     * Parse a map of parameters into a type-safe AppRoute object.
     * Returns AppRoute.Unknown if the parameters cannot be parsed or if the resulting
     * route does not implement ExternallyExposedRoute.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun parse(params: Map<String, String>): AppRoute {
        // Try to match against each route definition
        for (routeDefinition in routingTable) {
            try {
                val route = deserializeRoute(routeDefinition, params)
                if (route != null && isExternallyExposed(route)) {
                    return route
                }
            } catch (e: Exception) {
                // Continue to next route definition
                continue
            }
        }

        return Unknown
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
    private fun deserializeRoute(
        routeDefinition: RouteDefinition<R>,
        params: Map<String, String>,
    ): AppRoute? {
        return try {
            // Convert parameters to JSON for serialization
            val jsonParams = params.mapValues { JsonPrimitive(it.value) }
            val jsonObject = JsonObject(jsonParams)

            Json.decodeFromJsonElement(routeDefinition.serializer, jsonObject) as AppRoute
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
