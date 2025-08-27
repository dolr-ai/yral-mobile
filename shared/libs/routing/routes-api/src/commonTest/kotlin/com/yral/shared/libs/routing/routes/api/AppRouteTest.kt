package com.yral.shared.libs.routing.routes.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppRouteTest {

    @Test
    fun testProductDetailsSerializable() {
        val product = ProductDetails("123")
        val json = Json.Default.encodeToString(ProductDetails.serializer(), product)
        val decoded = Json.Default.decodeFromString(ProductDetails.serializer(), json)

        assertEquals("123", decoded.productId)
        assertEquals(product.productId, decoded.productId)
    }

    @Test
    fun testHomeSerializable() {
        val home = Home
        val json = Json.Default.encodeToString(Home.serializer(), home)
        val decoded = Json.Default.decodeFromString(Home.serializer(), json)

        assertEquals(home, decoded)
    }

    @Test
    fun testUnknownSerializable() {
        val unknown = Unknown
        val json = Json.Default.encodeToString(Unknown.serializer(), unknown)
        val decoded = Json.Default.decodeFromString(Unknown.serializer(), json)

        assertEquals(unknown, decoded)
    }

    @Test
    fun testExternallyExposedRoute() {
        val product = ProductDetails("123")
        val home = Home
        val unknown = Unknown

        assertTrue(product is ExternallyExposedRoute)
        assertFalse(home is ExternallyExposedRoute)
        assertFalse(unknown is ExternallyExposedRoute)
    }

    @Test
    fun testAppRouteHierarchy() {
        val product = ProductDetails("456")
        val home = Home
        val unknown = Unknown

        assertTrue(product is AppRoute)
        assertTrue(home is AppRoute)
        assertTrue(unknown is AppRoute)
    }

    @Test
    fun testTestRoutes() {
        val testProduct = TestProductRoute("456", "electronics")
        val testUser = TestUserRoute("user123")
        val testHome = TestHomeRoute
        val testInternal = TestInternalRoute("internal456")

        assertTrue(testProduct is ExternallyExposedRoute)
        assertTrue(testUser is ExternallyExposedRoute)
        assertFalse(testHome is ExternallyExposedRoute)
        assertFalse(testInternal is ExternallyExposedRoute)
    }

    @Test
    fun testTestRoutesSerializable() {
        val testProduct = TestProductRoute("123", "books")
        val json = Json.Default.encodeToString(TestProductRoute.serializer(), testProduct)
        val decoded = Json.Default.decodeFromString(TestProductRoute.serializer(), json)

        assertEquals("123", decoded.productId)
        assertEquals("books", decoded.category)
    }

    @Test
    fun testMetadataTransient() {
        val productWithMetadata = ProductDetails("123")
        val json = Json.Default.encodeToString(ProductDetails.serializer(), productWithMetadata)

        // Metadata should not be included in serialization due to @Transient
        assertFalse(json.contains("metadata"))
    }
}