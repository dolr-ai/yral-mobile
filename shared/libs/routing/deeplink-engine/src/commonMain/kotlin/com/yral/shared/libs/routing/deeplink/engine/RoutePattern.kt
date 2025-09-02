package com.yral.shared.libs.routing.deeplink.engine

/**
 * A class that encapsulates the logic for a URL path pattern.
 *
 * This class provides a single, authoritative implementation for both parsing
 * a path to extract parameters and building a path from a set of parameters.
 * This ensures that the parsing and building logic are always symmetrical.
 *
 * @param pattern The raw URL pattern string (e.g., "/product/{productId}").
 */
class RoutePattern(
    private val pattern: String,
) {
    private val segments = pattern.split("/").filter { it.isNotEmpty() }

    @Suppress("ReturnCount")
    fun extractParameters(pathSegments: List<String>): Map<String, String>? {
        if (pathSegments.size != segments.size) {
            return null
        }

        val extractedParams = mutableMapOf<String, String>()
        for (i in segments.indices) {
            val patternSegment = segments[i]
            val pathSegment = pathSegments[i]

            if (patternSegment.startsWith("{") && patternSegment.endsWith("}")) {
                val paramName = patternSegment.substring(1, patternSegment.length - 1)
                extractedParams[paramName] = pathSegment
            } else if (patternSegment != pathSegment) {
                return null // Static segment doesn't match
            }
        }
        return extractedParams
    }

    /**
     * Builds a list of path segments by substituting parameters into the pattern.
     * This avoids constructing an intermediate path string and then splitting,
     * which can be unsafe when parameter values contain '/'. The returned
     * segments are raw (unencoded). Callers should pass them to a URL builder
     * that performs proper percent-encoding per segment.
     *
     * @param params A map of all available parameters for the route.
     * @return A [BuiltPath] containing the constructed list of path segments and
     *         the parameter keys that were used in the path.
     */
    fun buildPathSegments(params: Map<String, String>): BuiltPath {
        val usedKeys = mutableSetOf<String>()
        val builtSegments = mutableListOf<String>()

        segments.forEach { segment ->
            if (segment.startsWith("{") && segment.endsWith("}")) {
                val key = segment.removeSurrounding("{", "}")
                usedKeys.add(key)
                val value = params[key].orEmpty()
                if (value.isNotEmpty()) {
                    builtSegments.add(value)
                }
            } else {
                builtSegments.add(segment)
            }
        }

        return BuiltPath(segments = builtSegments, usedKeys = usedKeys)
    }

    override fun toString(): String = pattern
}

/**
 * Represents a built path from a route pattern and parameter map.
 *
 * @property segments The raw, unencoded path segments to use in the URL.
 * @property usedKeys The parameter keys that were consumed in building the path.
 */
data class BuiltPath(
    val segments: List<String>,
    val usedKeys: Set<String>,
)
