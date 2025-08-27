import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrlBuilderTest {

    private val routingTable = buildRouting<AppRoute> {
        route<TestProductRoute>("/product/{productId}")
        route<TestUserRoute>("/user/{userId}")
        route<TestHomeRoute>("/")
        route<TestInternalRoute>("/internal/{internalId}")
    }

    private val urlBuilder = UrlBuilder(
        routingTable = routingTable,
        scheme = "https",
        host = "example.com"
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
        
        assertEquals("https://example.com/", url)
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
        val customBuilder = UrlBuilder(
            routingTable = routingTable,
            scheme = "myapp",
            host = "deeplink"
        )
        
        val route = TestProductRoute("789")
        val url = customBuilder.build(route)
        
        assertEquals("myapp://deeplink/product/789", url)
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
        
        // Note: Current implementation only extracts productId
        // Category is not part of the URL pattern, so it won't be included
        assertEquals("https://example.com/product/123", url)
    }

    @Test
    fun testBuildUrlWithEmptyProductId() {
        val route = TestProductRoute("")
        val url = urlBuilder.build(route)
        
        assertEquals("https://example.com/product/", url)
    }

    @Test
    fun testBuildUrlWithSpecialCharacters() {
        val route = TestProductRoute("product-with-dashes_and_underscores")
        val url = urlBuilder.build(route)
        
        assertEquals("https://example.com/product/product-with-dashes_and_underscores", url)
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
}
