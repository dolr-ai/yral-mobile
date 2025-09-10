package com.yral.shared.libs.routing.deeplink.engine

/**
 * Converts a pattern string like "/post/{postId}?ref={ref}&src" into
 * structured tokens for path and a query template description.
 */
internal object RoutePatternParser {
    fun parse(pattern: String): PatternSpec {
        val pathPart = pattern.substringBefore("?")
        val queryPart = pattern.substringAfter("?", missingDelimiterValue = "")

        val pathTokens = parsePathTokens(pathPart)
        val queryTemplate = parseQueryTemplate(queryPart)

        return PatternSpec(pathTokens = pathTokens, queryTemplate = queryTemplate)
    }

    private fun parsePathTokens(pathPart: String): List<PathToken> =
        pathPart
            .split('/')
            .filter { it.isNotEmpty() }
            .map { token ->
                if (token.startsWith("{") && token.endsWith("}")) {
                    val name = token.removePrefix("{").removeSuffix("}")
                    PathToken.Param(name)
                } else {
                    PathToken.Static(token)
                }
            }

    private fun parseQueryTemplate(queryPart: String): QueryTemplate? {
        if (queryPart.isBlank()) return null

        val keys = mutableSetOf<String>()
        val mappings = mutableMapOf<String, String>()

        queryPart
            .split('&')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { item ->
                val key = item.substringBefore("=", missingDelimiterValue = item).trim()
                if (key.isBlank()) return@forEach

                keys += key
                val valuePartRaw = item.substringAfter("=", missingDelimiterValue = "")
                val valuePart = valuePartRaw.trim()
                val isBraced = valuePart.startsWith("{") && valuePart.endsWith("}")
                val mappedParam =
                    when {
                        isBraced -> valuePart.removePrefix("{").removeSuffix("}").trim()
                        valuePart.isBlank() -> key
                        else -> key // literal defaults not supported; keep same-name lookup
                    }
                mappings[key] = mappedParam
            }

        return QueryTemplate(keys = keys, mappings = mappings)
    }
}

internal data class PatternSpec(
    val pathTokens: List<PathToken>,
    val queryTemplate: QueryTemplate?,
)

internal sealed interface PathToken {
    data class Static(
        val value: String,
    ) : PathToken
    data class Param(
        val name: String,
    ) : PathToken
}

internal data class QueryTemplate(
    val keys: Set<String>,
    // query key -> param name from which to source the value
    val mappings: Map<String, String>,
)
