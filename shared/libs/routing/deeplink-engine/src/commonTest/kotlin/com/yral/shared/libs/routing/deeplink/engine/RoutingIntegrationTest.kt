package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.TestHomeRoute
import com.yral.shared.libs.routing.routes.api.TestInternalRoute
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import com.yral.shared.libs.routing.routes.api.Unknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingIntegrationTest {
    private val routingTable =
        buildRoutingTable {
            route<TestProductRoute>(TestProductRoute.PATH)
            route<TestUserRoute>(TestUserRoute.PATH)
            route<TestHomeRoute>(TestHomeRoute.PATH)
        }

    private val parser = DeepLinkParser(routingTable)
    private val urlBuilder =
        UrlBuilder(
            routingTable = routingTable,
            scheme = "https",
            host = "example.com",
        )

    @Test
    fun testRoundTripProductRoute() {
        val originalRoute = TestProductRoute("123", "electronics")

        // Build URL from route
        val url = urlBuilder.build(originalRoute)
        assertEquals(
            "https://example.com${TestProductRoute.PATH.replace(
                "{productId}",
                "123",
            )}?category=electronics",
            url,
        )

        // Parse URL back to route
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestProductRoute)
        assertEquals("123", parsedRoute.productId)
        assertEquals("electronics", parsedRoute.category) // Now category should be preserved
    }

    @Test
    fun testRoundTripUserRoute() {
        val originalRoute = TestUserRoute("user456")

        // Build URL from route
        val url = urlBuilder.build(originalRoute)
        assertEquals("https://example.com${TestUserRoute.PATH.replace("{userId}", "user456")}", url)

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
        assertEquals("https://example.com", url)

        // Parse URL back to route
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestHomeRoute)
    }

    @Test
    fun testParseUrlWithQueryParameters() {
        // Test URL with query parameters that should be mapped to route properties
        val url = "https://example.com/test/product/789?category=books"
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

        assertEquals("https://example.com/test/product/product123", productUrl)
        assertEquals("https://example.com/test/user/user456", userUrl)
        assertEquals("https://example.com", homeUrl)

        // Parse URLs back
        val parsedProduct = parser.parse(productUrl!!)
        val parsedUser = parser.parse(userUrl!!)
        val parsedHome = parser.parse(homeUrl!!)

        assertTrue(parsedProduct is TestProductRoute)
        assertTrue(parsedUser is TestUserRoute)
        assertTrue(parsedHome is TestHomeRoute)

        assertEquals("product123", parsedProduct.productId)
        assertEquals("user456", parsedUser.userId)
    }

    @Test
    fun testDifferentSchemesAndHosts() {
        val customUrlBuilder =
            UrlBuilder(
                routingTable = routingTable,
                scheme = "myapp",
                host = "deeplinks",
            )

        val route = TestProductRoute("custom123")
        val url = customUrlBuilder.build(route)

        assertEquals("myapp://deeplinks/test/product/custom123", url)

        // Parser should work regardless of scheme/host
        val parsedRoute = parser.parse(url!!)
        assertTrue(parsedRoute is TestProductRoute)
        assertEquals("custom123", parsedRoute.productId)
    }

    @Test
    fun testSpecialCharactersRoundTrip() {
        // Test with special characters that need URL encoding
        val originalRoute = TestProductRoute("a/b c?d=e", "special&chars")

        // Build URL from route - URLBuilder should handle encoding
        val url = urlBuilder.build(originalRoute)

        // Verify URL was built (don't assert exact encoding format)
        assertTrue(url != null, "URL should be built successfully")
        assertTrue(url!!.contains("example.com"), "URL should contain host")
        assertTrue(url.contains("test/product"), "URL should contain product path")

        // Parse URL back to route - this is the critical round-trip test
        val parsedRoute = parser.parse(url)

        // Verify the route was parsed correctly
        assertTrue(parsedRoute is TestProductRoute, "Should parse back to TestProductRoute")
        assertEquals("a/b c?d=e", parsedRoute.productId, "Product ID should be preserved exactly")
        assertEquals("special&chars", parsedRoute.category, "Category should be preserved exactly")
    }

    @Test
    fun testSecurityModelIntegration() {
        val routingTableWithInternal =
            buildRoutingTable {
                route<TestProductRoute>("/product/{productId}")
                route<TestInternalRoute>("/internal/{internalId}")
            }

        val parserWithInternal = DeepLinkParser(routingTableWithInternal)
        val builderWithInternal =
            UrlBuilder(
                routingTable = routingTableWithInternal,
                scheme = "https",
                host = "example.com",
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
