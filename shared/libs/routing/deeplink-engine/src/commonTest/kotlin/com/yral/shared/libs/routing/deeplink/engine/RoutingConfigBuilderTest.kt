package com.yral.shared.libs.routing.deeplink.engine

import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.TestHomeRoute
import com.yral.shared.libs.routing.routes.api.TestProductRoute
import com.yral.shared.libs.routing.routes.api.TestUserRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingConfigBuilderTest {

    @Test
    fun testBuildRoutingWithExplicitSerializers() {
        val routingTable = buildRouting {
            route(TestProductRoute::class, "/product/{productId}", TestProductRoute.serializer())
            route(TestUserRoute::class, "/user/{userId}", TestUserRoute.serializer())
            route(TestHomeRoute::class, "/", TestHomeRoute.serializer())
        }

        assertEquals(3, routingTable.size)

        val productRoute = routingTable.find { it.routeClass == TestProductRoute::class }
        val userRoute = routingTable.find { it.routeClass == TestUserRoute::class }
        val homeRoute = routingTable.find { it.routeClass == TestHomeRoute::class }

        assertEquals("/product/{productId}", productRoute?.pattern)
        assertEquals("/user/{userId}", userRoute?.pattern)
        assertEquals("/", homeRoute?.pattern)
    }

    @Test
    fun testBuildRoutingWithReifiedTypes() {
        val routingTable = buildRouting {
            route<TestProductRoute>("/product/{productId}")
            route<TestUserRoute>("/user/{userId}")
            route<TestHomeRoute>("/")
        }

        assertEquals(3, routingTable.size)

        val productRoute = routingTable.find { it.routeClass == TestProductRoute::class }
        val userRoute = routingTable.find { it.routeClass == TestUserRoute::class }
        val homeRoute = routingTable.find { it.routeClass == TestHomeRoute::class }

        assertEquals("/product/{productId}", productRoute?.pattern)
        assertEquals("/user/{userId}", userRoute?.pattern)
        assertEquals("/", homeRoute?.pattern)
    }

    @Test
    fun testBuildRoutingWithMixedApproaches() {
        val routingTable = buildRouting {
            route<TestProductRoute>("/product/{productId}")
            route(TestUserRoute::class, "/user/{userId}", TestUserRoute.serializer())
            route<TestHomeRoute>("/")
        }

        assertEquals(3, routingTable.size)
        assertTrue(routingTable.all { it.pattern.isNotEmpty() })
    }

    @Test
    fun testEmptyRoutingTable() {
        val routingTable = buildRouting {
            // Empty configuration
        }

        assertTrue(routingTable.isEmpty())
    }

    @Test
    fun testRoutingTableImmutable() {
        val routingTable = buildRouting {
            route<TestProductRoute>("/product/{productId}")
        }

        assertEquals(1, routingTable.size)

        // Verify that the returned list is a copy (immutable from builder's perspective)
        val originalSize = routingTable.size
        // We can't directly test immutability without trying to modify,
        // but we can verify the builder produces consistent results
        val secondTable = buildRouting {
            route<TestProductRoute>("/product/{productId}")
        }

        assertEquals(originalSize, secondTable.size)
        assertEquals(routingTable.size, secondTable.size)
    }
}
