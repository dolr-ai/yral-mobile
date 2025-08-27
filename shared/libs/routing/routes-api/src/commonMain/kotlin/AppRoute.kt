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

// --- Test Route Definitions for Testing ---

@Serializable
data class TestProductRoute(
    val productId: String,
    val category: String? = null,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute, ExternallyExposedRoute

@Serializable
data class TestUserRoute(
    val userId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute, ExternallyExposedRoute

@Serializable
object TestHomeRoute : AppRoute {
    @Transient override val metadata: Map<String, Any> = emptyMap()
}

@Serializable
data class TestInternalRoute(
    val internalId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap(),
) : AppRoute // Note: does NOT implement ExternallyExposedRoute

@Serializable
object TestUnknownRoute : AppRoute {
    @Transient override val metadata: Map<String, Any> = emptyMap()
}
