import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepLinkParserTest {

    private val routingTable = buildRouting<AppRoute> {
        route<TestProductRoute>("/product/{productId}")
        route<TestUserRoute>("/user/{userId}")
        route<TestHomeRoute>("/")
        route<TestInternalRoute>("/internal/{internalId}")
    }

    private val parser = DeepLinkParser(routingTable)

    @Test
    fun testParseValidProductUrl() {
        val result = parser.parse("https://example.com/product/123")
        
        assertTrue(result is TestProductRoute)
        assertEquals("123", result.productId)
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
        val result = parser.parse("https://example.com/product/123?category=electronics&featured=true")
        
        assertTrue(result is TestProductRoute)
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
    fun testParseFromParameterMap() {
        val params = mapOf("productId" to "123", "category" to "books")
        val result = parser.parse(params)
        
        assertTrue(result is TestProductRoute)
        assertEquals("123", result.productId)
        assertEquals("books", result.category)
    }

    @Test
    fun testParseFromParameterMapInternalRoute() {
        val params = mapOf("internalId" to "secret")
        val result = parser.parse(params)
        
        // Should return Unknown due to security check
        assertTrue(result is Unknown)
    }

    @Test
    fun testParseFromInvalidParameterMap() {
        val params = mapOf("invalidParam" to "value")
        val result = parser.parse(params)
        
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
        
        assertTrue(result is Unknown)
    }
}
