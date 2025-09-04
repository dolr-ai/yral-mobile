package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.libs.routing.routes.api.TestHomeRoute
import com.yral.shared.libs.routing.routes.api.TestInternalRoute
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUnknownRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlBuilderTest {
    private val routingTable =
        buildRoutingTable {
            route<TestProductRoute>("/product/{productId}")
            route<TestUserRoute>("/user/{userId}")
            route<TestHomeRoute>("/")
            route<TestInternalRoute>("/internal/{internalId}")
            route<PostDetailsRoute>(PostDetailsRoute.PATH)
        }

    private val urlBuilder =
        UrlBuilder(
            routingTable = routingTable,
            scheme = "https",
            host = "example.com",
        )

    @Test
    fun testBuildProductUrl() {
        val route = TestProductRoute("123")
        val url = urlBuilder.build(route)

        assertEquals("https://example.com/product/123", url)
    }

    @Test
    fun testBuildUserUrl() {
        val route = TestUserRoute("456")
        val url = urlBuilder.build(route)

        assertEquals("https://example.com/user/456", url)
    }

    @Test
    fun testBuildHomeUrl() {
        val route = TestHomeRoute
        val url = urlBuilder.build(route)

        assertEquals("https://example.com", url)
    }

    @Test
    fun testBuildInternalUrl() {
        val route = TestInternalRoute("secret")
        val url = urlBuilder.build(route)

        // Should still build URL even for internal routes (security is enforced in parser)
        assertEquals("https://example.com/internal/secret", url)
    }

    @Test
    fun testBuildUrlWithCustomScheme() {
        val customBuilder =
            UrlBuilder(
                routingTable = routingTable,
                scheme = "myapp",
                host = "deeplink",
            )

        val route = TestProductRoute("789")
        val url = customBuilder.build(route)

        assertEquals("myapp://deeplink/product/789", url)
    }

    @Test
    fun testBuildUrlForPostDetails() {
        val customBuilder =
            UrlBuilder(
                routingTable = routingTable,
                scheme = "yralm",
                host = "",
            )

        val route = PostDetailsRoute(postId = "789")
        val url = customBuilder.build(route)

        assertEquals("yralm://post/details/789", url)
    }

    @Test
    fun testBuildUrlWithLeadingSlashPatternAndEmptyHost() {
        // Even if the pattern has a leading slash, hostless deep link should treat
        // the first path segment as the authority when scheme is custom and host is blank
        val customBuilder =
            UrlBuilder(
                routingTable = routingTable,
                scheme = "yralm",
                host = "",
            )

        val route = TestProductRoute("123") // pattern is "/test/product/{productId}"
        val url = customBuilder.build(route)

        assertEquals("yralm://product/123", url)
    }

    @Test
    fun testBuildUrlWithUnknownRoute() {
        val route = TestUnknownRoute
        val url = urlBuilder.build(route)

        // Should return null for unknown routes not in routing table
        assertNull(url)
    }

    @Test
    fun testBuildUrlWithProductAndCategory() {
        val route = TestProductRoute("123", "electronics")
        val url = urlBuilder.build(route)

        // Category should be included as query parameter since it's not in the path pattern
        assertEquals("https://example.com/product/123?category=electronics", url)
    }

    @Test
    fun testBuildUrlWithEmptyProductId() {
        val route = TestProductRoute("")
        val url = urlBuilder.build(route)

        assertEquals("https://example.com/product", url)
    }

    @Test
    fun testBuildUrlWithSpecialCharacters() {
        val route = TestProductRoute("a/b c?d=e")
        val url = urlBuilder.build(route)

        assertEquals("https://example.com/product/a%2Fb%20c%3Fd=e", url)
    }

    @Test
    fun testBuildUrlWithNumericIds() {
        val productRoute = TestProductRoute("12345")
        val userRoute = TestUserRoute("67890")

        val productUrl = urlBuilder.build(productRoute)
        val userUrl = urlBuilder.build(userRoute)

        assertEquals("https://example.com/product/12345", productUrl)
        assertEquals("https://example.com/user/67890", userUrl)
    }

    @Test
    fun testUrlBuilderConsistency() {
        val route = TestProductRoute("consistency-test")

        // Build URL multiple times - should be consistent
        val url1 = urlBuilder.build(route)
        val url2 = urlBuilder.build(route)
        val url3 = urlBuilder.build(route)

        assertEquals(url1, url2)
        assertEquals(url2, url3)
        assertEquals("https://example.com/product/consistency-test", url1)
    }

    @Test
    fun testBuildUrlWithQueryTemplateIncludedWhenPresent() {
        // PostDetails defines a query template for canisterId
        val customBuilder =
            UrlBuilder(
                routingTable = routingTable,
                scheme = "yralm",
                host = "",
            )

        val route = PostDetailsRoute(postId = "789", canisterId = "cid-123")
        val url = customBuilder.build(route)

        assertEquals("yralm://post/details/789?canisterId=cid-123", url)
    }

    @Test
    fun testBuildUrlFiltersBlankAndNullQueryParams() {
        // When no query template is present, remaining non-blank params are included.
        // Blank ("") and literal "null" should be filtered out.

        // Blank
        val blankCategoryUrl = urlBuilder.build(TestProductRoute(productId = "123", category = ""))
        assertEquals("https://example.com/product/123", blankCategoryUrl)

        // Literal "null"
        val literalNullCategoryUrl = urlBuilder.build(TestProductRoute(productId = "123", category = "null"))
        assertEquals("https://example.com/product/123", literalNullCategoryUrl)
    }

    @Test
    fun testQueryTemplateFiltersBlankMappedParam() {
        val customBuilder =
            UrlBuilder(
                routingTable = routingTable,
                scheme = "yralm",
                host = "",
            )
        val url = customBuilder.build(PostDetailsRoute(postId = "999", canisterId = ""))
        // canisterId blank -> should be filtered and no query appended
        assertEquals("yralm://post/details/999", url)
    }
}
