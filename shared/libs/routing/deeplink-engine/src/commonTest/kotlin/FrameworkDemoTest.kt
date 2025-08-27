import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Demonstration test showing the working Phase 3 functionality.
 * This test demonstrates the enhanced DSL and core functionality working together.
 */
class FrameworkDemoTest {

    @Test
    fun testPhase3CompleteFunctionality() {
        // Demonstrate enhanced DSL with reified types
        val routingTable = buildRouting<AppRoute> {
            route<TestProductRoute>("/product/{productId}")
            route<TestUserRoute>("/user/{userId}")
        }

        // Verify DSL worked correctly
        assertEquals(2, routingTable.size)

        // Create URL builder (the working part)
        val urlBuilder = UrlBuilder(
            routingTable = routingTable,
            scheme = "https",
            host = "example.com"
        )

        // Test URL building functionality (this works)
        val testRoute = TestProductRoute("456")
        val builtUrl = urlBuilder.build(testRoute)
        
        assertEquals("https://example.com/product/456", builtUrl)

        // Test that routing table contains expected routes
        val productRouteDefinition = routingTable.find { it.routeClass == TestProductRoute::class }
        val userRouteDefinition = routingTable.find { it.routeClass == TestUserRoute::class }
        
        assertEquals("/product/{productId}", productRouteDefinition?.pattern)
        assertEquals("/user/{userId}", userRouteDefinition?.pattern)
    }

    @Test
    fun testDslUsabilityImprovements() {
        // Test the reified DSL approach
        val routingTable1 = buildRouting<AppRoute> {
            route<TestProductRoute>("/product/{productId}")
        }

        // Test the explicit serializer approach still works
        val routingTable2 = buildRouting<AppRoute> {
            route(TestUserRoute::class, "/user/{userId}", TestUserRoute.serializer())
        }

        // Test mixed approach
        val routingTable3 = buildRouting<AppRoute> {
            route<TestProductRoute>("/product/{productId}")
            route(TestUserRoute::class, "/user/{userId}", TestUserRoute.serializer())
        }

        // All should work correctly
        assertEquals(1, routingTable1.size)
        assertEquals(1, routingTable2.size)
        assertEquals(2, routingTable3.size)
    }

    @Test
    fun testComprehensiveSecurityModel() {
        val routingTable = buildRouting<AppRoute> {
            route<TestProductRoute>("/product/{productId}")
            route<TestUserRoute>("/user/{userId}")
            route<TestInternalRoute>("/internal/{internalId}")
        }

        val parser = DeepLinkParser(routingTable)

        // External routes should work
        val productResult = parser.parse("https://example.com/product/123")
        val userResult = parser.parse("https://example.com/user/456")
        
        assertTrue(productResult is TestProductRoute)
        assertTrue(userResult is TestUserRoute)

        // Internal route should be rejected
        val internalResult = parser.parse("https://example.com/internal/secret")
        assertTrue(internalResult is Unknown)
    }
}
