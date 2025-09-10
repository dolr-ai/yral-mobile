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
-   **Best Practice:** Define the URL path pattern as a `const val PATH` inside a companion object. This co-locates the route's structure with its path, preventing magic strings and improving maintainability.
-   If it can be triggered from an external link, it **must** implement `ExternallyExposedRoute`.
-   If it needs to carry analytics metadata, it **must** implement `AppRouteWithMetadata`.

**Example:** Adding a new `UserProfile` route.

```kotlin
// In AppRoute.kt

@Serializable
data class UserProfile(
    val userId: String,
    @Transient override val metadata: Map<String, Any> = emptyMap()
) : AppRouteWithMetadata, ExternallyExposedRoute {
    companion object {
        const val PATH = "/user/{userId}"
    }
}
```

### Step 2: Register the Route in `:app`

Open the `routingModule` in the `:app` module (e.g., `app/src/main/java/.../di/RoutingModule.kt`) and add a new entry to the routing table, referencing the `PATH` constant from your route class.

Path parameters in the pattern are enclosed in `{}`. Any properties of the data class not found in the path are automatically treated as query parameters.

**Example:** Registering the new `UserProfile` route.

```kotlin
// In RoutingModule.kt

buildRoutingTable {
    route<ProductDetails>(ProductDetails.PATH)
    route<Home>(Home.PATH)
    
    // Add the new route here
    route<UserProfile>(UserProfile.PATH)

    // ... other routes
}
```

That's it. The `DeepLinkParser` will now be able to parse `/user/123` into a `UserProfile("123")` object, and the `UrlBuilder` will be able to do the reverse.

---

## How to Use the Framework

The `RoutingService` is provided as a singleton by Koin. You can inject it anywhere in your application to handle all routing-related tasks.

### Using the `RoutingService` to Parse URLs

Inject `RoutingService` in the Activity or platform-specific class responsible for receiving incoming links.

```kotlin
// In an Android Activity that receives the deep link
class DeepLinkActivity : AppCompatActivity(), KoinComponent {

    private val routingService: RoutingService by inject()
    private val analytics: Analytics by inject() // Your analytics service

    private fun handleIntent(intent: Intent?) {
        val url = intent?.data?.toString() ?: return
        
        // Use the injected service to parse the URL
        val route = routingService.parseUrl(url)

        // It is critical to log parsing failures
        if (route is Unknown) {
            analytics.logEvent("deep_link_parse_failed", mapOf("url" to url))
        }

        // Pass the typed route to your navigation logic
        navigator.navigateTo(route)
    }
}
```

### Using the `RoutingService` to Build URLs

Inject `RoutingService` into any ViewModel or class that needs to generate a shareable link.

```kotlin
// In a feature's ViewModel
class ProfileViewModel(
    private val routingService: RoutingService // Injected by Koin
) : ViewModel() {

    fun onShareProfile(userId: String) {
        // Create a type-safe route object
        val route = UserProfile(userId = userId)
        
        // Use the injected service to generate the URL
        val urlToShare = routingService.buildUrl(route)
        
        // ... now trigger a share action with this URL
    }
}
```
