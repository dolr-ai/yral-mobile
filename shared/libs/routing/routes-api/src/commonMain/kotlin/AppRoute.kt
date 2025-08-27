import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents every possible navigation destination in the app.
 * Also carries optional, non-navigational metadata for analytics.
 */
sealed interface AppRoute {
    @Transient
    val metadata: Map<String, Any>
}

// --- Example Route Definitions ---

@Serializable
data class ProductDetails(
    val productId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute, ExternallyExposedRoute

@Serializable
object Home : AppRoute {
    @Transient override val metadata: Map<String, Any> = emptyMap()
}

@Serializable
object Unknown : AppRoute {
    @Transient override val metadata: Map<String, Any> = emptyMap()
}
