package com.yral.shared.http.spacetime

import com.yral.shared.core.AppConfigurations
import com.yral.shared.http.exception.NetworkException
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

/**
 * Remote data source for SpacetimeDB REST API calls.
 *
 * Calls SpacetimeDB procedures and reducers via the REST endpoint:
 * `POST /v1/database/{db}/call/{name}` with a JSON array body of positional
 * arguments and the user's yral-auth JWT as the `Authorization: Bearer` token.
 *
 * The JWT's `iss` (issuer) and `sub` (subject) claims are used by SpacetimeDB
 * to compute the caller's `Identity` — this is the same mechanism yral-auth
 * uses for its admin token. User JWTs from yral-auth (Google OAuth / phone OTP)
 * work identically.
 *
 * ## REST response format
 * SpacetimeDB wraps procedure results in an outer array: `[[<result>]]`.
 * For `Option<T>`:
 * - `Some(value)` → `[[0, <value JSON>]]`
 * - `None` → `[[1, []]]`
 */
class SpacetimeDBRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
) {
    private val baseUrl: String = AppConfigurations.SPACETIMEDB_BASE_URL
    private val dbName: String = AppConfigurations.SPACETIMEDB_DB_NAME

    /**
     * Get a single post by ID from SpacetimeDB.
     *
     * Calls the `get_post_by_id` procedure, which checks the `posts_v2`
     * table first (has `creator_principal_text`), falling back to the
     * legacy `posts` table.
     *
     * @param postId The post's UUID.
     * @return `SpacetimePostDetails` if found, `null` if the post doesn't
     *         exist or is deleted.
     */
    suspend fun getPostById(postId: String): SpacetimePostDetails? {
        val idToken = getIdTokenOrNull()
        val responseBody = callProcedure("get_post_by_id", listOf(postId), idToken)
        return parseOptionPostDetails(responseBody)
    }

    /**
     * Call a SpacetimeDB procedure or reducer via REST.
     *
     * @param name The procedure/reducer name (e.g. "get_post_by_id").
     * @param args Positional arguments as a list.
     * @param token Optional bearer token (user JWT). If null, the call is
     *              anonymous (SpacetimeDB sets `sender = Identity::ZERO`).
     * @return The raw response body as a string.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun callProcedure(
        name: String,
        args: List<String>,
        token: String?,
    ): String {
        try {
            val response =
                httpClient.post {
                    url {
                        host = baseUrl
                        path("v1", "database", dbName, "call", name)
                    }
                    contentType(ContentType.Application.Json)
                    if (token != null) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                    // SpacetimeDB expects a JSON array of positional arguments.
                    val jsonArgs = JsonArray(args.map { JsonPrimitive(it) })
                    setBody(json.encodeToString(JsonArray.serializer(), jsonArgs))
                }
            return response.bodyAsText()
        } catch (e: Exception) {
            throw NetworkException(e)
        }
    }

    /**
     * Parse the `Option<PostDetailsForFrontend>` REST response.
     *
     * The response is `[[variant_index, payload]]`:
     * - `[[0, {post JSON}]]` → Some(post)
     * - `[[1, []]]` → None
     */
    @Suppress("ThrowsCount")
    private fun parseOptionPostDetails(responseBody: String): SpacetimePostDetails? {
        val outerArray =
            json.parseToJsonElement(responseBody) as? JsonArray
                ?: throw parseError("expected JSON array", responseBody)

        val innerArray =
            outerArray.firstOrNull() as? JsonArray
                ?: throw parseError("expected nested array", responseBody)

        val variantIndex =
            (innerArray.getOrNull(0) as? JsonPrimitive)?.content?.toIntOrNull()
                ?: throw parseError("expected variant index", responseBody)

        if (variantIndex != 0) return null // None

        val postJson =
            innerArray.getOrNull(1)
                ?: throw parseError("expected post payload", responseBody)

        return json.decodeFromString(SpacetimePostDetails.serializer(), postJson.toString())
    }

    private fun parseError(
        expected: String,
        responseBody: String,
    ): NetworkException =
        NetworkException(
            IllegalArgumentException("SpacetimeDB: $expected, got: $responseBody"),
        )

    /**
     * Read the yral-auth JWT from preferences. Returns null if not authenticated.
     * SpacetimeDB accepts any OIDC-compliant JWT — anonymous calls use
     * `Identity::ZERO` as the sender.
     */
    private suspend fun getIdTokenOrNull(): String? = preferences.getString(PrefKeys.ID_TOKEN.name)
}
