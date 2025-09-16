package com.yral.shared.libs.routing.deeplink.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutePatternParserTest {
    @Test
    fun parse_noQuery_onlyStaticPath() {
        val spec = RoutePatternParser.parse("/home")
        assertEquals(listOf(PathToken.Static("home")), spec.pathTokens)
        assertNull(spec.queryTemplate)
    }

    @Test
    fun parse_pathWithParams_noQuery() {
        val spec = RoutePatternParser.parse("/post/{postId}/comment/{commentId}")
        assertEquals(
            listOf(
                PathToken.Static("post"),
                PathToken.Param("postId"),
                PathToken.Static("comment"),
                PathToken.Param("commentId"),
            ),
            spec.pathTokens,
        )
        assertNull(spec.queryTemplate)
    }

    @Test
    fun parse_queryTemplate_sameNameAndMapped() {
        val spec = RoutePatternParser.parse("/post/{postId}?ref={ref}&src&tag={category}")
        val qt = requireNotNull(spec.queryTemplate)

        // Keys should preserve all declared query keys
        assertEquals(setOf("ref", "src", "tag"), qt.keys)

        // Mappings: ref -> ref, src -> src (no value part), tag -> category (mapped)
        assertEquals(
            mapOf(
                "ref" to "ref",
                "src" to "src",
                "tag" to "category",
            ),
            qt.mappings,
        )
    }

    @Test
    fun parse_queryTemplate_trimsWhitespace_and_ignoresBlankEntries() {
        val spec = RoutePatternParser.parse("/a?  x={x}  &  & y = { y } &  z  ")
        val qt = requireNotNull(spec.queryTemplate)

        // Blank entry between && should be ignored; whitespace around tokens should be trimmed
        assertTrue(qt.keys.containsAll(setOf("x", "y", "z")))
        assertEquals(3, qt.keys.size)
        assertEquals(
            mapOf(
                "x" to "x",
                "y" to "y",
                "z" to "z",
            ),
            qt.mappings,
        )
    }

    @Test
    fun parse_emptyQuery_returnsNullTemplate() {
        val spec1 = RoutePatternParser.parse("/a?")
        assertNull(spec1.queryTemplate)

        val spec2 = RoutePatternParser.parse("/a")
        assertNull(spec2.queryTemplate)
    }

    @Test
    fun parse_duplicateKeys_lastMappingWins() {
        val spec = RoutePatternParser.parse("/a?x={a}&x={b}&x")
        val qt = requireNotNull(spec.queryTemplate)
        assertEquals(setOf("x"), qt.keys)
        // Mapping should be the last one: for x, the last declaration is just key with no value -> maps to same name
        assertEquals(mapOf("x" to "x"), qt.mappings)
    }

    @Test
    fun parse_maintainsOrder_indirectlyByIteratingKeys() {
        val spec = RoutePatternParser.parse("/a?a&b&c")
        val qt = requireNotNull(spec.queryTemplate)
        // While keys is a Set, we ensure all are present; order is not guaranteed by contract
        assertTrue(qt.keys.containsAll(listOf("a", "b", "c")))
    }
}
