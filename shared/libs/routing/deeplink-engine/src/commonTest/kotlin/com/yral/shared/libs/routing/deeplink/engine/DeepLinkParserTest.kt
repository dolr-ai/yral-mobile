package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.PostDetails
import com.yral.shared.libs.routing.routes.api.TestHomeRoute
import com.yral.shared.libs.routing.routes.api.TestInternalRoute
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import com.yral.shared.libs.routing.routes.api.Unknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepLinkParserTest {
    private val routingTable =
        buildRoutingTable {
            route<TestProductRoute>("/product/{productId}")
            route<TestUserRoute>("/user/{userId}")
            route<TestHomeRoute>("/")
            route<TestInternalRoute>("/internal/{internalId}")
            route<PostDetails>(PostDetails.PATH)
        }

    private val parser = DeepLinkParser(routingTable)

    @Test
    fun testParseValidProductUrl() {
        val result = parser.parse("https://example.com/product/123")

        assertTrue(result is TestProductRoute)
        assertEquals("123", result.productId)
    }

    @Test
    fun testParseValidPostDetailsUrl() {
        val result = parser.parse("https://example.com/post/details/123")

        println("result: $result")

        assertTrue(result is PostDetails)
        assertEquals("123", result.postId)
    }

    @Test
    fun testParseValidUserUrl() {
        val result = parser.parse("https://example.com/user/456")

        assertTrue(result is TestUserRoute)
        assertEquals("456", result.userId)
    }

    @Test
    fun testParseHomeUrl() {
        val result = parser.parse("https://example.com/")

        assertTrue(result is TestHomeRoute)
    }

    @Test
    fun testParseUrlWithQueryParams() {
        // Test with only valid parameters first
        val result = parser.parse("https://example.com/product/123?category=electronics")

        if (result !is TestProductRoute) {
            throw AssertionError("Expected TestProductRoute, got: $result (${result::class.simpleName})")
        }
        assertEquals("123", result.productId)
        assertEquals("electronics", result.category)
    }

    @Test
    fun testParseInternalRouteReturnsUnknown() {
        // Internal route should be rejected due to security check
        val result = parser.parse("https://example.com/internal/secret")

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseInvalidUrlReturnsUnknown() {
        val result = parser.parse("https://example.com/nonexistent/path")

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseMalformedUrlReturnsUnknown() {
        val result = parser.parse("not-a-valid-url")

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseWithDifferentScheme() {
        val result = parser.parse("myapp://example.com/product/789")

        assertTrue(result is TestProductRoute)
        assertEquals("789", result.productId)
    }

    @Test
    fun testParseFromParameterMapWithoutRouteId() {
        // Test fallback behavior when route_id is not provided
        val params = mapOf("productId" to "123", "category" to "books")
        val result = parser.parse(params)

        // Should now return Unknown because route_id is missing
        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromParameterMapWithRouteId() {
        // Test explicit route_id behavior
        val productRouteId = TestProductRoute.serializer().descriptor.serialName
        val params =
            mapOf("route_id" to productRouteId, "productId" to "123", "category" to "books")
        val result = parser.parse(params)

        // Debug: show what we actually got
        when (result) {
            is TestProductRoute -> {
                assertEquals("123", result.productId)
                assertEquals("books", result.category)
            }

            else -> throw AssertionError("Expected TestProductRoute, got: $result (${result::class.simpleName})")
        }
    }

    @Test
    fun testParseFromParameterMapWithWrongRouteId() {
        // Test with wrong route_id - should fail
        val params = mapOf("route_id" to "NonExistentRoute", "productId" to "123")
        val result = parser.parse(params)

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromParameterMapUserRouteWithoutRouteId() {
        // Test fallback behavior with different route type
        val params = mapOf("userId" to "456")
        val result = parser.parse(params)

        // Should now return Unknown because route_id is missing
        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromParameterMapInternalRoute() {
        // Test without route_id - should return Unknown due to security check
        val params = mapOf("internalId" to "secret")
        val result = parser.parse(params)

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromParameterMapInternalRouteWithRouteId() {
        // Test with explicit route_id - should still return Unknown due to security check
        val internalRouteId = TestInternalRoute.serializer().descriptor.serialName
        val params = mapOf("route_id" to internalRouteId, "internalId" to "secret")
        val result = parser.parse(params)

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromInvalidParameterMap() {
        val params = mapOf("invalidParam" to "value")
        val result = parser.parse(params)

        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromParameterMapAmbiguousWithoutRouteId() {
        // Test case where parameters could match multiple routes
        // Should return Unknown because route_id is missing
        val params = mapOf<String, String>() // Empty params - could match TestHomeRoute
        val result = parser.parse(params)

        // Should now be Unknown
        assertTrue(result is Unknown)
    }

    @Test
    fun testParseUrlWithTrailingSlash() {
        val result = parser.parse("https://example.com/product/123/")

        // Should still match the pattern
        assertTrue(result is TestProductRoute)
        assertEquals("123", result.productId)
    }

    @Test
    fun testParseUrlWithExtraSegments() {
        val result = parser.parse("https://example.com/product/123/extra/segments")

        // Should not match due to extra segments
        assertTrue(result is Unknown)
    }

    @Test
    fun testParseUrlWithMissingSegments() {
        val result = parser.parse("https://example.com/product/")

        // Should not match due to missing productId
        assertTrue(result is Unknown)
    }

    @Test
    fun testParseEmptyUrl() {
        val result = parser.parse("")

        if (result !is Unknown) {
            throw AssertionError("Expected Unknown, got: $result (${result::class.simpleName})")
        }
    }
}
