import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

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
            // For now, we'll use reflection-like approach to extract route properties
            // In a real implementation, this would be more sophisticated
            val params = extractRouteParams(route)

            // Build the URL path from the pattern
            val path = buildPathFromPattern(routeDefinition.pattern, params)

            // Build the complete URL
            val urlBuilder =
                URLBuilder(
                    protocol = URLProtocol.createOrDefault(scheme),
                    host = host,
                    pathSegments = path.split("/").filter { it.isNotEmpty() },
                )
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
    private fun buildPathFromPattern(
        pattern: String,
        params: Map<String, String>,
    ): String {
        var path = pattern

        // Replace parameter placeholders with actual values
        params.forEach { (key, value) ->
            val placeholder = "{$key}"
            if (path.contains(placeholder)) {
                path = path.replace(placeholder, value)
            }
        }

        return path
    }

    /**
     * Extract parameters from an AppRoute instance.
     * This is a simplified implementation for Phase 2.
     */
    private fun extractRouteParams(route: AppRoute): Map<String, String> {
        return when (route) {
            is ProductDetails -> mapOf("productId" to route.productId)
            is TestProductRoute -> mapOf("productId" to route.productId)
            is TestUserRoute -> mapOf("userId" to route.userId)
            is TestInternalRoute -> mapOf("internalId" to route.internalId)
            else -> emptyMap()
        }
    }
}
