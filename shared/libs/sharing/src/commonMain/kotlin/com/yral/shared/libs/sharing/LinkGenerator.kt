package com.yral.shared.libs.sharing

/**
 * A platform-agnostic interface to generate shareable links from input data.
 * Implementations can use Branch, Firebase Dynamic Links, or any custom shortener.
 */
interface LinkGenerator {
    suspend fun generateShareLink(input: LinkInput): String
}

/**
 * Input for link generation. At minimum we pass an internal deep link URL that
 * our app can route after opening. Additional optional parameters allow
 * implementations to enrich the link with metadata and analytics.
 */
data class LinkInput(
    val internalUrl: String,
    val feature: String? = null,
    val tags: List<String> = emptyList(),
    val title: String? = null,
    val description: String? = null,
    val contentImageUrl: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)
