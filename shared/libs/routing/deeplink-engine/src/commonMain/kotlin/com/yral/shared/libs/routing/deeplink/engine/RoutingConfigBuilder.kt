package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * A builder class for creating routing configuration using a type-safe DSL.
 */
class RoutingConfigBuilder {
    private val routes = mutableListOf<RouteDefinition<out AppRoute>>()

    /**
     * Register a route with its pattern and serializer.
     */
    fun <T : AppRoute> route(
        routeClass: KClass<T>,
        pattern: String,
        serializer: KSerializer<T>,
    ) {
        routes.add(RouteDefinition(routeClass, RoutePattern(pattern), serializer))
    }

    /**
     * Register a route with its pattern using reified type parameter for better ergonomics.
     */
    inline fun <reified T : AppRoute> route(pattern: String) {
        route(T::class, pattern, serializer())
    }

    /**
     * Register a route with its pattern and explicit serializer using reified type parameter.
     */
    inline fun <reified T : AppRoute> route(pattern: String, serializer: KSerializer<T>) {
        route(T::class, pattern, serializer)
    }

    /**
     * Build the routing table from the configured routes.
     */
    fun build(): List<RouteDefinition<out AppRoute>> = routes.toList()
}

/**
 * Top-level function to create a routing configuration using DSL.
 */
fun buildRouting(block: RoutingConfigBuilder.() -> Unit): List<RouteDefinition<out AppRoute>> {
    val builder = RoutingConfigBuilder()
    builder.block()
    return builder.build()
}

/**
 * A convenience function that builds a [RoutingTable] directly from the DSL.
 * This is the recommended way to create a routing table for tests and DI.
 */
fun buildRoutingTable(block: RoutingConfigBuilder.() -> Unit): RoutingTable {
    return RoutingTable(buildRouting(block))
}
