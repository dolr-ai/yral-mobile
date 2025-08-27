import AppRoute

/**
 * A high-level service interface for all routing operations.
 * This is the primary API that feature modules should interact with.
 */
interface RoutingService {
    /**
     * Parse a URL into a type-safe route.
     */
    fun parseUrl(url: String): AppRoute

    /**
     * Parse URL parameters into a type-safe route.
     * Requires a "route_id" parameter in the map.
     */
    fun parseParameters(params: Map<String, String>): AppRoute

    /**
     * Generate a URL from a type-safe route.
     */
    fun buildUrl(route: AppRoute): String?
}
