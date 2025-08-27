import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * A builder class for creating routing configuration using a type-safe DSL.
 */
class RoutingConfigBuilder<R : AppRoute> {
    private val routes = mutableListOf<RouteDefinition<R>>()

    /**
     * Register a route with its pattern and serializer.
     */
    fun <T : R> route(
        routeClass: KClass<T>,
        pattern: String,
        serializer: KSerializer<T>,
    ) {
        routes.add(RouteDefinition(routeClass, pattern, serializer))
    }

    /**
     * Register a route with its pattern using reified type parameter for better ergonomics.
     */
    inline fun <reified T : R> route(pattern: String) {
        route(T::class, pattern, serializer())
    }

    /**
     * Register a route with its pattern and explicit serializer using reified type parameter.
     */
    inline fun <reified T : R> route(pattern: String, serializer: KSerializer<T>) {
        route(T::class, pattern, serializer)
    }

    /**
     * Build the routing table from the configured routes.
     */
    fun build(): List<RouteDefinition<R>> = routes.toList()
}

/**
 * Top-level function to create a routing configuration using DSL.
 */
fun <R : AppRoute> buildRouting(block: RoutingConfigBuilder<R>.() -> Unit): List<RouteDefinition<R>> {
    val builder = RoutingConfigBuilder<R>()
    builder.block()
    return builder.build()
}
