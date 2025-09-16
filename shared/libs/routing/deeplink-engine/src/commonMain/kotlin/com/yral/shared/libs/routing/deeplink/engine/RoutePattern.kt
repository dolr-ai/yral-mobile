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
    private val spec: PatternSpec = RoutePatternParser.parse(pattern)

    @Suppress("ReturnCount")
    fun extractParameters(pathSegments: List<String>): Map<String, String>? {
        if (pathSegments.size != spec.pathTokens.size) {
            return null
        }

        val extractedParams = mutableMapOf<String, String>()
        for (i in spec.pathTokens.indices) {
            val patternToken = spec.pathTokens[i]
            val pathSegment = pathSegments[i]

            when (patternToken) {
                is PathToken.Param -> extractedParams[patternToken.name] = pathSegment
                is PathToken.Static -> if (patternToken.value != pathSegment) return null
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

        spec.pathTokens.forEach { token ->
            when (token) {
                is PathToken.Param -> {
                    val key = token.name
                    usedKeys.add(key)
                    val value = params[key].orEmpty()
                    if (value.isNotEmpty()) {
                        builtSegments.add(value)
                    }
                }
                is PathToken.Static -> builtSegments.add(token.value)
            }
        }

        return BuiltPath(segments = builtSegments, usedKeys = usedKeys)
    }

    /**
     * Build both path segments and query parameters from the given params
     * according to this pattern's path and optional query template.
     *
     * - Path placeholders are substituted from [params]. All path parameters
     *   are required and must be non-blank; if any are missing or blank, the
     *   build will fail (callers should return null).
     * - Query parameters:
     *   - If the pattern defines a query template, only keys present in the
     *     template are included. If the template maps a key to a different
     *     param name (e.g., key={foo}), that name is used to source the value.
     *   - If no template is present, include all remaining non-blank params
     *     excluding metadata and those used in the path.
     * - Blank strings and the literal string "null" are filtered out.
     */
    fun buildComponents(params: Map<String, String>): BuiltComponents {
        // Enforce that all path placeholder parameters are present and non-blank
        val requiredKeys = spec.pathTokens.filterIsInstance<PathToken.Param>().map { it.name }
        val missingKey =
            requiredKeys.firstOrNull { key ->
                val v = params[key]
                v.isNullOrBlank() || v == "null"
            }
        require(missingKey == null) { "Missing required path parameter: $missingKey" }

        val builtPath = buildPathSegments(params)
        val usedPathKeys = builtPath.usedKeys

        val qp: Map<String, String> =
            spec.queryTemplate?.let { qt ->
                qt.keys
                    .associateWith { key ->
                        val source = qt.mappings[key] ?: key
                        params[source].orEmpty()
                    }.filterValues { v -> v.isNotBlank() && v != "null" }
            } ?: run {
                params
                    .filterKeys { k -> k !in usedPathKeys && k != "metadata" }
                    .filterValues { v -> v.isNotBlank() && v != "null" }
            }

        return BuiltComponents(pathSegments = builtPath.segments, queryParams = qp)
    }

    /** Returns true if this pattern contains a query template part. */
    fun hasQueryTemplate(): Boolean = spec.queryTemplate != null

    /** Returns allowed query parameter keys if a template exists, else empty. */
    fun queryTemplateKeys(): Set<String> = spec.queryTemplate?.keys ?: emptySet()

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

/**
 * Represents path and query components built from a pattern and params.
 */
data class BuiltComponents(
    val pathSegments: List<String>,
    val queryParams: Map<String, String>,
)
