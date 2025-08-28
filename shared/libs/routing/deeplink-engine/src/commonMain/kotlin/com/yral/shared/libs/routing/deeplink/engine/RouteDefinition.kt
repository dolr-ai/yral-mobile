package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * Represents a route definition that maps URL patterns to AppRoute classes.
 */
data class RouteDefinition<out R : AppRoute>(
    val routeClass: KClass<out R>,
    val pattern: RoutePattern,
    val serializer: KSerializer<out R>,
) {
    /**
     * A unique identifier for the route, typically the simple name of the class.
     * Used for disambiguating map-based parsing.
     */
    val routeId: String = routeClass.simpleName ?: ""
}
