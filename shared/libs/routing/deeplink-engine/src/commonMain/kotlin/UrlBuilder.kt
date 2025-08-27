/**
 * The engine for generating URL strings from type-safe AppRoute objects.
 */
@Suppress("UnusedPrivateProperty")
class UrlBuilder<R : AppRoute>(
    private val routingTable: List<RouteDefinition<R>>,
    private val scheme: String,
    private val host: String,
) {
    /**
     * Build a URL string from a type-safe AppRoute object.
     * Returns null if the route cannot be built into a URL.
     */
    @Suppress("UnusedParameter", "ForbiddenComment", "FunctionOnlyReturningConstant")
    fun build(route: AppRoute): String? {
        // TODO: Implement URL building logic in Phase 2
        return null
    }
}
