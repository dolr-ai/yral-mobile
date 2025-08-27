import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * Represents a route definition that maps URL patterns to AppRoute classes.
 */
data class RouteDefinition<out R : AppRoute>(
    val routeClass: KClass<out R>,
    val pattern: String,
    val serializer: KSerializer<out R>,
)
