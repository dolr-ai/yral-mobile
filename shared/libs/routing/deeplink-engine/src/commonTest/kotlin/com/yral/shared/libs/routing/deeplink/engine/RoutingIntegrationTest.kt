package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.TestHomeRoute
import com.yral.shared.libs.routing.routes.api.TestInternalRoute
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import com.yral.shared.libs.routing.routes.api.Unknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingIntegrationTest {

    private val routingTable = buildRouting<AppRoute> {
        route<TestProductRoute>("/product/{productId}")
        route<TestUserRoute>("/user/{userId}")
        route<TestHomeRoute>("/")
    }

    private val parser = DeepLinkParser(routingTable)
    private val urlBuilder = UrlBuilder(
        routingTable = routingTable,
        scheme = "https",
        host = "example.com"
    )

    @Test
    fun testRoundTripProductRoute() {
        val originalRoute = TestProductRoute("123", "electronics")
        
        // Build URL from route
        val url = urlBuilder.build(originalRoute)
        assertEquals("https://example.com/product/123", url)
        
        // Parse URL back to route
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestProductRoute)
        assertEquals("123", parsedRoute.productId)
        // Note: category is not preserved in URL pattern, so it won't be in parsed route
    }

    @Test
    fun testRoundTripUserRoute() {
        val originalRoute = TestUserRoute("user456")
        
        // Build URL from route
        val url = urlBuilder.build(originalRoute)
        assertEquals("https://example.com/user/user456", url)
        
        // Parse URL back to route
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestUserRoute)
        assertEquals("user456", parsedRoute.userId)
    }

    @Test
    fun testRoundTripHomeRoute() {
        val originalRoute = TestHomeRoute
        
        // Build URL from route
        val url = urlBuilder.build(originalRoute)
        assertEquals("https://example.com/", url)
        
        // Parse URL back to route
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestHomeRoute)
    }

    @Test
    fun testParseUrlWithQueryParameters() {
        // Test URL with query parameters that should be mapped to route properties
        val url = "https://example.com/product/789?category=books"
        val parsedRoute = parser.parse(url)
        
        assertTrue(parsedRoute is TestProductRoute)
        assertEquals("789", parsedRoute.productId)
        assertEquals("books", parsedRoute.category)
    }

    @Test
    fun testMultipleRoutesInSameSession() {
        val productRoute = TestProductRoute("product123")
        val userRoute = TestUserRoute("user456")
        val homeRoute = TestHomeRoute
        
        // Build URLs
        val productUrl = urlBuilder.build(productRoute)
        val userUrl = urlBuilder.build(userRoute)
        val homeUrl = urlBuilder.build(homeRoute)
        
        assertEquals("https://example.com/product/product123", productUrl)
        assertEquals("https://example.com/user/user456", userUrl)
        assertEquals("https://example.com/", homeUrl)
        
        // Parse URLs back
        val parsedProduct = parser.parse(productUrl!!)
        val parsedUser = parser.parse(userUrl!!)
        val parsedHome = parser.parse(homeUrl!!)
        
        assertTrue(parsedProduct is TestProductRoute)
        assertTrue(parsedUser is TestUserRoute)
        assertTrue(parsedHome is TestHomeRoute)
        
        assertEquals("product123", (parsedProduct as TestProductRoute).productId)
        assertEquals("user456", (parsedUser as TestUserRoute).userId)
    }

    @Test
    fun testDifferentSchemesAndHosts() {
        val customUrlBuilder = UrlBuilder(
            routingTable = routingTable,
            scheme = "myapp",
            host = "deeplinks"
        )
        
        val route = TestProductRoute("custom123")
        val url = customUrlBuilder.build(route)
        
        assertEquals("myapp://deeplinks/product/custom123", url)
        
        // Parser should work regardless of scheme/host
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestProductRoute)
        assertEquals("custom123", parsedRoute.productId)
    }

    @Test
    fun testSecurityModelIntegration() {
        val routingTableWithInternal = buildRouting<AppRoute> {
            route<TestProductRoute>("/product/{productId}")
            route<TestInternalRoute>("/internal/{internalId}")
        }
        
        val parserWithInternal = DeepLinkParser(routingTableWithInternal)
        val builderWithInternal = UrlBuilder(
            routingTable = routingTableWithInternal,
            scheme = "https",
            host = "example.com"
        )
        
        val internalRoute = TestInternalRoute("secret")
        
        // URL builder should still work (no security check in builder)
        val url = builderWithInternal.build(internalRoute)
        assertEquals("https://example.com/internal/secret", url)
        
        // But parser should reject it due to security check
        val parsedRoute = parserWithInternal.parse(url!!)
        assertTrue(parsedRoute is Unknown)
    }
}
