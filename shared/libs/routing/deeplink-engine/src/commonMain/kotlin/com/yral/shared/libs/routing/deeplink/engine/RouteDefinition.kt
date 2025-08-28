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
     * A stable, unique identifier for the route derived from the serializer descriptor's serial name.
     * This value is stable across obfuscation and platforms and is used for disambiguating
     * map-based parsing.
     */
    val routeId: String = serializer.descriptor.serialName
}
