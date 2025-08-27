package com.yral.shared.app.di

import AppRoute
import DeepLinkParser
import ExternallyExposedRoute
import Home
import TestProductRoute
import TestUserRoute
import Unknown
import UrlBuilder
import buildRouting
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin DI Module for Deep Link & Routing Framework
 * 
 * This demonstrates Phase 4 integration - how to set up the routing framework
 * in your application's dependency injection container.
 */

/**
 * Step 1: Build the complete application routing table using the DSL
 * 
 * This is where you register all routes from all feature modules.
 * Each feature defines its routes, but the app module is responsible
 * for building the complete routing table.
 */
val appRoutingTable = buildRouting<AppRoute> {
    // Core app routes
    route<Home>("/")
    route<Unknown>("/unknown")
    
    // Test routes (for development/testing)
    route<TestProductRoute>("/test/product/{productId}")
    route<TestUserRoute>("/test/user/{userId}")

    // Additional feature routes would be added here as features are developed
    // route<UserProfileRoute>("/user/{userId}/profile")
    // route<OrderDetailsRoute>("/order/{orderId}")
    // route<SearchResultsRoute>("/search")
}

/**
 * Step 2: Create the Koin module that provides routing components as singletons
 */
val routingModule = module {
    
    /**
     * Provide the DeepLinkParser as a singleton
     * This is the main component that parses URLs into type-safe AppRoute objects
     */
    single<DeepLinkParser<AppRoute>> {
        DeepLinkParser(
            routingTable = appRoutingTable
        )
    }
    
    /**
     * Provide the UrlBuilder as a singleton
     * This component generates URLs from type-safe AppRoute objects
     */
    single<UrlBuilder<AppRoute>> {
        UrlBuilder(
            routingTable = appRoutingTable,
            scheme = getProperty("deep_link_scheme", "https"),
            host = getProperty("deep_link_host", "yral.app")
        )
    }
    
    /**
     * Optional: Provide a higher-level routing service that combines both components
     */
    singleOf(::RoutingService)
}

/**
 * Step 3: Higher-level service that demonstrates how to use the routing components together
 */
class RoutingService(
    private val deepLinkParser: DeepLinkParser<AppRoute>,
    private val urlBuilder: UrlBuilder<AppRoute>
) {
    
    /**
     * Parse a URL into a type-safe route
     */
    fun parseUrl(url: String): AppRoute {
        return deepLinkParser.parse(url)
    }
    
    /**
     * Parse URL parameters into a type-safe route
     */
    fun parseParameters(params: Map<String, String>): AppRoute {
        return deepLinkParser.parse(params)
    }
    
    /**
     * Generate a URL from a type-safe route
     */
    fun buildUrl(route: AppRoute): String? {
        return urlBuilder.build(route)
    }
    
    /**
     * Generate a shareable URL for a given route
     * This is useful for social sharing, deep link generation, etc.
     */
    fun createShareableUrl(route: AppRoute): String {
        return buildUrl(route) ?: "https://yral.app"
    }
    
    /**
     * Validate that a route can be externally accessed (security check)
     */
    fun isExternallyAccessible(route: AppRoute): Boolean {
        return route is ExternallyExposedRoute
    }
}

/**
 * Step 4: Application-level configuration
 * 
 * This shows how to configure the routing system with environment-specific settings
 */
object RoutingConfig {
    
    /**
     * Configuration for different environments
     */
    data class Environment(
        val scheme: String,
        val host: String,
        val enableAnalytics: Boolean = true
    )
    
    val development = Environment(
        scheme = "yral-dev",
        host = "dev.yral.app"
    )
    
    val staging = Environment(
        scheme = "yral-staging", 
        host = "staging.yral.app"
    )
    
    val production = Environment(
        scheme = "https",
        host = "yral.app"
    )
    
    /**
     * Get configuration based on build variant
     */
    fun getEnvironment(buildType: String): Environment {
        return when (buildType.lowercase()) {
            "debug" -> development
            "staging" -> staging
            "release", "prod" -> production
            else -> development
        }
    }
}
