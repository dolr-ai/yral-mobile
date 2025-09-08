package com.yral.shared.libs.routing.deeplink.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class RoutePatternBuildComponentsTest {
    @Test
    fun template_present_ignores_non_template_params_and_maps_sources() {
        val pattern = RoutePattern("/x/{id}?a={alpha}&b")
        val components =
            pattern.buildComponents(
                mapOf(
                    "id" to "1",
                    "alpha" to "foo",
                    "b" to "bar",
                    "extra" to "should-be-ignored",
                ),
            )

        // Path uses id
        assertEquals(listOf("x", "1"), components.pathSegments)
        // Query includes only a and b from the template, with a sourced from alpha
        assertEquals(mapOf("a" to "foo", "b" to "bar"), components.queryParams)
    }
}
