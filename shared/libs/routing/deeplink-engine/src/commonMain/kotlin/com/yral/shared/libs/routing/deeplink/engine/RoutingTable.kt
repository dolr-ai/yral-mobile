package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import kotlin.reflect.KClass

/**
 * A dedicated class to hold the routing table and provide efficient lookups.
 *
 * This class pre-processes the list of route definitions into maps for
 * fast lookups by route ID and route class, optimizing performance.
 *
 * @param definitions The raw list of route definitions from the DSL.
 */
class RoutingTable(
    definitions: List<RouteDefinition<out AppRoute>>,
) {
    private val definitionsById: Map<String, RouteDefinition<out AppRoute>> =
        definitions.associateBy { it.routeId }

    private val definitionsByClass: Map<KClass<out AppRoute>, RouteDefinition<out AppRoute>> =
        definitions.associateBy { it.routeClass }

    /**
     * The original, ordered list of all route definitions.
     * Used for path matching where order might be important.
     */
    val all: List<RouteDefinition<out AppRoute>> = definitions

    /**
     * Finds a route definition by its unique route ID. O(1) complexity.
     *
     * @param routeId The simple class name of the route.
     * @return The matching [RouteDefinition] or null if not found.
     */
    fun findById(routeId: String): RouteDefinition<out AppRoute>? = definitionsById[routeId]

    /**
     * Finds a route definition by its KClass. O(1) complexity.
     *
     * @param routeClass The KClass of the route.
     * @return The matching [RouteDefinition] or null if not found.
     */
    fun findByClass(routeClass: KClass<out AppRoute>): RouteDefinition<out AppRoute>? =
        definitionsByClass[routeClass]
}
