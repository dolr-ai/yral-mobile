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
class RoutePattern(private val pattern: String) {

    private val segments = pattern.split("/").filter { it.isNotEmpty() }
    private val pathParamKeys = segments
        .filter { it.startsWith("{") && it.endsWith("}") }
        .map { it.removeSurrounding("{", "}") }
        .toSet()

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
     * Builds a path string by substituting parameters into the pattern.
     *
     * @param params A map of all available parameters for the route.
     * @return A pair containing the constructed path string and a set of the
     *         parameter keys that were used in the path.
     */
    fun buildPath(params: Map<String, String>): Pair<String, Set<String>> {
        var path = pattern
        val usedKeys = mutableSetOf<String>()

        pathParamKeys.forEach { key ->
            val value = params[key] ?: ""
            path = path.replace("{$key}", value)
            usedKeys.add(key)
        }

        // Clean up path for cases where optional params are empty
        // e.g., "/product/{id}/" becomes "/product/" if id is empty
        val finalPath = path.split("/").filter { it.isNotEmpty() }.joinToString(separator = "/", prefix = "/")
        
        return finalPath to usedKeys
    }

    override fun toString(): String {
        return pattern
    }
}
