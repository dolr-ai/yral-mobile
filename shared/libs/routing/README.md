# Deep Link & Routing Framework

This document serves as the developer guide for the type-safe, secure, and modular deep link and routing framework.

## Overview

The framework is composed of two main modules:

1.  **:routes-api**: A lightweight "contract" module that contains the public API for all navigation routes. It defines the `AppRoute` sealed interface and all of its concrete data class implementations.
2.  **:deeplink-engine**: The "implementation" module that contains the core logic. It provides the `DeepLinkParser` for parsing URLs and the `UrlBuilder` for generating them.

The entire system is configured via a type-safe DSL in a central Koin module, leveraging `Ktor-HTTP` for robust URL handling and `kotlinx.serialization` for automatic, type-safe object construction.

---

## How to Add a New Route

Adding a new, fully functional route is a simple, two-step process.

### Step 1: Define the Route in `:routes-api`

Open `shared/libs/routing/routes-api/src/commonMain/kotlin/AppRoute.kt` and add your new data class as an implementation of the `AppRoute` sealed interface.

-   It **must** be annotated with `@Serializable`.
-   If it can be triggered from an external link, it **must** implement `ExternallyExposedRoute`.
-   If it needs to carry analytics metadata, it **must** implement `AppRouteWithMetadata`.

**Example:** Adding a new `UserProfile` route.

```kotlin
// In AppRoute.kt

@Serializable
data class UserProfile(
    val userId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap()
) : AppRouteWithMetadata, ExternallyExposedRoute
```

### Step 2: Register the Route in `:app`

Open the `routingModule` in the `:app` module (e.g., `app/src/main/java/.../di/RoutingModule.kt`) and add a new entry to the routing table using the type-safe DSL.

The string pattern defines the URL structure. Path parameters are enclosed in `{}`. Any properties of the data class not found in the path are automatically treated as query parameters.

**Example:** Registering the new `UserProfile` route.

```kotlin
// In RoutingModule.kt

val appRoutingTable = buildRouting<AppRoute> {
    route<ProductDetails>("/product/{productId}")
    route<Home>("/home")
    
    // Add the new route here
    route<UserProfile>("/user/{userId}")

    // ... other routes
}
```

That's it. The `DeepLinkParser` will now be able to parse `/user/123` into a `UserProfile("123")` object, and the `UrlBuilder` will be able to do the reverse.

---

## How to Use the Framework

The `DeepLinkParser` and `UrlBuilder` are provided as singletons by Koin. You can inject them anywhere in your application.

### Using the `DeepLinkParser`

Inject `DeepLinkParser` in the Activity or platform-specific class responsible for receiving incoming links.

```kotlin
// In an Android Activity that receives the deep link
class DeepLinkActivity : AppCompatActivity(), KoinComponent {

    private val deepLinkParser: DeepLinkParser<AppRoute> by inject()
    private val analytics: Analytics by inject() // Your analytics service

    private fun handleIntent(intent: Intent?) {
        val url = intent?.data?.toString() ?: return
        
        // Use the injected parser
        val route = deepLinkParser.parse(url)

        // It is critical to log parsing failures
        if (route is Unknown) {
            analytics.logEvent("deep_link_parse_failed", mapOf("url" to url))
        }

        // Pass the typed route to your navigation logic
        navigator.navigateTo(route)
    }
}
```

### Using the `UrlBuilder`

Inject `UrlBuilder` into any ViewModel or class that needs to generate a shareable link.

```kotlin
// In a feature's ViewModel
class ProfileViewModel(
    private val urlBuilder: UrlBuilder<AppRoute> // Injected by Koin
) : ViewModel() {

    fun onShareProfile(userId: String) {
        // Create a type-safe route object
        val route = UserProfile(userId = userId)
        
        // Use the injected builder to generate the URL
        val urlToShare = urlBuilder.build(route)
        
        // ... now trigger a share action with this URL
    }
}
```
